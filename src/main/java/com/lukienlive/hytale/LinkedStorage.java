package com.lukienlive.hytale;

import com.google.gson.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LinkedStorage {

    private final File file;
    private HashMap<String, String> map = new HashMap<>();

    // Codes de liaison temporaires : code -> {playerName, timestamp}
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    public static class PendingLink {
        public final String playerName;
        public final String playerUuid;
        public final long timestamp;

        public PendingLink(String playerName, String playerUuid) {
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 5 * 60 * 1000; // 5 minutes
        }
    }

    public LinkedStorage(File file) {
        this.file = file;
    }

    public void load() {
        try (Reader reader = new FileReader(file)) {
            map = new Gson().fromJson(reader, map.getClass());
        } catch (IOException e) { /* fichier manquant → ok */ }
    }

    public void save() {
        try (Writer writer = new FileWriter(file)) {
            new Gson().toJson(map, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void link(String playerUuid, String discordId) {
        map.put(playerUuid, discordId);
        save();
    }

    public String getDiscordId(String playerUuid) {
        return map.get(playerUuid);
    }

    public String generateLinkCode(String playerName, String playerUuid) {
        // Générer un code aléatoire de 6 caractères
        String code = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        pendingLinks.put(code, new PendingLink(playerName, playerUuid));
        return code;
    }

    public PendingLink consumeLinkCode(String code) {
        PendingLink pending = pendingLinks.remove(code.toUpperCase());
        if (pending == null || pending.isExpired()) {
            return null;
        }
        return pending;
    }

    public void cleanExpiredCodes() {
        pendingLinks.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
