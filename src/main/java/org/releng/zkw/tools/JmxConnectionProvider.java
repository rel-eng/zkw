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

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import org.releng.zkw.functional.Function1V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class JmxConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxConnectionProvider.class);

    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    private JmxConnectionProvider() {
    }

    public static void withJmxConnector(VirtualMachine virtualMachine, Function1V<JMXConnector> handler) {
        String connectionAddress = prepareConnectionAddress(virtualMachine);
        JMXServiceURL jmxServiceURL = prepareJMXServiceURLUnchecked(connectionAddress);
        URLClassLoader classLoader = buildTargetLocalVmClassloader(virtualMachine);
        try {
            JMXConnector jmxConnector = connectToJMXUnchecked(jmxServiceURL, classLoader);
            try {
                JmxConnectionNotificationListener jmxConnectionNotificationListener = new JmxConnectionNotificationListener();
                jmxConnector.addConnectionNotificationListener(jmxConnectionNotificationListener, null, null);
                try {
                    handler.apply(jmxConnector);
                } finally {
                    safeRemoveConnectionNotificationListener(jmxConnector, jmxConnectionNotificationListener);
                }
            } finally {
                safeCloseJMXConnection(jmxConnector);
            }
        } finally {
            safeCloseURLClassloader(classLoader);
        }
    }

    private static String prepareConnectionAddress(VirtualMachine virtualMachine) {
        String connectorAddress = getAgentPropertiesUnchecked(virtualMachine).getProperty(CONNECTOR_ADDRESS);
        if (connectorAddress != null) {
            return connectorAddress;
        }
        String javaHome = getSystemPropertiesUnchecked(virtualMachine).getProperty("java.home");
        String agentPath = javaHome + File.separator + "lib" + File.separator + "management-agent.jar";
        loadAgentUnchecked(virtualMachine, agentPath);
        return getAgentPropertiesUnchecked(virtualMachine).getProperty(CONNECTOR_ADDRESS);
    }

    private static Properties getAgentPropertiesUnchecked(VirtualMachine virtualMachine) {
        try {
            return virtualMachine.getAgentProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties getSystemPropertiesUnchecked(VirtualMachine virtualMachine) {
        try {
            return virtualMachine.getSystemProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadAgentUnchecked(VirtualMachine virtualMachine, String agentPath) {
        try {
            LOGGER.info("Loading management agent from [{}] for JVM with PID=[{}]...", agentPath, virtualMachine.id());
            virtualMachine.loadAgent(agentPath);
            LOGGER.info("Loaded management agent from [{}] for JVM with PID=[{}]", agentPath, virtualMachine.id());
        } catch (AgentLoadException | AgentInitializationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JMXServiceURL prepareJMXServiceURLUnchecked(String connectionAddress) {
        try {
            return new JMXServiceURL(connectionAddress);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static JMXConnector connectToJMXUnchecked(JMXServiceURL jmxServiceURL, ClassLoader classLoader) {
        try {
            LOGGER.info("Establishing JMX connection to [{}]...", jmxServiceURL.toString());
            Map<String, Object> environment = new HashMap<>();
            environment.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, classLoader);
            JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, environment);
            String connectionId = safeGetJmxConnectionId(jmxConnector).orElse("<Undefined>");
            LOGGER.info("Established JMX connection to [{}] with id [{}]", jmxServiceURL.toString(), connectionId);
            return jmxConnector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void safeCloseJMXConnection(JMXConnector jmxConnector) {
        try {
            String connectionId = safeGetJmxConnectionId(jmxConnector).orElse("<Undefined>");
            LOGGER.info("Closing JMX connection [{}]...", connectionId);
            jmxConnector.close();
            LOGGER.info("Closed JMX connection [{}]", connectionId);
        } catch (Exception e) {
            LOGGER.error("JMX connection close error", e);
        }
    }

    private static Optional<String> safeGetJmxConnectionId(JMXConnector jmxConnector) {
        try {
            return Optional.ofNullable(jmxConnector.getConnectionId());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static void safeRemoveConnectionNotificationListener(JMXConnector jmxConnector,
            JmxConnectionNotificationListener jmxConnectionNotificationListener)
    {
        try {
            jmxConnector.removeConnectionNotificationListener(jmxConnectionNotificationListener, null, null);
        } catch (Exception e) {
            LOGGER.error("Error removing connection notification listener", e);
        }
    }

    private static final class JmxConnectionNotificationListener implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object handback) {
            LOGGER.info("JMX connection notification received: {}", notification.toString());
        }

    }

    private static URLClassLoader buildTargetLocalVmClassloader(VirtualMachine virtualMachine) {
        LOGGER.info("Creating class loader for JVM with PID=[{}]...", virtualMachine.id());
        ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader();
        List<String> classpath = getVmClasspath(virtualMachine);
        List<URL> classloaderURLs = classpath.stream().flatMap(e -> resolveClasspathEntry(e).stream())
                .distinct().collect(Collectors.toList());
        URL[] classloaderURLsArray = new URL[classloaderURLs.size()];
        classloaderURLs.toArray(classloaderURLsArray);
        URLClassLoader result = new URLClassLoader(classloaderURLsArray, defaultClassLoader);
        LOGGER.info("Class loader for JVM with PID=[{}] created, using URLs [{}]", virtualMachine.id(),
                Arrays.stream(result.getURLs()).map(URL::toExternalForm).collect(Collectors.joining(", ")));
        return result;
    }

    private static List<String> getVmClasspath(VirtualMachine virtualMachine) {
        Properties targetVmSystemProperties = getSystemPropertiesUnchecked(virtualMachine);
        String classpathSeparator = targetVmSystemProperties.getProperty("path.separator");
        String classpath = targetVmSystemProperties.getProperty("java.class.path");
        String[] splittedClasspath = classpath.split(Pattern.quote(classpathSeparator));
        return Arrays.stream(splittedClasspath).filter(e -> !e.isEmpty()).collect(Collectors.toList());
    }

    private static List<URL> resolveClasspathEntry(String entry) {
        if (entry.toLowerCase().endsWith("*.jar") || entry.endsWith("*")) {
            return resolveWildcardClasspathEntry(entry);
        }
        File entryFile = new File(entry);
        if (!entryFile.exists()) {
            return new ArrayList<>();
        }
        List<URL> result = new ArrayList<>();
        result.add(toURL(entryFile));
        return result;
    }

    private static List<URL> resolveWildcardClasspathEntry(String entry) {
        String dirPath;
        if (entry.toLowerCase().endsWith("*.jar")) {
            dirPath = entry.substring(0, entry.length() - 5);
        } else {
            dirPath = entry.substring(0, entry.length() - 1);
        }
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dir.list((f, n) -> f.isFile() && n.toLowerCase().endsWith(".jar")))
                .map(n -> toURL(new File(dir, n))).collect(Collectors.toList());
    }

    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void safeCloseURLClassloader(URLClassLoader urlClassLoader) {
        try {
            LOGGER.info("Closing URL class loader...");
            urlClassLoader.close();
            LOGGER.info("Closed URL class loader");
        } catch (IOException e) {
            LOGGER.error("Error closing class loader", e);
        }
    }

}
