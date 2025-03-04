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

package org.apache.ignite.internal.cli.core.repl.prompt;

import static org.apache.ignite.internal.cli.core.style.AnsiStringSupport.ansi;
import static org.apache.ignite.internal.cli.core.style.AnsiStringSupport.fg;

import jakarta.inject.Singleton;
import org.apache.ignite.internal.cli.core.repl.Session;
import org.apache.ignite.internal.cli.core.style.AnsiStringSupport.Color;

/**
 * Provider for prompt in REPL.
 */
@Singleton
public class ReplPromptProvider implements PromptProvider {
    private final Session session;

    public ReplPromptProvider(Session session) {
        this.session = session;
    }

    /**
     * Return prompt.
     */
    @Override
    public String getPrompt() {
        String postfix = "> ";
        return session.isConnectedToNode()
                ? ansi(fg(Color.GREEN).mark("[" + session.nodeName() + "]")) + postfix
                : ansi(fg(Color.RED).mark("[disconnected]")) + postfix;
    }
}
