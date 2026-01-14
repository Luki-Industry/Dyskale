package com.lukienlive.hytale.inject;

import com.google.inject.*;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.Main;
import com.lukienlive.hytale.hytale.HytaleConfig;
import com.lukienlive.hytale.inject.annotation.LinkedStorageFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;

@EqualsAndHashCode(callSuper = true)
@Data
public class HytaleInjector extends AbstractModule {
    private final Main main;
    private final Config<HytaleConfig> config;


    public Injector createInjector() {
        return Guice.createInjector(this);
    }

    @Override
    protected void configure() {
        bind(Main.class).toInstance(main);
        bind(Config.class).toInstance(config);
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
