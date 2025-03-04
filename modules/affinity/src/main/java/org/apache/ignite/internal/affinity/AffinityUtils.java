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

package org.apache.ignite.internal.affinity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import org.apache.ignite.network.ClusterNode;
import org.jetbrains.annotations.NotNull;

/**
 * Stateless affinity utils that produces helper methods for an affinity assignments calculation.
 */
public class AffinityUtils {
    /**
     * Calculates affinity assignments.
     *
     * @param partitions Partitions count.
     * @param replicas Replicas count.
     * @param aggregator Function that creates a collection for the partition assignments.
     * @return List nodes by partition.
     */
    public static <T extends Collection<ClusterNode>> List<T> calculateAssignments(
            @NotNull Collection<ClusterNode> baselineNodes,
            int partitions,
            int replicas,
            IntFunction<T> aggregator
    ) {
        return RendezvousAffinityFunction.assignPartitions(
                baselineNodes,
                partitions,
                replicas,
                false,
                null,
                aggregator
        );
    }

    /**
     * Calculates affinity assignments.
     *
     * @param partitions Partitions count.
     * @param replicas Replicas count.
     * @return List nodes by partition.
     */
    public static List<List<ClusterNode>> calculateAssignments(
            @NotNull Collection<ClusterNode> baselineNodes,
            int partitions,
            int replicas
    ) {
        return calculateAssignments(
                baselineNodes,
                partitions,
                replicas,
                ArrayList::new
        );
    }

    /**
     * Calculates affinity assignments for single partition.
     *
     * @param baselineNodes Nodes.
     * @param partition Partition id.
     * @param replicas Replicas count.
     * @return List of nodes.
     */
    public static Set<ClusterNode> calculateAssignmentForPartition(
            Collection<ClusterNode> baselineNodes,
            int partition,
            int replicas
    ) {
        return RendezvousAffinityFunction.assignPartition(
                partition,
                new ArrayList<>(baselineNodes),
                replicas,
                null,
                false,
                null,
                HashSet::new
        );
    }
}
