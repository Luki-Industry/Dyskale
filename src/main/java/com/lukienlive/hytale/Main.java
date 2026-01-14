package com.lukienlive.hytale;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main extends JavaPlugin {

    public static Main INSTANCE;
    public DiscordBot discordBot;
    public LinkedStorage storage;
    private JsonObject config;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        INSTANCE = this;

        // Créer le dossier de configuration si nécessaire
        File dataFolder = getDataDirectory().toFile();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().atInfo().log("Dossier de configuration créé: " + dataFolder.getAbsolutePath());
        }

        // Charger la configuration (créer si nécessaire)
        loadConfig();

        // Charger stockage JSON
        storage = new LinkedStorage(new File(dataFolder, "linked_players.json"));
        storage.load();

        // Enregistrer l'écouteur d'événements
        new EventsListener().register(getEventRegistry());

        getLogger().atInfo().log("Hytale Discord Plugin initialisé!");
    }

    @Override
    protected void start() {
        // Démarrer bot Discord
        discordBot = new DiscordBot();
        boolean success = discordBot.start();
        
        if (!success) {
            getLogger().atWarning().log("Le bot Discord n'a pas pu démarrer. Vérifiez votre configuration.");
        } else {
            // Installer le handler de logs Discord si activé
            if (getConfigBoolean("enable_console_logs")) {
                DiscordLogHandler.install();
            }
        }
    }

    @Override
    protected void shutdown() {
        // Désinstaller le handler de logs Discord
        DiscordLogHandler.uninstall();
        
        if (discordBot != null) {
            discordBot.shutdown();
        }
        if (storage != null) {
            storage.cleanExpiredCodes();
            storage.save();
        }
        getLogger().atInfo().log("Plugin arrêté.");
    }

    private void loadConfig() {
        File configFile = getDataDirectory().resolve("config.json").toFile();
        
        // Charger config existante ou créer une nouvelle
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = JsonParser.parseReader(reader).getAsJsonObject();
                getLogger().atInfo().log("Configuration chargée depuis: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Erreur lors du chargement de config.json");
                config = new JsonObject();
            }
        } else {
            config = new JsonObject();
        }
        
        // Fusionner avec les valeurs par défaut (ajouter les paramètres manquants)
        boolean needsSave = false;
        
        if (!config.has("discord_token")) {
            config.addProperty("discord_token", "VOTRE_TOKEN_DISCORD_ICI");
            needsSave = true;
        }
        
        if (!config.has("guild_id")) {
            config.addProperty("guild_id", "VOTRE_GUILD_ID_ICI");
            needsSave = true;
        }
        
        if (!config.has("require_discord_link")) {
            config.addProperty("require_discord_link", false);
            needsSave = true;
        }
        
        if (!config.has("kick_message")) {
            config.addProperty("kick_message", "Vous devez lier votre compte Discord pour rejoindre ce serveur. Utilisez !link sur notre Discord.");
            needsSave = true;
        }
        
        if (!config.has("console_channel_id")) {
            config.addProperty("console_channel_id", "VOTRE_CHANNEL_ID_ICI");
            needsSave = true;
        }
        
        if (!config.has("enable_console_logs")) {
            config.addProperty("enable_console_logs", true);
            needsSave = true;
        }
        
        // Sauvegarder si des paramètres ont été ajoutés
        if (needsSave) {
            try {
                java.nio.file.Files.writeString(configFile.toPath(), 
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(config));
                
                if (!configFile.exists()) {
                    getLogger().atWarning().log("Fichier config.json créé avec les valeurs par défaut.");
                    getLogger().atWarning().log("Veuillez configurer votre token Discord dans: " + configFile.getAbsolutePath());
                } else {
                    getLogger().atInfo().log("Configuration mise à jour avec les nouveaux paramètres.");
                }
            } catch (IOException e) {
                getLogger().atSevere().withCause(e).log("Impossible de sauvegarder config.json");
            }
        }
    }

    public String getConfigString(String key) {
        return config.has(key) ? config.get(key).getAsString() : null;
    }

    public boolean getConfigBoolean(String key) {
        return config.has(key) && config.get(key).getAsBoolean();
    }
}
