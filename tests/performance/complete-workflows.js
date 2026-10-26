import http from 'k6/http';
import {check, fail, sleep} from 'k6';

const dataset = JSON.parse(open('../fixtures/performance/complete-workflows.json'));
const base = (__ENV.SEC_API_BASE_URL || 'http://127.0.0.1:18087/api/v1').replace(/\/$/, '');
const processPath = `${base}/processes/${dataset.processId}`;
const vus = Number(__ENV.SEC_PERF_VUS || dataset.concurrentSessions);

export const options = {
  setupTimeout: '180s',
  scenarios: {queries: {executor: 'constant-vus', vus, duration: __ENV.SEC_PERF_DURATION || '60s', gracefulStop: '30s'}},
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
    'http_req_duration{kind:interactive}': [`p(95)<=${dataset.interactiveP95Milliseconds}`]
  }
};

function read(url, session, name) {
  return http.get(url, {headers: {Cookie: `SEC_SESSION=${session.cookie}`},
    tags: {kind: 'interactive', name}, timeout: '30s'});
}

export function setup() {
  if (!dataset.synthetic || dataset.institutionalValidity) fail('Only synthetic fixtures are permitted');
  const sessions = [];
  for (let n = 1; n <= dataset.users; n++) {
    const response = http.post(`${base}/sessions`, JSON.stringify({
      username: `${dataset.usernamePrefix}${String(n).padStart(3, '0')}`,
      password: __ENV.SEC_PERF_PASSWORD || 'Synthetic-Perf-2026'
    }), {headers: {'Content-Type': 'application/json'}, tags: {kind: 'setup', name: 'authenticate fixture'}});
    if (response.status !== 201 || !response.cookies.SEC_SESSION?.length) fail(`Synthetic login ${n} failed: ${response.status}`);
    sessions.push({cookie: response.cookies.SEC_SESSION[0].value});
  }
  const first = read(`${processPath}/documentary-progress?page=0&size=20`, sessions[0], 'progress page');
  if (first.status !== 200) fail(`Dataset validation failed: ${first.status}`);
  const a = first.json();
  if (!check(a, {
    'dataset has 500 applicable sources': x => x.totalSources === dataset.requiredSources && x.requiredSourceTotal === dataset.requiredSources,
    'registered sources are distinct despite 10000 versions': x => x.registeredSourceTotal === dataset.requiredSources && x.collectionPercent === 100,
    'documentary acceptance is not invented': x => x.acceptedSourceTotal === 0 && x.documentReviewPercent === 0,
    'page size is bounded': x => x.sources.length === 20 && x.page === 0 && x.size === 20
  })) fail('Unexpected synthetic dataset or pagination');
  const b = read(`${processPath}/documentary-progress?page=1&size=20`, sessions[0], 'progress page').json();
  if (!check(b, {'successive pages do not overlap': x => x.sources.length === 20 && x.sources.every(s => !a.sources.some(p => p.id === s.id))})) {
    fail('Pagination duplicates source IDs');
  }
  return {sessions};
}

export default function ({sessions}) {
  // Rotate the 100 distinct sessions through the 30 concurrent VUs.
  const session = sessions[(__VU - 1 + __ITER * vus) % sessions.length];
  const page = (__VU + __ITER) % Math.ceil(dataset.requiredSources / 20);
  const response = read(`${processPath}/documentary-progress?page=${page}&size=20`, session, 'progress page');
  check(response, {
    'progress is authorized': r => r.status === 200,
    'progress remains bounded and scoped': r => r.status === 200 && r.json('sources').length === 20
      && r.json('processId') === dataset.processId && r.json('totalSources') === dataset.requiredSources
  });
  const history = read(`${processPath}/closures?page=0&size=20`, session, 'closure history');
  check(history, {'history is paginated and empty before closure': r => r.status === 200
    && r.json('items').length === 0 && r.json('totalElements') === 0});
  sleep(1);
}
