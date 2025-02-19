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

package org.apache.ignite.internal.configuration;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.ignite.configuration.RootKey;
import org.apache.ignite.configuration.annotation.Config;
import org.apache.ignite.configuration.annotation.ConfigurationRoot;
import org.apache.ignite.configuration.annotation.InternalConfiguration;
import org.apache.ignite.configuration.annotation.PolymorphicConfigInstance;
import org.apache.ignite.configuration.validation.Validator;
import org.apache.ignite.internal.configuration.hocon.HoconConverter;
import org.apache.ignite.internal.configuration.storage.ConfigurationStorage;
import org.apache.ignite.internal.manager.IgniteComponent;
import org.intellij.lang.annotations.Language;

/**
 * Configuration manager is responsible for handling configuration lifecycle and provides configuration API.
 */
public class ConfigurationManager implements IgniteComponent {
    /** Configuration registry. */
    private final ConfigurationRegistry registry;

    /**
     * Constructor.
     *
     * @param rootKeys                    Configuration root keys.
     * @param validators                  Validators.
     * @param storage                     Configuration storage.
     * @param internalSchemaExtensions    Internal extensions ({@link InternalConfiguration}) of configuration schemas ({@link
     *                                    ConfigurationRoot} and {@link Config}).
     * @param polymorphicSchemaExtensions Polymorphic extensions ({@link PolymorphicConfigInstance}) of configuration schemas.
     * @throws IllegalArgumentException If the configuration type of the root keys is not equal to the storage type, or if the schema or its
     *                                  extensions are not valid.
     */
    public ConfigurationManager(
            Collection<RootKey<?, ?>> rootKeys,
            Map<Class<? extends Annotation>, Set<Validator<? extends Annotation, ?>>> validators,
            ConfigurationStorage storage,
            Collection<Class<?>> internalSchemaExtensions,
            Collection<Class<?>> polymorphicSchemaExtensions
    ) {
        registry = new ConfigurationRegistry(
                rootKeys,
                validators,
                storage,
                internalSchemaExtensions,
                polymorphicSchemaExtensions
        );
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        registry.start();
    }

    /** {@inheritDoc} */
    @Override
    public void stop() throws Exception {
        // TODO: IGNITE-15161 Implement component's stop.
        registry.stop();
    }

    /**
     * Bootstrap configuration manager with customer user cfg.
     *
     * @param hoconStr Customer configuration in hocon format.
     * @throws InterruptedException If thread is interrupted during bootstrap.
     * @throws ExecutionException   If configuration update failed for some reason.
     */
    public void bootstrap(@Language("HOCON") String hoconStr) throws InterruptedException, ExecutionException {
        ConfigObject hoconCfg = ConfigFactory.parseString(hoconStr).root();

        registry.change(HoconConverter.hoconSource(hoconCfg)).get();
    }

    /**
     * Get configuration registry.
     *
     * @return Configuration registry.
     */
    public ConfigurationRegistry configurationRegistry() {
        return registry;
    }
}
