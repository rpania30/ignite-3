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

package org.apache.ignite.internal.storage.pagememory.index;

import org.apache.ignite.configuration.schemas.table.TablesConfiguration;
import org.apache.ignite.internal.configuration.testframework.ConfigurationExtension;
import org.apache.ignite.internal.configuration.testframework.InjectConfiguration;
import org.apache.ignite.internal.pagememory.io.PageIoRegistry;
import org.apache.ignite.internal.storage.index.AbstractHashIndexStorageTest;
import org.apache.ignite.internal.storage.pagememory.VolatilePageMemoryStorageEngine;
import org.apache.ignite.internal.storage.pagememory.VolatilePageMemoryTableStorage;
import org.apache.ignite.internal.storage.pagememory.configuration.schema.VolatilePageMemoryStorageEngineConfiguration;
import org.apache.ignite.internal.util.IgniteUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Hash index test implementation for volatile page memory storage.
 */
@ExtendWith(ConfigurationExtension.class)
class VolatilePageMemoryHashIndexStorageTest extends AbstractHashIndexStorageTest {
    private VolatilePageMemoryStorageEngine engine;

    private VolatilePageMemoryTableStorage table;

    @BeforeEach
    void setUp(
            @InjectConfiguration
            VolatilePageMemoryStorageEngineConfiguration engineConfig,
            @InjectConfiguration(
                    value = "mock.tables.foo.dataStorage.name = " + VolatilePageMemoryStorageEngine.ENGINE_NAME
            )
            TablesConfiguration tablesConfig
    ) {
        PageIoRegistry ioRegistry = new PageIoRegistry();

        ioRegistry.loadFromServiceLoader();

        engine = new VolatilePageMemoryStorageEngine(engineConfig, ioRegistry);

        engine.start();

        table = engine.createMvTable(tablesConfig.tables().get("foo"), tablesConfig);

        table.start();

        initialize(table, tablesConfig);
    }

    @AfterEach
    void tearDown() throws Exception {
        IgniteUtils.closeAll(
                table == null ? null : table::stop,
                engine == null ? null : engine::stop
        );
    }

    //TODO IGNITE-17626 Enable the test.
    @Disabled
    @Override
    public void testDestroy() {
    }
}
