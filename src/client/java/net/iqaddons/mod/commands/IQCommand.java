package net.iqaddons.mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.iqaddons.mod.IQKeyBindings;
import net.iqaddons.mod.hud.HudManager;
import net.minecraft.client.MinecraftClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class IQCommand {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
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
        );
    }
}
