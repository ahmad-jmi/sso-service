package com.ahmad.sso.service.repository;

import com.ahmad.sso.service.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserIdAndTenantId(UUID userId, UUID tenantId);
}
