# #464 인기 동아리 배너/더보기 측정

## 문제

배너(`GET /api/clubs/banner/popular`)와 더보기(`GET /api/clubs/popular`)는 요청마다 동아리 전체를 집계한다.
부원 수(`club_members`), 즐겨찾기 수(`user_favorites`), 모집 상태(`applyforms`)를 GROUP BY 하고, 로그인 사용자면
내 즐겨찾기(`user_favorites` JOIN `users`)까지 한 쿼리에서 읽는다. 운영 스키마에는 `user_favorites`, `applyforms` 에
PK 외 인덱스가 없다(2026-11-19 읽기 전용 확인. PostgreSQL 은 FK 에 인덱스를 자동으로 만들지 않는다).

동아리 300개 기준 쿼리 1회 14.5ms(EXPLAIN ANALYZE 중앙값). DB 컨테이너 CPU 2코어가 포화되어 기준선은 80~100 RPS 에서 무너진다.

## 조건

- `load-test/docker-compose.yml` 스택. app 2 CPU / 2GB, PostgreSQL 17.4 2 CPU / 1GB, Redis 7.2. Windows Docker Desktop.
- 시드 `load-test/sql/seed-popular-clubs.sql`: 동아리 300, 사용자 10,000, 즐겨찾기 29,561, 부원 8,429, 지원서 300.
- 시나리오 `load-test/k6/scenarios/issue-464-popular-clubs.js`: constant-arrival-rate, 배너/더보기 반반, 비로그인/로그인 반반, VU 상한 150.
- 실행 `load-test/scripts/measure-popular-clubs.sh <variant>`: 워밍업 30초, 단계마다 쿨다운 30초, 단계당 60초.
- 30·60 RPS 는 한계점 아래 지연 비교, 150 RPS 는 과부하에서 실제 처리 RPS 비교용. 변형마다 1회 측정이라 수 ms 차이는 잡음일 수 있다.
- v0·v1 은 현행 코드(`develop` 1ef5b5a), v2·v3 은 Redis 캐시 코드(`fix/#464`).

## 변형

| 변형 | 내용 |
|---|---|
| v0-baseline | 현행 |
| v1-index-fav-user-club | 현행 + `user_favorites(user_id)`, `user_favorites(club_id)` |
| v1a / v1b | 현행 + 위 인덱스 하나씩 (150 RPS 만) |
| v2-redis | 후보 전체를 Redis 에 캐시(30초 갱신, 10분 만료, SET NX 락), 북마크는 요청마다 덧씌움 |
| v3-redis-index-fav-user | v2 + `user_favorites(user_id)` |

## 결과 (ms)

| 변형 | 목표 RPS | 실제 RPS | 처리 못 함 | p95 | p99 | 비로그인 p95 | 로그인 p95 |
|---|---|---|---|---|---|---|---|
| v0-baseline | 30 | 30.0 | 0 | 35.8 | 52.8 | 32.8 | 38.9 |
| v0-baseline | 60 | 60.0 | 0 | 22.5 | 30.3 | 20.6 | 24.3 |
| v0-baseline | 150 | **116.3** | 1,870 | **2,696.3** | 4,098.2 | 2,678.3 | 2,800.6 |
| v1-index-fav-user-club | 30 | 30.0 | 0 | 32.2 | 46.3 | 32.8 | 31.7 |
| v1-index-fav-user-club | 60 | 60.0 | 0 | 23.8 | 36.0 | 23.4 | 24.4 |
| v1-index-fav-user-club | 150 | 150.0 | 0 | 13.7 | 17.1 | 13.5 | 13.9 |
| v1a-index-fav-user | 150 | 150.0 | 0 | 16.5 | 21.3 | 16.1 | 16.9 |
| v1b-index-fav-club | 150 | 150.0 | 0 | 14.3 | 15.6 | 12.0 | 14.7 |
| v2-redis | 30 | 30.0 | 0 | 10.3 | 11.8 | 5.9 | 10.9 |
| v2-redis | 60 | 60.0 | 0 | 9.9 | 11.3 | 5.6 | 10.4 |
| v2-redis | 150 | 150.0 | 0 | 9.7 | 12.2 | 5.6 | 10.6 |
| v3-redis-index-fav-user | 30 | 30.0 | 0 | 6.9 | 8.1 | 5.4 | 7.4 |
| v3-redis-index-fav-user | 60 | 60.0 | 0 | 6.1 | 6.9 | 5.0 | 6.5 |
| v3-redis-index-fav-user | 150 | 150.0 | 0 | **6.5** | 7.7 | 5.1 | 6.8 |

30 RPS 의 v0·v1 이 60 RPS 보다 느린 것은 첫 측정 구간의 예열 영향으로 본다.

## 캐시 갱신과 스탬피드

`pg_stat_user_tables.seq_scan`(clubs) 증가분으로 후보 쿼리 실행 횟수를 셌다.

| 변형 | 측정 전체(약 5분, 요청 약 1만 6천 건)의 후보 쿼리 | 150 RPS 중 캐시 키 삭제 후 5초간 후보 쿼리 |
|---|---|---|
| v2-redis | 7 | 1 |
| v3-redis-index-fav-user | 7 | 1 |

캐시가 없으면 요청 수만큼(삭제 후 5초면 약 750번) 실행된다. 락을 잡은 요청 하나만 DB 를 읽고 나머지는 기다렸다가 채워진 값을 읽었다.
키 삭제를 포함한 40초 실행의 p95 는 v2 9.6ms, v3 7.3ms, 실패 0.

## 인덱스 EXPLAIN (현행 쿼리, 21회 중앙값)

| 인덱스 | 실행 시간 | 계획 변화 |
|---|---|---|
| 없음 | 14.45ms | user_favorites 전체 스캔 2회 |
| user_favorites(user_id) | 10.00ms | 내 즐겨찾기 → Bitmap Index Scan |
| user_favorites(club_id) | 11.34ms | 즐겨찾기 수 집계 → Index Only Scan |
| 둘 다 | 7.51ms | |
| applyforms(club_id) | 21.49ms | 플래너가 쓰지 않음 (Seq Scan 유지, 수치는 잡음) |

## 결론

- Redis 캐시(v2): 150 RPS 에서 p95 2,696ms → 9.7ms, 실제 처리 116 → 150 RPS. DB 후보 쿼리는 30초당 1회.
- `user_favorites(user_id)` 는 캐시 뒤에도 로그인 요청의 북마크 조회에 쓰여 로그인 p95 10.6 → 6.8ms(150 RPS). V34 로 추가한다.
- `user_favorites(club_id)` 는 캐시가 없을 때만 효과가 있었고, 캐시 뒤에는 30초에 한 번 도는 갱신 쿼리에만 쓰인다. 효과를 확인하지 않았으므로 추가하지 않는다.
- `applyforms(club_id)` 는 플래너가 쓰지 않아 추가하지 않는다.
- 운영은 동아리 24개 규모라 현재 부하로는 차이가 체감되지 않는다. 이 측정은 300개 상한 기준이다.

## 재현

```bash
# 스택: load-test/docker-compose.yml (설정 yml 과 Firebase 키는 이미지에 굽지 않고 읽기 전용 마운트, JWT_SECRET 은 측정 전용 값)
docker exec -i load-test-db-1 psql -U ttokttok_user -d ttokttok < load-test/sql/seed-popular-clubs.sql
JWT_SECRET=<측정 전용 시크릿> RATES="30 60 150" bash load-test/scripts/measure-popular-clubs.sh v0-baseline
```
