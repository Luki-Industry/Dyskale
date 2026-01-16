package com.lukienlive.hytale.infrastructure.discord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.hytale.HytaleConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
public class ConsoleLogService {

    private final BlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();
    private Thread consoleSenderThread;
    private JDA jda;

    @Inject
    private Config<HytaleConfig> config;

    @Inject
    private Logger logger; // Java logger to log errors of this service

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public void start() {
        consoleSenderThread = new Thread(this::runLoop, "Discord-Console-Sender");
        consoleSenderThread.setDaemon(true);
        consoleSenderThread.start();
    }

    public void shutdown() {
        if (consoleSenderThread != null) {
            consoleSenderThread.interrupt();
        }
    }

    public void offerLog(String message) {
        consoleQueue.offer(message);
    }

    private void runLoop() {
        StringBuilder buffer = new StringBuilder();
        long lastSendTime = System.currentTimeMillis();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String message = consoleQueue.poll(500, TimeUnit.MILLISECONDS);

                if (message != null) {
                    if (buffer.length() + message.length() + 1 > 1850) {
                        sendToConsoleChannel(buffer.toString());
                        buffer.setLength(0);
                        lastSendTime = System.currentTimeMillis();
                    }
                    buffer.append(message).append("\n");
                }

                if (buffer.length() > 0 && System.currentTimeMillis() - lastSendTime > 2000) {
                    sendToConsoleChannel(buffer.toString());
                    buffer.setLength(0);
                    lastSendTime = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        // Send remaining buffer
        if (buffer.length() > 0) {
            sendToConsoleChannel(buffer.toString());
        }
    }

    private void sendToConsoleChannel(String content) {
        String channelId = config.get().getString("Console_channel_id");
        if (channelId == null || channelId.equals("VOTRE_CHANNEL_ID_ICI") || jda == null) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String finalContent = content.trim();
            if (finalContent.isEmpty()) return;

            if (finalContent.length() > 1950) {
                finalContent = finalContent.substring(finalContent.length() - 1950);
                int newlinePos = finalContent.indexOf('\n');
                if (newlinePos > 0 && newlinePos < 100) {
                    finalContent = finalContent.substring(newlinePos + 1);
                }
                finalContent = "... (logs tronqués)\n" + finalContent;
            }

            final String contentToSend = finalContent;
            channel.sendMessage("```ansi\n" + contentToSend + "```").queue(
                    success -> {},
                    error -> channel.sendMessage("```\n" + contentToSend + "```").queue()
            );
        }
    }
}

