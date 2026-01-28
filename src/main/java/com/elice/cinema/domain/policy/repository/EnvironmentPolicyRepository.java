package com.elice.cinema.domain.policy.repository;

import com.elice.cinema.domain.policy.entity.EnvironmentPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentPolicyRepository extends JpaRepository<EnvironmentPolicy, Long> {
}
