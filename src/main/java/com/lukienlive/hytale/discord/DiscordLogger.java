package com.lukienlive.hytale.discord;

import com.google.inject.Inject;

import java.util.logging.Logger;

public class DiscordLogger {
    private final Logger logger;
    private final DiscordBot discordBot;

    private boolean installed = false;

    @Inject
    public DiscordLogger(Logger logger, DiscordBot discordBot) {
        this.logger = logger;
        this.discordBot = discordBot;
    }

    public void install() {
        if (installed) return;

        installed = true;
        logger.info("Installation du DiscordLogger...");
        info("🚀 Plugin DiscordLink actif - Logs disponibles ici");
    }

    public void uninstall() {
        if (!installed) return;

        info("🛑 Plugin DiscordLink arrêté");
        installed = false;
    }

    /* ===================== */
    /* Méthodes de log       */
    /* ===================== */

    public void info(String message) {
        send("ℹ️ " + message);
    }

    public void warning(String message) {
        send("⚠️ " + message);
    }

    public void error(String message) {
        send("🔴 " + message);
    }

    public void playerJoin(String playerName) {
        send("🟢 " + playerName + " a rejoint le serveur");
    }

    public void playerLeave(String playerName) {
        send("🔴 " + playerName + " a quitté le serveur");
    }

    public void send(String message) {
        if (!installed) return;
        discordBot.sendConsoleLog(message);
    }
}
