package com.lukienlive.hytale.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.LinkedStorage;
import com.lukienlive.hytale.Main;
import com.lukienlive.hytale.hytale.HytaleConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class DiscordBot extends ListenerAdapter {

    private JDA jda;
    private final BlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();
    private Thread consoleSenderThread;
    private ScheduledExecutorService statusUpdateExecutor;
    private String statusMessageId = null;

    @Inject
    private Logger logger;

    @Inject
    private LinkedStorage storage;

    @Inject
    private Config<HytaleConfig> config;

    public boolean start() {
        try {
            String token = config.get().getString("Discord_token");
            statusMessageId = Main .INSTANCE.getConfig().get().getString("Status_message_id");
            if (statusMessageId != null && statusMessageId.isEmpty()) {
                statusMessageId = null;
            }

            if (token == null || token.isEmpty()) {
                this.logger.log(Level.SEVERE, "Token Discord invalide ou manquant dans config.json");
                this.logger.log(Level.WARNING, "Veuillez configurer votre token Discord dans le fichier config.json");
                return false;
            }

            this.logger.log(Level.INFO, "Connexion au bot Discord...");

            jda = JDABuilder.createLight(token, 
                    GatewayIntent.GUILD_MESSAGES, 
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(this)
                .build();

            this.logger.log(Level.INFO, "Bot Discord connecté avec succès!");

            startConsoleSender();
            startStatusUpdater();

            return true;
        } catch (InvalidTokenException e) {
            this.logger.log(Level.SEVERE, "Token Discord invalide ou manquant dans config.json",e);
            return false;
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, "Erreur lors de la connexion au bot Discord", e);
            return false;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        if (event.isFromGuild()) {
            String consoleChannelId = Main.INSTANCE.getConfig().get().getString("Console_channel_id");
            if (consoleChannelId != null && !consoleChannelId.equals("VOTRE_CHANNEL_ID_ICI")) {
                if (event.getChannel().getId().equals(consoleChannelId)) {
                    String command = event.getMessage().getContentRaw();
                    executeServerCommand(command, event);
                    return;
                }
            }
        }

        if (!event.isFromGuild()) {
            String message = event.getMessage().getContentRaw().trim().toUpperCase();
            String discordId = event.getAuthor().getId();
            
            // Vérifier si c'est un code de liaison valide

            storage.consumeLinkCode(message)
                    .ifPresentOrElse(pending -> {
                        storage.link(pending.getPlayerUuid(), discordId);

                        event.getChannel().sendMessage(
                                "✅ **Compte lié avec succès!**\n\n" +
                                        "Votre compte Discord est maintenant lié à: `" + pending.getPlayerName() + "`\n" +
                                        "Vous pouvez maintenant rejoindre le serveur Hytale!"
                        ).queue();

                        this.logger.log(Level.INFO, "Compte Discord lié: " + pending.getPlayerName() + " (" + pending.getPlayerUuid() + ") <-> " + event.getAuthor().getAsTag() + " (" + discordId + ")");
                    }, () -> {
                        event.getChannel().sendMessage(
                                "❌ **Code invalide ou expiré**\n\n" +
                                        "Le code doit être envoyé dans les 5 minutes après votre tentative de connexion.\n" +
                                        "Reconnectez-vous au serveur pour obtenir un nouveau code."
                        ).queue();
                    });
        }
    }

    public Member getMemberById(String discordId) {
        try {
            if (jda == null) return null;

            String guildId = config.get().getString("Guild_id");
            if (guildId == null || guildId.equals("VOTRE_GUILD_ID_ICI")) {
                return null;
            }

            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return null;

            return guild.retrieveMemberById(discordId).complete();
        } catch (Exception e) {
            return null;
        }
    }

    public void sendConsoleLog(String message) {
        consoleQueue.offer(message);
    }

    private void startConsoleSender() {
        consoleSenderThread = new Thread(() -> {
            StringBuilder buffer = new StringBuilder();
            long lastSendTime = System.currentTimeMillis();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String message = consoleQueue.poll(500, TimeUnit.MILLISECONDS);
                    
                    if (message != null) {
                        if (buffer.length() + message.length() + 1 > 1850) {
                            sendToConsoleChannel(buffer.toString());
                            buffer.setLength(0);
                            lastSendTime = System.currentTimeMillis();
                        }
                        buffer.append(message).append("\n");
                    }

                    if (buffer.length() > 0 && System.currentTimeMillis() - lastSendTime > 2000) {
                        sendToConsoleChannel(buffer.toString());
                        buffer.setLength(0);
                        lastSendTime = System.currentTimeMillis();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            // Envoyer le buffer restant
            if (buffer.length() > 0) {
                sendToConsoleChannel(buffer.toString());
            }
        }, "Discord-Console-Sender");
        consoleSenderThread.setDaemon(true);
        consoleSenderThread.start();
    }

    private void sendToConsoleChannel(String content) {
        String channelId = config.get().getString("Console_channel_id");
        if (channelId == null || channelId.equals("VOTRE_CHANNEL_ID_ICI") || jda == null) {
            return;
        }
        
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            // Nettoyer le contenu
            String finalContent = content.trim();
            if (finalContent.isEmpty()) return;
            
            // Limiter la taille
            if (finalContent.length() > 1950) {
                finalContent = finalContent.substring(finalContent.length() - 1950);
                // Trouver le premier saut de ligne pour ne pas couper au milieu d'une ligne
                int newlinePos = finalContent.indexOf('\n');
                if (newlinePos > 0 && newlinePos < 100) {
                    finalContent = finalContent.substring(newlinePos + 1);
                }
                finalContent = "... (logs tronqués)\n" + finalContent;
            }
            
            // Envoyer en bloc de code
            final String contentToSend = finalContent;
            channel.sendMessage("```ansi\n" + contentToSend + "```").queue(
                success -> {}, // Succès : rien à faire
                error -> {
                    // En cas d'erreur, essayer sans formatage ANSI
                    channel.sendMessage("```\n" + contentToSend + "```").queue();
                }
            );
        }
    }

    private void executeServerCommand(String command, MessageReceivedEvent event) {
        // Log de la commande
        this.logger.log(Level.INFO, "Commande Discord reçue de " + event.getAuthor().getAsTag() + ": " + command);
        
        // Réagir pour indiquer que la commande est reçue
        event.getMessage().addReaction(Emoji.fromUnicode("⏳")).queue();
        
        try {
            // Créer un sender Discord personnalisé
            DiscordCommandSender sender = new DiscordCommandSender(event);
            
            // Exécuter la commande via CommandManager
            var commandManager = com.hypixel.hytale.server.core.command.system.CommandManager.get();
            commandManager.handleCommand(sender, command).whenComplete((result, error) -> {
                event.getMessage().removeReaction(Emoji.fromUnicode("⏳")).queue();
                
                if (error != null) {
                    event.getMessage().addReaction(Emoji.fromUnicode("❌")).queue();
                    event.getChannel().sendMessage("❌ Erreur: " + error.getMessage()).queue();

                    this.logger.log(Level.SEVERE, "Erreur lors de l'exécution de la commande Discord", error);
                } else {
                    event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue();
                }
            });
        } catch (Exception e) {
            event.getMessage().removeReaction(Emoji.fromUnicode("⏳")).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("❌")).queue();
            event.getChannel().sendMessage("❌ Erreur: " + e.getMessage()).queue();
            this.logger.log(Level.SEVERE, "Erreur lors de l'exécution de la commande Discord", e);
        }
    }

    public void shutdown() {
        try {
            // Mettre à jour le message de statut en rouge avant de déconnecter
            updateServerStatusOffline();

            if (consoleSenderThread != null) {
                consoleSenderThread.interrupt();
            }

            if (statusUpdateExecutor != null) {
                statusUpdateExecutor.shutdown();
            }

            jda.shutdown();
            if (!jda.awaitShutdown(Duration.ofSeconds(30))) {
                jda.shutdownNow();
            }

            this.logger.log(Level.INFO, "Bot Discord déconnecté avec succès.");
        }
        catch (InterruptedException e) {
            this.logger.log(Level.WARNING, "La fermeture du bot Discord a été interrompue", e);
        }
    }

    private void startStatusUpdater() {
        boolean botStatusEnabled = Main.INSTANCE.getConfig().get().getBoolean("Enable_bot_status");
        boolean statusMessageEnabled = Main.INSTANCE.getConfig().get().getBoolean("Enable_status_message");

        if (!botStatusEnabled && !statusMessageEnabled) {
            this.logger.log(Level.INFO, "Statut bot et message de statut désactivés");
            return;
        }

        // Vérifier le salon seulement si le message de statut est activé
        if (statusMessageEnabled) {
            String channelId = Main.INSTANCE.getConfig().get().getString("Status_channel_id");
            if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_STATUS_CHANNEL_ID_HERE")) {
                this.logger.log(Level.WARNING, "Status_channel_id non configuré - message de statut désactivé");
                statusMessageEnabled = false;
            }
        }

        final boolean finalStatusMessageEnabled = statusMessageEnabled;

        statusUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

        // Mise à jour immédiate puis toutes les 5 minutes
        statusUpdateExecutor.scheduleAtFixedRate(() -> {
            try {
                if (botStatusEnabled) {
                    updateBotStatus();
                }
                if (finalStatusMessageEnabled) {
                    updateServerStatusMessage();
                }
            } catch (Exception e) {
                this.logger.log(Level.WARNING, "Erreur lors de la mise à jour du statut", e);
            }
        }, 5, 300, TimeUnit.SECONDS); // 5 secondes de délai initial, puis toutes les 5 minutes (300s)
    }

    private void updateBotStatus() {
        try {
            int playerCount = getOnlinePlayerCount();
            var activity = playerCount + " joueur" + (playerCount > 1 ? "s" : "");

            jda.getPresence().setActivity(Activity.watching(activity));
        } catch (Exception e) {
            this.logger.log(Level.WARNING, "Erreur lors de la mise à jour du statut du bot", e);
        }
    }

    private void updateServerStatusMessage() {
        String channelId = Main.INSTANCE.getConfig().get().getString("Status_channel_id");
        if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_STATUS_CHANNEL_ID_HERE") || jda == null) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        sendStatusToChannel(channel);
    }

    private void sendStatusToChannel(TextChannel channel) {
        if (channel == null) {
            return;
        }

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
            this.logger.log(Level.WARNING, "Erreur lors de la mise à jour du message de statut", e);
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
        if (jda == null || statusMessageId == null) {
            return;
        }

        String channelId = Main.INSTANCE.getConfig().get().getString("Status_channel_id");
        if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_STATUS_CHANNEL_ID_HERE")) {
            return;
        }

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔴 Serveur Hytale - HORS LIGNE")
                .setColor(Color.RED)
                .addField("👥 Joueurs connectés", "0", true)
                .addField("⏱️ Statut", "Serveur arrêté", true)
                .addField("📋 Liste des joueurs", "Serveur hors ligne", false)
                .setFooter("État du serveur")
                .setTimestamp(Instant.now());

            // Utiliser submit().get() pour attendre de manière synchrone (avec timeout)
            try {
                Message msg = channel.retrieveMessageById(statusMessageId).submit().get(5, TimeUnit.SECONDS);
                msg.editMessageEmbeds(embed.build()).queue();
                Thread.sleep(1000);
            } catch (Exception e) {
                // Message introuvable ou timeout, ignorer
            }
        } catch (Exception e) {
            this.logger.log(Level.WARNING, "Erreur lors de la mise à jour du statut offline", e);
        }
    }
}
