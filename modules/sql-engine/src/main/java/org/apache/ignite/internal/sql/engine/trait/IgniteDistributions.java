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

package org.apache.ignite.internal.sql.engine.trait;

import java.util.List;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.util.ImmutableIntList;

/**
 * IgniteDistributions.
 * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
 */
public class IgniteDistributions {
    private static final IgniteDistribution BROADCAST = canonize(new DistributionTrait(DistributionFunction.broadcast()));

    private static final IgniteDistribution SINGLETON = canonize(new DistributionTrait(DistributionFunction.singleton()));

    private static final IgniteDistribution RANDOM = canonize(new DistributionTrait(DistributionFunction.random()));

    private static final IgniteDistribution ANY = canonize(new DistributionTrait(DistributionFunction.any()));

    /**
     * Get any distribution.
     */
    public static IgniteDistribution any() {
        return ANY;
    }

    /**
     * Get random distribution.
     */
    public static IgniteDistribution random() {
        return RANDOM;
    }

    /**
     * Get single distribution.
     */
    public static IgniteDistribution single() {
        return SINGLETON;
    }

    /**
     * Get broadcast distribution.
     */
    public static IgniteDistribution broadcast() {
        return BROADCAST;
    }

    /**
     * Affinity.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     *
     * @param key       Affinity key.
     * @param cacheName Affinity cache name.
     * @param identity  Affinity identity key.
     * @return Affinity distribution.
     */
    public static IgniteDistribution affinity(int key, String cacheName, Object identity) {
        // TODO: fix cacheId
        return affinity(key, 0, identity);
    }

    /**
     * Affinity.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     *
     * @param key      Affinity key.
     * @param cacheId  Affinity cache ID.
     * @param identity Affinity identity key.
     * @return Affinity distribution.
     */
    public static IgniteDistribution affinity(int key, int cacheId, Object identity) {
        return hash(ImmutableIntList.of(key), DistributionFunction.affinity(cacheId, identity));
    }

    /**
     * Affinity.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     *
     * @param keys     Affinity keys.
     * @param cacheId  Affinity cache ID.
     * @param identity Affinity identity key.
     * @return Affinity distribution.
     */
    public static IgniteDistribution affinity(ImmutableIntList keys, int cacheId, Object identity) {
        return hash(keys, DistributionFunction.affinity(cacheId, identity));
    }

    /**
     * Hash.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     *
     * @param keys Distribution keys.
     * @return Hash distribution.
     */
    public static IgniteDistribution hash(List<Integer> keys) {
        return canonize(new DistributionTrait(ImmutableIntList.copyOf(keys), DistributionFunction.hash()));
    }

    /**
     * Hash.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     *
     * @param keys     Distribution keys.
     * @param function Specific hash function.
     * @return Hash distribution.
     */
    public static IgniteDistribution hash(ImmutableIntList keys, DistributionFunction function) {
        return canonize(new DistributionTrait(keys, function));
    }

    /**
     * See {@link RelTraitDef#canonize(org.apache.calcite.plan.RelTrait)}.
     */
    private static IgniteDistribution canonize(IgniteDistribution distr) {
        return DistributionTraitDef.INSTANCE.canonize(distr);
    }
}
