package com.lukienlive.hytale.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.application.service.ConsoleLogCaptureService;
import com.lukienlive.hytale.domain.console.LogLevel;
import com.lukienlive.hytale.hytale.HytaleConfig;
import com.lukienlive.hytale.infrastructure.console.HytaleLoggerBridge;
import com.lukienlive.hytale.infrastructure.discord.DiscordConsoleLogHandler;
import com.lukienlive.hytale.infrastructure.discord.EventLogService;
import com.lukienlive.hytale.infrastructure.discord.PrivateMessageDetectorHandler;

import java.util.logging.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main orchestrator for Discord console logging.
 * This class ties together all the Clean Architecture layers:
 * - Domain (LogEntry, LogLevel, ConsoleLogHandler interface)
 * - Application (ConsoleLogCaptureService)
 * - Infrastructure (JavaLoggingBridge, DiscordConsoleLogHandler)
 */
@Singleton
public class DiscordLogger {
    private final Logger logger;
    private final DiscordBot discordBot;
    private final Config<HytaleConfig> config;
    private final EventLogService eventLogService;
    
    private ConsoleLogCaptureService captureService;
    private HytaleLoggerBridge hytaleLoggerBridge;
    private DiscordConsoleLogHandler discordHandler;
    private ScheduledExecutorService logProcessorExecutor;
    
    private boolean installed = false;

    @Inject
    public DiscordLogger(Logger logger, DiscordBot discordBot, Config<HytaleConfig> config, EventLogService eventLogService) {
        this.logger = logger;
        this.discordBot = discordBot;
        this.config = config;
        this.eventLogService = eventLogService;
    }

    /**
     * Install the console logging system.
     * This sets up the complete pipeline from Java logging to Discord.
     */
    public void install() {
        if (installed) {
            return;
        }

        try {
            // Initialize the application service
            captureService = new ConsoleLogCaptureService();
            
            // Configure minimum log level from config
            String configuredLevel = config.get().getString("Console_minimum_log_level");
            LogLevel minimumLevel = LogLevel.fromString(configuredLevel);
            captureService.setMinimumLogLevel(minimumLevel);

            // Create the Discord console handler
            discordHandler = new DiscordConsoleLogHandler(config, logger);
            discordHandler.setJda(discordBot.getJda());
            
            // Create the private message detector handler
            PrivateMessageDetectorHandler pmHandler = new PrivateMessageDetectorHandler(eventLogService);
            
            // Register handlers with the capture service
            captureService.registerHandler(discordHandler);
            captureService.registerHandler(pmHandler);

            // Create and install the Hytale logging bridge
            hytaleLoggerBridge = new HytaleLoggerBridge(captureService);
            hytaleLoggerBridge.install();

            // Start scheduled log processor (every 50ms)
            logProcessorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "console-log-processor");
                t.setDaemon(true);
                return t;
            });
            logProcessorExecutor.scheduleAtFixedRate(
                hytaleLoggerBridge::processLogs,
                50L, // Initial delay (ms)
                50L, // Period (ms) - every tick
                TimeUnit.MILLISECONDS
            );

            // Start the capture service
            captureService.start();

            installed = true;
            logger.info("✅ Système de capture de logs console installé");
            logger.info("   - Niveau minimum: " + minimumLevel.getName());
            logger.info("   - Handlers actifs: " + captureService.getActiveHandlerCount());
            
            // Send a test message to Discord
            info("🚀 Plugin Dyskale actif - Logs console maintenant disponibles sur Discord");
            
        } catch (Exception e) {
            logger.severe("❌ Erreur lors de l'installation du système de logging: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Uninstall the console logging system.
     * Cleans up all resources and removes handlers.
     */
    public void uninstall() {
        if (!installed) {
            return;
        }

        try {
            info("🛑 Arrêt du système de capture de logs console");
            
            // Stop log processor
            if (logProcessorExecutor != null) {
                logProcessorExecutor.shutdown();
                try {
                    if (!logProcessorExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                        logProcessorExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logProcessorExecutor.shutdownNow();
                }
            }
            
            // Stop the capture service
            if (captureService != null) {
                captureService.stop();
            }

            // Uninstall the Hytale logging bridge
            if (hytaleLoggerBridge != null) {
                hytaleLoggerBridge.uninstall();
            }

            installed = false;
            logger.info("✅ Système de capture de logs console désinstallé");
            
        } catch (Exception e) {
            logger.severe("❌ Erreur lors de la désinstallation du système de logging: " + e.getMessage());
        }
    }

    /* ===================== */
    /* Convenience Methods   */
    /* ===================== */

    /**
     * Send an info message directly to Discord (bypasses normal logging).
     */
    public void info(String message) {
        if (installed && discordHandler != null) {
            discordBot.sendConsoleLog("ℹ️ " + message);
        }
    }

    /**
     * Send a warning message to Events channel (for connection refusals, etc.).
     */
    public void warning(String message) {
        discordBot.sendEventLog("⚠️ " + message);
    }

    /**
     * Send an error message directly to Discord (bypasses normal logging).
     */
    public void error(String message) {
        if (installed && discordHandler != null) {
            discordBot.sendConsoleLog("🔴 " + message);
        }
    }

    /**
     * Log when a player joins the server (sent to Events channel).
     */
    public void playerJoin(String playerName) {
        discordBot.sendEventLog("🟢 **" + playerName + "** a rejoint le serveur");
    }

    /**
     * Log when a player leaves the server (sent to Events channel).
     */
    public void playerLeave(String playerName) {
        discordBot.sendEventLog("🔴 **" + playerName + "** a quitté le serveur");
    }

    /**
     * Log a chat message from a player (sent to Events channel).
     */
    public void playerChat(String playerName, String message) {
        discordBot.sendEventLog("💬 **" + playerName + "**: " + message);
    }

    /**
     * Log a player death message (sent to Events channel).
     */
    public void playerDeath(String message) {
        discordBot.sendEventLog("☠️ " + message);
    }

    /**
     * Update the JDA instance when Discord bot reconnects.
     * This is important if the bot needs to reconnect.
     */
    public void updateJda() {
        if (discordHandler != null) {
            discordHandler.setJda(discordBot.getJda());
        }
    }

    /**
     * Check if the logging system is currently installed and active.
     */
    public boolean isInstalled() {
        return installed;
    }

    /**
     * Get statistics about the current logging system.
     */
    public String getStats() {
        if (!installed || captureService == null) {
            return "Console logging is not active";
        }

        return String.format(
            "Console Logging Stats:\n" +
            "  - Status: %s\n" +
            "  - Minimum Level: %s\n" +
            "  - Active Handlers: %d\n" +
            "  - Queue Size: %d",
            captureService.isEnabled() ? "Active" : "Inactive",
            captureService.getMinimumLogLevel().getName(),
            captureService.getActiveHandlerCount(),
            discordHandler != null ? discordHandler.getQueueSize() : 0
        );
    }
}
