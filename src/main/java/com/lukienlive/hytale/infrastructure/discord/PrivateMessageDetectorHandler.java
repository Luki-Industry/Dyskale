package com.lukienlive.hytale.infrastructure.discord;

import com.lukienlive.hytale.domain.console.ConsoleLogHandler;
import com.lukienlive.hytale.domain.console.LogEntry;
import com.lukienlive.hytale.domain.console.LogLevel;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects private messages (/msg and /reply commands) from console logs
 * and forwards them to Discord Events channel.
 * 
 * <p><b>Infrastructure Layer:</b> Bridges console log parsing to Discord events.</p>
 * 
 * <p>Patterns detected:</p>
 * <ul>
 *     <li>Logger: CommandManager, Message: {@code PlayerName executed command: msg Target message}</li>
 *     <li>Logger: CommandManager, Message: {@code PlayerName executed command: reply message}</li>
 * </ul>
 */
public class PrivateMessageDetectorHandler implements ConsoleLogHandler {
    
    // Pattern: PlayerName executed command: msg TargetPlayer message
    // Also handles format with [CommandManager] prefix in message
    private static final Pattern MSG_PATTERN = Pattern.compile(
        "(?:\\[CommandManager\\]\\s+)?(\\w+)\\s+executed command:\\s+msg\\s+(\\w+)(?:\\s+(.+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern: PlayerName executed command: reply message
    // Also handles format with [CommandManager] prefix in message
    private static final Pattern REPLY_PATTERN = Pattern.compile(
        "(?:\\[CommandManager\\]\\s+)?(\\w+)\\s+executed command:\\s+reply(?:\\s+(.+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    private final EventLogService eventLogService;

    public PrivateMessageDetectorHandler(@Nonnull EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @Override
    public void handleLog(@Nonnull LogEntry entry) {
        // Only process INFO level logs
        if (entry.getLevel() != LogLevel.INFO) {
            return;
        }
        
        // Check if this is from CommandManager (either in loggerName or in message)
        String loggerName = entry.getLoggerName();
        String message = entry.getMessage();
        
        boolean isCommandManager = loggerName != null && loggerName.contains("CommandManager");
        
        if (!isCommandManager && !message.contains("CommandManager")) {
            return; // Not a command manager log
        }
        
        // Try to match /msg command
        Matcher msgMatcher = MSG_PATTERN.matcher(message);
        if (msgMatcher.find()) {
            String sender = msgMatcher.group(1);
            String target = msgMatcher.group(2);
            String content = msgMatcher.group(3);
            
            // Handle case where message content is empty
            if (content == null || content.trim().isEmpty()) {
                content = "(message vide)";
            }
            
            String formattedMessage = String.format(
                "✉️ **%s** → **%s**: %s",
                sender,
                target,
                content.trim()
            );
            
            eventLogService.sendEvent(formattedMessage);
            return;
        }
        
        // Try to match /reply command
        Matcher replyMatcher = REPLY_PATTERN.matcher(message);
        if (replyMatcher.find()) {
            String sender = replyMatcher.group(1);
            String content = replyMatcher.group(2);
            
            // Handle case where message content is empty
            if (content == null || content.trim().isEmpty()) {
                content = "(message vide)";
            }
            
            String formattedMessage = String.format(
                "↩️ **%s** (reply): %s",
                sender,
                content.trim()
            );
            
            eventLogService.sendEvent(formattedMessage);
        }
    }

    @Override
    public void start() {
        // No initialization needed
    }

    @Override
    public void stop() {
        // No cleanup needed
    }

    @Override
    public boolean isActive() {
        return eventLogService.isRunning();
    }
}
