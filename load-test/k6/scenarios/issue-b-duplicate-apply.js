import http from 'k6/http';
import { Counter, Trend } from 'k6/metrics';

const fixture = JSON.parse(open(__ENV.DATA_FILE));
const servers = (__ENV.SERVERS || 'http://127.0.0.1:18080').split(',');
const warmup = Number(__ENV.WARMUP || 10);
const accepted = new Counter('accepted');
const conflicts = new Counter('conflicts');
const unexpected = new Counter('unexpected');
const measuredElapsed = new Counter('measured_elapsed_ms');
const warmupElapsed = new Counter('warmup_elapsed_ms');
const acceptedLatency = new Trend('accepted_latency_ms', true);
const conflictLatency = new Trend('conflict_latency_ms', true);
let warmupStarted;
let measuredStarted;

export const options = {
  scenarios: {
    duplicate_apply: {
      executor: 'shared-iterations', vus: 1, iterations: fixture.batches.length,
      maxDuration: '30m',
    },
  },
  batch: Math.max(50, fixture.vus),
  batchPerHost: Math.max(50, fixture.vus),
  summaryTrendStats: ['avg', 'min', 'p(50)', 'p(95)', 'p(99)', 'max'],
  discardResponseBodies: true,
};

export default function () {
  const started = Date.now();
  const phase = __ITER < warmup ? 'warmup' : 'measured';
  if (__ITER === 0) warmupStarted = started;
  if (__ITER === warmup) measuredStarted = started;
  const requests = fixture.batches[__ITER].map((entry, index) => {
    const request = JSON.stringify({
      name: 'benchmark', age: 20, major: 'computer science', email: entry.email,
      phone: '010-1234-5678', studentStatus: 'ENROLLED', grade: 'FIRST_GRADE',
      gender: 'MALE', applyFormId: entry.formId,
      answers: [{ questionId: 'text', value: 'benchmark application' }],
    });
    return {
      method: 'POST',
      url: `${servers[index % servers.length]}/api/user/applies/${entry.clubId}`,
      body: { request: http.file(request, '', 'application/json') },
      params: {
        headers: { Authorization: `Bearer ${entry.token}` },
        tags: { phase, name: 'duplicate-apply' }, timeout: '60s',
      },
    };
  });
  const responses = http.batch(requests);
  if (phase === 'warmup') {
    if (__ITER === warmup - 1) warmupElapsed.add(Date.now() - warmupStarted);
    return;
  }
  responses.forEach(response => {
    if (response.status === 200) {
      accepted.add(1);
      acceptedLatency.add(response.timings.duration);
    } else if (response.status === 409) {
      conflicts.add(1);
      conflictLatency.add(response.timings.duration);
    } else {
      unexpected.add(1, { status: String(response.status) });
    }
  });
  if (__ITER === fixture.batches.length - 1) measuredElapsed.add(Date.now() - measuredStarted);
}

export function handleSummary(data) {
  const count = name => data.metrics[name] ? data.metrics[name].values.count : 0;
  const latency = name => data.metrics[name] ? data.metrics[name].values : null;
  const seconds = count('measured_elapsed_ms') / 1000;
  const success = count('accepted');
  const rejected = count('conflicts');
  const failed = count('unexpected');
  const summary = {
    scenario: fixture.scenario, vus: fixture.vus, rounds: fixture.batches.length - warmup,
    warmup_rounds: warmup, accepted: success, conflicts: rejected, unexpected: failed,
    measured_seconds: seconds, warmup_seconds: count('warmup_elapsed_ms') / 1000,
    request_rps: (success + rejected + failed) / seconds,
    new_application_rps: success / seconds,
    accepted_latency_ms: latency('accepted_latency_ms'),
    conflict_latency_ms: latency('conflict_latency_ms'),
  };
  const json = JSON.stringify(summary, null, 2);
  return { stdout: json + '\n', [__ENV.SUMMARY_FILE]: json };
}
