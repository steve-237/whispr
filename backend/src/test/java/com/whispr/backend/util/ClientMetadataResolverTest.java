package com.whispr.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientMetadataResolverTest {

    private ClientMetadataResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientMetadataResolver();
    }

    @Test
    void extractClientIp_ShouldPrioritizeCloudflareHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.195");
        request.addHeader("X-Forwarded-For", "198.51.100.10, 192.0.2.1");
        request.setRemoteAddr("127.0.0.1");

        String ip = resolver.extractClientIp(request);
        assertEquals("203.0.113.195", ip);
    }

    @Test
    void extractClientIp_ShouldUseFirstIpInXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.10, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");

        String ip = resolver.extractClientIp(request);
        assertEquals("198.51.100.10", ip);
    }

    @Test
    void hashIp_ShouldProduceConsistentSha256Hex() {
        String hash1 = resolver.hashIp("198.51.100.10");
        String hash2 = resolver.hashIp("198.51.100.10");
        String hashOther = resolver.hashIp("198.51.100.11");

        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 is 64 hex characters
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hashOther);
    }

    @Test
    void resolve_ShouldPopulateMetadataFields() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "198.51.100.50");
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)");
        request.addHeader("CF-IPCountry", "FR");
        request.addHeader("CF-IPCity", "Lyon");
        request.addHeader("Referer", "https://instagram.com/");

        ClientMetadata metadata = resolver.resolve(request);

        assertEquals("198.51.100.50", metadata.rawIp());
        assertEquals("FR", metadata.country());
        assertEquals("Lyon", metadata.city());
        assertEquals("https://instagram.com/", metadata.referrer());
        assertNotNull(metadata.hashedIp());
    }
}
