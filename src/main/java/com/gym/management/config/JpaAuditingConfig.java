package com.gym.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Agent 1 - SHARED FILE.
 * Bật JPA Auditing để BaseEntity.createdAt/updatedAt tự động điền.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
