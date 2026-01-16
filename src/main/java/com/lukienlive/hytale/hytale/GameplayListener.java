package com.lukienlive.hytale.hytale;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.lukienlive.hytale.application.service.LinkService;
import com.lukienlive.hytale.discord.DiscordLogger;

@Singleton
public class GameplayListener implements IEventListener {

    @Inject
    private DiscordLogger discordLogger;

    @Inject
    private LinkService linkService;

    @Override
    public void Register(EventRegistry registry) {
        registry.register(PlayerConnectEvent.class, this::onPlayerConnect);
        registry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        registry.registerGlobal(PlayerChatEvent.class, this::onPlayerChat);
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        var player = event.getPlayerRef();
        String discordId = linkService.getDiscordId(player.getUuid());
        String msg = player.getUsername();
        if (discordId != null) {
            msg += " (Discord lié)";
        }
        discordLogger.playerJoin(msg);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        discordLogger.playerLeave(event.getPlayerRef().getUsername());
    }

    private void onPlayerChat(PlayerChatEvent event) {
        discordLogger.playerChat(event.getSender().getUsername(), event.getContent());
    }
}
