import {spawnSync} from 'node:child_process';
import {readFileSync, writeFileSync, mkdirSync, existsSync} from 'node:fs';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(path.join(root, 'frontend/package.json'));
const {parse} = require('yaml');
const source = path.join(root, 'docs/technical/contracts/openapi.yaml');
const documents = new Map();
function document(file) {
  if (!documents.has(file)) documents.set(file, parse(readFileSync(file, 'utf8')));
  return documents.get(file);
}
function dereference(node, file = source) {
  if (Array.isArray(node)) return node.map(value => dereference(value, file));
  if (!node || typeof node !== 'object') return node;
  if (node.$ref) {
    const [relative, pointer] = node.$ref.split('#');
    const target = path.resolve(path.dirname(file), relative || path.basename(file));
    const resolved = pointer.split('/').slice(1).reduce((value, key) => value[key.replaceAll('~1', '/').replaceAll('~0', '~')], document(target));
    assert(resolved, `Unresolved schema ${node.$ref}`);
    return dereference(resolved, target);
  }
  return Object.fromEntries(Object.entries(node).map(([key, value]) => [key, dereference(value, file)]));
}

const contract = document(source);
const processId = 'f1060000-0000-0000-0011-000000000001';
const cases = [
  {method: 'post', route: '/sessions', status: 201,
    body: {username: 'perf001', password: 'Synthetic-Perf-2026'}},
  {method: 'get', route: '/me', status: 200},
  {method: 'get', route: '/processes/{processId}/documentary-progress', status: 200, query: '?page=0&size=20'},
  {method: 'get', route: '/processes/{processId}/closures', status: 200, query: '?page=0&size=20'},
  {method: 'get', route: '/processes/{processId}/documentary-progress', status: 422, query: '?page=-1&size=20'},
  {method: 'get', route: '/processes/{processId}/documentary-progress', status: 404,
    process: 'ffffffff-ffff-ffff-ffff-ffffffffffff'},
  {method: 'post', route: '/sessions', status: 201,
    body: {username: 'admin', password: 'Synthetic-Admin-2026'}},
  {method: 'get', route: '/models/{modelId}/versions/configured', status: 200},
  {method: 'post', route: '/model-versions/{versionId}/successors/configured', status: 201},
  {method: 'get', route: '/models/{modelId}/versions/configured', status: 200},
  {method: 'delete', route: '/sessions', status: 204},
  {method: 'get', route: '/me', status: 401}
];

const items = cases.map(({method, route, status, query = '', process: selected = processId, body}) => {
  const operation = dereference(contract.paths[route])?.[method];
  assert(operation, `Operation absent from common contract: ${method} ${route}`);
  const response = operation.responses[String(status)];
  assert(response, `Undocumented response: ${status} ${method} ${route}`);
  const schema = dereference(response).content?.['application/json']?.schema
    ?? dereference(response).content?.['application/problem+json']?.schema;
  const tests = [`pm.test('contract status ${status}', () => pm.response.to.have.status(${status}));`];
  if (schema) tests.push(`pm.test('response matches common OpenAPI schema', () => pm.response.to.have.jsonSchema(${JSON.stringify(schema)}));`);
  if (method === 'post' && route === '/sessions') tests.push("pm.variables.set('csrf', pm.response.json().csrfToken);");
  if (method === 'get' && status === 200 && route.endsWith('documentary-progress')) {
    tests.push("pm.test('synthetic scope and bounded page', () => { const body = pm.response.json(); pm.expect(body.totalSources).to.eql(500); pm.expect(body.sources.length).to.eql(20); });");
  }
  return {name: `${method.toUpperCase()} ${route} → ${status}`, request: {
    method: method.toUpperCase(), url: `{{baseUrl}}${route.replace('{processId}', selected)
      .replace('{modelId}', 'f1060000-0000-0000-0010-000000000001')
      .replace('{versionId}', 'f1060000-0000-0000-0010-000000000002')}${query}`,
    header: [{key: 'Content-Type', value: 'application/json'}, {key: 'X-CSRF-TOKEN', value: '{{csrf}}'}],
    ...(body ? {body: {mode: 'raw', raw: JSON.stringify(body)}} : {})
  }, event: [{listen: 'test', script: {type: 'text/javascript', exec: tests}}]};
});

const output = path.join(root, 'backend/target/postman-contracts');
mkdirSync(output, {recursive: true});
const file = path.join(output, 'complete-workflows.postman_collection.json');
writeFileSync(file, JSON.stringify({info: {name: 'SEC synthetic common OpenAPI contracts',
  schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},
  variable: [{key: 'baseUrl', value: process.env.SEC_API_BASE_URL ?? 'http://127.0.0.1:18087/api/v1'}], item: items}, null, 2));

const cli = process.env.SEC_POSTMAN_CLI ?? path.join(path.dirname(process.execPath), 'node_modules/postman-cli/bin/postman.js');
assert(existsSync(cli), 'Set SEC_POSTMAN_CLI to the installed Postman CLI JavaScript entrypoint');
const result = spawnSync(process.execPath, [cli, 'collection', 'run', file, '--no-report-events'], {
  cwd: root, stdio: 'inherit', env: process.env
});
if (result.error) throw result.error;
process.exit(result.status ?? 1);
