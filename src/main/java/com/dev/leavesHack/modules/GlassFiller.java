package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class GlassFiller extends Module {
    public static GlassFiller INSTANCE;
    public GlassFiller() {
        super(LeavesHack.LEAVES_COMBAT, "GlassFiller", "玻璃塞脚");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder()
            .name("TargetRange")
            .description("目标距离")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .build()
    );
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("PlaceDelay")
            .description("放置延迟")
            .defaultValue(50)
            .sliderRange(0, 500)
            .build()
    );
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("PlaceRange")
            .description("放置距离")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
            .name("UsingPause")
            .description("使用暂停")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> onlyMain = sgGeneral.add(new BoolSetting.Builder()
            .name("OnlyMain")
            .description("仅主手暂停")
            .defaultValue(true)
            .visible(usingPause::get)
            .build()
    );
    private final Setting<Boolean> skipPhased = sgGeneral.add(new BoolSetting.Builder()
            .name("SkipPhased")
            .description("忽略穿墙玩家")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate")
            .description("放置转头")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
            .name("InventorySwap")
            .description("静默背包切换")
            .defaultValue(true)
            .build()
    );
    private Timer glassTimer = new Timer();
    private PlayerEntity target;
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        update();
    }

    public boolean isPhased(PlayerEntity player) {
        return mc.world.canCollide(player,player.getBoundingBox());
    }
    public boolean checkPause(boolean onlyMain) {
        return mc.options.useKey.isPressed() && (!onlyMain || mc.player.getActiveHand() == Hand.MAIN_HAND);
    }

    public void update() {
        if (usingPause.get() && checkPause(onlyMain.get())) return;
        target = CombatUtil.getClosestEnemy(targetRange.get());
        if (target == null) {
            return;
        }
        if (skipPhased.get() && isPhased(target)) {
            return;
        }
        if (AutoCrystal.INSTANCE.crystalPos != null || AutoAnchor.INSTANCE.currentPos != null) {
            return;
        }
        Box boundingBox = target.getBoundingBox().shrink(0.01, 0.1, 0.01);
        int feetY = target.getBlockPos().getY();
        int minX = (int) Math.floor(boundingBox.minX);
        int maxX = (int) Math.floor(boundingBox.maxX);
        int minZ = (int) Math.floor(boundingBox.minZ);
        int maxZ = (int) Math.floor(boundingBox.maxZ);
        int slot = inventorySwap.get()?InventoryUtil.findItemInventorySlot(Items.GLASS):InventoryUtil.findItem(Items.GLASS);
        int old = mc.player.getInventory().getSelectedSlot();
        if(slot==-1) return;
        if (!glassTimer.passed((long) placeDelay.get())) return;
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos feetPos = new BlockPos(x, feetY, z);
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (Math.abs(offsetX) + Math.abs(offsetZ) != 1) {
                            continue;
                        }
                        BlockPos pos = feetPos.add(offsetX, 0, offsetZ);
                        if (!BlockUtil.clientCanPlace(pos, false)) continue;
                        Direction side = BlockUtil.getPlaceSide(pos, null);
                        if (side == null) continue;
                        if (notInPlaceBlockRange(pos)) continue;
                        doSwap(slot);
                        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
                        if (rotate.get()) {
                            Rotation.snapAt(directionVec);
                        }
                        // In 1.21.11, clickBlock already adds to placeList internally
                        BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false);
                        glassTimer.reset();
                        if (rotate.get()) {
                            Rotation.snapBack();
                        }
                        if (inventorySwap.get()) {
                            doSwap(slot);
                        } else {
                            doSwap(old);
                        }
                    }
                }
            }
        }
    }
    private boolean notInPlaceBlockRange(BlockPos pos) {
        Direction side = BlockUtil.getClickSide(pos);
        if (mc.player.getEyePos().distanceTo(pos.toCenterPos().add(0, -0.5, 0)) > placeRange.get()) {
            return true;
        }
        return side == null ||
                !(pos.toCenterPos().add(new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5)).distanceTo(mc.player.getEyePos())
                        <= placeRange.get());
    }
    private void doSwap(int slot) {
        if (inventorySwap.get()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().getSelectedSlot());
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }
}
