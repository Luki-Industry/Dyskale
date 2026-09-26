package com.lukienlive.hytale;

import com.google.inject.Inject;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.discord.DiscordBot;
import com.lukienlive.hytale.discord.DiscordLogger;
import com.lukienlive.hytale.discord.RoleSyncService;
import com.lukienlive.hytale.hytale.ConnectionListener;
import com.lukienlive.hytale.hytale.GameplayListener;
import com.lukienlive.hytale.hytale.HytaleConfig;
import com.lukienlive.hytale.inject.HytaleInjector;
import com.lukienlive.hytale.application.service.LinkService;
import com.lukienlive.hytale.hytale.commands.DiscordCommand;
import com.lukienlive.hytale.hytale.commands.OuihebergCommand;
import com.lukienlive.hytale.infrastructure.update.UpdateCheckService;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

import javax.annotation.Nonnull;

@Getter
public class Main extends JavaPlugin {
    private final Config<HytaleConfig> config;
    public static Main INSTANCE;

    @Inject
    private LinkService linkService;

    @Inject
    private ConnectionListener connectionListener;

    @Inject
    private GameplayListener gameplayListener;

    @Inject
    private DiscordBot discordBot;

    @Inject
    private DiscordLogger discordLogger;

    @Inject
    private RoleSyncService roleSyncService;

    @Inject
    private UpdateCheckService updateCheckService;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);

        this.config = withConfig("config", HytaleConfig.CODEC);
    }

    @Override
    protected void setup() {
        try {
            INSTANCE = this;

            this.config.load().join();
            this.config.save().join();
        }
        catch (Exception e) {
            getLogger().atSevere().log("Erreur lors de l'initialisation du plugin: " + e.getMessage());
        }
    }

    @Override
    protected void start() {
        try {
            LuckPerms api = LuckPermsProvider.get();

            HytaleInjector injector = new HytaleInjector(this, config, api);
            var injectorInstance = injector.createInjector();
            injectorInstance.injectMembers(this);

            linkService.init();

            this.connectionListener.Register(this.getEventRegistry());
            this.gameplayListener.Register(this.getEventRegistry());

            // Enregistrement des commandes
            getCommandRegistry().registerCommand(injectorInstance.getInstance(DiscordCommand.class));
            getCommandRegistry().registerCommand(new OuihebergCommand());

            getLogger().atInfo().log("Plugin Dyskale initialisé - version plugin 1.0.0, serveur Hytale "
                    + getHytaleServerVersion());
            updateCheckService.start();

            if (!discordBot.start()) {
                getLogger().atWarning().log("Le bot Discord n'a pas pu démarrer. Vérifiez votre configuration.");
            } else {
                if (config.get().getBoolean("Enable_console_logs")) {
                    discordLogger.install();
                }
                roleSyncService.init();
            }
        }
        catch (Exception e) {
            getLogger().atSevere().log("Erreur lors du démarrage du plugin: " + e.getMessage());
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

        if (linkService != null) {
            linkService.cleanExpiredCodes();
            linkService.shutdown();
        }
        if (updateCheckService != null) {
            updateCheckService.shutdown();
        }
        getLogger().atInfo().log("Plugin arrêté.");

        this.config.save().join();
    }

    private String getHytaleServerVersion() {
        String version = HytaleServer.class.getPackage().getImplementationVersion();
        return version != null ? version : "inconnue";
    }
}
