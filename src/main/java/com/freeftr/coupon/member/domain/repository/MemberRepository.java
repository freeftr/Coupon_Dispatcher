package com.freeftr.coupon.member.domain.repository;

import com.freeftr.coupon.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    public Member saveMember (Member member) {
        return memberJpaRepository.save(member);
    }

    public Optional<Member> findById (Long id) {
        return memberJpaRepository.findById(id);
    }
}
