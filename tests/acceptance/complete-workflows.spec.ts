import {expect, test, APIRequestContext, APIResponse} from '@playwright/test';
import {readFileSync} from 'node:fs';
import {randomUUID} from 'node:crypto';
import path from 'node:path';

const careerId = '11111111-1111-1111-1111-111111111111';
type Actor = {api: APIRequestContext; id: string; headers: Record<string, string>};
let admin: Actor, coordinator: Actor, collector: Actor, reviewer: Actor, evaluator: Actor;
let authorityId: string;
let apiFactory: () => Promise<APIRequestContext>;
const contexts: APIRequestContext[] = [];
const templates = JSON.parse(readFileSync(path.resolve('../tests/fixtures/models/five-model-configurations.json'), 'utf8')).models;

async function body(response: APIResponse, status = 200) {
  expect(response.status(), await response.text()).toBe(status);
  return response.json();
}
const etag = (revision: number) => `"${revision}"`;
const unique = () => `SYN-${randomUUID().slice(0, 12)}`;

test.beforeAll(async ({playwright, baseURL}) => {
  apiFactory = () => playwright.request.newContext({baseURL});
  async function actor(username: string): Promise<Actor> {
    const api = await playwright.request.newContext({baseURL}); contexts.push(api);
    const login = await body(await api.post('/api/v1/sessions', {data: {username,
      password: username === 'admin' ? 'Synthetic-Admin-2026' : 'Synthetic-User-2026'}}), 201);
    return {api, id: login.userId, headers: {'X-CSRF-TOKEN': login.csrfToken}};
  }
  admin = await actor('admin'); coordinator = await actor('coordinador');
  collector = await actor('responsable'); reviewer = await actor('revisor'); evaluator = await actor('evaluador');
  const roles = await body(await admin.api.get(`/api/v1/users/${coordinator.id}/assignments`));
  const current = roles.find((role: {roleCode: string}) => role.roleCode === 'PROCESS_AUTHORITY');
  authorityId = current?.id ?? (await body(await admin.api.post(`/api/v1/users/${coordinator.id}/assignments`, {
    headers: admin.headers, data: {roleCode: 'PROCESS_AUTHORITY', careerId,
      validFrom: '2020-01-01T00:00:00Z', validUntil: '2030-01-01T00:00:00Z'}
  }), 201)).id;
});
test.afterAll(async () => { for (const context of contexts) await context.dispose(); });

async function catalog() {
  const code = unique();
  const unit = await body(await admin.api.post('/api/v1/academic-units', {headers: admin.headers,
    data: {code, name: `Unidad sintetica ${code}`}}), 201);
  const data = {code, name: `Carrera sintetica ${code}`, academicUnitId: unit.id,
    validFrom: '2020-01-01', validUntil: null};
  const career = await body(await admin.api.post('/api/v1/careers', {headers: admin.headers, data}), 201);
  return {unit, career, data};
}

async function publishedModel(index = 0, documentary = false,
    configure?: (fixture: any) => void | Promise<void>, applicable = true) {
  const ids = new Map<string, string>();
  const fixture = JSON.parse(JSON.stringify(templates[index]), (_key, value) => {
    if (typeof value === 'string' && /^[0-9a-f-]{36}$/.test(value)) {
      if (!ids.has(value)) ids.set(value, randomUUID());
      return ids.get(value);
    }
    return value;
  });
  for (const indicator of fixture.configuration.indicators) {
    if (documentary) {
      indicator.nature = 'QUALITATIVE';
      const rule = fixture.configuration.rules.find((r: any) => r.id === indicator.valuationRuleId);
      Object.assign(rule, {ruleType: 'MANUAL_QUALITATIVE', formulaDefinition: null, strategyKey: null,
        strategyVersion: null, variables: [], thresholds: [], status: 'VALIDATED'});
      indicator.sources = [1, 2].map(n => ({id: randomUUID(), indicatorNodeId: indicator.nodeId,
        code: `DOC-${n}`, sourceType: 'EXTERNAL_LINK', name: `Fuente sintetica ${n}`,
        description: 'Solo aceptacion de software', required: true, cardinalityMin: 1,
        cardinalityMax: null, periodRule: 'Periodo 2026', ordinal: n}));
    }
    indicator.normativeReferences = [await body(await admin.api.post('/api/v1/normative-references', {
      headers: admin.headers, data: {id: randomUUID(), title: 'Referencia sintetica de aceptacion', issuer: 'Prueba',
        edition: '2026', documentReference: 'fixture://complete-workflows', documentSha256: 'a'.repeat(64), pages: '1',
        disposition: 'Prueba de software sin validez institucional', verificationStatus: 'VERIFIED',
        verifiedBy: admin.id, verifiedAt: new Date().toISOString()}
    }), 201)];
  }
  await configure?.(fixture);
  const model = await body(await admin.api.post('/api/v1/models', {headers: admin.headers,
    data: {name: unique(), description: 'Aceptacion exclusivamente sintetica'}}), 201);
  const draft = await body(await admin.api.post(`/api/v1/models/${model.id}/versions/configured`, {
    headers: admin.headers, data: {source: fixture.source, nodes: fixture.nodes, configuration: fixture.configuration}
  }), 201);
  const version = await body(await coordinator.api.post(`/api/v1/model-versions/${draft.id}/publish-configured`, {
    headers: {...coordinator.headers, 'If-Match': etag(draft.revision)}, data: {approverId: coordinator.id}
  }));
  if (applicable) await body(await admin.api.post(`/api/v1/model-versions/${version.id}/applicability-decisions`, {
    headers: {...admin.headers, 'If-Match': '"0"'}, data: {careerId, decision: 'APPLICABLE', validFrom: '2026-01-01',
      validUntil: '2026-12-31', provenance: {authorityFunction: 'Autoridad sintetica', legalBasis: 'Prueba de software',
        documentReference: 'fixture://applicability', reason: 'Decision sintetica para aceptacion'}}
  }), 201);
  return version;
}

async function processFor(version: any, activate = false) {
  const created = await body(await coordinator.api.post('/api/v1/processes', {headers: coordinator.headers,
    data: {careerId, code: unique(), name: 'Proceso sintetico de aceptacion', periodStart: '2026-01-01',
      periodEnd: '2026-12-31', modelVersionId: version.id}}), 201);
  if (!activate) return created;
  const url = `/api/v1/processes/${created.id}`;
  await body(await coordinator.api.post(`${url}/authorizations`, {
    headers: {...coordinator.headers, 'If-Match': etag(created.revision)}, data: {authorityAssignmentId: authorityId,
      legalBasis: 'Prueba sintetica de software', documentReference: 'fixture://authorization',
      reason: 'Autorizacion sintetica para probar el ciclo completo'}
  }), 201);
  const current = await body(await coordinator.api.get(url));
  return body(await coordinator.api.post(`${url}/activate`, {headers: {...coordinator.headers,
    'If-Match': etag(current.revision)}, data: {}}));
}

async function documentary(configure?: (fixture: any) => void | Promise<void>) {
  const model = await publishedModel(0, true, configure);
  const process = await processFor(model, true);
  for (const indicator of process.indicators) {
    for (const [actor, functionCode] of [[collector, 'RESPONSIBLE'], [reviewer, 'REVIEWER'], [evaluator, 'EVALUATOR']] as const) {
      const head = await body(await coordinator.api.get(`/api/v1/processes/${process.id}`));
      await body(await coordinator.api.post(`/api/v1/processes/${process.id}/assignments`, {
        headers: {...coordinator.headers, 'If-Match': etag(head.revision)}, data: {indicatorId: indicator.id,
          identityId: actor.id, function: functionCode, validFrom: '2026-01-01T00:00:00Z',
          validTo: '2026-12-31T00:00:00Z', reason: 'Asignacion sintetica de aceptacion'}
      }), 201);
    }
  }
  return {model, process, sources: model.configuration.indicators.flatMap((i: any) => i.sources)};
}

async function emptyDraft(flow: any, indicatorIds = [flow.sources[0].indicatorNodeId]) {
  return body(await collector.api.post('/api/v1/evidences', {headers: collector.headers,
    data: {processId: flow.process.id, indicatorIds, title: unique(), description: 'Contenido exclusivamente sintetico',
      provenance: 'Fixture de software', coveredFrom: '2026-01-01', coveredTo: '2026-06-30'}}), 201);
}

async function draft(flow: any, selected = [flow.sources[0]]) {
  const indicatorIds = [...new Set(selected.map((s: any) => s.indicatorNodeId))];
  const version = await emptyDraft(flow, indicatorIds);
  let revision = version.revision;
  for (const source of selected) {
    const coverage = await collector.api.post(`/api/v1/evidence-versions/${version.id}/source-coverages`, {
      headers: {...collector.headers, 'If-Match': etag(revision)}, data: {requiredSourceId: source.id, coverageNote: 'Cobertura explicita sintetica'}
    });
    await body(coverage, 201); revision = Number(coverage.headers()['etag'].replaceAll('"', ''));
  }
  const reference = await collector.api.post(`/api/v1/evidence-versions/${version.id}/external-references`, {
    headers: {...collector.headers, 'If-Match': etag(revision)},
    data: {uri: 'https://acceptance.invalid/document', description: 'Referencia sintetica sin disponibilidad remota'}
  });
  await body(reference, 201);
  return {version, etag: reference.headers()['etag']};
}

async function submit(prepared: Awaited<ReturnType<typeof draft>>) {
  return body(await collector.api.post(`/api/v1/evidence-versions/${prepared.version.id}/submit`, {
    headers: {...collector.headers, 'If-Match': prepared.etag}, data: {}}));
}

async function decision(id: string, outcomes: any[]) {
  return body(await reviewer.api.post(`/api/v1/evidence-versions/${id}/document-review`, {
    headers: {...reviewer.headers, 'If-Match': '"0"'}, data: {selfReviewException: false,
      justification: 'Revision documental sintetica con fundamento', outcomes}
  }), 201);
}

const handlers: Record<string, () => Promise<void>> = {
  'AC-P01-01': async () => {
    const response = await collector.api.get('/api/v1/me');
    const identity = await body(response);
    expect(identity.id).toBe(collector.id);
    expect(identity.assignments.every((a: any) => a.careerId === careerId)).toBe(true);
    expect((await collector.api.delete('/api/v1/sessions', {headers: collector.headers})).status()).toBe(204);
    expect((await collector.api.get('/api/v1/me')).status()).toBe(401);
    const restored = await body(await collector.api.post('/api/v1/sessions', {
      data: {username: 'responsable', password: 'Synthetic-User-2026'}}), 201);
    collector.headers = {'X-CSRF-TOKEN': restored.csrfToken};
  },
  'AC-P01-02': async () => {
    const username = unique();
    const user = await body(await admin.api.post('/api/v1/users', {headers: admin.headers,
      data: {username, displayName: 'Cuenta sintetica desactivada', initialPassword: 'Synthetic-User-2026', technical: false}}), 201);
    await body(await admin.api.patch(`/api/v1/users/${user.id}`, {headers: {...admin.headers, 'If-Match': '"0"'}, data: {active: false}}));
    const errors = [];
    for (const login of [{username, password: 'Synthetic-User-2026'}, {username: unique(), password: 'Synthetic-User-2026'},
      {username: 'responsable', password: 'Incorrect-Synthetic-2026'}]) {
      errors.push(await body(await admin.api.post('/api/v1/sessions', {data: login}), 401));
    }
    expect(new Set(errors.map(e => e.code)).size).toBe(1);
    expect(new Set(errors.map(e => e.detail)).size).toBe(1);
  },
  'AC-P02-01': async () => {
    const {unit, career} = await catalog();
    expect(career.data.academicUnitId).toBe(unit.id); expect(career.status).toBe('ACTIVE');
    expect((await body(await admin.api.get(`/api/v1/careers/${career.id}`))).id).toBe(career.id);
  },
  'AC-P02-02': async () => {
    const {data, career} = await catalog();
    await body(await admin.api.post('/api/v1/careers', {headers: admin.headers,
      data: {...data, code: ` ${data.code.toLowerCase()} `}}), 409);
    const correlation = randomUUID();
    const foreignResponse = await collector.api.get(`/api/v1/careers/${career.id}`, {headers: {'X-Correlation-ID': correlation}});
    const foreign = await body(foreignResponse, 404);
    expect(foreign.correlationId).toBe(correlation);
    expect(foreignResponse.headers()['x-correlation-id']).toBe(correlation);
    const missing = await body(await collector.api.get(`/api/v1/careers/${randomUUID()}`), 404);
    expect(foreign.code).toBe(missing.code);
  },
  'AC-P03-01': async () => {
    const version = await publishedModel();
    expect(version.status).toBe('PUBLISHED'); expect(version.authorId).not.toBe(version.approverId);
    const loaded = await body(await admin.api.get(`/api/v1/model-versions/${version.id}/configured`));
    expect(loaded.configuration.indicators[0].normativeReferences[0].verificationStatus).toBe('VERIFIED');
    expect(loaded.source).toBe(version.source);
  },
  'AC-P03-02': async () => {
    const model = await body(await admin.api.post('/api/v1/models', {headers: admin.headers,
      data: {name: unique(), description: 'Borrador sintetico incompleto'}}), 201);
    const draft = await body(await admin.api.post(`/api/v1/models/${model.id}/versions/configured`, {
      headers: admin.headers, data: {source: '', nodes: [], configuration: {indicators: [], scales: [], rules: []}}
    }), 201);
    await body(await coordinator.api.post(`/api/v1/model-versions/${draft.id}/publish-configured`, {
      headers: {...coordinator.headers, 'If-Match': etag(draft.revision)}, data: {approverId: coordinator.id}
    }), 400);
    expect((await body(await admin.api.get(`/api/v1/model-versions/${draft.id}/configured`))).status).toBe('DRAFT');
  },
  'AC-P03-03': async () => {
    const version = await publishedModel(); const process = await processFor(version);
    const successor = await body(await admin.api.post(`/api/v1/model-versions/${version.id}/successors/configured`, {headers: admin.headers, data: {}}), 201);
    expect(successor.predecessorId).toBe(version.id);
    expect((await body(await coordinator.api.get(`/api/v1/processes/${process.id}`))).modelVersionId).toBe(version.id);
    expect(await body(await admin.api.get(`/api/v1/model-versions/${version.id}/configured`))).toEqual(version);
  },
  'AC-P04-01': async () => {
    const depths = new Set<number>();
    let directIndicator = false;
    const depth = (nodes: any[]): number => nodes.length ? 1 + Math.max(...nodes.map(n => depth(n.children))) : 0;
    for (let n = 0; n < templates.length; n++) {
      const version = await publishedModel(n); depths.add(depth(version.criteria));
      directIndicator ||= version.criteria.some((node: any) => node.nodeType === 'CRITERION'
        && node.children.some((child: any) => child.nodeType === 'INDICATOR'));
      expect(version.configuration.indicators.every((i: any) => i.sources.length && i.standardText && i.evaluationPeriodDefinition)).toBe(true);
    }
    expect(depths.size).toBeGreaterThan(1);
    expect(directIndicator).toBe(true);
  },
  'AC-P05-01': async () => {
    const version = await publishedModel(); const process = await processFor(version, true);
    expect(process.state).toBe('ACTIVE'); expect(process.modelVersionId).toBe(version.id);
    expect(process.authorization.authorityAssignmentId).toBe(authorityId);
    expect(process.authorization.authorizedAt).toBeTruthy();
  },
  'AC-P05-03': async () => {
    const process = await processFor(await publishedModel(), true);
    const url = `/api/v1/processes/${process.id}`;
    let current = process;
    for (const target of ['SUSPENDED', 'ACTIVE']) {
      current = await body(await coordinator.api.post(`${url}/transitions`, {headers: {...coordinator.headers,
        'If-Match': etag(current.revision)}, data: {target, authorityAssignmentId: authorityId,
        legalBasis: 'Politica sintetica', documentReference: 'fixture://transition', reason: 'Decision de transicion sintetica fundamentada'}}));
      expect(current.state).toBe(target); expect(current.modelVersionId).toBe(process.modelVersionId);
      expect(current.periodEnd).toBe(process.periodEnd); expect(current.authorization).toEqual(process.authorization);
    }
  },
  'AC-P07-01': async () => {
    const flow = await documentary(); const prepared = await draft(flow);
    expect(prepared.version.status).toBe('DRAFT');
    const coverages = await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}/source-coverages`));
    expect(coverages.map((c: any) => c.requiredSourceId)).toEqual([flow.sources[0].id]);
  },
  'AC-P08-01': async () => {
    const flow = await documentary(); const prepared = await draft(flow); const submitted = await submit(prepared);
    expect(submitted.status).toBe('SUBMITTED'); expect(submitted.submittedAt).toBeTruthy();
    const original = await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}`));
    await body(await collector.api.patch(`/api/v1/evidence-versions/${prepared.version.id}`, {
      headers: {...collector.headers, 'If-Match': etag(submitted.revision)}, data: {title: 'Intento de sobrescritura',
        description: 'Cambio no permitido', provenance: 'Prueba', coveredFrom: '2026-01-01', coveredTo: '2026-06-30',
        indicatorIds: [flow.sources[0].indicatorNodeId]}
    }), 409);
    expect(await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}`))).toEqual(original);
    expect((await body(await reviewer.api.get(`/api/v1/processes/${flow.process.id}/document-review/inbox`)))
      .some((item: any) => item.versionId === prepared.version.id || item.id === prepared.version.id)).toBe(true);
  },
  'AC-P11-01': async () => {
    const flow = await documentary();
    for (let n = 0; n < 3; n++) {
      const prepared = await draft(flow, [flow.sources[n]]); await submit(prepared);
      await decision(prepared.version.id, [{requiredSourceId: flow.sources[n].id, outcome: n < 2 ? 'ACCEPTED' : 'OBSERVED',
        ...(n < 2 ? {} : {note: 'Pendiente documental sintetico'})}]);
    }
    const progress = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/documentary-progress`));
    expect(progress).toMatchObject({requiredSourceTotal: 4, registeredSourceTotal: 3, acceptedSourceTotal: 2,
      collectionPercent: 75, documentReviewPercent: 50}); expect(progress.academicStatus).toBeUndefined();
  },
  'AC-X02': async () => {
    const username = unique();
    const user = await body(await admin.api.post('/api/v1/users', {headers: admin.headers,
      data: {username, displayName: 'Sesion sintetica revocable', initialPassword: 'Synthetic-User-2026', technical: false}}), 201);
    const session = await body(await reviewer.api.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201);
    try {
      expect((await body(await reviewer.api.get('/api/v1/me'))).id).toBe(user.id);
      await body(await admin.api.patch(`/api/v1/users/${user.id}`, {headers: {...admin.headers, 'If-Match': '"0"'}, data: {active: false}}));
      expect((await reviewer.api.get('/api/v1/me')).status()).toBe(401);
      expect(session.userId).toBe(user.id);
    } finally {
      const restored = await body(await reviewer.api.post('/api/v1/sessions', {data: {username: 'revisor', password: 'Synthetic-User-2026'}}), 201);
      reviewer.headers = {'X-CSRF-TOKEN': restored.csrfToken};
    }
  },
  'AC-X03': async () => {
    const {career, data} = await catalog(); const url = `/api/v1/careers/${career.id}`;
    const username = unique();
    const user = await body(await admin.api.post('/api/v1/users', {headers: admin.headers,
      data: {username, displayName: 'Segundo actor de concurrencia', initialPassword: 'Synthetic-User-2026', technical: false}}), 201);
    await body(await admin.api.post(`/api/v1/users/${user.id}/assignments`, {headers: admin.headers,
      data: {roleCode: 'FUNCTIONAL_ADMIN', careerId: career.id, validFrom: '2020-01-01T00:00:00Z', validUntil: '2030-01-01T00:00:00Z'}}), 201);
    const other = await apiFactory(); contexts.push(other);
    const session = await body(await other.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201);
    expect(session.userId).not.toBe(admin.id);
    const results = await Promise.all([
      admin.api.patch(url, {headers: {...admin.headers, 'If-Match': '"1"'}, data: {...data, name: 'Cambio sintetico actor uno'}}),
      other.patch(url, {headers: {'X-CSRF-TOKEN': session.csrfToken, 'If-Match': '"1"'}, data: {...data, name: 'Cambio sintetico actor dos'}})
    ]);
    expect(results.map(r => r.status()).sort()).toEqual([200, 412]);
    expect((await body(await admin.api.get(`${url}/history`))).totalElements).toBe(2);
  }
};

// Aliases rerun the same complete assertion path; they never reuse a stored PASS.
handlers['AC-X08'] = handlers['AC-P11-01'];
handlers['AC-X06'] = async () => {
  const flow = await documentary(); const prepared = await draft(flow, flow.sources.slice(0, 2)); await submit(prepared);
  await decision(prepared.version.id, [{requiredSourceId: flow.sources[0].id, outcome: 'ACCEPTED'},
    {requiredSourceId: flow.sources[1].id, outcome: 'OBSERVED', note: 'Cobertura observada con motivo sintetico'}]);
  const progress = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/documentary-progress`));
  expect(progress).toMatchObject({requiredSourceTotal: 4, registeredSourceTotal: 2, acceptedSourceTotal: 1,
    collectionPercent: 50, documentReviewPercent: 25});
  const review = await body(await reviewer.api.get(`/api/v1/evidence-versions/${prepared.version.id}/document-review`));
  expect(review.decision).toBe('OBSERVED');
  for (const indicator of flow.process.indicators) {
    const evaluations = await body(await evaluator.api.get(`/api/v1/processes/${flow.process.id}/indicators/${indicator.id}/manual-academic-evaluations`));
    expect(evaluations.items).toEqual([]);
  }
};
handlers['AC-P09-01'] = handlers['AC-X06'];
handlers['AC-P08-03'] = async () => {
  const flow = await documentary(); const prepared = await draft(flow);
  const results = await Promise.all([1, 2].map(() => collector.api.post(
    `/api/v1/evidence-versions/${prepared.version.id}/submit`, {
      headers: {...collector.headers, 'If-Match': prepared.etag}, data: {}})));
  expect(results.map(r => r.status()).sort()).toEqual([200, 412]);
  const version = await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}`));
  expect(version.status).toBe('SUBMITTED'); expect(version.revisionNumber).toBe(1);
  const inbox = await body(await reviewer.api.get(`/api/v1/processes/${flow.process.id}/document-review/inbox`));
  expect(inbox.filter((item: any) => item.versionId === prepared.version.id)).toHaveLength(1);
};
handlers['AC-P10-01'] = async () => {
  const flow = await documentary(); const prepared = await draft(flow); await submit(prepared);
  const original = await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}`));
  const rejected = await decision(prepared.version.id, [{requiredSourceId: flow.sources[0].id,
    outcome: 'REJECTED', note: 'Documento sintetico requiere correccion fundamentada'}]);
  const correction = await body(await collector.api.post(`/api/v1/document-review-decisions/${rejected.id}/correction`, {
    headers: {...collector.headers, 'If-Match': '"0"'}}), 201);
  const successor = await body(await collector.api.get(`/api/v1/evidence-versions/${correction.successorVersionId}`));
  expect(successor.predecessorId).toBe(original.id); expect(successor.status).toBe('DRAFT');
  expect((await body(await reviewer.api.get(`/api/v1/evidence-versions/${successor.id}/document-review`))).decision).toBe('PENDING_REVIEW');
  await submit({version: successor, etag: '"0"'});
  const accepted = await decision(successor.id, [{requiredSourceId: flow.sources[0].id, outcome: 'ACCEPTED'}]);
  expect(accepted.decision).toBe('ACCEPTED');
  expect(await body(await collector.api.get(`/api/v1/evidence-versions/${original.id}`))).toEqual(original);
  expect((await body(await reviewer.api.get(`/api/v1/evidence-versions/${original.id}/document-review`))).decision).toBe('REJECTED');
};

async function processHeaders(flow: any) {
  const current = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}`));
  return {...coordinator.headers, 'If-Match': etag(current.revision)};
}

async function transition(flow: any, target: string) {
  return body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/transitions`, {
    headers: await processHeaders(flow), data: {target, authorityAssignmentId: authorityId,
      legalBasis: 'Politica sintetica de aceptacion', documentReference: 'fixture://transition',
      reason: 'Decision sintetica fundamentada para verificar estados'}
  }));
}

async function progress(flow: any) {
  return body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/documentary-progress`));
}

async function accepted(flow: any, source = flow.sources[0]) {
  const prepared = await draft(flow, [source]); await submit(prepared);
  const review = await decision(prepared.version.id, [{requiredSourceId: source.id, outcome: 'ACCEPTED'}]);
  return {prepared, review};
}

async function exclusion(flow: any, source: any) {
  return body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/applicability-decisions`, {
    headers: {...coordinator.headers, 'If-Match': '"0"'}, data: {subjectType: 'REQUIRED_SOURCE', subjectId: source.id,
      decision: 'NOT_APPLICABLE', ruleReference: 'Regla sintetica de exclusion', provenance: {
        authorityAssignmentId: authorityId, legalBasis: 'Politica sintetica', documentReference: 'fixture://exclusion',
        reason: 'Exclusion sintetica con justificacion y autoridad explicita'}}
  }), 201);
}

handlers['AC-P02-03'] = async () => {
  const process = await processFor(await publishedModel(), true);
  const career = await body(await admin.api.get(`/api/v1/careers/${careerId}`));
  const previous = await body(await coordinator.api.get(`/api/v1/processes/${process.id}`));
  expect((await admin.api.delete(`/api/v1/careers/${careerId}`, {headers: admin.headers})).status()).toBe(405);
  const disabled = await body(await admin.api.post(`/api/v1/careers/${careerId}/state-decisions`, {
    headers: {...admin.headers, 'If-Match': etag(career.revision)},
    data: {status: 'DISABLED', reason: 'Desactivacion sintetica de carrera referenciada'}
  }), 201);
  try {
    expect(disabled.status).toBe('DISABLED');
    expect(await body(await coordinator.api.get(`/api/v1/processes/${process.id}`))).toEqual(previous);
    const history = await body(await admin.api.get(`/api/v1/careers/${careerId}/history`));
    expect(history.items.some((h: any) => h.status === 'DISABLED')).toBe(true);
  } finally {
    await body(await admin.api.post(`/api/v1/careers/${careerId}/state-decisions`, {
      headers: {...admin.headers, 'If-Match': etag(disabled.revision)},
      data: {status: 'ACTIVE', reason: 'Restauracion sintetica de la carrera piloto tras la prueba'}
    }), 201);
  }
};

handlers['AC-P05-02'] = async () => {
  const version = await publishedModel(); const process = await processFor(version);
  const url = `/api/v1/processes/${process.id}`;
  const rejected = await body(await coordinator.api.post(`${url}/activate`, {
    headers: {...coordinator.headers, 'If-Match': etag(process.revision)}, data: {}}), 409);
  expect(rejected.code).toBe('AUTHORIZATION_MISSING');
  expect(await body(await coordinator.api.get(url))).toEqual(process);
  const draftVersion = await body(await admin.api.post(`/api/v1/model-versions/${version.id}/successors/configured`, {
    headers: admin.headers, data: {}}), 201);
  await body(await coordinator.api.post('/api/v1/processes', {headers: coordinator.headers, data: {careerId,
    code: unique(), name: 'Proceso con modelo borrador rechazado', periodStart: '2026-01-01', periodEnd: '2026-12-31',
    modelVersionId: draftVersion.id}}), 400);
  await body(await coordinator.api.post(`${url}/authorizations`, {headers: {...coordinator.headers,
    'If-Match': etag(process.revision)}, data: {authorityAssignmentId: authorityId,
    legalBasis: 'Politica sintetica', documentReference: 'fixture://authorization', reason: 'Decision sintetica documentada'}}), 201);
  await body(await admin.api.post(`/api/v1/model-versions/${version.id}/applicability-decisions`, {
    headers: {...admin.headers, 'If-Match': '"1"'}, data: {careerId, decision: 'NOT_APPLICABLE', validFrom: '2026-01-01',
      validUntil: '2026-12-31', provenance: {authorityFunction: 'Autoridad sintetica', legalBasis: 'Prueba de software',
        documentReference: 'fixture://not-applicable', reason: 'Inaplicabilidad sintetica explicita'}}
  }), 201);
  const before = await body(await coordinator.api.get(url));
  await body(await coordinator.api.post(`${url}/activate`, {headers: {...coordinator.headers,
    'If-Match': etag(before.revision)}, data: {}}), 409);
  expect(await body(await coordinator.api.get(url))).toEqual(before);
  expect(before.state).toBe('PREPARATION');
};
handlers['AC-X04'] = handlers['AC-P05-02'];

handlers['AC-P07-02'] = async () => {
  const flow = await documentary(); const prepared = await draft(flow);
  const url = `/api/v1/evidence-versions/${prepared.version.id}`;
  const before = await body(await collector.api.get(url));
  await body(await collector.api.post(`${url}/source-coverages`, {headers: {...collector.headers, 'If-Match': prepared.etag},
    data: {requiredSourceId: randomUUID(), coverageNote: 'Intento de fuente ajena rechazado'}}), 422);
  await body(await collector.api.post(`${url}/external-references`, {headers: {...collector.headers, 'If-Match': prepared.etag},
    data: {uri: 'file:///synthetic-private-document', description: 'URI local no permitida'}}), 400);
  const oversized = await collector.api.post(`${url}/files`, {headers: {...collector.headers, 'If-Match': prepared.etag},
    multipart: {file: {name: 'oversized-synthetic.pdf', mimeType: 'application/pdf', buffer: Buffer.alloc(26214401)}}});
  expect(oversized.status(), await oversized.text()).toBe(413);
  expect(await body(await collector.api.get(url))).toEqual(before);
};

handlers['AC-P08-02'] = async () => {
  const flow = await documentary(); const version = await emptyDraft(flow);
  const url = `/api/v1/evidence-versions/${version.id}`;
  const beforeEmpty = await body(await collector.api.get(url));
  await body(await collector.api.post(`${url}/submit`, {headers: {...collector.headers, 'If-Match': '"0"'}, data: {}}), 409);
  expect(await body(await collector.api.get(url))).toEqual(beforeEmpty);
  const complete = await draft(flow); await transition(flow, 'SUSPENDED');
  const before = await body(await collector.api.get(`/api/v1/evidence-versions/${complete.version.id}`));
  await body(await collector.api.post(`/api/v1/evidence-versions/${complete.version.id}/submit`, {
    headers: {...collector.headers, 'If-Match': complete.etag}, data: {}}), 409);
  expect(await body(await collector.api.get(`/api/v1/evidence-versions/${complete.version.id}`))).toEqual(before);
};

handlers['AC-P09-02'] = async () => {
  const flow = await documentary(); const prepared = await draft(flow); await submit(prepared);
  await body(await collector.api.post(`/api/v1/evidence-versions/${prepared.version.id}/document-review`, {
    headers: {...collector.headers, 'If-Match': '"0"'}, data: {selfReviewException: false,
      justification: 'Intento de autorrevision sin excepcion', outcomes: [{requiredSourceId: flow.sources[0].id, outcome: 'ACCEPTED'}]}
  }), 403);
  expect((await body(await reviewer.api.get(`/api/v1/evidence-versions/${prepared.version.id}/document-review`))).decision).toBe('PENDING_REVIEW');
  expect((await progress(flow)).acceptedSourceTotal).toBe(0);
};

handlers['AC-P10-02'] = async () => {
  const flow = await documentary(); const prepared = await draft(flow); await submit(prepared);
  const rejected = await decision(prepared.version.id, [{requiredSourceId: flow.sources[0].id,
    outcome: 'REJECTED', note: 'Correccion sintetica necesaria'}]);
  const original = await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}`));
  await body(await collector.api.post(`/api/v1/document-review-decisions/${rejected.id}/correction`, {
    headers: {...collector.headers, 'If-Match': '"9"'}}), 412);
  await transition(flow, 'SUSPENDED');
  await body(await collector.api.post(`/api/v1/document-review-decisions/${rejected.id}/correction`, {
    headers: {...collector.headers, 'If-Match': '"0"'}}), 409);
  expect(await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}`))).toEqual(original);
};

handlers['AC-P10-03'] = async () => {
  const flow = await documentary(); const {prepared} = await accepted(flow);
  const original = await body(await collector.api.get(`/api/v1/evidence-versions/${prepared.version.id}`));
  expect((await progress(flow)).acceptedSourceTotal).toBe(1);
  const successor = await body(await collector.api.post(`/api/v1/evidence-versions/${original.id}/successor`, {headers: collector.headers}), 201);
  expect(successor.predecessorId).toBe(original.id);
  expect((await progress(flow)).acceptedSourceTotal).toBe(1);
  await submit({version: successor, etag: '"0"'});
  expect((await progress(flow)).acceptedSourceTotal).toBe(0);
  expect((await body(await reviewer.api.get(`/api/v1/evidence-versions/${successor.id}/document-review`))).decision).toBe('PENDING_REVIEW');
  expect(await body(await collector.api.get(`/api/v1/evidence-versions/${original.id}`))).toEqual(original);
};
handlers['AC-X07'] = handlers['AC-P10-03'];

handlers['AC-P11-02'] = async () => {
  const optional = await documentary(fixture => fixture.configuration.indicators.forEach((i: any) =>
    i.sources.forEach((s: any) => { s.required = false; })));
  expect(await progress(optional)).toMatchObject({state: 'NOT_CONFIGURED', requiredSourceTotal: 0,
    collectionPercent: null, documentReviewPercent: null});
  const excluded = await documentary();
  for (const source of excluded.sources) await exclusion(excluded, source);
  expect(await progress(excluded)).toMatchObject({state: 'NOT_APPLICABLE', requiredSourceTotal: 0,
    collectionPercent: null, documentReviewPercent: null});
};
handlers['AC-X09'] = handlers['AC-P11-02'];

handlers['AC-X14'] = async () => {
  const flow = await documentary(); const before = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}`));
  const rejected = await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/documentary-close`, {
    headers: await processHeaders(flow), data: {closurePolicyVersion: 'DOCUMENTARY_STRICT_V1',
      reason: 'Intento de cierre con pendientes documentales identificado'}}), 409);
  expect(rejected.code).toBe('CLOSURE_BLOCKED');
  expect(await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}`))).toEqual(before);
  const history = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/documentary-closures`));
  expect(history.items).toEqual([]);
};

handlers['AC-X16'] = async () => {
  const flow = await documentary(fixture => {
    fixture.configuration.indicators[0].sources[0].sourceType = 'STRUCTURED_DATA';
    fixture.configuration.indicators[0].sources[1].sourceType = 'INSTITUTIONAL_REFERENCE';
  });
  const version = await emptyDraft(flow); const url = `/api/v1/evidence-versions/${version.id}`;
  let match = '"0"';
  for (const [n, source] of flow.sources.slice(0, 2).entries()) {
    const coverage = await collector.api.post(`${url}/source-coverages`, {headers: {...collector.headers, 'If-Match': match},
      data: {requiredSourceId: source.id, coverageNote: 'Cobertura de snapshot tipado sintetico'}});
    await body(coverage, 201); match = coverage.headers()['etag'];
    const snapshot = await collector.api.post(`${url}/structured-sources`, {headers: {...collector.headers, 'If-Match': match},
      data: {requiredSourceId: source.id, kind: source.sourceType, value: n === 0 ? 37 : 'ACTA-SYNTHETIC',
        unit: n === 0 ? 'personas' : null, periodStart: '2026-01-01', periodEnd: '2026-06-30',
        reference: n === 0 ? null : 'fixture://institutional/ACTA-SYNTHETIC'}});
    await body(snapshot, 201); match = snapshot.headers()['etag'];
  }
  const before = await body(await collector.api.get(`${url}/structured-sources`));
  await submit({version, etag: match});
  expect(await body(await collector.api.get(`${url}/structured-sources`))).toEqual(before);
  expect(before).toContainEqual(expect.objectContaining({kind: 'STRUCTURED_DATA', value: 37, unit: 'personas'}));
  expect(before).toContainEqual(expect.objectContaining({kind: 'INSTITUTIONAL_REFERENCE', reference: 'fixture://institutional/ACTA-SYNTHETIC'}));
  expect((await body(await collector.api.get(url))).files).toEqual([]);
};

handlers['AC-P04-02'] = async () => {
  const version = await publishedModel();
  const successor = await body(await admin.api.post(`/api/v1/model-versions/${version.id}/successors/configured`, {
    headers: admin.headers, data: {}}), 201);
  const repeated = JSON.parse(JSON.stringify(successor));
  const indicatorNodes: any[] = [];
  const collect = (nodes: any[]) => { for (const node of nodes) {
    if (node.nodeType === 'INDICATOR') indicatorNodes.push(node); collect(node.children);
  } };
  collect(repeated.criteria); indicatorNodes[1].code = indicatorNodes[0].code;
  const edited = await body(await admin.api.put(`/api/v1/model-versions/${successor.id}/configured`, {
    headers: {...admin.headers, 'If-Match': etag(successor.revision)}, data: {source: successor.source,
      nodes: repeated.criteria, configuration: successor.configuration}}));
  const rejected = await body(await coordinator.api.post(`/api/v1/model-versions/${successor.id}/publish-configured`, {
    headers: {...coordinator.headers, 'If-Match': etag(edited.revision)}, data: {approverId: coordinator.id}}), 400);
  expect(JSON.stringify(rejected.details)).toMatch(/DUPLICATE/);
  expect((await body(await admin.api.get(`/api/v1/model-versions/${successor.id}/configured`))).status).toBe('DRAFT');
  expect(await body(await admin.api.get(`/api/v1/model-versions/${version.id}/configured`))).toEqual(version);
};
handlers['AC-X05'] = async () => { await handlers['AC-P04-01'](); await handlers['AC-P04-02'](); };

handlers['AC-P04-03'] = async () => {
  const pending = await body(await admin.api.post('/api/v1/normative-references', {headers: admin.headers,
    data: {id: randomUUID(), title: 'Fuente con aplicabilidad por aclarar', issuer: 'Prueba sintetica', edition: '2026',
      documentReference: 'fixture://pending-methodology', verificationStatus: 'PENDING_VALIDATION'}}), 201);
  const version = await publishedModel(0, true, fixture => {
    for (const indicator of fixture.configuration.indicators) indicator.normativeReferences = [pending];
  }, false);
  const snapshot = await body(await admin.api.get(`/api/v1/model-versions/${version.id}/configured`));
  expect(snapshot.configuration.indicators[0].normativeReferences).toContainEqual(expect.objectContaining({
    id: pending.id, verificationStatus: 'PENDING_VALIDATION'}));
  const attempt = await admin.api.post(`/api/v1/model-versions/${version.id}/applicability-decisions`, {
    headers: {...admin.headers, 'If-Match': '"0"'}, data: {careerId, decision: 'APPLICABLE', validFrom: '2026-01-01',
      validUntil: '2026-12-31', provenance: {authorityFunction: 'Autoridad sintetica', legalBasis: 'Prueba de software',
        documentReference: 'fixture://unresolved-applicability', reason: 'Intento de aplicar fuente pendiente de revision'}}});
  expect(attempt.status(), await attempt.text()).toBe(412);
  expect(await body(await admin.api.get(`/api/v1/model-versions/${version.id}/applicability-decisions`))).toEqual([]);
  expect((await body(await admin.api.get(`/api/v1/model-versions/${version.id}/configured`))).configuration.indicators[0]
    .normativeReferences[0].verificationStatus).toBe('PENDING_VALIDATION');
};

handlers['AC-P11-03'] = async () => {
  const flow = await documentary(fixture => { fixture.configuration.indicators[0].sources[0].cardinalityMin = 2; });
  await accepted(flow);
  expect(await progress(flow)).toMatchObject({registeredSourceTotal: 1, acceptedSourceTotal: 0});
  const second = await accepted(flow);
  expect(await progress(flow)).toMatchObject({registeredSourceTotal: 1, acceptedSourceTotal: 1});
  const snapshot = await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/documentary-snapshots`, {
    headers: await processHeaders(flow), data: {reason: 'Corte historico con cardinalidad y fuentes distintas'}}), 201);
  await body(await reviewer.api.post(`/api/v1/evidence-versions/${second.prepared.version.id}/document-review`, {
    headers: {...reviewer.headers, 'If-Match': '"1"'}, data: {selfReviewException: false,
      justification: 'Rectificacion documental sintetica fundamentada', outcomes: [{requiredSourceId: flow.sources[0].id,
        outcome: 'OBSERVED', note: 'Nueva revision documental requerida'}]}}), 201);
  expect(await progress(flow)).toMatchObject({registeredSourceTotal: 1, acceptedSourceTotal: 0});
  expect(await body(await coordinator.api.get(`/api/v1/documentary-snapshots/${snapshot.data.id}`))).toEqual(snapshot);
};

handlers['AC-P09-03'] = async () => {
  const flow = await documentary(); const {prepared, review} = await accepted(flow);
  const indicator = flow.model.configuration.indicators[0];
  const path = `/api/v1/processes/${flow.process.id}/indicators/${indicator.nodeId}/manual-academic-evaluations`;
  const current = await body(await evaluator.api.get(path));
  const judgement = await body(await evaluator.api.post(path, {headers: {...evaluator.headers, 'If-Match': '"0"'},
    data: {ratingOptionId: current.ratingOptions[0].id, evidenceVersionIds: [prepared.version.id],
      justification: 'Juicio sintetico previo a la rectificacion documental', limitations: 'Alcance limitado a prueba de software', predecessorId: null}}), 201);
  const revised = await body(await reviewer.api.post(`/api/v1/evidence-versions/${prepared.version.id}/document-review`, {
    headers: {...reviewer.headers, 'If-Match': '"1"'}, data: {selfReviewException: false,
      justification: 'Rectificacion sintetica con conservacion de predecesora', outcomes: [{requiredSourceId: flow.sources[0].id,
        outcome: 'OBSERVED', note: 'Sustento requiere nueva comprobacion documental'}]}}), 201);
  expect(revised.predecessorId).toBe(review.id);
  const history = await body(await reviewer.api.get(`/api/v1/processes/${flow.process.id}/document-review-decisions`));
  expect(history.some((d: any) => d.id === review.id && d.decision === 'ACCEPTED')).toBe(true);
  const after = await body(await evaluator.api.get(path));
  expect(after.items.find((j: any) => j.id === judgement.id)).toEqual(judgement);
  expect(after.acceptedSupports).toEqual([]);
};

handlers['AC-P01-03'] = async () => {
  const username = unique();
  const user = await body(await admin.api.post('/api/v1/users', {headers: admin.headers,
    data: {username, displayName: 'Asignacion sintetica revocable', initialPassword: 'Synthetic-User-2026', technical: false}}), 201);
  const role = await body(await admin.api.post(`/api/v1/users/${user.id}/assignments`, {headers: admin.headers,
    data: {roleCode: 'RESPONSIBLE', careerId, validFrom: '2020-01-01T00:00:00Z', validUntil: '2030-01-01T00:00:00Z'}}), 201);
  const api = await apiFactory(); contexts.push(api);
  const session = await body(await api.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201);
  expect((await body(await api.get(`/api/v1/careers/${careerId}`))).id).toBe(careerId);
  const revoke = `/api/v1/users/${user.id}/assignments/${role.id}/revocations`;
  await body(await admin.api.post(revoke, {headers: {...admin.headers, 'If-Match': '"9"'},
    data: {reason: 'Revision obsoleta de revocacion rechazada'}}), 412);
  const revoked = await body(await admin.api.post(revoke, {headers: {...admin.headers, 'If-Match': '"0"'},
    data: {reason: 'Retiro sintetico de competencia con sesion abierta'}}));
  expect(revoked).toMatchObject({id: role.id, status: 'REVOKED', version: 1});
  expect((await api.get(`/api/v1/careers/${careerId}`)).status()).toBe(404);
  const identity = await body(await api.get('/api/v1/me'));
  expect(identity.id).toBe(session.userId); expect(identity.assignments).toEqual([]);
  const audit = await body(await admin.api.get(`/api/v1/audit-events?objectId=${role.id}`));
  expect(audit.items.some((event: any) => event.action === 'ASSIGNMENT_REVOKED')).toBe(true);
};

async function activity(flow: any, options: Record<string, unknown> = {}) {
  const assignments = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/assignments`));
  const assigned = assignments.find((a: any) => a.function === 'RESPONSIBLE' && a.indicatorId === flow.sources[0].indicatorNodeId);
  const data = {title: unique(), indicatorId: flow.sources[0].indicatorNodeId, requiredSourceId: flow.sources[0].id,
    assignmentId: assigned.id, dueAt: '2026-11-20T12:00:00Z', dependencyIds: [], ...options};
  const item = await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/activities`, {
    headers: await processHeaders(flow), data}), 201);
  return {item, data};
}

handlers['AC-P06-01'] = async () => {
  const flow = await documentary();
  const role = (await body(await admin.api.get(`/api/v1/users/${collector.id}/assignments`)))
    .find((r: any) => r.roleCode === 'RESPONSIBLE');
  const team = await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/teams`, {
    headers: await processHeaders(flow), data: {name: unique(), processRole: 'RESPONSIBLE',
      validFrom: '2026-01-01T00:00:00Z', validUntil: '2026-12-31T00:00:00Z'}}), 201);
  const member = await body(await coordinator.api.post(`/api/v1/teams/${team.id}/memberships`, {
    headers: await processHeaders(flow), data: {userId: collector.id, roleAssignmentId: role.id,
      validFrom: '2026-01-01T00:00:00Z', validUntil: '2026-12-31T00:00:00Z'}}), 201);
  expect(member.roleAssignmentId).toBe(role.id);
  const assignment = await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/assignments`, {
    headers: await processHeaders(flow), data: {indicatorId: flow.sources[0].indicatorNodeId, userId: null, teamId: team.id,
      function: 'RESPONSIBLE', validFrom: '2026-01-01T00:00:00Z', validTo: '2026-12-31T00:00:00Z',
      reason: 'Asignacion de equipo sintetico con membresia vigente'}}), 201);
  const task = await activity(flow, {assignmentId: assignment.id});
  expect(task.item.requiredSourceId).toBe(flow.sources[0].id);
  expect((await body(await collector.api.get(`/api/v1/processes/${flow.process.id}/activities`))).some((a: any) => a.id === task.item.id)).toBe(true);
  expect((await body(await reviewer.api.get(`/api/v1/processes/${flow.process.id}/activities`))).some((a: any) => a.id === task.item.id)).toBe(false);
};

handlers['AC-P06-02'] = async () => {
  const flow = await documentary(); const before = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/assignments`));
  await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/assignments`, {headers: await processHeaders(flow),
    data: {indicatorId: flow.sources[0].indicatorNodeId, identityId: collector.id, function: 'REVIEWER',
      validFrom: '2026-01-01T00:00:00Z', validTo: '2026-12-31T00:00:00Z', reason: 'Conflicto sintetico responsable y revisor'}}), 409);
  expect(await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/assignments`))).toEqual(before);
  const first = await activity(flow); const second = await activity(flow, {dependencyIds: [first.item.id]});
  const cyclic = await body(await coordinator.api.patch(`/api/v1/activities/${first.item.id}`, {
    headers: await processHeaders(flow), data: {...first.data, dependencyIds: [second.item.id]}}), 409);
  expect(cyclic.code).toBe('DEPENDENCY_CYCLE');
  expect(await body(await coordinator.api.get(`/api/v1/activities/${first.item.id}`))).toEqual(first.item);
};

handlers['AC-P06-03'] = async () => {
  const flow = await documentary(); const first = await activity(flow, {dueAt: '2026-01-20T12:00:00Z'});
  const tasks = await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/assignments`));
  const other = tasks.find((a: any) => a.function === 'REVIEWER' && a.indicatorId === flow.sources[0].indicatorNodeId);
  const changed = await body(await coordinator.api.patch(`/api/v1/activities/${first.item.id}`, {
    headers: await processHeaders(flow), data: {...first.data, assignmentId: other.id, dueAt: '2026-11-30T12:00:00Z'}}));
  expect(changed.revision).toBe(first.item.revision + 1); expect(changed.assignmentId).toBe(other.id);
  expect((await body(await collector.api.get(`/api/v1/processes/${flow.process.id}/activities`))).some((a: any) => a.id === first.item.id)).toBe(false);
  expect((await body(await reviewer.api.get(`/api/v1/processes/${flow.process.id}/activities`))).some((a: any) => a.id === first.item.id)).toBe(true);
  const audit = await body(await admin.api.get(`/api/v1/audit-events?objectId=${flow.process.id}&size=100`));
  expect(audit.items.map((event: any) => event.action)).toEqual(expect.arrayContaining(['PROCESS_ACTIVITY_CREATED', 'PROCESS_ACTIVITY_UPDATED']));
};

const syntheticPdf = Buffer.from('%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF');
async function uploaded(flow: any) {
  const version = await emptyDraft(flow); const url = `/api/v1/evidence-versions/${version.id}`;
  const coverage = await collector.api.post(`${url}/source-coverages`, {headers: {...collector.headers, 'If-Match': '"0"'},
    data: {requiredSourceId: flow.sources[0].id, coverageNote: 'Cobertura sintetica de archivo'}});
  await body(coverage, 201);
  const response = await collector.api.post(`${url}/files`, {headers: {...collector.headers, 'If-Match': coverage.headers()['etag']},
    multipart: {file: {name: 'synthetic.pdf', mimeType: 'application/pdf', buffer: syntheticPdf}}});
  const file = await body(response, 201);
  return {version, file, etag: response.headers()['etag']};
}

handlers['AC-P07-03'] = async () => {
  const flow = await documentary(fixture => { fixture.configuration.indicators[0].sources[0].sourceType = 'FILE'; });
  const first = await uploaded(flow); const second = await uploaded(flow);
  expect(first.file.sha256).toBe(second.file.sha256);
  await submit(first); await submit(second);
  expect(await progress(flow)).toMatchObject({requiredSourceTotal: 4, registeredSourceTotal: 1, collectionPercent: 25});
  for (const item of [first, second]) {
    const download = await collector.api.get(`/api/v1/evidence-versions/${item.version.id}/files/${item.file.id}/content`);
    expect(download.status()).toBe(200); expect(await download.body()).toEqual(syntheticPdf);
    expect(download.headers()['x-content-sha256']).toBe(item.file.sha256);
  }
};

handlers['AC-X01'] = async () => {
  const flow = await documentary(fixture => { fixture.configuration.indicators[0].sources[0].sourceType = 'FILE'; });
  const item = await uploaded(flow); await submit(item);
  const api = await apiFactory(); contexts.push(api);
  const session = await body(await api.post('/api/v1/sessions', {data: {username: 'perf001', password: 'Synthetic-Perf-2026'}}), 201);
  const known = `/api/v1/evidence-versions/${item.version.id}`;
  const hidden = await body(await api.get(known), 404);
  const absent = await body(await api.get(`/api/v1/evidence-versions/${randomUUID()}`), 404);
  expect(hidden.code).toBe(absent.code);
  await body(await api.get(`${known}/files/${item.file.id}/content`), 404);
  await body(await api.patch(known, {headers: {'X-CSRF-TOKEN': session.csrfToken, 'If-Match': item.etag},
    data: {title: 'Alteracion ajena rechazada', description: 'Intento sintetico', provenance: 'Fixture',
      coveredFrom: '2026-01-01', coveredTo: '2026-06-30', indicatorIds: [flow.sources[0].indicatorNodeId]}}), 404);
  const list = await body(await api.get(`/api/v1/evidences?processId=${flow.process.id}`));
  expect(list.items).toEqual([]); expect(list.page.totalElements).toBe(0);
  await body(await api.get(`/api/v1/processes/${flow.process.id}/documentary-progress`), 404);
  expect((await body(await collector.api.get(known))).title).toBe(item.version.title);
};

handlers['AC-X10'] = async () => {
  const model = await publishedModel(); const process = await processFor(model, true);
  const indicator = model.configuration.indicators.find((i: any) => i.nature === 'QUANTITATIVE');
  const before = await body(await admin.api.get(`/api/v1/model-versions/${model.id}/configured`));
  const failed = await body(await evaluator.api.post(`/api/v1/processes/${process.id}/indicators/${indicator.nodeId}/quantitative-calculations`, {
    headers: {...evaluator.headers, 'If-Match': '"0"'}, data: {inputSetId: randomUUID(),
      justification: 'Intento sintetico de calculo con regla no validada', limitations: 'Regla pendiente de validacion'}}), 422);
  expect(failed.code).toBe('RULE_NOT_VALIDATED');
  expect(await body(await admin.api.get(`/api/v1/model-versions/${model.id}/configured`))).toEqual(before);
};

async function functionalAssignment(actor: Actor, code: string) {
  return (await body(await admin.api.get(`/api/v1/users/${actor.id}/assignments`)))
    .find((r: any) => r.roleCode === code).id as string;
}

async function syntheticActor(flow: any, roleCode: string) {
  const username = unique();
  const user = await body(await admin.api.post('/api/v1/users', {headers: admin.headers,
    data: {username, displayName: 'Actor sintetico de aceptacion', initialPassword: 'Synthetic-User-2026', technical: false}}), 201);
  const assignment = await body(await admin.api.post(`/api/v1/users/${user.id}/assignments`, {headers: admin.headers,
    data: {roleCode, careerId, processId: flow.process.id, validFrom: '2020-01-01T00:00:00Z', validUntil: '2030-01-01T00:00:00Z'}}), 201);
  const api = await apiFactory(); contexts.push(api);
  const session = await body(await api.post('/api/v1/sessions', {data: {username, password: 'Synthetic-User-2026'}}), 201);
  return {api, id: user.id, headers: {'X-CSRF-TOKEN': session.csrfToken}, assignmentId: assignment.id};
}

const academicPath = (flow: any) => `/api/v1/processes/${flow.process.id}/indicators/${flow.sources[0].indicatorNodeId}`;
async function judgement(flow: any, versionId: string, actor = evaluator, option = 0) {
  const url = `${academicPath(flow)}/manual-academic-evaluations`;
  const current = await body(await actor.api.get(url));
  return body(await actor.api.post(url, {headers: {...actor.headers, 'If-Match': etag(current.ownRevision)}, data: {
    ratingOptionId: current.ratingOptions[option].id, evidenceVersionIds: [versionId],
    justification: 'Fundamento academico exclusivamente sintetico', limitations: 'Alcance limitado a la aceptacion de software',
    predecessorId: current.ownLatestId}}), 201);
}

async function quantitative() {
  const flow = await documentary(fixture => {
    const indicator = fixture.configuration.indicators[0]; indicator.nature = 'QUANTITATIVE';
    const rule = fixture.configuration.rules.find((r: any) => r.id === indicator.valuationRuleId);
    const scale = fixture.configuration.scales.find((s: any) => s.id === indicator.ratingScaleId);
    Object.assign(rule, {ruleType: 'QUANTITATIVE', formulaDefinition: 'A / B * 100', strategyKey: 'ratio-percent',
      strategyVersion: '1', missingDataPolicy: 'REJECT', variables: ['A', 'B'].map((code, ordinal) => ({
        code, name: `Cantidad ${code}`, dataType: 'INTEGER', unit: 'personas', required: true,
        validationDefinition: ordinal ? 'POSITIVE' : 'NON_NEGATIVE', ordinal: ordinal + 1})),
      thresholds: [{lowerBound: '0', upperBound: '50', lowerInclusive: true, upperInclusive: false, ratingOptionId: scale.options[1].id},
        {lowerBound: '50', upperBound: null, lowerInclusive: true, upperInclusive: false, ratingOptionId: scale.options[0].id}]});
  });
  const {prepared} = await accepted(flow);
  const data = {periodStart: '2026-01-01', periodEnd: '2026-06-30', predecessorId: null,
    variables: {A: {value: '3', unit: 'personas', evidenceVersionIds: [prepared.version.id]},
      B: {value: '4', unit: 'personas', evidenceVersionIds: [prepared.version.id]}}};
  return {flow, prepared, data, url: academicPath(flow)};
}

handlers['AC-P12-01'] = async () => {
  const manual = await documentary(); const support = await accepted(manual);
  const saved = await judgement(manual, support.prepared.version.id);
  expect(saved.supports.map((s: any) => s.evidenceVersionId)).toEqual([support.prepared.version.id]);
  expect(saved.evaluatorId ?? saved.createdBy ?? saved.authorId).toBeTruthy();
  const {flow, data, url} = await quantitative();
  const first = await body(await evaluator.api.post(`${url}/quantitative-inputs`, {headers: {...evaluator.headers, 'If-Match': '"0"'}, data}), 201);
  const command = {inputSetId: first.id, justification: 'Calculo sintetico con variables verificadas', limitations: 'Prueba sin validez institucional'};
  await body(await evaluator.api.post(`${url}/quantitative-calculations`, {headers: {...evaluator.headers, 'If-Match': '"1"'}, data: command}), 422);
  await body(await evaluator.api.post(`${url}/quantitative-inputs/${first.id}/verification`, {
    headers: {...evaluator.headers, 'If-Match': '"1"'}, data: {reason: 'Variables, unidades y fuentes contrastadas explicitamente'}}), 201);
  const run = await body(await evaluator.api.post(`${url}/quantitative-calculations`, {headers: {...evaluator.headers, 'If-Match': '"1"'}, data: command}), 201);
  expect(run.result).toBe('75'); expect(run.inputs).toEqual(first); expect(run.rule.formulaDefinition).toBe('A / B * 100');
  expect(run.executedBy).toBe(evaluator.id);
  const second = await body(await evaluator.api.post(`${url}/quantitative-inputs`, {headers: {...evaluator.headers, 'If-Match': '"1"'},
    data: {...data, predecessorId: first.id, variables: {...data.variables, A: {...data.variables.A, value: '2'}}}}), 201);
  expect(second.predecessorId).toBe(first.id);
  await body(await evaluator.api.post(`${url}/quantitative-inputs/${second.id}/verification`, {
    headers: {...evaluator.headers, 'If-Match': '"2"'}, data: {reason: 'Segunda revision sintetica contrastada explicitamente'}}), 201);
  const corrected = await body(await evaluator.api.post(`${url}/quantitative-calculations`, {headers: {...evaluator.headers, 'If-Match': '"2"'}, data: {...command, inputSetId: second.id}}), 201);
  expect(corrected.result).toBe('50');
  expect((await body(await evaluator.api.get(`${url}/quantitative-evaluations`))).items.find((r: any) => r.id === run.id)).toEqual(run);
  expect(await body(await evaluator.api.get(`${academicPath(flow)}/quantitative-inputs/${first.id}`))).toEqual(first);
};

handlers['AC-P12-02'] = async () => {
  const {data, url} = await quantitative();
  await body(await evaluator.api.post(`${url}/quantitative-inputs`, {headers: {...evaluator.headers, 'If-Match': '"0"'},
    data: {...data, variables: {A: data.variables.A}}}), 400);
  await body(await evaluator.api.post(`${url}/quantitative-inputs`, {headers: {...evaluator.headers, 'If-Match': '"0"'},
    data: {...data, variables: {...data.variables, A: {...data.variables.A, unit: 'horas'}}}}), 422);
  expect((await body(await evaluator.api.get(`${url}/quantitative-evaluations`))).items).toEqual([]);
  const flow = await documentary(); const {prepared} = await accepted(flow);
  await body(await evaluator.api.post(`${academicPath(flow)}/manual-academic-evaluations`, {
    headers: {...evaluator.headers, 'If-Match': '"0"'}, data: {ratingOptionId: randomUUID(), evidenceVersionIds: [prepared.version.id],
      justification: 'Escala ajena sintetica que debe rechazarse', limitations: 'Sin resultado academico valido', predecessorId: null}}), 422);
  await handlers['AC-X10']();
};

handlers['AC-P12-03'] = async () => {
  const flow = await documentary(); const {prepared} = await accepted(flow);
  const two = await syntheticActor(flow, 'EVALUATOR'); const committee = await syntheticActor(flow, 'ACCEPTANCE_COMMITTEE');
  const first = await judgement(flow, prepared.version.id); const second = await judgement(flow, prepared.version.id, two, 1);
  const url = academicPath(flow);
  const disagreement = await body(await two.api.post(`${url}/academic-disagreements`, {headers: {...two.headers, 'If-Match': '"0"'},
    data: {reason: 'Discrepancia explicita entre fundamentos sinteticos'}}), 201);
  const policy = await body(await committee.api.post(`${url}/academic-reconciliation-policies`, {headers: {...committee.headers, 'If-Match': '"0"'},
    data: {procedureReference: 'Procedimiento sintetico version 1', method: 'SELECT_EXISTING_JUDGEMENT_V1', legalBasis: 'Competencia sintetica de conciliacion',
      documentReference: 'fixture://reconciliation', authorityAssignmentId: committee.assignmentId}}), 201);
  await body(await committee.api.post(`${url}/academic-disagreements/${disagreement.id}/resolution`, {
    headers: {...committee.headers, 'If-Match': etag(disagreement.revision)}, data: {policyId: policy.id, selectedJudgementId: first.id,
      participants: [evaluator.id, two.id, committee.id], reason: 'Resolucion sintetica conforme al procedimiento registrado', authorityAssignmentId: committee.assignmentId}}), 201);
  const resolved = await body(await committee.api.get(`${url}/academic-reconciliation`));
  expect(resolved.resolution.selectedJudgement.id).toBe(first.id);
  await body(await reviewer.api.post(`/api/v1/evidence-versions/${prepared.version.id}/document-review`, {
    headers: {...reviewer.headers, 'If-Match': '"1"'}, data: {selfReviewException: false, justification: 'Rectificacion documental sintetica exige revisar sustentos',
      outcomes: [{requiredSourceId: flow.sources[0].id, outcome: 'OBSERVED', note: 'Fuente sintetica requiere comprobacion adicional'}]}}), 201);
  const changed = await body(await committee.api.get(`${url}/academic-reconciliation`));
  expect(changed.status).toBe('REQUIRES_REVIEW'); expect(changed.result).toBeNull();
  expect(changed.resolution).toEqual(resolved.resolution);
  expect(changed.current.every((j: any) => j.flags.includes('SUPPORTS_INVALIDATED'))).toBe(true);
  for (const item of [first, second]) expect((await body(await evaluator.api.get(`${url}/manual-academic-evaluations`))).items.find((j: any) => j.id === item.id)).toEqual(item);
};
handlers['AC-X11'] = () => handlers['AC-P12-03']();

const provenance = (assignment: string) => ({authorityAssignmentId: assignment,
  legalBasis: 'Competencia sintetica registrada para aceptacion', documentReference: 'fixture://governance',
  reason: 'Decision sintetica fundamentada sin validez institucional'});
async function finding(flow: any, support: any, evaluation: any, type = 'WEAKNESS') {
  return body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/findings`, {
    headers: {...coordinator.headers, 'If-Match': '"0"'}, data: {type, title: unique(),
      description: 'Hallazgo sintetico vinculado a soportes y valoracion exactos', indicatorIds: [flow.sources[0].indicatorNodeId],
      evaluationIds: [evaluation.id], evidenceVersionIds: [support.version.id]}}), 201);
}
async function consolidation(flow: any, findings: any[], revision = 0) {
  return body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/consolidations`, {
    headers: {...coordinator.headers, 'If-Match': etag(revision)}, data: {findingIds: findings.map(f => f.id), aggregationRuleId: null,
      reason: 'Consolidacion sintetica por indicador sin promedio global'}}), 201);
}
async function findingsFlow() {
  const flow = await documentary(); const {prepared} = await accepted(flow);
  const evaluation = await judgement(flow, prepared.version.id);
  const weakness = await finding(flow, prepared, evaluation); const strength = await finding(flow, prepared, evaluation, 'STRENGTH');
  const consolidated = await consolidation(flow, [weakness, strength]);
  return {...flow, prepared, evaluation, weakness, strength, consolidated};
}
async function issueReport(flow: any, predecessor?: any, validated = true) {
  const revision = predecessor ? (await body(await coordinator.api.get(`/api/v1/report-versions/${predecessor.id}`))).revision : 0;
  return body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/report-versions`, {
    headers: {...coordinator.headers, 'If-Match': etag(revision)}, data: {kind: predecessor ? 'FINAL' : 'PRELIMINARY',
      consolidationId: flow.consolidated.id, templateReference: 'Plantilla sintetica version 1', templateValidated: validated,
      content: `Informe sintetico ${unique()}: no constituye acreditacion.`, predecessorVersionId: predecessor?.id ?? null}}), 201);
}
async function reportDecision(version: any, decisionCode: string) {
  const url = `/api/v1/report-versions/${version.id}`; const current = await body(await coordinator.api.get(url));
  await body(await coordinator.api.post(`${url}/approval-decisions`, {headers: {...coordinator.headers, 'If-Match': etag(current.revision)},
    data: {decision: decisionCode, provenance: provenance(await functionalAssignment(coordinator, 'COORDINATOR'))}}), 201);
  return body(await coordinator.api.get(url));
}
async function reportFlow(returnFirst = false) {
  const flow = await findingsFlow(); const preliminary = await issueReport(flow);
  const url = `/api/v1/report-versions/${preliminary.id}`;
  await body(await coordinator.api.post(`${url}/socializations`, {headers: {...coordinator.headers, 'If-Match': '"0"'},
    data: {occurredAt: '2026-10-20T10:00:00Z', participantAssignmentIds: [await functionalAssignment(reviewer, 'REVIEWER')], recordReference: 'fixture://socialization'}}), 201);
  const observation = await body(await reviewer.api.post(`${url}/observations`, {headers: {...reviewer.headers, 'If-Match': '"1"'},
    data: {sectionReference: 'Seccion de resultados', text: 'Se solicita ampliar la explicacion del hallazgo sintetico'}}), 201);
  await body(await coordinator.api.post(`/api/v1/report-observations/${observation.id}/responses`, {headers: {...coordinator.headers, 'If-Match': '"2"'},
    data: {response: 'Se incorpora la explicacion en la nueva version final sintetica', correctedVersionId: null}}), 201);
  let final = await issueReport(flow, preliminary);
  let returned;
  if (returnFirst) { returned = await reportDecision(final, 'RETURNED'); final = await issueReport(flow, returned); }
  const approved = await reportDecision(final, 'APPROVED');
  const preserved = await body(await coordinator.api.get(url));
  expect(preserved.data).toEqual(preliminary.data); expect(preserved.socializations).toHaveLength(1);
  expect(preserved.observations).toHaveLength(1); expect(preserved.replies).toHaveLength(1);
  expect(approved.decisions[0].provenance.authorityAssignmentId).toBe(await functionalAssignment(coordinator, 'COORDINATOR'));
  expect(approved.state).toBe('APPROVED');
  return {...flow, preliminary, approved, returned};
}

handlers['AC-P13-01'] = async () => {
  const flow = await findingsFlow();
  expect(flow.consolidated.data.findingIds).toEqual([flow.weakness.id, flow.strength.id]);
  const items = (await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/findings`))).items;
  for (const f of [flow.weakness, flow.strength]) {
    expect(items.find((item: any) => item.id === f.id)).toEqual(f);
    expect(f.data.evaluationIds).toEqual([flow.evaluation.id]); expect(f.data.evidenceVersionIds).toEqual([flow.prepared.version.id]);
  }
};
handlers['AC-P13-02'] = async () => {
  const flow = await findingsFlow();
  const rejected = await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/consolidations`, {
    headers: {...coordinator.headers, 'If-Match': '"1"'}, data: {findingIds: [flow.weakness.id], aggregationRuleId: randomUUID(),
      reason: 'Promedio sintetico sin regla metodologica autorizada'}}), 422);
  expect(rejected.code).toBe('ACADEMIC_AGGREGATION_RULE_UNVALIDATED');
  expect(flow.consolidated.data.aggregationRuleId).toBeNull(); expect(flow.consolidated).not.toHaveProperty('globalPercent');
  expect((await body(await evaluator.api.get(`${academicPath(flow)}/manual-academic-evaluations`))).items[0].id).toBe(flow.evaluation.id);
};
handlers['AC-P13-03'] = async () => {
  const flow = await findingsFlow(); const firstReport = await issueReport(flow);
  const corrected = await judgement(flow, flow.prepared.version.id, evaluator, 1);
  expect(corrected.predecessorId).toBe(flow.evaluation.id);
  const nextFinding = await finding(flow, flow.prepared, corrected);
  const next = await consolidation(flow, [nextFinding], 1); expect(next.revision).toBe(2);
  const report = await issueReport({...flow, consolidated: next}, firstReport);
  expect(report.data.consolidationId).toBe(next.id);
  expect((await body(await coordinator.api.get(`/api/v1/report-versions/${firstReport.id}`))).data.consolidationId).toBe(flow.consolidated.id);
  const history = await body(await evaluator.api.get(`${academicPath(flow)}/manual-academic-evaluations`));
  expect(history.items.find((j: any) => j.id === flow.evaluation.id)).toEqual(flow.evaluation);
};
handlers['AC-P14-01'] = async () => { await reportFlow(); };
handlers['AC-P14-02'] = async () => {
  const flow = await findingsFlow();
  await body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/report-versions`, {
    headers: {...coordinator.headers, 'If-Match': '"0"'}, data: {kind: 'PRELIMINARY', consolidationId: flow.consolidated.id,
      templateReference: 'Plantilla no validada', templateValidated: false, content: 'Informe sintetico que no debe emitirse', predecessorVersionId: null}}), 400);
  expect((await body(await coordinator.api.get(`/api/v1/processes/${flow.process.id}/report-versions`))).items).toEqual([]);
  const preliminary = await issueReport(flow); const final = await issueReport(flow, preliminary);
  expect(final.state).toBe('FINAL'); expect(final.decisions).toEqual([]);
  const returned = await reportDecision(final, 'RETURNED'); expect(returned.state).toBe('RETURNED');
  expect(returned.decisions.every((d: any) => d.decision !== 'APPROVED')).toBe(true);
};
handlers['AC-P14-03'] = async () => {
  const flow = await reportFlow(); const successor = await issueReport(flow, flow.approved);
  expect(successor.data.predecessorVersionId).toBe(flow.approved.id); expect(successor.state).toBe('FINAL'); expect(successor.decisions).toEqual([]);
  const previous = await body(await coordinator.api.get(`/api/v1/report-versions/${flow.approved.id}`));
  expect(previous.data).toEqual(flow.approved.data); expect(previous.decisions).toEqual(flow.approved.decisions); expect(previous.state).toBe('APPROVED');
  const approved = await reportDecision(successor, 'APPROVED'); expect(approved.decisions).toHaveLength(1);
};
handlers['AC-X12'] = async () => {
  const flow = await reportFlow(true); expect(flow.returned.state).toBe('RETURNED');
  expect(flow.approved.data.predecessorVersionId).toBe(flow.returned.id);
  expect((await body(await coordinator.api.get(`/api/v1/report-versions/${flow.returned.id}`))).decisions).toEqual(flow.returned.decisions);
};

async function improvementFlow(overdue = false) {
  const flow = await reportFlow(); const root = `/api/v1/processes/${flow.process.id}`;
  const plan = await body(await coordinator.api.post(`${root}/improvement-plans`, {headers: {...coordinator.headers, 'If-Match': '"0"'},
    data: {title: unique(), reportVersionId: flow.approved.id}}), 201);
  const responsible = await functionalAssignment(collector, 'RESPONSIBLE');
  const data = {planId: plan.id, findingIds: [flow.weakness.id], title: unique(), description: 'Accion sintetica con meta y medio de verificacion',
    responsibleAssignmentId: responsible, dueAt: overdue ? '2026-01-01T00:00:00Z' : '2027-02-01T00:00:00Z',
    target: 'Reducir la brecha sintetica al menos 50%', verificationMethod: 'Muestreo documental y contraste de avances'};
  const action = await body(await coordinator.api.post(`${root}/improvement-actions`, {headers: {...coordinator.headers, 'If-Match': '"0"'}, data}), 201);
  return {...flow, plan, action, actionData: data, responsible, actionUrl: `/api/v1/improvement-actions/${action.id}`};
}
async function improvementDecision(flow: any, actor: Actor, action: string, assignment: string, revision: number, status = 201) {
  return body(await actor.api.post(`${flow.actionUrl}/decisions`, {headers: {...actor.headers, 'If-Match': etag(revision)},
    data: {action, provenance: provenance(assignment)}}), status);
}
handlers['AC-P15-01'] = async () => {
  const flow = await improvementFlow(); const approval = await functionalAssignment(coordinator, 'COORDINATOR');
  const verifier = await functionalAssignment(reviewer, 'REVIEWER');
  await improvementDecision(flow, coordinator, 'APPROVE', approval, 1);
  await improvementDecision(flow, collector, 'START', flow.responsible, 2);
  await body(await collector.api.post(`${flow.actionUrl}/progress`, {headers: {...collector.headers, 'If-Match': '"3"'},
    data: {description: 'Avance sintetico sustentado para reducir la brecha', achievedValue: '50% de reduccion',
      evidenceVersionIds: [flow.prepared.version.id], recordedAt: '2026-10-20T10:00:00Z'}}), 201);
  await improvementDecision(flow, collector, 'SUBMIT_VERIFICATION', flow.responsible, 4);
  await improvementDecision(flow, reviewer, 'VERIFY', verifier, 5);
  const detail = await body(await reviewer.api.get(flow.actionUrl)); expect(detail.state).toBe('VERIFIED');
  expect(detail.progress[0].data.evidenceVersionIds).toEqual([flow.prepared.version.id]);
  expect(detail.decisions.map((d: any) => d.action)).toEqual(['APPROVE', 'START', 'SUBMIT_VERIFICATION', 'VERIFY']);
  expect(detail.data.findingIds).toEqual([flow.weakness.id]);
};
handlers['AC-P15-02'] = async () => {
  const flow = await improvementFlow(); const verifier = await functionalAssignment(reviewer, 'REVIEWER');
  await improvementDecision(flow, collector, 'START', flow.responsible, 1, 409);
  await improvementDecision(flow, reviewer, 'VERIFY', verifier, 1, 409);
  const detail = await body(await reviewer.api.get(flow.actionUrl)); expect(detail.state).toBe('PROPOSED'); expect(detail.decisions).toEqual([]);
  await improvementDecision(flow, coordinator, 'APPROVE', await functionalAssignment(coordinator, 'COORDINATOR'), 1);
  await improvementDecision(flow, collector, 'START', flow.responsible, 2);
  await improvementDecision(flow, collector, 'SUBMIT_VERIFICATION', flow.responsible, 3, 409);
  expect((await body(await reviewer.api.get(flow.actionUrl))).state).toBe('IN_PROGRESS');
};
handlers['AC-P15-03'] = async () => {
  const flow = await improvementFlow(true); expect(flow.action.overdue).toBe(true);
  const revised = await body(await coordinator.api.patch(flow.actionUrl, {headers: {...coordinator.headers, 'If-Match': '"1"'},
    data: {...flow.actionData, dueAt: '2027-02-01T00:00:00Z'}}));
  expect(revised.revision).toBe(2); expect(revised.overdue).toBe(false); expect(revised.state).toBe('PROPOSED');
  const audit = await body(await admin.api.get(`/api/v1/audit-events?objectId=${flow.action.id}&size=100`));
  expect(audit.items.map((e: any) => e.action)).toEqual(expect.arrayContaining(['IMPROVEMENT_ACTION_PROPOSED', 'IMPROVEMENT_ACTION_REVISED']));
};
handlers['AC-X13'] = () => handlers['AC-P15-03']();

async function documentaryClose(flow: any) {
  return body(await coordinator.api.post(`/api/v1/processes/${flow.process.id}/documentary-close`, {
    headers: await processHeaders(flow), data: {closurePolicyVersion: 'DOCUMENTARY_STRICT_V1', reason: 'Cierre documental sintetico con todas las fuentes aceptadas'}}), 201);
}
async function archiveFlow() {
  const flow = await reportFlow();
  for (const source of flow.sources.slice(1)) await accepted(flow, source);
  await documentaryClose(flow);
  const root = `/api/v1/processes/${flow.process.id}`;
  const close = await body(await coordinator.api.post(`${root}/close`, {headers: await processHeaders(flow),
    data: {closurePolicyVersion: 'INTEGRAL_STRICT_V1', exceptionDecisionIds: [], provenance: provenance(authorityId)}}), 201);
  const history = await body(await coordinator.api.get(`${root}/closures`));
  const closure = history.items.find((c: any) => c.type === 'INTEGRAL_CLOSURE');
  expect(closure.sha256).toMatch(/^[a-f0-9]{64}$/); expect(history.totalElements).toBe(2);
  const archive = await body(await coordinator.api.post(`${root}/archive`, {headers: await processHeaders(flow),
    data: {closureSnapshotId: closure.id, retentionPolicyVersion: 'RETENTION_10Y', provenance: provenance(authorityId)}}), 201);
  expect(archive.integrityStatus).toBe('VERIFIED'); expect(archive.closureSnapshotId).toBe(closure.id);
  return {...flow, close, closure, archive, root};
}
handlers['AC-P16-01'] = async () => {
  const flow = await archiveFlow();
  expect((await body(await coordinator.api.get(flow.root))).state).toBe('CLOSED');
  expect((await body(await coordinator.api.get(`${flow.root}/closures`))).items.find((c: any) => c.id === flow.closure.id)).toEqual(flow.closure);
  await body(await coordinator.api.post(`${flow.root}/archive`, {headers: await processHeaders(flow),
    data: {closureSnapshotId: flow.closure.id, retentionPolicyVersion: 'RETENTION_10Y', provenance: provenance(authorityId)}}), 409);
};
handlers['AC-P16-02'] = async () => {
  const flow = await improvementFlow(true);
  for (const source of flow.sources.slice(1)) await accepted(flow, source);
  await documentaryClose(flow);
  const root = `/api/v1/processes/${flow.process.id}`;
  const preview = await body(await coordinator.api.get(`${root}/closure-preview?closureType=INTEGRAL_CLOSURE`));
  expect(preview.eligible).toBe(false); expect(preview.blockers.some((b: string) => b.includes(flow.action.data.title))).toBe(true);
  const rejected = await body(await coordinator.api.post(`${root}/close`, {headers: await processHeaders(flow),
    data: {closurePolicyVersion: 'INTEGRAL_STRICT_V1', exceptionDecisionIds: [], provenance: provenance(authorityId)}}), 409);
  expect(rejected.code).toBe('CLOSURE_BLOCKED'); expect((await body(await coordinator.api.get(root))).state).toBe('DOCUMENTARY_CLOSED');
};
handlers['AC-P16-03'] = async () => {
  const flow = await archiveFlow(); await transition(flow, 'REOPENED');
  expect((await body(await coordinator.api.get(flow.root))).state).toBe('REOPENED');
  const next = await processFor(flow.model, true); expect(next.id).not.toBe(flow.process.id); expect(next.modelVersionId).toBe(flow.model.id);
  const history = await body(await coordinator.api.get(`${flow.root}/closures`));
  expect(history.items.find((c: any) => c.id === flow.closure.id)).toEqual(flow.closure);
  expect((await body(await coordinator.api.get(`/api/v1/report-versions/${flow.approved.id}`))).decisions).toEqual(flow.approved.decisions);
};
handlers['AC-X15'] = () => handlers['AC-P16-03']();

const ids = Array.from({length: 16}, (_, p) => [1, 2, 3].map(n => `AC-P${String(p + 1).padStart(2, '0')}-${String(n).padStart(2, '0')}`)).flat()
  .concat(Array.from({length: 16}, (_, n) => `AC-X${String(n + 1).padStart(2, '0')}`));
for (const id of ids) {
  test(id, async () => {
    test.skip(!handlers[id], 'Pendiente de incorporar el recorrido completo a esta suite; las pruebas por entrega no se convierten en PASS nominales.');
    await handlers[id]();
  });
}
