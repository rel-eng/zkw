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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import java.io.IOException;
import java.util.Set;

public final class MBeanProvider {

    private MBeanProvider() {
    }

    public static <T> T getMBean(String beanName, Class<T> beanClass, MBeanServerConnection connection) {
        try {
            final ObjectName objectName = new ObjectName(beanName);
            if (!connection.isInstanceOf(objectName, beanClass.getName())) {
                throw new IllegalArgumentException(beanName + " is not an instance of " + beanClass);
            }

            boolean emitter = connection.isInstanceOf(objectName, "javax.management.NotificationEmitter");
            return MBeanServerInvocationHandler.newProxyInstance(connection, objectName, beanClass, emitter);
        } catch (MalformedObjectNameException | InstanceNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<ObjectInstance> queryMBeans(ObjectName objectName, QueryExp queryExp, MBeanServerConnection connection) {
        try {
            return connection.queryMBeans(objectName, queryExp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
