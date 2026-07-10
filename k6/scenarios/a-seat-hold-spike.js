// 시나리오 A: 좌석 선점 Spike Test
//
// 40석짜리 상영에 200 VU가 거의 동시에 좌석 홀드를 시도한다.
//
// 로그인/좌석선택 페이지 진입은 setup()에서 미리 끝내고, 측정 구간(default())은
// 순수하게 홀드 POST 1번만 재도록 설계했다. 실제 티켓 오픈런 시나리오에서도
// 사용자들은 오픈 전부터 이미 로그인해서 대기 중이지, 오픈 순간에 로그인까지
// 동시에 몰리는 게 아니기 때문이다. (로그인의 BCrypt 비용까지 burst에 섞이면
// "좌석락 코드 자체의 성능"과 "동시 로그인 처리 용량"이 뒤섞여서 원인 분석이 어려워진다.)
//
// 목표:
//   - 정확히 좌석 수(40)만큼만 성공(302)하고 나머지는 SEAT_ALREADY_HELD(400)로 떨어지는지
//   - 성공 응답의 p95 latency (이제 홀드 요청 자체만의 순수한 지연시간)
//   - (스크립트 밖에서 DB로 확인) 같은 좌석이 2명에게 동시 할당되는 경우가 0건인지
//
// 실행 전 준비물:
//   - dev 프로필로 뜬 앱 인스턴스 (BASE_URL)
//   - 관리자 계정 (기본 admin@test.com / 1234 — DataInitialize가 항상 만들어줌)
//   - 현재 NOW_SHOWING 상태이고 TWO_D를 지원하는 영화 1개의 movieId
//
// 실행 예:
//   k6 run k6/scenarios/a-seat-hold-spike.js \
//     -e BASE_URL=http://localhost:8080 \
//     -e MOVIE_ID=1 \
//     -e ADMIN_USERNAME=admin@test.com -e ADMIN_PASSWORD=1234 \
//     -e TEST_USERNAME=user@test.com -e TEST_PASSWORD=1234

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { extractCsrf, login, loginAndGetSessionCookie } from '../lib/auth.js';
import { createSpikeTestFixture } from '../lib/fixtures.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MOVIE_ID = __ENV.MOVIE_ID;
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin@test.com';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || '1234';
const TEST_USERNAME = __ENV.TEST_USERNAME || 'user@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || '1234';
const VU_COUNT = Number(__ENV.VU_COUNT || 200);

if (!MOVIE_ID) {
  throw new Error('MOVIE_ID 환경변수가 필요합니다 (-e MOVIE_ID=<현재 NOW_SHOWING인 영화 id>).');
}

export const holdSuccess = new Counter('seat_hold_success');
export const holdConflict = new Counter('seat_hold_conflict');
export const holdUnexpected = new Counter('seat_hold_unexpected_error');
export const holdLatency = new Trend('seat_hold_duration', true);

export const options = {
  scenarios: {
    spike: {
      // ramping-vus는 램프다운 시점에 아직 안 끝난 VU를 gracefulRampDown 안에 강제 종료시킨다.
      // 응답이 느린 환경에서는 "정확히 1번씩 시도"가 중간에 끊겨버려서 결과가 왜곡된다.
      // per-vu-iterations는 VU당 iteration 수를 못박고, 느려도 maxDuration까지는 끝까지 기다려준다.
      executor: 'per-vu-iterations',
      vus: VU_COUNT,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds: {
    seat_hold_unexpected_error: ['count==0'],
    seat_hold_duration: ['p(95)<800'], // 현재 dev 환경은 이 기준을 못 맞출 가능성이 높다 - 그 자체가 결과임
  },
};

// setup()은 메인 스레드에서 1회만 실행된다 (VU 쿠키 jar와 무관).
export function setup() {
  login(BASE_URL, ADMIN_USERNAME, ADMIN_PASSWORD);

  // 서버 컨테이너의 JVM 기본 타임존(UTC/KST)을 리포만으로 확정할 수 없어서,
  // @Future 검증이 어느 쪽으로 해석되든 안전하게 미래가 되도록 24시간 여유를 둔다.
  // (UTC로 보내는데 서버가 KST로 해석해도 실제로는 -9시간이라 여전히 미래)
  const startAt = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const startAtStr = startAt.toISOString().slice(0, 19); // 'yyyy-MM-ddTHH:mm:ss' (초 단위, offset 제거)

  const fixture = createSpikeTestFixture(BASE_URL, {
    movieId: MOVIE_ID,
    screeningType: 'TWO_D',
    startAt: startAtStr,
  });

  console.log(`[setup] 픽스처 생성 완료: screenId=${fixture.screenId}, screeningId=${fixture.screeningId}, screenName=${fixture.screenName}`);

  // 일반 사용자로 미리 로그인 (측정 구간 밖 - 실제로도 오픈 전에 이미 로그인해서 대기하는 상황을 재현)
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

  // holds 폼의 CSRF도 미리 받아둔다. Spring Security 기본(HttpSessionCsrfTokenRepository)은
  // 세션당 토큰이 고정이라 로그인 이후 한 번만 받아도 이후 요청에 그대로 재사용 가능하다.
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

// per-vu-iterations(vus: VU_COUNT, iterations: 1) 덕분에 이 함수는 VU당 정확히 1번만 호출된다.
// 로그인/CSRF/좌석목록 조회는 전부 setup()에서 끝내놨으므로, 여기서는 홀드 요청 자체만 측정한다.
export default function (data) {
  // VU마다 결정론적으로 좌석 1개를 겨냥 -> 200 VU / 40석이면 좌석당 평균 5명이 경쟁하게 됨
  const seatId = data.seatIds[(__VU - 1) % data.seatIds.length];

  const res = http.post(
    `${BASE_URL}/reservations/holds`,
    { screeningId: data.screeningId, seatIds: seatId, _csrf: data.csrf },
    { redirects: 0, headers: { Cookie: data.sessionCookie } }
  );

  holdLatency.add(res.timings.duration);

  if (res.status === 302 && res.headers.Location && res.headers.Location.includes('/reservations/')) {
    holdSuccess.add(1);
    check(res, { 'hold 성공(302 redirect)': () => true });
  } else if (res.status === 400) {
    holdConflict.add(1);
    check(res, { '좌석 이미 선점됨(400, SEAT_ALREADY_HELD 예상)': () => true });
  } else {
    holdUnexpected.add(1);
    console.error(`[VU ${__VU}] 예상 밖 응답: status=${res.status} body=${(res.body || '').slice(0, 200)}`);
  }
}
