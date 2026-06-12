/**
 * [부하 테스트 시나리오 A: 인기 동아리 목록 조회 성능 검증]
 * 
 * 1. 목적: 메인 페이지 진입 시 발생하는 '인기 동아리 목록' 조회 API의 N+1 쿼리 병목 및 지연 시간 확인.
 * 2. 부하 모델 (Stages):
 *    - Ramp-up: 30초 동안 가상 유저(VUs)를 0에서 50명까지 점진적 증가.
 *    - Steady State: 1분 동안 50명의 유저가 1초 간격으로 지속적 요청 (N+1 문제 집중 관찰).
 *    - Ramp-down: 30초 동안 유저를 0명으로 감소시켜 세션 종료.
 * 3. 기대 결과: 95%의 요청이 1초(1000ms) 이내에 응답해야 함.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// 테스트 옵션 설정
export const options = {
    stages: [
        { duration: '30s', target: 50 },  // 30초 동안 50명의 유저(VUs)로 증가
        { duration: '1m', target: 50 },   // 1분 동안 50명의 유저 유지
        { duration: '30s', target: 0 },   // 30초 동안 0명으로 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'], // 95%의 요청이 1초(1000ms) 이내에 완료되어야 함
        http_req_failed: ['rate<0.01'],    // 실패율이 1% 미만이어야 함
    },
};

const LOGIN_URL = 'http://app:8080/api/user/auth/login';
const TARGET_URL = 'http://app:8080/api/favorites';

export default function () {
    // 1. JWT 토큰 발급 (로그인 모방)
    const loginPayload = JSON.stringify({
        email: 'user00001@sangmyung.kr',
        password: 'UserPass123!',
        isKeepLogin: false
    });

    const loginParams = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const loginRes = http.post(LOGIN_URL, loginPayload, loginParams);

    if (!check(loginRes, { 'login status is 200': (r) => r.status === 200 })) {
        console.error('Login failed! Status:', loginRes.status, 'Body:', loginRes.body);
        sleep(1);
        return;
    }

    const accessToken = loginRes.json('data.accessToken');

    // 2. 인기 동아리 (즐겨찾기 목록 정렬) API 호출
    const targetParams = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
        },
    };

    const res = http.get(`${TARGET_URL}?sort=popular`, targetParams);

    // 3. 응답 검증
    check(res, {
        'favorite list status is 200': (r) => r.status === 200,
    });

    // 4. 유저별 호출 간격 (1초 대기 후 다시 요청)
    sleep(1);
}

// GUI 형식의 HTML 리포트 생성을 위한 함수
export function handleSummary(data) {
  return {
    "/app/load-test/summary.html": htmlReport(data), // 컨테이너 내부 경로 기준 (호스트의 load-test 폴더와 매핑됨)
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}
