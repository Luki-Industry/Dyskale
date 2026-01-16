package com.lukienlive.hytale.application.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lukienlive.hytale.domain.PendingLink;
import com.lukienlive.hytale.domain.repository.LinkRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class LinkService {

    private final LinkRepository linkRepository;
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    @Inject
    public LinkService(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public void init() {
        linkRepository.load();
    }

    public void shutdown() {
        cleanExpiredCodes();
        linkRepository.save();
    }

    public boolean isLinked(UUID playerUuid) {
        return linkRepository.getDiscordId(playerUuid) != null;
    }

    public String getDiscordId(UUID playerUuid) {
        return linkRepository.getDiscordId(playerUuid);
    }

    public UUID getPlayerUuid(String discordId) {
        return linkRepository.getPlayerUuid(discordId);
    }

    public void linkAccount(UUID playerUuid, String discordId) {
        linkRepository.saveLink(playerUuid, discordId);
    }

    public void unlinkAccount(UUID playerUuid) {
        linkRepository.removeLink(playerUuid);
    }

    public String  generateLinkCode(String playerName, UUID playerUuid) {
        // Check if there is already a code? Maybe refreshing it is better.
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        pendingLinks.put(code, new PendingLink(playerName, playerUuid.toString()));
        return code;
    }

    public Optional<PendingLink> consumeLinkCode(String code) {
        PendingLink link = pendingLinks.remove(code);
        if (link == null || link.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(link);
    }

    public void cleanExpiredCodes() {
        pendingLinks.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
