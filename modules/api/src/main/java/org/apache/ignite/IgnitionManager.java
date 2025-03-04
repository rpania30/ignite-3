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

package org.apache.ignite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.lang.IgniteException;
import org.jetbrains.annotations.Nullable;

/**
 * Service loader based implementation of an entry point for handling grid lifecycle.
 */
public class IgnitionManager {
    /**
     * Loaded Ignition instance.
     *
     * <p>Concurrent access is guarded by the {@link Class} instance.
     */
    @Nullable
    private static Ignition ignition;

    /**
     * Starts an Ignite node with an optional bootstrap configuration from an input stream with HOCON configs.
     *
     * <p>When this method returns, the node is partially started and ready to accept the init command (that is, its
     * REST endpoint is functional).
     *
     * @param nodeName Name of the node. Must not be {@code null}.
     * @param configStr Optional node configuration based on
     *      {@link org.apache.ignite.configuration.schemas.network.NetworkConfigurationSchema}.
     *      Following rules are used for applying the configuration properties:
     *      <ol>
     *        <li>Specified property overrides existing one or just applies itself if it wasn't
     *            previously specified.</li>
     *        <li>All non-specified properties either use previous value or use default one from
     *            corresponding configuration schema.</li>
     *      </ol>
     *      So that, in case of initial node start (first start ever) specified configuration, supplemented
     *      with defaults, is used. If no configuration was provided defaults are used for all
     *      configuration properties. In case of node restart, specified properties override existing
     *      ones, non specified properties that also weren't specified previously use default values.
     *      Please pay attention that previously specified properties are searched in the
     *      {@code workDir} specified by the user.
     *
     * @param workDir Work directory for the started node. Must not be {@code null}.
     * @return Completable future that resolves into an Ignite node after all components are started and the cluster initialization is
     *         complete.
     * @throws IgniteException If error occurs while reading node configuration.
     */
    // TODO IGNITE-14580 Add exception handling logic to IgnitionProcessor.
    public static CompletableFuture<Ignite> start(String nodeName, @Nullable String configStr, Path workDir) {
        Ignition ignition = loadIgnitionService(Thread.currentThread().getContextClassLoader());

        if (configStr == null) {
            return ignition.start(nodeName, workDir);
        } else {
            try (InputStream inputStream = new ByteArrayInputStream(configStr.getBytes(StandardCharsets.UTF_8))) {
                return ignition.start(nodeName, inputStream, workDir);
            } catch (IOException e) {
                throw new IgniteException("Couldn't close the stream with node config.", e);
            }
        }
    }

    /**
     * Starts an Ignite node with an optional bootstrap configuration from a HOCON file.
     *
     * <p>When this method returns, the node is partially started and ready to accept the init command (that is, its
     * REST endpoint is functional).
     *
     * @param nodeName Name of the node. Must not be {@code null}.
     * @param cfgPath  Path to the node configuration in the HOCON format. Can be {@code null}.
     * @param workDir  Work directory for the started node. Must not be {@code null}.
     * @param clsLdr   The class loader to be used to load provider-configuration files and provider classes, or {@code null} if the system
     *                 class loader (or, failing that, the bootstrap class loader) is to be used
     * @return Completable future that resolves into an Ignite node after all components are started and the cluster initialization is
     *         complete.
     */
    // TODO IGNITE-14580 Add exception handling logic to IgnitionProcessor.
    public static CompletableFuture<Ignite> start(String nodeName, @Nullable Path cfgPath, Path workDir, @Nullable ClassLoader clsLdr) {
        Ignition ignition = loadIgnitionService(clsLdr);

        return ignition.start(nodeName, cfgPath, workDir, clsLdr);
    }

    /**
     * Stops the node with given {@code nodeName}. It's possible to stop both already started node or node that is currently starting.
     * Has no effect if node with specified name doesn't exist.
     *
     * @param nodeName Node name to stop.
     */
    public static void stop(String nodeName) {
        Ignition ignition = loadIgnitionService(Thread.currentThread().getContextClassLoader());

        ignition.stop(nodeName);
    }

    /**
     * Stops the node with given {@code nodeName}. It's possible to stop both already started node or node that is currently starting.
     * Has no effect if node with specified name doesn't exist.
     *
     * @param nodeName Node name to stop.
     * @param clsLdr The class loader to be used to load provider-configuration files and provider classes, or {@code null} if the system
     *               class loader (or, failing that, the bootstrap class loader) is to be used
     */
    public static void stop(String nodeName, @Nullable ClassLoader clsLdr) {
        Ignition ignition = loadIgnitionService(clsLdr);

        ignition.stop(nodeName);
    }

    /**
     * Initializes the cluster that this node is present in.
     *
     * @param nodeName Name of the node that the initialization request will be sent to.
     * @param metaStorageNodeNames names of nodes that will host the Meta Storage and the CMG.
     * @param clusterName Human-readable name of the cluster.
     * @throws IgniteException If the given node has not been started or has been stopped.
     * @throws NullPointerException If any of the parameters are null.
     * @throws IllegalArgumentException If {@code metaStorageNodeNames} is empty or contains blank strings.
     * @throws IllegalArgumentException If {@code clusterName} is blank.
     * @see Ignition#init(String, Collection, String)
     */
    public static synchronized void init(String nodeName, Collection<String> metaStorageNodeNames, String clusterName) {
        Objects.requireNonNull(nodeName);
        Objects.requireNonNull(metaStorageNodeNames);
        Objects.requireNonNull(clusterName);

        if (ignition == null) {
            throw new IgniteException("Ignition service has not been started");
        }

        ignition.init(nodeName, metaStorageNodeNames, clusterName);
    }

    /**
     * Initializes the cluster that this node is present in.
     *
     * @param nodeName Name of the node that the initialization request will be sent to.
     * @param metaStorageNodeNames names of nodes that will host the Meta Storage.
     * @param cmgNodeNames names of nodes that will host the CMG.
     * @param clusterName Human-readable name of the cluster.
     * @throws IgniteException If the given node has not been started or has been stopped.
     * @throws NullPointerException If any of the parameters are null.
     * @throws IllegalArgumentException If {@code metaStorageNodeNames} is empty or contains blank strings.
     * @throws IllegalArgumentException If {@code cmgNodeNames} contains blank strings.
     * @throws IllegalArgumentException If {@code clusterName} is blank.
     * @see Ignition#init(String, Collection, Collection, String)
     */
    public static synchronized void init(
            String nodeName,
            Collection<String> metaStorageNodeNames,
            Collection<String> cmgNodeNames,
            String clusterName
    ) {
        Objects.requireNonNull(nodeName);
        Objects.requireNonNull(metaStorageNodeNames);
        Objects.requireNonNull(cmgNodeNames);
        Objects.requireNonNull(clusterName);

        if (ignition == null) {
            throw new IgniteException("Ignition service has not been started");
        }

        ignition.init(nodeName, metaStorageNodeNames, cmgNodeNames, clusterName);
    }

    private static synchronized Ignition loadIgnitionService(@Nullable ClassLoader clsLdr) {
        if (ignition == null) {
            ServiceLoader<Ignition> ldr = ServiceLoader.load(Ignition.class, clsLdr);
            ignition = ldr.iterator().next();
        }

        return ignition;
    }
}
