package com.lukienlive.hytale.hytale;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.application.service.LinkService;
import com.lukienlive.hytale.discord.DiscordBot;
import com.lukienlive.hytale.discord.DiscordLogger;
import com.lukienlive.hytale.discord.RoleSyncService;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
    private LinkService linkService;

    @Inject
    private DiscordLogger discordLogger;

    @Inject
    private DiscordBot discordBot;

    @Inject
    private RoleSyncService roleSyncService;

    @Override
    public void Register(EventRegistry registry) {
        registry.register(PlayerSetupConnectEvent.class, this::onPlayerSetupConnect);
        registry.register(PlayerConnectEvent.class, this::onPlayerConnect);
        registry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        logger.log(Level.INFO, "EventsListener enregistré pour PlayerSetupConnectEvent, PlayerConnectEvent et PlayerDisconnectEvent");
    }

    private void onPlayerSetupConnect(PlayerSetupConnectEvent event) {
        String username = event.getUsername();
        String uuidString = event.getUuid().toString();
        java.util.UUID uuid = event.getUuid();

        boolean requireDiscord = config.get().getBoolean("Require_discord_link");
        String discordId = linkService.getDiscordId(uuid);

        if (!requireDiscord) {
            String message = discordId != null ?
                    String.format(PLAYER_LINKED_MESSAGE, username, discordId) :
                    String.format(PLAYER_UNLINKED_MESSAGE, username);
            this.logger.log(Level.INFO, message);
            return;
        }

        // Compte non lié
        if (discordId == null) {
            String linkCode = linkService.generateLinkCode(username, uuid);

            String kickMessage = config.get().getString("Link_message")
                    .replace("{code}", linkCode)
                    .replace("{bot_username}", discordBot.getBotUsername())
                    .replace("{discord_invite}", config.get().getString("Discord_invite_link"));

            this.logger.log(Level.WARNING, "Joueur " + username + " refusé: compte Discord non lié (code: " + linkCode + ")");
            discordLogger.warning("⛔ Connexion refusée: " + username + " - Code de liaison: " + linkCode);

            event.setReason(kickMessage);
            event.setCancelled(true);
            return;
        }

        // Compte lié - vérifier si le joueur est toujours sur le serveur Discord
        boolean requireGuildMembership = config.get().getBoolean("Require_guild_membership");
        if (requireGuildMembership && !discordBot.isUserInGuild(discordId)) {
            String kickMessage = config.get().getString("Not_in_guild_message")
                    .replace("{discord_invite}", config.get().getString("Discord_invite_link"));

            this.logger.log(Level.WARNING, "Joueur " + username + " refusé: compte lié mais plus membre du Discord (ID: " + discordId + ")");
            discordLogger.warning("⛔ Connexion refusée: " + username + " - Compte lié mais plus sur le Discord");

            event.setReason(kickMessage);
            event.setCancelled(true);
            return;
        }

        // Vérifier si le joueur a le rôle requis
        boolean requireRole = config.get().getBoolean("Require_role");
        if (requireRole) {
            var requiredRoles = config.get().getStringList("Required_role_ids");
            if (!discordBot.hasRequiredRole(discordId, requiredRoles)) {
                String kickMessage = config.get().getString("Missing_role_message")
                        .replace("{discord_invite}", config.get().getString("Discord_invite_link"));

                this.logger.log(Level.WARNING, "Joueur " + username + " refusé: n'a pas le rôle requis (ID: " + discordId + ")");
                discordLogger.warning("⛔ Connexion refusée: " + username + " - Rôle Discord manquant");

                event.setReason(kickMessage);
                event.setCancelled(true);
                return;
            }
        }

        // Tout est OK
        this.logger.log(Level.INFO, "Joueur " + username + " autorisé (Discord: " + discordId + ")");
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        var player = event.getPlayerRef();
        String discordId = linkService.getDiscordId(player.getUuid());

        if (discordId != null) {
            discordLogger.playerJoin(player.getUsername() + " (Discord lié)");
            roleSyncService.syncUser(player.getUuid(), discordId);
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        var player = event.getPlayerRef();
        discordLogger.playerLeave(player.getUsername());
    }
}
