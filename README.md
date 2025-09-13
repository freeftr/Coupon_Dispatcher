# Coupon_Dispatcher
쿠폰 발급 시스템 설계
***

## 목표
- 특정 시간에 한정된 수량의 쿠폰을 선착순으로 받을 수 있는 시스템 설계가 목표입니다.

## ERD
![ERD](./img/erd.png)

## 시스템 아키텍처
![아키텍처](./img/sysarch.png)

---
## 쿠폰 발급 플로우
![시퀸스](./img/coupon_sequence.png)

트래픽이 몰리는 시점에 Lost Update 문제가 발생하여 쿠폰이 oversell 될 수 있습니다. 이를 해결하기 위해 다음의 두 가지 방법을 고민했습니다.
- 락을 이용한 동시성 제어.
- **원자적 증가를 지원하는 데이터베이스 사용. (선택방안)**

락을 사용하면 동시성은 제어할 수 있지만, 쿠폰 레코드에 대한 트랜잭션의 race condition이 발생해 성능적 측면에서
빠른 연산 처리 성능을 지닌 In-Memory 데이터베이스인 Redis를 사용하여 구현하였습니다.

Singe thread 기반으로 동작하는 Redis Event loop에서는 Lua Script를 단일 명령어로 취급해 원자적 처리가 가능합니다.<br>
다음 Lua Script를 통해 Set을 통해 중복 발급을 검증하고, 쿠폰 발급 한도를 검증했습니다.
~~~
-- 발급한 사용자 검증용
local issuedMemberSet = KEYS[1]
-- 쿠폰 한도 검증용
local couponCounter = KEYS[2]
local limit = tonumber(ARGV[1])
local memberId = ARGV[2]
			
-- 발급된 수량, 기본값 0
local issued = tonumber(redis.call('GET', couponCounter) or '0')
	
-- 발급 한도 검증
if issued >= limit then 
-- 1: 이미 품절된 쿠폰입니다.
return 1
end
	
-- 중복 발급 검증
if redis.call('SISMEMBER', issuedMemberSet, memberId) == 1 then 
-- 2: 이미 발급한 쿠폰입니다.
return 2
end
	
redis.call('SADD', issuedMemberSet, memberId)
redis.call('INCR', couponCounter)
	
-- 쿠폰 발급에 성공
return 0
~~~

쿠폰 발급이 성공하면 메세지 큐에 쿠폰 발급 이벤트를 발행해 컨슈머 측에서 이를 소비해 비동기로 DB에 영속화합니다. 다음과 같은 두 가지 이유로
메시지 큐를 도입하였습니다.
- 쿠폰 이력을 저장하는 로직이 무거우면 트래픽이 몰리는 시점에서 API 서버에 부하가 생길 것입니다.
- 쿠폰을 발급함으로써 이를 처리해야 하는 다른 서비스가 존재할 경우 확장성을 위함입니다.
  - ex) 쇼핑몰에서 이를 발급 시, 가맹점에 지급해야 할 쿠폰 대금 처리

---

