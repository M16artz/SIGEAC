import {expect, test, APIRequestContext, APIResponse, Response} from '@playwright/test';

const processId = '40000000-0000-0000-0000-000000000001';
const indicatorAId = '50000000-0000-0000-0000-000000000001';
const requiredSourceAId = '80000000-0000-0000-0000-000000000001';

async function json(response: APIResponse | Response, status = 200) {
  expect(response.status(), await response.text()).toBe(status);
  return response.json();
}

async function login(api: APIRequestContext, username: string) {
  return json(await api.post('/api/v1/sessions', {data: {username,
    password: username === 'admin' ? 'Synthetic-Admin-2026' : 'Synthetic-User-2026'}}), 201);
}

test('D5: v1 documental REJECTED exige fundamento, la subsanacion crea un sucesor y v2 puede ACCEPTED', async ({page, playwright, baseURL}) => {
  const collect = await playwright.request.newContext({baseURL});
  const review = await playwright.request.newContext({baseURL});
  try {
    const collectSession = await login(collect, 'responsable');
    const reviewerSession = await login(review, 'revisor');
    const collectHeaders = {'X-CSRF-TOKEN': collectSession.csrfToken};
    const reviewerHeaders = {'X-CSRF-TOKEN': reviewerSession.csrfToken};

    const evidence = await json(await collect.post('/api/v1/evidences', {headers: collectHeaders, data: {
      processId, indicatorIds: [indicatorAId], title: `Evidencia D5 ${Date.now()}`,
      description: 'Evidencia sintetica para el recorrido de revision documental',
      provenance: 'Fuente sintetica de prueba de software D5',
      coveredFrom: '2026-05-01', coveredTo: '2026-08-31'
    }}), 201);
    const version1 = evidence.id;

    const coverage = await collect.post(`/api/v1/evidence-versions/${version1}/source-coverages`, {
      headers: {...collectHeaders, 'If-Match': '"0"'},
      data: {requiredSourceId: requiredSourceAId, coverageNote: 'Cobertura sintetica declarada para D5'}
    });
    expect(coverage.status()).toBe(201);
    expect((await coverage.json()).versionId).toBe(version1);
    const coverageEtag = coverage.headers()['etag'];
    const reference = await collect.post(`/api/v1/evidence-versions/${version1}/external-references`, {
      headers: {...collectHeaders, 'If-Match': coverageEtag},
      data: {uri: 'https://sintetico.unl.edu.ec/d5/evidencia-v1', description: 'Referencia externa sintetica para la revision D5'}
    });
    expect(reference.status()).toBe(201);
    expect((await reference.json()).uri).toBe('https://sintetico.unl.edu.ec/d5/evidencia-v1');
    const submitted = await collect.post(`/api/v1/evidence-versions/${version1}/submit`, {
      headers: {...collectHeaders, 'If-Match': reference.headers()['etag']}, data: {}
    });
    expect(submitted.status()).toBe(200);

    const forbiddenReview = await collect.post(`/api/v1/evidence-versions/${version1}/document-review`, {
      headers: {...collectHeaders, 'If-Match': '"0"'}, data: {selfReviewException: false, justification: 'Revisor no autorizado', outcomes: [{
        requiredSourceId: requiredSourceAId, outcome: 'REJECTED', note: 'Intento sin capacidad EVIDENCE_REVIEW'
      }]}
    });
    expect(forbiddenReview.status()).toBe(403);

    const staleDecision = await review.post(`/api/v1/evidence-versions/${version1}/document-review`, {
      headers: {...reviewerHeaders, 'If-Match': '"5"'},
      data: {selfReviewException: false, justification: 'Fundamento sintetico de la revision', outcomes: [{
        requiredSourceId: requiredSourceAId, outcome: 'REJECTED', note: 'Rechazo sintetico con nota mayor a deca'
      }]}
    });
    expect(staleDecision.status()).toBe(412);
    expect((await staleDecision.json()).code).toBe('STALE_REVIEW');

    const inbox = await json(await review.get(`/api/v1/processes/${processId}/document-review/inbox`));
    expect(inbox).toContainEqual(expect.objectContaining({versionId: version1, revisionNumber: 1}));
    const pending = await json(await review.get(`/api/v1/evidence-versions/${version1}/document-review`));
    expect(pending.decision).toBe('PENDING_REVIEW');

    const rejected = await review.post(`/api/v1/evidence-versions/${version1}/document-review`, {
      headers: {...reviewerHeaders, 'If-Match': '"0"'},
      data: {selfReviewException: false, justification: 'Fundamento sintetico obligatorio de la revision v1', outcomes: [{
        requiredSourceId: requiredSourceAId, outcome: 'REJECTED',
        note: 'La cobertura no contiene trabajo documentario suficiente segun D5'
      }]}
    });
    expect(rejected.status()).toBe(201);
    const decisionBody = await rejected.json();
    const decisionId = decisionBody.id;
    expect(decisionBody.versionId).toBe(version1);
    expect(decisionBody.decision).toBe('REJECTED');
    expect(decisionBody.selfReviewException).toBe(false);
    expect(decisionBody.outcomes[0].outcome).toBe('REJECTED');
    expect(rejected.headers()['etag']).toBe('"0"');
    const afterOpen = await json(await review.get(`/api/v1/evidence-versions/${version1}/document-review`));
    expect(afterOpen.decision).toBe('REJECTED');
    expect(afterOpen.successorVersionId).toBeNull();

    const staleCorrection = await collect.post(`/api/v1/document-review-decisions/${decisionId}/correction`, {
      headers: {...collectHeaders, 'If-Match': '"5"'}
    });
    expect(staleCorrection.status()).toBe(412);
    expect((await staleCorrection.json()).code).toBe('STALE_REVIEW');

    const correction = await collect.post(`/api/v1/document-review-decisions/${decisionId}/correction`, {
      headers: {...collectHeaders, 'If-Match': '"0"'}
    });
    expect(correction.status()).toBe(201);
    const corrected = await correction.json();
    expect(corrected.correctedVersionId).toBe(version1);
    const version2 = corrected.successorVersionId;
    expect(version2).not.toBe(version1);
    expect(correction.headers()['location'] ?? correction.headers()['Location']).toBe(`/api/v1/evidence-versions/${version2}`);

    const repeated = await collect.post(`/api/v1/document-review-decisions/${decisionId}/correction`, {
      headers: {...collectHeaders, 'If-Match': '"0"'}
    });
    expect(repeated.status()).toBe(409);
    expect((await repeated.json()).code).toBe('SUCCESSOR_EXISTS');

    const inheritedCoverage = await json(await collect.get(`/api/v1/evidence-versions/${version2}/source-coverages`));
    expect(inheritedCoverage).toContainEqual(expect.objectContaining({requiredSourceId: requiredSourceAId}));
    const carriedDraft = await json(await collect.get(`/api/v1/evidence-versions/${version2}`));
    expect(carriedDraft.revisionNumber).toBe(2);
    expect(carriedDraft.externalReferences).toContainEqual(expect.objectContaining({uri: 'https://sintetico.unl.edu.ec/d5/evidencia-v1'}));
    const resubmit = await collect.post(`/api/v1/evidence-versions/${version2}/submit`, {
      headers: {...collectHeaders, 'If-Match': '"0"'}, data: {}
    });
    expect(resubmit.status()).toBe(200);

    const accepted = await review.post(`/api/v1/evidence-versions/${version2}/document-review`, {
      headers: {...reviewerHeaders, 'If-Match': '"0"'},
      data: {selfReviewException: false, justification: 'La subsanacion resuelve la observacion registrada en la v1', outcomes: [{
        requiredSourceId: requiredSourceAId, outcome: 'ACCEPTED'
      }]}
    });
    expect(accepted.status()).toBe(201);
    expect((await accepted.json()).decision).toBe('ACCEPTED');
    expect(accepted.headers()['etag']).toBe('"0"');

    const correctedReview = await json(await review.get(`/api/v1/evidence-versions/${version2}/document-review`));
    expect(correctedReview.decision).toBe('ACCEPTED');
    expect(correctedReview.successorVersionId).toBeNull();
    const originalReview = await json(await review.get(`/api/v1/evidence-versions/${version1}/document-review`));
    expect(originalReview.decision).toBe('REJECTED');
    expect(originalReview.successorVersionId).toBe(version2);

    const history = await json(await review.get(`/api/v1/processes/${processId}/document-review-decisions`));
    const runDecisions = (history.items ?? history).filter((item: {versionId: string}) => [version1, version2].includes(item.versionId));
    expect(runDecisions.length).toBe(2);
    expect(runDecisions.some((item: {id: string, decision: string}) => item.id === decisionId && item.decision === 'REJECTED')).toBe(true);
    expect(runDecisions.some((item: {id: string, decision: string}) => item.id !== decisionId && item.decision === 'ACCEPTED')).toBe(true);

    await page.goto('/access/login?returnUrl=/reviews');
    await page.locator('#username').fill('revisor');
    await page.locator('#password').fill('Synthetic-User-2026');
    await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
    await expect(page.getByRole('heading', {name: 'Revisión documental'})).toBeVisible();
    await expect(page.getByRole('link', {name: 'Revisión documental'})).toBeVisible();
  } finally {
    await collect.dispose();
    await review.dispose();
  }
});
