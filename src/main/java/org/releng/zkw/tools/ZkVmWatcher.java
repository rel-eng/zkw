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
package org.releng.zkw.tools;

import com.sun.tools.attach.VirtualMachineDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ZkVmWatcher implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkVmWatcher.class);

    private volatile boolean shutdown = false;
    private final Object shutdownLock = new Object();
    private final Set<VirtualMachineDescriptor> watchedVMs = new HashSet<>();
    private final Map<VirtualMachineDescriptor, ZkVmListener> watchedVmListeners = new HashMap<>();
    private final Map<VirtualMachineDescriptor, Thread> watchedVmListenerThreads = new HashMap<>();
    private final long vmPollPauseMillis;
    private final long vmListPollPauseMillis;

    public ZkVmWatcher(long vmPollPauseMillis, long vmListPollPauseMillis) {
        this.vmPollPauseMillis = vmPollPauseMillis;
        this.vmListPollPauseMillis = vmListPollPauseMillis;
    }

    @Override
    public void run() {
        try {
            doWatch();
        } catch (Exception e) {
            LOGGER.error("VM watcher error", e);
        }
    }

    private void doWatch() {
        LOGGER.info("VM watcher is running...");
        synchronized (shutdownLock) {
            while (!shutdown) {
                Set<VirtualMachineDescriptor> machines = new HashSet<>(ZkVmProvider.getZkVmDescriptors());
                machines.forEach(m -> {
                    if (!watchedVMs.contains(m)) {
                        watchVm(m);
                    }
                });
                watchedVMs.forEach(m -> {
                    if (!machines.contains(m)) {
                        unwatchVM(m);
                    }
                });
                watchedVMs.forEach(m -> {
                    if (!watchedVmListenerThreads.get(m).isAlive()) {
                        unwatchVM(m);
                    }
                });
                try {
                    shutdownLock.wait(vmListPollPauseMillis);
                } catch (InterruptedException e) {
                }
            }
            watchedVMs.forEach(this::unwatchVM);
        }
        LOGGER.info("VM watcher was stopped");
    }

    private void watchVm(VirtualMachineDescriptor m) {
        ZkVmListener listener = new ZkVmListener(m, vmPollPauseMillis);
        Thread listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();
        watchedVMs.add(m);
        watchedVmListeners.put(m, listener);
        watchedVmListenerThreads.put(m, listenerThread);
        LOGGER.info("Watching VM with PID=[{}]", m.id());
    }

    private void unwatchVM(VirtualMachineDescriptor m) {
        watchedVmListeners.get(m).shutdown();
        try {
            watchedVmListenerThreads.get(m).join(100);
        } catch (InterruptedException e) {
        }
        watchedVmListeners.remove(m);
        watchedVmListenerThreads.remove(m);
        watchedVMs.remove(m);
        LOGGER.info("No longer watching VM with PID=[{}]", m.id());
    }

    public void shutdown() {
        synchronized (shutdownLock) {
            shutdown = true;
            shutdownLock.notifyAll();
        }
    }
}
