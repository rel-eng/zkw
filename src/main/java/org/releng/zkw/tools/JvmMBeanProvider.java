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

import javax.management.MBeanServerConnection;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

public final class JvmMBeanProvider {

    private JvmMBeanProvider() {
    }

    public static CompilationMXBean getCompilationMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.COMPILATION_MXBEAN_NAME, CompilationMXBean.class, connection);
    }

    public static MemoryMXBean getMemoryMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class, connection);
    }

    public static RuntimeMXBean getRuntimeMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class, connection);
    }

    public static ThreadMXBean getThreadMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class, connection);
    }

    public static ClassLoadingMXBean getClassLoadingMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class, connection);
    }

    public static OperatingSystemMXBean getOperatingSystemMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class, connection);
    }

    public static GarbageCollectorMXBean getGarbageCollectorMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE, GarbageCollectorMXBean.class, connection);
    }

    public static MemoryManagerMXBean getMemoryManagerMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE, MemoryManagerMXBean.class, connection);
    }

    public static MemoryPoolMXBean getMemoryPoolMXBean(MBeanServerConnection connection) {
        return MBeanProvider.getMBean(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE, MemoryPoolMXBean.class, connection);
    }

}
