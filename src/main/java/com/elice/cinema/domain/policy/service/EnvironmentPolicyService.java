package com.elice.cinema.domain.policy.service;

import com.elice.cinema.domain.policy.entity.EnvironmentPolicy;
import com.elice.cinema.domain.policy.repository.EnvironmentPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnvironmentPolicyService {
    private final EnvironmentPolicyRepository repo;

    public EnvironmentPolicy getPolicy() {
        return repo.findById(1L)
                .orElseThrow(() -> new IllegalStateException("환경 정책이 없습니다."));
    }

    public int getCleaningMinutes() {
        return getPolicy().getCleaningMinutes();
    }

    public int getScheduledToOpenDays() {
        return getPolicy().getScheduledToOpenDays();
    }
}
