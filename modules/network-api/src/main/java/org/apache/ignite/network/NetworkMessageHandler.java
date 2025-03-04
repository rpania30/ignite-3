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

package org.apache.ignite.network;

import org.jetbrains.annotations.Nullable;

/**
 * Handler of incoming messages.
 */
public interface NetworkMessageHandler {
    /**
     * Method that gets invoked when a network message is received.
     *
     * @param message       Message, which was received from the cluster.
     * @param senderAddr    Sender address. Use {@link TopologyService#getByAddress} to resolve the corresponding {@link ClusterNode}.
     * @param correlationId Correlation id. Used to track correspondence between requests and responses. Can be {@code null} if the received
     *                      message is not a request from a {@link MessagingService#invoke} method from another node.
     */
    void onReceived(NetworkMessage message, NetworkAddress senderAddr, @Nullable Long correlationId);
}
