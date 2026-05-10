package com.dev.leavesHack.utils.entity;

import com.dev.leavesHack.events.KeyboardInputEvent;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MoveFixUtil {
    public static void fixMovement(KeyboardInputEvent event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();

        int angleUnit = 45;
        float angleTolerance = 22.5f;

        float directionFactor =
                Math.max(Math.abs(forward), Math.abs(strafe));

        double angleDifference =
                MathHelper.wrapDegrees(
                        getDirection(forward, strafe) - yaw
                );

        double angleDistance = Math.abs(angleDifference);

        forward = 0.0F;
        strafe = 0.0F;

        if (angleDistance <= angleUnit + angleTolerance) {
            forward++;
        } else if (angleDistance >= 180.0F - angleUnit - angleTolerance) {
            forward--;
        }

        if (angleDifference >= angleUnit - angleTolerance
                && angleDifference <= 180.0F - angleUnit + angleTolerance) {

            strafe--;

        } else if (angleDifference <= -angleUnit + angleTolerance
                && angleDifference >= -180.0F + angleUnit - angleTolerance) {

            strafe++;
        }

        forward *= directionFactor;
        strafe *= directionFactor;

        event.setForward(forward);
        event.setStrafe(strafe);
    }

    private static float getDirection(float forward, float strafe) {
        float yaw = mc.player.getYaw();
        boolean isMovingForward = forward > 0;
        boolean isMovingBack = forward < 0;
        boolean isMovingRight = strafe > 0;
        boolean isMovingLeft = strafe < 0;
        boolean sideways = isMovingLeft || isMovingRight;
        boolean straight = isMovingForward || isMovingBack;
        if (forward != 0.0F || strafe != 0.0F) {
            if (isMovingBack && !sideways) {
                return yaw + 180.0f;
            }
            if (isMovingForward && isMovingLeft) {
                return yaw + 45.0f;
            }
            if (isMovingForward && isMovingRight) {
                return yaw - 45.0f;
            }
            if (!straight && isMovingLeft) {
                return yaw + 90.0f;
            }
            if (!straight) {
                return yaw - 90.0f;
            }
            if (isMovingBack && isMovingLeft) {
                return yaw + 135.0f;
            }
            if (isMovingBack) {
                return yaw - 135.0f;
            }
        }
        return yaw;
    }
}