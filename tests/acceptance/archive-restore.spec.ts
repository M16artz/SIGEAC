import {expect, test, APIResponse, Page} from '@playwright/test';

const processId = 'd7000000-0000-0000-0000-000000000003';
const findingId = 'd8000000-0000-0000-0000-000000000200';
const responsibleAssignment = 'd8000000-0000-0000-0000-000000000901';
const authorityAssignment = 'd9000000-0000-0000-0000-000000000901';
const exceptionDecision = 'd9000000-0000-0000-0000-000000000920';
const processPath = `/api/v1/processes/${processId}`;
const actionTitle = 'Accion vencida conservada durante el cierre integral P16';

async function body(response: APIResponse, status = 200) {
  expect(response.status(), await response.text()).toBe(status);
  return response.json();
}

async function login(page: Page) {
  await page.goto('/access/login?returnUrl=/archive');
  await page.locator('#username').fill('coordinador');
  await page.locator('#password').fill('Synthetic-User-2026');
  await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
  await page.waitForURL(/\/archive/);
}

const provenance = {
  authorityAssignmentId: authorityAssignment,
  legalBasis: 'Reglamento interno de cierre integral y conservacion.',
  documentReference: 'fixture://acta-cierre-integral-d8',
  reason: 'Decision sintetica autorizada para verificar el cierre integral P16.'
};

test('AC-P16-01/02/03: blocks overdue closure, archives sealed history and preserves it after reopening', async ({page, playwright, baseURL}) => {
  const coordinator = await playwright.request.newContext({baseURL});
  try {
    const session = await body(await coordinator.post('/api/v1/sessions', {
      data: {username: 'coordinador', password: 'Synthetic-User-2026'}
    }), 201) as {csrfToken: string};
    const headers = {'X-CSRF-TOKEN': session.csrfToken};

    let preview = await body(await coordinator.get(`${processPath}/closure-preview?closureType=INTEGRAL_CLOSURE`));
    expect(preview.eligible).toBe(true);
    expect(preview.revision).toBe(2);
    expect(preview.closureType).toBe('INTEGRAL_CLOSURE');

    const plan = await body(await coordinator.post(`${processPath}/improvement-plans`, {
      headers: {...headers, 'If-Match': '"0"'},
      data: {title: 'Plan de conservacion para el cierre integral P16', reportVersionId: 'd8000000-0000-0000-0000-000000000100'}
    }), 201) as {id: string; revision: number};

    const action = await body(await coordinator.post(`${processPath}/improvement-actions`, {
      headers: {...headers, 'If-Match': '"0"'},
      data: {
        planId: plan.id,
        findingIds: [findingId],
        title: actionTitle,
        description: 'Accion sintetica con plazo vencido que debe bloquear el cierre integral.',
        responsibleAssignmentId: responsibleAssignment,
        dueAt: '2026-01-01T00:00:00Z',
        target: 'Cerrar la brecha antes del cierre integral.',
        verificationMethod: 'Evidencia documental y verificacion institucional.'
      }
    }), 201);
    expect(action.overdue).toBe(true);

    preview = await body(await coordinator.get(`${processPath}/closure-preview?closureType=INTEGRAL_CLOSURE`));
    expect(preview.eligible).toBe(false);
    expect(preview.revision).toBe(2);
    expect(preview.blockers.some((blocker: string) => blocker.includes(actionTitle))).toBe(true);

    const rejected = await coordinator.post(`${processPath}/close`, {
      headers: {...headers, 'If-Match': '"2"'},
      data: {closurePolicyVersion: 'INTEGRAL_STRICT_V1', provenance, exceptionDecisionIds: []}
    });
    expect(rejected.status(), await rejected.text()).toBe(409);
    expect((await rejected.json()).code).toBe('CLOSURE_BLOCKED');

    await login(page);
    await page.locator('#archive-process').selectOption(processId);
    await expect(page.locator('.blocked')).toContainText(actionTitle);
    await page.locator('#closure-assignment').fill(authorityAssignment);
    await page.locator('#closure-legal').fill(provenance.legalBasis);
    await page.locator('#closure-document').fill(provenance.documentReference);
    await page.locator('#closure-reason').fill(provenance.reason);
    await page.locator('#closure-exceptions').fill(exceptionDecision);
    await page.getByRole('button', {name: 'Cerrar integralmente', exact: true}).click();
    await expect(page.getByText(/Cierre integral registrado/)).toBeVisible();

    let history = await body(await coordinator.get(`${processPath}/closures?page=0&size=20`));
    const closure = history.items.find((item: {type: string}) => item.type === 'INTEGRAL_CLOSURE');
    expect(closure.processId).toBe(processId);
    expect(closure.type).toBe('INTEGRAL_CLOSURE');
    expect(closure.policyVersion).toBe('INTEGRAL_STRICT_V1');
    expect(closure.sha256).toMatch(/^[0-9a-f]{64}$/);
    expect(closure.exceptionDecisionIds).toEqual([exceptionDecision]);
    expect(closure.pendingIds).toEqual([]);
    expect(history.totalElements).toBe(2);
    expect(history.items.map((item: {type: string}) => item.type)).toEqual(['INTEGRAL_CLOSURE', 'DOCUMENTARY_CLOSURE']);
    expect(history.items[0].id).toBe(closure.id);

    await body(await coordinator.post(`${processPath}/archive`, {
      headers: {...headers, 'If-Match': '"4"'},
      data: {closureSnapshotId: closure.id, retentionPolicyVersion: 'RETENTION_10Y', provenance}
    }), 201).then((record: {integrityStatus: string; closureSnapshotId: string}) => {
      expect(record.integrityStatus).toBe('VERIFIED');
      expect(record.closureSnapshotId).toBe(closure.id);
    });

    const duplicate = await coordinator.post(`${processPath}/archive`, {
      headers: {...headers, 'If-Match': '"4"'},
      data: {closureSnapshotId: closure.id, retentionPolicyVersion: 'RETENTION_10Y', provenance}
    });
    expect(duplicate.status(), await duplicate.text()).toBe(409);
    expect((await duplicate.json()).code).toBe('ARCHIVE_ALREADY_EXISTS');

    await page.getByRole('button', {name: 'Actualizar', exact: true}).click();
    await expect(page.getByText('2 cierre(s) conservado(s).')).toBeVisible();
    await expect(page.locator('article[aria-label="Cierre INTEGRAL_CLOSURE"]')).toContainText('Cierre integral');
    await page.screenshot({path: '../tests/acceptance/results/playwright-artifacts/d8-archive/archive-history.png', fullPage: true});

    const reopened = await body(await coordinator.post(`${processPath}/transitions`, {
      headers: {...headers, 'If-Match': '"4"'},
      data: {
        target: 'REOPENED',
        reason: 'Reapertura sintetica controlada que conserva el archivo y el historial P16.',
        authorityAssignmentId: authorityAssignment,
        legalBasis: 'Reglamento interno de reapertura controlada.',
        documentReference: 'fixture://acta-reapertura-d8'
      }
    }));
    expect(reopened.state).toBe('REOPENED');

    history = await body(await coordinator.get(`${processPath}/closures?page=0&size=1`));
    expect(history.totalElements).toBe(2);
    expect(history.items).toHaveLength(1);
    expect(history.items[0].id).toBe(closure.id);
    expect(history.items[0].sha256).toBe(closure.sha256);

    preview = await body(await coordinator.get(`${processPath}/closure-preview?closureType=INTEGRAL_CLOSURE`));
    expect(preview.eligible).toBe(false);
    expect(preview.revision).toBe(5);

    const rejectedAgain = await coordinator.post(`${processPath}/close`, {
      headers: {...headers, 'If-Match': '"5"'},
      data: {closurePolicyVersion: 'INTEGRAL_STRICT_V1', provenance, exceptionDecisionIds: [exceptionDecision]}
    });
    expect(rejectedAgain.status(), await rejectedAgain.text()).toBe(409);
    expect((await rejectedAgain.json()).code).toBe('CLOSURE_STATE_INVALID');
  } finally {
    await coordinator.dispose();
  }
});
