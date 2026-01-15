package com.lukienlive.hytale.hytale;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.LinkedStorage;
import com.lukienlive.hytale.discord.DiscordLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ConnectionListener implements IEventListener {
    private static String PLAYER_LINKED_MESSAGE = "Joueur %s connecté (Discord lié: %s)";
    private static String PLAYER_UNLINKED_MESSAGE = "Joueur %s connecté (pas de compte Discord lié)";

    @Inject
    private Logger logger;

    @Inject
    private Config<HytaleConfig> config;

    @Inject
    private LinkedStorage storage;

    @Inject
    private DiscordLogger discordLogger;

    @Override
    public void Register(EventRegistry registry) {
        registry.register(PlayerConnectEvent.class, this::onPlayerConnect);
        logger.log(Level.INFO, "EventsListener enregistré pour PlayerConnectEvent");
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        var player = event.getPlayerRef();

        boolean requireDiscord = config.get().getBoolean("Require_discord_link");
        String discordId = storage.getDiscordId(player.getUuid().toString());

        if (!requireDiscord) {
            this.logger.log(Level.INFO, discordId != null ?
                    String.format(PLAYER_LINKED_MESSAGE, player.getUsername(), discordId) :
                    String.format(PLAYER_UNLINKED_MESSAGE, player.getUsername()));
            return;
        }

        if (discordId == null) {
            String linkCode = storage.generateLinkCode(player.getUsername(), player.getUuid().toString());

            String kickMessage = "=== COMPTE DISCORD REQUIS ===\n\n" +
                    "Vous devez lier votre compte Discord\npour rejoindre ce serveur.\n\n" +
                    "Votre code de liaison:\n" + linkCode + "\n\n" +
                    "Envoyez UNIQUEMENT ce code en message privé\nau bot Discord sur le serveur.\n\n" +
                    "(Le code expire dans 5 minutes)";

            this.logger.log(Level.WARNING, "Joueur " + player.getUsername() + " refusé: compte Discord non lié (code: " + linkCode + ")");
            discordLogger.warning("⛔ Connexion refusée: " + player.getUsername() + " - Code de liaison: " + linkCode);

            player.getPacketHandler().disconnect(kickMessage);
        } else {
            this.logger.log(Level.INFO, "Joueur " + player.getUsername() + " autorisé (Discord: " + discordId + ")");
            discordLogger.playerJoin(player.getUsername() + " (Discord lié)");
        }
    }
}
