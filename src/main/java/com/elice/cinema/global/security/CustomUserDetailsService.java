package com.elice.cinema.global.security;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return new CustomUserDetails(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                "ROLE_" + member.getRole().name()
        );

        // 추후 개발 완료 시 이걸로 전환, 현재는 레디스에 세션 값을 넣어주기 위해 객체가 아닌 필드 값을 직렬화해서 만듦.
        /*return new CustomUserDetails(member);*/
    }
}
