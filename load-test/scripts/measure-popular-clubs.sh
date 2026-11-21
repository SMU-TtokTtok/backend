#!/usr/bin/env bash
# [이슈 #464] 인기 동아리 측정 러너
#
# 한 변형(variant)에 대해 워밍업 후 RATES 의 각 초당 요청 수로 k6 를 돌리고 결과 JSON 을 남긴다.
# 기본값 30, 60 RPS 는 한계점 아래의 지연(p95) 비교용이고, 200 RPS 는 과부하에서 실제로 처리한 RPS(최대 처리량) 측정용이다.
# 기준선의 한계점은 80~100 RPS 에서 실행마다 흔들렸다(DB 컨테이너 CPU 2코어 포화). 그 근처 값은 비교에 쓰지 않는다.
# 앱과 DB 는 docker-compose.yml 스택이 떠 있고 sql/seed-popular-clubs.sql 이 들어간 상태여야 한다.
#
#   JWT_SECRET=<부하테스트 앱에 넣은 측정 전용 시크릿> \
#     bash load-test/scripts/measure-popular-clubs.sh v0-baseline
#
# 결과: load-test/results/issue-464/<variant>-<rate>rps.json
set -euo pipefail

VARIANT="${1:?variant 이름이 필요하다 (예: v0-baseline)}"
RATES="${RATES:-30 60 200}"
DURATION="${DURATION:-60s}"
: "${JWT_SECRET:?JWT_SECRET 이 필요하다}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCENARIO="$ROOT/k6/scenarios/issue-464-popular-clubs.js"
OUT_DIR="$ROOT/results/issue-464"
mkdir -p "$OUT_DIR"

# 첫 측정이 JIT 과 커넥션 풀 예열 비용을 떠안지 않게 한다.
k6 run -q -e JWT_SECRET="$JWT_SECRET" -e RATE=50 -e DURATION=30s "$SCENARIO" > /dev/null

for rate in $RATES; do
  # 앞선 과부하에서 쌓인 요청이 다음 측정에 섞이지 않게 잠깐 쉰다.
  sleep "${COOLDOWN:-30}"
  k6 run -q -e JWT_SECRET="$JWT_SECRET" -e RATE="$rate" -e DURATION="$DURATION" \
    -e VARIANT="$VARIANT" -e OUT="$OUT_DIR/$VARIANT-${rate}rps.json" "$SCENARIO" > /dev/null
  echo "$VARIANT ${rate}rps -> $OUT_DIR/$VARIANT-${rate}rps.json"
done
