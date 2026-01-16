package com.lukienlive.hytale.inject;

import com.google.inject.*;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.Main;
import com.lukienlive.hytale.hytale.HytaleConfig;
import com.lukienlive.hytale.inject.annotation.LinkedStorageFile;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.luckperms.api.LuckPerms;

import java.io.File;
import java.util.logging.Logger;

@EqualsAndHashCode(callSuper = true)
@Data
public class HytaleInjector extends AbstractModule {
    private final Main main;
    private final Config<HytaleConfig> config;
    private final LuckPerms luckPerms;


    public Injector createInjector() {
        return Guice.createInjector(this);
    }

    @Override
    protected void configure() {
        bind(Main.class).toInstance(main);
        bind(new TypeLiteral<Config<HytaleConfig>>() {}).toInstance(config);
        bind(LuckPerms.class).toInstance(luckPerms);
    }

    @Provides
    @Singleton
    @LinkedStorageFile
    public File ProvideLinkedStorageFile() {
        try {
            return new File(main.getDataDirectory().toFile(), "linked_players.json");
        }
        catch (Exception e) {
            main.getLogger().atSevere().log("Error providing linked storage file: " + e.getMessage());
            return null;
        }
    }
}
