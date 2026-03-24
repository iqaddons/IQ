package net.iqaddons.mod.utils.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class ModrinthUpdateChecker {

    private static final String MOD_ID = "iqaddons";
    private static final String MODRINTH_PROJECT_SLUG = "iq-addons";
    private static final String MODRINTH_PROJECT_URL = "https://modrinth.com/mod/" + MODRINTH_PROJECT_SLUG;
    private static final String USER_AGENT = "IQAddons/" + currentVersion() + " (https://github.com/pehenrii/IQ)";

    public static final ModrinthUpdateChecker INSTANCE = new ModrinthUpdateChecker();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "iq-modrinth-update-checker");
        thread.setDaemon(true);
        return thread;
    });

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .executor(executor)
            .build();

    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);

    private ModrinthUpdateChecker() {
    }

    public void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onServerJoin(client));
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void onServerJoin(@NotNull MinecraftClient client) {
        if (client.getCurrentServerEntry() == null) {
            return;
        }

        if (!checkInProgress.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture
                .supplyAsync(this::fetchUpdateState, executor)
                .whenComplete((state, throwable) -> {
                    checkInProgress.set(false);

                    if (throwable != null) {
                        log.debug("Failed to check Modrinth updates", throwable);
                        return;
                    }

                    if (state == null || !state.hasUpdate()) {
                        return;
                    }

                    client.execute(() -> notifyPlayer(state));
                });
    }

    private @NotNull UpdateState fetchUpdateState() {
        String currentVersion = currentVersion();
        URI requestUri = URI.create(buildVersionsUrl(currentMinecraftVersion()));
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .timeout(Duration.ofSeconds(8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.debug("Modrinth update check returned status {}", response.statusCode());
                return UpdateState.noUpdate();
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            List<VersionInfo> parsedVersions = parseVersions(versions);
            Optional<VersionInfo> latestVersion = parsedVersions.stream()
                    .max(Comparator.comparing(VersionInfo::publishedAt));

            if (latestVersion.isEmpty()) {
                return UpdateState.noUpdate();
            }

            Optional<VersionInfo> currentListedVersion = parsedVersions.stream()
                    .filter(info -> matchesCurrentVersion(info.versionNumber(), currentVersion))
                    .findFirst();

            if (currentListedVersion.isEmpty()) {
                log.debug("Current version {} was not found on Modrinth; skipping alert", currentVersion);
                return UpdateState.noUpdate();
            }

            VersionInfo currentInfo = currentListedVersion.get();
            VersionInfo latestInfo = latestVersion.get();
            if (!latestInfo.publishedAt().isAfter(currentInfo.publishedAt())) {
                return UpdateState.noUpdate();
            }

            return new UpdateState(true, currentVersion, latestInfo.versionNumber(), latestInfo.versionUrl());
        } catch (Exception e) {
            log.debug("Failed to fetch Modrinth versions", e);
            return UpdateState.noUpdate();
        }
    }

    private Optional<VersionInfo> parseVersionInfo(@NotNull JsonObject json) {
        if (!json.has("version_number") || !json.has("date_published")) {
            return Optional.empty();
        }

        try {
            String versionNumber = json.get("version_number").getAsString();
            Instant publishedAt = Instant.parse(json.get("date_published").getAsString());
            String versionId = json.has("id") ? json.get("id").getAsString() : "";
            String versionUrl = versionId.isBlank()
                    ? MODRINTH_PROJECT_URL
                    : MODRINTH_PROJECT_URL + "/version/" + versionId;

            return Optional.of(new VersionInfo(versionNumber, publishedAt, versionUrl));
        } catch (Exception e) {
            log.debug("Failed to parse Modrinth version payload", e);
            return Optional.empty();
        }
    }

    private @NotNull List<VersionInfo> parseVersions(@NotNull JsonArray versions) {
        List<VersionInfo> parsedVersions = new ArrayList<>();
        for (JsonElement element : versions) {
            if (!element.isJsonObject()) {
                continue;
            }

            parseVersionInfo(element.getAsJsonObject()).ifPresent(parsedVersions::add);
        }

        return parsedVersions;
    }

    private void notifyPlayer(@NotNull UpdateState state) {
        MessageUtil.WARNING.sendMessage(String.format(
                Locale.ROOT,
                "A new Modrinth update is available: %s (you are on %s).",
                state.latestVersion(),
                state.currentVersion()
        ));
        MessageUtil.INFO.sendMessage("Download it here: " + state.downloadUrl());
    }

    private static boolean matchesCurrentVersion(@NotNull String remoteVersion, @NotNull String currentVersion) {
        if (remoteVersion.equalsIgnoreCase(currentVersion)) {
            return true;
        }

        String normalizedRemote = normalizeVersion(remoteVersion);
        String normalizedCurrent = normalizeVersion(currentVersion);
        return normalizedRemote.equalsIgnoreCase(normalizedCurrent);
    }

    private static @NotNull String normalizeVersion(@NotNull String version) {
        int plusIndex = version.indexOf('+');
        return plusIndex >= 0 ? version.substring(0, plusIndex) : version;
    }

    private static @NotNull String buildVersionsUrl(@NotNull String minecraftVersion) {
        String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String gameVersions = URLEncoder.encode("[\"" + minecraftVersion + "\"]", StandardCharsets.UTF_8);
        return "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_SLUG
                + "/version?loaders=" + loaders
                + "&game_versions=" + gameVersions;
    }

    private static @NotNull String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static @NotNull String currentMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private record VersionInfo(String versionNumber, Instant publishedAt, String versionUrl) {
    }

    private record UpdateState(
            boolean hasUpdate,
            String currentVersion,
            String latestVersion,
            String downloadUrl
    ) {
        private static @NotNull UpdateState noUpdate() {
            return new UpdateState(false, "", "", "");
        }
    }
}
