package com.lukienlive.hytale.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Singleton
public class DiscordLogger extends Handler {
    private final Logger logger;
    private final DiscordBot discordBot;

    private boolean installed = false;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Inject
    public DiscordLogger(Logger logger, DiscordBot discordBot) {
        this.logger = logger;
        this.discordBot = discordBot;
    }

    public void install() {
        if (installed) return;

        // S'attacher au logger racine
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(this);

        installed = true;
        logger.info("Installation du DiscordLogger...");
        info("🚀 Plugin DiscordLink actif - Logs disponibles ici");
    }

    public void uninstall() {
        if (!installed) return;

        Logger rootLogger = Logger.getLogger("");
        rootLogger.removeHandler(this);

        info("🛑 Plugin DiscordLink arrêté");
        installed = false;
    }

    @Override
    public void publish(LogRecord record) {
        if (!installed || discordBot == null) return;

        // Éviter les boucles infinies (logs venant de nos propres services)
        if (record.getLoggerName() != null && (
                record.getLoggerName().contains("DiscordBot") ||
                record.getLoggerName().contains("jda") || // JDA logs
                record.getLoggerName().contains("ConsoleLogService")
        )) {
            return;
        }

        try {
            StringBuilder message = new StringBuilder();

            // Format timestamp
            message.append("[").append(dateFormat.format(new Date(record.getMillis()))).append("] ");

            // Level icon
            String level = record.getLevel().getName();
            if (level.equals("SEVERE")) message.append("🔴 ");
            else if (level.equals("WARNING")) message.append("⚠️ ");
            else if (level.equals("INFO")) message.append("ℹ️ ");

            message.append("[").append(level).append("] ");

            // Message
            message.append(record.getMessage());

            // Exception trace if any
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                message.append("\n").append(sw.toString());
            }

            discordBot.sendConsoleLog(message.toString());
        } catch (Exception e) {
            // Ne pas logger l'erreur ici pour éviter une boucle infinie
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        uninstall();
    }

    /* ===================== */
    /* Méthodes de log       */
    /* ===================== */

    public void info(String message) {
        if (installed) discordBot.sendConsoleLog("ℹ️ " + message);
    }

    public void warning(String message) {
        if (installed) discordBot.sendConsoleLog("⚠️ " + message);
    }

    public void error(String message) {
        if (installed) discordBot.sendConsoleLog("🔴 " + message);
    }

    public void playerJoin(String playerName) {
        if (installed) discordBot.sendConsoleLog("🟢 **" + playerName + "** a rejoint le serveur");
    }

    public void playerLeave(String playerName) {
        if (installed) discordBot.sendConsoleLog("🔴 **" + playerName + "** a quitté le serveur");
    }

    public void playerChat(String playerName, String message) {
        if(installed) discordBot.sendConsoleLog("💬 **" + playerName + "**: " + message);
    }

    public void playerDeath(String message) {
        if(installed) discordBot.sendConsoleLog("☠️ " + message);
    }

    // Le send direct n'est plus exposé publiquement pour encourager l'usage des méthodes typées ou du Handler global
    void send(String message) {
        if (!installed) return;
        discordBot.sendConsoleLog(message);
    }
}
