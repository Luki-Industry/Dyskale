package com.lukienlive.hytale.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.LinkedStorage;
import com.lukienlive.hytale.Main;
import com.lukienlive.hytale.hytale.HytaleConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class DiscordBot extends ListenerAdapter {

    private JDA jda;
    private final BlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();
    private Thread consoleSenderThread;

    @Inject
    private Logger logger;

    @Inject
    private LinkedStorage storage;

    @Inject
    private Config<HytaleConfig> config;

    public boolean start() {
        try {
            String token = config.get().getString("Discord_token");
            if (token == null || token.isEmpty()) {
                this.logger.log(Level.SEVERE, "Token Discord invalide ou manquant dans config.json");
                this.logger.log(Level.WARNING, "Veuillez configurer votre token Discord dans le fichier config.json");
                return false;
            }

            this.logger.log(Level.INFO, "Connexion au bot Discord...");
            
            // createLight désactive les caches inutilisés pour optimiser la mémoire
            // GUILD_MESSAGES pour recevoir les messages dans les serveurs
            // MESSAGE_CONTENT pour accéder au contenu des messages
            // DIRECT_MESSAGES pour recevoir les MPs
            jda = JDABuilder.createLight(token, 
                    GatewayIntent.GUILD_MESSAGES, 
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(this)
                .build();

            this.logger.log(Level.INFO, "Bot Discord connecté avec succès!");

            startConsoleSender();
            
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

        // Salon console : exécuter les commandes
        if (event.isFromGuild()) {
            String consoleChannelId = config.get().getString("Console_channel_id");
            if (consoleChannelId != null && !consoleChannelId.equals("VOTRE_CHANNEL_ID_ICI")) {
                if (event.getChannel().getId().equals(consoleChannelId)) {
                    String command = event.getMessage().getContentRaw();
                    executeServerCommand(command, event);
                    return;
                }
            }
        }
        
        // Messages privés : codes de liaison
        if (!event.isFromGuild()) {
            String message = event.getMessage().getContentRaw().trim().toUpperCase();
            String discordId = event.getAuthor().getId();
            
            // Vérifier si c'est un code de liaison valide

            var pending = storage.consumeLinkCode(message);
            if (pending != null) {
                storage.link(pending.getPlayerUuid(), discordId);
                
                event.getChannel().sendMessage(
                    "✅ **Compte lié avec succès!**\n\n" +
                    "Votre compte Discord est maintenant lié à: `" + pending.getPlayerName() + "`\n" +
                    "Vous pouvez maintenant rejoindre le serveur Hytale!"
                ).queue();

                this.logger.log(Level.INFO, "Compte Discord lié: " + pending.getPlayerName() + " (" + pending.getPlayerUuid() + ") <-> " + event.getAuthor().getAsTag() + " (" + discordId + ")");
            } else {
                // Code invalide ou expiré
                event.getChannel().sendMessage(
                    "❌ **Code invalide ou expiré**\n\n" +
                    "Le code doit être envoyé dans les 5 minutes après votre tentative de connexion.\n" +
                    "Reconnectez-vous au serveur pour obtenir un nouveau code."
                ).queue();
            }
        }
    }

    public String consumeToken(String token) {
        // Méthode obsolète, gardée pour compatibilité avec LinkCommand
        return null;
    }

    public Member getMemberById(String discordId) {
        if (jda == null) return null;
        
        String guildId = config.get().getString("Guild_id");
        if (guildId == null || guildId.equals("VOTRE_GUILD_ID_ICI")) {
            return null;
        }
        
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return null;
        
        try {
            return guild.retrieveMemberById(discordId).complete();
        } catch (Exception e) {
            return null;
        }
    }

    public String getBotUsername() {
        if (jda == null || jda.getSelfUser() == null) {
            return "Bot Discord";
        }
        return jda.getSelfUser().getAsTag();
    }

    // Envoi de logs vers Discord
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
                        // Ajouter le message au buffer
                        if (buffer.length() + message.length() + 1 > 1850) {
                            // Buffer plein, envoyer maintenant
                            sendToConsoleChannel(buffer.toString());
                            buffer.setLength(0);
                            lastSendTime = System.currentTimeMillis();
                        }
                        buffer.append(message).append("\n");
                    }
                    
                    // Envoyer si buffer non vide et >2 secondes depuis le dernier envoi
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
            if (consoleSenderThread != null) {
                consoleSenderThread.interrupt();
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
}
