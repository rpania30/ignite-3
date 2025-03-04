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

package org.apache.ignite.internal.sql.engine.rel;

import java.util.List;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelInput;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.hint.RelHint;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.ignite.internal.sql.engine.metadata.cost.IgniteCost;
import org.apache.ignite.internal.sql.engine.schema.IgniteIndex;
import org.apache.ignite.internal.sql.engine.schema.IgniteIndex.Type;
import org.apache.ignite.internal.sql.engine.util.IndexConditions;
import org.jetbrains.annotations.Nullable;

/**
 * Class with index conditions info.
 */
public abstract class AbstractIndexScan extends ProjectableFilterableTableScan {
    protected final String idxName;

    protected final IndexConditions idxCond;

    protected final IgniteIndex.Type type;

    /**
     * Constructor used for deserialization.
     *
     * @param input Serialized representation.
     */
    protected AbstractIndexScan(RelInput input) {
        super(input);
        idxName = input.getString("index");
        type = input.getEnum("type", IgniteIndex.Type.class);
        idxCond = new IndexConditions(input);
    }

    /**
     * Constructor.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    protected AbstractIndexScan(
            RelOptCluster cluster,
            RelTraitSet traitSet,
            List<RelHint> hints,
            RelOptTable table,
            String idxName,
            IgniteIndex.Type type,
            @Nullable List<RexNode> proj,
            @Nullable RexNode cond,
            @Nullable IndexConditions idxCond,
            @Nullable ImmutableBitSet reqColumns
    ) {
        super(cluster, traitSet, hints, table, proj, cond, reqColumns);

        this.idxName = idxName;
        this.type = type;
        this.idxCond = idxCond;
    }

    /** {@inheritDoc} */
    @Override
    protected RelWriter explainTerms0(RelWriter pw) {
        pw = pw.item("index", idxName);
        pw = pw.item("type", type.name());
        pw = super.explainTerms0(pw);

        return idxCond.explainTerms(pw);
    }

    /**
     * Get index name.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public String indexName() {
        return idxName;
    }

    /**
     * Get lower index condition.
     */
    public List<RexNode> lowerCondition() {
        return idxCond == null ? null : idxCond.lowerCondition();
    }

    /**
     * Get lower index condition.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public List<RexNode> lowerBound() {
        return idxCond == null ? null : idxCond.lowerBound();
    }

    /**
     * Get upper index condition.
     */
    public List<RexNode> upperCondition() {
        return idxCond == null ? null : idxCond.upperCondition();
    }

    /**
     * Get upper index condition.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public List<RexNode> upperBound() {
        return idxCond == null ? null : idxCond.upperBound();
    }

    /** {@inheritDoc} */
    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        double rows = table.getRowCount();

        double cost = rows * IgniteCost.ROW_PASS_THROUGH_COST;

        if (condition != null) {
            RexBuilder builder = getCluster().getRexBuilder();

            double selectivity = 1;

            cost = 0;

            // for hash index both bounds are set to the same search row
            // because only lookup is possible. So if either bound is null,
            // then there will be a full index scan.
            if (type == Type.HASH && lowerBound() != null) {
                cost += IgniteCost.HASH_LOOKUP_COST;

                selectivity -= 1 - mq.getSelectivity(this, RexUtil.composeConjunction(builder, List.of(condition)));
            } else if (type == Type.SORTED) {
                if (lowerCondition() != null) {
                    double selectivity0 = mq.getSelectivity(this, RexUtil.composeConjunction(builder, lowerCondition()));

                    selectivity -= 1 - selectivity0;

                    cost += Math.log(rows);
                }

                if (upperCondition() != null && lowerCondition() != null && !lowerCondition().equals(upperCondition())) {
                    double selectivity0 = mq.getSelectivity(this, RexUtil.composeConjunction(builder, upperCondition()));

                    selectivity -= 1 - selectivity0;
                }
            }

            rows *= selectivity;

            if (rows <= 0) {
                rows = 1;
            }

            cost += rows * (IgniteCost.ROW_COMPARISON_COST + IgniteCost.ROW_PASS_THROUGH_COST);
        }

        // additional tiny cost for preventing equality with table scan.
        return planner.getCostFactory().makeCost(rows, cost, 0).plus(planner.getCostFactory().makeTinyCost());
    }

    /**
     * Get index conditions.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public IndexConditions indexConditions() {
        return idxCond;
    }
}
