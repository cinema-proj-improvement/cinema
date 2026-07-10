import http from 'k6/http';

const CSRF_REGEX = /name="_csrf"\s+value="([^"]+)"/;

// GET으로 렌더링된 페이지 HTML에서 Spring Security CSRF 토큰을 추출한다.
// (이 앱은 SecurityConfig에서 /admin/api/**, /h2-console/** 외 전 구간 CSRF가 켜져 있음)
export function extractCsrf(html) {
  const match = html.match(CSRF_REGEX);
  if (!match) {
    throw new Error('CSRF 토큰을 찾을 수 없습니다. 응답 HTML 구조가 바뀌었거나 로그인/권한 문제로 다른 페이지가 반환됐을 수 있습니다.');
  }
  return match[1];
}

// /login 폼 로그인. 성공 시 302, 세션 쿠키는 k6가 VU 단위로 자동 유지한다.
export function login(baseUrl, username, password) {
  const loginPage = http.get(`${baseUrl}/login`);
  if (loginPage.status === 0) {
    throw new Error(`${baseUrl} 에 연결할 수 없습니다 (BASE_URL이 맞는지, 서버가 떠 있는지 확인). error=${loginPage.error}`);
  }
  const csrf = extractCsrf(loginPage.body);

  const res = http.post(
    `${baseUrl}/login`,
    { username, password, _csrf: csrf },
    { redirects: 0 }
  );

  if (res.status !== 302) {
    throw new Error(`로그인 실패: status=${res.status} body=${(res.body || '').slice(0, 300)}`);
  }

  return res;
}

// 로그인 후 세션 쿠키(이름/값)를 뽑아낸다.
// 이 앱은 Spring Session + Redis라서 컨테이너 기본 JSESSIONID가 아니라
// Spring Session의 기본 쿠키명인 "SESSION"이 발급된다.
export function loginAndGetSessionCookie(baseUrl, username, password) {
  const res = login(baseUrl, username, password);

  const cookieNames = Object.keys(res.cookies || {});
  if (cookieNames.length === 0) {
    throw new Error('로그인 응답에 세션 쿠키가 없습니다.');
  }

  // SESSION이 있으면 그걸 쓰고, 없으면(설정이 다르면) 첫 번째 쿠키를 쓴다.
  const cookieName = cookieNames.includes('SESSION') ? 'SESSION' : cookieNames[0];
  const cookieValue = res.cookies[cookieName][0].value;

  return { cookieName, cookieValue };
}
