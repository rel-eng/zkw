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
package org.releng.zkw.metrics;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.util.Map;
import java.util.Set;

import static org.releng.zkw.tools.AttributesHelper.getBeanAttributes;
import static org.releng.zkw.tools.AttributesHelper.queryNames;

public class JvmMetricsCollector {

    public static void collectMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        collectMemoryPoolMetrics(prefix, con, metrics);
        collectMemoryMetrics(prefix, con, metrics);
        collectGCMetrics(prefix, con, metrics);
        collectThreadMetrics(prefix, con, metrics);
        collectBufferPoolMetrics(prefix, con, metrics);
        collectCompilationMetrics(prefix, con, metrics);
        collectClassLoadingMetrics(prefix, con, metrics);
        collectOSMetrics(prefix, con, metrics);
    }

    private static void collectMemoryPoolMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> memoryPoolBeanNames = queryNames(con, "java.lang:type=MemoryPool,name=*", null);
        memoryPoolBeanNames.forEach(n -> {
            Map<String, Object> attributes = getBeanAttributes(con, n, "Name", "Usage", "PeakUsage", "CollectionUsage",
                    "UsageThreshold", "UsageThresholdCount", "UsageThresholdSupported", "CollectionUsageThreshold",
                    "CollectionUsageThresholdCount", "CollectionUsageThresholdSupported", "Valid");
            if (!attributes.containsKey("Name") || !attributes.containsKey("Valid")) {
                return;
            }
            String poolName = ((String) attributes.get("Name")).replaceAll("\\s", "");
            Boolean valid = (Boolean) attributes.get("Valid");
            if (!valid) {
                return;
            }
            if (attributes.containsKey("CollectionUsage") && attributes.get("CollectionUsage") != null) {
                CompositeData collectionUsage = (CompositeData) attributes.get("CollectionUsage");
                if (collectionUsage.containsKey("committed")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "collectionUsage.committed"))
                            .setValue((Long) collectionUsage.get("committed"));
                }
                if (collectionUsage.containsKey("max")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "collectionUsage.max"))
                            .setValue((Long) collectionUsage.get("max"));
                }
                if (collectionUsage.containsKey("init")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "collectionUsage.init"))
                            .setValue((Long) collectionUsage.get("init"));
                }
                if (collectionUsage.containsKey("used")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "collectionUsage.used"))
                            .setValue((Long) collectionUsage.get("used"));
                }
            }
            if (attributes.containsKey("PeakUsage") && attributes.get("PeakUsage") != null) {
                CompositeData collectionUsage = (CompositeData) attributes.get("PeakUsage");
                if (collectionUsage.containsKey("committed")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "peakUsage.committed"))
                            .setValue((Long) collectionUsage.get("committed"));
                }
                if (collectionUsage.containsKey("max")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "peakUsage.max"))
                            .setValue((Long) collectionUsage.get("max"));
                }
                if (collectionUsage.containsKey("init")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "peakUsage.init"))
                            .setValue((Long) collectionUsage.get("init"));
                }
                if (collectionUsage.containsKey("used")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "peakUsage.used"))
                            .setValue((Long) collectionUsage.get("used"));
                }
            }
            if (attributes.containsKey("Usage") && attributes.get("Usage") != null) {
                CompositeData collectionUsage = (CompositeData) attributes.get("Usage");
                if (collectionUsage.containsKey("committed")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "usage.committed"))
                            .setValue((Long) collectionUsage.get("committed"));
                }
                if (collectionUsage.containsKey("max")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "usage.max"))
                            .setValue((Long) collectionUsage.get("max"));
                }
                if (collectionUsage.containsKey("init")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "usage.init"))
                            .setValue((Long) collectionUsage.get("init"));
                }
                if (collectionUsage.containsKey("used")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "usage.used"))
                            .setValue((Long) collectionUsage.get("used"));
                }
            }
            if (attributes.containsKey("UsageThresholdSupported") && (Boolean) attributes.get("UsageThresholdSupported")) {
                if (attributes.containsKey("UsageThreshold")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "usageThreshold"))
                            .setValue((Long) attributes.get("UsageThreshold"));
                }
                if (attributes.containsKey("UsageThresholdCount")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "usageThresholdCount"))
                            .setValue((Long) attributes.get("UsageThresholdCount"));
                }
            }
            if (attributes.containsKey("CollectionUsageThresholdSupported")
                    && (Boolean) attributes.get("CollectionUsageThresholdSupported"))
            {
                if (attributes.containsKey("CollectionUsageThreshold")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "collectionUsageThreshold"))
                            .setValue((Long) attributes.get("CollectionUsageThreshold"));
                }
                if (attributes.containsKey("CollectionUsageThresholdCount")) {
                    metrics.numericGauge(memoryPoolMetricName(prefix, poolName, "collectionUsageThresholdCount"))
                            .setValue((Long) attributes.get("CollectionUsageThresholdCount"));
                }
            }
        });
    }

    private static void collectMemoryMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> memoryBeanNames = queryNames(con, "java.lang:type=Memory", null);
        if (memoryBeanNames.isEmpty()) {
            return;
        }
        ObjectName name = memoryBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "ObjectPendingFinalizationCount",
                "HeapMemoryUsage", "NonHeapMemoryUsage");
        if (attributes.containsKey("HeapMemoryUsage") && attributes.get("HeapMemoryUsage") != null) {
            CompositeData heapMemoryUsage = (CompositeData) attributes.get("HeapMemoryUsage");
            if (heapMemoryUsage.containsKey("committed")) {
                metrics.numericGauge(memoryMetricName(prefix, "heap.committed"))
                        .setValue((Long) heapMemoryUsage.get("committed"));
            }
            if (heapMemoryUsage.containsKey("init")) {
                metrics.numericGauge(memoryMetricName(prefix, "heap.init"))
                        .setValue((Long) heapMemoryUsage.get("init"));
            }
            if (heapMemoryUsage.containsKey("max")) {
                metrics.numericGauge(memoryMetricName(prefix, "heap.max"))
                        .setValue((Long) heapMemoryUsage.get("max"));
            }
            if (heapMemoryUsage.containsKey("used")) {
                metrics.numericGauge(memoryMetricName(prefix, "heap.used"))
                        .setValue((Long) heapMemoryUsage.get("used"));
            }
        }
        if (attributes.containsKey("NonHeapMemoryUsage") && attributes.get("NonHeapMemoryUsage") != null) {
            CompositeData nonHeapMemoryUsage = (CompositeData) attributes.get("NonHeapMemoryUsage");
            if (nonHeapMemoryUsage.containsKey("committed")) {
                metrics.numericGauge(memoryMetricName(prefix, "nonHeap.committed"))
                        .setValue((Long) nonHeapMemoryUsage.get("committed"));
            }
            if (nonHeapMemoryUsage.containsKey("init")) {
                metrics.numericGauge(memoryMetricName(prefix, "nonHeap.init"))
                        .setValue((Long) nonHeapMemoryUsage.get("init"));
            }
            if (nonHeapMemoryUsage.containsKey("max")) {
                metrics.numericGauge(memoryMetricName(prefix, "nonHeap.max"))
                        .setValue((Long) nonHeapMemoryUsage.get("max"));
            }
            if (nonHeapMemoryUsage.containsKey("used")) {
                metrics.numericGauge(memoryMetricName(prefix, "nonHeap.used"))
                        .setValue((Long) nonHeapMemoryUsage.get("used"));
            }
        }
        if (attributes.containsKey("ObjectPendingFinalizationCount")) {
            metrics.numericGauge(memoryMetricName(prefix, "objectPendingFinalizationCount"))
                    .setValue((Integer) attributes.get("ObjectPendingFinalizationCount"));
        }
    }

    private static void collectGCMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> gcBeanNames = queryNames(con, "java.lang:type=GarbageCollector,name=*", null);
        gcBeanNames.forEach(n -> {
            Map<String, Object> attributes = getBeanAttributes(con, n, "Name", "CollectionCount", "CollectionTime",
                    "Valid");
            if (!attributes.containsKey("Name") || !attributes.containsKey("Valid")) {
                return;
            }
            String gcName = ((String) attributes.get("Name")).replaceAll("\\s", "");
            Boolean valid = (Boolean) attributes.get("Valid");
            if (!valid) {
                return;
            }
            if (attributes.containsKey("CollectionCount")) {
                metrics.numericGauge(gcMetricName(prefix, gcName, "collectionCount"))
                        .setValue((Long) attributes.get("CollectionCount"));
            }
            if (attributes.containsKey("CollectionTime")) {
                metrics.numericGauge(gcMetricName(prefix, gcName, "collectionTime"))
                        .setValue((Long) attributes.get("CollectionTime"));
            }
        });
    }

    private static void collectThreadMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> threadingBeanNames = queryNames(con, "java.lang:type=Threading", null);
        if (threadingBeanNames.isEmpty()) {
            return;
        }
        ObjectName name = threadingBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "ThreadCount",
                "TotalStartedThreadCount", "PeakThreadCount", "DaemonThreadCount");
        if (attributes.containsKey("DaemonThreadCount")) {
            metrics.numericGauge(threadMetricName(prefix, "daemonThreadCount"))
                    .setValue((Integer) attributes.get("DaemonThreadCount"));
        }
        if (attributes.containsKey("PeakThreadCount")) {
            metrics.numericGauge(threadMetricName(prefix, "peakThreadCount"))
                    .setValue((Integer) attributes.get("PeakThreadCount"));
        }
        if (attributes.containsKey("ThreadCount")) {
            metrics.numericGauge(threadMetricName(prefix, "threadCount"))
                    .setValue((Integer) attributes.get("ThreadCount"));
        }
        if (attributes.containsKey("TotalStartedThreadCount")) {
            metrics.numericGauge(threadMetricName(prefix, "totalStartedThreadCount"))
                    .setValue((Long) attributes.get("TotalStartedThreadCount"));
        }
    }

    private static void collectBufferPoolMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> booferPoolBeanNames = queryNames(con, "java.nio:type=BufferPool,name=*", null);
        booferPoolBeanNames.forEach(n -> {
            Map<String, Object> attributes = getBeanAttributes(con, n, "Name", "Count", "TotalCapacity", "MemoryUsed");
            if (!attributes.containsKey("Name")) {
                return;
            }
            String poolName = ((String) attributes.get("Name")).replaceAll("\\s", "");
            if (attributes.containsKey("Count")) {
                metrics.numericGauge(bufferPoolMetricName(prefix, poolName, "count"))
                        .setValue((Long) attributes.get("Count"));
            }
            if (attributes.containsKey("TotalCapacity")) {
                metrics.numericGauge(bufferPoolMetricName(prefix, poolName, "totalCapacity"))
                        .setValue((Long) attributes.get("TotalCapacity"));
            }
            if (attributes.containsKey("MemoryUsed")) {
                metrics.numericGauge(bufferPoolMetricName(prefix, poolName, "memoryUsed"))
                        .setValue((Long) attributes.get("MemoryUsed"));
            }
        });
    }

    private static void collectCompilationMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> compilationBeanNames = queryNames(con, "java.lang:type=Compilation", null);
        if (compilationBeanNames.isEmpty()) {
            return;
        }
        ObjectName name = compilationBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "TotalCompilationTime");
        if (attributes.containsKey("TotalCompilationTime")) {
            metrics.numericGauge(compilationMetricName(prefix, "totalCompilationTime"))
                    .setValue((Long) attributes.get("TotalCompilationTime"));
        }
    }

    private static void collectClassLoadingMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> classLoadingBeanNames = queryNames(con, "java.lang:type=ClassLoading", null);
        if (classLoadingBeanNames.isEmpty()) {
            return;
        }
        ObjectName name = classLoadingBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "TotalLoadedClassCount", "LoadedClassCount",
                "UnloadedClassCount");
        if (attributes.containsKey("TotalLoadedClassCount")) {
            metrics.numericGauge(classLoadingMetricName(prefix, "totalLoadedClassCount"))
                    .setValue((Long) attributes.get("TotalLoadedClassCount"));
        }
        if (attributes.containsKey("LoadedClassCount")) {
            metrics.numericGauge(classLoadingMetricName(prefix, "loadedClassCount"))
                    .setValue((Integer) attributes.get("LoadedClassCount"));
        }
        if (attributes.containsKey("UnloadedClassCount")) {
            metrics.numericGauge(classLoadingMetricName(prefix, "unloadedClassCount"))
                    .setValue((Long) attributes.get("UnloadedClassCount"));
        }
    }

    private static void collectOSMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> osBeanNames = queryNames(con, "java.lang:type=OperatingSystem", null);
        if (osBeanNames.isEmpty()) {
            return;
        }
        ObjectName name = osBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "OpenFileDescriptorCount", "MaxFileDescriptorCount",
                "CommittedVirtualMemorySize", "TotalSwapSpaceSize", "FreeSwapSpaceSize", "ProcessCpuTime",
                "FreePhysicalMemorySize", "TotalPhysicalMemorySize", "SystemCpuLoad", "ProcessCpuLoad",
                "SystemLoadAverage");
        if (attributes.containsKey("SystemLoadAverage")) {
            metrics.floatingGauge(osMetricName(prefix, "systemLoadAverage"))
                    .setValue((Double) attributes.get("SystemLoadAverage"));
        }
        if (attributes.containsKey("ProcessCpuLoad")) {
            metrics.floatingGauge(osMetricName(prefix, "processCpuLoad"))
                    .setValue((Double) attributes.get("ProcessCpuLoad"));
        }
        if (attributes.containsKey("SystemCpuLoad")) {
            metrics.floatingGauge(osMetricName(prefix, "systemCpuLoad"))
                    .setValue((Double) attributes.get("SystemCpuLoad"));
        }
        if (attributes.containsKey("TotalPhysicalMemorySize")) {
            metrics.numericGauge(osMetricName(prefix, "totalPhysicalMemorySize"))
                    .setValue((Long) attributes.get("TotalPhysicalMemorySize"));
        }
        if (attributes.containsKey("FreePhysicalMemorySize")) {
            metrics.numericGauge(osMetricName(prefix, "freePhysicalMemorySize"))
                    .setValue((Long) attributes.get("FreePhysicalMemorySize"));
        }
        if (attributes.containsKey("ProcessCpuTime")) {
            metrics.numericGauge(osMetricName(prefix, "processCpuTime"))
                    .setValue((Long) attributes.get("ProcessCpuTime"));
        }
        if (attributes.containsKey("FreeSwapSpaceSize")) {
            metrics.numericGauge(osMetricName(prefix, "freeSwapSpaceSize"))
                    .setValue((Long) attributes.get("FreeSwapSpaceSize"));
        }
        if (attributes.containsKey("TotalSwapSpaceSize")) {
            metrics.numericGauge(osMetricName(prefix, "totalSwapSpaceSize"))
                    .setValue((Long) attributes.get("TotalSwapSpaceSize"));
        }
        if (attributes.containsKey("CommittedVirtualMemorySize")) {
            metrics.numericGauge(osMetricName(prefix, "committedVirtualMemorySize"))
                    .setValue((Long) attributes.get("CommittedVirtualMemorySize"));
        }
        if (attributes.containsKey("MaxFileDescriptorCount")) {
            metrics.numericGauge(osMetricName(prefix, "maxFileDescriptorCount"))
                    .setValue((Long) attributes.get("MaxFileDescriptorCount"));
        }
        if (attributes.containsKey("OpenFileDescriptorCount")) {
            metrics.numericGauge(osMetricName(prefix, "openFileDescriptorCount"))
                    .setValue((Long) attributes.get("OpenFileDescriptorCount"));
        }
    }

    private static String memoryPoolMetricName(String prefix, String pool, String metric) {
        return prefix + ".memoryPools." + pool + "." + metric;
    }

    private static String memoryMetricName(String prefix, String metric) {
        return prefix + ".memory." + metric;
    }

    private static String gcMetricName(String prefix, String gc, String metric) {
        return prefix + ".gc." + gc + "." + metric;
    }

    private static String threadMetricName(String prefix, String metric) {
        return prefix + ".thread." + metric;
    }

    private static String bufferPoolMetricName(String prefix, String pool, String metric) {
        return prefix + ".bufferPools." + pool + "." + metric;
    }

    private static String compilationMetricName(String prefix, String metric) {
        return prefix + ".compilation." + metric;
    }

    private static String classLoadingMetricName(String prefix, String metric) {
        return prefix + ".classLoading." + metric;
    }

    private static String osMetricName(String prefix, String metric) {
        return prefix + ".os." + metric;
    }

}
