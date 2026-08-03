package com.dev.leavesHack.asm.accessors;

import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Friends.class)
public interface IFriend {

    @Accessor("friends")
    List<Friend> getFriends();
}
