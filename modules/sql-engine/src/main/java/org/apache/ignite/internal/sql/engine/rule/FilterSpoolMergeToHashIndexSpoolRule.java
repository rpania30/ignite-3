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

package org.apache.ignite.internal.sql.engine.rule;

import static org.apache.ignite.internal.util.CollectionUtils.nullOrEmpty;

import java.util.List;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Spool;
import org.apache.calcite.rex.RexNode;
import org.apache.ignite.internal.sql.engine.rel.IgniteFilter;
import org.apache.ignite.internal.sql.engine.rel.IgniteHashIndexSpool;
import org.apache.ignite.internal.sql.engine.rel.IgniteTableSpool;
import org.apache.ignite.internal.sql.engine.trait.CorrelationTrait;
import org.apache.ignite.internal.sql.engine.trait.TraitUtils;
import org.apache.ignite.internal.sql.engine.util.RexUtils;
import org.immutables.value.Value;

/**
 * Rule that pushes filter into the spool.
 */
@Value.Enclosing
public class FilterSpoolMergeToHashIndexSpoolRule extends RelRule<FilterSpoolMergeToHashIndexSpoolRule.Config> {
    /** Instance. */
    public static final RelOptRule INSTANCE = Config.DEFAULT.toRule();

    private FilterSpoolMergeToHashIndexSpoolRule(Config cfg) {
        super(cfg);
    }

    /** {@inheritDoc} */
    @Override
    public void onMatch(RelOptRuleCall call) {
        final IgniteFilter filter = call.rel(0);
        final IgniteTableSpool spool = call.rel(1);

        RelOptCluster cluster = spool.getCluster();

        RelTraitSet trait = spool.getTraitSet();
        CorrelationTrait filterCorr = TraitUtils.correlation(filter);

        if (filterCorr.correlated()) {
            trait = trait.replace(filterCorr);
        }

        final RelNode input = spool.getInput();

        List<RexNode> searchRow = RexUtils.buildHashSearchRow(
                cluster,
                filter.getCondition(),
                spool.getRowType()
        );

        if (nullOrEmpty(searchRow)) {
            return;
        }

        RelNode res = new IgniteHashIndexSpool(
                cluster,
                trait.replace(RelCollations.EMPTY),
                input,
                searchRow,
                filter.getCondition()
        );

        call.transformTo(res);
    }

    /**
     * Config interface.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    @SuppressWarnings("ClassNameSameAsAncestorName")
    @Value.Immutable
    public interface Config extends RelRule.Config {
        Config DEFAULT = ImmutableFilterSpoolMergeToHashIndexSpoolRule.Config.of()
                .withDescription("FilterSpoolMergeToHashIndexSpoolRule")
                .withOperandFor(IgniteFilter.class, IgniteTableSpool.class);

        /** Defines an operand tree for the given classes. */
        default Config withOperandFor(Class<? extends Filter> filterClass, Class<? extends Spool> spoolClass) {
            return withOperandSupplier(
                    o0 -> o0.operand(filterClass)
                            .oneInput(o1 -> o1.operand(spoolClass)
                                    .anyInputs()
                            )
            )
                    .as(Config.class);
        }

        /** {@inheritDoc} */
        @Override
        default FilterSpoolMergeToHashIndexSpoolRule toRule() {
            return new FilterSpoolMergeToHashIndexSpoolRule(this);
        }
    }
}
