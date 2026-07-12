package com.ahmad.sso.service.repository;

import com.ahmad.sso.service.entity.AuthProvider;
import com.ahmad.sso.service.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository extends JpaRepository<Identity, UUID> {
    Optional<Identity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
