package com.lukienlive.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.hytale.ConnectionListener;
import com.lukienlive.hytale.hytale.HytaleConfig;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.File;

@Getter
public class Main extends JavaPlugin {

    public static Main INSTANCE;
    private DiscordBot discordBot;
    private LinkedStorage storage;
    private Config<HytaleConfig> config;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);

        this.config = withConfig("config", HytaleConfig.CODEC);
    }

    @Override
    protected void setup() {
        INSTANCE = this;

        this.config.load().join();
        this.config.save().join();

        // Créer le dossier de configuration si nécessaire
        File dataFolder = getDataDirectory().toFile();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().atInfo().log("Dossier de configuration créé: " + dataFolder.getAbsolutePath());
        }
        // Charger stockage JSON
        storage = new LinkedStorage(new File(dataFolder, "linked_players.json"));
        storage.load();

        // Enregistrer l'écouteur d'événements
        new ConnectionListener().Register(getEventRegistry());

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
            if (config.get().getBoolean("Enable_console_logs")) {
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
}
