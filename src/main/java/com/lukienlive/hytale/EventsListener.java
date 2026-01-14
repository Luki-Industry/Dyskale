package com.lukienlive.hytale;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;

public class EventsListener {

    private static String PLAYER_LINKED_MESSAGE = "Joueur %s connecté (Discord lié: %s)";
    private static String PLAYER_UNLINKED_MESSAGE = "Joueur %s connecté (pas de compte Discord lié)";

    public void register(EventRegistry eventRegistry) {
        // Enregistrer l'écouteur pour PlayerSetupConnectEvent (avant l'ajout au monde)
        //eventRegistry.register(PlayerSetupConnectEvent.class, this::onPlayerSetupConnect);
        eventRegistry.register(PlayerConnectEvent.class, this::onPlayerConnect);
        Main.INSTANCE.getLogger().atInfo().log("EventsListener enregistré pour PlayerSetupConnectEvent");
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        var player = event.getPlayerRef();

        boolean requireDiscord = Main.INSTANCE.getConfigBoolean("require_discord_link");
        String discordId = Main.INSTANCE.storage.getDiscordId(player.getUuid().toString());

        if (!requireDiscord) {
            Main.INSTANCE.getLogger().atInfo().log(discordId != null ?
                    String.format(PLAYER_LINKED_MESSAGE, player.getUsername(), discordId) :
                    String.format(PLAYER_UNLINKED_MESSAGE, player.getUsername()));
            return;
        }

        if (discordId == null) {
            String linkCode = Main.INSTANCE.storage.generateLinkCode(player.getUsername(), player.getUuid().toString());

            String kickMessage = "=== COMPTE DISCORD REQUIS ===\n\n" +
                    "Vous devez lier votre compte Discord\npour rejoindre ce serveur.\n\n" +
                    "Votre code de liaison:\n" + linkCode + "\n\n" +
                    "Envoyez UNIQUEMENT ce code en message privé\nau bot Discord sur le serveur.\n\n" +
                    "(Le code expire dans 5 minutes)";

            Main.INSTANCE.getLogger().atWarning().log("Joueur " + player.getUsername() + " refusé: compte Discord non lié (code: " + linkCode + ")");
            DiscordLogHandler.logWarning("⛔ Connexion refusée: " + player.getUsername() + " - Code de liaison: " + linkCode);

            player.getPacketHandler().disconnect(kickMessage);
        } else {
            Main.INSTANCE.getLogger().atInfo().log("Joueur " + player.getUsername() + " autorisé (Discord: " + discordId + ")");
            DiscordLogHandler.logPlayerJoin(player.getUsername() + " (Discord lié)");
        }

    }

    private void onPlayerSetupConnect(PlayerSetupConnectEvent event) {
        String playerName = event.getUsername();
        String playerUuid = event.getUuid().toString();
        
        // Vérifier si le requirement Discord est activé
        boolean requireDiscord = Main.INSTANCE.getConfigBoolean("require_discord_link");
        
        if (!requireDiscord) {
            // Si pas requis, juste logger si le joueur est lié
            String discordId = Main.INSTANCE.storage.getDiscordId(playerUuid);
            if (discordId != null) {
                Main.INSTANCE.getLogger().atInfo().log("Joueur " + playerName + " connecté (Discord lié: " + discordId + ")");
            } else {
                Main.INSTANCE.getLogger().atInfo().log("Joueur " + playerName + " connecté (pas de compte Discord lié)");
            }
            return;
        }
        
        // Vérifier si le joueur a lié son compte Discord
        String discordId = Main.INSTANCE.storage.getDiscordId(playerUuid);
        
        if (discordId == null) {
            // Générer un code de liaison temporaire
            String linkCode = Main.INSTANCE.storage.generateLinkCode(playerName, playerUuid);
            
            // Message de kick simple (Hytale ne supporte pas le markup HTML)
            String kickMessage = "=== COMPTE DISCORD REQUIS ===\n\n" +
                    "Vous devez lier votre compte Discord\npour rejoindre ce serveur.\n\n" +
                    "Votre code de liaison:\n" + linkCode + "\n\n" +
                    "Envoyez UNIQUEMENT ce code en message privé\nau bot Discord sur le serveur.\n\n" +
                    "(Le code expire dans 5 minutes)";
            
            Main.INSTANCE.getLogger().atWarning().log("Joueur " + playerName + " refusé: compte Discord non lié (code: " + linkCode + ")");
            DiscordLogHandler.logWarning("⛔ Connexion refusée: " + playerName + " - Code de liaison: " + linkCode);
            
            // Annuler la connexion avec setCancelled + setReason (ne PAS utiliser disconnect en plus)
            event.setReason(kickMessage);
            event.setCancelled(true);
        } else {
            Main.INSTANCE.getLogger().atInfo().log("Joueur " + playerName + " autorisé (Discord: " + discordId + ")");
            DiscordLogHandler.logPlayerJoin(playerName + " (Discord lié)");
        }
    }
}
