package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IFriend;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import javax.swing.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FriendsManager extends Module {
    public static FriendsManager INSTANCE;
    public FriendsManager() {
        super(LeavesHack.CATEGORY, "FriendsManager", "好友管理器");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public final Setting<Boolean> deBug = sgGeneral.add(new BoolSetting.Builder()
        .name("DeBug")
        .description("IQ+++")
        .defaultValue(false)
        .build()
    );
    public final Setting<String> alienPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("Prefix")
        .description("alien前缀")
        .defaultValue(";")
        .build()
    );
    public final Setting<String> hachimiPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("HachimiPrefix")
        .description("Hachimi前缀")
        .defaultValue(";")
        .build()
    );
    public final Setting<String> mioPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("MioPrefix")
        .description("Mio前缀")
        .defaultValue(".")
        .build()
    );
    private final Setting<Boolean> alien = sgGeneral.add(new BoolSetting.Builder()
        .name("Alien")
        .description("Alien")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> hachimi = sgGeneral.add(new BoolSetting.Builder()
        .name("Hachimi")
        .description("Hachimi")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> mio = sgGeneral.add(new BoolSetting.Builder()
        .name("Mio")
        .description("Mio")
        .defaultValue(false)
        .build()
    );
    public final Setting<String> addFCommand = sgGeneral.add(new StringSetting.Builder()
        .name("AddFCommand")
        .description("加F指令")
        .defaultValue("friend add")
        .build()
    );
    public final Setting<String> removeFCommand = sgGeneral.add(new StringSetting.Builder()
        .name("RemoveFCommand")
        .description("删F指令")
        .defaultValue("friend remove")
        .build()
    );
    private List<Friend> lastFriendList = List.of();
    @Override
    public void onActivate() {
        lastFriendList = new ArrayList<>(((IFriend) Friends.get()).getFriends());
    }
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        List<Friend> friendList = ((IFriend) Friends.get()).getFriends();
//        info("now" + friendList.size() + " last" + lastFriendList.size());
        if (friendList.size() == lastFriendList.size()) return;
        for (Friend friend : lastFriendList) {
            if (!containsFriend(friendList, friend)) {
                remove(friend);
            }
        }
        for (Friend friend : friendList) {
            if (!containsFriend(lastFriendList, friend)) {
                add(friend);
            }
        }
        lastFriendList = new ArrayList<>(friendList);
    }
    private boolean containsFriend(List<Friend> list, Friend target) {
        for (Friend friend : list) {
            if (friend.name.equals(target.name)) {
                return true;
            }
        }
        return false;
    }
    private void add(Friend friend) {
        executeCommand(addFCommand.get(), friend);
    }

    private void remove(Friend friend) {
        executeCommand(removeFCommand.get(), friend);
    }
    private void executeCommand(String command, Friend friend) {
        String name = friend.getName();
        if (alien.get()) {
            send(alienPrefix.get() + command + " " + name);
        }
        if (hachimi.get()) {
            send(hachimiPrefix.get() + command + " " + name);
        }
        if (mio.get()) {
            String mioName = containsChinese(name) ? "\"" + name + "\"" : name;
            send(mioPrefix.get() + command + " " + mioName);
        }
    }
    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }
    private void send(String command) {
        if (deBug.get()) {
            info("执行: " + command);
        }
        mc.player.networkHandler.sendChatMessage(command);
    }
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton syncButton = theme.button("Sync Friends");
        syncButton.action = this::syncFriends;
        WButton importButton = theme.button("Import TXT");
        importButton.action = this::importFriends;
        table.add(syncButton).centerX();
        table.row();
        table.add(importButton).centerX();
        return table;
    }
    private void syncFriends() {
        info("开始同步");
        List<Friend> friendList = ((IFriend) Friends.get()).getFriends();
        for (Friend friend : friendList) {
            add(friend);
        }
        info("同步完毕");
    }
    private void importFriends() {
        new Thread(() -> {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("选择好友列表 TXT");
                JFrame frame = new JFrame();
                frame.setAlwaysOnTop(true);
                frame.setUndecorated(true);
                frame.setSize(0, 0);
                frame.setLocationRelativeTo(null);
                int result = chooser.showOpenDialog(frame);
                frame.dispose();
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File file = chooser.getSelectedFile();
                List<String> names = Files.readAllLines(
                    file.toPath(),
                    StandardCharsets.UTF_8
                );
                for (String name : names) {
                    name = name.trim();
                    if (name.isEmpty()) continue;
                    String finalName = name;
                    mc.execute(() -> {
                        addFriend(finalName);
                    });
                }
                mc.execute(() -> {
                    info("导入完成，共 " + names.size() + " 个好友");
                });
            } catch (Exception e) {
                mc.execute(() -> {
                    info("读取文件失败: " + e.getMessage());
                });
            }
        }, "FriendsManager-Import").start();
    }
    private void addFriend(String name) {
        Friends.get().add(new Friend(name));
    }
    private static class TopMostFileChooser extends JFileChooser {
        public JDialog showDialog() {
            JDialog dialog = super.createDialog(null);
            dialog.setTitle("选择好友列表 TXT");
            dialog.setAlwaysOnTop(true);
            return dialog;
        }
    }
}
