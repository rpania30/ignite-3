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

package org.apache.ignite.internal.sql.engine.externalize;

import static org.apache.ignite.lang.ErrorGroups.Sql.REL_DESERIALIZATION_ERR;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.prepare.RelOptTableImpl;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelInput;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.Pair;
import org.apache.calcite.util.Util;
import org.apache.ignite.internal.sql.engine.schema.IgniteTable;
import org.apache.ignite.internal.sql.engine.schema.SqlSchemaManager;
import org.apache.ignite.internal.sql.engine.util.Commons;
import org.apache.ignite.sql.SqlException;

/**
 * RelJsonReader.
 * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RelJsonReader {
    private static final TypeReference<LinkedHashMap<String, Object>> TYPE_REF = new TypeReference<>() {
    };

    private final ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private final SqlSchemaManager schemaManager;

    private final RelJson relJson;

    private final Map<String, RelNode> relMap = new LinkedHashMap<>();

    private RelNode lastRel;

    /**
     * FromJson.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public static <T extends RelNode> T fromJson(SqlSchemaManager schemaManager, String json) {
        RelJsonReader reader = new RelJsonReader(schemaManager);

        return (T) reader.read(json);
    }

    /**
     * Constructor.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public RelJsonReader(SqlSchemaManager schemaManager) {
        this.schemaManager = schemaManager;

        relJson = new RelJson();
    }

    /**
     * Read.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public RelNode read(String s) {
        try {
            lastRel = null;
            Map<String, Object> o = mapper.readValue(s, TYPE_REF);
            List<Map<String, Object>> rels = (List) o.get("rels");
            readRels(rels);
            return lastRel;
        } catch (IOException e) {
            throw new SqlException(REL_DESERIALIZATION_ERR, e);
        }
    }

    private void readRels(List<Map<String, Object>> jsonRels) {
        for (Map<String, Object> jsonRel : jsonRels) {
            readRel(jsonRel);
        }
    }

    private void readRel(Map<String, Object> jsonRel) {
        String id = (String) jsonRel.get("id");
        String type = (String) jsonRel.get("relOp");
        Function<RelInput, RelNode> factory = relJson.factory(type);
        RelNode rel = factory.apply(new RelInputImpl(jsonRel));
        relMap.put(id, rel);
        lastRel = rel;
    }

    private class RelInputImpl implements RelInputEx {
        private final Map<String, Object> jsonRel;

        private RelInputImpl(Map<String, Object> jsonRel) {
            this.jsonRel = jsonRel;
        }

        /** {@inheritDoc} */
        @Override
        public RelOptCluster getCluster() {
            return Commons.cluster();
        }

        /** {@inheritDoc} */
        @Override
        public RelTraitSet getTraitSet() {
            return Commons.cluster().traitSet();
        }

        /** {@inheritDoc} */
        @Override
        public RelOptTable getTable(String table) {
            // For deserialization #getTableById() should be used instead because
            // it's the only way to find out that someone just recreate the table
            // (probably with different schema) with the same name while the plan
            // was serialized
            throw new AssertionError("Unexpected method was called");
        }

        /** {@inheritDoc} */
        @Override
        public RelOptTable getTableById() {
            String tableId = getString("tableId");
            int ver = ((Number) get("tableVer")).intValue();

            IgniteTable table = schemaManager.tableById(UUID.fromString(tableId), ver);

            List<String> tableName = getStringList("table");

            return RelOptTableImpl.create(null, table.getRowType(Commons.typeFactory()), tableName,
                    table, c -> null);
        }

        /** {@inheritDoc} */
        @Override
        public RelNode getInput() {
            List<RelNode> inputs = getInputs();
            assert inputs.size() == 1;
            return inputs.get(0);
        }

        /** {@inheritDoc} */
        @Override
        public List<RelNode> getInputs() {
            List<String> jsonInputs = getStringList("inputs");
            if (jsonInputs == null) {
                return List.of(lastRel);
            }
            List<RelNode> inputs = new ArrayList<>();
            for (String jsonInput : jsonInputs) {
                inputs.add(lookupInput(jsonInput));
            }
            return inputs;
        }

        /** {@inheritDoc} */
        @Override
        public RexNode getExpression(String tag) {
            return relJson.toRex(this, jsonRel.get(tag));
        }

        /** {@inheritDoc} */
        @Override
        public ImmutableBitSet getBitSet(String tag) {
            return ImmutableBitSet.of(getIntegerList(tag));
        }

        /** {@inheritDoc} */
        @Override
        public List<ImmutableBitSet> getBitSetList(String tag) {
            List<List<Integer>> list = getIntegerListList(tag);

            if (list == null) {
                return null;
            }

            List<ImmutableBitSet> bitSets = new ArrayList<>();

            for (List<Integer> integers : list) {
                bitSets.add(ImmutableBitSet.of(integers));
            }

            return List.copyOf(bitSets);
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getStringList(String tag) {
            return (List<String>) jsonRel.get(tag);
        }

        /** {@inheritDoc} */
        @Override
        public List<Integer> getIntegerList(String tag) {
            return (List<Integer>) jsonRel.get(tag);
        }

        /** {@inheritDoc} */
        @Override
        public List<List<Integer>> getIntegerListList(String tag) {
            return (List<List<Integer>>) jsonRel.get(tag);
        }

        /** {@inheritDoc} */
        @Override
        public List<AggregateCall> getAggregateCalls(String tag) {
            List<Map<String, Object>> jsonAggs = (List) jsonRel.get(tag);
            List<AggregateCall> inputs = new ArrayList<>();
            for (Map<String, Object> jsonAggCall : jsonAggs) {
                inputs.add(toAggCall(jsonAggCall));
            }
            return inputs;
        }

        /** {@inheritDoc} */
        @Override
        public Object get(String tag) {
            return jsonRel.get(tag);
        }

        /** {@inheritDoc} */
        @Override
        public String getString(String tag) {
            return (String) jsonRel.get(tag);
        }

        /** {@inheritDoc} */
        @Override
        public float getFloat(String tag) {
            return ((Number) jsonRel.get(tag)).floatValue();
        }

        /** {@inheritDoc} */
        @Override
        public boolean getBoolean(String tag, boolean def) {
            Boolean b = (Boolean) jsonRel.get(tag);
            return b != null ? b : def;
        }

        /** {@inheritDoc} */
        @Override
        public <E extends Enum<E>> E getEnum(String tag, Class<E> enumClass) {
            return Util.enumVal(enumClass,
                    getString(tag).toUpperCase(Locale.ROOT));
        }

        /** {@inheritDoc} */
        @Override
        public List<RexNode> getExpressionList(String tag) {
            List<Object> jsonNodes = (List) jsonRel.get(tag);
            List<RexNode> nodes = new ArrayList<>();
            for (Object jsonNode : jsonNodes) {
                nodes.add(relJson.toRex(this, jsonNode));
            }
            return nodes;
        }

        /** {@inheritDoc} */
        @Override
        public RelDataType getRowType(String tag) {
            Object o = jsonRel.get(tag);
            return relJson.toType(Commons.typeFactory(), o);
        }

        /** {@inheritDoc} */
        @Override
        public RelDataType getRowType(String expressionsTag, String fieldsTag) {
            List<RexNode> expressionList = getExpressionList(expressionsTag);
            List<String> names =
                    (List<String>) get(fieldsTag);
            return Commons.typeFactory().createStructType(
                    new AbstractList<Map.Entry<String, RelDataType>>() {
                        @Override
                        public Map.Entry<String, RelDataType> get(int index) {
                            return Pair.of(names.get(index),
                                    expressionList.get(index).getType());
                        }

                        @Override
                        public int size() {
                            return names.size();
                        }
                    });
        }

        /** {@inheritDoc} */
        @Override
        public RelCollation getCollation() {
            return relJson.toCollation((List) get("collation"));
        }

        /** {@inheritDoc} */
        @Override
        public RelCollation getCollation(String tag) {
            return relJson.toCollation((List) get(tag));
        }

        /** {@inheritDoc} */
        @Override
        public RelDistribution getDistribution() {
            return relJson.toDistribution(get("distribution"));
        }

        /** {@inheritDoc} */
        @Override
        public ImmutableList<ImmutableList<RexLiteral>> getTuples(String tag) {
            List<List> jsonTuples = (List) get(tag);
            ImmutableList.Builder<ImmutableList<RexLiteral>> builder =
                    ImmutableList.builder();

            for (List jsonTuple : jsonTuples) {
                builder.add(getTuple(jsonTuple));
            }

            return builder.build();
        }

        private RelNode lookupInput(String jsonInput) {
            RelNode node = relMap.get(jsonInput);
            if (node == null) {
                throw new RuntimeException("unknown id " + jsonInput
                        + " for relational expression");
            }
            return node;
        }

        private ImmutableList<RexLiteral> getTuple(List jsonTuple) {
            ImmutableList.Builder<RexLiteral> builder =
                    ImmutableList.builder();

            for (Object jsonValue : jsonTuple) {
                builder.add((RexLiteral) relJson.toRex(this, jsonValue));
            }

            return builder.build();
        }

        private AggregateCall toAggCall(Map<String, Object> jsonAggCall) {
            Map<String, Object> aggMap = (Map) jsonAggCall.get("agg");
            SqlAggFunction aggregation = (SqlAggFunction) relJson.toOp(aggMap);
            Boolean distinct = (Boolean) jsonAggCall.get("distinct");
            List<Integer> operands = (List<Integer>) jsonAggCall.get("operands");
            Integer filterOperand = (Integer) jsonAggCall.get("filter");
            RelDataType type = relJson.toType(Commons.typeFactory(), jsonAggCall.get("type"));
            String name = (String) jsonAggCall.get("name");
            return AggregateCall.create(aggregation, distinct, false, false, operands,
                    filterOperand == null ? -1 : filterOperand,
                    RelCollations.EMPTY,
                    type, name);
        }
    }
}
