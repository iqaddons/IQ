package net.iqaddons.mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.IQKeyBindings;
import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.config.loader.PileConfigLoader;
import net.iqaddons.mod.config.loader.WaypointConfigLoader;
import net.iqaddons.mod.config.loader.CratePriorityConfigLoader;
import net.iqaddons.mod.features.kuudra.waypoints.EtherwarpHelperFeature;
import net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.iqaddons.mod.manager.EtherwarpCategoryToggleManager;
import net.iqaddons.mod.manager.PersonalBestManager;
import net.iqaddons.mod.manager.PhaseSplitsPBManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.manager.pricing.KuudraProfitTrackerManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.spot.PileLocation;
import net.iqaddons.mod.model.profit.ProfitScope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
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

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class IQCommand {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void register(@NotNull CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("iq")
                        .executes(ctx -> {
                            mc.send(() -> IQKeyBindings.openConfigScreen(mc));
                            return 1;
                        })
                        .then(literal("hud").executes(ctx -> {
                            mc.send(() -> HudManager.get().openEditor());
                            return 1;
                        }))
                        .then(literal("etherwarps").executes(ctx -> openEtherwarpCategories(ctx.getSource())))
                        .then(literal("etherwarp").executes(ctx -> openEtherwarpCategories(ctx.getSource())))
                        .then(literal("reload").executes(ctx -> {
                            mc.send(() -> {
                                List<PileLocation> pileLocations = PileConfigLoader.get().reload();
                                SupplyStateManager.get().reloadPileLocations(pileLocations);
                                CratePriorityConfigLoader.get().reload();

                                IQModClient client = IQModClient.get();
                                if (client != null && client.getFeatureManager() != null) {
                                    PearlWaypointFeature pearlFeature = client.getFeatureManager().get(PearlWaypointFeature.class);
                                    if (pearlFeature != null) {
                                        pearlFeature.reloadConfig();
                                    } else {
                                        WaypointConfigLoader.get().reload();
                                    }

                                    EtherwarpHelperFeature etherwarpFeature = client.getFeatureManager().get(EtherwarpHelperFeature.class);
                                    if (etherwarpFeature != null) {
                                        etherwarpFeature.reloadConfig();
                                    } else {
                                        List<EtherwarpCategory> categories = EtherwarpConfigLoader.get().reload();
                                        EtherwarpCategoryToggleManager.get().syncWithCategories(categories);
                                    }
                                    return;
                                }

                                WaypointConfigLoader.get().reload();
                                List<EtherwarpCategory> categories = EtherwarpConfigLoader.get().reload();
                                EtherwarpCategoryToggleManager.get().syncWithCategories(categories);
                                CratePriorityConfigLoader.get().reload();
                            });
                            ctx.getSource().sendFeedback(Text.literal("§d§l[IQ] §r§fWaypoints and configs reloaded."));
                            return 1;
                        }))
                          .then(literal("pearls").executes(ctx -> {
                              try {
                                  Path configDir = FabricLoader.getInstance().getConfigDir().resolve("iq");
                                  Files.createDirectories(configDir);
                                  Util.getOperatingSystem().open(configDir.toFile());
                                  ctx.getSource().sendFeedback(Text.literal("§d§l[IQ] §r§fOpening pearl waypoints config folder..."));
                              } catch (Exception e) {
                                  ctx.getSource().sendFeedback(Text.literal("§d§l[IQ] §r§cFailed to open config folder."));
                              }
                              return 1;
                          }))
                        .then(literal("discord").executes(ctx -> {
                            Util.getOperatingSystem().open("https://discord.gg/HdhXhCWcW9");
                            ctx.getSource().sendFeedback(Text.literal("§d§l[IQ] §r§fOpening IQ Discord invite..."));
                            return 1;
                        }))
                        .then(literal("resetchests").executes(ctx -> {
                            ChestCounterManager.get().reset();
                            ctx.getSource().sendFeedback(Text.literal("§d§l[IQ] §r§fChest counter reseted."));
                            return 1;
                        }))
                        .then(literal("pb").executes(ctx -> sendPersonalBest(ctx.getSource())))
                        .then(literal("pbs").executes(ctx -> sendPhaseSplitsPBs(ctx.getSource())))
                        .then(literal("profit")
                                .executes(ctx -> sendProfitTrackerMode(ctx.getSource()))
                                .then(literal("toggle").executes(ctx -> toggleProfitTrackerMode(ctx.getSource())))
                                .then(literal("session").executes(ctx -> setProfitTrackerMode(ctx.getSource(), ProfitScope.SESSION)))
                                .then(literal("lifetime").executes(ctx -> setProfitTrackerMode(ctx.getSource(), ProfitScope.LIFETIME)))
                                .then(literal("reset")
                                        .then(literal("session").executes(ctx -> resetProfitTracker(ctx.getSource(), ProfitScope.SESSION)))
                                        .then(literal("lifetime").executes(ctx -> resetProfitTracker(ctx.getSource(), ProfitScope.LIFETIME)))
                                        .executes(ctx -> resetProfitTrackerAll(ctx.getSource()))))

        );
    }

    private static int openEtherwarpCategories(@NotNull FabricClientCommandSource source) {
        mc.send(KuudraGeneralConfig.openEtherwarpCategorySelector);
        source.sendFeedback(Text.literal("§d§l[IQ] §r§fOpening Etherwarp categories..."));
        return 1;
    }

    private static int sendProfitTrackerMode(@NotNull FabricClientCommandSource source) {
        ProfitScope scope = KuudraProfitTrackerManager.get().scope();
        source.sendFeedback(Text.literal("§d§l[IQ] §r§fKuudra Profit Tracker mode: §e" + scope.name()));
        source.sendFeedback(Text.literal("§8Use §f/iq profit toggle§8, §f/iq profit session§8 or §f/iq profit lifetime§8."));
        return 1;
    }

    private static int toggleProfitTrackerMode(@NotNull FabricClientCommandSource source) {
        ProfitScope scope = KuudraProfitTrackerManager.get().toggleScope();
        source.sendFeedback(Text.literal("§d§l[IQ] §r§fKuudra Profit Tracker mode changed to §e" + scope.name() + "§f."));
        return 1;
    }

    private static int setProfitTrackerMode(@NotNull FabricClientCommandSource source, @NotNull ProfitScope scope) {
        KuudraProfitTrackerManager.get().setScope(scope);
        source.sendFeedback(Text.literal("§d§l[IQ] §r§fKuudra Profit Tracker mode set to §e" + scope.name() + "§f."));
        return 1;
    }

    private static int resetProfitTracker(@NotNull FabricClientCommandSource source, @NotNull ProfitScope scope) {
        KuudraProfitTrackerManager manager = KuudraProfitTrackerManager.get();
        if (scope == ProfitScope.SESSION) manager.resetSession();
        else manager.resetLifetime();

        source.sendFeedback(Text.literal("§d§l[IQ] §r§fKuudra Profit Tracker §e" + scope.name() + "§f data reset."));
        return 1;
    }

    private static int resetProfitTrackerAll(@NotNull FabricClientCommandSource source) {
        KuudraProfitTrackerManager.get().resetAll();
        source.sendFeedback(Text.literal("§d§l[IQ] §r§fKuudra Profit Tracker §eALL§f data reset."));
        return 1;
    }

    private static int sendPersonalBest(@NotNull FabricClientCommandSource source) {
        PersonalBestManager personalBestManager = PersonalBestManager.get();
        if (!personalBestManager.hasPersonalBest()) {
            source.sendFeedback(Text.literal("§d§l[IQ] §r§7No Personal Best recorded yet."));
            return 0;
        }

        Map<KuudraPhase, Long> splits = personalBestManager.getSplitsMillis();
        List<net.iqaddons.mod.model.PersonalBest.SupplyTiming> supplyTimings = personalBestManager.getSupplyTimings().stream()
                .sorted(Comparator.comparingInt(net.iqaddons.mod.model.PersonalBest.SupplyTiming::currentSupply))
                .toList();
        List<net.iqaddons.mod.model.PersonalBest.FreshTiming> freshTimings = personalBestManager.getFreshTimings();

        final String sep = "§8§m──────────────────────";

        source.sendFeedback(Text.literal(sep));
        source.sendFeedback(Text.literal("  §d§l[IQ] §bPersonal Best §8> §3"
                + formatSeconds(personalBestManager.getBestTimeMillis())
                + " §8(Tier §b" + personalBestManager.getTier().getDisplayName() + "§8)"));
        source.sendFeedback(Text.literal("  §bData: §3" + formatPbDate(personalBestManager.getRecordedAtEpochMillis())
                + " §8- §bHour: §3" + formatPbHour(personalBestManager.getRecordedAtEpochMillis())));
        source.sendFeedback(Text.literal(sep));

        source.sendFeedback(Text.literal("  §bSplits:"));
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            long millis = splits.getOrDefault(phase, 0L);
            String phaseColor = getPhaseSplitColor(phase);
            String value = millis > 0 ? phaseColor + formatSeconds(millis) : "§8-";
            source.sendFeedback(Text.literal("    " + phaseColor + phase.getDisplayName() + " §8> " + value));
        }

        source.sendFeedback(Text.literal("  §bSupplies §8[§3" + supplyTimings.size() + "§8]:"));
        if (supplyTimings.isEmpty()) {
            source.sendFeedback(Text.literal("    §8-"));
        } else {
            for (net.iqaddons.mod.model.PersonalBest.SupplyTiming timing : supplyTimings) {
                source.sendFeedback(Text.literal("    " + timing.playerName()
                        + " §8(§b" + timing.currentSupply() + "§8/§36§8) §3"
                        + String.format(Locale.ROOT, "%.2fs", timing.seconds())));
            }
        }

        source.sendFeedback(Text.literal("  §bFreshs §8[§3" + freshTimings.size() + "§8]:"));
        if (freshTimings.isEmpty()) {
            source.sendFeedback(Text.literal("    §8-"));
        } else {
            for (net.iqaddons.mod.model.PersonalBest.FreshTiming timing : freshTimings) {
                source.sendFeedback(Text.literal("    " + timing.playerName() + " §8- §3"
                        + String.format(Locale.ROOT, "%.2fs", timing.seconds())));
            }
        }

        source.sendFeedback(Text.literal(sep));
        return 1;
    }

    private static int sendPhaseSplitsPBs(@NotNull FabricClientCommandSource source) {
        PhaseSplitsPBManager pbManager = PhaseSplitsPBManager.get();
        if (!pbManager.hasAnyPB()) {
            source.sendFeedback(Text.literal("§d§l[IQ] §r§7No Phase Split PBs recorded yet. Complete a T5 Infernal run!"));
            return 0;
        }

        Map<KuudraPhase, Long> splits = pbManager.getAllSplits();
        Integer buildFreshCount = pbManager.getBuildPbFreshCount();
        final String sep = "§8§m──────────────────────";

        source.sendFeedback(Text.literal(sep));
        source.sendFeedback(Text.literal("  §d§l[IQ]  §b§lPhase Split PBs  §8(§3T5 Infernal§8)"));
        source.sendFeedback(Text.literal(sep));

        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            sendPbPhaseLine(source, splits, phase, buildFreshCount);
        }

        source.sendFeedback(Text.literal(sep));
        return 1;
    }

    private static void sendPbPhaseLine(
            @NotNull FabricClientCommandSource source,
            @NotNull Map<KuudraPhase, Long> splits,
            @NotNull KuudraPhase phase,
            Integer buildFreshCount
    ) {
        long millis = splits.getOrDefault(phase, 0L);
        String phaseColor = getPhaseSplitColor(phase);
        String timeStr = millis > 0 ? phaseColor + formatSeconds(millis) : "§8-";

        String phaseLabel = phase.getDisplayName();
        if (phase == KuudraPhase.BUILD && buildFreshCount != null) {
            phaseLabel += " §8(§b" + buildFreshCount + "§8)";
        }

        source.sendFeedback(Text.literal("    §8▸ " + phaseColor + phaseLabel + " §8» " + timeStr));
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
