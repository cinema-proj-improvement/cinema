package com.elice.cinema.domain.member.service;

import com.elice.cinema.domain.auth.dto.request.SignupRequest;
import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.entity.Role;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SignupRequest req) {
        if (!req.getPassword().equals(req.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_MISMATCH);
        }

        if (memberRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }

        if (memberRepository.existsByNickname(req.getNickname())) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }

        int age = calculateAge(req.getBirthDate());

        Member member = Member.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .nickname(req.getNickname())
                .age(age)
                .role(Role.USER)
                .build();

        return memberRepository.save(member).getId();
    }

    private int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
