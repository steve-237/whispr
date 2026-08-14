package com.whispr.backend.config;

import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Profile;
import com.whispr.backend.domain.Role;
import com.whispr.backend.domain.User;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.ProfileRepository;
import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DemoInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final LinkRepository linkRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        Optional<User> existingUser = userRepository.findByPseudo("demo");
        
        if (existingUser.isEmpty()) {
            User demoUser = User.builder()
                    .email("demo@whispr.com")
                    .pseudo("demo")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.USER)
                    .build();
            userRepository.save(demoUser);

            Profile profile = Profile.builder()
                    .user(demoUser)
                    .bio("Bienvenue sur le compte de Démo ! Laissez-moi un message secret.")
                    .dailyQuestion("Testez l'envoi de message ci-dessous !")
                    .themeId("default")
                    .build();
            profileRepository.save(profile);

            Link link = Link.builder()
                    .user(demoUser)
                    .slug("demo")
                    .isActive(true)
                    .build();
            linkRepository.save(link);
            
            System.out.println("Compte demo@whispr.com créé avec le mot de passe 'password123'");
        } else {
            // S'assurer qu'il a le rôle ADMIN et le bon mot de passe, et le bon email
            User user = existingUser.get();
            user.setEmail("demo@whispr.com");
            user.setRole(Role.USER);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            userRepository.save(user);
        }
    }
}
