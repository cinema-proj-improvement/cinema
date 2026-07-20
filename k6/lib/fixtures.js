import http from 'k6/http';
import { extractCsrf } from './auth.js';

const ROWS = ['A', 'B', 'C', 'D', 'E'];
const COLS_PER_ROW = 8; // 5 * 8 = 40석

// 관리자 생성 폼이 검증/비즈니스 예외로 200을 재렌더링할 때
// <div class="alert error"><p>메시지</p></div> 안의 실제 에러 문구를 뽑아낸다.
function extractFormError(html) {
  const match = html.match(/class="alert error"[\s\S]*?<p[^>]*>([\s\S]*?)<\/p>/);
  return match ? match[1].trim() : '(에러 메시지를 찾지 못함 — 응답 HTML 구조 확인 필요)';
}

// 관리자 화면(/admin/screens/new)과 동일한 form-urlencoded 바디를 만든다.
// ScreenCreateRequest.seats 는 List<SeatCreateRequest> 라서
// Spring MVC의 인덱스 바인딩 규칙(seats[0].seatCode 등)을 그대로 따라야 한다.
function buildScreenForm(screenName, screeningType, csrf) {
  const form = {
    _csrf: csrf,
    name: screenName,
    screeningType,
    totalSeats: String(ROWS.length * COLS_PER_ROW),
    operating: 'true',
  };

  let idx = 0;
  for (const row of ROWS) {
    for (let col = 1; col <= COLS_PER_ROW; col++) {
      form[`seats[${idx}].seatCode`] = `${row}${col}`;
      form[`seats[${idx}].active`] = 'true';
      form[`seats[${idx}].rowNo`] = String(ROWS.indexOf(row) + 1);
      form[`seats[${idx}].colNo`] = String(col);
      idx++;
    }
  }

  return form;
}

// 방금 만든 상영관을 목록에서 찾는다 (생성 API가 redirect만 반환해서 id를 안 줌 -> id desc 정렬 1건 조회로 역추적).
function findLatestScreenId(baseUrl) {
  const res = http.get(`${baseUrl}/admin/screens?sort=id,desc&size=1`);
  const match = res.body.match(/\/admin\/screens\/(\d+)/);
  if (!match) {
    throw new Error('방금 생성한 상영관의 id를 목록 페이지에서 찾지 못했습니다.');
  }
  return match[1];
}

function findLatestScreeningId(baseUrl, screenId) {
  const res = http.get(`${baseUrl}/admin/screenings?screenId=${screenId}&sort=startAt,desc&size=1`);
  const match = res.body.match(/\/admin\/screenings\/(\d+)/);
  if (!match) {
    throw new Error('방금 생성한 상영의 id를 목록 페이지에서 찾지 못했습니다.');
  }
  return match[1];
}

// 40석짜리 상영관 + 그 상영관에 대한 상영 1건을 만들고 screeningId를 반환한다.
// movieId는 호출부에서 넘겨받는다 (현재 NOW_SHOWING 상태이고, screeningType을 지원하는 영화여야 함).
// 시나리오 A(spike), B(stress) 공용 픽스처.
export function createSeatFixture(baseUrl, { movieId, screeningType = 'TWO_D', startAt }) {
  const stamp = Date.now();
  const screenName = `K6-PERF-SCREEN-${stamp}`;

  // 1) 상영관 생성
  const newScreenPage = http.get(`${baseUrl}/admin/screens/new`);
  const screenCsrf = extractCsrf(newScreenPage.body);
  const screenForm = buildScreenForm(screenName, screeningType, screenCsrf);

  const createScreenRes = http.post(`${baseUrl}/admin/screens/new`, screenForm, { redirects: 0 });
  if (createScreenRes.status !== 302) {
    throw new Error(`상영관 생성 실패: status=${createScreenRes.status} 사유="${extractFormError(createScreenRes.body || '')}"`);
  }

  const screenId = findLatestScreenId(baseUrl);

  // 2) 상영 생성
  const newScreeningPage = http.get(`${baseUrl}/admin/screenings/new`);
  const screeningCsrf = extractCsrf(newScreeningPage.body);

  const createScreeningRes = http.post(
    `${baseUrl}/admin/screenings/new`,
    {
      _csrf: screeningCsrf,
      movieId: String(movieId),
      screenId: String(screenId),
      screeningType,
      startAt, // 'yyyy-MM-ddTHH:mm' (LocalDateTime 폼 포맷)
    },
    { redirects: 0 }
  );
  if (createScreeningRes.status !== 302) {
    throw new Error(`상영 생성 실패: status=${createScreeningRes.status} 사유="${extractFormError(createScreeningRes.body || '')}"`);
  }

  const screeningId = findLatestScreeningId(baseUrl, screenId);

  return { screenId, screeningId, screenName };
}
