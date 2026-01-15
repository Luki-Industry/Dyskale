package com.lukienlive.hytale;

import com.google.inject.Inject;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.discord.DiscordBot;
import com.lukienlive.hytale.discord.DiscordLogger;
import com.lukienlive.hytale.hytale.ConnectionListener;
import com.lukienlive.hytale.hytale.HytaleConfig;
import com.lukienlive.hytale.inject.HytaleInjector;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class Main extends JavaPlugin {
    private Config<HytaleConfig> config;
    public static Main INSTANCE;

    @Inject
    private LinkedStorage storage;

    @Inject
    private ConnectionListener connectionListener;

    @Inject
    private DiscordBot discordBot;

    @Inject
    private DiscordLogger discordLogger;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);

        this.config = withConfig("config", HytaleConfig.CODEC);
    }

    @Override
    protected void setup() {
        INSTANCE = this;

        this.config.load().join();
        this.config.save().join();

        HytaleInjector injector = new HytaleInjector(this, config);
        var injectorInstance = injector.createInjector();
        injectorInstance.injectMembers(this);

        storage.load();

        this.connectionListener.Register(this.getEventRegistry());

        getLogger().atInfo().log("Hytale Discord Plugin initialisé!");
    }

    @Override
    protected void start() {
        if (!discordBot.start()) {
            getLogger().atWarning().log("Le bot Discord n'a pas pu démarrer. Vérifiez votre configuration.");
        } else if (config.get().getBoolean("Enable_console_logs")){
            discordLogger.install();
        }
    }

    @Override
    protected void shutdown() {
        if (discordLogger != null) {
            discordLogger.uninstall();
        }

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
