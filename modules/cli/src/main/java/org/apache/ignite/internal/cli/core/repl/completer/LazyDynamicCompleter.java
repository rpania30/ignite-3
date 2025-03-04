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

package org.apache.ignite.internal.cli.core.repl.completer;

import java.util.List;
import java.util.function.Supplier;

/**
 * Wrapper for the dynamic completer that is going to be initialized on demand.
 */
public class LazyDynamicCompleter implements DynamicCompleter {

    private final Supplier<DynamicCompleter> delegateInitializer;

    private DynamicCompleter lazyDelegate;

    private final Object lock = new Object();

    /** Default constructor. Supplier will be called once it is needed. */
    public LazyDynamicCompleter(Supplier<DynamicCompleter> delegateInitializer) {
        this.delegateInitializer = delegateInitializer;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> complete(String[] words) {
        if (lazyDelegate == null) {
            synchronized (lock) {
                if (lazyDelegate == null) {
                    lazyDelegate = delegateInitializer.get();
                }
            }
        }
        return lazyDelegate.complete(words);
    }
}
