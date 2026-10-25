import {expect, test, APIResponse} from '@playwright/test';
const processId = 'd7000000-0000-0000-0000-000000000003';
const indicatorId = '50000000-0000-0000-0000-000000000001';
const sourceId = 'd7000000-0000-0000-0000-000000000101';
const assignment = 'd7000000-0000-0000-0000-000000000901';
const path = `/api/v1/processes/${processId}/indicators/${indicatorId}`;
async function body(response: APIResponse, status = 200) {expect(response.status(), await response.text()).toBe(status);return response.json();}

test('AC-X11: preserves two foundations, competent resolution and invalidated support without overwriting history', async ({page, playwright, baseURL}) => {
  const collect = await playwright.request.newContext({baseURL});const review = await playwright.request.newContext({baseURL});
  const one = await playwright.request.newContext({baseURL});const two = await playwright.request.newContext({baseURL});const committee = await playwright.request.newContext({baseURL});
  try {
    const sessions = [];
    for (const [api, username] of [[collect, 'responsable'], [review, 'revisor'], [one, 'evaluador'], [two, 'evaluador2'], [committee, 'conciliador']] as const)
      sessions.push(await body(await api.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201));
    const collectHeaders = {'X-CSRF-TOKEN': sessions[0].csrfToken};const reviewHeaders = {'X-CSRF-TOKEN': sessions[1].csrfToken};
    const oneHeaders = {'X-CSRF-TOKEN': sessions[2].csrfToken};const twoHeaders = {'X-CSRF-TOKEN': sessions[3].csrfToken};const committeeHeaders = {'X-CSRF-TOKEN': sessions[4].csrfToken};
    const evidence = await body(await collect.post('/api/v1/evidences', {headers: collectHeaders, data: {processId, indicatorIds: [indicatorId], title: 'Sustento sintético de discrepancia',
      description: 'Evidencia sintética compartida por dos juicios distintos.', provenance: 'Prueba de software sin validación institucional.', coveredFrom: '2026-01-01', coveredTo: '2026-06-30'}}), 201);
    const coverage = await collect.post(`/api/v1/evidence-versions/${evidence.id}/source-coverages`, {headers: {...collectHeaders, 'If-Match': '"0"'}, data: {requiredSourceId: sourceId, coverageNote: 'Cobertura sintética para discrepancia.'}});await body(coverage, 201);
    const reference = await collect.post(`/api/v1/evidence-versions/${evidence.id}/external-references`, {headers: {...collectHeaders, 'If-Match': coverage.headers()['etag']}, data: {uri: 'https://synthetic.invalid/reconciliation', description: 'Referencia documental sintética.'}});await body(reference, 201);
    await body(await collect.post(`/api/v1/evidence-versions/${evidence.id}/submit`, {headers: {...collectHeaders, 'If-Match': reference.headers()['etag']}, data: {}}));
    await body(await review.post(`/api/v1/evidence-versions/${evidence.id}/document-review`, {headers: {...reviewHeaders, 'If-Match': '"0"'}, data: {selfReviewException: false,
      justification: 'Aceptación documental sintética para evaluar discrepancias.', outcomes: [{requiredSourceId: sourceId, outcome: 'ACCEPTED'}]}}), 201);
    const own = await body(await one.get(`${path}/manual-academic-evaluations`));
    const first = await body(await one.post(`${path}/manual-academic-evaluations`, {headers: {...oneHeaders, 'If-Match': `"${own.ownRevision}"`}, data: {
      ratingOptionId: 'd7000000-0000-0000-0000-000000000011', evidenceVersionIds: [evidence.id], justification: 'Primer fundamento sintético: suficiencia académica declarada.', limitations: 'Limitaciones del primer evaluador sintético.', predecessorId: own.ownLatestId}}), 201);
    const secondCommand = {ratingOptionId: 'd7000000-0000-0000-0000-000000000012', evidenceVersionIds: [evidence.id], justification: 'Segundo fundamento sintético: satisfacción parcial declarada.', limitations: 'Limitaciones del segundo evaluador sintético.', predecessorId: null};
    const second = await body(await two.post(`${path}/manual-academic-evaluations`, {headers: {...twoHeaders, 'If-Match': '"0"'}, data: secondCommand}), 201);
    const disagreement = await body(await two.post(`${path}/academic-disagreements`, {headers: {...twoHeaders, 'If-Match': '"0"'}, data: {reason: 'Discrepancia explícita entre ambos fundamentos académicos sintéticos.'}}), 201);
    const policyCommand = {procedureReference: 'Procedimiento sintético de conciliación, versión 1', method: 'SELECT_EXISTING_JUDGEMENT_V1', legalBasis: 'Competencia sintética registrada para esta prueba de software.', documentReference: 'fixture://academic-reconciliation-procedure', authorityAssignmentId: assignment};
    expect((await one.post(`${path}/academic-reconciliation-policies`, {headers: {...oneHeaders, 'If-Match': '"0"'}, data: policyCommand})).status()).toBe(403);
    expect((await collect.get(`${path}/academic-reconciliation`)).status()).toBe(403);
    await page.goto('/access/login?returnUrl=/evaluations/reconciliation');await page.locator('#username').fill('conciliador');await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
    await page.getByLabel('Proceso', {exact: true}).selectOption(processId);await page.getByLabel('Indicador', {exact: true}).selectOption(indicatorId);
    await expect(page.getByRole('heading', {name: 'Estado: Pendiente de conciliación'})).toBeVisible();
    await expect(page.getByText(first.justification, {exact: true}).first()).toBeVisible();await expect(page.getByText(second.justification, {exact: true}).first()).toBeVisible();
    await page.getByLabel('Asignación autorizadora').selectOption(assignment);
    await page.getByText('Registrar o versionar procedimiento de conciliación', {exact: true}).click();
    await page.getByLabel('Referencia y versión del procedimiento').fill(policyCommand.procedureReference);await page.getByLabel('Fundamento de competencia').fill(policyCommand.legalBasis);
    await page.getByLabel('Constancia documental').fill(policyCommand.documentReference);
    await page.getByRole('button', {name: 'Conservar procedimiento autorizado'}).click();
    await expect(page.getByRole('status')).toContainText('Procedimiento autorizado conservado');
    const configured = await body(await committee.get(`${path}/academic-reconciliation`));
    await page.getByLabel('Juicio existente seleccionado').selectOption(first.id);
    for (const checkbox of await page.getByRole('checkbox').all()) await checkbox.check();
    const reason = 'Resolución sintética: se selecciona el primer juicio conforme al procedimiento registrado.';
    await page.getByLabel('Fundamento de la resolución').fill(reason);
    await page.getByRole('button', {name: 'Conservar resolución autorizada'}).click();
    await expect(page.getByRole('heading', {name: 'Estado: Resolución aplicable'})).toBeVisible();
    const resolved = await body(await committee.get(`${path}/academic-reconciliation`));
    const resolution = resolved.resolution;expect(resolution.selectedJudgement.id).toBe(first.id);expect(resolution.participants).toHaveLength(3);
    expect((await body(await one.get(`${path}/manual-academic-evaluations`))).items.find((j: {id: string}) => j.id === first.id)).toEqual(first);
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d7-reconciliation/reconciliation-resolved.png', fullPage: true});
    const corrected = await body(await two.post(`${path}/manual-academic-evaluations`, {headers: {...twoHeaders, 'If-Match': '"1"'}, data: {...secondCommand,
      ratingOptionId: 'd7000000-0000-0000-0000-000000000011', justification: 'Segundo evaluador amplía su fundamento en una revisión posterior.', predecessorId: second.id}}), 201);
    const pending = await body(await committee.get(`${path}/academic-reconciliation`));expect(pending.status).toBe('PENDING_RECONCILIATION');expect(pending.result).toBeNull();
    expect(pending.resolution).toEqual(resolution);
    const next = await body(await two.post(`${path}/academic-disagreements`, {headers: {...twoHeaders, 'If-Match': '"1"'}, data: {reason: 'Nueva discrepancia tras la revisión del segundo evaluador.'}}), 201);
    await body(await review.post(`/api/v1/evidence-versions/${evidence.id}/document-review`, {headers: {...reviewHeaders, 'If-Match': '"1"'}, data: {selfReviewException: false,
      justification: 'Rectificación sintética que exige revisar los sustentos.', outcomes: [{requiredSourceId: sourceId, outcome: 'OBSERVED', note: 'Fuente pendiente después de la rectificación sintética.'}]}}), 201);
    const changed = await body(await committee.get(`${path}/academic-reconciliation`));expect(changed.status).toBe('REQUIRES_REVIEW');expect(changed.result).toBeNull();
    expect(changed.current.every((j: {flags: string[]}) => j.flags.includes('SUPPORTS_INVALIDATED'))).toBe(true);
    expect(changed.cases.find((item: {disagreement: {id: string}}) => item.disagreement.id === disagreement.id).resolution).toEqual(resolution);
    expect((await body(await two.get(`${path}/manual-academic-evaluations`))).items.find((j: {id: string}) => j.id === second.id)).toEqual(second);
    expect(corrected.predecessorId).toBe(second.id);
    const resolutionCommand = {policyId: configured.policy.id, selectedJudgementId: first.id, participants: resolution.participants, reason, authorityAssignmentId: assignment};
    expect((await committee.post(`${path}/academic-disagreements/${next.id}/resolution`, {headers: {...committeeHeaders, 'If-Match': '"2"'}, data: resolutionCommand})).status()).toBe(409);
    expect((await committee.post(`${path}/academic-disagreements/${next.id}/resolution`, {headers: {...committeeHeaders, 'If-Match': '"1"'}, data: resolutionCommand})).status()).toBe(412);
    await page.getByRole('button', {name: 'Recargar conciliación'}).click();await expect(page.getByRole('heading', {name: /Estado: Requiere revisión/})).toBeVisible();
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d7-reconciliation/reconciliation-review.png', fullPage: true});
  } finally {await collect.dispose();await review.dispose();await one.dispose();await two.dispose();await committee.dispose();}
});
