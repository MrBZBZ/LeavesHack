package com.dev.leavesHack.events;

public class KeyboardInputEvent {

    private float forward;
    private float strafe;

    public boolean jump;
    public boolean sneak;

    public KeyboardInputEvent(
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jump,
            boolean sneak
    ) {
        this.jump = jump;
        this.sneak = sneak;

        this.forward =
                (forward ? 1.0F : 0.0F)
                        - (backward ? 1.0F : 0.0F);

        this.strafe =
                (left ? 1.0F : 0.0F)
                        - (right ? 1.0F : 0.0F);
    }

    public float getForward() {
        return forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public float getStrafe() {
        return strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    // 给mc.player.input同步用
    public float getMovementForward() {
        return forward;
    }

    public float getMovementSideways() {
        return strafe;
    }

    // 如果你还需要bool状态
    public boolean isForward() {
        return forward > 0;
    }

    public boolean isBackward() {
        return forward < 0;
    }

    public boolean isLeft() {
        return strafe < 0;
    }

    public boolean isRight() {
        return strafe > 0;
    }
}