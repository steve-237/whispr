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
    private final com.whispr.backend.repository.WalletRepository walletRepository;

    @Transactional
    public User registerUser(String email, String pseudo, String password) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String cleanPseudo = pseudo != null ? pseudo.trim().replaceAll("\\s+", "").toLowerCase() : "";

        if (cleanEmail.isEmpty() || cleanPseudo.isEmpty() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Tous les champs sont obligatoires.");
        }

        if (cleanPseudo.length() < 3 || cleanPseudo.length() > 30) {
            throw new IllegalArgumentException("Le pseudo doit contenir entre 3 et 30 caractères.");
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Cette adresse email est déjà utilisée.");
        }
        if (userRepository.existsByPseudo(cleanPseudo)) {
            throw new IllegalArgumentException("Ce pseudo est déjà pris.");
        }

        User user = User.builder()
                .email(cleanEmail)
                .pseudo(cleanPseudo)
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
                .slug(cleanPseudo) // default slug based on sanitized pseudo
                .isCustom(false)
                .isActive(true)
                .build();
        linkRepository.save(link);

        // Initialisation immédiate du Wallet avec 30 pièces de bienvenue
        com.whispr.backend.domain.Wallet wallet = com.whispr.backend.domain.Wallet.builder()
                .user(user)
                .coins(30)
                .totalSpentEur(java.math.BigDecimal.ZERO)
                .isPro(false)
                .build();
        walletRepository.save(wallet);

        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmailIgnoreCase(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByPseudoIgnoreCase(String pseudo) {
        return userRepository.findByPseudoIgnoreCase(pseudo);
    }
}
