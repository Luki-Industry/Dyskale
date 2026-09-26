package com.lukienlive.hytale.infrastructure.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.hytale.HytaleConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Service responsible for sending gameplay events to Discord.
 * Handles player joins, leaves, chat messages, private messages, and connection refusals.
 * 
 * <p><b>Infrastructure Layer:</b> Implements Discord-specific batching and formatting.</p>
 * 
 * <p><b>Separation of concerns:</b></p>
 * <ul>
 *     <li>Console logs → ConsoleLogService (via DiscordConsoleLogHandler)</li>
 *     <li>Gameplay events → EventLogService (this class)</li>
 * </ul>
 */
@Singleton
public class EventLogService {
    private static final int MAX_MESSAGE_LENGTH = 1900;
    private static final long BATCH_DELAY_MS = 1000; // 1 second batching
    
    private final Logger logger;
    private final Config<HytaleConfig> config;
    private final BlockingQueue<String> eventQueue;
    private final ScheduledExecutorService scheduler;
    
    private JDA jda;
    private volatile boolean running = false;

    @Inject
    public EventLogService(@Named("Dyskale") Logger logger, Config<HytaleConfig> config) {
        this.logger = logger;
        this.config = config;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "event-log-sender");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Set the JDA instance for sending messages.
     */
    public void setJda(JDA jda) {
        this.jda = jda;
    }

    /**
     * Start the event log sender service.
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        scheduler.scheduleAtFixedRate(
            this::processBatch,
            BATCH_DELAY_MS,
            BATCH_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
        
        logger.info("✅ Service d'événements Discord démarré");
    }

    /**
     * Add an event to the queue for sending to Discord.
     */
    public void sendEvent(String message) {
        if (!running) {
            return;
        }
        
        if (!eventQueue.offer(message)) {
            logger.warning("⚠️ File d'événements Discord pleine, message ignoré: " + message);
        }
    }

    /**
     * Process and send batched events.
     */
    private void processBatch() {
        if (jda == null || eventQueue.isEmpty()) {
            return;
        }

        String channelId = config.get().getString("Events_channel_id");
        if (channelId == null || channelId.equals("YOUR_EVENTS_CHANNEL_ID_HERE")) {
            return; // Channel not configured
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            logger.warning("⚠️ Salon d'événements Discord introuvable: " + channelId);
            eventQueue.clear(); // Clear queue to prevent infinite accumulation
            return;
        }

        StringBuilder batch = new StringBuilder();
        int processedCount = 0;

        while (!eventQueue.isEmpty() && batch.length() < MAX_MESSAGE_LENGTH) {
            String event = eventQueue.poll();
            if (event == null) {
                break;
            }

            // Check if adding this event would exceed the limit
            if (batch.length() + event.length() + 1 > MAX_MESSAGE_LENGTH) {
                // Send current batch and start a new one
                sendToDiscord(channel, batch.toString());
                batch = new StringBuilder();
            }

            if (batch.length() > 0) {
                batch.append("\n");
            }
            batch.append(event);
            processedCount++;
        }

        // Send remaining batch
        if (batch.length() > 0) {
            sendToDiscord(channel, batch.toString());
        }
    }

    /**
     * Send a message to Discord.
     */
    private void sendToDiscord(TextChannel channel, String message) {
        try {
            channel.sendMessage(message).queue(
                success -> {},
                error -> logger.warning("❌ Erreur lors de l'envoi d'événement Discord: " + error.getMessage())
            );
        } catch (Exception e) {
            logger.warning("❌ Erreur lors de l'envoi d'événement Discord: " + e.getMessage());
        }
    }

    /**
     * Stop the event log sender service.
     */
    public void shutdown() {
        running = false;
        
        // Process remaining events
        processBatch();
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        logger.info("✅ Service d'événements Discord arrêté");
    }

    /**
     * Check if the service is running.
     */
    public boolean isRunning() {
        return running;
    }
}
