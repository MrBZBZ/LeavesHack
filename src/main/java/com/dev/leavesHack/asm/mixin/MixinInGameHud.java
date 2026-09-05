package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Gui.class)
public class MixinInGameHud {
    @ModifyArg(method = "extractItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 1), index = 2)
    private int selectedSlotX(int x) {
        double originalX = GlobalSetting.INSTANCE.clientSwitch.get() ? mc.getWindow().getGuiScaledWidth() / 2D - 91 - 1 + InventoryUtil.serverSlot * 20 : x;
        return (int) originalX;
    }
}
