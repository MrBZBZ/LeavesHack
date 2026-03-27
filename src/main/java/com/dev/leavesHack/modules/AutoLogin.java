package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.modules.autoLogin.AutoLoginAccount;
import com.dev.leavesHack.modules.autoLogin.AutoLoginAccounts;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.text.Text;

import java.util.List;

public class AutoLogin extends Module {

    public static AutoLogin INSTANCE;
    private boolean check = false;
    private String pw,lastIp = "";
    private List<AutoLoginAccount> accounts() {
        return AutoLoginAccounts.get().getAccounts();
    }

    public AutoLogin() {
        super(LeavesHack.CATEGORY, "AutoLogin", "Automatically logs you into the server.");
        MeteorClient.EVENT_BUS.subscribe(new StaticListener());
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public final Setting<Boolean> autoSave = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoSave")
            .description("登录信息自动保存")
            .defaultValue(true)
            .build()
    );
    public final Setting<String> loginCommand = sgGeneral.add(new StringSetting.Builder()
            .name("LoginCommand")
            .description("Login Command 登录的指令")
            .defaultValue("l")
            .build()
    );
    public final Setting<String> registerCommand = sgGeneral.add(new StringSetting.Builder()
            .name("RegCommand")
            .description("Register Command 注册的指令")
            .defaultValue("reg")
            .build()
    );
    public final Setting<String> cpCommand = sgGeneral.add(new StringSetting.Builder()
            .name("CpCommand")
            .description("Change Password Command 改密码的指令")
            .defaultValue("cp")
            .build()
    );
    private class StaticListener {
        @EventHandler
        private void onGameJoined(ServerConnectBeginEvent event) {
            check = false;
            lastIp = event.address.getAddress();
            for (AutoLoginAccount account : accounts()) {
                if (account.username.get().equals(mc.getSession().getUsername()) && account.serverIp.get().equals(event.address.getAddress())) {
                    check = true;
                    pw = account.password.get();
                }
            }
        }
    }
    @EventHandler
    public void onMessageSend(PacketEvent.Send event) {
        if (event.packet instanceof CommandExecutionC2SPacket packet && autoSave.get()) {
            String message = packet.command();
            String[] args = message.split(" ");
            if (args.length < 2) return;
            if (args[0].equals(loginCommand.get()) || args[0].equals(registerCommand.get()) || args[0].equals(cpCommand.get())) {
                String password = args[0].equals(cpCommand.get()) ? args[2] : args[1];
                String username = mc.getSession().getUsername();
                String server = lastIp;
                for (AutoLoginAccount account : accounts()) {
                    if (account.username.get().equals(username)
                            && account.serverIp.get().equals(server)) {
                        account.password.set(password);
                        return;
                    }
                }
                AutoLoginAccount account = new AutoLoginAccount();
                account.username.set(username);
                account.serverIp.set(server);
                account.password.set(password);
                AutoLoginAccounts.get().add(account);
            }
        }
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (check) {
            mc.getNetworkHandler().sendCommand(loginCommand.get() + " " + pw);
            check = false;
        }
    }
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        initTable(theme, table);
        return table;
    }
    private void initTable(GuiTheme theme, WTable table) {
        table.clear();
        table.add(theme.label("Username"));
        table.add(theme.label("Server"));
        table.add(theme.label("Password"));
        table.row();
        for (AutoLoginAccount account : accounts()) {
            table.add(theme.label(account.username.get()));
            table.add(theme.label(account.serverIp.get() + "  "));
            table.add(theme.label(account.password.get()));
            WButton edit = table.add(theme.button("Edit")).widget();
            edit.action = () -> mc.setScreen(new EditAccountScreen(theme, account, () -> initTable(theme, table)));
            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                AutoLoginAccounts.get().remove(account);
                initTable(theme, table);
            };
            table.row();
        }
        table.row();
        table.add(theme.horizontalSeparator()).expandX();
        table.row();
        WButton createAccount = table.add(theme.button("CreateAccount")).expandX().widget();
        createAccount.action = () -> mc.setScreen(
                new EditAccountScreen(theme, null, () -> initTable(theme, table))
        );
    }
    private class EditAccountScreen extends EditSystemScreen<AutoLoginAccount> {
        public EditAccountScreen(GuiTheme theme, AutoLoginAccount value, Runnable reload) {
            super(theme, value, reload);
            if (value == null) {
                this.value.username.set(mc.getSession().getUsername());
                this.value.serverIp.set(lastIp);
            }
        }
        @Override
        public AutoLoginAccount create() {
            return new AutoLoginAccount();
        }
        @Override
        public boolean save() {
            if (value.username.get().isBlank()) return false;
            if (!accounts().contains(value)) {
                AutoLoginAccounts.get().add(value);
            }
            return true;
        }
        @Override
        public Settings getSettings() {
            return value.settings;
        }
    }
}