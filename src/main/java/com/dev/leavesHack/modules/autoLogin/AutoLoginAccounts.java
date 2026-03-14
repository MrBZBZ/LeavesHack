package com.dev.leavesHack.modules.autoLogin;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;

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
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        NbtList list = new NbtList();

        for (AutoLoginAccount acc : accounts) {
            NbtCompound t = new NbtCompound();
            t.putString("username", acc.username.get());
            t.putString("ip", acc.serverIp.get());
            t.putString("password", acc.password.get());
            list.add(t);
        }

        tag.put("accounts", list);
        return tag;
    }

    @Override
    public AutoLoginAccounts fromTag(NbtCompound tag) {
        accounts.clear();

        NbtList list = tag.getList("accounts", 10);

        for (NbtElement e : list) {
            NbtCompound t = (NbtCompound) e;

            AutoLoginAccount acc = new AutoLoginAccount();
            acc.username.set(t.getString("username"));
            acc.serverIp.set(t.getString("ip"));
            acc.password.set(t.getString("password"));

            accounts.add(acc);
        }

        return this;
    }
}