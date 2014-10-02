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

import org.releng.zkw.functional.Function1V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import java.io.IOException;

public final class MBeanServerConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MBeanServerConnectionProvider.class);

    private MBeanServerConnectionProvider() {
    }

    public static void withMBeanServerConnection(JMXConnector jmxConnector, Function1V<MBeanServerConnection> handler) {
        MBeanServerConnection mBeanServerConnection = connectToMBeanServerUnchecked(jmxConnector);
        handler.apply(mBeanServerConnection);
    }

    private static MBeanServerConnection connectToMBeanServerUnchecked(JMXConnector jmxConnector) {
        try {
            LOGGER.info("Connecting to MBean server with connection [{}]...", jmxConnector.getConnectionId());
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            LOGGER.info("Connected to MBean server with connection [{}]", jmxConnector.getConnectionId());
            return mBeanServerConnection;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
