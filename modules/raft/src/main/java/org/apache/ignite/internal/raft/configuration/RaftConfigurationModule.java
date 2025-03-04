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

package org.apache.ignite.internal.raft.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ignite.configuration.RootKey;
import org.apache.ignite.configuration.annotation.ConfigurationType;
import org.apache.ignite.configuration.schemas.table.EntryCountBudgetConfigurationSchema;
import org.apache.ignite.configuration.schemas.table.UnlimitedBudgetConfigurationSchema;
import org.apache.ignite.internal.configuration.ConfigurationModule;

/**
 * {@link ConfigurationModule} for node-local configuration provided by ignite-raft.
 */
public class RaftConfigurationModule implements ConfigurationModule {
    @Override
    public ConfigurationType type() {
        return ConfigurationType.LOCAL;
    }

    @Override
    public Collection<RootKey<?, ?>> rootKeys() {
        return Collections.singleton(RaftConfiguration.KEY);
    }

    @Override
    public Collection<Class<?>> polymorphicSchemaExtensions() {
        return List.of(
                UnlimitedBudgetConfigurationSchema.class,
                EntryCountBudgetConfigurationSchema.class
        );
    }
}
