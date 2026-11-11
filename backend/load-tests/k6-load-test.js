import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Custom Metrics for SLA and latency tracking
const catalogLatency = new Trend('catalog_latency_ms');
const homeSessionLatency = new Trend('home_session_latency_ms');
const profileLatency = new Trend('profile_latency_ms');
const errorRate = new Rate('custom_error_rate');
const successfulRequests = new Counter('successful_requests');

// Load Test Configuration: Ramp-up, Sustained Load, Traffic Spike, Ramp-down
export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp-up to 20 virtual users (VUs)
    { duration: '1m',  target: 50 },  // Normal sustained load: 50 concurrent users
    { duration: '30s', target: 150 }, // Traffic Spike: burst to 150 concurrent users
    { duration: '1m',  target: 50 },  // Recovery phase: return to 50 VUs
    { duration: '20s', target: 0 },   // Graceful ramp-down to 0
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],       // HTTP failure rate below 2%
    http_req_duration: ['p(95)<300', 'p(99)<600'], // 95% of requests < 300ms, 99% < 600ms
    catalog_latency_ms: ['p(95)<100'],    // Cached endpoints must be super fast
    home_session_latency_ms: ['p(95)<250'],
    custom_error_rate: ['rate<0.01'],     // Error rate below 1%
  },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

export default function () {
  const commonHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  // 1. Browsing public catalogs (Cached endpoints)
  group('Catalog Endpoints (Cached)', () => {
    const resLanguages = http.get(`${BASE_URL}/api/catalog/languages`, { headers: commonHeaders });
    catalogLatency.add(resLanguages.timings.duration);
    const ok = check(resLanguages, {
      'catalog languages returns 200': (r) => r.status === 200,
      'catalog languages has data': (r) => r.body.length > 0,
    });
    if (ok) {
      successfulRequests.add(1);
    } else {
      errorRate.add(1);
    }

    const resLevels = http.get(`${BASE_URL}/api/catalog/proficiency-levels`, { headers: commonHeaders });
    check(resLevels, {
      'catalog levels returns 200': (r) => r.status === 200,
    });
  });

  sleep(0.5);

  // 2. Authentication & Session check
  group('Authentication Flow', () => {
    const loginPayload = JSON.stringify({
      email: 'student_loadtest@example.com',
      password: 'Password123!',
    });

    const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, { headers: commonHeaders });
    const isLoginOk = check(loginRes, {
      'login status 200 or 401': (r) => r.status === 200 || r.status === 401,
    });

    let token = '';
    if (loginRes.status === 200) {
      try {
        const json = loginRes.json();
        token = json.token || '';
      } catch (e) {
        // Ignored
      }
    }

    // Authenticated calls if token received, otherwise use mock header
    const authHeaders = {
      ...commonHeaders,
      'Authorization': token ? `Bearer ${token}` : 'Bearer mock-test-token',
    };

    // 3. Home / Session lookup
    const homeRes = http.get(`${BASE_URL}/api/home`, { headers: authHeaders });
    homeSessionLatency.add(homeRes.timings.duration);
    check(homeRes, {
      'home returns valid response': (r) => r.status === 200 || r.status === 401 || r.status === 403,
    });
  });

  sleep(0.5);

  // 4. Student Profile & Language Query
  group('Student Profile & Dashboard', () => {
    const profileRes = http.get(`${BASE_URL}/api/student/profile`, {
      headers: { ...commonHeaders, 'Authorization': 'Bearer test-student-token' },
    });
    profileLatency.add(profileRes.timings.duration);
    check(profileRes, {
      'profile response received': (r) => r.status === 200 || r.status === 401 || r.status === 404,
    });
  });

  sleep(1);
}
