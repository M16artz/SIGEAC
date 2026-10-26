import {spawnSync} from 'node:child_process';
import {readFileSync, writeFileSync, mkdirSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const frontend = path.join(root, 'frontend');
const report = path.join(root, 'backend/target/complete-workflows-playwright.json');
mkdirSync(path.dirname(report), {recursive: true});
writeFileSync(report, '');
const result = spawnSync(process.execPath, [path.join(frontend, 'node_modules/@playwright/test/cli.js'),
  'test', '--config=playwright.complete-workflows.config.ts', ...process.argv.slice(2)], {
  cwd: frontend, stdio: 'inherit', env: {...process.env,
    NODE_PATH: [path.join(frontend, 'node_modules'), process.env.NODE_PATH].filter(Boolean).join(path.delimiter)}
});
if (result.error) throw result.error;
const run = JSON.parse(readFileSync(report, 'utf8'));
function specs(suites) { return suites.flatMap(s => [...s.specs, ...specs(s.suites ?? [])]); }
const cases = specs(run.suites).map(spec => {
  const test = spec.tests[0];
  const last = test.results.at(-1);
  return {id: spec.title, status: last?.status === 'passed' ? 'PASS' : last?.status === 'skipped' ? 'SKIPPED' : 'FAIL',
    reason: test.annotations?.find(a => a.type === 'skip')?.description ?? '', duration: last?.duration ?? 0};
});
assert.equal(cases.length, 64, 'The campaign must retain all 48 process and 16 transversal criteria');
assert.equal(new Set(cases.map(c => c.id)).size, 64, 'Scenario IDs must be unique');
const counts = Object.fromEntries(['PASS', 'FAIL', 'SKIPPED'].map(status => [status, cases.filter(c => c.status === status).length]));
const rows = cases.map(c => `| ${c.id} | ${c.status} | ${c.status === 'SKIPPED' ? c.reason : `${c.duration} ms; API real, sin mocks`} |`).join('\n');
writeFileSync(path.join(root, 'tests/acceptance/results/process-acceptance.md'), `# Campaña consolidada P01–P16\n\nFecha UTC: ${new Date().toISOString()}. Datos sintéticos; PostgreSQL local.\n\n` +
  `Resultado: **${counts.PASS} PASS, ${counts.FAIL} FAIL, ${counts.SKIPPED} SKIPPED**.\n\n` +
  'Comando: `npm run test:complete-workflows` desde `frontend`, con backend preparado como en `tests/performance/README.md`.\n\n' +
  'Cada PASS ejecuta el recorrido de `tests/acceptance/complete-workflows.spec.ts`; no deriva de nombres de tareas ni de resultados anteriores. ' +
  'SKIPPED conserva trabajo que aún debe incorporarse a esta campaña, aunque existan pruebas por entrega. ' +
  'Esta ejecución cubre los criterios técnicos con fixtures sintéticos; la aprobación institucional corresponde a T111. ' +
  'El quickstart y la restauración tienen evidencias separadas. El JSON detallado se genera en `backend/target/complete-workflows-playwright.json` y no se versiona.\n\n' +
  '| Escenario | Resultado | Evidencia o motivo |\n|---|---|---|\n' + rows + '\n');
console.log(JSON.stringify(counts));
process.exit(result.status ?? 1);
