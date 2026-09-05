package com.dev.leavesHack.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public class PlaceBlockEvent {

    public BlockPos blockPos;
    public Block block;

    public PlaceBlockEvent(BlockPos blockPos, Block block) {
        this.blockPos = blockPos;
        this.block = block;
    }
}