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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.ReflectionException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        try {
            Set<ObjectName> names =  con.queryNames(new ObjectName("org.apache.ZooKeeperService:name0=StandaloneServer_port*"),
                    Query.isInstanceOf(Query.value("org.apache.zookeeper.server.ZooKeeperServerBean")));
            if (names.isEmpty()) {
                return Optional.empty();
            }
            ObjectName name = names.iterator().next();
            MBeanInfo info = con.getMBeanInfo(name);
            Set<String> readableAttributes = Arrays.asList(info.getAttributes()).stream()
                    .filter(MBeanAttributeInfo::isReadable).map(MBeanFeatureInfo::getName)
                    .collect(Collectors.toSet());
            if (!readableAttributes.contains("ClientPort")) {
                return Optional.empty();
            }
            String clientPort = (String) con.getAttribute(name, "ClientPort");
            if (clientPort == null || clientPort.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(clientPort.replaceAll("\\s", "").replace(".", "_").replace(":", "_"));
        } catch (IOException | MalformedObjectNameException | IntrospectionException | ReflectionException | InstanceNotFoundException | AttributeNotFoundException | MBeanException e) {
            throw new RuntimeException(e);
        }
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
