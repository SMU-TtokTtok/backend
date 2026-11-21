/**
 * [이슈 #464] 인기 동아리 배너와 더보기의 지연 측정 시나리오
 *
 * 두 API 모두 요청마다 동아리 전체(부원 수, 즐겨찾기 수, 모집 상태)를 집계한다. 캐시와 인덱스 후보를
 * 같은 부하에서 비교하려고, 응답 시간에 끌려 요청 수가 바뀌는 VU 루프 대신 constant-arrival-rate 로
 * 초당 요청 수를 고정한다. 비교 지표는 이름 태그별 p95 다.
 *
 *   banner  GET /api/clubs/banner/popular
 *   popular GET /api/clubs/popular?size=20&sort=popular|member|latest  (첫 페이지)
 *
 * 절반은 비로그인, 절반은 로그인 요청이다. 로그인 요청은 북마크 조회가 추가로 붙는다.
 * 토큰은 init 단계에서 직접 서명한다. 서명 키는 부하테스트 앱에만 넣은 측정 전용 시크릿이다(JWT_SECRET).
 * 사용자는 sql/seed-popular-clubs.sql 이 심는 lt464-user-N@ttokttok.dev 다.
 */
import http from 'k6/http';
import { check } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';
const RATE = parseInt(__ENV.RATE || '50', 10);
const DURATION = __ENV.DURATION || '60s';
const JWT_SECRET = __ENV.JWT_SECRET;
const JWT_ISSUER = __ENV.JWT_ISSUER || 'ttokttok-loadtest-464';
const USERS = parseInt(__ENV.USERS || '500', 10);
// VU 상한은 고정이다. 한계점 아래에서는 VU 가 몇 개면 충분하고(초당 요청 수 × 응답 시간),
// 과부하에서는 몇 개를 띄워도 모자라 대기열과 메모리만 키운다. 처리 못 한 요청은 dropped_iterations 로 따로 센다.
const MAX_VUS = parseInt(__ENV.MAX_VUS || '150', 10);
const SORTS = ['popular', 'member', 'latest'];

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET 이 필요하다 (부하테스트 앱에 넣은 측정 전용 시크릿)');
}

function b64url(text) {
  return encoding.b64encode(text, 'rawurl');
}

function sign(email) {
  const now = Math.floor(Date.now() / 1000);
  const header = b64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = b64url(JSON.stringify({ iss: JWT_ISSUER, sub: email, iat: now, exp: now + 3600, role: 'ROLE_USER' }));
  const key = encoding.b64decode(JWT_SECRET, 'std');
  const signature = crypto.hmac('sha256', key, `${header}.${payload}`, 'base64rawurl');
  return `${header}.${payload}.${signature}`;
}

const TOKENS = Array.from({ length: USERS }, (_, i) => sign(`lt464-user-${i + 1}@ttokttok.dev`));

export const options = {
  discardResponseBodies: false,
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    popular_clubs: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.min(20, MAX_VUS),
      maxVUs: MAX_VUS,
    },
  },
  // 빈 임계값은 판정이 아니라 태그별 하위 지표를 요약에 나오게 하려는 것이다.
  thresholds: {
    'http_req_duration{name:banner}': [],
    'http_req_duration{name:popular}': [],
    'http_req_duration{auth:anon}': [],
    'http_req_duration{auth:user}': [],
    http_req_failed: [],
  },
};

export default function () {
  const loggedIn = Math.random() < 0.5;
  const headers = loggedIn ? { Authorization: `Bearer ${TOKENS[Math.floor(Math.random() * USERS)]}` } : {};
  const auth = loggedIn ? 'user' : 'anon';

  if (Math.random() < 0.5) {
    const res = http.get(`${BASE_URL}/api/clubs/banner/popular`, { headers, tags: { name: 'banner', auth } });
    check(res, { 'banner 200': (r) => r.status === 200 });
    return;
  }

  const sort = SORTS[Math.floor(Math.random() * SORTS.length)];
  const res = http.get(`${BASE_URL}/api/clubs/popular?size=20&sort=${sort}`, { headers, tags: { name: 'popular', auth } });
  check(res, { 'popular 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  const pick = (key) => {
    const metric = data.metrics[key];
    return metric ? { p50: metric.values['p(50)'], p95: metric.values['p(95)'], p99: metric.values['p(99)'], max: metric.values.max } : null;
  };
  const result = {
    variant: __ENV.VARIANT || 'unnamed',
    rate: RATE,
    duration: DURATION,
    requests: data.metrics.http_reqs.values.count,
    rps: data.metrics.http_reqs.values.rate,
    failedRate: data.metrics.http_req_failed.values.rate,
    droppedIterations: data.metrics.dropped_iterations ? data.metrics.dropped_iterations.values.count : 0,
    all: pick('http_req_duration'),
    banner: pick('http_req_duration{name:banner}'),
    popular: pick('http_req_duration{name:popular}'),
    anon: pick('http_req_duration{auth:anon}'),
    user: pick('http_req_duration{auth:user}'),
  };
  const out = { stdout: JSON.stringify(result, null, 2) + '\n' };
  if (__ENV.OUT) {
    out[__ENV.OUT] = JSON.stringify(result, null, 2);
  }
  return out;
}
