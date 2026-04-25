package com.dev.leavesHack.utils.combat;

import com.dev.leavesHack.utils.entity.EntityUtil;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import com.google.common.collect.Lists;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
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
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof PlayerEntity player && Friends.get().isFriend(player) || mc.player.getPos().distanceTo(entity.getPos()) > range;

        return !invalid;
    }
    public static boolean isValid(Entity entity) {
        boolean invalid = entity == null || !entity.isAlive() || entity.equals(mc.player) || entity instanceof PlayerEntity player && Friends.get().isFriend(player);

        return !invalid;
    }
    public static PlayerEntity getClosestEnemy(double distance) {
        PlayerEntity closest = null;

        for (PlayerEntity player : getEnemies(distance)) {
            if (closest == null) {
                closest = player;
                continue;
            }

            if (!(mc.player.squaredDistanceTo(player.getPos()) < mc.player.squaredDistanceTo(closest))) continue;

            closest = player;
        }
        return closest;
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
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            mc.player.resetLastAttackedTicks();
            EntityUtil.attackSwingHand();
            if (rotate) {
               Rotation.snapBack();
            }
        }
    }
}
