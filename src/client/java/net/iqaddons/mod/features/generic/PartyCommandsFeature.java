package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.ChestCounterManager;
import net.iqaddons.mod.manager.pricing.KuudraProfitTrackerManager;
import net.iqaddons.mod.model.profit.ProfitData;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.PingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PartyCommandsFeature extends Feature {

    private static final Pattern PARTY_CHAT_PATTERN = Pattern.compile("^Party > (?:\\[[^]]+] )?([A-Za-z0-9_]+):\\s*(.+)$");
    private static final Map<Integer, String> KUUDRA_TIER_MAP = Map.of(
            1, "KUUDRA_NORMAL",
            2, "KUUDRA_HOT",
            3, "KUUDRA_BURNING",
            4, "KUUDRA_FIERY",
            5, "KUUDRA_INFERNAL"
    );

    public PartyCommandsFeature() {
        super("partyCommands", "Party Commands", () -> Configuration.PartyCommands.enable);
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        Matcher matcher = PARTY_CHAT_PATTERN.matcher(event.getStrippedMessage());
        if (!matcher.matches()) return;

        String sender = matcher.group(1);
        String content = matcher.group(2).trim();
        if (!content.startsWith("!")) return;

        String[] parts = content.split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);

        switch (command) {
            case "!warp", "!w", "!wp" -> runPartyCommand(Configuration.PartyCommands.warpCommand, "party warp");
            case "!pt", "!ptme", "!transfer" -> runPartyCommand(Configuration.PartyCommands.transferCommand, "party transfer " + sender);
            case "!ping" -> sendPing();
            case "!allinvite", "!allinv", "!invites" -> runPartyCommand(Configuration.PartyCommands.allInviteCommand, "party settings allinvite");
            case "!tps" -> sendTps();
            case "!promote" -> runPartyCommand(Configuration.PartyCommands.promoteCommand, "party promote " + sender);
            case "!kick" -> handleKick(parts);
            case "!t1", "!t2", "!t3", "!t4", "!t5" -> handleKuudraTier(command);
            case "!chests" -> sendChestProgress();
            case "!runs" -> sendRuns(parts);
            case "!profit" -> sendProfit();
            default -> {}
        }
    }

    private void handleKick(@NotNull String[] parts) {
        if (!Configuration.PartyCommands.partyCommandKick || parts.length < 2) return;
        runRawCommand("party kick " + parts[1]);
    }

    private void handleKuudraTier(@NotNull String command) {
        if (!Configuration.PartyCommands.kuudraCommand) return;
        int tier = Integer.parseInt(command.substring(2));
        String kuudraTier = KUUDRA_TIER_MAP.get(tier);
        if (kuudraTier == null) {
            MessageUtil.ERROR.sendMessage("Invalid Kuudra tier: " + tier);
            return;
        }

        runRawCommand("joininstance " + kuudraTier);
    }

    private void runPartyCommand(boolean enabled, @NotNull String command) {
        if (!enabled) return;
        runRawCommand(command);
    }

    private void runRawCommand(@NotNull String command) {
        if (mc.player == null || mc.player.networkHandler == null) return;
        mc.player.networkHandler.sendChatCommand(command);
    }

    private void sendPing() {
        if (!Configuration.PartyCommands.partyCommandPing || mc.player == null || mc.player.networkHandler == null) return;

        var averagePing = PingUtils.getAveragePing();
        MessageUtil.PARTY.sendMessage(String.format("[IQ] %,dms", averagePing.toMillis()));
    }

    private void sendTps() {
        if (!Configuration.PartyCommands.partyCommandTps || mc.world == null) return;

        float tps = mc.world.getTickManager().getTickRate();
        MessageUtil.PARTY.sendMessage(String.format(Locale.ROOT, "[IQ] %.1f", tps));
    }

    private void sendChestProgress() {
        if (!Configuration.PartyCommands.partyCommandChests) return;

        int current = ChestCounterManager.get().getChests();
        int limit = ChestCounterManager.MAX_CHESTS;
        MessageUtil.PARTY.sendMessage("[IQ] I am currently at " + current + "/" + limit + " of my chest limit.");
    }

    private void sendRuns(@NotNull String @NotNull [] parts) {
        if (!Configuration.PartyCommands.partyCommandRuns) return;

        if (parts.length > 1 && mc.player != null
                && !parts[1].equalsIgnoreCase(mc.player.getGameProfile().name())) {
            return;
        }

        ProfitData data = KuudraProfitTrackerManager.get().lifetime();
        MessageUtil.PARTY.sendMessage(String.format(
                "[IQ] Runs: %d (F:%d) | Avg: %.2fs",
                data.runs, data.failedRuns, data.averageRunMillis() / 1000.0
        ));
    }

    private void sendProfit() {
        if (!Configuration.PartyCommands.partyCommandProfit) return;

        ProfitData data = KuudraProfitTrackerManager.get().current();
        MessageUtil.PARTY.sendMessage(String.format(
                "[IQ] Profit: %s | Rate: %s/h | Runs: %d | Avg: %.2fs",
                formatCoins(data.profit),
                formatCoins(Math.max(0, data.hourlyRateCoins())),
                data.runs,
                data.averageRunMillis() / 1000.0
        ));
    }

    private @NotNull String formatCoins(long coins) {
        long abs = Math.abs(coins);
        String prefix = coins < 0 ? "-" : "";
        if (abs >= 1_000_000_000L) return String.format(Locale.ROOT, "%s%.2fb", prefix, abs / 1_000_000_000d);
        if (abs >= 1_000_000L) return String.format(Locale.ROOT, "%s%.2fm", prefix, abs / 1_000_000d);
        if (abs >= 1_000L) return String.format(Locale.ROOT, "%s%.1fk", prefix, abs / 1_000d);
        return prefix + abs;
    }
}
