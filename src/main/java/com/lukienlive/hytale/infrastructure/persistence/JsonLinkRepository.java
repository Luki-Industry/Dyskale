package com.lukienlive.hytale.infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lukienlive.hytale.domain.repository.LinkRepository;
import com.lukienlive.hytale.inject.annotation.LinkedStorageFile;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class JsonLinkRepository implements LinkRepository {

    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> links = new ConcurrentHashMap<>();

    @Inject
    public JsonLinkRepository(@LinkedStorageFile File file) {
        this.file = file;
    }

    @Override
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

    @Override
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

    @Override
    public void saveLink(UUID playerUuid, String discordId) {
        links.put(playerUuid.toString(), discordId);
        save();
    }

    @Override
    public void removeLink(UUID playerUuid) {
        links.remove(playerUuid.toString());
        save();
    }

    @Override
    public String getDiscordId(UUID playerUuid) {
        return links.get(playerUuid.toString());
    }

    @Override
    public UUID getPlayerUuid(String discordId) {
        for (Map.Entry<String, String> entry : links.entrySet()) {
            if (entry.getValue().equals(discordId)) {
                try {
                    return UUID.fromString(entry.getKey());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
