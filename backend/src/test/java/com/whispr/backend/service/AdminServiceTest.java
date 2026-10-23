package com.whispr.backend.service;

import com.whispr.backend.domain.Role;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.admin.AdminUserDto;
import com.whispr.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("target@whispr.com")
                .pseudo("badActor")
                .role(Role.USER)
                .build();
    }

    @Test
    void getStats_ShouldReturnCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(messageRepository.count()).thenReturn(50L);
        when(profileRepository.count()).thenReturn(8L);

        var stats = adminService.getStats();

        assertEquals(10L, stats.get("totalUsers"));
        assertEquals(50L, stats.get("totalMessages"));
        assertEquals(8L, stats.get("totalProfiles"));
    }

    @Test
    void getUsers_ShouldReturnPaginatedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(mockUser)));

        Page<AdminUserDto> page = adminService.getUsers(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("badActor", page.getContent().get(0).pseudo());
    }

    @Test
    void banUser_ShouldChangeRoleToBanned() {
        when(userRepository.findByPseudo("badActor")).thenReturn(Optional.of(mockUser));

        adminService.banUser("badActor");

        assertEquals(Role.BANNED, mockUser.getRole());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void unbanUser_ShouldChangeRoleToUser() {
        mockUser.setRole(Role.BANNED);
        when(userRepository.findByPseudo("badActor")).thenReturn(Optional.of(mockUser));

        adminService.unbanUser("badActor");

        assertEquals(Role.USER, mockUser.getRole());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void resetPassword_ShouldGenerateNewEncodedPassword() {
        when(userRepository.findByPseudo("badActor")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedNewPass");

        Optional<String> newPassword = adminService.resetPassword("badActor");

        assertTrue(newPassword.isPresent());
        assertEquals("hashedNewPass", mockUser.getPasswordHash());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void deleteUser_ShouldCallDelete() {
        when(userRepository.findByPseudo("badActor")).thenReturn(Optional.of(mockUser));

        adminService.deleteUser("badActor");

        verify(userRepository, times(1)).delete(mockUser);
    }
}
