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

package org.apache.ignite.internal.cli.call.connect;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.ignite.internal.cli.core.call.Call;
import org.apache.ignite.internal.cli.core.call.CallOutput;
import org.apache.ignite.internal.cli.core.call.DefaultCallOutput;
import org.apache.ignite.internal.cli.core.call.EmptyCallInput;
import org.apache.ignite.internal.cli.core.repl.Session;
import org.apache.ignite.internal.cli.core.style.component.MessageUiComponent;
import org.apache.ignite.internal.cli.core.style.element.UiElements;

/**
 * Call for disconnect.
 */
@Singleton
public class DisconnectCall implements Call<EmptyCallInput, String> {
    @Inject
    private final Session session;

    public DisconnectCall(Session session) {
        this.session = session;
    }

    @Override
    public CallOutput<String> execute(EmptyCallInput input) {
        if (session.isConnectedToNode()) {
            String nodeUrl = session.nodeUrl();
            session.setNodeUrl(null);
            session.setNodeName(null);
            session.setConnectedToNode(false);

            return DefaultCallOutput.success(
                    MessageUiComponent.fromMessage("Disconnected from %s", UiElements.url(nodeUrl)).render()
            );
        }

        return DefaultCallOutput.empty();
    }
}
