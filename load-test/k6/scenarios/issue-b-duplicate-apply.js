import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend } from 'k6/metrics';
import { generateId, getName } from '../lib/http-utils.js';

const duplicateRejected = new Trend('duplicate_rejected');
const duplicateAccepted = new Trend('duplicate_accepted');
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

  group('POST /api/applicants (duplicate race test)', () => {
    const response = http.post(`${BASE_URL}/api/applicants`, payload, {
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      tags: { name: 'apply' },
    });

    const responseTimeMs = response.timings.duration;
    responseTime.add(responseTimeMs);

    check(response, {
      'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
      'response time < 2s': (r) => r.timings.duration < 2000,
    });

    if (response.status === 409) {
      duplicateRejected.add(1);
    } else if (response.status === 201) {
      duplicateAccepted.add(1);
    }
  });

  sleep(10);
}

export function handleSummary(data) {
  console.log('\n=== Duplicate Apply Test Summary ===');
  console.log(`VU: ${VUS}, Duration: ${DURATION}`);
  console.log(`Total requests: ${data.metrics.http_reqs.values.count}`);
  console.log(`p95 response time: ${data.metrics.http_req_duration.values['p(95)']}`);
  console.log(`Duplicate rejected: ${data.metrics.duplicate_rejected.values.aggregate}`);
  console.log(`Duplicate accepted: ${data.metrics.duplicate_accepted.values.aggregate}`);
  console.log(`===================================\n`);
}
