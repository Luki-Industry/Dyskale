package com.lukienlive.hytale.hytale;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.LinkedStorage;
import com.lukienlive.hytale.discord.DiscordLogger;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ConnectionListener implements IEventListener {
    private static String PLAYER_LINKED_MESSAGE = "Joueur %s connecté (Discord lié: %s)";
    private static String PLAYER_UNLINKED_MESSAGE = "Joueur %s connecté (pas de compte Discord lié)";

    private final Set<String> onlinePlayers = new HashSet<>();

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
        registry.register(PlayerSetupConnectEvent.class, this::onPlayerSetupConnect);
        registry.register(PlayerConnectEvent.class, this::onPlayerConnect);
        registry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        logger.log(Level.INFO, "EventsListener enregistré pour PlayerSetupConnectEvent, PlayerConnectEvent et PlayerDisconnectEvent");
    }

    private void onPlayerSetupConnect(PlayerSetupConnectEvent event) {
        String username = event.getUsername();
        String uuid = event.getUuid().toString();

        boolean requireDiscord = config.get().getBoolean("Require_discord_link");
        String discordId = storage.getDiscordId(uuid);

        if (!requireDiscord) {
            String message = discordId != null ?
                    String.format(PLAYER_LINKED_MESSAGE, username, discordId) :
                    String.format(PLAYER_UNLINKED_MESSAGE, username);
            this.logger.log(Level.INFO, message);
            return;
        }

        if (discordId == null) {
            String linkCode = storage.generateLinkCode(username, uuid);

            String kickMessage = "Discord account required!\n\n" +
                    "Your link code: " + linkCode + "\n\n" +
                    "Send this code to the Discord bot in a private message.\n" +
                    "(Code expires in 5 minutes)";

            this.logger.log(Level.WARNING, "Joueur " + username + " refusé: compte Discord non lié (code: " + linkCode + ")");
            discordLogger.warning("⛔ Connexion refusée: " + username + " - Code de liaison: " + linkCode);

            event.setReason(kickMessage);
            event.setCancelled(true);
        } else {
            this.logger.log(Level.INFO, "Joueur " + username + " autorisé (Discord: " + discordId + ")");
        }
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        var player = event.getPlayerRef();
        String discordId = storage.getDiscordId(player.getUuid().toString());

        if (discordId != null) {
            discordLogger.playerJoin(player.getUsername() + " (Discord lié)");
            onlinePlayers.add(player.getUsername());
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        var player = event.getPlayerRef();
        onlinePlayers.remove(player.getUsername());
        discordLogger.playerLeave(player.getUsername());
    }
}
