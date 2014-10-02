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

import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
            synchronized (shutdownLock) {
                while (!shutdown) {
                    pollVM(mbsc);
                    try {
                        shutdownLock.wait(pollPauseMillis);
                    } catch (InterruptedException e) {
                    }
                }
            }
        })));
        LOGGER.info("VM listener for VM with PID=[{}] was stopped", vmDescriptor.id());
    }

    public void shutdown() {
        synchronized (shutdownLock) {
            shutdown = true;
            shutdownLock.notifyAll();
        }
    }

    private void pollVM(MBeanServerConnection con) {
        LOGGER.info("-----------------------------------------------");
        Set<ObjectInstance> mBeanInstances = MBeanProvider.queryMBeans(null, null, con);
        mBeanInstances.forEach(inst -> {
            try {
                MBeanInfo info = con.getMBeanInfo(inst.getObjectName());
                LOGGER.info("MBean className = [{}], objectName = [{}], info = [{}]", inst.getClassName(),
                        inst.getObjectName(), info);
                List<String> readableAttributeNames = Arrays.asList(info.getAttributes()).stream()
                        .filter(MBeanAttributeInfo::isReadable).map(MBeanFeatureInfo::getName)
                        .collect(Collectors.toList());
                String[] attributeNamesArray = new String[readableAttributeNames.size()];
                attributeNamesArray = readableAttributeNames.toArray(attributeNamesArray);
                try {
                    AttributeList attrs = con.getAttributes(inst.getObjectName(), attributeNamesArray);
                    attrs.asList().forEach(attr -> {
                        try {
                            LOGGER.info("{}: {}", attr.getName(), attr.getValue());
                        } catch (Throwable e) {
                            LOGGER.error("Attribute print error", e);
                        }
                    });
                } catch (Throwable e) {
                    LOGGER.error("Attribute loading error", e);
                }
                LOGGER.info("-----------------------------------------------");
            } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
