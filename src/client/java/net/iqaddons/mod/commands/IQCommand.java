package net.iqaddons.mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.iqaddons.mod.IQKeyBindings;
import net.minecraft.client.MinecraftClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class IQCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("iq")
                        .executes(ctx -> {
                            IQKeyBindings.openConfigScreen(MinecraftClient.getInstance());
                            return 1;
                        })
        );
    }
}
