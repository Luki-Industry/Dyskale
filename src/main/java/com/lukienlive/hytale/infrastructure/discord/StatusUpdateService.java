package com.lukienlive.hytale.infrastructure.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.Main;
import com.lukienlive.hytale.hytale.HytaleConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class StatusUpdateService {

    private JDA jda;
    private ScheduledExecutorService statusUpdateExecutor;
    private String statusMessageId = null;

    @Inject
    private Config<HytaleConfig> config;

    @Inject
    private Logger logger;

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public void start() {
        statusMessageId = config.get().getString("Status_message_id");
        if (statusMessageId != null && statusMessageId.isEmpty()) {
            statusMessageId = null;
        }

        boolean botStatusEnabled = config.get().getBoolean("Enable_bot_status");
        boolean statusMessageEnabled = config.get().getBoolean("Enable_status_message");

        if (!botStatusEnabled && !statusMessageEnabled) {
            return;
        }

        if (statusMessageEnabled) {
            String channelId = config.get().getString("Status_channel_id");
            if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_STATUS_CHANNEL_ID_HERE")) {
                logger.log(Level.WARNING, "Status_channel_id non configuré - message de statut désactivé");
                statusMessageEnabled = false;
            }
        }

        final boolean finalStatusMessageEnabled = statusMessageEnabled;

        statusUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

        statusUpdateExecutor.scheduleAtFixedRate(() -> {
            try {
                if (botStatusEnabled) {
                    updateBotStatus();
                }
                if (finalStatusMessageEnabled) {
                    updateServerStatusMessage();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erreur lors de la mise à jour du statut", e);
            }
        }, 5, 300, TimeUnit.SECONDS);
    }

    public void shutdown() {
        try {
            updateServerStatusOffline();

            if (statusUpdateExecutor != null) {
                statusUpdateExecutor.shutdown();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'arrêt du service de statut", e);
        }
    }

    private void updateBotStatus() {
        if (jda == null) return;
        try {
            int playerCount = getOnlinePlayerCount();
            var activity = playerCount + " joueur" + (playerCount > 1 ? "s" : "");
            jda.getPresence().setActivity(Activity.watching(activity));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la mise à jour du statut du bot", e);
        }
    }

    private void updateServerStatusMessage() {
        String channelId = config.get().getString("Status_channel_id");
        if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_STATUS_CHANNEL_ID_HERE") || jda == null) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        sendStatusToChannel(channel);
    }

    private void sendStatusToChannel(TextChannel channel) {
        try {
            var onlinePlayers = Universe.get().getPlayers();
            int playerCount = onlinePlayers.size();
            String playerList = onlinePlayers.isEmpty() ?
                    "Aucun joueur connecté" :
                    onlinePlayers.stream()
                            .map(p -> "• " + p)
                            .collect(Collectors.joining("\n"));

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🟢 Serveur Hytale - EN LIGNE")
                    .setColor(Color.GREEN)
                    .addField("👥 Joueurs connectés", String.valueOf(playerCount), true)
                    .addField("⏱️ Statut", "Serveur actif", true)
                    .addField("📝 Liste des joueurs", playerList.length() > 1024 ?
                            playerList.substring(0, 1021) + "..." : playerList, false)
                    .setFooter("Dernière mise à jour")
                    .setTimestamp(Instant.now());

            if (statusMessageId == null) {
                channel.sendMessageEmbeds(embed.build()).queue(msg -> {
                    statusMessageId = msg.getId();
                    saveStatusMessageId();
                });
            } else {
                channel.retrieveMessageById(statusMessageId).queue(
                        msg -> msg.editMessageEmbeds(embed.build()).queue(),
                        error -> {
                            channel.sendMessageEmbeds(embed.build()).queue(msg -> {
                                statusMessageId = msg.getId();
                                saveStatusMessageId();
                            });
                        }
                );
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de la mise à jour du message de statut", e);
        }
    }

    private int getOnlinePlayerCount() {
        try {
            return Universe.get().getPlayerCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveStatusMessageId() {
        if (statusMessageId != null) {
            // Needed to access Main.INSTANCE for save? Or use config object?
            // The injected config object is a wrapper around HytaleConfig, but does it support 'set' and 'save'?
            // The injected config is `Config<HytaleConfig>`.
            // In DiscordBot it used `Main.INSTANCE.getConfig().get().set(...)` which requires map manipulation or codec support.

            // HytaleConfig stores values in a map.
            // But `Config` object from Hytale API. `config.get()` returns the POJO.
            // HytaleConfig has `values` map but it's private.
            // But HytaleConfig has put methods via codec updates? No.

            // The existing code was:
            // Main.INSTANCE.getConfig().get().set("Status_message_id", statusMessageId);
            // Main.INSTANCE.getConfig().save();

            // I should assume HytaleConfig exposes a setter or I can add one.
            // Checking HytaleConfig: it has `values` map.

            // Wait, HytaleConfig is a custom class. I'll check if I can add a setter to HytaleConfig or if it exists.

            // Main.INSTANCE.getConfig() is accessible.
            // But I should try to use dependency injection.
            // I have `config` injected.

            // I'll resort to Main.INSTANCE for now to keep it working as before, or add method to HytaleConfig.
            // I'll assume HytaleConfig needs a way to set values.
            // I'll update HytaleConfig later if needed. For now I'll use Main.INSTANCE.

            Main.INSTANCE.getConfig().get().set("Status_message_id", statusMessageId);
            Main.INSTANCE.getConfig().save();
        }
    }

    private void updateServerStatusOffline() {
         if (jda == null || statusMessageId == null) return;

         String channelId = config.get().getString("Status_channel_id");
         if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_STATUS_CHANNEL_ID_HERE")) return;

         try {
             TextChannel channel = jda.getTextChannelById(channelId);
             if (channel == null) return;

             EmbedBuilder embed = new EmbedBuilder()
                 .setTitle("🔴 Serveur Hytale - HORS LIGNE")
                 .setColor(Color.RED)
                 .addField("👥 Joueurs connectés", "0", true)
                 .addField("⏱️ Statut", "Serveur arrêté", true)
                 .addField("📋 Liste des joueurs", "Serveur hors ligne", false)
                 .setFooter("État du serveur")
                 .setTimestamp(Instant.now());

            try {
                Message msg = channel.retrieveMessageById(statusMessageId).submit().get(5, TimeUnit.SECONDS);
                msg.editMessageEmbeds(embed.build()).queue();
                // Thread.sleep(1000); // Avoid sleep in shutdown if possible
            } catch (Exception e) {
            }
         } catch (Exception e) {
             logger.log(Level.WARNING, "Erreur lors de la mise à jour du statut offline", e);
         }
    }
}

