package com.lukienlive.hytale.infrastructure.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.Main;
import com.lukienlive.hytale.hytale.HytaleConfig;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class StatusUpdateService {

    @Setter
    private JDA jda;
    private ScheduledExecutorService statusUpdateExecutor;
    private String statusMessageId = null;

    @Inject
    private Config<HytaleConfig> config;

    @Inject
    private Logger logger;

    public void start() {
        statusMessageId = config.get().getString("Status_message_id");
        if (statusMessageId != null && statusMessageId.isEmpty()) {
            statusMessageId = null;
        }

        boolean botStatusEnabled = config.get().getBoolean("Enable_bot_status");
        boolean statusMessageEnabled = config.get().getBoolean("Enable_status_message");

        logger.info("[StatusService] Démarrage... BotStatus=" + botStatusEnabled + ", MessageStatus=" + statusMessageEnabled);

        if (!botStatusEnabled && !statusMessageEnabled) {
            logger.info("[StatusService] Services désactivés dans la config.");
            return;
        }

        if (statusMessageEnabled) {
            String channelId = config.get().getString("Status_channel_id");
            if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_STATUS_CHANNEL_ID_HERE")) {
                logger.log(Level.WARNING, "Status_channel_id non configuré - message de statut désactivé");
                statusMessageEnabled = false;
            } else {
                logger.info("[StatusService] Channel ID configuré: " + channelId);
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
        }, 5, 30, TimeUnit.SECONDS);
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
        if (channel == null) {
            logger.warning("[StatusService] Channel introuvable avec l'ID: " + channelId);
            return;
        }

        sendStatusToChannel(channel);
    }

    private void sendStatusToChannel(TextChannel channel) {
        try {
            var onlinePlayers = Universe.get().getPlayers();
            int playerCount = onlinePlayers.size();
            String playerList = onlinePlayers.isEmpty() ?
                    "Aucun joueur connecté" :
                    onlinePlayers.stream()
                            .map(p -> "• " + p.getUsername())
                            .collect(Collectors.joining("\n"));

            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            String uptime = String.format("%dh %02dm",
                TimeUnit.MILLISECONDS.toHours(uptimeMillis),
                TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60);

            long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
            long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            String ramUsage = String.format("%d MB / %d MB", usedMemory, maxMemory);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🟢 Serveur Hytale - EN LIGNE")
                    .setColor(Color.GREEN)
                    .addField("👥 Joueurs", String.valueOf(playerCount), true)
                    .addField("⏱️ Uptime", uptime, true)
                    //.addField("💾 RAM", ramUsage, true)
                    .addField("📝 Liste des joueurs", playerList.length() > 1024 ?
                            playerList.substring(0, 1021) + "..." : playerList, false)
                    .setFooter("Dernière mise à jour")
                    .setTimestamp(Instant.now());

            if (statusMessageId == null) {
                logger.info("[StatusService] Envoi d'un nouveau message de statut...");
                channel.sendMessageEmbeds(embed.build()).queue(msg -> {
                    statusMessageId = msg.getId();
                    saveStatusMessageId();
                }, error -> logger.log(Level.WARNING, "[StatusService] Erreur envoi message statut", error));
            } else {
                channel.retrieveMessageById(statusMessageId).queue(
                        msg -> msg.editMessageEmbeds(embed.build()).queue(
                                s -> {},
                                e -> logger.log(Level.WARNING, "[StatusService] Erreur lors de l'édition du message", e)
                        ),
                        error -> {
                            logger.warning("[StatusService] Message statut introuvable (" + error.getMessage() + "), création d'un nouveau...");
                            channel.sendMessageEmbeds(embed.build()).queue(msg -> {
                                statusMessageId = msg.getId();
                                saveStatusMessageId();
                            }, e -> logger.log(Level.WARNING, "[StatusService] Erreur envoi nouveau message statut", e));
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
