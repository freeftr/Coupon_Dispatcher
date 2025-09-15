package com.freeftr.coupon.member.domain;

import com.freeftr.coupon.common.entity.BaseEntity;
import com.freeftr.coupon.member.domain.enums.MemberGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long id;

    @Column(name = "member_name", nullable = false)
    private String name;

    @Column(name = "member_grade", nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberGrade grade;

    public boolean isAdmin(Member member) {
        return member.grade.equals(MemberGrade.ADMIN);
    }

    @Builder
    public Member(
            String name,
            MemberGrade grade
    ) {
        this.name = name;
        this.grade = grade;
    }
}
