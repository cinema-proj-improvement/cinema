package com.elice.cinema.global.config;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.entity.Role;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.policy.entity.EnvironmentPolicy;
import com.elice.cinema.domain.policy.repository.EnvironmentPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitialize implements CommandLineRunner {
    private final MemberRepository memberRepository;
    private final EnvironmentPolicyRepository environmentPolicyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 1. 관리자 계정 생성
        if (memberRepository.findByEmail("admin@test.com").isEmpty()) {

            Member admin = Member.builder()
                    .email("admin@test.com")
                    .password(passwordEncoder.encode("1234"))
                    .name("관리자")
                    .nickname("AdminUser")
                    .age(30)
                    .role(Role.ADMIN)
                    .build();

            memberRepository.save(admin);

            log.info("관리자 계정 생성 완료: admin@test.com / 1234");
        }

        // 2. 일반 사용자 계정 생성 + Cart 할당
        if (memberRepository.findByEmail("user@test.com").isEmpty()) {

            Member user = Member.builder()
                    .email("user@test.com")
                    .password(passwordEncoder.encode("1234"))
                    .name("일반유저")
                    .nickname("GeneralUser")
                    .age(30)
                    .role(Role.USER)
                    .build();

            memberRepository.save(user);

            log.info("일반 사용자 계정 생성 완료: user@test.com / 1234");
        }

        /* ================= 환경변수 정책 ================= */

        if (!environmentPolicyRepository.existsById(1L)) {

            EnvironmentPolicy policy = new EnvironmentPolicy(
                    15, // cleaningMinutes
                    15, // reservationDeadlineMinutes
                    20, // refundDeadlineMinutes
                    8,  // maxReservationCount
                    7,  // scheduledToOpenDays
                    20, // openToClosedMinutes
                    10,  // cinemaOpenHour
                    8000  // defaultPrice
            );

            environmentPolicyRepository.save(policy);
            log.info("환경변수 정책 초기 데이터 생성 완료 (id=1)");
        }
    }
}
