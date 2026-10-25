import {expect, test, APIRequestContext, APIResponse} from '@playwright/test';

const processId = 'd7000000-0000-0000-0000-000000000003';
const indicatorId = '50000000-0000-0000-0000-000000000001';
const sourceId = 'd7000000-0000-0000-0000-000000000101';
const optionId = 'd7000000-0000-0000-0000-000000000011';
const path = `/api/v1/processes/${processId}/indicators/${indicatorId}/manual-academic-evaluations`;
async function body(response: APIResponse, expected = 200) {
  expect(response.status(), await response.text()).toBe(expected); return response.json();
}
async function login(api: APIRequestContext, username: string) {
  return body(await api.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201);
}

test('manual academic judgement preserves its method, support decisions and predecessor', async ({page, playwright, baseURL}) => {
  const collect = await playwright.request.newContext({baseURL});
  const review = await playwright.request.newContext({baseURL});
  const evaluate = await playwright.request.newContext({baseURL});
  try {
    const collector = await login(collect, 'responsable'); const reviewer = await login(review, 'revisor');
    const evaluator = await login(evaluate, 'evaluador');
    const collectHeaders = {'X-CSRF-TOKEN': collector.csrfToken}; const reviewHeaders = {'X-CSRF-TOKEN': reviewer.csrfToken};
    const version = await body(await collect.post('/api/v1/evidences', {headers: collectHeaders, data: {
      processId, indicatorIds: [indicatorId], title: 'Sustento sintético D7 manual', description: 'Evidencia sintética de valoración cualitativa.',
      provenance: 'Prueba de software sin validación institucional.', coveredFrom: '2026-01-01', coveredTo: '2026-06-30'
    }}), 201);
    await body(await collect.post(`/api/v1/evidence-versions/${version.id}/source-coverages`, {
      headers: {...collectHeaders, 'If-Match': '"0"'}, data: {requiredSourceId: sourceId, coverageNote: 'Cobertura sintética de la fuente requerida.'}
    }), 201);
    const draft = await collect.get(`/api/v1/evidence-versions/${version.id}`);
    await body(await collect.post(`/api/v1/evidence-versions/${version.id}/external-references`, {
      headers: {...collectHeaders, 'If-Match': draft.headers()['etag']},
      data: {uri: 'https://example.org/d7-manual/sustento', description: 'Referencia documental sintética'}
    }), 201);
    const ready = await collect.get(`/api/v1/evidence-versions/${version.id}`);
    await body(await collect.post(`/api/v1/evidence-versions/${version.id}/submit`, {headers: {...collectHeaders, 'If-Match': ready.headers()['etag']}, data: {}}), 200);
    const command = {ratingOptionId: optionId, evidenceVersionIds: [version.id], justification: 'Juicio cualitativo sintético basado en sustento aceptado.',
      limitations: 'Limitado al alcance de esta prueba sintética de software.', predecessorId: null};
    expect((await evaluate.post(path, {headers: {'X-CSRF-TOKEN': evaluator.csrfToken, 'If-Match': '"0"'}, data: command})).status()).toBe(422);
    await body(await review.post(`/api/v1/evidence-versions/${version.id}/document-review`, {
      headers: {...reviewHeaders, 'If-Match': '"0"'}, data: {justification: 'Suficiencia documental sintética verificada.',
        selfReviewException: false, outcomes: [{requiredSourceId: sourceId, outcome: 'ACCEPTED', note: 'Fuente documental suficiente.'}]}
    }), 201);
    expect((await collect.get(path)).status()).toBe(403);

    await page.goto('/access/login?returnUrl=/evaluations');
    await page.locator('#username').fill('evaluador'); await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button', {name: 'Ingresar'}).click();
    await expect(page).not.toHaveURL(/login/);
    await page.goto('/evaluations');
    await page.getByLabel('Proceso', {exact: true}).selectOption(processId);
    await page.getByLabel('Indicador', {exact: true}).selectOption(indicatorId);
    await expect(page.getByText('Método de la versión publicada:')).toContainText('Juicio cualitativo sintético');
    await page.getByLabel('Resultado según escala del indicador').selectOption(optionId);
    await page.getByRole('checkbox').check();
    await page.getByLabel('Justificación', {exact: true}).fill(command.justification);
    await page.getByLabel('Limitaciones y método aplicado').fill(command.limitations);
    const firstSaved = page.waitForResponse(response => response.url().endsWith('/manual-academic-evaluations') && response.request().method() === 'POST');
    await page.getByRole('button', {name: 'Guardar valoración manual'}).click();
    const first = await body(await firstSaved, 201);
    await expect(page.getByRole('status')).toContainText('Valoración conservada');
    await expect(page.getByText('Revisión propia: 1.')).toBeVisible();
    await page.getByLabel('Justificación', {exact: true}).fill('Segunda valoración sintética con justificación ampliada.');
    const corrected = page.waitForResponse(response => response.url().endsWith('/manual-academic-evaluations') && response.request().method() === 'POST');
    await page.getByRole('button', {name: 'Guardar valoración manual'}).click();
    const second = await body(await corrected, 201);
    expect(second.predecessorId).toBe(first.id); expect(second.revision).toBe(2);
    await expect(page.locator('article')).toHaveCount(2);
    const history = await body(await evaluate.get(path));
    expect(history.items.find((item: {id: string}) => item.id === first.id)).toEqual(first);
    expect(first.supports[0].evidenceVersionId).toBe(version.id);
    expect(first.supports[0].reviewDecisionId).toBeTruthy();
    expect((await evaluate.post(path, {headers: {'X-CSRF-TOKEN': evaluator.csrfToken, 'If-Match': '"0"'}, data: command})).status()).toBe(412);
    expect((await evaluate.post(`/api/v1/academic-evaluations/indicators/${indicatorId}/calculate?strategy=VALIDATED`, {
      headers: {'X-CSRF-TOKEN': evaluator.csrfToken}
    })).status()).toBe(501);
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d7-manual/manual-evaluation.png', fullPage: true});
  } finally { await collect.dispose(); await review.dispose(); await evaluate.dispose(); }
});
