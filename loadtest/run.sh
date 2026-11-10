#!/usr/bin/env bash
# 한 회차: 초기화 → 워밍업 → 초기화 → 스파이크 측정. 결과는 results/<TARGET>-r<RATE>-<ROUND>.json
# 사용: ./run.sh <TARGET: direct|queue|legacy> <RATE> <ROUND>
# 환경 변수: BASE_URL(기본 대상별), DB(기본 대상별), QTY(1000), DURATION(10s),
#   APP_PID·JCMD(있으면 스파이크 중 스레드 덤프로 Hikari 커넥션을 기다리는 스레드 수를 센다)
set -euo pipefail
TARGET=${1:?TARGET}
RATE=${2:?RATE}
ROUND=${3:?ROUND}
DIR=$(cd "$(dirname "$0")" && pwd)
QTY=${QTY:-1000}
DURATION=${DURATION:-10s}
if [ "$TARGET" = "legacy" ]; then
  BASE_URL=${BASE_URL:-http://localhost:18081}; DB=${DB:-coupon_legacy}
else
  BASE_URL=${BASE_URL:-http://localhost:18080}; DB=${DB:-coupon}
fi
MODE=$TARGET
COMPOSE="docker compose -f $DIR/docker-compose.yml"
MYSQL="$COMPOSE exec -T mysql mysql -uroot -proot"
OUT=$DIR/results/$TARGET-r$RATE-$ROUND.json
mkdir -p "$DIR/results"

# 워밍업: JIT·커넥션 풀을 데운다. 측정과 같은 경로로 1,000건(초당 200)
"$DIR/reset.sh" "$BASE_URL" "$DB" "$MODE" "$QTY"
TARGET=$TARGET BASE_URL=$BASE_URL RATE=200 DURATION=5s k6 run -q --no-summary "$DIR/k6/spike.js" >/dev/null 2>&1 || true
"$DIR/reset.sh" "$BASE_URL" "$DB" "$MODE" "$QTY"
sleep 2

# 실행 중 MySQL 연결·실행 스레드 수를 0.5초마다 기록
MON=$(mktemp)
$COMPOSE exec -T mysql sh -c 'while true; do mysql -uroot -proot -N -e "SHOW GLOBAL STATUS WHERE Variable_name IN (\"Threads_connected\",\"Threads_running\")" 2>/dev/null | tr "\n" " "; echo; sleep 0.5; done' > "$MON" &
MON_PID=$!

HIKARI_WAIT=-1
if [ -n "${APP_PID:-}" ]; then
  # 스파이크 첫 4초 동안 0.5초 간격으로 덤프를 떠 커넥션을 기다리는 스레드 수의 최댓값을 남긴다
  JCMD=${JCMD:-jcmd}
  ( m=0; for _ in 1 2 3 4 5 6 7 8; do sleep 0.5
      # 한 스레드 스택에 getConnection 프레임이 두 줄(오버로드) 찍히므로 줄이 아니라 스레드 블록을 센다
      n=$("$JCMD" "$APP_PID" Thread.print 2>/dev/null | awk 'BEGIN{RS=""} /HikariPool.getConnection/{n++} END{print n+0}')
      [ "${n:-0}" -gt "$m" ] && m=$n; done; echo "$m" > "$MON.hikari" ) &
fi

TARGET=$TARGET BASE_URL=$BASE_URL RATE=$RATE DURATION=$DURATION OUT=$OUT k6 run -q "$DIR/k6/spike.js" > "$MON.k6" 2>&1 || true
kill $MON_PID 2>/dev/null || true
wait $MON_PID 2>/dev/null || true
[ -f "$MON.hikari" ] && HIKARI_WAIT=$(cat "$MON.hikari")

CONN_MAX=$(awk '{for(i=1;i<NF;i++){if($i=="Threads_connected")c=$(i+1); if($i=="Threads_running")r=$(i+1)} if(c>mc)mc=c; if(r>mr)mr=r} END{print mc+0}' "$MON")
RUN_MAX=$(awk '{for(i=1;i<NF;i++){if($i=="Threads_running")r=$(i+1)} if(r>mr)mr=r} END{print mr+0}' "$MON")

# 대기열은 접수 뒤 스케줄러가 초당 10명씩 발급한다. 발급이 끝날 때까지(최대 200초) 기다려 걸린 시간을 잰다
ISSUE_SPAN=null
if [ "$MODE" = "queue" ]; then
  for _ in $(seq 1 200); do
    C=$($MYSQL "$DB" -N -e "SELECT COUNT(*) FROM coupon_member" 2>/dev/null)
    [ "$C" -ge "$QTY" ] && break
    sleep 1
  done
fi
read -r ISSUED SPAN <<<"$($MYSQL "$DB" -N -e "SELECT COUNT(*), IFNULL(TIMESTAMPDIFF(MICROSECOND, MIN(created_at), MAX(created_at))/1e6, 0) FROM coupon_member" 2>/dev/null)"
[ "$MODE" = "queue" ] && ISSUE_SPAN=$SPAN

python3 - "$OUT" "$ROUND" "$CONN_MAX" "$RUN_MAX" "$HIKARI_WAIT" "$ISSUED" "$ISSUE_SPAN" <<'PY'
import json, sys
out, rnd, conn, run, hik, issued, span = sys.argv[1:]
d = json.load(open(out))
d.update(round=int(rnd), mysql_threads_connected_max=int(conn), mysql_threads_running_max=int(run),
         hikari_waiting_threads=int(hik), issued_rows=int(issued),
         queue_issue_span_s=None if span == "null" else float(span))
json.dump(d, open(out, "w"), indent=2)
print(json.dumps(d, ensure_ascii=False))
PY
rm -f "$MON" "$MON.hikari" "$MON.k6"
