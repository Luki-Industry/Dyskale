package com.lukienlive.hytale.hytale.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/**
 * Command to display Ouiheberg hosting promotional information.
 * Accessible to all players without permission requirements.
 */
public class OuihebergCommand extends CommandBase {

    public OuihebergCommand() {
        super("oh", "Affiche la promotion Ouiheberg");
                requireNoPermission();
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        // Composition du message complet
        Message message = Message.raw("Promotion OuiHeberg !")
                .color("#FFD700")
                .bold(true);
        
        message.insert(Message.raw("\n15% de réduction sur tout le site")
                .color("#00FF00"));
        
        message.insert(Message.raw("\nAvec ce lien: ")
                .color("#00FFFF"));
        
        message.insert(Message.raw("https://www.ouiheberg.com/panel/aff.php?aff=326")
                .color("#FFFF00")
                .link("https://www.ouiheberg.com/panel/aff.php?aff=326"));
        
        message.insert(Message.raw("\nEt avec le code: ")
                .color("#00FFFF"));
        
        message.insert(Message.raw("LUKI15")
                .color("#FFA500")
                .bold(true));
        
        context.sender().sendMessage(message);
    }
}

