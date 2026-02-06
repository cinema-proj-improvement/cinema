package com.elice.cinema.domain.policy.service;

import com.elice.cinema.domain.policy.entity.EnvironmentPolicy;
import com.elice.cinema.domain.policy.repository.EnvironmentPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnvironmentPolicyService {
    private final EnvironmentPolicyRepository repo;

    // @Cacheable - 한 번 조회한 결과를 캐쉬에 저장, @CacheEvict - 캐쉬 삭제
    @Cacheable(cacheNames = "environmentPolicy", key = "'policy'")
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

    public int getRefundDeadlineMinutes() {
        return getPolicy().getRefundDeadlineMinutes();
    }

    public int getMaxReservationCount() {
        return getPolicy().getMaxReservationCount();
    }

    public int getDefaultPrice() {
        return getPolicy().getDefaultPrice();
    }
}
