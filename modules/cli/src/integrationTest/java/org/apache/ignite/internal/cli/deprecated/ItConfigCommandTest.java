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

package org.apache.ignite.internal.cli.deprecated;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.ignite.internal.testframework.IgniteTestUtils.testNodeName;
import static org.apache.ignite.internal.testframework.matchers.CompletableFutureMatcher.willCompleteSuccessfully;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgnitionManager;
import org.apache.ignite.internal.app.IgniteImpl;
import org.apache.ignite.internal.testframework.WorkDirectory;
import org.apache.ignite.internal.testframework.WorkDirectoryExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration test for {@code ignite node/cluster config} commands.
 */
@ExtendWith(WorkDirectoryExtension.class)
public class ItConfigCommandTest extends AbstractCliIntegrationTest {
    /** DI context. */
    private ApplicationContext ctx;

    /** Node. */
    private IgniteImpl node;

    @BeforeEach
    void setup(@WorkDirectory Path workDir, TestInfo testInfo) {
        String nodeName = testNodeName(testInfo, 0);

        CompletableFuture<Ignite> future = IgnitionManager.start(nodeName, null, workDir);

        IgnitionManager.init(nodeName, List.of(nodeName), "cluster");

        assertThat(future, willCompleteSuccessfully());

        node = (IgniteImpl) future.join();

        ctx = ApplicationContext.run(Environment.TEST);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        IgnitionManager.stop(testNodeName(testInfo, 0));

        ctx.stop();
    }

    @Test
    public void setAndGetWithManualHost() {
        int exitCode = cmd(ctx).execute(
                "node",
                "config",
                "update",
                "--node-url",
                "http://localhost:" + node.restAddress().port(),
                "network.shutdownQuietPeriod=1"
        );

        assertEquals(0, exitCode);
        assertThat(
                out.toString(UTF_8),
                containsString("Node configuration was updated successfully")

        );

        resetStreams();

        exitCode = cmd(ctx).execute(
                "node",
                "config",
                "show",
                "--node-url",
                "http://localhost:" + node.restAddress().port()
        );

        assertEquals(0, exitCode);

        assertThat(
                out.toString(UTF_8),
                containsString("\"shutdownQuietPeriod\" : 1")
        );
    }

    @Test
    public void setWithWrongData() {
        int exitCode = cmd(ctx).execute(
                "node",
                "config",
                "update",
                "--node-url",
                "http://localhost:" + node.restAddress().port(),
                "network.foo=\"bar\""
        );

        assertEquals(1, exitCode);
        assertThat(
                err.toString(UTF_8),
                containsString("'network' configuration doesn't have the 'foo' sub-configuration")
        );

        resetStreams();

        exitCode = cmd(ctx).execute(
                "node",
                "config",
                "update",
                "--node-url",
                "http://localhost:" + node.restAddress().port(),
                "network.shutdownQuietPeriod=asd"
        );

        assertEquals(1, exitCode);
        assertThat(
                err.toString(UTF_8),
                containsString("'long' is expected as a type for the 'network.shutdownQuietPeriod' configuration value")
        );
    }

    @Test
    public void partialGet() {
        int exitCode = cmd(ctx).execute(
                "node",
                "config",
                "show",
                "--node-url",
                "http://localhost:" + node.restAddress().port(),
                "network"
        );

        assertEquals(0, exitCode);

        assertThat(
                out.toString(UTF_8),
                containsString("\"inbound\"")
        );

        assertFalse(out.toString(UTF_8).contains("\"node\""));
    }

    /**
     * Reset stderr and stdout streams.
     */
    private void resetStreams() {
        err.reset();
        out.reset();
    }

    /**
     * Unescapes quotes in the input string.
     *
     * @param input String.
     * @return String with unescaped quotes.
     */
    private static String unescapeQuotes(String input) {
        return input.replace("\\\"", "\"");
    }

    /**
     * Removes trailing quotes from the input string.
     *
     * @param input String.
     * @return String without trailing quotes.
     */
    private static String removeTrailingQuotes(String input) {
        return input.substring(1, input.length() - 1);
    }
}
