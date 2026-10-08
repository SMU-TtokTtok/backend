# #426 HTTP 비교 실험 재실행

Java 17, Docker, Python 3, k6가 필요하다. 운영 설정·키 파일은 사용하지 않는다.
각 후보는 [patch 설명](../../variants/issue-b/README.md)에 따라 별도 checkout에서 구성한다.

```powershell
docker network create ttokttok426
docker run -d --name ttokttok-426-repro-pg --network ttokttok426 --cpus 2 --memory 1g -p '127.0.0.1::5432' -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=ttokttok426 postgres:17.4-alpine
.\gradlew.bat prepareDuplicateApplyBenchmark
docker build -f load-test/variants/issue-b/Dockerfile -t ttokttok426-benchmark-runtime build/duplicate-apply-runtime
$taskBytes = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Fill($taskBytes)
$env:TTOKTTOK_BENCH_JWT_SECRET = [Convert]::ToBase64String($taskBytes)
$env:TTOKTTOK_CONCURRENCY_JDBC_URL = 'jdbc:postgresql://ttokttok-426-repro-pg:5432/ttokttok426'
$env:TTOKTTOK_BENCH_VARIANT = 'V1'
docker run -d --name ttokttok426-app --network ttokttok426 --cpus 2 --memory 1536m -p 127.0.0.1:18080:8080 -p 127.0.0.1:18081:8081 -e TTOKTTOK_BENCH_JWT_SECRET -e TTOKTTOK_CONCURRENCY_JDBC_URL -e TTOKTTOK_BENCH_VARIANT ttokttok426-benchmark-runtime
Invoke-RestMethod http://127.0.0.1:18081/ready
python load-test/scripts/measure-duplicate-apply.py --variant V1 --output load-test/results/issue-b/benchmark/V1
```

ready가 true가 된 후 측정한다. 준비가 끝나기 전에는 요청이 실패할 수 있다.
동일 조건에서 후보를 바꿀 때 이전 앱을 정지하고 이미지를 다시 빌드한다.
**앱 시작 시 전용 DB 스키마를 재생성하고 prepare 호출 시 데이터를 비운다.** 다른 DB를 연결하지 않는다.
서버는 전용 DB 주소 하나만 허용하며 컨트롤 API는 loopback에만 공개한다.

- hot: 같은 사용자·모집폼에 동시 20/50요청. 배치마다 새 사용자.
- users: 다른 사용자, 같은 모집폼. 배치마다 새 사용자 집합.
- forms: 같은 사용자, 다른 모집폼. 배치마다 새 사용자.
- k6 VU 한 개가 http.batch로 20/50개의 실제 HTTP 요청을 동시 실행한다. 지속 유입 모델이 아닌 배치 부하이다.
- 각 조합은 워밍업 10배치와 측정 50배치, 기본 3회 반복이다. 흔들림을 확인하면 별도 `--repetitions 2 --output .../V1-extra`로 추가한다.
- 측정 RPS는 측정 구간 wall time 기준이다. 200/409 지연은 HTTP duration이며 연결 대기 시간과 구분한다.
- DB 100ms 표본은 시작·워밍업·측정 전체 구간이다. 예외 충돌 수도 워밍업을 포함한다. 표본 사이의 짧은 잠금은 놓칠 수 있다.
- DB 표본 조회도 같은 Hikari 풀을 사용하므로 포화 시 표본이 지연되고 측정 부하를 추가한다. 표본 timestamp는 조회 시작 시점이다. 잠금 수를 정확한 시간 비율로 해석하지 않는다.
- 파일 없는 실제 JWT·필터·컨트롤러·서비스·JPA 요청이다. 외부 S3/메일/FCM/Redis만 mock하며 barrier/spy를 사용하지 않는다.
- fixture JWT는 임시 디렉터리에서 생성·삭제되며 결과 JSON에는 저장하지 않는다.
- DB 판정은 기대 그룹 수·전체 지원서/서류 수·중복 그룹 수를 비교한다. 기대 그룹 집합의 일치나 답변 내용 전부를 검증하는 실험은 아니다.
- V0의 PASS는 실험 수행 조건 통과이며 중복 방지 성공이 아니다. V0의 new_application_rps에는 중복 저장도 포함되므로 유효 지원 처리량으로 비교하지 않는다.

## 두 인스턴스 정합성 확인

선택 후보 첫 앱을 시작한 다음 동일 이미지와 JWT secret으로 두 번째 앱을 실행한다.
두 번째는 `TTOKTTOK_BENCH_DDL=validate`로 기존 스키마를 유지한다.

```powershell
docker run -d --name ttokttok426-app2 --network ttokttok426 --cpus 2 --memory 1536m -p 127.0.0.1:18082:8080 -p 127.0.0.1:18083:8081 -e TTOKTTOK_BENCH_DDL=validate -e TTOKTTOK_BENCH_JWT_SECRET -e TTOKTTOK_CONCURRENCY_JDBC_URL -e TTOKTTOK_BENCH_VARIANT ttokttok426-benchmark-runtime
python load-test/scripts/measure-duplicate-apply.py --variant V1 --server http://127.0.0.1:18080,http://127.0.0.1:18082 --repetitions 1 --output load-test/results/issue-b/benchmark/two-instances
```

두 서버 결과의 DB 건수는 공통 DB 전체이며 예외 충돌 카운터는 control 서버 한 개만 집계한다.
이 실험은 두 앱 정합성 검증이며 단일 앱 성능 점수와 합치지 않는다.
종료 시 생성한 앱과 전용 PG 컨테이너를 `docker stop`으로 정지한다. 환경변수는 전용 세션 종료로 해제한다.
