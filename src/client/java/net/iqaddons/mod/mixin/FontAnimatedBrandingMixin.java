package net.iqaddons.mod.mixin;

import net.iqaddons.mod.render.text.AnimatedBrandingText;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontAnimatedBrandingMixin {
    @Shadow
    public abstract Font.PreparedText prepareText(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean inverseDepth, int light);

    @Inject(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), cancellable = true)
    private void iq$animateRawText(String text, float x, float y, int color, boolean shadow, int light, CallbackInfoReturnable<Font.PreparedText> cir) {
        if (!AnimatedBrandingText.containsNick(text)) return;
        cir.setReturnValue(prepareText(AnimatedBrandingText.transform(text, System.currentTimeMillis(), false), x, y, color, shadow, false, light));
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence iq$animateStyledText(FormattedCharSequence text) {
        return AnimatedBrandingText.transform(text, System.currentTimeMillis(), false);
    }
}
