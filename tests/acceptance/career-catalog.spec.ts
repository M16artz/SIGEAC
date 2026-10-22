import {expect,test} from '@playwright/test';
test('D1 API: ámbitos, CSRF, duplicados, concurrencia e historial referenciado',async({playwright,baseURL})=>{
  const admin=await playwright.request.newContext({baseURL});const reader=await playwright.request.newContext({baseURL});
  try {
    const session=await admin.post('/api/v1/sessions',{data:{username:'admin',password:'Synthetic-Admin-2026'}});expect(session.status()).toBe(201);const identity=await session.json();const headers={'X-CSRF-TOKEN':identity.csrfToken};
    const suffix=`API-${Date.now()}`;const unitResponse=await admin.post('/api/v1/academic-units',{headers,data:{code:suffix,name:'Unidad sintética API'}});expect(unitResponse.status()).toBe(201);const unit=await unitResponse.json();
    const input={code:suffix,name:'Carrera sintética API',academicUnitId:unit.id,validFrom:'2020-01-01',validUntil:null};
    expect((await admin.post('/api/v1/careers',{data:input})).status()).toBe(403);
    const created=await admin.post('/api/v1/careers',{headers,data:input});expect(created.status()).toBe(201);expect(created.headers()['etag']).toBe('"1"');const career=await created.json();
    expect((await admin.post('/api/v1/careers',{headers,data:{...input,code:` ${suffix.toLowerCase()} `}})).status()).toBe(409);
    const url=`/api/v1/careers/${career.id}`;
    expect((await admin.patch(url,{headers,data:input})).status()).toBe(428);
    const changed=await admin.patch(url,{headers:{...headers,'If-Match':'"1"'},data:{...input,name:'Carrera sintética actualizada'}});expect(changed.status()).toBe(200);
    expect((await admin.patch(url,{headers:{...headers,'If-Match':'"1"'},data:input})).status()).toBe(412);
    expect((await admin.delete(url,{headers})).status()).toBe(405);
    const history=await admin.get(`${url}/history`);const rows=await history.json();expect(rows.totalElements).toBe(2);expect(rows.items[0].data.name).toBe(input.name);
    expect((await admin.post(`${url}/state-decisions`,{headers:{...headers,'If-Match':'"2"'},data:{status:'DISABLED',reason:'Desactivación sintética API'}})).status()).toBe(201);
    expect((await admin.post(`/api/v1/users/${identity.userId}/assignments`,{headers,data:{roleCode:'COORDINATOR',careerId:career.id,processId:null,indicatorId:null,validFrom:'2020-01-01T00:00:00Z',validUntil:'9999-12-31T00:00:00Z',exceptionReason:null}})).status()).toBe(201);
    const process=await admin.post('/api/v1/processes',{headers,data:{careerId:career.id,code:suffix,name:'Proceso sintético bloqueado',periodStart:'2026-01-01',periodEnd:'2026-12-31',modelVersionId:'60000000-0000-0000-0000-000000000002'}});expect(process.status()).toBe(409);expect((await process.json()).code).toBe('CAREER_INACTIVE');
    expect((await reader.post('/api/v1/sessions',{data:{username:'responsable',password:'Synthetic-User-2026'}})).status()).toBe(201);
    const foreign=await reader.get(url);const missing=await reader.get('/api/v1/careers/ffffffff-ffff-ffff-ffff-ffffffffffff');expect(foreign.status()).toBe(404);expect(missing.status()).toBe(404);expect((await foreign.json()).code).toBe((await missing.json()).code);
    expect((await (await reader.get('/api/v1/careers',{params:{q:suffix}})).json()).totalElements).toBe(0);
    expect((await (await reader.get('/api/v1/academic-units',{params:{q:suffix}})).json()).totalElements).toBe(0);
  } finally {await admin.dispose();await reader.dispose();}
});
test('D1: unidad, carrera, actualización, desactivación e historial',async({page})=>{
  await page.goto('/access/login');await page.getByLabel('Usuario',{exact:true}).fill('admin');await page.getByLabel(/Contrase/).fill('Synthetic-Admin-2026');await page.getByRole('button',{name:'Ingresar'}).click();
  await page.getByRole('link',{name:'Carreras',exact:true}).click();await expect(page.getByRole('heading',{name:'Catálogo de carreras'})).toBeVisible();
  const code=`D1-${Date.now()}`;
  await page.getByLabel('Código de unidad',{exact:true}).fill(code);await page.getByLabel('Nombre de unidad',{exact:true}).fill('Unidad sintética D1');await page.getByRole('button',{name:'Guardar unidad',exact:true}).click();await expect(page.getByRole('status')).toContainText('Unidad guardada');
  await page.getByLabel('Código de carrera',{exact:true}).fill(code);await page.getByLabel('Nombre de carrera',{exact:true}).fill('Carrera sintética D1');await page.getByLabel('Unidad de la carrera').selectOption({label:`${code} · Unidad sintética D1`});await page.getByLabel('Vigente desde',{exact:true}).fill('2020-01-01');await page.getByRole('button',{name:'Guardar carrera',exact:true}).click();await expect(page.getByRole('status')).toContainText('Carrera guardada');
  await page.getByRole('button',{name:`Editar carrera ${code}`,exact:true}).click();await page.getByLabel('Nombre de carrera',{exact:true}).fill('Carrera sintética D1 actualizada');await page.getByRole('button',{name:'Guardar carrera',exact:true}).click();await expect(page.getByRole('row').filter({has:page.getByRole('button',{name:`Editar carrera ${code}`,exact:true})}).getByRole('cell',{name:'Carrera sintética D1 actualizada',exact:true})).toBeVisible();
  await page.getByLabel('Motivo de desactivación o reactivación').fill('Desactivación de prueba sintética D1');await page.getByRole('button',{name:`Desactivar carrera ${code}`,exact:true}).click();await expect(page.getByRole('status')).toContainText('Estado de carrera actualizado');
  await page.getByRole('button',{name:`Historial ${code}`,exact:true}).click();await expect(page.getByRole('region',{name:'Historial de carrera'})).toContainText('Revisión 1');await expect(page.getByRole('region',{name:'Historial de carrera'})).toContainText('Revisión 3');
});
