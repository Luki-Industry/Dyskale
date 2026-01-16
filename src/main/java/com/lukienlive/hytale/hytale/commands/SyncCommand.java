package com.lukienlive.hytale.hytale.commands;

import com.google.inject.Inject;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.Universe;
import com.lukienlive.hytale.application.service.LinkService;
import com.lukienlive.hytale.discord.RoleSyncService;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class SyncCommand extends AbstractAsyncCommand {

    private final RequiredArg<String> playerArg;
    private final LinkService linkService;
    private final RoleSyncService roleSyncService;

    @Inject
    public SyncCommand(LinkService linkService, RoleSyncService roleSyncService) {
        super("sync", "Force la synchronisation d'un joueur");
        this.linkService = linkService;
        this.roleSyncService = roleSyncService;

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
            context.sender().sendMessage(Message.raw("§cCe joueur n'a pas de compte Discord lié."));
            return CompletableFuture.completedFuture(null);
        }

        String discordId = linkService.getDiscordId(player.getUuid());
        context.sender().sendMessage(Message.raw("§eSynchronisation en cours pour " + playerName + "..."));

        roleSyncService.syncUser(player.getUuid(), discordId);

        context.sender().sendMessage(Message.raw("§aSynchronisation terminée pour " + playerName));
        return CompletableFuture.completedFuture(null);
    }
}

