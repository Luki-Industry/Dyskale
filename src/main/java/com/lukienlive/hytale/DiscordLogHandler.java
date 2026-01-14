package com.lukienlive.hytale;

public class DiscordLogHandler {
    
    private static boolean installed = false;
    
    public static void install() {
        if (installed) return;
        
        try {
            installed = true;
            Main.INSTANCE.getLogger().atInfo().log("DiscordLogHandler installé");
            
            // Envoyer un message de démarrage
            sendLog("🚀 Plugin DiscordLink actif - Logs disponibles ici");
        } catch (Exception e) {
            Main.INSTANCE.getLogger().atSevere().withCause(e).log("Erreur lors de l'installation du DiscordLogHandler");
        }
    }
    
    public static void uninstall() {
        if (!installed) return;
        
        sendLog("🛑 Plugin DiscordLink arrêté");
        installed = false;
    }
    
    public static void sendLog(String message) {
        if (!installed || Main.INSTANCE == null || Main.INSTANCE.discordBot == null) {
            return;
        }
        Main.INSTANCE.discordBot.sendConsoleLog(message);
    }
    
    public static void logInfo(String message) {
        sendLog("ℹ️ " + message);
    }
    
    public static void logWarning(String message) {
        sendLog("⚠️ " + message);
    }
    
    public static void logError(String message) {
        sendLog("🔴 " + message);
    }
    
    public static void logPlayerJoin(String playerName) {
        sendLog("🟢 " + playerName + " a rejoint le serveur");
    }
    
    public static void logPlayerLeave(String playerName) {
        sendLog("🔴 " + playerName + " a quitté le serveur");
    }
}
