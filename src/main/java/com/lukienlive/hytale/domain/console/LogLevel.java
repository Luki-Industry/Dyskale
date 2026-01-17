package com.lukienlive.hytale.domain.console;

/**
 * Represents the severity level of a log entry.
 * Domain value object - no dependencies on infrastructure.
 */
public enum LogLevel {
    SEVERE("SEVERE", "🔴", 3),
    WARNING("WARNING", "⚠️", 2),
    INFO("INFO", "ℹ️", 1),
    DEBUG("DEBUG", "🐛", 0),
    TRACE("TRACE", "📝", -1);

    private final String name;
    private final String icon;
    private final int priority;

    LogLevel(String name, String icon, int priority) {
        this.name = name;
        this.icon = icon;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Parse a string log level name into the corresponding enum value.
     * 
     * @param levelName The log level name to parse
     * @return The corresponding LogLevel, or INFO if unknown
     */
    public static LogLevel fromString(String levelName) {
        if (levelName == null) {
            return INFO;
        }

        String normalized = levelName.trim().toUpperCase();
        for (LogLevel level : values()) {
            if (level.name.equals(normalized)) {
                return level;
            }
        }
        
        return INFO;
    }

    /**
     * Check if this log level should be logged based on a minimum level.
     * 
     * @param minimumLevel The minimum level required
     * @return true if this level should be logged
     */
    public boolean shouldLog(LogLevel minimumLevel) {
        return this.priority >= minimumLevel.priority;
    }
}
