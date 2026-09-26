package com.lukienlive.hytale.hytale.commands;

import com.google.inject.Inject;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.Universe;
import com.lukienlive.hytale.application.service.LinkService;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class StatusCommand extends AbstractAsyncCommand {

    private final RequiredArg<String> playerArg;
    private final LinkService linkService;

    @Inject
    public StatusCommand(LinkService linkService) {
        super("status", "Voir le statut de liaison d'un joueur");
        this.linkService = linkService;

        playerArg = withRequiredArg("player", "Nom du joueur", ArgTypes.STRING);
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        String playerName = playerArg.get(context);

        var player = Universe.get().getPlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if (player == null) {
            context.sender().sendMessage(Message.raw("§cJoueur introuvable ou hors ligne."));
            return CompletableFuture.completedFuture(null);
        }

        String discordId = linkService.getDiscordId(player.getUuid());

        if (discordId != null) {
            context.sender().sendMessage(Message.raw("§a" + playerName + " est lié au compte Discord ID: " + discordId));
        } else {
            context.sender().sendMessage(Message.raw("§c" + playerName + " n'est pas lié à un compte Discord."));
        }
        return CompletableFuture.completedFuture(null);
    }
}

