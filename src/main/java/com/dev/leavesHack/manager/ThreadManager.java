package com.dev.leavesHack.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

public class ThreadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("LeavesHack-ThreadManager");
    public static final ThreadManager INSTANCE = new ThreadManager();
    private final CopyOnWriteArrayList<LeavesModule> threadModules = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;
    private Thread serviceThread = null;
    private ThreadManager() {
        startServiceThread();
    }

    /**
     * 注册一个模块到线程轮询列表
     */
    public void register(LeavesModule module) {
        if (!threadModules.contains(module)) {
            threadModules.add(module);
        }
    }

    /**
     * 从线程轮询列表中移除一个模块
     */
    public void unregister(LeavesModule module) {
        threadModules.remove(module);
    }

    /**
     * 启动服务线程
     */
    private void startServiceThread() {
        if (running) return;
        running = true;

        serviceThread = Thread.ofVirtual()
            .name("LeavesHack-ThreadService")
            .start(() -> {
                LOGGER.info("ThreadService started");
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        for (LeavesModule module : threadModules) {
                            if (module.isActive()) {
                                try {
                                    module.onThread();
                                } catch (Exception e) {
                                    LOGGER.error("Error in onThread() for module: {}", module.name, e);
                                }
                            }
                        }
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                LOGGER.info("ThreadService stopped");
            });
    }

    /**
     * 停止服务线程
     */
    public void stopServiceThread() {
        running = false;
        if (serviceThread != null) {
            serviceThread.interrupt();
            serviceThread = null;
        }
        threadModules.clear();
    }
}
