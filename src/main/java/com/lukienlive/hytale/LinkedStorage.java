package com.lukienlive.hytale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lukienlive.hytale.domain.PendingLink;
import com.lukienlive.hytale.inject.annotation.LinkedStorageFile;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class LinkedStorage {

    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> links = new ConcurrentHashMap<>();

    // Codes de liaison temporaires : code -> {playerName, timestamp}
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    @Inject
    public LinkedStorage(@LinkedStorageFile File file) {
        this.file = file;
    }

    public synchronized void load() {
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> data = gson.fromJson(reader, type);
            if (data != null) {
                links.clear();
                links.putAll(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(links, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void link(String playerUuid, String discordId) {
        links.put(playerUuid, discordId);
        save();
    }

    public String getDiscordId(String playerUuid) {
        return links.get(playerUuid);
    }

    public String generateLinkCode(String playerName, String playerUuid) {
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
