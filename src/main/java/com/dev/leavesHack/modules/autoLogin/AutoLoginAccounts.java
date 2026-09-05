package com.dev.leavesHack.modules.autoLogin;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class AutoLoginAccounts extends System<AutoLoginAccounts> {

    private final List<AutoLoginAccount> accounts = new ArrayList<>();

    public AutoLoginAccounts() {
        super("autologin-accounts");
    }

    public static AutoLoginAccounts get() {
        return Systems.get(AutoLoginAccounts.class);
    }

    public List<AutoLoginAccount> getAccounts() {
        return accounts;
    }

    public void add(AutoLoginAccount acc) {
        accounts.add(acc);
        save();
    }

    public void remove(AutoLoginAccount acc) {
        accounts.remove(acc);
        save();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();

        for (AutoLoginAccount acc : accounts) {
            CompoundTag t = new CompoundTag();
            t.putString("username", acc.username.get());
            t.putString("ip", acc.serverIp.get());
            t.putString("password", acc.password.get());
            list.add(t);
        }

        tag.put("accounts", list);
        return tag;
    }

    @Override
    public AutoLoginAccounts fromTag(CompoundTag tag) {
        accounts.clear();

        ListTag list = tag.getListOrEmpty("accounts");

        for (Tag e : list) {
            CompoundTag t = (CompoundTag) e;

            AutoLoginAccount acc = new AutoLoginAccount();
            acc.username.set(t.getStringOr("username", ""));
            acc.serverIp.set(t.getStringOr("ip", ""));
            acc.password.set(t.getStringOr("password", ""));

            accounts.add(acc);
        }

        return this;
    }
}
