package com.lukienlive.hytale.hytale.commands;

import com.google.inject.Inject;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.hytale.HytaleConfig;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ReloadCommand extends AbstractAsyncCommand {

    private final Config<HytaleConfig> config;

    @Inject
    public ReloadCommand(Config<HytaleConfig> config) {
        super("reload", "Recharger la configuration");
        this.config = config;
    }

    @Override
    protected boolean canGeneratePermission() {
        return true;
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        context.sender().sendMessage(Message.raw("§eRechargement de la configuration..."));

        try {
            config.load().join();
            context.sender().sendMessage(Message.raw("§aConfiguration rechargée avec succès !"));
        } catch (Exception e) {
            context.sender().sendMessage(Message.raw("§cErreur lors du rechargement: " + e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }
}

