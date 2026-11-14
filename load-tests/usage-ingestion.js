import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const businessFailures = new Rate('business_failures');
const replayFailures = new Rate('idempotent_replay_failures');
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiKey = __ENV.API_KEY || 'bk_demo.demo-secret';
const subscriptionId = __ENV.SUBSCRIPTION_ID || '40000000-0000-0000-0000-000000000001';

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '40s', target: 40 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    business_failures: ['rate<0.01'],
    idempotent_replay_failures: ['rate<0.001'],
  },
};

export default function () {
  const eventId = `load-${__VU}-${__ITER}-${Date.now()}`;
  const occurredAt = new Date().toISOString();
  const payload = JSON.stringify({
    eventId,
    subscriptionId,
    meterKey: 'api_calls',
    quantity: 1,
    occurredAt,
    attributes: { source: 'k6', region: 'ap-south-1' },
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
      'X-Correlation-Id': eventId,
    },
  };

  const created = http.post(`${baseUrl}/v1/usage-events`, payload, params);
  const createdOk = check(created, {
    'usage event created': (response) => response.status === 201,
    'event identity preserved': (response) => response.json('eventId') === eventId,
    'new event is not replayed': (response) => response.json('replayed') === false,
  });
  businessFailures.add(!createdOk);

  const replayed = http.post(`${baseUrl}/v1/usage-events`, payload, params);
  const replayOk = check(replayed, {
    'exact replay accepted': (response) => response.status === 200,
    'replay identity preserved': (response) => response.json('eventId') === eventId,
    'replay is identified': (response) => response.json('replayed') === true,
  });
  replayFailures.add(!replayOk);
  sleep(0.1);
}
