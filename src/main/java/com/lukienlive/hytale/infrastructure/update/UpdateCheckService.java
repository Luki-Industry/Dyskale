package com.lukienlive.hytale.infrastructure.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hypixel.hytale.server.core.util.Config;
import com.lukienlive.hytale.hytale.HytaleConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class UpdateCheckService {
    private static final String CURRENT_VERSION = "1.0.0";
    private static final long INITIAL_DELAY_MINUTES = 1;

    private final Config<HytaleConfig> config;
    private final Logger logger;
    private final HttpClient httpClient;
    private ScheduledExecutorService executor;

    @Inject
    public UpdateCheckService(Config<HytaleConfig> config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void start() {
        if (!config.get().getBoolean("Enable_update_check") || executor != null) {
            return;
        }

        long intervalHours = Math.max(1, config.get().getInt("Update_check_interval_hours"));
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "dyskale-update-check");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(this::checkForUpdate, INITIAL_DELAY_MINUTES, intervalHours * 60, TimeUnit.MINUTES);
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void checkForUpdate() {
        try {
            String repository = config.get().getString("Update_repository");
            if (repository.isBlank() || repository.contains("YOUR_")) {
                return;
            }

            URI uri = URI.create("https://api.github.com/repos/" + repository + "/releases/latest");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Dyskale-Update-Checker")
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(this::handleResponse)
                    .exceptionally(error -> {
                        logger.log(Level.FINE, "Vérification de mise à jour Dyskale impossible", error);
                        return null;
                    });
        } catch (Exception e) {
            logger.log(Level.FINE, "Configuration du check de mise à jour invalide", e);
        }
    }

    private void handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            logger.log(Level.FINE, "GitHub a répondu HTTP " + response.statusCode() + " lors du check Dyskale.");
            return;
        }

        try {
            JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
            String latestVersion = normalizeVersion(release.get("tag_name").getAsString());
            if (compareVersions(latestVersion, CURRENT_VERSION) > 0) {
                String releaseUrl = release.has("html_url") ? release.get("html_url").getAsString() : "";
                logger.log(Level.INFO, "Nouvelle version Dyskale disponible: " + latestVersion
                        + " (version actuelle: " + CURRENT_VERSION + ") " + releaseUrl);
            }
        } catch (RuntimeException e) {
            logger.log(Level.FINE, "Réponse GitHub invalide pour le check Dyskale", e);
        }
    }

    private String normalizeVersion(String version) {
        return version.trim().replaceFirst("^[vV]", "");
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = normalizeVersion(left).split("\\.");
        String[] rightParts = normalizeVersion(right).split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);

        for (int index = 0; index < length; index++) {
            int leftNumber = index < leftParts.length ? parseVersionPart(leftParts[index]) : 0;
            int rightNumber = index < rightParts.length ? parseVersionPart(rightParts[index]) : 0;
            if (leftNumber != rightNumber) {
                return Integer.compare(leftNumber, rightNumber);
            }
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        String numericPart = part.split("[-+]")[0];
        return Integer.parseInt(numericPart);
    }
}