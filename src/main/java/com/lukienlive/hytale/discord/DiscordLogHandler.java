package com.lukienlive.hytale.discord;

import com.lukienlive.hytale.Main;

import java.io.OutputStream;
import java.io.PrintStream;

public class DiscordLogHandler {
    
    private static boolean installed = false;
    private static PrintStream originalOut;
    private static PrintStream originalErr;
    
    public static void install() {
        if (installed) return;
        
        try {
            // Rediriger System.out et System.err UNIQUEMENT
            originalOut = System.out;
            originalErr = System.err;
            
            System.setOut(new PrintStream(new DiscordOutputStream(originalOut, false), true));
            System.setErr(new PrintStream(new DiscordOutputStream(originalErr, true), true));
            
            installed = true;
            
            // Envoyer un message de démarrage
            sendLog("🚀 Plugin DiscordLink actif - Logs du serveur disponibles ici");
        } catch (Exception e) {
            // Ignore
        }
    }
    
    public static void uninstall() {
        if (!installed) return;
        
        sendLog("🛑 Plugin DiscordLink arrêté");
        
        try {
            if (originalOut != null) {
                System.setOut(originalOut);
            }
            if (originalErr != null) {
                System.setErr(originalErr);
            }
        } catch (Exception e) {
            // Ignore
        }
        
        installed = false;
    }
    
    // Classe pour intercepter System.out et System.err
    private static class DiscordOutputStream extends OutputStream {
        private final PrintStream original;
        private final boolean isError;
        private final StringBuilder buffer = new StringBuilder();
        
        public DiscordOutputStream(PrintStream original, boolean isError) {
            this.original = original;
            this.isError = isError;
        }
        
        @Override
        public void write(int b) {
            // Toujours écrire dans le stream original
            try {
                original.write(b);
            } catch (Exception e) {
                // Ignore
            }
            
            if (!installed) return;
            
            try {
                char c = (char) b;
                if (c == '\n') {
                    String line = buffer.toString().trim();
                    buffer.setLength(0);
                    
                    if (!line.isEmpty() && shouldSend(line)) {
                        String icon = isError ? "🔴" : "ℹ️";
                        sendToDiscord(icon + " " + line);
                    }
                } else if (c != '\r') {
                    buffer.append(c);
                }
            } catch (Throwable t) {
                // Ignore
            }
        }
        
        @Override
        public void flush() {
            try {
                original.flush();
            } catch (Exception ignored) {}
        }
        
        private boolean shouldSend(String line) {
            return !line.isEmpty() && line.length() <= 2000 
                && !line.contains("DiscordLogHandler") 
                && !line.contains("sendConsoleLog");
        }
        
        private void sendToDiscord(String message) {
            try {
                if (Main.INSTANCE != null && Main.INSTANCE.getDiscordBot() != null) {
                    Main.INSTANCE.getDiscordBot().sendConsoleLog(message);
                }
            } catch (Throwable t) {
                // Ignore
            }
        }
    }
    
    // Méthodes statiques pour compatibilité
    public static void sendLog(String message) {
        if (!installed || Main.INSTANCE == null || Main.INSTANCE.getDiscordBot() == null) {
            return;
        }
        Main.INSTANCE.getDiscordBot().sendConsoleLog(message);
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
