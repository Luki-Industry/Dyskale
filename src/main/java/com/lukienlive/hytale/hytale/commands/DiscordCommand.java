package com.lukienlive.hytale.hytale.commands;

import com.google.inject.Inject;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.application.service.LinkService;
import com.lukienlive.hytale.discord.RoleSyncService;
import com.lukienlive.hytale.hytale.HytaleConfig;

public class DiscordCommand extends AbstractCommandCollection {

    @Inject
    public DiscordCommand(
            LinkService linkService,
            RoleSyncService roleSyncService,
            Config<HytaleConfig> config
    ) {
        super("discord", "Commandes d'administration Discord");

        addSubCommand(new SyncCommand(linkService, roleSyncService));
        addSubCommand(new UnlinkCommand(linkService));
        addSubCommand(new StatusCommand(linkService));
        addSubCommand(new ReloadCommand(config));
    }

    @Override
    protected boolean canGeneratePermission() {
        return true;
    }
}

