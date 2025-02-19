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

package org.apache.ignite.example.storage;

import static org.apache.ignite.example.ExampleTestUtils.assertConsoleOutputContains;

import java.util.concurrent.TimeUnit;
import org.apache.ignite.example.AbstractExamplesTest;
import org.apache.ignite.internal.storage.pagememory.PersistentPageMemoryStorageEngine;
import org.apache.ignite.internal.storage.pagememory.VolatilePageMemoryStorageEngine;
import org.apache.ignite.internal.storage.pagememory.configuration.schema.PersistentPageMemoryStorageEngineConfiguration;
import org.apache.ignite.internal.storage.pagememory.configuration.schema.VolatilePageMemoryStorageEngineConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * For testing examples demonstrating work with {@link VolatilePageMemoryStorageEngine} and {@link PersistentPageMemoryStorageEngine}.
 */
@Disabled("https://issues.apache.org/jira/browse/IGNITE-17197")
public class ItPageMemoryStorageExampleTest extends AbstractExamplesTest {
    @Test
    public void testPersistentExample() throws Exception {
        addPersistentDataRegionConfig("persistent");

        assertConsoleOutputContains(PersistentPageMemoryStorageExample::main, EMPTY_ARGS,
                "\nAll accounts:\n"
                        + "    1, John, Doe, 1000.0\n"
                        + "    2, Jane, Roe, 2000.0\n"
                        + "    3, Mary, Major, 1500.0\n"
                        + "    4, Richard, Miles, 1450.0\n"
        );
    }

    @Test
    public void testInMemoryExample() throws Exception {
        addVolatileDataRegionConfig("in-memory");

        assertConsoleOutputContains(VolatilePageMemoryStorageExample::main, EMPTY_ARGS,
                "\nAll accounts:\n"
                        + "    1, John, Doe, 1000.0\n"
                        + "    2, Jane, Roe, 2000.0\n"
                        + "    3, Mary, Major, 1500.0\n"
                        + "    4, Richard, Miles, 1450.0\n"
        );
    }

    private void addVolatileDataRegionConfig(String name) throws Exception {
        ignite.clusterConfiguration().getConfiguration(VolatilePageMemoryStorageEngineConfiguration.KEY)
                .regions()
                .change(regionsChange -> regionsChange.create(name, c -> {}))
                .get(1, TimeUnit.SECONDS);
    }

    private void addPersistentDataRegionConfig(String name) throws Exception {
        ignite.clusterConfiguration().getConfiguration(PersistentPageMemoryStorageEngineConfiguration.KEY)
                .regions()
                .change(regionsChange -> regionsChange.create(name, c -> {}))
                .get(1, TimeUnit.SECONDS);
    }
}
