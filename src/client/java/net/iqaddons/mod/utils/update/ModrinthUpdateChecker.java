package net.iqaddons.mod.utils.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
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
    private static final String CHAT_SEPARATOR = "§8§m------------------------------------------------";
    private static final String PREFIX = "§d§l[IQ] §r";

    public static final ModrinthUpdateChecker INSTANCE = new ModrinthUpdateChecker();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "iq-modrinth-update-checker");
        thread.setDaemon(true);
        return thread;
    });

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private final AtomicBoolean checkInProgress = new AtomicBoolean(false);
    private volatile boolean checkedHypixelThisConnection = false;

    private ModrinthUpdateChecker() {
    }

    public void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onServerJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onServerDisconnect(client));
    }

    public void checkNow(boolean notifyWhenUpToDate) {
        Minecraft client = Minecraft.getInstance();
        if (!checkInProgress.compareAndSet(false, true)) {
            MessageUtil.INFO.sendMessage("Update check is already running.");
            return;
        }

        CompletableFuture
                .supplyAsync(this::fetchUpdateState, executor)
                .whenComplete((state, throwable) -> {
                    checkInProgress.set(false);

                    if (throwable != null) {
                        log.debug("Failed to check Modrinth updates", throwable);
                        client.execute(() -> MessageUtil.ERROR.sendMessage("Failed to check Modrinth updates."));
                        return;
                    }

                    client.execute(() -> notifyPlayer(state, notifyWhenUpToDate));
                });
    }

    public void sendTestMessage() {
        notifyPlayer(new UpdateState(
                UpdateCheckStatus.UPDATE_AVAILABLE,
                "1.0.4",
                "1.0.5",
                List.of("26.1.2", "26.2"),
                MODRINTH_PROJECT_URL,
                ""
        ), false);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void onServerJoin(@NotNull Minecraft client) {
        executor.schedule(() -> client.execute(() -> checkAfterServerJoin(client)), 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void onServerDisconnect(@NotNull Minecraft client) {
        executor.schedule(() -> client.execute(() -> {
            if (client.getConnection() == null) {
                checkedHypixelThisConnection = false;
            }
        }), 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void checkAfterServerJoin(@NotNull Minecraft client) {
        if (client.getCurrentServer() == null || !isHypixelServer(client.getCurrentServer().ip)) {
            return;
        }

        if (checkedHypixelThisConnection) {
            return;
        }

        if (!checkInProgress.compareAndSet(false, true)) {
            return;
        }

        checkedHypixelThisConnection = true;

        CompletableFuture
                .supplyAsync(this::fetchUpdateState, executor)
                .whenComplete((state, throwable) -> {
                    checkInProgress.set(false);

                    if (throwable != null) {
                        log.debug("Failed to check Modrinth updates", throwable);
                        return;
                    }

                    client.execute(() -> notifyPlayer(state, true));
                });
    }

    private @NotNull UpdateState fetchUpdateState() {
        String currentVersion = currentVersion();
        URI requestUri = URI.create(buildVersionsUrl());
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        Exception lastException = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                UpdateState state = fetchUpdateStateOnce(currentVersion, request);
                if (state.status() != UpdateCheckStatus.FAILED || attempt == 2) {
                    return state;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to fetch Modrinth versions on attempt {}", attempt, e);
            }

            sleepBeforeRetry(attempt);
        }

        if (lastException != null) {
            return UpdateState.failed(currentVersion, "Could not reach Modrinth updates.");
        }

        return UpdateState.failed(currentVersion, "Could not check Modrinth updates.");
    }

    private @NotNull UpdateState fetchUpdateStateOnce(@NotNull String currentVersion, @NotNull HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Modrinth update check returned status {}", response.statusCode());
                return UpdateState.failed(currentVersion, "Modrinth returned HTTP " + response.statusCode() + ".");
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            List<VersionInfo> parsedVersions = parseVersions(versions);
            Optional<VersionInfo> latestVersion = parsedVersions.stream()
                    .max((left, right) -> compareVersions(left.versionNumber(), right.versionNumber()));

            if (latestVersion.isEmpty()) {
                log.warn("Modrinth update check found no parseable versions");
                return UpdateState.failed(currentVersion, "No Modrinth versions were found.");
            }

            VersionInfo latestInfo = latestVersion.get();
            log.info(
                    "IQ Modrinth update check: current={}, latest={}, versions={}",
                    currentVersion,
                    latestInfo.versionNumber(),
                    parsedVersions.stream().map(VersionInfo::versionNumber).toList()
            );

            if (compareVersions(currentVersion, latestInfo.versionNumber()) >= 0) {
                return UpdateState.upToDate(currentVersion, latestInfo.versionNumber());
            }

            return new UpdateState(
                    UpdateCheckStatus.UPDATE_AVAILABLE,
                    currentVersion,
                    latestInfo.versionNumber(),
                    latestInfo.gameVersions(),
                    latestInfo.versionUrl(),
                    ""
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Duration.ofSeconds(attempt * 2L).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
            List<String> gameVersions = parseGameVersions(json);
            String versionUrl = versionId.isBlank()
                    ? MODRINTH_PROJECT_URL
                    : MODRINTH_PROJECT_URL + "/version/" + versionId;

            return Optional.of(new VersionInfo(versionNumber, publishedAt, gameVersions, versionUrl));
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

    private void notifyPlayer(UpdateState state, boolean notifyWhenUpToDate) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || state == null) {
            return;
        }

        if (state.status() == UpdateCheckStatus.FAILED) {
            client.player.sendSystemMessage(Component.literal(PREFIX + "§c" + state.failureMessage()));
            return;
        }

        if (!state.hasUpdate()) {
            if (notifyWhenUpToDate) {
                client.player.sendSystemMessage(Component.literal(PREFIX + "§aYour version is already up to date."));
            }
            return;
        }

        client.player.sendSystemMessage(Component.literal(CHAT_SEPARATOR));
        client.player.sendSystemMessage(Component.literal(String.format(
                Locale.ROOT,
                "%s§fUpdate available §7(§c%s §7-> §a%s§f for %s§7)",
                PREFIX,
                displayVersion(state.currentVersion()),
                displayVersion(state.latestVersion()),
                formatGameVersions(state.gameVersions())
        )));
        client.player.sendSystemMessage(Component.literal(PREFIX + "§7Click here to download.")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(state.downloadUrl())))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("§fOpen IQ on Modrinth")))));
        client.player.sendSystemMessage(Component.literal(CHAT_SEPARATOR));
    }

    private static @NotNull List<String> parseGameVersions(@NotNull JsonObject json) {
        if (!json.has("game_versions") || !json.get("game_versions").isJsonArray()) {
            return List.of(currentMinecraftVersion());
        }

        List<String> gameVersions = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("game_versions")) {
            if (element.isJsonPrimitive()) {
                gameVersions.add(element.getAsString());
            }
        }
        return gameVersions.isEmpty() ? List.of(currentMinecraftVersion()) : gameVersions;
    }

    private static boolean isHypixelServer(String serverAddress) {
        return serverAddress != null && serverAddress.toLowerCase(Locale.ROOT).contains("hypixel.net");
    }

    private static int compareVersions(@NotNull String currentVersion, @NotNull String latestVersion) {
        String[] currentParts = displayVersion(currentVersion).split("\\.");
        String[] latestParts = displayVersion(latestVersion).split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int current = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            int latest = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            if (current != latest) {
                return Integer.compare(current, latest);
            }
        }

        return 0;
    }

    private static int parseVersionPart(@NotNull String part) {
        String digits = part.replaceFirst("[^0-9].*$", "");
        if (digits.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static @NotNull String displayVersion(@NotNull String version) {
        return normalizeVersion(version);
    }

    private static @NotNull String normalizeVersion(@NotNull String version) {
        int plusIndex = version.indexOf('+');
        return plusIndex >= 0 ? version.substring(0, plusIndex) : version;
    }

    private static @NotNull String formatGameVersions(@NotNull List<String> gameVersions) {
        List<String> uniqueVersions = gameVersions.stream()
                .filter(version -> version != null && !version.isBlank())
                .distinct()
                .toList();

        if (uniqueVersions.isEmpty()) {
            return currentMinecraftVersion();
        }

        if (uniqueVersions.size() == 1) {
            return uniqueVersions.getFirst();
        }

        return String.join(" & ", uniqueVersions);
    }

    private static @NotNull String buildVersionsUrl() {
        String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        return "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_SLUG
                + "/version?loaders=" + loaders;
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

    private record VersionInfo(String versionNumber, Instant publishedAt, List<String> gameVersions, String versionUrl) {
    }

    private enum UpdateCheckStatus {
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        FAILED
    }

    private record UpdateState(
            UpdateCheckStatus status,
            String currentVersion,
            String latestVersion,
            List<String> gameVersions,
            String downloadUrl,
            String failureMessage
    ) {
        private boolean hasUpdate() {
            return status == UpdateCheckStatus.UPDATE_AVAILABLE;
        }

        private static @NotNull UpdateState upToDate(@NotNull String currentVersion, @NotNull String latestVersion) {
            return new UpdateState(UpdateCheckStatus.UP_TO_DATE, currentVersion, latestVersion, List.of(), "", "");
        }

        private static @NotNull UpdateState failed(@NotNull String currentVersion, @NotNull String failureMessage) {
            return new UpdateState(UpdateCheckStatus.FAILED, currentVersion, "", List.of(), "", failureMessage);
        }
    }
}
