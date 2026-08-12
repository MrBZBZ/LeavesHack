package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.events.DeathEvent;
import com.dev.leavesHack.events.MoveEvent;
import com.dev.leavesHack.manager.LeavesModule;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockPosX;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class SelfTrap extends LeavesModule {
    public static SelfTrap INSTANCE;

    public SelfTrap() {
        super(LeavesHack.LEAVES_COMBAT, "SelfTrapPlus", "黑曜石自围");
        INSTANCE = this;
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCheck = settings.createGroup("Check");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .description("操作距离")
        .defaultValue(5.0)
        .sliderRange(1, 6)
        .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("放置延迟(ms)")
        .defaultValue(0)
        .sliderRange(0, 500)
        .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("转头")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> blocksPer = sgGeneral.add(new IntSetting.Builder()
        .name("BlocksPer")
        .description("每帧最大操作数")
        .defaultValue(2)
        .sliderRange(0, 6)
        .build()
    );
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("PacketPlace")
        .description("发包放置")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> enderChest = sgGeneral.add(new BoolSetting.Builder()
        .name("EnderChest")
        .description("末影箱")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> attack = sgGeneral.add(new BoolSetting.Builder()
        .name("Attack")
        .description("打水晶")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("AntiSuicide")
        .description("防暴毙")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> head = sgGeneral.add(new BoolSetting.Builder()
        .name("Head")
        .description("封头")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> feet = sgGeneral.add(new BoolSetting.Builder()
        .name("Feet")
        .description("封脚")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> chest = sgGeneral.add(new BoolSetting.Builder()
        .name("Chest")
        .description("封胸")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> extend = sgGeneral.add(new BoolSetting.Builder()
        .name("Extend")
        .description("扩展包裹(绕过障碍)")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> blockerExtend = sgGeneral.add(new BoolSetting.Builder()
        .name("BlockerExtend")
        .description("额外点位")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("Center")
        .description("居中对齐")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> inventory = sgGeneral.add(new BoolSetting.Builder()
        .name("InventorySwap")
        .description("静默背包切换")
        .defaultValue(true)
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
        .defaultValue(false)
        .visible(usingPause::get)
        .build()
    );

    private final Setting<Boolean> allowNotOnGround = sgCheck.add(new BoolSetting.Builder()
        .name("allowNotOnGround")
        .description("允许在空中放置")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> moveDisable = sgCheck.add(new BoolSetting.Builder()
        .name("MoveDisable")
        .description("移动后关闭")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> jumpDisable = sgCheck.add(new BoolSetting.Builder()
        .name("JumpDisable")
        .description("跳跃后关闭")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> noBlockDisable = sgCheck.add(new BoolSetting.Builder()
        .name("NoBlockDisable")
        .description("无方块时关闭")
        .defaultValue(false)
        .build()
    );

    private final Timer placeTimer = new Timer();
    private final List<BlockPos> placedPositions = new ArrayList<>();
    private double startX, startY, startZ;
    @Override
    public void onActivate() {
        placeTimer.setMs(999999);
        shouldCenter = true;
        if (mc.player != null) {
            startX = mc.player.getX();
            startY = mc.player.getY();
            startZ = mc.player.getZ();
        }
    }
    @EventHandler
    public void onDeath(DeathEvent event) {
        if (event.getPlayer() == mc.player && isActive()) {
            toggle();
        }
    }
    @Override
    public void onDeactivate() {
        placedPositions.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (usingPause.get() && checkPause(onlyMain.get())) return;

        int block = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.OBSIDIAN) : InventoryUtil.findItem(Items.OBSIDIAN);
        if (block == -1 && enderChest.get()) {
            block = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.ENDER_CHEST) : InventoryUtil.findItem(Items.ENDER_CHEST);
        }
        if (block == -1) {
            if (noBlockDisable.get()) toggle();
            return;
        }

        if (!allowNotOnGround.get() && !mc.player.isOnGround()) return;

        if (moveDisable.get() || jumpDisable.get()) {
            double dist = mc.player.squaredDistanceTo(startX, startY, startZ);
            if (moveDisable.get() && dist > 1.0) { toggle(); return; }
            if (jumpDisable.get() && mc.player.input.playerInput.jump()) { toggle(); return; }
        }

        if (!placeTimer.passedMs(delay.get())) return;
        placeTimer.reset();
        placedPositions.clear();

        BlockPos pos = getPlayerPos();
        ArrayList<BlockPos> trapList = getSurList(pos);

        int count = 0;
        for (BlockPos target : trapList) {
            if (count >= blocksPer.get()) return;
            count += tryPlaceBlock(target, block);
        }
    }

    private ArrayList<BlockPos> getSurList(BlockPos pos) {
        ArrayList<BlockPos> list = new ArrayList<>();
        if (head.get()) {
            addUnique(list, pos.up(2));
        }
        if (feet.get()) {
            addSurround(list, pos);
        }
        if (chest.get()) {
            addSurround(list, pos.up());
        }
        if (blockerExtend.get()) {
            addBlockerExtend(list);
        }
        return list;
    }

    private void addSurround(ArrayList<BlockPos> list, BlockPos pos) {
        for (Direction dir : Direction.HORIZONTAL) {
            BlockPos target = pos.offset(dir);
            addUnique(list, target);
            if (selfIntersectPos(target) && extend.get()) {
                addExtend(list, target);
            }
        }
    }

    private void addExtend(ArrayList<BlockPos> list, BlockPos pos) {
        for (Direction dir : Direction.HORIZONTAL) {
            BlockPos target = pos.offset(dir);
            addUnique(list, target);
            if (selfIntersectPos(target)) {
                for (Direction dir2 : Direction.HORIZONTAL) {
                    addUnique(list, target.offset(dir2));
                }
            }
        }
    }

    private void addBlockerExtend(ArrayList<BlockPos> list) {
        double[] offset = {-0.3, 0, 0.3};
        for (double x : offset) {
            for (double z : offset) {
                BlockPos base = new BlockPosX(
                    mc.player.getX() + x,
                    mc.player.getY() + 0.5,
                    mc.player.getZ() + z
                );
                for (Direction dir : Direction.HORIZONTAL) {
                    BlockPos surround = base.offset(dir);
                    for (Direction dir2 : Direction.HORIZONTAL) {
                        BlockPos target = surround.offset(dir2);
                        if (BlockUtil.canPlace(target, null))
                            addUnique(list, target);
                    }
                }
            }
        }
    }

    private static void addUnique(ArrayList<BlockPos> list, BlockPos pos) {
        if (!list.contains(pos)) list.add(pos);
    }

    private int tryPlaceBlock(BlockPos pos, int block) {
        if (pos == null || placedPositions.contains(pos)) return 0;
        if (mc.player.getEyePos().distanceTo(pos.toCenterPos()) > range.get()) return 0;
        if (!BlockUtil.canPlace(pos, true)) return 0;
        if (BlockUtil.hasCrystal(pos) && attack.get()) {
            if (!attackCrystals(pos)) return 0;
        } else if (BlockUtil.hasEntity(pos, false)) return 0;

        Direction side = BlockUtil.getPlaceSide(pos, null);
        if (side == null) {
            BlockPos helper = findHelperPos(pos);
            if (helper == null) return 0;
            side = BlockUtil.getPlaceSide(helper, null);
            if (side == null) return 0;
            pos = helper;
        }

        int old = mc.player.getInventory().getSelectedSlot();
        doSwap(block);
        BlockUtil.placeBlock(pos, side, rotate.get(), packetPlace.get());
        placedPositions.add(pos);
        if (inventory.get()) {
            doSwap(block);
        } else {
            doSwap(old);
        }
        return 1;
    }

    private BlockPos findHelperPos(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            BlockPos neighbor = pos.offset(dir);
            if (BlockUtil.isGrimDirection(neighbor, dir.getOpposite()) && BlockUtil.canPlace(neighbor, null)) {
                return neighbor;
            }
        }
        return null;
    }

    private boolean attackCrystals(BlockPos pos) {
        for (Entity entity : BlockUtil.getEndCrystals(new Box(pos))) {
            if (!entity.isAlive() || !(entity instanceof EndCrystalEntity crystal)) continue;
            float self = DamageUtils.crystalDamage(mc.player, entity.getEntityPos(), false, crystal.getBlockPos().down());
            if (antiSuicide.get() && self >= EntityUtils.getTotalHealth(mc.player)) return false;
            CombatUtil.attackCrystal(entity, rotate.get(), false);
        }
        return true;
    }

    private BlockPos getPlayerPos() {
        return new BlockPosX(mc.player.getEntityPos(), true);
    }

    public boolean selfIntersectPos(BlockPos pos) {
        return mc.player.getBoundingBox().intersects(new Box(pos));
    }

    private void doSwap(int slot) {
        if (!inventory.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().getSelectedSlot());
        }
    }

    private boolean shouldCenter = true;

    @EventHandler(priority = -1)
    public void onMove(MoveEvent event) {
        if (mc.player == null || !center.get() || !isActive()) return;
        if (mc.options.sneakKey.isPressed()) return;

        BlockPos blockPos = getPlayerPos();
        double px = mc.player.getX() - blockPos.getX() - 0.5;
        double pz = mc.player.getZ() - blockPos.getZ() - 0.5;

        if (Math.abs(px) <= 0.2 && Math.abs(pz) <= 0.2) {
            if (shouldCenter && (mc.player.isOnGround() || isMoving())) {
                event.setX(0);
                event.setZ(0);
                shouldCenter = false;
            }
        } else {
            if (shouldCenter) {
                Vec3d centerPos = blockPos.toCenterPos();
                float yaw = getRotationTo(mc.player.getEntityPos(), centerPos);
                float yawRad = (float) Math.toRadians(yaw);
                double dist = mc.player.getEntityPos().distanceTo(new Vec3d(centerPos.x, mc.player.getY(), centerPos.z));
                double speed = Math.min(0.2873, dist);
                double x = -Math.sin(yawRad) * speed;
                double z = Math.cos(yawRad) * speed;
                event.setX(x);
                event.setZ(z);
            }
        }
    }

    private static float getRotationTo(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffZ = to.z - from.z;
        return (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
    }

    private boolean isMoving() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
            || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
    }

    private boolean checkPause(boolean onlyMain) {
        return (mc.options.useKey.isPressed() || mc.player.isUsingItem())
            && (!onlyMain || mc.player.getActiveHand() == Hand.MAIN_HAND);
    }

}
