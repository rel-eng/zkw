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
import javax.management.Query;
import java.util.Map;
import java.util.Set;

import static org.releng.zkw.tools.AttributesHelper.getBeanAttributes;
import static org.releng.zkw.tools.AttributesHelper.queryNames;

public class ZkMetricsCollector {

    public static void collectMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        collectStandaloneServerMetrics(prefix, con, metrics);
        collectStandaloneServerDataTreeMetrics(prefix, con, metrics);
    }

    private static void collectStandaloneServerMetrics(String prefix, MBeanServerConnection con, MetricsCollection metrics) {
        Set<ObjectName> zkServerBeanNames = queryNames(con, "org.apache.ZooKeeperService:name0=StandaloneServer_port*",
                Query.isInstanceOf(Query.value("org.apache.zookeeper.server.ZooKeeperServerBean")));
        if (zkServerBeanNames.isEmpty()) {
            return;
        }
        ObjectName name = zkServerBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "NumAliveConnections", "OutstandingRequests",
                "PacketsReceived", "PacketsSent", "MinRequestLatency", "AvgRequestLatency", "MaxRequestLatency");
        if (attributes.containsKey("NumAliveConnections")) {
            metrics.numericGauge(standaloneServerMetricName(prefix, "numAliveConnections"))
                    .setValue((Long) attributes.get("NumAliveConnections"));
        }
        if (attributes.containsKey("OutstandingRequests")) {
            metrics.numericGauge(standaloneServerMetricName(prefix, "outstandingRequests"))
                    .setValue((Long) attributes.get("OutstandingRequests"));
        }
        if (attributes.containsKey("PacketsReceived")) {
            metrics.numericGauge(standaloneServerMetricName(prefix, "packetsReceived"))
                    .setValue((Long) attributes.get("PacketsReceived"));
        }
        if (attributes.containsKey("PacketsSent")) {
            metrics.numericGauge(standaloneServerMetricName(prefix, "packetsSent"))
                    .setValue((Long) attributes.get("PacketsSent"));
        }
        if (attributes.containsKey("MinRequestLatency")) {
            metrics.numericGauge(standaloneServerMetricName(prefix, "minRequestLatency"))
                    .setValue((Long) attributes.get("MinRequestLatency"));
        }
        if (attributes.containsKey("AvgRequestLatency")) {
            metrics.numericGauge(standaloneServerMetricName(prefix, "avgRequestLatency"))
                    .setValue((Long) attributes.get("AvgRequestLatency"));
        }
        if (attributes.containsKey("MaxRequestLatency")) {
            metrics.numericGauge(standaloneServerMetricName(prefix, "maxRequestLatency"))
                    .setValue((Long) attributes.get("MaxRequestLatency"));
        }
    }

    private static void collectStandaloneServerDataTreeMetrics(String prefix, MBeanServerConnection con,
            MetricsCollection metrics)
    {
        Set<ObjectName> zkDataTreeBeanNames = queryNames(con,
                "org.apache.ZooKeeperService:name0=StandaloneServer_port*,name1=InMemoryDataTree",
                Query.isInstanceOf(Query.value("org.apache.zookeeper.server.DataTreeBean")));
        if (zkDataTreeBeanNames.isEmpty()) {
            return;
        }
        ObjectName name = zkDataTreeBeanNames.iterator().next();
        Map<String, Object> attributes = getBeanAttributes(con, name, "NodeCount", "WatchCount");
        if (attributes.containsKey("NodeCount")) {
            metrics.numericGauge(standaloneServerNodeTreeMetricName(prefix, "nodeCount"))
                    .setValue((Integer) attributes.get("NodeCount"));
        }
        if (attributes.containsKey("WatchCount")) {
            metrics.numericGauge(standaloneServerNodeTreeMetricName(prefix, "watchCount"))
                    .setValue((Integer) attributes.get("WatchCount"));
        }
    }

    private static String standaloneServerMetricName(String prefix, String metric) {
        return prefix + ".zk.standaloneServer." + metric;
    }

    private static String standaloneServerNodeTreeMetricName(String prefix, String metric) {
        return prefix + ".zk.standaloneServer.nodeTree." + metric;
    }

}
