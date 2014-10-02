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

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.releng.zkw.functional.Function1V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class ZkVmProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkVmProvider.class);

    private static String ZK_MAIN = "org.apache.zookeeper.server.quorum.QuorumPeerMain";

    public static List<VirtualMachineDescriptor> getZkVmDescriptors() {
        return VirtualMachine.list().stream()
                .filter(d -> d.displayName().startsWith(ZK_MAIN))
                .collect(Collectors.toList());
    }

    public static void withZkVm(VirtualMachineDescriptor descriptor, Function1V<VirtualMachine> handler) {
        VirtualMachine vm = uncheckedAttach(descriptor);
        try {
            handler.apply(vm);
        } finally {
            safeDetach(vm);
        }
    }

    private static VirtualMachine uncheckedAttach(VirtualMachineDescriptor descriptor) {
        try {
            LOGGER.info("Attaching to {} JVM with PID = [{}]...", descriptor.displayName(), descriptor.id());
            VirtualMachine vm = VirtualMachine.attach(descriptor);
            LOGGER.info("Attached to {} JVM with PID = [{}]", descriptor.displayName(), descriptor.id());
            return vm;
        } catch (AttachNotSupportedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void safeDetach(VirtualMachine virtualMachine) {
        try {
            LOGGER.info("Detaching from JVM with PID = [{}]...", virtualMachine.id());
            virtualMachine.detach();
            LOGGER.info("Detached from JVM with PID = [{}]", virtualMachine.id());
        } catch (Exception e) {
            LOGGER.error("JVM detach error", e);
        }
    }

}
