package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Role;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.VisitorAnalyticsDto;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorAnalyticsServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VisitorAnalyticsService visitorAnalyticsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("analytics@whispr.com")
                .pseudo("statsUser")
                .role(Role.USER)
                .build();
    }

    @Test
    void getVisitorAnalytics_ShouldAggregateSourcesAndCities() {
        when(userRepository.findByEmailIgnoreCase("analytics@whispr.com")).thenReturn(Optional.of(mockUser));

        AuditLog log1 = AuditLog.builder()
                .hashedIp("hash1")
                .city("Paris")
                .country("France 🇫🇷")
                .referrer("https://instagram.com/p/123")
                .build();

        AuditLog log2 = AuditLog.builder()
                .hashedIp("hash2")
                .city("Lyon")
                .country("France 🇫🇷")
                .referrer("https://tiktok.com/")
                .build();

        when(auditLogRepository.findByUserId(mockUser.getId())).thenReturn(List.of(log1, log2));

        VisitorAnalyticsDto dto = visitorAnalyticsService.getVisitorAnalytics("analytics@whispr.com");

        assertNotNull(dto);
        assertEquals(2, dto.totalMessages());
        assertTrue(dto.sources().containsKey("Instagram"));
        assertTrue(dto.sources().containsKey("TikTok"));
        assertEquals(1, dto.topCities().get("Paris"));
        assertEquals(1, dto.topCities().get("Lyon"));
    }

    @Test
    void getVisitorAnalytics_ShouldProvideDefaultValues_WhenEmpty() {
        when(userRepository.findByEmailIgnoreCase("analytics@whispr.com")).thenReturn(Optional.of(mockUser));
        when(auditLogRepository.findByUserId(mockUser.getId())).thenReturn(List.of());

        VisitorAnalyticsDto dto = visitorAnalyticsService.getVisitorAnalytics("analytics@whispr.com");

        assertNotNull(dto);
        assertEquals(0, dto.totalMessages());
        assertTrue(dto.sources().containsKey("Direct / Lien partagé"));
        assertTrue(dto.topCities().containsKey("Paris"));
    }
}
