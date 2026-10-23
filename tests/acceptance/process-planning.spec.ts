import {expect, test, APIRequestContext, APIResponse, Response} from '@playwright/test';
import {readFileSync} from 'node:fs';
import {randomUUID} from 'node:crypto';
import path from 'node:path';

const careerId = '11111111-1111-1111-1111-111111111111';
const from = '2026-01-02T05:00:00Z', until = '2026-12-30T05:00:00Z';

async function json(response: APIResponse | Response, status = 200) {
  expect(response.status(), await response.text()).toBe(status);
  return response.json();
}

async function login(api: APIRequestContext, username: string) {
  return json(await api.post('/api/v1/sessions', {data: {username,
    password: username === 'admin' ? 'Synthetic-Admin-2026' : 'Synthetic-User-2026'}}), 201);
}

test('D3: authorize, plan, suspend, read scoped activities and resume with real persistence', async ({page, browser, playwright, baseURL}) => {
  const admin = await playwright.request.newContext({baseURL});
  const coordinator = await playwright.request.newContext({baseURL});
  const participant = await playwright.request.newContext({baseURL});
  try {
    const adminSession = await login(admin, 'admin');
    const coordinatorSession = await login(coordinator, 'coordinador');
    const participantSession = await login(participant, 'responsable');
    const adminHeaders = {'X-CSRF-TOKEN': adminSession.csrfToken};
    const coordinatorHeaders = {'X-CSRF-TOKEN': coordinatorSession.csrfToken};
    const users = await json(await admin.get('/api/v1/users'));
    const reviewer = users.items.find((user: {username: string}) => user.username === 'revisor');
    const coordinatorRoles = await json(await admin.get(`/api/v1/users/${coordinatorSession.userId}/assignments`));
    let authority = coordinatorRoles.find((role: {roleCode: string}) => role.roleCode === 'PROCESS_AUTHORITY');
    if (!authority) authority = await json(await admin.post(`/api/v1/users/${coordinatorSession.userId}/assignments`, {
      headers: adminHeaders, data: {roleCode:'PROCESS_AUTHORITY', careerId,
        validFrom:'2020-01-01T00:00:00Z', validUntil:'9999-12-31T00:00:00Z'}
    }), 201);
    const participantRoles = await json(await admin.get(`/api/v1/users/${participantSession.userId}/assignments`));
    const participantRole = participantRoles.find((role: {roleCode: string}) => role.roleCode === 'RESPONSIBLE');

    // Prepare D2 prerequisites through public APIs; no database edits or mocked responses.
    const template = JSON.parse(readFileSync(path.resolve('../tests/fixtures/models/five-model-configurations.json'), 'utf8')).models[0];
    const ids = new Map<string,string>();
    const fixture = JSON.parse(JSON.stringify(template), (_key, value) => {
      if (typeof value === 'string' && /^[0-9a-f-]{36}$/.test(value)) {
        if (!ids.has(value)) ids.set(value, randomUUID());
        return ids.get(value);
      }
      return value;
    });
    for (const indicator of fixture.configuration.indicators) {
      indicator.normativeReferences = [await json(await admin.post('/api/v1/normative-references', {
        headers: adminHeaders, data: {id:randomUUID(), title:'Referencia exclusivamente sintetica D3', issuer:'Prueba',
          edition:'2026', documentReference:'fixture://d3-browser', documentSha256:'a'.repeat(64), pages:'1',
          disposition:'Disposicion sintetica sin validez institucional', verificationStatus:'VERIFIED',
          verifiedBy:adminSession.userId, verifiedAt:new Date().toISOString()}
      }), 201)];
    }
    const model = await json(await admin.post('/api/v1/models', {headers:adminHeaders,
      data:{name:`Modelo D3 ${Date.now()}`, description:'Solo prueba de aceptacion'}}), 201);
    const version = await json(await admin.post(`/api/v1/models/${model.id}/versions/configured`, {
      headers:adminHeaders, data:{source:fixture.source, nodes:fixture.nodes, configuration:fixture.configuration}
    }), 201);
    await json(await coordinator.post(`/api/v1/model-versions/${version.id}/publish-configured`, {
      headers:{...coordinatorHeaders,'If-Match':`"${version.revision}"`}, data:{approverId:coordinatorSession.userId}
    }));
    await json(await admin.post(`/api/v1/model-versions/${version.id}/applicability-decisions`, {
      headers:{...adminHeaders,'If-Match':'"0"'}, data:{careerId, decision:'APPLICABLE', validFrom:'2026-01-01',
        validUntil:'2026-12-31', provenance:{authorityFunction:'Autoridad sintetica', legalBasis:'Prueba de software',
          documentReference:'fixture://d3-applicability', reason:'Aplicabilidad sintetica para aceptacion'}}
    }), 201);

    await page.goto('/access/login?returnUrl=/processes');
    await page.locator('#username').fill('coordinador');
    await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button',{name:'Ingresar',exact:true}).click();
    await expect(page.locator('#career')).toBeVisible();
    await page.locator('#career').selectOption(careerId);
    await page.locator('#code').fill(`D3-${Date.now()}`);
    await page.locator('#name').fill('Proceso de aceptacion D3');
    await page.locator('#start').fill('2026-01-01'); await page.locator('#end').fill('2026-12-31');
    await page.locator('#model').fill(version.id);
    const createdResponse = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/api/v1/processes'));
    await page.getByRole('button',{name:/Crear proceso en/}).click();
    const process = await json(await createdResponse,201);
    expect(process.state).toBe('PREPARATION');
    const processPath = `/api/v1/processes/${process.id}`;
    const revision = async () => (await coordinator.get(processPath)).headers()['etag'];
    const mutationHeaders = async () => ({...coordinatorHeaders,'If-Match':await revision()});
    const blocked = await coordinator.post(`${processPath}/activate`, {headers:await mutationHeaders(),data:{}});
    expect(blocked.status()).toBe(409); expect((await blocked.json()).code).toBe('AUTHORIZATION_MISSING');
    await page.getByRole('link',{name:'Administrar proceso'}).click();
    await page.getByRole('link',{name:'Autorizacion y planificacion'}).click();
    await page.locator('#authority-assignment').selectOption(authority.id);
    await page.locator('#legal-basis').fill('Designacion sintetica para probar D3');
    await page.locator('#authorization-document').fill('fixture://d3-authorization');
    await page.locator('#authorization-reason').fill('Inicio autorizado en prueba sintetica');
    await page.getByRole('button',{name:/Registrar autorizaci/}).click();
    await expect(page.getByRole('button',{name:'Activar proceso autorizado'})).toBeEnabled();
    await page.getByRole('button',{name:'Activar proceso autorizado'}).click();
    await expect(page.locator('#assign-indicator')).toBeVisible();
    expect((await json(await coordinator.get(processPath))).state).toBe('ACTIVE');

    const teamInput = {name:'Equipo D3',processRole:'COLLECTION',validFrom:from,validUntil:until};
    expect((await coordinator.post(`${processPath}/teams`,{data:teamInput})).status()).toBe(403);
    expect((await coordinator.post(`${processPath}/teams`,{headers:coordinatorHeaders,data:teamInput})).status()).toBe(428);
    expect((await coordinator.post(`${processPath}/teams`,{headers:{...coordinatorHeaders,'If-Match':'"0"'},data:teamInput})).status()).toBe(412);
    await page.locator('#team-name').fill(teamInput.name); await page.locator('#team-role').fill(teamInput.processRole);
    await page.locator('#team-from').fill('2026-01-02T00:00'); await page.locator('#team-until').fill('2026-12-30T00:00');
    const teamResponse = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/teams'));
    await page.getByRole('button',{name:'Crear equipo',exact:true}).click();
    const team = await json(await teamResponse,201);
    await page.locator('#member-team').selectOption(team.id);
    await page.locator('#member-user').fill(participantSession.userId); await page.locator('#member-role').fill(participantRole.id);
    await page.locator('#member-from').fill('2026-01-02T00:00'); await page.locator('#member-until').fill('2026-12-30T00:00');
    const membership = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/memberships'));
    await page.getByRole('button',{name:'Agregar integrante',exact:true}).click(); await json(await membership,201);
    const indicator = fixture.configuration.indicators[0];
    await page.locator('#assign-indicator').selectOption(indicator.nodeId);
    await page.locator('#assign-subject').selectOption('TEAM'); await page.locator('#assign-team').selectOption(team.id);
    await page.locator('#assign-from').fill('2026-01-02T00:00'); await page.locator('#assign-until').fill('2026-12-30T00:00');
    await page.locator('#assign-reason').fill('Responsabilidad de equipo sintetica');
    const assignmentResponse = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/assignments'));
    await page.getByRole('button',{name:'Asignar',exact:true}).click(); const assignment = await json(await assignmentResponse,201);
    await page.locator('#activity-title').fill('Recolectar D3'); await page.locator('#activity-indicator').selectOption(indicator.nodeId);
    await page.locator('#activity-assignment').selectOption(assignment.id);
    await page.locator('#activity-source').fill(indicator.sources[0].id); await page.locator('#activity-due').fill('2026-11-01T12:00');
    const activityResponse = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/activities'));
    await page.getByRole('button',{name:'Crear actividad',exact:true}).click(); const activity = await json(await activityResponse,201);
    await page.locator('li').filter({hasText:'Recolectar D3'}).getByRole('button',{name:'Editar',exact:true}).click();
    await page.locator('#activity-title').fill('Recolectar D3 corregida');
    const edited = page.waitForResponse(response => response.request().method() === 'PATCH' && response.url().endsWith(`/activities/${activity.id}`));
    await page.getByRole('button',{name:'Actualizar actividad',exact:true}).click();
    expect((await json(await edited)).requiredSourceId).toBe(indicator.sources[0].id);

    const input = {title:'Dependiente D3',indicatorId:indicator.nodeId,assignmentId:assignment.id,
      dueAt:activity.dueAt,dependencyIds:[activity.id]};
    const dependent = await json(await coordinator.post(`${processPath}/activities`,{headers:await mutationHeaders(),data:input}),201);
    const cyclic = await coordinator.patch(`/api/v1/activities/${activity.id}`,{headers:await mutationHeaders(),
      data:{...input,title:'Ciclo rechazado',dependencyIds:[dependent.id]}});
    expect(cyclic.status()).toBe(409); expect((await cyclic.json()).code).toBe('DEPENDENCY_CYCLE');
    const privateAssignment = await json(await coordinator.post(`${processPath}/assignments`,{headers:await mutationHeaders(),
      data:{indicatorId:indicator.nodeId,identityId:reviewer.id,function:'REVIEWER',validFrom:from,validTo:until,reason:'Revision privada sintetica'}}),201);
    const privateActivity = await json(await coordinator.post(`${processPath}/activities`,{headers:await mutationHeaders(),
      data:{...input,title:'Privada D3',assignmentId:privateAssignment.id,dependencyIds:[]}}),201);
    await page.reload();
    await page.locator('#transition-authority').selectOption(authority.id);
    await page.locator('#transition-basis').fill('Designacion sintetica D3');
    await page.locator('#transition-document').fill('fixture://d3-suspension');
    await page.locator('#transition-reason').fill('Suspension justificada de prueba D3');
    await page.getByRole('button',{name:'Suspender',exact:true}).click();
    await expect(page.getByRole('button',{name:'Retomar',exact:true})).toBeVisible();
    await expect(page.getByRole('button',{name:'Crear actividad',exact:true})).toHaveCount(0);
    const suspended = await coordinator.post(`${processPath}/activities`,{headers:await mutationHeaders(),data:input});
    expect(suspended.status()).toBe(409); expect((await suspended.json()).code).toBe('PROCESS_NOT_PLANNABLE');

    const memberContext = await browser.newContext();
    try {
      const memberPage = await memberContext.newPage();
      await memberPage.goto(`${baseURL}/access/login?returnUrl=/processes/${process.id}/activities`);
      await memberPage.locator('#username').fill('responsable'); await memberPage.locator('#password').fill('Synthetic-User-2026');
      await memberPage.getByRole('button',{name:'Ingresar',exact:true}).click();
      await expect(memberPage.getByRole('heading',{name:'Actividades del proceso'})).toBeVisible();
      await expect(memberPage.getByRole('table')).toContainText('Recolectar D3 corregida');
      await expect(memberPage.getByRole('table')).not.toContainText('Privada D3');
      await expect(memberPage.getByRole('status')).toContainText('suspendido');
      expect((await participant.get(`/api/v1/activities/${privateActivity.id}`)).status()).toBe(404);
      expect((await participant.get(`/api/v1/activities/${randomUUID()}`)).status()).toBe(404);
      expect((await participant.post(`${processPath}/teams`,{headers:{'X-CSRF-TOKEN':participantSession.csrfToken,
        'If-Match':await revision()},data:teamInput})).status()).toBe(404);
    } finally { await memberContext.close(); }
    await page.locator('#transition-document').fill('fixture://d3-resumption');
    await page.locator('#transition-reason').fill('Reanudacion autorizada de prueba D3');
    await page.getByRole('button',{name:'Retomar',exact:true}).click();
    await expect(page.getByRole('button',{name:'Crear actividad',exact:true})).toBeVisible();
    await page.reload();
    await expect(page.locator('li').filter({hasText:'Recolectar D3 corregida'})).toBeVisible();
    const final = await json(await coordinator.get(processPath));
    expect(final.state).toBe('ACTIVE'); expect(final.modelVersionId).toBe(version.id);
    expect(final.authorization.documentReference).toBe('fixture://d3-authorization');
    expect((await json(await coordinator.get(`/api/v1/activities/${activity.id}`))).revision).toBe(1);
  } finally { await admin.dispose(); await coordinator.dispose(); await participant.dispose(); }
});
