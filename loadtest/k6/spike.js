// 이벤트 오픈 스파이크: RATE 명이 DURATION 동안 매초 새로 들어와 각자 발급 요청을 1번 보낸다.
// memberId 는 반복 순번으로 정해 사용자마다 다르다(시드한 회원 id 2..N+1).
//
// 환경 변수
//   TARGET    direct | queue | legacy   (기록용 이름, 경로는 PATH_KIND 가 정한다)
//   BASE_URL  예 http://localhost:18080
//   RATE      초당 새 사용자 수 (기본 500)
//   DURATION  스파이크 길이 (기본 10s)
//   COUPON_ID 발급 대상 쿠폰 (기본 1)
//   MEMBER_OFFSET 첫 memberId (기본 2, admin 이 1)
//   OUT       요약 JSON 경로
import http from 'k6/http';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const TARGET = __ENV.TARGET || 'direct';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const RATE = parseInt(__ENV.RATE || '500', 10);
const DURATION = __ENV.DURATION || '10s';
const COUPON_ID = __ENV.COUPON_ID || '1';
const MEMBER_OFFSET = parseInt(__ENV.MEMBER_OFFSET || '2', 10);

const accepted = new Counter('result_accepted');      // 200/204: 발급 성공 또는 대기열 접수
const bizReject = new Counter('result_biz_reject');   // 4xx + 품절(1003)/중복(1002/4001): 정상 업무 거절
const other4xx = new Counter('result_other_4xx');     // 그 밖의 4xx: 시드·설정 문제
const server5xx = new Counter('result_5xx');
const netError = new Counter('result_net_error');     // status 0: 타임아웃·연결 실패
const acceptedDuration = new Trend('accepted_duration', true);

const BIZ_CODES = new Set([1002, 1003, 4001]);

// 업무 거절은 실패로 세지 않는다. http_req_failed 는 5xx·네트워크 오류만 센다.
// 현재 코드는 업무 예외를 400 으로, 옛 커밋(ae00c90)은 404 로 돌려준다.
http.setResponseCallback(http.expectedStatuses(200, 204, 400, 404));

export const options = {
  discardResponseBodies: false,
  scenarios: {
    spike: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.min(RATE * 2, 4000),
      maxVUs: 6000,
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function path(memberId) {
  if (TARGET === 'queue') {
    return `${BASE_URL}/api/v1/coupons/${COUPON_ID}/queue?memberId=${memberId}`;
  }
  return `${BASE_URL}/api/v1/coupons/${COUPON_ID}?memberId=${memberId}`;
}

export default function () {
  const memberId = MEMBER_OFFSET + exec.scenario.iterationInTest;
  const res = http.post(path(memberId), null, { timeout: '30s', tags: { target: TARGET } });

  if (res.status === 200 || res.status === 204) {
    accepted.add(1);
    acceptedDuration.add(res.timings.duration);
  } else if (res.status === 400 || res.status === 404) {
    let code = -1;
    try { code = res.json('code'); } catch (e) { /* 본문 없음 */ }
    if (BIZ_CODES.has(code)) bizReject.add(1); else other4xx.add(1);
  } else if (res.status >= 500) {
    server5xx.add(1);
  } else if (res.status === 0) {
    netError.add(1);
  } else {
    other4xx.add(1);
  }
}

function val(data, name, key) {
  const m = data.metrics[name];
  return m && m.values[key] !== undefined ? m.values[key] : 0;
}

export function handleSummary(data) {
  const total = val(data, 'http_reqs', 'count');
  const out = {
    target: TARGET,
    rate: RATE,
    duration: DURATION,
    requests: total,
    dropped_iterations: val(data, 'dropped_iterations', 'count'),
    rps: val(data, 'http_reqs', 'rate'),
    p50: val(data, 'http_req_duration', 'med'),
    p95: val(data, 'http_req_duration', 'p(95)'),
    p99: val(data, 'http_req_duration', 'p(99)'),
    max: val(data, 'http_req_duration', 'max'),
    avg: val(data, 'http_req_duration', 'avg'),
    accepted: val(data, 'result_accepted', 'count'),
    biz_reject: val(data, 'result_biz_reject', 'count'),
    other_4xx: val(data, 'result_other_4xx', 'count'),
    server_5xx: val(data, 'result_5xx', 'count'),
    net_error: val(data, 'result_net_error', 'count'),
    error_rate: val(data, 'http_req_failed', 'rate'),
    accepted_p50: val(data, 'accepted_duration', 'med'),
    accepted_p95: val(data, 'accepted_duration', 'p(95)'),
    accepted_p99: val(data, 'accepted_duration', 'p(99)'),
    accepted_max: val(data, 'accepted_duration', 'max'),
    test_duration_ms: data.state.testRunDurationMs,
  };
  const lines = `${JSON.stringify(out)}\n`;
  const res = { stdout: lines };
  if (__ENV.OUT) res[__ENV.OUT] = JSON.stringify(out, null, 2);
  return res;
}
