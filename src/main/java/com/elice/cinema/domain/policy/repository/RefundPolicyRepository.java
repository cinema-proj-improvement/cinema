package com.elice.cinema.domain.policy.repository;

import com.elice.cinema.domain.policy.entity.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {

    boolean existsByBeforeStartMinutes(Integer beforeStartMinutes);

    Optional<RefundPolicy>
    findFirstByBeforeStartMinutesLessThanEqualOrderByBeforeStartMinutesDesc(
            long minutesBeforeStart
    );

}
