package com.lukienlive.hytale.infrastructure.console;

import com.hypixel.hytale.logger.backend.HytaleLoggerBackend;
import com.lukienlive.hytale.application.service.ConsoleLogCaptureService;
import com.lukienlive.hytale.domain.console.LogEntry;
import com.lukienlive.hytale.domain.console.LogLevel;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.LogRecord;

/**
 * Bridge between Hytale's native logging backend and our console log capture system.
 * Uses Hytale's subscription mechanism to intercept ALL server logs (core + plugins).
 *
 * <p><b>Infrastructure Layer:</b> Adapts Hytale's logging API to our domain model.</p>
 *
 * <h3>How it works:</h3>
 * <ol>
 *     <li>Creates a thread-safe subscriber list (CopyOnWriteArrayList)</li>
 *     <li>Registers it with {@link HytaleLoggerBackend#subscribe(CopyOnWriteArrayList)}</li>
 *     <li>Hytale pushes {@link LogRecord} entries to the list</li>
 *     <li>We poll the list and convert to {@link LogEntry}</li>
 *     <li>Forwards to {@link ConsoleLogCaptureService}</li>
 * </ol>
 *
 * <h3>Threading model:</h3>
 * <ul>
 *     <li>Hytale writes to the list from logging threads (async)</li>
 *     <li>We poll from our processing thread (scheduled task)</li>
 *     <li>{@link CopyOnWriteArrayList} ensures thread safety</li>
 * </ul>
 *
 * @see HytaleLoggerBackend#subscribe(CopyOnWriteArrayList)
 * @see HytaleLoggerBackend#unsubscribe(CopyOnWriteArrayList)
 */
public class HytaleLoggerBridge {

    private final ConsoleLogCaptureService captureService;
    private final CopyOnWriteArrayList<LogRecord> logBuffer;
    private volatile boolean active = false;

    public HytaleLoggerBridge(@Nonnull ConsoleLogCaptureService captureService) {
        this.captureService = captureService;
        this.logBuffer = new CopyOnWriteArrayList<>();
    }

    /**
     * Subscribes to Hytale's logging backend to receive ALL logs.
     * This captures:
     * <ul>
     *     <li>Hytale core server logs (QUICTransport, HandshakeHandler, World, etc.)</li>
     *     <li>Plugin logs (including ours)</li>
     *     <li>All log levels (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)</li>
     * </ul>
     */
    public void install() {
        if (active) {
            return; // Already installed
        }

        HytaleLoggerBackend.subscribe(logBuffer);
        active = true;
    }

    /**
     * Unsubscribes from Hytale's logging backend.
     * Should be called during plugin shutdown.
     */
    public void uninstall() {
        if (!active) {
            return;
        }

        HytaleLoggerBackend.unsubscribe(logBuffer);
        active = false;
        logBuffer.clear();
    }

    /**
     * Processes accumulated log records from Hytale.
     * Should be called periodically (e.g., every tick or every 50ms).
     *
     * <p><b>Performance:</b> Removes processed entries to prevent unbounded growth.</p>
     */
    public void processLogs() {
        if (logBuffer.isEmpty()) {
            return;
        }

        // Process all pending logs
        for (LogRecord record : logBuffer) {
            try {
                LogEntry entry = convertToLogEntry(record);
                captureService.processLog(entry);
            } catch (Exception e) {
                // Silently ignore errors to prevent logging loops
                // (we can't log here because it would create a feedback loop)
            }
        }

        // Clear processed entries
        logBuffer.clear();
    }

    /**
     * Converts Hytale's {@link LogRecord} to our domain {@link LogEntry}.
     */
    @Nonnull
    private LogEntry convertToLogEntry(@Nonnull LogRecord record) {
        LogLevel level = mapLogLevel(record.getLevel());
        Instant timestamp = Instant.ofEpochMilli(record.getMillis());
        String loggerName = record.getLoggerName() != null ? record.getLoggerName() : "Unknown";
        String message = formatMessage(record);

        return LogEntry.builder()
                .level(level)
                .message(message)
                .timestamp(timestamp)
                .loggerName(loggerName)
                .threadName(getThreadName(record))
                .throwable(record.getThrown())
                .build();
    }

    /**
     * Maps java.util.logging.Level to our LogLevel enum.
     */
    @Nonnull
    private LogLevel mapLogLevel(@Nonnull java.util.logging.Level julLevel) {
        int value = julLevel.intValue();

        if (value >= java.util.logging.Level.SEVERE.intValue()) {
            return LogLevel.SEVERE;
        } else if (value >= java.util.logging.Level.WARNING.intValue()) {
            return LogLevel.WARNING;
        } else if (value >= java.util.logging.Level.INFO.intValue()) {
            return LogLevel.INFO;
        } else if (value >= java.util.logging.Level.CONFIG.intValue()) {
            return LogLevel.DEBUG;
        } else {
            return LogLevel.TRACE; // FINE, FINER, FINEST → TRACE
        }
    }

    /**
     * Formats the log message, including parameters if present.
     */
    @Nonnull
    private String formatMessage(@Nonnull LogRecord record) {
        String message = record.getMessage();
        Object[] params = record.getParameters();

        if (params == null || params.length == 0) {
            return message;
        }

        // Simple parameter substitution for {0}, {1}, etc.
        try {
            return String.format(message.replaceAll("\\{(\\d+)\\}", "%$1\\$s"), params);
        } catch (Exception e) {
            // If formatting fails, return raw message + params
            return message + " " + java.util.Arrays.toString(params);
        }
    }

    /**
     * Extracts thread name from LogRecord.
     */
    @Nonnull
    private String getThreadName(@Nonnull LogRecord record) {
        // Hytale doesn't set thread name in LogRecord, use thread ID
        long threadId = record.getThreadID();
        return threadId >= 0 ? "Thread-" + threadId : "Unknown";
    }

    /**
     * Checks if the bridge is currently active.
     */
    public boolean isActive() {
        return active;
    }
}
