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

public class UnlinkCommand extends AbstractAsyncCommand {

    private final RequiredArg<String> playerArg;
    private final LinkService linkService;

    @Inject
    public UnlinkCommand(LinkService linkService) {
        super("unlink", "Dissocier un joueur de son compte Discord");
        this.linkService = linkService;

        playerArg = withRequiredArg("player", "Nom du joueur", ArgTypes.STRING);
    }

    @Override
    protected boolean canGeneratePermission() {
        return true;
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

        if (!linkService.isLinked(player.getUuid())) {
            context.sender().sendMessage(Message.raw("§cCe joueur n'est pas lié."));
            return CompletableFuture.completedFuture(null);
        }

        String discordId = linkService.getDiscordId(player.getUuid());
        linkService.unlinkAccount(player.getUuid());

        context.sender().sendMessage(Message.raw("§aLe joueur " + playerName + " a été dissocié du compte Discord (ID: " + discordId + ")."));
        return CompletableFuture.completedFuture(null);
    }
}

