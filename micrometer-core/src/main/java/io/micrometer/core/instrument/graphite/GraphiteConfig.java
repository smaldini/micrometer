/**
 * Copyright 2017 Pivotal Software, Inc.
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
package io.micrometer.core.instrument.graphite;

import info.ganglia.gmetric4j.gmetric.GMetric;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Schneider
 */
public interface GraphiteConfig {
    /**
     * Get the value associated with a key.
     *
     * @param k
     *     Key to lookup in the config.
     * @return
     *     Value for the key or null if no key is present.
     */
    String get(String k);

    /**
     * Property prefix to prepend to configuration names.
     */
    default String prefix() {
        return "graphite";
    }

    /**
     * Returns the step size (reporting frequency) to use. The default is 10 seconds, matching the default
     * for the Datadog agent.
     */
    default Duration step() {
        String v = get(prefix() + ".step");
        return v == null ? Duration.ofSeconds(10) : Duration.parse(v);
    }

    default TimeUnit rateUnits() {
        String v = get(prefix() + ".rateUnits");
        return v == null ? TimeUnit.SECONDS : TimeUnit.valueOf(v.toUpperCase());
    }

    default TimeUnit durationUnits() {
        String v = get(prefix() + ".durationUnits");
        return v == null ? TimeUnit.MILLISECONDS : TimeUnit.valueOf(v.toUpperCase());
    }

    default String host() {
        String v = get(prefix() + ".host");
        return (v == null) ? "localhost" : v;
    }

    default int port() {
        String v = get(prefix() + ".port");
        return (v == null) ? 2004 : Integer.parseInt(v);
    }
}
