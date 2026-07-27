import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';
import { generateId } from '../lib/http-utils.js';

const quotaExceeded = new Trend('quota_exceeded');
const quotaSucceeded = new Trend('quota_succeeded');
const responseTime = new Trend('response_time');

const VUS = __ENV.VUS || 50;
const DURATION = __ENV.DURATION || '30s';
const BASE_URL = __ENV.BASE_URL || 'http://nginx:80';
const APPLYFORM_ID = __ENV.APPLYFORM_ID || '1';

export const options = {
  vus: parseInt(VUS, 10),
  duration: DURATION,
};

export default function () {
  const userEmail = `user_${generateId()}@ttokttok.test`;
  const payload = JSON.stringify({
    userEmail: userEmail,
    applyformId: parseInt(APPLYFORM_ID, 10),
  });

  group('POST /api/applicants (maxApplyCount race test)', () => {
    const response = http.post(`${BASE_URL}/api/applicants`, payload, {
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      tags: { name: 'apply' },
    });

    const responseTimeMs = response.timings.duration;
    responseTime.add(responseTimeMs);

    check(response, {
      'status is 201 or 422': (r) => r.status === 201 || r.status === 422,
      'response time < 2s': (r) => r.timings.duration < 2000,
    });

    if (response.status === 422) {
      quotaExceeded.add(1);
    } else if (response.status === 201) {
      quotaSucceeded.add(1);
    }
  });

  sleep(10);
}

export function handleSummary(data) {
  console.log('\n=== Max Apply Count Test Summary ===');
  console.log(`VU: ${VUS}, Duration: ${DURATION}`);
  console.log(`Total requests: ${data.metrics.http_reqs.values.count}`);
  console.log(`p95 response time: ${data.metrics.http_req_duration.values['p(95)']}`);
  console.log(`Quota exceeded: ${data.metrics.quota_exceeded.values.aggregate}`);
  console.log(`Quota succeeded: ${data.metrics.quota_succeeded.values.aggregate}`);
  console.log(`===================================\n`);
}
