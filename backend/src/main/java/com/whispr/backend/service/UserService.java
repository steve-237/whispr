package com.whispr.backend.service;

import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Profile;
import com.whispr.backend.domain.User;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.ProfileRepository;
import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final LinkRepository linkRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String email, String pseudo, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByPseudo(pseudo)) {
            throw new IllegalArgumentException("Pseudo already taken");
        }

        User user = User.builder()
                .email(email)
                .pseudo(pseudo)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        
        user = userRepository.save(user);

        Profile profile = Profile.builder()
                .user(user)
                .themeId("default")
                .build();
        profileRepository.save(profile);

        Link link = Link.builder()
                .user(user)
                .slug(pseudo.toLowerCase()) // default slug based on pseudo
                .isCustom(false)
                .isActive(true)
                .build();
        linkRepository.save(link);

        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByPseudo(String pseudo) {
        return userRepository.findByPseudo(pseudo);
    }
}
