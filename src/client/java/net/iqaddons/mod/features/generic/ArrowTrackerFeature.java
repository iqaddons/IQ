package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ArrowTrackerFeature extends Feature {

    private String currentArrowType = "Unknown";
    private int currentArrowCount = 0;
    private boolean isOutOfArrows = false;
    private boolean hasTrackedData = false;
    private boolean waitingForQuiverRefresh = false;
    private String lockedArrowType = "Unknown";
    private int lockedArrowCount = 0;

    public ArrowTrackerFeature() {
        super(
                "arrowTracker",
                "Arrow Tracker",
                () -> Configuration.arrowTracker
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(ClientTickEvent.class, this::onClientTick);
    }

    private void onClientTick(@NotNull ClientTickEvent event) {
        if (!isEnabled()) return;
        if (!isOnSkyblock()) return;
        if (mc.player == null) return;

        var inventory = mc.player.getInventory();
        ItemStack slotItem = inventory.getItem(8);
        if (slotItem.isEmpty()) return;

        // Check if it's a feather (minecraft:feather)
        String itemId = slotItem.getItem().getDescriptionId();
        if (!itemId.contains("feather")) return;

        // It's a feather, extract arrow data.
        String itemName = slotItem.getHoverName().getString();
        itemName = itemName.replaceAll("§.", "").trim(); // Remove formatting codes
        if (itemName.isEmpty()) return;

        Integer parsedCount = null;
        ItemLore lore = slotItem.get(DataComponents.LORE);
        if (lore != null) {
            for (var loreLine : lore.lines()) {
                String line = loreLine.getString();
                if (!line.contains("Arrows Remaining:")) continue;
                try {
                    String[] parts = line.split(":");
                    if (parts.length < 2) break;
                    String countStr = parts[1]
                            .replaceAll("§.", "")
                            .replaceAll("[^0-9]", "")
                            .trim();
                    if (!countStr.isEmpty()) {
                        parsedCount = Integer.parseInt(countStr);
                    }
                } catch (Exception e) {
                    log.debug("Could not parse arrow count from lore: {}", line, e);
                }
                break;
            }
        }

        if (parsedCount == null) return;

        if (waitingForQuiverRefresh) {
            boolean slotRefreshed = !itemName.equals(lockedArrowType) || parsedCount != lockedArrowCount;
            if (!slotRefreshed) {
                return;
            }
            waitingForQuiverRefresh = false;
        }

        currentArrowType = itemName;
        currentArrowCount = parsedCount;
        isOutOfArrows = false;
        hasTrackedData = true;
        log.debug("Arrow Tracker: Found {} arrows of type: {}", currentArrowCount, currentArrowType);
    }

    private boolean isOnSkyblock() {
        return ScoreboardUtils.hasTitle(IQConstants.SKYBLOCK_AREA_ID);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        if (!isEnabled()) return;
        if (!isOnSkyblock()) return;

        String message = event.getStrippedMessage();
        String normalizedMessage = message.toLowerCase();

        // Check for "QUIVER! You only have XXXX" message
        if (message.startsWith("QUIVER! You only have")) {
            extractAndNotifyArrowCount(message, false);
        }
        // Check for "Your quiver is now completely empty!" message
        else if (normalizedMessage.contains("your quiver is now completely empty")) {
            extractAndNotifyArrowCount(message, true);
        }
    }

    private void extractAndNotifyArrowCount(@NotNull String message, boolean outOfArrows) {
        try {
            // Remove formatting codes for parsing
            String cleanMessage = message.replaceAll("§.", "");
            String[] parts = cleanMessage.split(" ");

            if (outOfArrows) {
                // "Your quiver is now completely empty!"
                lockedArrowType = currentArrowType;
                lockedArrowCount = currentArrowCount;
                currentArrowCount = 0;
                isOutOfArrows = true;
                hasTrackedData = true;
                waitingForQuiverRefresh = true;

                if (Configuration.arrowTrackerNotifications) {
                    MessageUtil.showTitle(
                            "§cYou run out of arrows",
                            "",
                            0, 40, 10
                    );
                    playAlertSound();
                    MessageUtil.PARTY.sendMessage("[IQ] Oh! I ran out of arrows mid run, unlucky :(");
                }

                log.info("Arrow Tracker: Out of arrows");
            } else {
                // "QUIVER! You only have XXXX {ARROW_TYPE}"
                // parts: [0]="QUIVER!" [1]="You" [2]="only" [3]="have" [4]=XXXX [5+]={ARROW_TYPE}
                if (parts.length >= 6) {
                    try {
                        int count = Integer.parseInt(parts[4]);
                        String arrowType = String.join(" ", java.util.Arrays.copyOfRange(parts, 5, parts.length));
                        currentArrowType = arrowType;
                        currentArrowCount = count;
                        isOutOfArrows = false;
                        hasTrackedData = true;
                        waitingForQuiverRefresh = false;

                        if (Configuration.arrowTrackerNotifications) {
                            MessageUtil.showTitle(
                                    "§eYou are running out of arrows",
                                    "",
                                    0, 40, 10
                            );
                            playAlertSound();
                        }
                        log.info("Arrow Tracker: Running out of arrows - {} ({})", count, arrowType);
                    } catch (NumberFormatException ignored) {
                        log.debug("Could not parse arrow count from: {}", message);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting arrow count from message: {}", message, e);
        }
    }

    private void playAlertSound() {
        mc.execute(() -> {
            if (mc.player != null && mc.level != null) {
                mc.level.playSound(
                        mc.player, mc.player.blockPosition(),
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),
                        net.minecraft.sounds.SoundSource.MASTER,
                        2.0f, 1.2f
                );
            }
        });
    }

    public String getCurrentArrowType() {
        return currentArrowType;
    }

    public int getCurrentArrowCount() {
        return currentArrowCount;
    }

    public boolean isOutOfArrows() {
        return isOutOfArrows;
    }

    public boolean hasTrackedData() {
        return hasTrackedData;
    }
}
