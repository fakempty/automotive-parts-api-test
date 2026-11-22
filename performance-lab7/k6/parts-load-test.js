import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = (__ENV.SCENARIO || 'baseline').toLowerCase();

const partCreateDuration = new Trend('part_create_duration', true);
const partGetDuration = new Trend('part_get_duration', true);
const partCancelDuration = new Trend('part_cancel_duration', true);
const conflictCount = new Counter('conflict_409_responses');
const businessErrorRate = new Rate('business_error_rate');

const scenariosConfig = {
  baseline: {
    executor: 'constant-vus',
    vus: 2,
    duration: '15s',
  },
  load: {
    executor: 'ramping-vus',
    startVUs: 2,
    stages: [
      { duration: '15s', target: 15 },
      { duration: '30s', target: 15 },
      { duration: '10s', target: 0 },
    ],
    gracefulRampDown: '5s',
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 10,
    stages: [
      { duration: '15s', target: 40 },
      { duration: '20s', target: 80 },
      { duration: '15s', target: 120 },
      { duration: '10s', target: 0 },
    ],
    gracefulRampDown: '5s',
  },
};

const activeScenario = scenariosConfig[SCENARIO] || scenariosConfig.baseline;

export const options = {
  scenarios: {
    parts_api_scenario: activeScenario,
  },
  thresholds: SCENARIO === 'stress'
    ? {
        http_req_failed: ['rate<0.25'],
        http_req_duration: ['p(95)<2500'],
      }
    : {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
      },
};

export default function () {
  const uniqueSuffix = `${__VU}_${__ITER}_${Date.now()}`;
  const payload = JSON.stringify({
    name: `PerfPart_${uniqueSuffix}`,
    category: 'Brakes',
    price: 2400.0,
    stock: 15,
    status: 'ACTIVE',
  });

  const headers = { 'Content-Type': 'application/json' };
  let createdId = null;

  group('1. Create Part (POST)', function () {
    const res = http.post(`${BASE_URL}/api/parts`, payload, { headers });
    partCreateDuration.add(res.timings.duration);

    const ok = check(res, {
      'POST /api/parts status is 200': (r) => r.status === 200,
      'POST returns dynamic id': (r) => {
        try {
          const body = r.json();
          createdId = body && body.id;
          return createdId !== undefined && createdId !== null;
        } catch (_) {
          return false;
        }
      },
    });
    businessErrorRate.add(!ok);
  });

  if (createdId !== null && createdId !== undefined) {
    group('2. Get Part by Dynamic ID (GET)', function () {
      const res = http.get(`${BASE_URL}/api/parts/${createdId}`);
      partGetDuration.add(res.timings.duration);

      const ok = check(res, {
        'GET /api/parts/{id} status is 200': (r) => r.status === 200,
        'GET returns matching id': (r) => {
          try {
            return r.json().id === createdId;
          } catch (_) {
            return false;
          }
        },
      });
      businessErrorRate.add(!ok);
    });

    group('3. Cancel Part (DELETE)', function () {
      const res = http.del(`${BASE_URL}/api/parts/${createdId}`);
      partCancelDuration.add(res.timings.duration);

      if (res.status === 409) {
        conflictCount.add(1);
      }

      const ok = check(res, {
        'DELETE /api/parts/{id} status is 200 or 409': (r) => r.status === 200 || r.status === 409,
      });
      businessErrorRate.add(!ok);
    });
  }

  sleep(0.2);
}
