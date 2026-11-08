import assert from 'node:assert/strict';
import {mkdirSync, writeFileSync} from 'node:fs';
import {createRequire} from 'node:module';
import {fileURLToPath, pathToFileURL} from 'node:url';
import path from 'node:path';
import {randomUUID} from 'node:crypto';
import {document, resolve} from './check-contract-compatibility.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(path.join(root, 'frontend/package.json'));
const Ajv = require('ajv/dist/2020.js').default;
const ajv = new Ajv({strict: false, allErrors: true, validateSchema: false});
require('ajv-formats')(ajv);
const contract = path.join(root, 'docs/technical/contracts/openapi.yaml');
const files = [contract, ...['001-identity-access-scopes', '002-version-evaluation-model', '003-manage-self-assessment',
  '004-submit-evidence', '005-complete-evaluation-workflows'].map(feature => path.join(root, 'specs', feature, 'contracts/openapi.yaml'))];
for (const file of files) ajv.addSchema(document(file), pathToFileURL(file).href);
const base = new URL(process.env.SEC_API_BASE_URL ?? 'http://127.0.0.1:18085/api/v1/');
assert(['127.0.0.1', 'localhost', '[::1]'].includes(base.hostname), 'Run only against a dedicated local synthetic backend');
assert.equal(process.env.SEC_API_SMOKE_SYNTHETIC, 'true', 'Set SEC_API_SMOKE_SYNTHETIC=true only for the dedicated synthetic instance');
base.pathname = base.pathname.replace(/\/?$/, '/');
const output = path.resolve(process.env.SEC_API_SMOKE_OUTPUT ?? path.join(root, 'backend/target/api-response-smoke.json'));
const cases = [];
const admin = {}, reader = {};

function schemaValidator(schema, file) {
  assert(schema?.$ref, 'Expected a referenced public response schema');
  return ajv.compile({$ref: new URL(schema.$ref, pathToFileURL(file)).href});
}
async function call(name, client, method, route, expected, {body, suffix = '', headers = {}, csrf = true} = {}) {
  const operation = resolve(document(contract).paths[route], contract);
  const responseSchema = resolve(operation.node[method.toLowerCase()].responses[String(expected)]
    ?? operation.node[method.toLowerCase()].responses.default, operation.file);
  assert(responseSchema.node, `Response not documented: ${method} ${route} ${expected}`);
  const schema = responseSchema.node.content?.['application/json']?.schema;
  const actualPath = route.replace('{careerId}', client.careerId ?? randomUUID()) + suffix;
  const response = await fetch(new URL(actualPath.replace(/^\//, ''), base), {
    method, signal: AbortSignal.timeout(15_000), headers: {'Content-Type': 'application/json',
      ...(client.cookie ? {Cookie: client.cookie} : {}), ...(csrf && client.csrf ? {'X-CSRF-TOKEN': client.csrf} : {}), ...headers},
    ...(body ? {body: JSON.stringify(body)} : {})
  });
  const text = await response.text();
  let value = null, errors = null;
  if (expected !== 204) {
    value = JSON.parse(text);
    const validate = schemaValidator(schema, responseSchema.file);
    if (!validate(value)) errors = validate.errors;
  }
  const passed = response.status === expected && !errors && (expected !== 204 || text.length === 0);
  cases.push({name, method, route, expectedStatus: expected, actualStatus: response.status,
    schemaValid: expected === 204 ? null : !errors, passed, ...(errors ? {schemaErrors: errors} : {})});
  assert(passed, `${name}: HTTP ${response.status}; schema errors: ${JSON.stringify(errors)}`);
  if (method === 'POST' && route === '/sessions' && expected === 201) {
    client.cookie = response.headers.get('set-cookie')?.split(';')[0];
    client.csrf = value.data.csrfToken;
  }
  return {value, response};
}

try {
  await call('sin autenticación', {}, 'GET', '/me', 401);
  await call('validación de acceso', {}, 'POST', '/sessions', 400, {body: {username: 'ab', password: 'short'}});
  await call('sesión sintética', admin, 'POST', '/sessions', 201, {body: {username: 'admin', password: 'Synthetic-Admin-2026'}});
  const code = `API-RESPONSE-${randomUUID()}`;
  const unit = await call('crear unidad', admin, 'POST', '/academic-units', 201, {body: {code, name: 'Unidad sintética HTTP'}});
  const body = {code, name: 'Carrera sintética HTTP', academicUnitId: unit.value.data.id, validFrom: '2020-01-01'};
  await call('rechazo CSRF', admin, 'POST', '/careers', 403, {body, csrf: false});
  const career = await call('crear carrera', admin, 'POST', '/careers', 201, {body});
  admin.careerId = career.value.data.id;
  assert(career.response.headers.get('location')); assert.equal(career.response.headers.get('etag'), '"1"');
  await call('lectura persistida', admin, 'GET', '/careers/{careerId}', 200);
  await call('duplicidad', admin, 'POST', '/careers', 409, {body});
  await call('campos incompletos', admin, 'POST', '/careers', 400, {body: {code}});
  await call('paginación', admin, 'GET', '/careers', 200, {suffix: '?page=0&size=1'});
  await call('revisión requerida', admin, 'PATCH', '/careers/{careerId}', 428, {body});
  await call('actualizar con revisión', admin, 'PATCH', '/careers/{careerId}', 200, {body, headers: {'If-Match': '"1"'}});
  await call('revisión obsoleta', admin, 'PATCH', '/careers/{careerId}', 412, {body, headers: {'If-Match': '"1"'}});
  await call('recurso inexistente', {}, 'GET', '/careers/{careerId}', 404, {headers: {Cookie: admin.cookie}});
  await call('lector sintético', reader, 'POST', '/sessions', 201, {body: {username: 'responsable', password: 'Synthetic-User-2026'}});
  reader.careerId = admin.careerId;
  await call('objeto ajeno', reader, 'GET', '/careers/{careerId}', 404);
  await call('capacidad insuficiente', reader, 'POST', '/careers', 403, {body});
  await call('logout sin cuerpo', admin, 'DELETE', '/sessions', 204);
  await call('sesión revocada', admin, 'GET', '/me', 401);
} finally {
  mkdirSync(path.dirname(output), {recursive: true});
  // Never retain request passwords, cookies, bearer tokens or CSRF tokens.
  writeFileSync(output, JSON.stringify({executedAt: new Date().toISOString(), synthetic: true,
    passed: cases.every(test => test.passed) && cases.length === 19, cases}, null, 2) + '\n');
  console.log(`${cases.filter(test => test.passed).length}/${cases.length} HTTP/schema checks passed. ${output}`);
}
