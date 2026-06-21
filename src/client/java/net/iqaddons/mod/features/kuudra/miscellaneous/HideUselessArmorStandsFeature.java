package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ArmorStandRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.BoundingBox2D;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class HideUselessArmorStandsFeature extends KuudraFeature {

    public HideUselessArmorStandsFeature() {
        super("hideUselessArmorStands",
                "Hide Useless ArmorStand",
                () -> KuudraGeneralConfig.hideUselessArmorStands
                        && KuudraGeneralConfig.HideUselessArmorStandsConfig.anyEnabled(),
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ArmorStandRenderEvent.class, this::onArmorStandRender);
    }

    private void onArmorStandRender(ArmorStandRenderEvent event) {
        if (!isActive()) return;

        var state = event.getRenderState();
        if (state == null) return;
        if (shouldHide(state)) {
            event.setCancelled(true);
        }
    }

    private boolean shouldHide(ArmorStandRenderState state) {
        EnumSet<HiddenArmorStandType> selected = selectedTypes();
        if (selected.isEmpty()) return false;

        for (HiddenArmorStandType type : selected) {
            if (type == HiddenArmorStandType.OTHERS) continue;
            if (type.getArea() != null && type.getArea().contains(state.x, state.z)) {
                return state.nameTag == null;
            }
        }

        if (!selected.contains(HiddenArmorStandType.OTHERS)) return false;
        if (state.nameTag != null) return false;

        for (HiddenArmorStandType type : HiddenArmorStandType.values()) {
            if (type == HiddenArmorStandType.OTHERS || type.getArea() == null) {
                continue;
            }

            if (type.getArea().contains(state.x, state.z)) {
                return false;
            }
        }

        return true;
    }

    private @NotNull EnumSet<HiddenArmorStandType> selectedTypes() {
        EnumSet<HiddenArmorStandType> selected = EnumSet.noneOf(HiddenArmorStandType.class);
        if (KuudraGeneralConfig.HideUselessArmorStandsConfig.build) {
            selected.add(HiddenArmorStandType.BUILD);
        }
        if (KuudraGeneralConfig.HideUselessArmorStandsConfig.rightCannon) {
            selected.add(HiddenArmorStandType.RIGHT_CANNON);
        }
        if (KuudraGeneralConfig.HideUselessArmorStandsConfig.leftCannon) {
            selected.add(HiddenArmorStandType.LEFT_CANNON);
        }
        if (KuudraGeneralConfig.HideUselessArmorStandsConfig.shop) {
            selected.add(HiddenArmorStandType.SHOP);
        }
        if (KuudraGeneralConfig.HideUselessArmorStandsConfig.others) {
            selected.add(HiddenArmorStandType.OTHERS);
        }

        return selected;
    }

    @Getter
    @RequiredArgsConstructor
    public enum HiddenArmorStandType {
        BUILD(BoundingBox2D.fromCorners(-113, -96, -91, -116)),
        RIGHT_CANNON(BoundingBox2D.fromCorners(-126, -115, -132, -108)),
        LEFT_CANNON(BoundingBox2D.fromCorners(-67, -105, -72, -100)),
        SHOP(BoundingBox2D.fromCorners(-98, -129, -93, -132)),
        OTHERS;

        private final BoundingBox2D area;

        HiddenArmorStandType() {
            this.area = null;
        }
    }
}
