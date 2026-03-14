package com.dev.leavesHack.modules.autoLogin;

import meteordevelopment.meteorclient.settings.*;

public class AutoLoginAccount{

    public final Settings settings = new Settings();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> username = sgGeneral.add(new StringSetting.Builder()
            .name("username")
            .description("Account username.")
            .defaultValue("")
            .build()
    );

    public final Setting<String> serverIp = sgGeneral.add(new StringSetting.Builder()
            .name("server-ip")
            .description("Server ip.")
            .defaultValue("")
            .build()
    );

    public final Setting<String> password = sgGeneral.add(new StringSetting.Builder()
            .name("password")
            .description("Account password.")
            .defaultValue("")
            .build()
    );
}