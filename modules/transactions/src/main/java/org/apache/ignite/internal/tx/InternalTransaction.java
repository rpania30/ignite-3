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

package org.apache.ignite.internal.tx;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.network.ClusterNode;
import org.apache.ignite.tx.Transaction;
import org.jetbrains.annotations.NotNull;

/**
 * An extension of a transaction for internal usage.
 */
public interface InternalTransaction extends Transaction {
    /**
     * Returns an id.
     *
     * @return The id.
     */
    @NotNull UUID id();

    /**
     * Returns enlisted primary replica node associated with given replication group.
     *
     * @param partGroupId Replication group id.
     * @return Enlisted primary replica node and raft term associated with given replication group.
     */
    // TODO: IGNITE-17256 IgniteBiTuple along with second parameter term will be removed after introducing leased based primary replica
    // TODO: selection and failover engine.
    IgniteBiTuple<ClusterNode, Long> enlistedNodeAndTerm(String partGroupId);

    /**
     * Returns a transaction state.
     *
     * @return The state.
     */
    TxState state();

    /**
     * Enlists a partition group into a transaction.
     *
     * @param replicationGroupId Replication group id to enlist.
     * @param nodeAndTerm Primary replica cluster node and raft term to enlist for given replication group.
     * @return {@code True} if a partition is enlisted into the transaction.
     */
    IgniteBiTuple<ClusterNode, Long> enlist(String replicationGroupId, IgniteBiTuple<ClusterNode, Long> nodeAndTerm);

    /**
     * Enlists operation future in transaction. It's used in order to wait corresponding tx operations before commit.
     *
     * @param resultFuture Operation result future.
     */
    void enlistResultFuture(CompletableFuture<?> resultFuture);
}
