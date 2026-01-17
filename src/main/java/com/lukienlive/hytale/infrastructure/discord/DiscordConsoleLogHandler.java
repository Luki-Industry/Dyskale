package com.lukienlive.hytale.infrastructure.discord;

import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.domain.console.ConsoleLogHandler;
import com.lukienlive.hytale.domain.console.LogEntry;
import com.lukienlive.hytale.hytale.HytaleConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Infrastructure implementation that sends console logs to a Discord channel.
 * 
 * This handler batches log messages and sends them to Discord at regular intervals
 * to avoid rate limiting.
 */
public class DiscordConsoleLogHandler implements ConsoleLogHandler {
    
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final int MAX_DISCORD_MESSAGE_LENGTH = 1850;
    private static final long BATCH_INTERVAL_MS = 2000;

    private final BlockingQueue<String> messageQueue;
    private final Config<HytaleConfig> config;
    private final Logger logger;
    
    private JDA jda;
    private Thread senderThread;
    private volatile boolean running;

    public DiscordConsoleLogHandler(Config<HytaleConfig> config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.running = false;
    }

    /**
     * Set the JDA instance for sending messages.
     * This is called by DiscordBot once JDA is ready.
     * 
     * @param jda The JDA instance
     */
    public void setJda(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void handleLog(LogEntry entry) {
        if (!running || jda == null) {
            return;
        }

        String formattedMessage = formatLogEntry(entry);
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            messageQueue.offer(formattedMessage);
        }
    }

    /**
     * Format a log entry for Discord display.
     * 
     * @param entry The log entry to format
     * @return Formatted string ready for Discord
     */
    private String formatLogEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();

        // Timestamp
        sb.append("[").append(TIME_FORMAT.format(Date.from(entry.getTimestamp()))).append("] ");

        // Level icon and name
        sb.append(entry.getLevel().getIcon()).append(" ");
        sb.append("[").append(entry.getLevel().getName()).append("] ");

        // Logger name (simplified)
        if (entry.getLoggerName() != null && !entry.getLoggerName().isEmpty()) {
            String simplifiedLogger = simplifyLoggerName(entry.getLoggerName());
            sb.append("[").append(simplifiedLogger).append("] ");
        }

        // Message
        sb.append(entry.getMessage());

        // Exception if present
        if (entry.hasThrowable()) {
            sb.append("\n").append(formatThrowable(entry.getThrowable()));
        }

        return sb.toString();
    }

    /**
     * Simplify a logger name for more readable output.
     * 
     * @param loggerName The full logger name
     * @return Simplified logger name
     */
    private String simplifyLoggerName(String loggerName) {
        // Extract class name from package
        String[] parts = loggerName.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return loggerName;
    }

    /**
     * Format a throwable for Discord display.
     * 
     * @param throwable The throwable to format
     * @return Formatted stack trace (truncated if too long)
     */
    private String formatThrowable(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        
        String stackTrace = sw.toString();
        
        // Truncate if too long
        if (stackTrace.length() > 500) {
            stackTrace = stackTrace.substring(0, 500) + "\n... (truncated)";
        }
        
        return stackTrace;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        running = true;
        senderThread = new Thread(this::processingLoop, "Discord-Console-Log-Sender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (senderThread != null) {
            senderThread.interrupt();
            try {
                senderThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isActive() {
        return running && jda != null;
    }

    /**
     * Main processing loop that batches and sends messages to Discord.
     */
    private void processingLoop() {
        StringBuilder buffer = new StringBuilder();
        long lastSendTime = System.currentTimeMillis();

        while (running) {
            try {
                String message = messageQueue.poll(500, TimeUnit.MILLISECONDS);

                if (message != null) {
                    // Check if adding this message would exceed Discord's limit
                    if (buffer.length() + message.length() + 1 > MAX_DISCORD_MESSAGE_LENGTH) {
                        sendToDiscord(buffer.toString());
                        buffer.setLength(0);
                        lastSendTime = System.currentTimeMillis();
                    }
                    buffer.append(message).append("\n");
                }

                // Send buffer if it's been sitting for too long
                if (buffer.length() > 0 && 
                    System.currentTimeMillis() - lastSendTime > BATCH_INTERVAL_MS) {
                    sendToDiscord(buffer.toString());
                    buffer.setLength(0);
                    lastSendTime = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log error but continue processing
                logger.warning("Error in console log processing loop: " + e.getMessage());
            }
        }

        // Send any remaining messages
        if (buffer.length() > 0) {
            sendToDiscord(buffer.toString());
        }
    }

    /**
     * Send a message to the configured Discord console channel.
     * 
     * @param content The message content to send
     */
    private void sendToDiscord(String content) {
        if (jda == null) {
            return;
        }

        String channelId = config.get().getString("Console_channel_id");
        if (channelId == null || channelId.equals("YOUR_CHANNEL_ID_HERE") || 
            channelId.equals("VOTRE_CHANNEL_ID_ICI")) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            return;
        }

        String finalContent = content.trim();
        if (finalContent.isEmpty()) {
            return;
        }

        // Truncate if still too long
        if (finalContent.length() > 1950) {
            finalContent = finalContent.substring(finalContent.length() - 1950);
            int newlinePos = finalContent.indexOf('\n');
            if (newlinePos > 0 && newlinePos < 100) {
                finalContent = finalContent.substring(newlinePos + 1);
            }
            finalContent = "... (logs tronqués)\n" + finalContent;
        }

        final String messageToSend = finalContent;
        
        // Try to send with ANSI formatting, fallback to plain text
        channel.sendMessage("```ansi\n" + messageToSend + "```").queue(
                success -> {},
                error -> channel.sendMessage("```\n" + messageToSend + "```").queue()
        );
    }

    /**
     * Get the current size of the message queue.
     * Useful for monitoring backpressure.
     * 
     * @return Number of pending messages
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
}
