import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';

// Custom metrics
const responseTime = new Trend('response_time');

// Configuration via environment variables
const VUS = __ENV.VUS || 100;
const DURATION = __ENV.DURATION || '30s';
const BASE_URL = __ENV.BASE_URL || 'http://nginx:80';
const CLUB_ID = __ENV.CLUB_ID || '1';

export const options = {
  vus: parseInt(VUS, 10),
  duration: DURATION,
};

export default function () {
  group('GET /api/clubs/{id}/introduction', () => {
    const response = http.get(`${BASE_URL}/api/clubs/${CLUB_ID}/introduction`, {
      headers: { 'Accept': 'application/json' },
      tags: { name: 'club-introduction' },
    });

    const responseTimeMs = response.timings.duration;
    responseTime.add(responseTimeMs);

    check(response, {
      'status is 200': (r) => r.status === 200,
      'response time < 1s': (r) => r.timings.duration < 1000,
    });
  });

  sleep(0.01);
}

export function handleSummary(data) {
  console.log(`\n=== Load Test Summary ===`);
  console.log(`VU: ${VUS}, Duration: ${DURATION}`);
  console.log(`Total requests: ${data.metrics.http_reqs.values.count}`);
  console.log(`p95 response time: ${data.metrics.http_req_duration.values['p(95)']}`);
  console.log(`Failed requests: ${data.metrics.http_req_failed.values.aggregate}`);
  console.log(`============================\n`);
}
