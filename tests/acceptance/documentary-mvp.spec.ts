import {expect, test, APIRequestContext, APIResponse} from '@playwright/test';

const processId = 'd6000000-0000-0000-0000-000000000003';
const sourceId = (index: number) => `d6000000-0000-0000-0000-00000000010${index}`;
const indicatorId = (index: number) => `50000000-0000-0000-0000-00000000000${index <= 2 ? 1 : 2}`;

async function body(response: APIResponse, expected = 200) {
  expect(response.status(), await response.text()).toBe(expected);
  return response.json();
}
async function login(api: APIRequestContext, username: string) {
  return body(await api.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201);
}

test('D6: 75/50, corte histórico, bloqueo de pendientes y cierre documental sin P12-P15', async ({page, playwright, baseURL}) => {
  const collect = await playwright.request.newContext({baseURL});
  const review = await playwright.request.newContext({baseURL});
  const coordinate = await playwright.request.newContext({baseURL});
  const browserRequests: string[] = [];
  page.on('request', request => { if (request.url().includes('/api/v1/')) browserRequests.push(request.url()); });
  try {
    const collector = await login(collect, 'responsable');
    const reviewer = await login(review, 'revisor');
    const coordinator = await login(coordinate, 'coordinador');
    const collectHeaders = {'X-CSRF-TOKEN': collector.csrfToken};
    const reviewHeaders = {'X-CSRF-TOKEN': reviewer.csrfToken};
    const coordinateHeaders = {'X-CSRF-TOKEN': coordinator.csrfToken};
    const versions: string[] = [];
    async function submit(index: number) {
      const version = await body(await collect.post('/api/v1/evidences', {headers: collectHeaders, data: {
        processId, indicatorIds: [indicatorId(index)], title: `Documento sintético D6 fuente ${index}`,
        description: 'Evidencia sintética del MVP documental de cuatro fuentes.', provenance: 'Fixture de software D6, sin validación institucional.',
        coveredFrom: '2026-01-01', coveredTo: '2026-06-30'
      }}), 201);
      const coverage = await collect.post(`/api/v1/evidence-versions/${version.id}/source-coverages`, {
        headers: {...collectHeaders, 'If-Match': '"0"'}, data: {requiredSourceId: sourceId(index), coverageNote: 'Cobertura explícita para la fuente sintética D6.'}
      });
      await body(coverage, 201);
      const reference = await collect.post(`/api/v1/evidence-versions/${version.id}/external-references`, {
        headers: {...collectHeaders, 'If-Match': coverage.headers()['etag']},
        data: {uri: `https://synthetic.invalid/d6/source-${index}`, description: 'Referencia documental sintética, sin afirmar disponibilidad remota.'}
      });
      await body(reference, 201);
      await body(await collect.post(`/api/v1/evidence-versions/${version.id}/submit`, {
        headers: {...collectHeaders, 'If-Match': reference.headers()['etag']}, data: {}
      }));
      versions[index - 1] = version.id;
    }
    async function decide(index: number, outcome: 'ACCEPTED' | 'OBSERVED', revision = 0) {
      await body(await review.post(`/api/v1/evidence-versions/${versions[index - 1]}/document-review`, {
        headers: {...reviewHeaders, 'If-Match': `"${revision}"`}, data: {selfReviewException: false,
          justification: 'Revisión documental sintética para demostrar independencia del MVP.',
          outcomes: [{requiredSourceId: sourceId(index), outcome,
            ...(outcome === 'OBSERVED' ? {note: 'Pendiente documental explícito para bloquear el cierre.'} : {})}]
        }
      }), 201);
    }
    for (let index = 1; index <= 3; index++) { await submit(index); await decide(index, index === 3 ? 'OBSERVED' : 'ACCEPTED'); }
    const progress = await body(await coordinate.get(`/api/v1/processes/${processId}/documentary-progress`));
    expect(progress).toMatchObject({requiredSourceTotal: 4, registeredSourceTotal: 3, acceptedSourceTotal: 2, collectionPercent: 75, documentReviewPercent: 50});
    expect(progress.academicStatus).toBeUndefined();

    await page.goto('/access/login?returnUrl=/reporting');
    await page.locator('#username').fill('coordinador'); await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
    await page.locator('#progress-process').selectOption(processId);
    await expect(page.getByText('75.00 %', {exact: false})).toBeVisible();
    await expect(page.getByText('50.00 %', {exact: false})).toBeVisible();
    await page.getByRole('link', {name: 'Guardar y consultar cortes documentales'}).click();
    await page.locator('#snapshot-process').selectOption(processId);
    await page.locator('#snapshot-reason').fill('Corte sintético de seguimiento con avance 75/50 y pendientes.');
    const savedResponse = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/documentary-snapshots'));
    await page.getByRole('button', {name: 'Guardar corte actual'}).click();
    const saved = await savedResponse; expect(saved.status(), await saved.text()).toBe(201);
    const snapshot = await saved.json();
    const before = await body(await coordinate.get(`/api/v1/processes/${processId}`));
    const blocked = await coordinate.post(`/api/v1/processes/${processId}/documentary-close`, {
      headers: {...coordinateHeaders, 'If-Match': `"${before.revision}"`},
      data: {closurePolicyVersion: 'DOCUMENTARY_STRICT_V1', reason: 'Intento de cierre sintético con fuentes pendientes.'}
    });
    expect((await body(blocked, 409)).code).toBe('CLOSURE_BLOCKED');
    expect((await body(await coordinate.get(`/api/v1/processes/${processId}`))).revision).toBe(before.revision);
    expect((await body(await coordinate.get(`/api/v1/processes/${processId}/documentary-snapshots`))).items).toHaveLength(1);
    await decide(3, 'ACCEPTED', 1); await submit(4); await decide(4, 'ACCEPTED');

    await page.goto('/reporting/closure'); await page.locator('#closure-process').selectOption(processId);
    await page.locator('#closure-reason').fill('Cierre documental sintético con cuatro fuentes aceptadas e integridad verificada.');
    const closedResponse = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/documentary-close'));
    await page.getByRole('button', {name: 'Finalizar fase documental'}).click();
    const closed = await closedResponse; expect(closed.status(), await closed.text()).toBe(201);
    const closure = await closed.json(); expect(closure.data.state).toBe('DOCUMENTARY_CLOSED');
    await expect(page.getByRole('heading', {name: 'Fase documental cerrada', exact: true})).toBeVisible();
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d6/documentary-closure.png', fullPage: true});
    expect((await body(await coordinate.get(`/api/v1/documentary-closures/${closure.data.id}`))).sha256).toBe(closure.sha256);
    const historical = await body(await coordinate.get(`/api/v1/documentary-snapshots/${snapshot.header.id}`));
    expect(historical.header.sha256).toBe(snapshot.header.sha256);
    expect(historical.data.progress).toMatchObject({requiredSourceTotal: 4, registeredSourceTotal: 3, acceptedSourceTotal: 2});
    await page.goto(`/reporting/history?snapshotId=${snapshot.header.id}`);
    await expect(page.getByText('Recopilación: 75.00 %', {exact: false})).toBeVisible();
    await expect(page.getByText('Revisión documental: 50.00 %', {exact: false})).toBeVisible();
    const late = await collect.post('/api/v1/evidences', {headers: collectHeaders, data: {processId, indicatorIds: [indicatorId(1)],
      title: 'Borrador tardío sintético', description: 'Debe bloquearse tras el cierre.', provenance: 'Fixture D6', coveredFrom: '2026-01-01', coveredTo: '2026-06-30'}});
    expect((await body(late, 409)).code).toBe('PROCESS_NOT_ACTIVE');
    const lateReview = await review.post(`/api/v1/evidence-versions/${versions[0]}/document-review`, {
      headers: {...reviewHeaders, 'If-Match': '"1"'}, data: {selfReviewException: false,
        justification: 'Revisión tardía bloqueada después del cierre documental.', outcomes: [{requiredSourceId: sourceId(1), outcome: 'ACCEPTED'}]}});
    expect((await body(lateReview, 409)).code).toBe('PROCESS_NOT_ACTIVE');
    expect((await collect.get(`/api/v1/documentary-snapshots/${snapshot.header.id}`)).status()).toBe(403);
    expect(browserRequests.some(url => /\/(academic-evaluations|variable-values|findings|reports|improvement)(\/|\?|$)/.test(url))).toBe(false);
  } finally { await collect.dispose(); await review.dispose(); await coordinate.dispose(); }
});
