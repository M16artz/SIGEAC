import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';
import {appendFileSync} from 'node:fs';
import assert from 'node:assert/strict';
const require = createRequire(new URL('../frontend/package.json', import.meta.url));
const {request} = require('@playwright/test');
const processId = process.argv[2];
assert.match(processId ?? '', /^[0-9a-f-]{36}$/);
const contexts = [];
try {
  for (const port of [18087, 18088]) {
    const api = await request.newContext({baseURL: `http://127.0.0.1:${port}`}); contexts.push(api);
    const response = await api.post('/api/v1/sessions', {data: {username: 'coordinador', password: 'Synthetic-User-2026'}});
    assert.equal(response.status(), 201);
  }
  const root = `/api/v1/processes/${processId}`;
  const paths = [root, `${root}/closures`, `${root}/report-versions`, `${root}/findings`];
  const closures = await contexts[0].get(paths[1]).then(r => r.json());
  assert.equal(closures.totalElements, 2);
  for (const version of (await contexts[0].get(paths[2]).then(r => r.json())).items)
    paths.push(`/api/v1/report-versions/${version.id}`);
  for (const path of paths) {
    const responses = await Promise.all(contexts.map(api => api.get(path)));
    for (const response of responses) assert.equal(response.status(), 200, path);
    const values = await Promise.all(responses.map(r => r.json()));
    assert.deepEqual(values[1], values[0], `restored API differs: ${path}`);
  }
  const closure = closures.items.find(c => c.type === 'INTEGRAL_CLOSURE');
  assert.match(closure.sha256, /^[0-9a-f]{64}$/);
  let fileEvidence = '';
  if (process.argv[3] && process.argv[4]) {
    for (const api of contexts) {
      assert.equal((await api.post('/api/v1/sessions', {data: {username: 'responsable', password: 'Synthetic-User-2026'}})).status(), 201);
    }
    const responses = await Promise.all(contexts.map(api => api.get(`/api/v1/evidence-versions/${process.argv[3]}/files/${process.argv[4]}/content`)));
    for (const response of responses) assert.equal(response.status(), 200);
    const bytes = await Promise.all(responses.map(r => r.body())); assert.deepEqual(bytes[1], bytes[0]);
    const hash = createHash('sha256').update(bytes[1]).digest('hex');
    assert.equal(hash, responses[1].headers()['x-content-sha256']);
    fileEvidence = ` Descarga autenticada de archivo restaurado: PASS, bytes idénticos y SHA-256 \`${hash}\`.`;
  }
  appendFileSync(new URL('../tests/acceptance/results/p01-p16-restore.md', import.meta.url),
    `\nConsulta posterior desde una segunda instancia Spring Boot en puerto 18088: **PASS**, ${paths.length} respuestas JSON idénticas ` +
    `(proceso cerrado, cierres, hallazgos, informes y tramitación). Hash de cierre conservado: \`${closure.sha256}\`.${fileEvidence}\n`);
  console.log(`PASS: ${paths.length} restored API responses match the original instance.`);
} finally { for (const api of contexts) await api.dispose(); }
