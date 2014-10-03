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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.releng.zkw.functional.Function1V;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MetricsCollection {

    private final ConcurrentMap<String, Counter> registeredCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Histogram> registeredHistograms = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Meter> registeredMeters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> registeredTimers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, NumericGauge> registeredNumericGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FloatingGauge> registeredFloatingGauges = new ConcurrentHashMap<>();

    private MetricsCollection() {
    }

    public static void withMetrics(Function1V<MetricsCollection> handler) {
        MetricsCollection collection = new MetricsCollection();
        try {
            handler.apply(collection);
        } finally {
            collection.unregisterAllMetrics();
        }
    }

    public Counter counter(String name) {
        return registeredCounters.computeIfAbsent(name, k -> MetricsRegistryHolder.getRegistry().counter(k));
    }

    public Histogram histogram(String name) {
        return registeredHistograms.computeIfAbsent(name, k -> MetricsRegistryHolder.getRegistry().histogram(k));
    }

    public Meter meter(String name) {
        return registeredMeters.computeIfAbsent(name, k -> MetricsRegistryHolder.getRegistry().meter(k));
    }

    public Timer timer(String name) {
        return registeredTimers.computeIfAbsent(name, k -> MetricsRegistryHolder.getRegistry().timer(k));
    }

    public NumericGauge numericGauge(String name) {
        return registeredNumericGauges.computeIfAbsent(name, k -> {
            Gauge gauge = MetricsRegistryHolder.getRegistry().getGauges().get(k);
            if (gauge instanceof NumericGauge) {
                return (NumericGauge) gauge;
            }
            if (gauge == null) {
                try {
                    return MetricsRegistryHolder.getRegistry().register(k, new NumericGauge());
                } catch (IllegalArgumentException e) {
                    Gauge addedGauge = MetricsRegistryHolder.getRegistry().getGauges().get(k);
                    if (addedGauge instanceof NumericGauge) {
                        return (NumericGauge) addedGauge;
                    }
                }
            }
            throw new IllegalArgumentException(k + " is already used for a different type of metric");
        });
    }

    public FloatingGauge floatingGauge(String name) {
        return registeredFloatingGauges.computeIfAbsent(name, k -> {
            Gauge gauge = MetricsRegistryHolder.getRegistry().getGauges().get(k);
            if (gauge instanceof FloatingGauge) {
                return (FloatingGauge) gauge;
            }
            if (gauge == null) {
                try {
                    return MetricsRegistryHolder.getRegistry().register(k, new FloatingGauge());
                } catch (IllegalArgumentException e) {
                    Gauge addedGauge = MetricsRegistryHolder.getRegistry().getGauges().get(k);
                    if (addedGauge instanceof FloatingGauge) {
                        return (FloatingGauge) addedGauge;
                    }
                }
            }
            throw new IllegalArgumentException(k + " is already used for a different type of metric");
        });
    }

    private void unregisterAllMetrics() {
        registeredCounters.keySet().forEach(m -> MetricsRegistryHolder.getRegistry().remove(m));
        registeredHistograms.keySet().forEach(m -> MetricsRegistryHolder.getRegistry().remove(m));
        registeredMeters.keySet().forEach(m -> MetricsRegistryHolder.getRegistry().remove(m));
        registeredTimers.keySet().forEach(m -> MetricsRegistryHolder.getRegistry().remove(m));
        registeredNumericGauges.keySet().forEach(m -> MetricsRegistryHolder.getRegistry().remove(m));
        registeredFloatingGauges.keySet().forEach(m -> MetricsRegistryHolder.getRegistry().remove(m));
    }

}
