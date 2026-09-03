// 시나리오 C: 조회 트래픽 Load Test
//
// 평시 트래픽(영화 목록/상세, 예매 화면 진입, 스케줄 조회)을 재현한다.
// A/B와 달리 "동시 사용자 수"를 그대로 rps로 착각하면 안 된다 - 실제 사용자는
// 페이지 사이사이 생각할 시간(think-time)이 있으므로, 각 액션 사이에 sleep을 넣어
// VU_COUNT를 올려도 실질 rps는 훨씬 낮게 유지되도록 설계했다. (안 넣으면 "평시 트래픽"이
// 아니라 또 다른 스트레스 테스트가 되어버려서 k6/docs/healthcheck-actuator-port-separation.md
// 에서 다루는 헬스체크 기아 문제를 다시 건드릴 수 있다.)
//
// 대상 엔드포인트가 전부 GET이라 CSRF 토큰은 필요 없다. 로그인은 A/B와 동일하게
// setup()에서 한 번만 하고 세션을 재사용한다.
//
// 실행 예:
//   k6 run k6/scenarios/c-browse-load.js \
//     -e BASE_URL=http://cinema-alb-528585222.ap-northeast-2.elb.amazonaws.com \
//     -e VU_COUNT=100 -e DURATION=3m

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { loginAndGetSessionCookie } from '../lib/auth.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_USERNAME = __ENV.TEST_USERNAME || 'user@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || '1234';
const VU_COUNT = Number(__ENV.VU_COUNT || 100);
const DURATION = __ENV.DURATION || '3m';

const movieListDuration = new Trend('browse_movie_list_duration', true);
const movieDetailDuration = new Trend('browse_movie_detail_duration', true);
const reservationPageDuration = new Trend('browse_reservation_page_duration', true);
const scheduleDuration = new Trend('browse_schedule_duration', true);
const unexpectedError = new Counter('browse_unexpected_error');

export const options = {
  scenarios: {
    browse: {
      executor: 'constant-vus',
      vus: VU_COUNT,
      duration: DURATION,
    },
  },
  thresholds: {
    browse_unexpected_error: ['count==0'],
    http_req_duration: ['p(95)<500'],
  },
};

const SORT_OPTIONS = [null, null, 'releaseDate', 'reservationRate']; // 대부분 정렬 없이 기본값
const KEYWORDS = [null, null, null, null, '인터', '라라']; // 대부분 검색어 없이, 가끔 검색

function randomSleep(minSec, maxSec) {
  sleep(minSec + Math.random() * (maxSec - minSec));
}

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function checkOrCount(res, label) {
  const ok = check(res, { [`${label} 200`]: (r) => r.status === 200 });
  if (!ok) {
    unexpectedError.add(1);
    console.error(`[${label}] 예상 밖 응답: status=${res.status} body=${(res.body || '').slice(0, 200)}`);
  }
}

// GET /movies 응답 HTML에서 상세 링크(/movies/{id})를 뽑아 실제 존재하는 영화 id 목록을 얻는다.
// (하드코딩된 movieId에 의존하지 않고, dev DB에 뭐가 있든 그때그때 존재하는 영화로 탐색)
function extractMovieIds(html) {
  const matches = html.match(/\/movies\/\d+/g) || [];
  const seen = {};
  const ids = [];
  for (const m of matches) {
    const id = m.split('/').pop();
    if (!seen[id]) {
      seen[id] = true;
      ids.push(id);
    }
  }
  return ids;
}

export function setup() {
  const session = loginAndGetSessionCookie(BASE_URL, TEST_USERNAME, TEST_PASSWORD);
  return { sessionCookie: `${session.cookieName}=${session.cookieValue}` };
}

export default function (data) {
  const headers = { Cookie: data.sessionCookie };

  // 1) 영화 목록 (가끔 검색어/정렬/페이지 다르게)
  const sort = pick(SORT_OPTIONS);
  const keyword = pick(KEYWORDS);
  const page = Math.floor(Math.random() * 3); // 0~2 페이지

  const params = [`page=${page}`];
  if (sort) params.push(`sort=${sort}`);
  if (keyword) params.push(`keyword=${encodeURIComponent(keyword)}`);

  const listRes = http.get(`${BASE_URL}/movies?${params.join('&')}`, { headers });
  movieListDuration.add(listRes.timings.duration);
  checkOrCount(listRes, '영화 목록 조회');

  randomSleep(1, 3);

  const movieIds = extractMovieIds(listRes.body || '');
  if (movieIds.length === 0) {
    randomSleep(2, 5);
    return;
  }
  const movieId = pick(movieIds);

  // 2) 영화 상세
  const detailRes = http.get(`${BASE_URL}/movies/${movieId}`, { headers });
  movieDetailDuration.add(detailRes.timings.duration);
  checkOrCount(detailRes, '영화 상세 조회');

  randomSleep(1, 3);

  // 3) 예매(영화 선택) 페이지 진입
  const reservationRes = http.get(`${BASE_URL}/reservations`, { headers });
  reservationPageDuration.add(reservationRes.timings.duration);
  checkOrCount(reservationRes, '예매 페이지 진입');

  randomSleep(1, 2);

  // 4) 예매 스케줄 조회
  const today = new Date().toISOString().slice(0, 10);
  const scheduleRes = http.get(
    `${BASE_URL}/api/reservations/schedule?date=${today}&movieId=${movieId}`,
    { headers }
  );
  scheduleDuration.add(scheduleRes.timings.duration);
  checkOrCount(scheduleRes, '예매 스케줄 조회');

  randomSleep(2, 5); // 다음 액션(다른 영화 둘러보기 등)까지 대기
}
