package net.iqaddons.mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.IQKeyBindings;
import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.config.loader.PearlWaypointConfigLoader;
import net.iqaddons.mod.config.loader.PileConfigLoader;
import net.iqaddons.mod.config.loader.CratePriorityConfigLoader;
import net.iqaddons.mod.config.screen.KuudraWaypointEditorStore;
import net.iqaddons.mod.config.screen.KuudraWaypointsScreen;
import net.iqaddons.mod.features.kuudra.waypoints.DpsWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.EtherwarpWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.SkipWaypointFeature;
import net.iqaddons.mod.features.widgets.CustomSplitsWidget;
import net.iqaddons.mod.features.widgets.FreshersTimerWidget;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.iqaddons.mod.manager.KuudraDebugManager;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.PersonalBestManager;
import net.iqaddons.mod.manager.PhaseSplitsPBManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.manager.pricing.KuudraProfitTrackerManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.profit.ProfitScope;
import net.iqaddons.mod.utils.update.ModrinthUpdateChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class IQCommand {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final String CHAT_SEPARATOR = "§8§m--------------------";
    private static final long PROFIT_RESET_CONFIRM_WINDOW_MS = 10_000L;
    private static ProfitScope pendingProfitResetScope = null;
    private static long pendingProfitResetExpiresAt = 0L;

    public static void register(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("iq")
                        .executes(ctx -> {
                            mc.schedule(() -> IQKeyBindings.openConfigScreen(mc));
                            return 1;
                        })
                        .then(literal("hud")
                                .executes(ctx -> {
                                    mc.schedule(() -> HudManager.get().openEditor());
                                    ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§fHUD Editor opened. §dY§f toggles center guides."));
                                    return 1;
                                })
                        )
                        .then(literal("etherwarps").executes(ctx -> openEtherwarpCategories(ctx.getSource())))
                        .then(literal("etherwarp").executes(ctx -> openEtherwarpCategories(ctx.getSource())))
                        .then(literal("waypoints").executes(ctx -> openKuudraWaypoints(ctx.getSource())))
                        .then(literal("updatewaypoints").executes(ctx -> updateWaypointsFromDefaults(ctx.getSource())))
                        .then(literal("reload").executes(ctx -> {
                            mc.schedule(() -> {
                                SupplyStateManager.get().reloadPileLocations(PileConfigLoader.get().reload());
                                CratePriorityConfigLoader.get().reload();

                                IQModClient client = IQModClient.get();
                                if (client != null && client.getFeatureManager() != null) {
                                    reloadWaypointFeatures(client);
                                    return;
                                }

                                EtherwarpConfigLoader.getNewEtherwarpWaypoints().reload();
                                EtherwarpConfigLoader.get().reload();
                                CratePriorityConfigLoader.get().reload();
                            });
                            ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§fWaypoints and configs reloaded."));
                            return 1;
                        }))
                        .then(literal("discord").executes(ctx -> {
                            Util.getPlatform().openUri("https://discord.gg/HdhXhCWcW9");
                            ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§fOpening IQ Discord invite..."));
                            return 1;
                        }))
                        .then(literal("updatecheck")
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§7Checking Modrinth updates..."));
                                    ModrinthUpdateChecker.INSTANCE.checkNow(true);
                                    return 1;
                                })
                                .then(literal("test").executes(ctx -> {
                                    ModrinthUpdateChecker.INSTANCE.sendTestMessage();
                                    return 1;
                                })))
                        .then(literal("resetchests").executes(ctx -> {
                            ChestCounterManager.get().reset();
                            ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§fChest counter reseted."));
                            return 1;
                        }))
                        .then(literal("chests")
                                .executes(ctx -> {
                                    int current = ChestCounterManager.get().getChests();
                                    ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§fChest counter: §e" + current + "§7/" + ChestCounterManager.MAX_CHESTS));
                                    ctx.getSource().sendFeedback(Component.literal("§8Use §f/iq chests set <amount>§8 or §f/iq chests reset§8."));
                                    return 1;
                                })
                                .then(literal("set")
                                        .then(argument("amount", integer(0, ChestCounterManager.MAX_CHESTS)).executes(ctx -> {
                                            int amount = getInteger(ctx, "amount");
                                            ChestCounterManager.get().set(amount);
                                            ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§fChest counter set to §e" + amount + "§7/" + ChestCounterManager.MAX_CHESTS + "§f."));
                                            return 1;
                                        })))
                                .then(literal("reset").executes(ctx -> {
                                    ChestCounterManager.get().reset();
                                    ctx.getSource().sendFeedback(Component.literal("§d§l[IQ] §r§fChest counter reset to §e0§f."));
                                    return 1;
                                }))
                        )
                        .then(literal("pb")
                                .executes(ctx -> sendPersonalBest(ctx.getSource()))
                                .then(literal("details").executes(ctx -> sendPersonalBestDetails(ctx.getSource()))))
                        .then(literal("pbs").executes(ctx -> sendPhaseSplitsPBs(ctx.getSource())))
                        .then(literal("profit")
                                .executes(ctx -> sendProfitTrackerMode(ctx.getSource()))
                                .then(literal("toggle").executes(ctx -> toggleProfitTrackerMode(ctx.getSource())))
                                .then(literal("session").executes(ctx -> setProfitTrackerMode(ctx.getSource(), ProfitScope.SESSION)))
                                .then(literal("lifetime").executes(ctx -> setProfitTrackerMode(ctx.getSource(), ProfitScope.LIFETIME)))
                                .then(literal("reset")
                                        .then(literal("session").executes(ctx -> requestProfitTrackerReset(ctx.getSource(), ProfitScope.SESSION)))
                                        .then(literal("lifetime").executes(ctx -> requestProfitTrackerReset(ctx.getSource(), ProfitScope.LIFETIME)))
                                        .then(literal("confirm").executes(ctx -> confirmProfitTrackerReset(ctx.getSource())))
                                        .executes(ctx -> requestProfitTrackerReset(ctx.getSource(), KuudraProfitTrackerManager.get().scope()))))
                        .then(literal("kuudradebug")
                                .then(literal("start")
                                        .executes(ctx -> startKuudraDebugAtPlayer(ctx.getSource()))
                                        .then(argument("x", doubleArg())
                                                .then(argument("y", doubleArg())
                                                        .then(argument("z", doubleArg())
                                                                .executes(ctx -> startKuudraDebug(
                                                                        ctx.getSource(),
                                                                        new Vec3(
                                                                                getDouble(ctx, "x"),
                                                                                getDouble(ctx, "y"),
                                                                                getDouble(ctx, "z")
                                                                        )
                                                                ))))))
                                .then(literal("front")
                                        .executes(ctx -> startKuudraDebugInFront(ctx.getSource(), 13.5d))
                                        .then(argument("distance", doubleArg(1.0d, 80.0d))
                                                .executes(ctx -> startKuudraDebugInFront(ctx.getSource(), getDouble(ctx, "distance")))))
                                .then(literal("stop").executes(ctx -> stopKuudraDebug(ctx.getSource()))))
                         .then(literal("updateconfig").executes(ctx -> updateConfigToDefault(ctx.getSource())))

        );
    }

    private static int startKuudraDebugAtPlayer(@NotNull FabricClientCommandSource source) {
        if (mc.player == null) {
            source.sendFeedback(Component.literal("\u00A7d\u00A7l[IQ] \u00A7r\u00A7cCannot start Kuudra debug without a player."));
            return 0;
        }

        return startKuudraDebug(source, mc.player.position());
    }

    private static int startKuudraDebugInFront(@NotNull FabricClientCommandSource source, double distance) {
        if (mc.player == null) {
            source.sendFeedback(Component.literal("\u00A7d\u00A7l[IQ] \u00A7r\u00A7cCannot start Kuudra debug without a player."));
            return 0;
        }

        Vec3 look = mc.player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0d, look.z);
        if (horizontalLook.lengthSqr() <= 1.0E-4) {
            horizontalLook = new Vec3(0.0d, 0.0d, 1.0d);
        }

        Vec3 pos = mc.player.position().add(horizontalLook.normalize().scale(distance));
        return startKuudraDebug(source, pos);
    }

    private static int startKuudraDebug(@NotNull FabricClientCommandSource source, @NotNull Vec3 kuudraPosition) {
        boolean started = KuudraStateManager.get().beginDebugBossPhase(kuudraPosition);
        if (!started) {
            source.sendFeedback(Component.literal("\u00A7d\u00A7l[IQ] \u00A7r\u00A7cCannot start debug while a real Kuudra run is active."));
            return 0;
        }

        source.sendFeedback(Component.literal(String.format(
                Locale.ROOT,
                "\u00A7d\u00A7l[IQ] \u00A7r\u00A7aKuudra debug started at \u00A7f%.1f %.1f %.1f\u00A7a.",
                kuudraPosition.x,
                kuudraPosition.y,
                kuudraPosition.z
        )));
        source.sendFeedback(Component.literal("\u00A78Use \u00A7f/iq kuudradebug stop\u00A78 to stop the simulation."));
        return 1;
    }

    private static int stopKuudraDebug(@NotNull FabricClientCommandSource source) {
        boolean stopped = KuudraStateManager.get().endDebugBossPhase();
        if (!stopped) {
            source.sendFeedback(Component.literal("\u00A7d\u00A7l[IQ] \u00A7r\u00A7cKuudra debug is not active."));
            return 0;
        }

        source.sendFeedback(Component.literal("\u00A7d\u00A7l[IQ] \u00A7r\u00A7fKuudra debug stopped."));
        return 1;
    }

    private static int updateConfigToDefault(@NotNull FabricClientCommandSource source) {
        mc.schedule(() -> {
            try {
                Path configDir = FabricLoader.getInstance().getConfigDir().resolve("iq");
                
                // Delete config files to force reload from defaults
                Path[] configFiles = {
                    configDir.resolve("crate_priority.json"),
                    configDir.resolve("custom_waypoints.json"),
                    configDir.resolve("new_etherwarp_waypoints.json"),
                    configDir.resolve("new_pearl_waypoints.json")
                };
                
                for (Path file : configFiles) {
                    if (Files.exists(file)) {
                        Files.delete(file);
                    }
                }
                
                // Reload all configs from defaults
                CratePriorityConfigLoader.get().reload();
                PearlWaypointConfigLoader.get().reload();
                SupplyStateManager.get().reloadPileLocations(PileConfigLoader.get().reload());
                EtherwarpConfigLoader.getNewEtherwarpWaypoints().reload();
                EtherwarpConfigLoader.get().reload();
                
                // Reload feature managers if available
                IQModClient client = IQModClient.get();
                if (client != null && client.getFeatureManager() != null) {
                    reloadWaypointFeatures(client);
                }
            } catch (Exception e) {
                source.sendFeedback(Component.literal("§d§l[IQ] §r§cFailed to update configs: " + e.getMessage()));
            }
        });
        source.sendFeedback(Component.literal("§d§l[IQ] §r§fConfig files updated to default successfully."));
        return 1;
    }

    private static int openEtherwarpCategories(@NotNull FabricClientCommandSource source) {
        mc.schedule(() -> mc.setScreen(new KuudraWaypointsScreen(mc.screen)));
        source.sendFeedback(Component.literal("§d§l[IQ] §r§fOpening Etherwarp categories..."));
        return 1;
    }

    private static int openKuudraWaypoints(@NotNull FabricClientCommandSource source) {
        mc.schedule(() -> mc.setScreen(new KuudraWaypointsScreen(mc.screen)));
        source.sendFeedback(Component.literal("\u00A7d\u00A7l[IQ] \u00A7r\u00A7fOpening Kuudra Waypoints..."));
        return 1;
    }

    private static int updateWaypointsFromDefaults(@NotNull FabricClientCommandSource source) {
        mc.schedule(() -> {
            try {
                KuudraWaypointEditorStore store = new KuudraWaypointEditorStore();
                store.load();
                store.updateWaypointsFromBundledDefaults();
                source.sendFeedback(Component.literal("§d§l[IQ] §r§fWaypoints updated. §7Custom waypoints were preserved."));
            } catch (Exception e) {
                source.sendFeedback(Component.literal("§d§l[IQ] §r§cFailed to update waypoints: " + e.getMessage()));
            }
        });
        return 1;
    }

    private static void reloadWaypointFeatures(@NotNull IQModClient client) {
        PearlWaypointFeature pearlFeature = client.getFeatureManager().get(PearlWaypointFeature.class);
        if (pearlFeature != null) {
            pearlFeature.reloadConfig();
        }

        EtherwarpWaypointsFeature etherwarpFeature = client.getFeatureManager().get(EtherwarpWaypointsFeature.class);
        if (etherwarpFeature != null) {
            etherwarpFeature.reloadConfig();
        }

        DpsWaypointFeature dpsFeature = client.getFeatureManager().get(DpsWaypointFeature.class);
        if (dpsFeature != null) {
            dpsFeature.reloadConfig();
        }

        SkipWaypointFeature skipFeature = client.getFeatureManager().get(SkipWaypointFeature.class);
        if (skipFeature != null) {
            skipFeature.reloadConfig();
        }
    }

    private static int sendProfitTrackerMode(@NotNull FabricClientCommandSource source) {
        ProfitScope scope = KuudraProfitTrackerManager.get().scope();
        source.sendFeedback(Component.literal("§d§l[IQ] §r§fKuudra Profit Tracker mode: §e" + scope.name()));
        source.sendFeedback(Component.literal("§8Use §f/iq profit toggle§8, §f/iq profit session§8, §f/iq profit lifetime§8 or edit Display Options inside IQ Config."));
        return 1;
    }

    private static int toggleProfitTrackerMode(@NotNull FabricClientCommandSource source) {
        ProfitScope scope = KuudraProfitTrackerManager.get().toggleScope();
        source.sendFeedback(Component.literal("§d§l[IQ] §r§fKuudra Profit Tracker mode changed to §e" + scope.name() + "§f."));
        return 1;
    }

    private static int setProfitTrackerMode(@NotNull FabricClientCommandSource source, @NotNull ProfitScope scope) {
        KuudraProfitTrackerManager.get().setScope(scope);
        source.sendFeedback(Component.literal("§d§l[IQ] §r§fKuudra Profit Tracker mode set to §e" + scope.name() + "§f."));
        return 1;
    }

    private static int requestProfitTrackerReset(@NotNull FabricClientCommandSource source, @NotNull ProfitScope scope) {
        pendingProfitResetScope = scope;
        pendingProfitResetExpiresAt = System.currentTimeMillis() + PROFIT_RESET_CONFIRM_WINDOW_MS;

        source.sendFeedback(Component.literal("§d§l[IQ] §r§eConfirm reset of Kuudra Profit Tracker §6"
                + scope.name() + "§e data."));
        source.sendFeedback(createProfitResetConfirmButton(scope));
        return 1;
    }

    private static int confirmProfitTrackerReset(@NotNull FabricClientCommandSource source) {
        if (pendingProfitResetScope == null || System.currentTimeMillis() > pendingProfitResetExpiresAt) {
            clearPendingProfitReset();
            source.sendFeedback(Component.literal("§d§l[IQ] §r§cNo pending profit reset. Run §f/iq profit reset§c again first."));
            return 0;
        }

        ProfitScope scope = pendingProfitResetScope;
        clearPendingProfitReset();
        resetProfitTracker(source, scope);
        return 1;
    }

    private static void clearPendingProfitReset() {
        pendingProfitResetScope = null;
        pendingProfitResetExpiresAt = 0L;
    }

    private static void resetProfitTracker(@NotNull FabricClientCommandSource source, @NotNull ProfitScope scope) {
        KuudraProfitTrackerManager manager = KuudraProfitTrackerManager.get();
        if (scope == ProfitScope.SESSION) manager.resetSession();
        else manager.resetLifetime();

        source.sendFeedback(Component.literal("§d§l[IQ] §r§fKuudra Profit Tracker §e" + scope.name() + "§f data reset."));
    }

    private static @NotNull Component createProfitResetConfirmButton(@NotNull ProfitScope scope) {
        return Component.literal("§8[§cConfirm reset " + scope.name() + "§8]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/iq profit reset confirm"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                                "§cClick to reset only " + scope.name() + " profit data."
                        ))));
    }

    private static int sendPersonalBest(@NotNull FabricClientCommandSource source) {
        PersonalBestManager personalBestManager = PersonalBestManager.get();
        if (!personalBestManager.hasPersonalBest()) {
            source.sendFeedback(Component.literal("§d§l[IQ] §r§7No Personal Best recorded yet."));
            return 0;
        }

        Map<KuudraPhase, Long> splits = personalBestManager.getSplitsMillis();

        source.sendFeedback(Component.literal(CHAT_SEPARATOR));
        source.sendFeedback(Component.literal("§d§l[IQ] §aPersonal Best §7> §9"
                + formatSeconds(personalBestManager.getBestTimeMillis())
                + " §8(Tier " + personalBestManager.getTier().getDisplayName() + ")"));
        source.sendFeedback(Component.literal("§aDate: §f" + formatPbDate(personalBestManager.getRecordedAtEpochMillis())
                + " §7- §aHour: §f" + formatPbHour(personalBestManager.getRecordedAtEpochMillis())));
        source.sendFeedback(Component.empty());
        source.sendFeedback(Component.literal("§b§lSplits:"));
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            sendCompactSplitLine(source, splits, phase);
        }
        source.sendFeedback(Component.empty());
        source.sendFeedback(createPersonalBestDetailsButton());
        source.sendFeedback(Component.literal(CHAT_SEPARATOR));
        return 1;
    }

    private static int sendPersonalBestDetails(@NotNull FabricClientCommandSource source) {
        PersonalBestManager personalBestManager = PersonalBestManager.get();
        if (!personalBestManager.hasPersonalBest()) {
            source.sendFeedback(Component.literal("§d§l[IQ] §r§7No Personal Best recorded yet."));
            return 0;
        }

        Map<KuudraPhase, Long> splits = personalBestManager.getSplitsMillis();
        List<net.iqaddons.mod.model.PersonalBest.SupplyTiming> supplyTimings = personalBestManager.getSupplyTimings().stream()
                .sorted(Comparator.comparingInt(net.iqaddons.mod.model.PersonalBest.SupplyTiming::currentSupply))
                .toList();
        List<net.iqaddons.mod.model.PersonalBest.FreshTiming> freshTimings = personalBestManager.getFreshTimings();

        source.sendFeedback(Component.literal(CHAT_SEPARATOR));
        source.sendFeedback(Component.literal("§d§l[IQ] §aPersonal Best §7> §9"
                + formatSeconds(personalBestManager.getBestTimeMillis())
                + " §8(Tier " + personalBestManager.getTier().getDisplayName() + ")"));
        source.sendFeedback(Component.literal("§aDate: §f" + formatPbDate(personalBestManager.getRecordedAtEpochMillis())
                + " §7- §aHour: §f" + formatPbHour(personalBestManager.getRecordedAtEpochMillis())));
        source.sendFeedback(Component.empty());

        source.sendFeedback(Component.literal("§b§lSplits:"));
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            sendCompactSplitLine(source, splits, phase);
        }
        source.sendFeedback(Component.empty());

        source.sendFeedback(Component.literal("§b§lSupply Times §8[§a" + supplyTimings.size() + "§8/§a6§8]"));
        if (supplyTimings.isEmpty()) {
            source.sendFeedback(Component.literal("  §8-"));
        } else {
            for (net.iqaddons.mod.model.PersonalBest.SupplyTiming timing : supplyTimings) {
                source.sendFeedback(Component.literal("  §3" + timing.playerName()
                        + " §8(§7" + timing.currentSupply() + "§8/§76§8) "
                        + getSupplyTimeColor(timing.seconds())
                        + String.format(Locale.ROOT, "%.2fs", timing.seconds())));
            }
        }

        source.sendFeedback(Component.empty());
        source.sendFeedback(Component.literal("§b§lFresh Times §8[§e" + freshTimings.size() + "§8]"));
        if (freshTimings.isEmpty()) {
            source.sendFeedback(Component.literal("  §8-"));
        } else {
            for (net.iqaddons.mod.model.PersonalBest.FreshTiming timing : freshTimings) {
                source.sendFeedback(Component.literal("  §3" + timing.playerName() + " §8- "
                        + FreshersTimerWidget.getTimeColor(timing.seconds()) + "§l"
                        + String.format(Locale.ROOT, "%.2fs", timing.seconds())));
            }
        }

        source.sendFeedback(Component.literal(CHAT_SEPARATOR));
        return 1;
    }

    private static int sendPhaseSplitsPBs(@NotNull FabricClientCommandSource source) {
        PhaseSplitsPBManager pbManager = PhaseSplitsPBManager.get();
        if (!pbManager.hasAnyPB()) {
            source.sendFeedback(Component.literal("§d§l[IQ] §r§7No Phase Split PBs recorded yet. Complete a T5 Infernal run!"));
            return 0;
        }

        Map<KuudraPhase, Long> splits = pbManager.getAllSplits();
        Integer buildFreshCount = pbManager.getBuildPbFreshCount();

        source.sendFeedback(Component.literal(CHAT_SEPARATOR));
        source.sendFeedback(Component.literal("§d§l[IQ] §aSplits Personal Best §8(Tier Infernal)"));

        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            sendPbPhaseLine(source, splits, phase, buildFreshCount);
        }

        source.sendFeedback(Component.literal(CHAT_SEPARATOR));
        return 1;
    }

    private static void sendCompactSplitLine(
            @NotNull FabricClientCommandSource source,
            @NotNull Map<KuudraPhase, Long> splits,
            @NotNull KuudraPhase phase
    ) {
        long millis = splits.getOrDefault(phase, 0L);
        String phaseColor = getPhaseSplitColor(phase);
        String value = millis > 0 ? getKuudraSplitTimeColor(millis, phase) + formatSeconds(millis) : "§8-";
        source.sendFeedback(Component.literal("  " + phaseColor + phase.getDisplayName() + " §8> " + value));
    }

    private static @NotNull Component createPersonalBestDetailsButton() {
        return Component.literal("§8[§bShow detailed info§8]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/iq pb details"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                                "§fClick to see full run info: §bSupply Times §fand §eFresh Times§f."
                        ))));
    }

    private static void sendPbPhaseLine(
            @NotNull FabricClientCommandSource source,
            @NotNull Map<KuudraPhase, Long> splits,
            @NotNull KuudraPhase phase,
            Integer buildFreshCount
    ) {
        long millis = splits.getOrDefault(phase, 0L);
        String phaseColor = getPhaseSplitColor(phase);
        String timeStr = millis > 0 ? getKuudraSplitTimeColor(millis, phase) + formatSeconds(millis) : "§8-";

        String phaseLabel = phase.getDisplayName();
        if (phase == KuudraPhase.BUILD && buildFreshCount != null) {
            phaseLabel += " §8(§a" + buildFreshCount + "§8)";
        }

        source.sendFeedback(Component.literal("  " + phaseColor + phaseLabel + " §8> " + timeStr));
    }

    private static @NotNull String getKuudraSplitTimeColor(long millis, @NotNull KuudraPhase phase) {
        return CustomSplitsWidget.getSplitColor(millis / 1000.0, phase);
    }

    private static @NotNull String getSupplyTimeColor(double seconds) {
        long millis = (long) (seconds * 1000.0);
        if (millis < 19000) return "§f§l";
        if (millis < 20000) return "§5§l";
        if (millis < 22600) return "§9§l";
        if (millis < 25000) return "§a§l";
        if (millis < 28000) return "§2§l";
        if (millis < 32000) return "§e§l";
        return "§c§l";
    }

    private static @NotNull String getPhaseSplitColor(@NotNull KuudraPhase phase) {
        return switch (phase) {
            case SUPPLIES -> net.iqaddons.mod.config.categories.KuudraGeneralConfig.SplitColorConfig.supplies.code();
            case BUILD    -> net.iqaddons.mod.config.categories.KuudraGeneralConfig.SplitColorConfig.build.code();
            case EATEN    -> net.iqaddons.mod.config.categories.KuudraGeneralConfig.SplitColorConfig.eaten.code();
            case STUN     -> net.iqaddons.mod.config.categories.KuudraGeneralConfig.SplitColorConfig.stun.code();
            case DPS      -> net.iqaddons.mod.config.categories.KuudraGeneralConfig.SplitColorConfig.dps.code();
            case SKIP     -> net.iqaddons.mod.config.categories.KuudraGeneralConfig.SplitColorConfig.skip.code();
            case BOSS     -> net.iqaddons.mod.config.categories.KuudraGeneralConfig.SplitColorConfig.boss.code();
            default       -> "§f";
        };
    }

    private static @NotNull String formatSeconds(long millis) {
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0);
    }

    private static @NotNull String formatPbDate(long epochMillis) {
        if (epochMillis <= 0) {
            return "--/--/----";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private static @NotNull String formatPbHour(long epochMillis) {
        if (epochMillis <= 0) {
            return "--:--:--";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }
}

