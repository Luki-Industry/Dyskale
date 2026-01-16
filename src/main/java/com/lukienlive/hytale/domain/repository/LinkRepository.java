package com.lukienlive.hytale.domain.repository;

import java.util.UUID;

public interface LinkRepository {
    void load();
    void save();
    void saveLink(UUID playerUuid, String discordId);
    void removeLink(UUID playerUuid);
    String getDiscordId(UUID playerUuid);
    UUID getPlayerUuid(String discordId);
}
