package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockPosX;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;

public class LegitNoFall extends Module {
    public static LegitNoFall INSTANCE;
    public LegitNoFall() {
        super(LeavesHack.CATEGORY, "LegitNoFall", "");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> checkDown = sgGeneral.add(new IntSetting.Builder()
            .name("checkDown")
            .defaultValue(1)
            .min(0)
            .sliderMax(3)
            .build()
    );
    private final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
            .name("inventorySwap")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> offSet = sgGeneral.add(new DoubleSetting.Builder()
            .name("offSet")
            .defaultValue(0.3)
            .min(0)
            .sliderMax(1)
            .build()
    );
    private boolean hasPlacedWater = false;
    private BlockPos lastPos = null;
    @Override
    public void onActivate() {
        hasPlacedWater = false;
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.world.getRegistryKey() == World.NETHER) return;
        int old = mc.player.getInventory().selectedSlot;
        int water = hasPlacedWater ? findItem(Items.BUCKET) : findItem(Items.WATER_BUCKET);
        if (water != -1) {
            if (hasPlacedWater && lastPos != null) {
                doSwap(water);
                Color color = new Color(70, 177, 229, 80);
                event.renderer.box(lastPos,color,color, ShapeMode.Both,0);
                Rotation.snapAt(lastPos.toCenterPos());
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, Rotation.getRotation(lastPos.toCenterPos())[0], Rotation.getRotation(lastPos.toCenterPos())[1]));
                if (inventorySwap.get()) {
                    doSwap(water);
                } else {
                    doSwap(old);
                }
                Rotation.snapBack();
                hasPlacedWater = false;
            } else if (!hasPlacedWater) {
                BlockPos pos = mc.player.getBlockPos().down(checkDown.get());
                double[] xzOffset = new double[]{offSet.get(), -offSet.get()};
                for (double x : xzOffset){
                    for (double z : xzOffset){
                        BlockPos offSetPos = new BlockPosX(pos.getX() + x, pos.getY(), pos.getZ() + z);
                        if (checkFalling() && !mc.world.isAir(offSetPos) && !mc.world.getBlockState(offSetPos).isReplaceable()) {
                            Direction side = BlockUtil.getPlaceSide(pos.up(), null);
                            if (side != null && !behindWall(offSetPos.up())) {
                                Color color = new Color(70, 177, 229, 80);
                                event.renderer.box(offSetPos.up(), color, color, ShapeMode.Both, 0);
                                doSwap(water);
                                Rotation.snapAt(offSetPos.up().toCenterPos());
                                lastPos = offSetPos.up();
                                mc.player.swingHand(Hand.MAIN_HAND);
                                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 1, Rotation.getRotation(offSetPos.up().toCenterPos())[0], Rotation.getRotation(offSetPos.up().toCenterPos())[1]));
                                if (inventorySwap.get()) {
                                    doSwap(water);
                                } else {
                                    doSwap(old);
                                }
                                Rotation.snapBack();
                                hasPlacedWater = true;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    public boolean behindWall(BlockPos pos) {
        Vec3d testVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 2 * 0.85, pos.getZ() + 0.5);
        HitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), testVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (result == null || result.getType() == HitResult.Type.MISS) return false;
        return false;
    }
    private boolean checkFalling() {
        return mc.player.fallDistance > mc.player.getSafeFallDistance() && !mc.player.isOnGround() && !mc.player.isFallFlying();
    }
    private int findItem(Item item) {
        if (inventorySwap.get()) {
            return InventoryUtil.findItemInventorySlot(item);
        } else {
            return InventoryUtil.findItem(item);
        }
    }
    private void doSwap(int slot) {
        if (!inventorySwap.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }
}
