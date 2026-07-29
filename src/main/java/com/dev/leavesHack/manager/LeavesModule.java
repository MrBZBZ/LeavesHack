package com.dev.leavesHack.manager;

import com.dev.leavesHack.events.ModuleActiveEvent;
import com.dev.leavesHack.events.ModuleDeactiveEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public abstract class LeavesModule extends Module {

    public LeavesModule(Category category, String name, String description) {
        super(category, name, description);
    }
    public LeavesModule(Category category, String name, String description, String... aliases) {
        super(category, name, description, aliases);
    }
    public void onThread() {
        // 默认空实现，子类按需重写
    }
    @EventHandler
    public void onActivate(ModuleActiveEvent event) {
        ThreadManager.INSTANCE.register(this);
    }
    @EventHandler
    public void onDeactivate(ModuleDeactiveEvent event) {
        ThreadManager.INSTANCE.unregister(this);
    }
}
