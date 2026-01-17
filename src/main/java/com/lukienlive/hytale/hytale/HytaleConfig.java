package com.lukienlive.hytale.hytale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HytaleConfig {
    public static final BuilderCodec<HytaleConfig> CODEC;

    private static Map<String, Object> CODEC_MAP = new HashMap<>() {
        {
            put("Discord_token", "DISCORD_BOT_TOKEN");
            put("Guild_id", "YOUR_GUILD_ID_HERE");
            put("Discord_invite_link", "https://discord.gg/VOTRE_INVITE");
            put("Required_role_ids", "ROLE_ID_1,ROLE_ID_2");
            put("Require_discord_link", false);
            put("Require_guild_membership", true);
            put("Require_role", false);
            put("Link_message", "Compte Discord requis!\n\nVotre code de liaison: {code}\n\nEnvoyez ce code en message prive a {bot_username}\n(Le code expire dans 5 minutes)\n\nPas encore sur le Discord? Rejoignez-nous: {discord_invite}");
            put("Not_in_guild_message", "Vous devez etre membre du serveur Discord!\n\nVotre compte est lie mais vous avez quitte le serveur Discord.\nRejoignez-nous pour acceder au serveur: {discord_invite}");
            put("Missing_role_message", "Vous n'avez pas le role requis!\n\nVotre compte est lie mais vous n'avez pas le role necessaire sur le Discord.\nContactez un administrateur ou rejoignez: {discord_invite}");
            put("Console_channel_id", "YOUR_CHANNEL_ID_HERE");
            put("Events_channel_id", "YOUR_EVENTS_CHANNEL_ID_HERE");
            put("Status_channel_id", "YOUR_STATUS_CHANNEL_ID_HERE");
            put("Status_message_id", "");
            put("Enable_console_logs", true);
            put("Console_minimum_log_level", "INFO");
            put("Enable_bot_status", false);
            put("Enable_status_message", false);
            put("Role_Group_Mapping", "");
        }
    };

    static {
        BuilderCodec.Builder<HytaleConfig> codec = BuilderCodec.builder(HytaleConfig.class, HytaleConfig::new);

        for (Map.Entry<String, Object> entry : CODEC_MAP.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            var keyedCodec = new KeyedCodec<Object>(key, (Codec<Object>) createCodec(defaultValue));
            codec.append(keyedCodec,
                    (config, newValue) -> {
                        config.values.put(key, newValue);
                    },
                    config -> {
                        return config.values.getOrDefault(key, defaultValue);
                    }).add();
        }
        CODEC = codec.build();
    }

    private final Map<String, Object> values = new HashMap<>();

    private static Codec<?> createCodec(Object defaultValue) {
        if (defaultValue instanceof String) {
            return Codec.STRING;
        } else if (defaultValue instanceof Integer) {
            return Codec.INTEGER;
        } else if (defaultValue instanceof Boolean) {
            return Codec.BOOLEAN;
        }
        throw new IllegalArgumentException("Unsupported type for codec creation");
    }

    public boolean getBoolean(String key) {
        Object value = values.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    public String getString(String key) {
        Object value = values.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return "";
    }

    public int getInt(String key) {
        Object value = values.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    public void set(String key, Object value) {
        this.values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object value = values.get(key);
        if (value instanceof String) {
            String str = (String) value;
            if (str.isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(str.split(","));
        }
        return new ArrayList<>();
    }
}
