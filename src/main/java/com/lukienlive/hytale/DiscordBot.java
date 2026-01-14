package com.lukienlive.hytale;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DiscordBot extends ListenerAdapter {

    private JDA jda;
    private final BlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();
    private Thread consoleSenderThread;

    public boolean start() {
        try {
            String token = Main.INSTANCE.getConfigString("discord_token");
            if (token == null || token.isEmpty() || token.equals("VOTRE_TOKEN_DISCORD_ICI")) {
                Main.INSTANCE.getLogger().atSevere().log("Token Discord invalide ou manquant dans config.json");
                Main.INSTANCE.getLogger().atWarning().log("Veuillez configurer votre token Discord dans le fichier config.json");
                return false;
            }
            
            Main.INSTANCE.getLogger().atInfo().log("Connexion au bot Discord...");
            
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
                
            Main.INSTANCE.getLogger().atInfo().log("Bot Discord connecté avec succès!");
            
            // Démarrer le thread d'envoi des logs vers le salon console
            startConsoleSender();
            
            return true;
        } catch (net.dv8tion.jda.api.exceptions.InvalidTokenException e) {
            Main.INSTANCE.getLogger().atSevere().log("Token Discord invalide! Vérifiez votre token dans config.json");
            return false;
        } catch (Exception e) {
            Main.INSTANCE.getLogger().atSevere().withCause(e).log("Erreur lors de la connexion au bot Discord");
            return false;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignorer les messages du bot lui-même
        if (event.getAuthor().isBot()) return;

        // Salon console : exécuter les commandes
        if (event.isFromGuild()) {
            String consoleChannelId = Main.INSTANCE.getConfigString("console_channel_id");
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
            var pending = Main.INSTANCE.storage.consumeLinkCode(message);
            
            if (pending != null) {
                // Code valide ! Lier le compte
                Main.INSTANCE.storage.link(pending.getPlayerUuid(), discordId);
                
                event.getChannel().sendMessage(
                    "✅ **Compte lié avec succès!**\n\n" +
                    "Votre compte Discord est maintenant lié à: `" + pending.getPlayerName() + "`\n" +
                    "Vous pouvez maintenant rejoindre le serveur Hytale!"
                ).queue();
                
                Main.INSTANCE.getLogger().atInfo().log("Compte lié: " + pending.getPlayerName() + " (" + pending.getPlayerUuid() + ") <-> " + event.getAuthor().getAsTag() + " (" + discordId + ")");
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
        
        String guildId = Main.INSTANCE.getConfigString("guild_id");
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
        String channelId = Main.INSTANCE.getConfigString("console_channel_id");
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
        Main.INSTANCE.getLogger().atInfo().log("Commande Discord reçue de " + event.getAuthor().getAsTag() + ": " + command);
        
        // Réagir pour indiquer que la commande est reçue
        event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⏳")).queue();
        
        try {
            // Créer un sender Discord personnalisé
            DiscordCommandSender sender = new DiscordCommandSender(event);
            
            // Exécuter la commande via CommandManager
            var commandManager = com.hypixel.hytale.server.core.command.system.CommandManager.get();
            commandManager.handleCommand(sender, command).whenComplete((result, error) -> {
                event.getMessage().removeReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⏳")).queue();
                
                if (error != null) {
                    event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❌")).queue();
                    event.getChannel().sendMessage("❌ Erreur: " + error.getMessage()).queue();
                    Main.INSTANCE.getLogger().atSevere().withCause(error).log("Erreur lors de l'exécution de la commande Discord");
                } else {
                    event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")).queue();
                }
            });
        } catch (Exception e) {
            event.getMessage().removeReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("⏳")).queue();
            event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❌")).queue();
            event.getChannel().sendMessage("❌ Erreur: " + e.getMessage()).queue();
            Main.INSTANCE.getLogger().atSevere().withCause(e).log("Erreur lors de l'exécution de la commande Discord");
        }
    }

    public void shutdown() {
        if (consoleSenderThread != null) {
            consoleSenderThread.interrupt();
        }
        if (jda != null) {
            jda.shutdown();
            Main.INSTANCE.getLogger().atInfo().log("Discord bot déconnecté.");
        }
    }
}
