import {expect, test, APIResponse} from '@playwright/test';
const processId = 'd7000000-0000-0000-0000-000000000003';
const indicatorId = '50000000-0000-0000-0000-000000000001';
const committeeAssignment = 'd7000000-0000-0000-0000-000000000911';
const findingsPath = `/api/v1/processes/${processId}/findings`;
const versionsPath = `/api/v1/processes/${processId}/report-versions`;
async function body(response: APIResponse, status = 200) {expect(response.status(), await response.text()).toBe(status);return response.json();}
const finding = {type: 'WEAKNESS', title: 'Matrícula tardía frente a la meta sintética',
  description: 'La matrícula efectiva se rezagó dos puntos frente a la meta declarada del período sintético.',
  indicatorIds: [indicatorId], evaluationIds: [], evidenceVersionIds: []};
const template = {templateReference: 'Plantilla institucional validada v1', templateValidated: true,
  content: 'Informe preliminar sintético: la debilidad de rezago de matrícula se sustenta en la consolidación autorizada y no constituye acreditación.'};

test('AC-X12: preserves finding supports, immutable emission and authorized decision across the P13–P14 chain', async ({page, playwright, baseURL}) => {
  const coordinator = await playwright.request.newContext({baseURL});
  const reviewer = await playwright.request.newContext({baseURL});
  const responsible = await playwright.request.newContext({baseURL});
  const committee = await playwright.request.newContext({baseURL});
  try {
    const sessions = [];
    for (const [context, username] of [[coordinator, 'coordinador'], [reviewer, 'revisor'], [responsible, 'responsable'], [committee, 'presidente']] as const)
      sessions.push(await body(await context.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201));
    const [coordinatorHeaders, reviewerHeaders, responsibleHeaders, committeeHeaders] =
      sessions.map((session: {csrfToken: string}) => ({'X-CSRF-TOKEN': session.csrfToken}));
    const empty = await body(await coordinator.get(`${findingsPath}?page=0&size=100`));
    expect(empty.items).toEqual([]);
    expect(await coordinator.get(findingsPath).then(r => r.headers()['etag'])).toBe('"0"');

    await expect((await responsible.post(findingsPath, {headers: {...responsibleHeaders, 'If-Match': '"0"'}, data: finding})).status()).toBe(404);
    await expect((await responsible.get(findingsPath)).status()).toBe(404);
    await expect((await coordinator.post(findingsPath, {headers: coordinatorHeaders, data: finding})).status()).toBe(428);
    const registered = await body(await coordinator.post(findingsPath, {headers: {...coordinatorHeaders, 'If-Match': '"0"'}, data: finding}), 201);
    expect(registered.data.type).toBe('WEAKNESS');expect(registered.data.indicatorIds).toEqual([indicatorId]);
    const listing = await body(await coordinator.get(findingsPath));
    expect(listing.totalElements).toBe(1);expect(await coordinator.get(findingsPath).then(r => r.headers()['etag'])).toBe('"1"');

    await page.goto('/access/login?returnUrl=/reports');
    await page.locator('#username').fill('coordinador');
    await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
    await page.getByLabel('Proceso', {exact: true}).selectOption(processId);
    await expect(page.locator('article', {hasText: registered.data.title}).first()).toBeVisible();

    await page.getByLabel('Título').fill('Permanencia bajas en plantilla académica sintética');
    await page.getByLabel('Descripción sustentada').fill('La permanencia efectiva cedió un punto respecto del valor consolidado del periodo sintético.');
    await page.getByLabel('Clase').selectOption('OBSERVATION');
    await page.getByRole('checkbox', {name: /^IND-SQL-A/}).check();
    await page.getByRole('button', {name: 'Registrar hallazgo'}).click();
    await expect(page.getByRole('status')).toContainText('Hallazgo registrado');
    const refreshed = await body(await coordinator.get(findingsPath));
    expect(refreshed.totalElements).toBe(2);
    const uiFinding = (refreshed.items as Array<{data: {title: string; type: string; indicatorIds: string[]}}>)
      .find(item => item.data.title.includes('Permanencia bajas'));
    expect(uiFinding?.data.type).toBe('OBSERVATION');expect(uiFinding?.data.indicatorIds).toEqual([indicatorId]);
    const chainIds = (refreshed.items as Array<{id: string}>).map(item => item.id);

    await expect((await coordinator.post(`/api/v1/processes/${processId}/consolidations`, {headers: {...coordinatorHeaders, 'If-Match': '"9"'}, data: {findingIds: chainIds, aggregationRuleId: null, reason: 'Se agrupan por soporte común del indicador sintético.'}})).status()).toBe(412);
    const consolidation = await body(await coordinator.post(`/api/v1/processes/${processId}/consolidations`, {headers: {...coordinatorHeaders, 'If-Match': '"0"'}, data: {findingIds: chainIds, aggregationRuleId: null, reason: 'Se agrupan por soporte común del indicador dual sintético.'}}), 201);
    expect(consolidation.revision).toBe(1);expect(consolidation.data.findingIds).toEqual(chainIds);
    await expect((await coordinator.post(`/api/v1/processes/${processId}/consolidations`, {headers: {...coordinatorHeaders, 'If-Match': '"1"'}, data: {findingIds: chainIds, aggregationRuleId: null, reason: 'Intento duplicado que debe rechazarse por hallazgos ya consolidados.'}})).status()).toBe(409);

    await expect((await coordinator.post(versionsPath, {headers: coordinatorHeaders, data: {kind: 'PRELIMINARY', consolidationId: consolidation.id, ...template, predecessorVersionId: null}})).status()).toBe(428);
    const preliminary = await body(await coordinator.post(versionsPath, {headers: {...coordinatorHeaders, 'If-Match': '"0"'}, data: {kind: 'PRELIMINARY', consolidationId: consolidation.id, ...template, predecessorVersionId: null}}), 201);
    expect(preliminary.data.kind).toBe('PRELIMINARY');expect(preliminary.state).toBe('PRELIMINARY');
    const preliminaryPath = `/api/v1/report-versions/${preliminary.id}`;
    expect(await coordinator.get(preliminaryPath).then(r => r.headers()['etag'])).toBe('"0"');
    const preliminarySnapshot = await body(await coordinator.get(preliminaryPath));
    expect(preliminarySnapshot.data.content).toBe(template.content);expect(preliminarySnapshot.revision).toBe(0);

    await expect((await committee.post(`${preliminaryPath}/socializations`, {headers: {...committeeHeaders, 'If-Match': '"0"'}, data: {occurredAt: '2026-06-20T10:00:00Z', participantAssignmentIds: ['d7000000-0000-0000-0000-000000000801'], recordReference: 'fixture://acta-socializacion'}})).status()).toBe(404);
    const socialization = await body(await coordinator.post(`${preliminaryPath}/socializations`, {headers: {...coordinatorHeaders, 'If-Match': '"0"'}, data: {occurredAt: '2026-06-20T10:00:00Z', participantAssignmentIds: ['d7000000-0000-0000-0000-000000000801', 'd7000000-0000-0000-0000-000000000802'], recordReference: 'fixture://acta-socializacion-v1'}}), 201);
    expect(socialization.data.occurredAt).toBe('2026-06-20T10:00:00Z');
    const observation = await body(await reviewer.post(`${preliminaryPath}/observations`, {headers: {...reviewerHeaders, 'If-Match': '"1"'}, data: {sectionReference: 'Capítulo 3 · Matrícula', text: 'La evidencia del rezago no contrasta la meta institucional declarada en el capítulo.'}}), 201);
    expect(observation.data.sectionReference).toBe('Capítulo 3 · Matrícula');
    const afterObservation = await body(await coordinator.get(preliminaryPath));
    expect(afterObservation.socializations).toHaveLength(1);expect(afterObservation.observations).toHaveLength(1);
    const reply = await body(await coordinator.post(`/api/v1/report-observations/${observation.id}/responses`, {headers: {...coordinatorHeaders, 'If-Match': '"2"'}, data: {response: 'La contrastación se añade al capítulo y la corrección se encadena como versión final.', correctedVersionId: null}}), 201);
    expect(reply.data.response).toContain('corrección');

    const finalVersion = await body(await coordinator.post(versionsPath, {headers: {...coordinatorHeaders, 'If-Match': '"3"'}, data: {kind: 'FINAL', consolidationId: consolidation.id, ...template, predecessorVersionId: preliminary.id, content: 'Informe final sintético: incorpora la contrastación institucional del rezago y conserva la debilidad trazada.'}}), 201);
    expect(finalVersion.data.kind).toBe('FINAL');expect(finalVersion.data.predecessorVersionId).toBe(preliminary.id);expect(finalVersion.revision).toBe(0);
    const finalPath = `/api/v1/report-versions/${finalVersion.id}`;
    const preserved = await body(await coordinator.get(preliminaryPath));
    expect(preserved.data.content).toBe(preliminarySnapshot.data.content);
    expect((preserved.observations as Array<{text: string}>)[0].text).toBe((afterObservation.observations as Array<{text: string}>)[0].text);

    await expect((await coordinator.post(`${finalPath}/approval-decisions`, {headers: {...coordinatorHeaders, 'If-Match': '"0"'}, data: {decision: 'APPROVED', provenance: {authorityAssignmentId: committeeAssignment, legalBasis: 'Competencia retenida del coordinador sintético.', documentReference: 'fixture://acta-autoridad', reason: 'El coordinador no detenta la asignación del comité autorizante.'}}})).status()).toBe(403);
    await page.goto('/access/login?returnUrl=/reports');
    await page.locator('#username').fill('presidente');
    await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
    await page.getByLabel('Proceso', {exact: true}).selectOption(processId);
    const finalRow = page.locator('article', {hasText: 'FINAL'}).first();
    await finalRow.getByRole('button', {name: 'Seleccionar para tramitar'}).click();
    const detailCard = page.locator('article[aria-label="Tramitación de la versión seleccionada"]');
    await expect(detailCard).toContainText('FINAL');
    await detailCard.getByLabel('Decisión').selectOption('APPROVED');
    await detailCard.getByLabel('Asignación de autoridad').fill(committeeAssignment);
    await detailCard.getByLabel('Base legal').fill('Competencia del comité de decisión registrada para la prueba sintética.');
    await detailCard.getByLabel('Referencia documental').fill('fixture://resolucion-informe');
    await detailCard.getByLabel('Justificación').fill('La corrección final incorpora la contrastación exigida y el sustento queda trazable.');
    await detailCard.getByRole('button', {name: 'Registrar decisión'}).click();
    await expect(page.getByRole('status')).toContainText('Decisión institucional registrada');
    const decided = await body(await committee.get(finalPath));
    expect(decided.state).toBe('APPROVED');expect(decided.decisions).toHaveLength(1);
    const decision = (decided.decisions as Array<{decision: string; provenance: {authorityAssignmentId: string; decidedBy: string}}>)[0];
    expect(decision.decision).toBe('APPROVED');expect(decision.provenance.authorityAssignmentId).toBe(committeeAssignment);
    expect(decision.provenance.decidedBy).toBeTruthy();
    await expect((await coordinator.post(versionsPath, {headers: {...coordinatorHeaders, 'If-Match': '"0"'}, data: {kind: 'PRELIMINARY', consolidationId: consolidation.id, ...template, predecessorVersionId: null}})).status()).toBe(409);
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d7-reports/report-approved.png', fullPage: true});
  } finally {
    await coordinator.dispose();await reviewer.dispose();await responsible.dispose();await committee.dispose();
  }
});
