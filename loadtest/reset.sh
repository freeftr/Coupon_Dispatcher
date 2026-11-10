#!/usr/bin/env bash
# 회차마다 같은 조건을 맞춘다: 발급 기록·쿠폰을 비우고 Redis 를 flush 한 뒤 쿠폰 1장(id 1)을 새로 만든다.
# 사용: ./reset.sh <BASE_URL> <DB_NAME> <MODE: direct|queue|legacy> [QUANTITY]
set -euo pipefail
BASE_URL=${1:?BASE_URL}
DB=${2:?DB_NAME}
MODE=${3:?MODE}
QTY=${4:-1000}
DIR=$(dirname "$0")
MYSQL="docker compose -f $DIR/docker-compose.yml exec -T mysql mysql -uroot -proot"

if [ "$MODE" = "queue" ]; then
  # 대기열 스케줄러가 돌던 중이면 멈추도록 먼저 비활성화
  curl -sf -o /dev/null -X POST "$BASE_URL/api/v1/coupons/1/queue/deactivate?memberId=1" || true
fi
docker compose -f "$DIR/docker-compose.yml" exec -T redis redis-cli FLUSHALL >/dev/null
$MYSQL "$DB" -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE TABLE coupon; TRUNCATE TABLE coupon_member; TRUNCATE TABLE coupon_history;" 2>/dev/null

curl -sf -X POST "$BASE_URL/api/v1/coupons?memberId=1" -H 'Content-Type: application/json' \
  -d "{\"type\":\"치킨\",\"validityPeriod\":1,\"quantity\":$QTY}" >/dev/null

if [ "$MODE" = "queue" ]; then
  curl -sf -o /dev/null -X POST "$BASE_URL/api/v1/coupons/1/queue/activate?memberId=1"
fi
