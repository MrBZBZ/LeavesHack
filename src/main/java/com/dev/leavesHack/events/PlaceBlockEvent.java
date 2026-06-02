package com.dev.leavesHack.events;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class PlaceBlockEvent {

    public BlockPos blockPos;
    public Block block;

    public PlaceBlockEvent(BlockPos blockPos, Block block) {
        this.blockPos = blockPos;
        this.block = block;
    }
}