package com.lukienlive.hytale.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.UUID;

@Singleton
public class DiscordCommandSender implements CommandSender {

    @Inject
    private DiscordLogger logger;
    
    private final MessageReceivedEvent event;
    private static final UUID DISCORD_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    
    public DiscordCommandSender(MessageReceivedEvent event) {
        this.event = event;
    }
    
    @Override
    public void sendMessage(Message message) {
        // Envoyer le message vers Discord
        String content = message.toString();
        if (content != null && !content.isEmpty()) {
            event.getChannel().sendMessage(content).queue();
            // Aussi l'envoyer dans les logs
            logger.info("📤 " + content);
        }
    }
    
    @Override
    public String getDisplayName() {
        return "Discord:" + event.getAuthor().getName();
    }
    
    @Override
    public UUID getUuid() {
        return DISCORD_UUID;
    }
    
    @Override
    public boolean hasPermission(String permission) {
        // Les commandes Discord ont toutes les permissions
        return true;
    }
    
    @Override
    public boolean hasPermission(String permission, boolean defaultValue) {
        return true;
    }
}
