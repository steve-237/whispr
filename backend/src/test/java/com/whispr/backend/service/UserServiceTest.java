package com.whispr.backend.service;

import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Profile;
import com.whispr.backend.domain.User;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.ProfileRepository;
import com.whispr.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MonetizationService monetizationService;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_WhenPseudoIsReserved_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser("admin@whispr.com", "admin", "SecurePass123!")
        );
        assertEquals("Ce pseudo est réservé par le système.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WhenPseudoIsTooShort_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser("test@whispr.com", "ab", "SecurePass123!")
        );
        assertEquals("Le pseudo doit contenir entre 3 et 30 caractères.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WhenEmailAlreadyExists_ShouldThrowIllegalArgumentException() {
        when(userRepository.existsByEmail("test@whispr.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser("test@whispr.com", "validpseudo", "SecurePass123!")
        );
        assertEquals("Cette adresse email est déjà utilisée.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WhenValid_ShouldSaveUserAndProfileAndLink() {
        when(userRepository.existsByEmail("test@whispr.com")).thenReturn(false);
        when(userRepository.existsByPseudo("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        User result = userService.registerUser("test@whispr.com", "newuser", "SecurePass123!");

        assertNotNull(result);
        assertEquals("test@whispr.com", result.getEmail());
        assertEquals("newuser", result.getPseudo());
        verify(userRepository, times(1)).save(any(User.class));
        verify(profileRepository, times(1)).save(any(Profile.class));
        verify(linkRepository, times(1)).save(any(Link.class));
        verify(monetizationService, times(1)).getOrCreateWallet(any(User.class));
    }
}
