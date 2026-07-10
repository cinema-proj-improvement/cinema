// 시나리오 B: 좌석 선점 Stress Test
//
// 시나리오 A(spike)가 "순간 폭증에도 정합성이 깨지지 않는가"를 봤다면,
// B는 "요청률을 계단식으로 계속 올리면 어디서부터 무너지기 시작하는가"를 본다.
// 부하량은 고정하지 않고 ramping-arrival-rate로 rps를 단계적으로 올린다.
//
// 좌석이 40개뿐이라 그냥 부어버리면 금방 매진되고 그 뒤론 전부 "정상적인 거부"만
// 찍혀서 정작 보고 싶은 락 처리량을 못 본다. 그래서 성공한 홀드는 바로 cancel-hold로
// 반납해서 같은 40석으로 계속 경합 상태를 유지한다 (hold -> cancel -> hold 반복).
//
// 로그인/CSRF/좌석목록은 시나리오 A와 동일하게 setup()에서 미리 끝내고, 측정 구간은
// 순수하게 홀드+취소 사이클만 반복한다.
//
// 실행 예:
//   k6 run k6/scenarios/b-seat-hold-stress.js \
//     -e BASE_URL=http://cinema-alb-528585222.ap-northeast-2.elb.amazonaws.com \
//     -e MOVIE_ID=<영화_id>

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { extractCsrf, login, loginAndGetSessionCookie } from '../lib/auth.js';
import { createSeatFixture } from '../lib/fixtures.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MOVIE_ID = __ENV.MOVIE_ID;
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin@test.com';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || '1234';
const TEST_USERNAME = __ENV.TEST_USERNAME || 'user@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || '1234';

if (!MOVIE_ID) {
  throw new Error('MOVIE_ID 환경변수가 필요합니다 (-e MOVIE_ID=<현재 NOW_SHOWING인 영화 id>).');
}

export const holdSuccess = new Counter('stress_hold_success');
export const holdConflict = new Counter('stress_hold_conflict');
export const holdUnexpected = new Counter('stress_hold_unexpected_error');
export const cancelFailed = new Counter('stress_cancel_failed');
export const holdLatency = new Trend('stress_hold_duration', true);

// 1차 실행(5~100rps)에서 100rps까지도 안 무너졌다(p95 217ms, 에러 0). 그래서 100은 빠르게
// 통과시키고, 그 위(200~1500rps) 구간의 해상도를 촘촘히 잡아서 실제 한계점을 찾는다.
// 각 단계를 30초씩 유지하는 이유: db.t4g.micro는 버스터블 인스턴스라 CPU 크레딧을 다 쓰면
// 갑자기 성능이 꺾이는 특성이 있는데, 단계가 너무 짧으면 이 "크레딧 고갈형 절벽"을 놓칠 수 있다.
export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 3000, // 꺾이는 지점에서 latency가 급증하면 필요한 동시 VU 수도 급증하므로 넉넉히
      stages: [
        { target: 100, duration: '5s' },  // 1차에서 이미 검증된 구간 - 빠르게 통과
        { target: 100, duration: '15s' },
        { target: 200, duration: '10s' },
        { target: 200, duration: '30s' },
        { target: 400, duration: '10s' },
        { target: 400, duration: '30s' },
        { target: 800, duration: '15s' },
        { target: 800, duration: '30s' },
        { target: 1500, duration: '15s' },
        { target: 1500, duration: '30s' },
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    // 한계점을 "찾는" 게 목적이므로 초반 에러 몇 건에 바로 멈추면 안 된다.
    // 다만 완전히 무의미하게 계속 도는 것도 막기 위해 넉넉한 상한선만 안전장치로 둔다.
    stress_hold_unexpected_error: [{ threshold: 'count<500', abortOnFail: true }],
  },
};

export function setup() {
  login(BASE_URL, ADMIN_USERNAME, ADMIN_PASSWORD);

  const startAt = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const startAtStr = startAt.toISOString().slice(0, 19);

  const fixture = createSeatFixture(BASE_URL, {
    movieId: MOVIE_ID,
    screeningType: 'TWO_D',
    startAt: startAtStr,
  });

  console.log(`[setup] 픽스처 생성 완료: screenId=${fixture.screenId}, screeningId=${fixture.screeningId}, screenName=${fixture.screenName}`);

  const session = loginAndGetSessionCookie(BASE_URL, TEST_USERNAME, TEST_PASSWORD);
  const sessionHeader = { Cookie: `${session.cookieName}=${session.cookieValue}` };

  const infoRes = http.get(`${BASE_URL}/reservations/screenings/${fixture.screeningId}/seat-selection/info`, {
    headers: sessionHeader,
  });
  if (infoRes.status !== 200) {
    throw new Error(`좌석 정보 조회 실패: status=${infoRes.status}`);
  }
  const info = JSON.parse(infoRes.body);
  const seatIds = info.seatInfos.filter((s) => s.selectable).map((s) => s.seatId);

  if (seatIds.length === 0) {
    throw new Error('선점 가능한 좌석이 없습니다. 픽스처 생성 로직을 확인하세요.');
  }

  const pageRes = http.get(`${BASE_URL}/reservations/screenings/${fixture.screeningId}/seat-selection`, {
    headers: sessionHeader,
  });
  const csrf = extractCsrf(pageRes.body);

  console.log(`[setup] 선점 가능 좌석 수=${seatIds.length}, 사전 로그인/CSRF 준비 완료`);

  return {
    screeningId: fixture.screeningId,
    screenId: fixture.screenId,
    seatIds,
    csrf,
    sessionCookie: `${session.cookieName}=${session.cookieValue}`,
  };
}

export default function (data) {
  const headers = { Cookie: data.sessionCookie };

  // 40석 중 무작위 1개를 노린다 - 부하가 높아질수록 자연스럽게 경합이 발생한다.
  const seatId = data.seatIds[Math.floor(Math.random() * data.seatIds.length)];

  const holdRes = http.post(
    `${BASE_URL}/reservations/holds`,
    { screeningId: data.screeningId, seatIds: seatId, _csrf: data.csrf },
    { redirects: 0, headers }
  );

  holdLatency.add(holdRes.timings.duration);

  if (holdRes.status === 302 && holdRes.headers.Location && holdRes.headers.Location.includes('/reservations/')) {
    holdSuccess.add(1);
    check(holdRes, { 'hold 성공(302 redirect)': () => true });

    // 좌석 풀을 계속 순환시키기 위해 즉시 반납한다. (실패해도 이터레이션 자체는 실패 처리하지 않음 -
    // 5분 뒤 ExpireHoldScheduler가 어차피 정리하고, cancel 실패가 홀드 처리량 측정을 왜곡할 필요는 없음)
    const reservationId = holdRes.headers.Location.split('/').pop();
    const cancelRes = http.post(`${BASE_URL}/reservations/${reservationId}/cancel-hold`, null, {
      headers: { ...headers, 'X-CSRF-TOKEN': data.csrf },
    });
    if (cancelRes.status >= 300) {
      cancelFailed.add(1);
    }
  } else if (holdRes.status === 400) {
    holdConflict.add(1);
    check(holdRes, { '좌석 이미 선점됨(400, SEAT_ALREADY_HELD 예상)': () => true });
  } else {
    holdUnexpected.add(1);
    console.error(`예상 밖 응답: status=${holdRes.status} body=${(holdRes.body || '').slice(0, 200)}`);
  }
}
