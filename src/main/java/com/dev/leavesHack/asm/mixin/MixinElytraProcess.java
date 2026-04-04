package com.dev.leavesHack.asm.mixin;

import baritone.process.ElytraProcess;
import com.dev.leavesHack.modules.FireworkElytraFly;
import com.dev.leavesHack.modules.GlobalSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ElytraProcess.class)
public class MixinElytraProcess {
    @Inject(method = "a()Z", at = @At("HEAD"), cancellable = true)
    private void shouldLandForSafety(CallbackInfoReturnable<Boolean> cir){
        if (FireworkElytraFly.INSTANCE.isActive() && FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.GrimDurability && GlobalSetting.INSTANCE.baritone.get()){
            if (mc.player.isOnGround()) cir.setReturnValue(true);
            int fireworks = 0;
            if (FireworkElytraFly.INSTANCE.inventorySwap.get()) {
                for (int i = 0; i < 45; ++i) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() == Items.FIREWORK_ROCKET) fireworks = fireworks + stack.getCount();
                }
            } else {
                for (int i = 0; i < 9; ++i) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() == Items.FIREWORK_ROCKET) fireworks = fireworks + stack.getCount();
                }
            }
            if (fireworks <= GlobalSetting.INSTANCE.minFireworks.get()) {
                cir.setReturnValue(true);
                return;
            }
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getMaxDamage() - mc.player.getEquippedStack(EquipmentSlot.CHEST).getDamage() <= GlobalSetting.INSTANCE.elytraMinDamage.get()) {
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(false);
        }
    }
}
