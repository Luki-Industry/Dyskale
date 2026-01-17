package com.lukienlive.hytale.domain.console;

/**
 * Domain interface for handling console log entries.
 * Implementations will be in the infrastructure layer.
 */
public interface ConsoleLogHandler {
    
    /**
     * Process a log entry from the console.
     * 
     * @param entry The log entry to process
     */
    void handleLog(LogEntry entry);

    /**
     * Called when the handler should start processing logs.
     */
    void start();

    /**
     * Called when the handler should stop processing logs.
     */
    void stop();

    /**
     * Check if the handler is currently active.
     * 
     * @return true if the handler is running
     */
    boolean isActive();
}
