/**
 * [이슈 #346] 동아리 조회수 동시성(Lost Update) 측정 시나리오
 *
 * 목적: GET /api/clubs/{clubId}/content 를 동시 호출했을 때 조회수 증가분이 유실되는지 측정한다.
 *
 * 이 시나리오는 응답시간만 보는 일반 부하 시나리오와 다르다. 핵심은 "요청 수를 결정적으로 고정해서
 * DB 의 view_count 증가분과 1:1 대조 가능하게 만드는 것"이다. 그래서 두 가지를 지킨다.
 *
 *   1) duration 기반이 아니라 shared-iterations 로 총 요청 수를 고정한다.
 *      duration 기반은 실행마다 요청 수가 달라져 기대값을 세울 수 없다.
 *   2) 유실 계산의 기준값은 설정된 iterations 가 아니라 successful_views(2xx 응답 수)다.
 *      실패한 요청은 조회수를 올리지 않으므로, iterations 로 계산하면 유실을 과대평가한다.
 *
 * view_count 는 어떤 API 응답에도 노출되지 않으므로(ClubDetailServiceResponse 에 필드가 없다)
 * 정확성 판정은 k6 가 아니라 psql 로 한다. scripts/measure-viewcount.ps1 이 리셋 → 실행 → 대조를 묶어준다.
 *
 * 엔드포인트는 /api/clubs/{clubId}/content 다. permitAll 이므로 JWT 없이 호출되며,
 * 비로그인 요청에서는 즐겨찾기 EXISTS 서브쿼리가 실행되지 않는다.
 */
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// DB view_count 증가분과 대조할 기준값
const successfulViews = new Counter('successful_views');

const VUS = parseInt(__ENV.VUS || '100', 10);
const ITERATIONS = parseInt(__ENV.ITERATIONS || '5000', 10);
const BASE_URL = __ENV.BASE_URL || 'http://nginx:80';
// sql/seed-viewcount.sql 이 심는 부하테스트 전용 행 (mock 시드와 겹치지 않는 UUID)
const CLUB_ID = __ENV.CLUB_ID || '00000000-0000-4000-8000-000000000346';

export const options = {
  scenarios: {
    view_count_race: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: '10m',
    },
  },
  // p(99) 는 k6 기본 요약에 포함되지 않아 명시해야 한다.
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const res = http.get(`${BASE_URL}/api/clubs/${CLUB_ID}/content`, {
    headers: { Accept: 'application/json' },
    tags: { name: 'club-content' },
  });

  const ok = res.status === 200;
  if (ok) {
    successfulViews.add(1);
  }

  check(res, { 'status is 200': () => ok });

  // sleep 을 두지 않는다 — 동일 행에 대한 쓰기 경합을 최대화하는 것이 이 시나리오의 목적이다.
}

export function handleSummary(data) {
  const m = data.metrics;
  const trend = m.http_req_duration.values;

  const summary = {
    vus: VUS,
    iterations_configured: ITERATIONS,
    successful_views: m.successful_views ? m.successful_views.values.count : 0,
    http_reqs: m.http_reqs.values.count,
    http_req_failed_rate: m.http_req_failed.values.rate,
    rps: m.http_reqs.values.rate,
    latency_ms: {
      med: trend.med,
      p90: trend['p(90)'],
      p95: trend['p(95)'],
      p99: trend['p(99)'],
      max: trend.max,
    },
  };

  console.log('=== view-count summary ===');
  console.log(JSON.stringify(summary, null, 2));

  return {
    '/results/summary.json': JSON.stringify(summary, null, 2),
  };
}
