package com.whispr.backend.security;

import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        com.whispr.backend.domain.User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPseudo(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with: " + identifier)));

        return new User(
                user.getEmail(),
                user.getPasswordHash(),
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
