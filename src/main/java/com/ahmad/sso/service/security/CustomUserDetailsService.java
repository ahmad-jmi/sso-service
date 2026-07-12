package com.ahmad.sso.service.security;

import com.ahmad.sso.service.entity.User;
import com.ahmad.sso.service.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// NOTE: this simple lookup assumes email is globally unique. In true multi-tenant
// login flows you'll typically also need the tenant_id (e.g. from a subdomain,
// header, or the login request body) to disambiguate - wire that in via
// AuthService.login() rather than this generic UserDetailsService contract.
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new CustomUserDetails(user, java.util.List.of());
    }
}
