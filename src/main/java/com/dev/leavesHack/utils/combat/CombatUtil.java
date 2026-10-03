package com.dev.leavesHack.utils.combat;

import com.dev.leavesHack.modules.GlobalSetting;
import com.dev.leavesHack.utils.entity.EntityUtil;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import com.google.common.collect.Lists;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtil {
    public static BlockPos modifyPos;
    public static BlockState modifyBlockState = Blocks.AIR.getDefaultState();
    public static List<PlayerEntity> getEnemies(double range) {
        List<PlayerEntity> list = new ArrayList<>();
        for (AbstractClientPlayerEntity player : Lists.newArrayList(mc.world.getPlayers())) {
            if (!isValid(player, range)) continue;
            list.add(player);
        }
        return list;
    }
    public static boolean isValid(Entity entity, double range) {
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof PlayerEntity player && Friends.get().isFriend(player) || mc.player.getEntityPos().distanceTo(entity.getEntityPos()) > range;

        return !invalid;
    }
    public static boolean isValid(Entity entity) {
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof PlayerEntity player && Friends.get().isFriend(player);

        return !invalid;
    }
    // ==================== 目标选择（全局设置 -> Combat -> TargetMode） ====================

    /** 目标选择方法：Health=生命值优先，Distance=距离优先，Both=距离&生命值综合 */
    public enum TargetMode {
        Health, Distance, Both
    }

    /** 读取全局目标模式，GlobalSetting 未初始化时回退为 Distance（保持旧行为） */
    public static TargetMode getTargetMode() {
        return GlobalSetting.INSTANCE == null ? TargetMode.Distance : GlobalSetting.INSTANCE.targetMode.get();
    }

    /** 血量维度评分：越低越优先；非生物实体视为最差（排最后） */
    private static double healthScore(Entity entity) {
        if (entity instanceof LivingEntity living) return living.getHealth() + living.getAbsorptionAmount();
        return Double.MAX_VALUE;
    }

    /** 目标综合评分，越小越优先 */
    public static double targetScore(Entity entity, double range) {
        double dist = mc.player.squaredDistanceTo(entity);
        return switch (getTargetMode()) {
            case Health -> healthScore(entity);
            case Distance -> dist;
            case Both -> dist / Math.max(range * range, 1.0) + healthScore(entity) / 20.0;
        };
    }

    /** 从候选列表中按全局目标模式选出最优目标 */
    public static <T extends Entity> T getTarget(List<T> candidates, double range) {
        T best = null;
        double bestScore = Double.MAX_VALUE;
        for (T entity : candidates) {
            double score = targetScore(entity, range);
            if (best == null || score < bestScore) {
                best = entity;
                bestScore = score;
            }
        }
        return best;
    }

    /** 按全局目标模式排序的比较器（越小越优先） */
    public static <T extends Entity> Comparator<T> targetComparator(double range) {
        return Comparator.comparingDouble(entity -> targetScore(entity, range));
    }

    /** 按全局目标模式选出范围内最优敌人（玩家） */
    public static PlayerEntity getTargetEnemy(double distance) {
        return getTarget(getEnemies(distance), distance);
    }

    public static void attackCrystal(BlockPos pos, boolean rotate, boolean eatingPause) {
        attackCrystal(new Box(pos), rotate, eatingPause);
    }

    public static void attackCrystal(Box box, boolean rotate, boolean eatingPause) {
        for (EndCrystalEntity entity : BlockUtil.getEndCrystals(box)) {
            attackCrystal(entity, rotate, eatingPause);
        }
    }
    public static void attackCrystal(Entity crystal, boolean rotate, boolean usingPause) {
        if (usingPause && mc.player.isUsingItem())
            return;
        if (crystal != null) {
            Rotation.snapAt(new Vec3d(crystal.getX(), crystal.getY() + 0.25, crystal.getZ()));
//            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.resetTicksSinceLastAttack();
            EntityUtil.attackSwingHand();
            if (rotate) {
               Rotation.snapBack();
            }
        }
    }
}
