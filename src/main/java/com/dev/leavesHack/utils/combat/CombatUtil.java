package com.dev.leavesHack.utils.combat;

import com.dev.leavesHack.utils.entity.EntityUtil;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtil {
    public static BlockPos modifyPos;
    public static BlockState modifyBlockState = Blocks.AIR.defaultBlockState();
    public static List<Player> getEnemies(double range) {
        List<Player> list = new ArrayList<>();
        for (AbstractClientPlayer player : Lists.newArrayList(mc.level.players())) {
            if (!isValid(player, range)) continue;
            list.add(player);
        }
        return list;
    }
    public static boolean isValid(Entity entity, double range) {
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof Player player && Friends.get().isFriend(player) || mc.player.position().distanceTo(entity.position()) > range;

        return !invalid;
    }
    public static boolean isValid(Entity entity) {
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof Player player && Friends.get().isFriend(player);

        return !invalid;
    }
    public static Player getClosestEnemy(double distance) {
        Player closest = null;

        for (Player player : getEnemies(distance)) {
            if (closest == null) {
                closest = player;
                continue;
            }

            if (!(mc.player.distanceToSqr(player.position()) < mc.player.distanceToSqr(closest))) continue;

            closest = player;
        }
        return closest;
    }
    public static void attackCrystal(BlockPos pos, boolean rotate, boolean eatingPause) {
        attackCrystal(new AABB(pos), rotate, eatingPause);
    }

    public static void attackCrystal(AABB box, boolean rotate, boolean eatingPause) {
        for (EndCrystal entity : BlockUtil.getEndCrystals(box)) {
            attackCrystal(entity, rotate, eatingPause);
        }
    }
    public static void attackCrystal(Entity crystal, boolean rotate, boolean usingPause) {
        if (usingPause && mc.player.isUsingItem())
            return;
        if (crystal != null) {
            Rotation.snapAt(new Vec3(crystal.getX(), crystal.getY() + 0.25, crystal.getZ()));
//            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            mc.gameMode.attack(mc.player, crystal);
            mc.player.resetOnlyAttackStrengthTicker();
            EntityUtil.attackSwingHand();
            if (rotate) {
               Rotation.snapBack();
            }
        }
    }
}
