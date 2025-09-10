package com.freeftr.coupon.member.domain.repository;

import com.freeftr.coupon.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
}
