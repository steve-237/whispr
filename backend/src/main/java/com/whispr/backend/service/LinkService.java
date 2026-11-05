package com.whispr.backend.service;

import com.whispr.backend.domain.Link;
import com.whispr.backend.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;

    @Transactional(readOnly = true)
    public Optional<Link> getLinkBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        String cleanSlug = (slug.startsWith("@") ? slug.substring(1) : slug).trim();
        return linkRepository.findBySlugIgnoreCase(cleanSlug);
    }

    @Transactional
    public Link updateLinkStatus(UUID linkId, boolean isActive) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));
        link.setIsActive(isActive);
        return linkRepository.save(link);
    }
}
