#!/usr/bin/env bash
# 회원 시드. 관리자 1명(id 1) + 일반 회원 N명(id 2..N+1)을 회원 생성 API 로 만든다.
# 사용: ./seed.sh <BASE_URL> <DB_NAME> [N]
#   ./seed.sh http://localhost:18080 coupon 20000
set -euo pipefail
BASE_URL=${1:?BASE_URL}
DB=${2:?DB_NAME}
N=${3:-20000}
MYSQL="docker compose -f $(dirname "$0")/docker-compose.yml exec -T mysql mysql -uroot -proot"

# id 를 1 부터 다시 매기려고 테이블을 비운다(테이블은 앱 기동 시 ddl-auto 가 만든다)
$MYSQL "$DB" -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE TABLE member; TRUNCATE TABLE coupon; TRUNCATE TABLE coupon_member;" 2>/dev/null

post_member() {
  curl -sf -o /dev/null -X POST "$BASE_URL/api/v1/members" \
    -H 'Content-Type: application/json' -d "{\"name\":\"$1\",\"grade\":\"$2\"}"
}
export -f post_member
export BASE_URL

post_member admin ADMIN
# 관리자가 id 1 이 되도록 먼저 만들고, 나머지는 병렬로 만든다(순서 무관)
seq 1 "$N" | xargs -P 32 -I{} bash -c 'post_member user{} MEMBER'

COUNT=$($MYSQL "$DB" -N -e "SELECT COUNT(*), MIN(member_id), MAX(member_id) FROM member" 2>/dev/null)
echo "members(count,min,max): $COUNT"
