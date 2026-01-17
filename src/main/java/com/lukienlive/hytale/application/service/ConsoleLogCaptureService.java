package com.lukienlive.hytale.application.service;

import com.lukienlive.hytale.domain.console.ConsoleLogHandler;
import com.lukienlive.hytale.domain.console.LogEntry;
import com.lukienlive.hytale.domain.console.LogLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Application service for managing console log capture and forwarding.
 * Orchestrates the flow between log handlers and output destinations.
 * 
 * This service is framework-agnostic and depends only on domain abstractions.
 */
public class ConsoleLogCaptureService {
    
    private final List<ConsoleLogHandler> handlers;
    private LogLevel minimumLogLevel;
    private boolean enabled;

    public ConsoleLogCaptureService() {
        this.handlers = new ArrayList<>();
        this.minimumLogLevel = LogLevel.INFO;
        this.enabled = false;
    }

    /**
     * Register a new log handler.
     * 
     * @param handler The handler to register
     */
    public void registerHandler(ConsoleLogHandler handler) {
        Objects.requireNonNull(handler, "Handler cannot be null");
        if (!handlers.contains(handler)) {
            handlers.add(handler);
        }
    }

    /**
     * Unregister a log handler.
     * 
     * @param handler The handler to unregister
     */
    public void unregisterHandler(ConsoleLogHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Get all registered handlers (read-only).
     * 
     * @return Unmodifiable list of handlers
     */
    public List<ConsoleLogHandler> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    /**
     * Set the minimum log level to capture.
     * 
     * @param level The minimum level
     */
    public void setMinimumLogLevel(LogLevel level) {
        this.minimumLogLevel = Objects.requireNonNull(level, "Log level cannot be null");
    }

    /**
     * Get the current minimum log level.
     * 
     * @return The minimum log level
     */
    public LogLevel getMinimumLogLevel() {
        return minimumLogLevel;
    }

    /**
     * Process a log entry through all registered handlers.
     * 
     * @param entry The log entry to process
     */
    public void processLog(LogEntry entry) {
        if (!enabled) {
            return;
        }

        if (entry == null) {
            return;
        }

        // Filter based on log level
        if (!entry.getLevel().shouldLog(minimumLogLevel)) {
            return;
        }

        // Filter based on domain rules
        if (entry.shouldBeFiltered()) {
            return;
        }

        // Pass to all active handlers
        for (ConsoleLogHandler handler : handlers) {
            if (handler.isActive()) {
                try {
                    handler.handleLog(entry);
                } catch (Exception e) {
                    // Silently ignore handler errors to prevent logging loops
                    // In production, this could be logged to a separate error channel
                }
            }
        }
    }

    /**
     * Start the log capture service and all registered handlers.
     */
    public void start() {
        if (enabled) {
            return;
        }

        enabled = true;
        for (ConsoleLogHandler handler : handlers) {
            try {
                handler.start();
            } catch (Exception e) {
                // Continue with other handlers even if one fails
            }
        }
    }

    /**
     * Stop the log capture service and all registered handlers.
     */
    public void stop() {
        if (!enabled) {
            return;
        }

        enabled = false;
        for (ConsoleLogHandler handler : handlers) {
            try {
                handler.stop();
            } catch (Exception e) {
                // Continue with other handlers even if one fails
            }
        }
    }

    /**
     * Check if the service is currently enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the number of active handlers.
     * 
     * @return The count of active handlers
     */
    public int getActiveHandlerCount() {
        return (int) handlers.stream()
                .filter(ConsoleLogHandler::isActive)
                .count();
    }
}
