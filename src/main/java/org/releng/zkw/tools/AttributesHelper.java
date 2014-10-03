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

import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AttributesHelper {

    public static Map<String, Object> getBeanAttributes(MBeanServerConnection con, ObjectName beanName,
            String... requestedAtributes)
    {
        Set<String> attributes = findReadableAttributes(con, beanName);
        String[] attributesToQuery = collectAttributesToQuery(attributes, requestedAtributes);
        try {
            AttributeList receivedAttributes = con.getAttributes(beanName, attributesToQuery);
            Map<String, Object> result = new HashMap<>();
            receivedAttributes.asList().forEach(a -> result.put(a.getName(), a.getValue()));
            return result;
        } catch (InstanceNotFoundException | ReflectionException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Set<ObjectName> queryNames(MBeanServerConnection con, String name, QueryExp query) {
        try {
            return con.queryNames(buildObjectName(name), query);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> findReadableAttributes(MBeanServerConnection con, ObjectName beanName) {
        try {
            MBeanInfo info = con.getMBeanInfo(beanName);
            return Arrays.asList(info.getAttributes()).stream()
                    .filter(MBeanAttributeInfo::isReadable).map(MBeanFeatureInfo::getName)
                    .collect(Collectors.toSet());
        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] collectAttributesToQuery(Set<String> existingAttributes, String... requestedAtributes) {
        Set<String> attributesToQuery = new HashSet<>();
        for (String attribute : requestedAtributes) {
            if (existingAttributes.contains(attribute)) {
                attributesToQuery.add(attribute);
            }
        }
        String[] result = new String[attributesToQuery.size()];
        return attributesToQuery.toArray(result);
    }

    private static ObjectName buildObjectName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

}
