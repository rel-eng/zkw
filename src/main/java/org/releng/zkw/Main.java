/*
 * Copyright 2014 rel-eng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.releng.zkw;

import org.releng.zkw.log.LogConfiguration;
import org.releng.zkw.tools.ZkVmWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Object shutdownLock = new Object();
    private static volatile boolean shutdown = false;

    public static void main(String[] args) {
        registerShutdownHook();
        LogConfiguration.withLogConfiguration(() -> {
            LOGGER.info("Starting up...");
            ZkVmWatcher vmWatcher = new ZkVmWatcher(10000, 1000);
            Thread vmWatcherThread = new Thread(vmWatcher);
            vmWatcherThread.setDaemon(true);
            vmWatcherThread.start();
            synchronized (shutdownLock) {
                while (!shutdown) {
                    try {
                        shutdownLock.wait(100);
                    } catch (InterruptedException e) {
                    }
                }
            }
            LOGGER.info("Shutting down...");
            vmWatcher.shutdown();
            try {
                vmWatcherThread.join(100);
            } catch (InterruptedException e) {
            }
            LOGGER.info("Shutdown complete");
        });

    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            synchronized (shutdownLock) {
                shutdown = true;
                shutdownLock.notifyAll();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }));
    }

}
