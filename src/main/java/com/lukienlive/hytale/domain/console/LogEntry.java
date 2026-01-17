package com.lukienlive.hytale.domain.console;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a single log entry from the console.
 * Immutable value object following Clean Architecture principles.
 */
public final class LogEntry {
    private final Instant timestamp;
    private final LogLevel level;
    private final String loggerName;
    private final String message;
    private final String threadName;
    private final Throwable throwable;

    private LogEntry(Builder builder) {
        this.timestamp = builder.timestamp;
        this.level = builder.level;
        this.loggerName = builder.loggerName;
        this.message = builder.message;
        this.threadName = builder.threadName;
        this.throwable = builder.throwable;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getMessage() {
        return message;
    }

    public String getThreadName() {
        return threadName;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean hasThrowable() {
        return throwable != null;
    }

    /**
     * Check if this log entry should be filtered out based on certain criteria.
     * 
     * @return true if the log should be filtered (not sent)
     */
    public boolean shouldBeFiltered() {
        // Filter logs from our own Discord bot to avoid loops
        if (loggerName != null && (
                loggerName.contains("DiscordBot") ||
                loggerName.contains("jda") ||
                loggerName.contains("ConsoleLogService") ||
                loggerName.contains("DiscordLogger"))) {
            return true;
        }

        // Filter empty messages
        return message == null || message.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(timestamp, logEntry.timestamp) &&
                level == logEntry.level &&
                Objects.equals(loggerName, logEntry.loggerName) &&
                Objects.equals(message, logEntry.message) &&
                Objects.equals(threadName, logEntry.threadName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, level, loggerName, message, threadName);
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", level=" + level +
                ", loggerName='" + loggerName + '\'' +
                ", message='" + message + '\'' +
                ", threadName='" + threadName + '\'' +
                ", hasThrowable=" + hasThrowable() +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Instant timestamp = Instant.now();
        private LogLevel level = LogLevel.INFO;
        private String loggerName = "";
        private String message = "";
        private String threadName = "";
        private Throwable throwable = null;

        private Builder() {
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
