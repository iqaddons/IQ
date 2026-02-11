package net.iqaddons.mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.iqaddons.mod.IQKeyBindings;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.iqaddons.mod.manager.PersonalBestManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

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
                        .then(literal("resetchests").executes(ctx -> {
                            ChestCounterManager.get().reset();
                            ctx.getSource().sendFeedback(Text.literal("§d§l[IQ] §r§fChest counter reset."));
                            return 1;
                        }))
                        .then(literal("pb").executes(ctx -> sendPersonalBest(ctx.getSource())))
        );
    }

    private static int sendPersonalBest(@NotNull FabricClientCommandSource source) {
        PersonalBestManager personalBestManager = PersonalBestManager.get();
        if (!personalBestManager.hasPersonalBest()) {
            source.sendFeedback(Text.literal("§d§l[IQ] §r§7No Personal Best recorded yet."));
            return 1;
        }

        source.sendFeedback(Text.literal("§d§l[IQ] §r§aYour Kuudra PB: §f" + formatSeconds(personalBestManager.getBestTimeMillis())));

        Map<KuudraPhase, Long> splits = personalBestManager.getSplitsMillis();
        source.sendFeedback(Text.literal("§d§l[IQ] §r§bSplits:"));
        for (KuudraPhase phase : KuudraPhase.RUN_PHASES) {
            long millis = splits.getOrDefault(phase, 0L);
            source.sendFeedback(Text.literal("§8- §f" + phase.getDisplayName() + ": §b" + formatSeconds(millis)));
        }

        return 1;
    }

    private static @NotNull String formatSeconds(long millis) {
        return String.format("%.2fs", millis / 1000.0);
    }
}