package com.dev.leavesHack.manager;

import com.dev.leavesHack.modules.PacketMine;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BreakManager {
    public static final BreakManager INSTANCE = new BreakManager();

    public final ConcurrentHashMap<Integer, BreakData> breakMap = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer, BreakData> doubleMap = new ConcurrentHashMap<>();

    public boolean detectDouble = true;
    public double doubleMineTimeout = 2.0;
    public double minTimeout = 2.0;
    public double breakTimeout = 1.5;

    private BreakManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onServerConnectBegin(ServerConnectBeginEvent event) {
        breakMap.clear();
        doubleMap.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (detectDouble) {
            doubleMap.entrySet().removeIf(entry -> {
                BreakData data = entry.getValue();
                return data == null
                    || data.getEntity() == null
                    || mc.world.isAir(data.pos)
                    || data.timer.passedMs(Math.max(minTimeout * 1000, data.breakTime * doubleMineTimeout));
            });
        }

        for (BreakData data : breakMap.values()) {
            data.breakTime = Math.max(getBreakTime(data.pos, false), 50);

            if (unbreakable(data.pos)) {
                data.complete = false;
                data.failed = true;
            } else if (mc.world.isAir(data.pos)) {
                data.complete = true;
                data.failed = false;
            } else if (!data.complete && data.timer.passedMs(data.breakTime * breakTimeout)) {
                data.failed = true;
            }
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        if (!(event.packet instanceof BlockBreakingProgressS2CPacket packet)) return;
        if (packet.getPos() == null) return;

        BreakData data = new BreakData(packet.getPos(), packet.getEntityId(), false);
        if (data.getEntity() == null) return;

        if (MathHelper.sqrt((float) data.getEntity().getEyePos().squaredDistanceTo(packet.getPos().toCenterPos())) > 8) return;

        if (detectDouble) {
            if (packet.getProgress() != 255) {
                if (packet.getProgress() != 0) {
                    BreakData doubleData = doubleMap.get(packet.getEntityId());
                    if (doubleData != null) {
                        doubleData.pos = packet.getPos();
                        doubleData.timer.reset();
                    } else if (!unbreakable(packet.getPos())) {
                        doubleMap.put(packet.getEntityId(), new BreakData(packet.getPos(), packet.getEntityId(), true));
                    }
                    return;
                }

                BreakData doubleData = doubleMap.get(packet.getEntityId());
                if (doubleData != null && doubleData.pos.equals(packet.getPos()) && !doubleData.timer.passedS(150)) {
                    return;
                }
            }
        }

        BreakData current = breakMap.get(packet.getEntityId());
        if (current != null && !current.failed && current.pos.equals(packet.getPos())) return;

        breakMap.put(packet.getEntityId(), data);

        if (detectDouble && !doubleMap.containsKey(packet.getEntityId()) && !unbreakable(packet.getPos())) {
            doubleMap.put(packet.getEntityId(), new BreakData(packet.getPos(), packet.getEntityId(), true));
        }
    }

    public ConcurrentHashMap<Integer, BreakData> getMainBreaks() {
        return breakMap;
    }

    public ConcurrentHashMap<Integer, BreakData> getSecondaryBreaks() {
        return doubleMap;
    }

    public List<BlockPos> getMainBreakPositions() {
        List<BlockPos> positions = new ArrayList<>();
        for (BreakData data : breakMap.values()) {
            if (data == null || data.failed || data.complete || data.getEntity() == null) continue;
            positions.add(data.pos);
        }
        return positions;
    }

    public List<BlockPos> getSecondaryBreakPositions() {
        List<BlockPos> positions = new ArrayList<>();
        for (BreakData data : doubleMap.values()) {
            if (data == null || data.getEntity() == null || mc.world == null || mc.world.isAir(data.pos)) continue;
            positions.add(data.pos);
        }
        return positions;
    }

    public boolean isMining(BlockPos pos) {
        return isMining(pos, true);
    }

    public boolean isMining(BlockPos pos, boolean self) {
        if (self && PacketMine.targetPos != null && PacketMine.targetPos.equals(pos)) {
            return true;
        }

        for (BreakData data : breakMap.values()) {
            if (data == null || data.getEntity() == null || data.failed) continue;
            if (data.getEntity().getEyePos().distanceTo(pos.toCenterPos()) > 7) continue;
            if (data.pos.equals(pos)) return true;
        }

        return false;
    }

    public static boolean unbreakable(BlockPos pos) {
        if (mc.world == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock) && (block.getHardness() == -1 || block.getHardness() == 100);
    }

    public static double getBreakTime(BlockPos pos, boolean extraBreak) {
        int slot = getTool(pos);
        if (slot == -1) slot = mc.player.getInventory().getSelectedSlot();
        return getBreakTime(pos, slot, extraBreak ? 1 : 1);
    }

    private static int getTool(BlockPos pos) {
        AtomicInteger slot = new AtomicInteger(-1);
        float fastest = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AirBlockItem) continue;

            float digSpeed = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
            if (digSpeed + destroySpeed > fastest) {
                fastest = digSpeed + destroySpeed;
                slot.set(i);
            }
        }

        return slot.get();
    }

    private static double getBreakTime(BlockPos pos, int slot, double damage) {
        return 1 / getBlockStrength(pos, mc.player.getInventory().getStack(slot)) / 20 * 1000 * damage;
    }

    private static float getBlockStrength(BlockPos pos, ItemStack stack) {
        BlockState state = mc.world.getBlockState(pos);
        float hardness = state.getHardness(mc.world, pos);
        if (hardness < 0) return 0;

        float base = !state.isToolRequired() || stack.isSuitableFor(state) ? 30 : 100;
        return getDigSpeed(state, stack) / hardness / base;
    }

    private static float getDigSpeed(BlockState state, ItemStack stack) {
        float speed = getDestroySpeed(state, stack);
        if (speed > 1) {
            int efficiency = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0 && !stack.isEmpty()) {
                speed += (float) (Math.pow(efficiency, 2) + 1);
            }
        }
        return Math.max(0, speed);
    }

    private static float getDestroySpeed(BlockState state, ItemStack stack) {
        float speed = 1;
        if (stack != null && !stack.isEmpty()) {
            speed *= stack.getMiningSpeedMultiplier(state);
        }
        return speed;
    }

    public static class BreakData {
        public BlockPos pos;
        private final int entityId;
        public final Timer timer;
        public double breakTime;
        public boolean failed = false;
        public boolean complete = false;

        public BreakData(BlockPos pos, int entityId, boolean extraBreak) {
            this.pos = pos;
            this.entityId = entityId;
            this.breakTime = Math.max(getBreakTime(pos, extraBreak), 50);
            this.timer = new Timer();
        }

        public Entity getEntity() {
            if (mc.world == null) return null;
            Entity entity = mc.world.getEntityById(entityId);
            return entity instanceof PlayerEntity ? entity : null;
        }
    }
}
