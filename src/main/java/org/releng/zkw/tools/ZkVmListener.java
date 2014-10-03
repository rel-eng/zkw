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
import org.releng.zkw.metrics.JvmMetricsCollector;
import org.releng.zkw.metrics.MetricsCollection;
import org.releng.zkw.metrics.ZkMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.Query;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.releng.zkw.tools.AttributesHelper.getBeanAttributes;
import static org.releng.zkw.tools.AttributesHelper.queryNames;
import static org.releng.zkw.tools.JmxConnectionProvider.withJmxConnector;
import static org.releng.zkw.tools.MBeanServerConnectionProvider.withMBeanServerConnection;
import static org.releng.zkw.tools.ZkVmProvider.withZkVm;

public class ZkVmListener implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkVmListener.class);

    private volatile boolean shutdown = false;
    private final Object shutdownLock = new Object();
    private final VirtualMachineDescriptor vmDescriptor;
    private final long pollPauseMillis;
    private String prefix;

    public ZkVmListener(VirtualMachineDescriptor vmDescriptor, long pollPauseMillis) {
        this.vmDescriptor = vmDescriptor;
        this.pollPauseMillis = pollPauseMillis;
    }

    @Override
    public void run() {
        try {
            doListen();
        } catch (Exception e) {
            LOGGER.error("VM listener error", e);
        }
    }

    private void doListen() {
        LOGGER.info("VM listener is running for VM with PID=[{}]...", vmDescriptor.id());
        withZkVm(vmDescriptor, vm -> withJmxConnector(vm, con -> withMBeanServerConnection(con, mbsc -> {
            MetricsCollection.withMetrics(mc -> {
                synchronized (shutdownLock) {
                    while (!shutdown) {
                        pollVM(mc, mbsc);
                        try {
                            shutdownLock.wait(pollPauseMillis);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
        })));
        LOGGER.info("VM listener for VM with PID=[{}] was stopped", vmDescriptor.id());
    }

    public void shutdown() {
        synchronized (shutdownLock) {
            shutdown = true;
            shutdownLock.notifyAll();
        }
    }

    private void pollVM(MetricsCollection mc, MBeanServerConnection con) {
        JvmMetricsCollector.collectMetrics(getMetricsPrefix(con), con, mc);
        ZkMetricsCollector.collectMetrics(getMetricsPrefix(con), con, mc);
    }

    private String getMetricsPrefix(MBeanServerConnection con) {
        if (prefix != null) {
            return prefix;
        }
        String hostname = getLocalHostName();
        Optional<String> standaloneZkPort = tryResolveStandaloneZKPort(con);
        if (standaloneZkPort.isPresent()) {
            prefix = "one_min." + hostname + ".zookeeper." + standaloneZkPort.get();
        } else {
            prefix = "one_min." + hostname + ".zookeeper." + vmDescriptor.id();
        }
        return prefix;
    }

    private Optional<String> tryResolveStandaloneZKPort(MBeanServerConnection con) {
        Set<ObjectName> zkServerBeanNames = queryNames(con, "org.apache.ZooKeeperService:name0=StandaloneServer_port*",
                Query.isInstanceOf(Query.value("org.apache.zookeeper.server.ZooKeeperServerBean")));
        if (zkServerBeanNames.isEmpty()) {
            return Optional.empty();
        }
        ObjectName name = zkServerBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "ClientPort");
        if(!attributes.containsKey("ClientPort") || attributes.get("ClientPort") == null
                || ((String) attributes.get("ClientPort")).trim().isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(((String) attributes.get("ClientPort")).replaceAll("\\s", "").replace(".", "_").replace(":", "_"));
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
