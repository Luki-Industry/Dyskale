package com.lukienlive.hytale.hytale.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/**
 * Command to display Ouiheberg hosting promotional information.
 * Accessible to all players.
 */
public class OuihebergCommand extends CommandBase {

    public OuihebergCommand() {
        super("oh", "Affiche la promotion Ouiheberg");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false; // Accessible à tous, pas de permission requise
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        // En-tête
        context.sender().sendMessage(Message.raw("§6§l═══════════════════════════════"));
        context.sender().sendMessage(Message.raw("§e§l🎁 Promotion Ouiheberg 🎁"));
        context.sender().sendMessage(Message.raw(""));
        
        // Message principal
        context.sender().sendMessage(Message.raw("§a15% de réduction sur tout le site ! §c❤"));
        context.sender().sendMessage(Message.raw("§7§o(sauf noms de domaine)"));
        context.sender().sendMessage(Message.raw(""));
        
        // Lien
        context.sender().sendMessage(Message.raw("§b🔗 Lien: §e§nhttps://www.ouiheberg.com/panel/aff.php?aff=326"));
        
        // Code promo
        context.sender().sendMessage(Message.raw("§b🏷️ Code: §6§lLUKI15"));
        
        // Pied
        context.sender().sendMessage(Message.raw(""));
        context.sender().sendMessage(Message.raw("§6§l═══════════════════════════════"));
    }
}
