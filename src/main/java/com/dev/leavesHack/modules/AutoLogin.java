package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AutoLogin extends Module {
    public static AutoLogin INSTANCE;
    public AutoLogin() {
        super(LeavesHack.CATEGORY, "AutoLogin", "Automatically logs you into the server.");
        INSTANCE = this;
    }
}
