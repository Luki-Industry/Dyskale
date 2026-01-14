package com.lukienlive.hytale;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;

public class EventsListener {

    public void register(EventRegistry eventRegistry) {
        // Enregistrer l'écouteur pour PlayerSetupConnectEvent (avant l'ajout au monde)
        eventRegistry.register(PlayerSetupConnectEvent.class, this::onPlayerSetupConnect);
        Main.INSTANCE.getLogger().atInfo().log("EventsListener enregistré pour PlayerSetupConnectEvent");
    }

    private void onPlayerSetupConnect(PlayerSetupConnectEvent event) {
        String playerName = event.getUsername();
        
        // Vérifier si le requirement Discord est activé
        boolean requireDiscord = Main.INSTANCE.getConfigBoolean("require_discord_link");
        
        if (!requireDiscord) {
            // Si pas requis, juste logger si le joueur est lié
            String discordId = Main.INSTANCE.storage.getDiscordId(playerName);
            if (discordId != null) {
                Main.INSTANCE.getLogger().atInfo().log("Joueur " + playerName + " connecté (Discord lié: " + discordId + ")");
            } else {
                Main.INSTANCE.getLogger().atInfo().log("Joueur " + playerName + " connecté (pas de compte Discord lié)");
            }
            return;
        }
        
        // Vérifier si le joueur a lié son compte Discord
        String discordId = Main.INSTANCE.storage.getDiscordId(playerName);
        
        if (discordId == null) {
            // Générer un code de liaison temporaire
            String linkCode = Main.INSTANCE.storage.generateLinkCode(playerName);
            
            // Envoyer le code par Discord DM si possible (via un webhook ou notification)
            // Pour l'instant, juste refuser la connexion avec un message simple
            String kickMessage = "§c§lCompte Discord requis\n\n" +
                    "§eVous devez lier votre compte Discord pour rejoindre ce serveur.\n\n" +
                    "§fRendez-vous sur notre Discord et consultez le salon #instructions\n" +
                    "§fVotre code de liaison: §b§l" + linkCode + "\n\n" +
                    "§fEnvoyez ce code en MP au bot: §a" + Main.INSTANCE.discordBot.getBotUsername() + "\n" +
                    "§7(Le code expire dans 5 minutes)";
            
            Main.INSTANCE.getLogger().atWarning().log("Joueur " + playerName + " refusé: compte Discord non lié (code: " + linkCode + ")");
            DiscordLogHandler.logWarning("⛔ Connexion refusée: " + playerName + " - Code de liaison: " + linkCode);
            
            event.setCancelled(true);
            event.setReason(kickMessage);
        } else {
            Main.INSTANCE.getLogger().atInfo().log("Joueur " + playerName + " autorisé (Discord: " + discordId + ")");
            DiscordLogHandler.logPlayerJoin(playerName + " (Discord lié)");
        }
    }
}
