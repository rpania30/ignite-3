/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.rest.api.metric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST representation of MetricSource.
 */
@Schema(name = "MetricSource")
public class MetricSourceDto {
    /** Name of the metric source. */
    private final String name;

    /** Enabled. */
    private final boolean enabled;

    /**
     * Constructor.
     *
     * @param name metric source name
     * @param enabled flags showing whether this metric source is enabled or not
     */
    @JsonCreator
    public MetricSourceDto(
            @JsonProperty("name") String name,
            @JsonProperty("enabled") boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    /**
     * Returns the metric source name.
     *
     * @return metric source name
     */
    @JsonGetter("name")
    public String name() {
        return name;
    }

    /**
     * Returns the status of the metric source.
     *
     * @return {@code true} if metrics are enabled, otherwise - {@code false}
     */
    @JsonGetter("enabled")
    public boolean enabled() {
        return enabled;
    }
}
