package com.lukienlive.hytale;

import com.google.gson.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lukienlive.hytale.domain.PendingLink;
import com.lukienlive.hytale.inject.annotation.LinkedStorageFile;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class LinkedStorage {

    private final File file;
    private HashMap<String, String> map = new HashMap<>();

    // Codes de liaison temporaires : code -> {playerName, timestamp}
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    @Inject
    public LinkedStorage(@LinkedStorageFile File file) {
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
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        pendingLinks.put(code, new PendingLink(playerName, playerUuid));
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
