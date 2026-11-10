#!/usr/bin/env python3
"""results/*.json 을 README 에 붙일 마크다운 표로 바꾼다. 사용: python3 loadtest/summarize.py"""
import glob
import json
import os

ORDER = {"direct": 0, "queue": 1, "legacy": 2}
NAME = {"direct": "A-직접 발급", "queue": "A-대기열 접수", "legacy": "B-옛 커밋(DB만)"}
here = os.path.dirname(os.path.abspath(__file__))
rows = [json.load(open(p)) for p in glob.glob(os.path.join(here, "results", "*.json"))]
rows.sort(key=lambda d: (d["rate"], ORDER[d["target"]], d["round"]))


def ms(v):
    return f"{v:,.1f}" if v < 100 else f"{v:,.0f}"


print("| 부하 | 대상 | 회차 | p50 ms | p95 ms | p99 ms | max ms | 성공·접수 | 업무 거절(품절·중복) | 5xx·타임아웃 | 실패율 | 처리량 req/s | 실제 발급 행 | Hikari 대기 스레드(최대) | MySQL Threads_running(최대) |")
print("|---|---|---|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|")
for d in rows:
    fail = d["server_5xx"] + d["net_error"]
    print(f"| {d['rate']:,}/s × {d['duration']} | {NAME[d['target']]} | {d['round']} | {ms(d['p50'])} | {ms(d['p95'])} | {ms(d['p99'])} | {ms(d['max'])} "
          f"| {d['accepted']:,} | {d['biz_reject']:,} | {fail:,} | {d['error_rate']*100:.2f}% | {d['rps']:,.0f} | {d['issued_rows']:,} "
          f"| {d['hikari_waiting_threads']} | {d['mysql_threads_running_max']} |")

print()
print("성공·접수 요청만 본 응답 시간(직접·옛 커밋은 실제 발급된 요청, 대기열은 접수된 요청)")
print()
print("| 부하 | 대상 | 회차 | p50 ms | p95 ms | p99 ms | max ms |")
print("|---|---|---|--:|--:|--:|--:|")
for d in rows:
    print(f"| {d['rate']:,}/s | {NAME[d['target']]} | {d['round']} | {ms(d['accepted_p50'])} | {ms(d['accepted_p95'])} | {ms(d['accepted_p99'])} | {ms(d['accepted_max'])} |")

q = [d for d in rows if d["target"] == "queue"]
if q:
    print()
    print("대기열 발급 완료까지 걸린 시간(첫 발급 ~ 마지막 발급, coupon_member.created_at 기준)")
    print()
    print("| 부하 | 회차 | 발급 행 | 걸린 시간 s |")
    print("|---|---|--:|--:|")
    for d in q:
        print(f"| {d['rate']:,}/s | {d['round']} | {d['issued_rows']:,} | {d['queue_issue_span_s']:.1f} |")
