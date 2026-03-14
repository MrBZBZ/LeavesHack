package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.modules.autoLogin.AutoLoginAccount;
import com.dev.leavesHack.modules.autoLogin.AutoLoginAccounts;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.orbit.EventHandler;

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
    private void onTick(TickEvent.Pre event) {
        if (check) {
            mc.getNetworkHandler().sendCommand("login " + pw);
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