import {expect, test, APIResponse, Page} from '@playwright/test';
const processId = 'd7000000-0000-0000-0000-000000000003';
const findingId = 'd8000000-0000-0000-0000-000000000200';
const reportVersionId = 'd8000000-0000-0000-0000-000000000100';
const responsibleAssignment = 'd8000000-0000-0000-0000-000000000901';
const reviewerAssignment = 'd8000000-0000-0000-0000-000000000902';
const coordinatorAssignment = 'd8000000-0000-0000-0000-000000000903';
const actionsPath = `/api/v1/processes/${processId}/improvement-actions`;
const findingTitle = 'Brecha sintética de seguimiento académico';
async function body(response: APIResponse, status = 200) {expect(response.status(), await response.text()).toBe(status);return response.json();}
async function login(page: Page, username: string) {
  await page.goto('/access/login?returnUrl=/improvement');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill('Synthetic-User-2026');
  await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
}
const decision = (action: string, assignment: string, reason: string) => ({action, provenance: {
  authorityAssignmentId: assignment, legalBasis: 'Competencia sintética registrada para la prueba P15.',
  documentReference: 'fixture://acta-mejora-continua', reason}});

test('AC-P15-01/02/03: improves, verifies, rejects premature closure and preserves overdue revisions', async ({page, playwright, baseURL}) => {
  const coordinator = await playwright.request.newContext({baseURL});
  const responsible = await playwright.request.newContext({baseURL});
  const reviewer = await playwright.request.newContext({baseURL});
  const titles = {verified: 'Cierre de brecha de seguimiento académico', overdue: 'Acción con plazo vencido sin verificación'};
  try {
    const sessions = [];
    for (const [context, username] of [[coordinator, 'coordinador'], [responsible, 'responsable'], [reviewer, 'revisor']] as const)
      sessions.push(await body(await context.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201));
    const [coordinatorHeaders, responsibleHeaders, reviewerHeaders] =
      sessions.map((session: {csrfToken: string}) => ({'X-CSRF-TOKEN': session.csrfToken}));

    await login(page, 'coordinador');
    await page.getByLabel('Proceso', {exact: true}).selectOption(processId);
    await expect(page.locator(`#plan-report option[value="${reportVersionId}"]`)).toHaveCount(1);
    await page.getByLabel('Título del plan').fill('Plan de mejora sintético P15');
    await page.getByLabel('Versión de informe emitida').selectOption(reportVersionId);
    await page.getByLabel('Revisión previa de la cadena de planes').fill('0');
    await page.getByRole('button', {name: 'Crear plan'}).click();
    await expect(page.getByText('Plan de mejora creado')).toBeVisible();

    await page.getByRole('checkbox', {name: new RegExp(findingTitle.slice(0, 20))}).check();
    await page.getByLabel('Título', {exact: true}).fill(titles.verified);
    await page.getByLabel('Descripción', {exact: true}).fill('Acción sintética que reduce la brecha con evidencia verificable.');
    await page.getByLabel('Asignación del responsable').fill(responsibleAssignment);
    await page.getByLabel('Plazo').fill('2027-01-15T10:00');
    await page.getByLabel('Meta medible').fill('Reducir la brecha en al menos 50% medible.');
    await page.getByLabel('Medio de verificación').fill('Revisión documental y muestreo aleatorio.');
    await page.getByRole('button', {name: 'Proponer acción'}).click();
    await expect(page.getByRole('status')).toContainText('Acción propuesta');
    await expect(page.locator('article', {hasText: titles.verified}).first()).toContainText('Propuesta');

    const listed = await body(await coordinator.get(actionsPath)) as
      {items: Array<{id: string; revision: number; data: {title: string; findingIds: string[]; planId: string}}>};
    const created = listed.items.find(item => item.data.title === titles.verified);
    expect(created?.data.findingIds).toEqual([findingId]);
    const actionPath = `/api/v1/improvement-actions/${created!.id}`;

    await body(await coordinator.post(`${actionPath}/decisions`, {headers: {...coordinatorHeaders, 'If-Match': '"1"'}, data: decision('APPROVE', coordinatorAssignment, 'La acción cumple meta, plazo y medio de verificación.')}), 201);
    await expect((await reviewer.post(`${actionPath}/decisions`, {headers: {...reviewerHeaders, 'If-Match': '"2"'}, data: decision('VERIFY', reviewerAssignment, 'La acción aún no ha iniciado ejecución ni presenta avances.')})).status()).toBe(409);

    await body(await responsible.post(`${actionPath}/decisions`, {headers: {...responsibleHeaders, 'If-Match': '"2"'}, data: decision('START', responsibleAssignment, 'Inicia la ejecución autorizada de la acción aprobada.')}), 201);
    const evidence = 'd8000000-0000-0000-0000-000000000301';
    await body(await responsible.post(`${actionPath}/progress`, {headers: {...responsibleHeaders, 'If-Match': '"3"'}, data: {
      description: 'Avance sintético con soporte documental verificable del responsable.', achievedValue: '40% de reducción',
      evidenceVersionIds: [evidence], recordedAt: '2026-10-20T10:00:00Z'}}), 201);
    await body(await responsible.post(`${actionPath}/decisions`, {headers: {...responsibleHeaders, 'If-Match': '"4"'}, data: decision('SUBMIT_VERIFICATION', responsibleAssignment, 'El avance alcanza la meta declarada y se somete a verificación.')}), 201);
    await body(await reviewer.post(`${actionPath}/decisions`, {headers: {...reviewerHeaders, 'If-Match': '"5"'}, data: decision('VERIFY', reviewerAssignment, 'La evidencia y el avance acreditan el cumplimiento declarado.')}), 201);

    const detail = await body(await reviewer.get(actionPath));
    expect(detail.state).toBe('VERIFIED');expect(detail.overdue).toBe(false);
    expect(detail.progress).toHaveLength(1);expect(detail.decisions).toHaveLength(4);
    expect(detail.decisions.map((entry: {action: string}) => entry.action))
      .toEqual(['APPROVE', 'START', 'SUBMIT_VERIFICATION', 'VERIFY']);
    await expect((await responsible.post(`${actionPath}/progress`, {headers: {...responsibleHeaders, 'If-Match': '"6"'}, data: {
      description: 'Avance posterior al cierre que debe rechazarse.', achievedValue: '60%',
      evidenceVersionIds: [evidence], recordedAt: '2026-10-21T10:00:00Z'}})).status()).toBe(409);

    const overdue = await body(await coordinator.post(actionsPath, {headers: {...coordinatorHeaders, 'If-Match': '"1"'}, data: {
      planId: created!.data.planId, findingIds: [findingId],
      title: titles.overdue, description: 'Acción sintética con plazo vencido que conserva su incumplimiento.',
      responsibleAssignmentId: responsibleAssignment, dueAt: '2026-01-01T00:00:00Z',
      target: 'Reducir la brecha en 30% medible.', verificationMethod: 'Muestreo documental posterior.'}}), 201);
    expect(overdue.overdue).toBe(true);
    const overduePath = `/api/v1/improvement-actions/${overdue.id}`;
    const revised = await body(await coordinator.patch(overduePath, {headers: {...coordinatorHeaders, 'If-Match': '"1"'}, data: {
      planId: overdue.data.planId, findingIds: [findingId], title: titles.overdue,
      description: 'Acción sintética con plazo extendido que conserva la revisión anterior.',
      responsibleAssignmentId: responsibleAssignment, dueAt: '2027-02-01T00:00:00Z',
      target: 'Reducir la brecha en 30% medible.', verificationMethod: 'Muestreo documental posterior.'}}), 200);
    expect(revised.revision).toBe(2);expect(revised.state).toBe('PROPOSED');expect(revised.overdue).toBe(false);

    await page.goto('/improvement');
    await page.getByLabel('Proceso', {exact: true}).selectOption(processId);
    await expect(page.locator('article', {hasText: titles.verified}).first()).toContainText('Verificada');
    await expect(page.locator('article', {hasText: titles.verified}).first()).toContainText('Avances 1');
    await expect(page.locator('article', {hasText: titles.overdue}).first()).not.toContainText('Vencida sin verificación');
    await page.locator('article', {hasText: titles.verified}).first().getByRole('button', {name: 'Abrir seguimiento'}).click();
    await expect(page.getByText(/Seguimiento de la acci.n/)).toContainText('Verificada');
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d8-improvement/improvement-verified.png', fullPage: true});
  } finally {
    await coordinator.dispose();await responsible.dispose();await reviewer.dispose();
  }
});
