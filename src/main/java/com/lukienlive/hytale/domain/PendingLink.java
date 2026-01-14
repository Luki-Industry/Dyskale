package com.lukienlive.hytale.domain;

import lombok.Data;

/**
 * Data model for a pending Discord link request.
 */
@Data
public class PendingLink {
    private final String playerName;
    private final String playerUuid;

    private long timestamp;

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 5 * 60 * 1000; // 5 minutes
    }
}
