import {expect, test, APIResponse} from '@playwright/test';
const processId = 'd7000000-0000-0000-0000-000000000003';
const indicatorId = '50000000-0000-0000-0000-000000000002';
const sourceId = 'd7000000-0000-0000-0000-000000000103';
const path = `/api/v1/processes/${processId}/indicators/${indicatorId}`;
async function body(response: APIResponse, status = 200) {expect(response.status(), await response.text()).toBe(status); return response.json();}

test('verified quantitative calculation preserves 75 and 50 with their original inputs and method', async ({page, playwright, baseURL}) => {
  const collect = await playwright.request.newContext({baseURL}); const review = await playwright.request.newContext({baseURL});
  const evaluate = await playwright.request.newContext({baseURL});
  try {
    const sessions = [];
    for (const [api, username] of [[collect, 'responsable'], [review, 'revisor'], [evaluate, 'evaluador']] as const)
      sessions.push(await body(await api.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201));
    const collectHeaders = {'X-CSRF-TOKEN': sessions[0].csrfToken}; const reviewHeaders = {'X-CSRF-TOKEN': sessions[1].csrfToken};
    const evaluateHeaders = {'X-CSRF-TOKEN': sessions[2].csrfToken};
    const version = await body(await collect.post('/api/v1/evidences', {headers: collectHeaders, data: {processId, indicatorIds: [indicatorId],
      title: 'Cifras sintéticas para razón porcentual', description: 'Sustento sintético de cantidades A y B.', provenance: 'Prueba de software sin validez institucional.',
      coveredFrom: '2026-01-01', coveredTo: '2026-06-30'}}), 201);
    const coverage = await collect.post(`/api/v1/evidence-versions/${version.id}/source-coverages`, {headers: {...collectHeaders, 'If-Match': '"0"'},
      data: {requiredSourceId: sourceId, coverageNote: 'Fuente sintética con cifras A y B.'}}); await body(coverage, 201);
    const reference = await collect.post(`/api/v1/evidence-versions/${version.id}/external-references`, {headers: {...collectHeaders, 'If-Match': coverage.headers()['etag']},
      data: {uri: 'https://synthetic.invalid/d7/quantitative', description: 'Referencia sintética de datos.'}}); await body(reference, 201);
    await body(await collect.post(`/api/v1/evidence-versions/${version.id}/submit`, {headers: {...collectHeaders, 'If-Match': reference.headers()['etag']}, data: {}}));
    await body(await review.post(`/api/v1/evidence-versions/${version.id}/document-review`, {headers: {...reviewHeaders, 'If-Match': '"0"'},
      data: {selfReviewException: false, justification: 'Aceptación documental sintética para cifras verificables.', outcomes: [{requiredSourceId: sourceId, outcome: 'ACCEPTED'}]}}), 201);
    expect((await collect.get(`${path}/quantitative-evaluations`)).status()).toBe(403);
    const initial = await body(await evaluate.get(`${path}/quantitative-evaluations`)); expect(initial.items).toHaveLength(0);
    expect(initial.rule.strategyKey).toBe('ratio-percent');

    await page.goto('/access/login?returnUrl=/evaluations/quantitative');
    await page.locator('#username').fill('evaluador'); await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
    await page.getByLabel('Proceso', {exact: true}).selectOption(processId); await page.getByLabel('Indicador', {exact: true}).selectOption(indicatorId);
    await page.getByLabel('Valor A', {exact: true}).fill('3'); await page.getByLabel('Valor B', {exact: true}).fill('4');
    await page.getByLabel('Fin del período de variables').fill('2026-06-30');
    await page.locator('fieldset').nth(0).getByRole('checkbox').check(); await page.locator('fieldset').nth(1).getByRole('checkbox').check();
    const captured = page.waitForResponse(r => r.url().endsWith('/quantitative-inputs') && r.request().method() === 'POST');
    await page.getByRole('button', {name: 'Conservar nueva revisión de variables'}).click(); const first = await body(await captured, 201);
    await expect(page.getByText('Variables conservadas · revisión 1')).toBeVisible();
    const calculateCommand = {inputSetId: first.id, justification: 'Resultado cuantitativo sintético sustentado.', limitations: 'Limitado al alcance del piloto sintético.'};
    expect((await evaluate.post(`${path}/quantitative-calculations`, {headers: {...evaluateHeaders, 'If-Match': '"1"'}, data: calculateCommand})).status()).toBe(422);
    await page.getByLabel('Fundamento de la verificación').fill('Cifras, unidades, período y sustentos contrastados explícitamente.');
    await page.getByRole('button', {name: 'Verificar cifras, fuentes y período'}).click();
    await page.getByLabel('Justificación del resultado').fill(calculateCommand.justification); await page.getByLabel('Limitaciones del cálculo').fill(calculateCommand.limitations);
    const calculated = page.waitForResponse(r => r.url().endsWith('/quantitative-calculations') && r.request().method() === 'POST');
    await page.getByRole('button', {name: 'Calcular y conservar resultado'}).click(); expect((await calculated).status()).toBe(201);
    await expect(page.getByRole('heading', {name: /Resultado: 75 %/})).toBeVisible();
    const firstRun = (await body(await evaluate.get(`${path}/quantitative-evaluations`))).items.find((r: {inputs: {id: string}}) => r.inputs.id === first.id);
    expect(firstRun.result).toBe('75'); expect(firstRun.inputs).toEqual(first); expect(firstRun.rule.formulaDefinition).toBe('A / B * 100');
    await expect(page.getByRole('heading', {name: /Resultado: 75 %/})).toBeVisible();
    await page.getByLabel('Valor A', {exact: true}).fill('2');
    const corrected = page.waitForResponse(r => r.url().endsWith('/quantitative-inputs') && r.request().method() === 'POST');
    await page.getByRole('button', {name: 'Conservar nueva revisión de variables'}).click(); const second = await body(await corrected, 201);
    expect(second.predecessorId).toBe(first.id); expect(second.revision).toBe(2);
    expect((await evaluate.post(`${path}/quantitative-inputs/${first.id}/verification`, {headers: {...evaluateHeaders, 'If-Match': '"1"'}, data: {reason: 'Intento de verificar una revisión obsoleta.'}})).status()).toBe(412);
    await page.getByLabel('Fundamento de la verificación').fill('Segunda revisión de cifras y sustentos contrastados.');
    await page.getByRole('button', {name: 'Verificar cifras, fuentes y período'}).click();
    const secondCalculated = page.waitForResponse(r => r.url().endsWith('/quantitative-calculations') && r.request().method() === 'POST');
    await page.getByRole('button', {name: 'Calcular y conservar resultado'}).click(); expect((await secondCalculated).status()).toBe(201);
    await expect(page.getByRole('heading', {name: /Resultado: 50 %/})).toBeVisible();
    const secondRun = (await body(await evaluate.get(`${path}/quantitative-evaluations`))).items.find((r: {inputs: {id: string}}) => r.inputs.id === second.id);
    expect(secondRun.result).toBe('50');
    await expect(page.getByRole('heading', {name: /Resultado: 50 %/})).toBeVisible();
    await expect(page.getByRole('heading', {name: /Resultado: 75 %/})).toBeVisible();
    const history = await body(await evaluate.get(`${path}/quantitative-evaluations`));
    expect(history.items.find((r: {id: string}) => r.id === firstRun.id)).toEqual(firstRun);
    expect(await body(await evaluate.get(`${path}/quantitative-inputs/${first.id}`))).toEqual(first);
    expect((await evaluate.post(`${path}/quantitative-calculations`, {headers: {...evaluateHeaders, 'If-Match': '"2"'}, data: {...calculateCommand, inputSetId: second.id}})).status()).toBe(409);
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d7-quantitative/quantitative-evaluation.png', fullPage: true});
  } finally {await collect.dispose(); await review.dispose(); await evaluate.dispose();}
});
