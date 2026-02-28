package com.dev.leavesHack.utils.combat;

import com.google.common.collect.Lists;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CombatUtil {
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
}
