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

package org.apache.ignite.internal.metastorage.server;

import java.util.Collection;

/**
 * Simple operations + result wrapper to describe the terminal branch of {@link If} execution.
 */
public class Update {
    /** Operations. */
    private final Collection<Operation> ops;

    /** Result. */
    private final StatementResult result;

    /**
     * Constructs new update object.
     *
     * @param ops operations
     * @param result result
     */
    public Update(Collection<Operation> ops, StatementResult result) {
        this.ops = ops;
        this.result = result;
    }

    /**
     * Returns operations.
     *
     * @return operations.
     */
    public Collection<Operation> operations() {
        return ops;
    }

    /**
     * Returns result.
     *
     * @return result.
     */
    public StatementResult result() {
        return result;
    }
}
