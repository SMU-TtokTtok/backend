import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 옵션 설정
export const options = {
    // 3단계에 걸친 부하 테스트 (Ramp-up -> 유지 -> Ramp-down)
    stages: [
        { duration: '30s', target: 50 },  // 30초 동안 50명의 유저(VUs)로 증가
        { duration: '1m', target: 50 },   // 1분 동안 50명의 유저 유지 (이 구간에서 N+1 터지는지 확인)
        { duration: '30s', target: 0 },   // 30초 동안 0명으로 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'], // 95%의 요청이 1초(1000ms) 이내에 완료되어야 함
        http_req_failed: ['rate<0.01'],    // 실패율이 1% 미만이어야 함
    },
};

// VUs(Virtual Users) 간에 데이터를 공유하지 않으므로, 로그인 로직을 setup 또는 각 유저 함수 내에서 처리.
// 이 테스트에서는 모든 가상 유저가 동일한 테스트 계정(test@sangmyung.kr)을 사용해 JWT를 발급받는다고 가정합니다.
const LOGIN_URL = 'http://app:8080/api/user/auth/login';
const TARGET_URL = 'http://app:8080/api/favorites'; // Swagger 컨트롤러에 등록된 기본 목록조회 URL

export default function () {
    // 1. JWT 토큰 발급 (로그인 모방)
    const loginPayload = JSON.stringify({
        email: 'user00001@sangmyung.kr', // Seed 데이터를 이용한 유저
        password: 'UserPass123!', // V12__insert_mock_users.sql의 기본 비밀번호. Swagger에도 TestPass123! 라고 되어 있으나 변경 가능. (확인필요)
        isKeepLogin: false
    });

    const loginParams = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const loginRes = http.post(LOGIN_URL, loginPayload, loginParams);

    // 로그인 실패 시 더 이상 진행하지 않음
    if (!check(loginRes, { 'login status is 200': (r) => r.status === 200 })) {
        console.error('Login failed! Status:', loginRes.status, 'Body:', loginRes.body);
        sleep(1);
        return;
    }

    // 서버 응답 구조 (ApiResponse)에 맞게 accessToken 파싱
    // body: { "status": 200, "message": "로그인 성공", "data": { "accessToken": "..." } }
    const accessToken = loginRes.json('data.accessToken');

    // 2. 인기 동아리 (즐겨찾기 목록 정렬) API 호출
    const targetParams = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
        },
    };

    // sort 파라미터를 popular로 주면 N+1 문제가 발생하는 popularFavorites 로직을 탑니다.
    const res = http.get(`${TARGET_URL}?sort=popular`, targetParams);

    // 3. 응답 검증
    check(res, {
        'favorite list status is 200': (r) => r.status === 200,
    });

    // 4. 유저별 호출 간격 (1초 대기 후 다시 요청)
    sleep(1);
}