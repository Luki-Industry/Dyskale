package com.lukienlive.hytale.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.Main;
import com.lukienlive.hytale.hytale.HytaleConfig;
import com.lukienlive.hytale.application.service.LinkService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lukienlive.hytale.infrastructure.discord.ConsoleLogService;
import com.lukienlive.hytale.infrastructure.discord.StatusUpdateService;

@Singleton
public class DiscordBot extends ListenerAdapter {

    private JDA jda;

    @Inject
    private Logger logger;

    @Inject
    private LinkService linkService;

    @Inject
    private Config<HytaleConfig> config;

    @Inject
    private RoleSyncService roleSyncService;

    @Inject
    private ConsoleLogService consoleLogService;

    @Inject
    private StatusUpdateService statusUpdateService;

    public boolean start() {
        try {
            String token = config.get().getString("Discord_token");

            if (token == null || token.isEmpty()) {
                this.logger.log(Level.SEVERE, "Token Discord invalide ou manquant dans config.json");
                this.logger.log(Level.WARNING, "Veuillez configurer votre token Discord dans le fichier config.json");
                return false;
            }

            this.logger.log(Level.INFO, "Connexion au bot Discord...");

            jda = JDABuilder.createLight(token, 
                    GatewayIntent.GUILD_MESSAGES, 
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(this)
                .build();
            
            jda.awaitReady();

            this.logger.log(Level.INFO, "Bot Discord connecté avec succès! (" + getBotUsername() + ")");

            consoleLogService.setJda(jda);
            consoleLogService.start();

            statusUpdateService.setJda(jda);
            statusUpdateService.start();

            return true;
        } catch (InvalidTokenException e) {
            this.logger.log(Level.SEVERE, "Token Discord invalide ou manquant dans config.json",e);
            return false;
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, "Erreur lors de la connexion au bot Discord", e);
            return false;
        }
    }

    public JDA getJda() {
        return jda;
    }

    public Guild getGuild() {
        if (jda == null) return null;
        String guildId = config.get().getString("Guild_id");
        if (guildId == null || guildId.equals("VOTRE_GUILD_ID_HERE")) return null;
        return jda.getGuildById(guildId);
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

            linkService.consumeLinkCode(message)
                    .ifPresentOrElse(pending -> {
                        UUID playerUuid = UUID.fromString(pending.getPlayerUuid());
                        linkService.linkAccount(playerUuid, discordId);

                        // Sync roles immediately upon linking
                        roleSyncService.syncUser(playerUuid, discordId);

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

    public boolean isUserInGuild(String discordId) {
        return getMemberById(discordId) != null;
    }

    public String getBotUsername() {
        if (jda == null || jda.getSelfUser() == null) {
            return "Bot";
        }
        return jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator();
    }

    public boolean hasRequiredRole(String discordId, List<String> requiredRoleIds) {
        if (requiredRoleIds == null || requiredRoleIds.isEmpty()) {
            return true;
        }

        Member member = getMemberById(discordId);
        if (member == null) {
            return false;
        }

        return member.getRoles().stream()
                .anyMatch(role -> requiredRoleIds.contains(role.getId()));
    }

    public void sendConsoleLog(String message) {
        consoleLogService.offerLog(message);
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
            statusUpdateService.shutdown();
            consoleLogService.shutdown();

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

