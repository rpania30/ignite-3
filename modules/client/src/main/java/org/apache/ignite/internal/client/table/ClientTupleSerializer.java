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

package org.apache.ignite.internal.client.table;

import static org.apache.ignite.internal.client.proto.ClientMessageCommon.NO_VALUE;
import static org.apache.ignite.internal.client.table.ClientTable.writeTx;
import static org.apache.ignite.lang.ErrorGroups.Client.PROTOCOL_ERR;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.apache.ignite.internal.binarytuple.BinaryTupleBuilder;
import org.apache.ignite.internal.binarytuple.BinaryTupleReader;
import org.apache.ignite.internal.client.PayloadOutputChannel;
import org.apache.ignite.internal.client.proto.ClientBinaryTupleUtils;
import org.apache.ignite.internal.client.proto.ClientDataType;
import org.apache.ignite.internal.client.proto.ClientMessageUnpacker;
import org.apache.ignite.internal.client.proto.TuplePart;
import org.apache.ignite.internal.util.HashCalculator;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteException;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.table.mapper.Mapper;
import org.apache.ignite.tx.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tuple serializer.
 */
public class ClientTupleSerializer {
    /** Table ID. */
    private final UUID tableId;

    /**
     * Constructor.
     *
     * @param tableId Table id.
     */
    ClientTupleSerializer(UUID tableId) {
        this.tableId = tableId;
    }

    /**
     * Writes {@link Tuple}.
     *
     * @param tuple Tuple.
     * @param schema Schema.
     * @param out Out.
     */
    void writeTuple(
            @Nullable Transaction tx,
            @NotNull Tuple tuple,
            ClientSchema schema,
            PayloadOutputChannel out
    ) {
        writeTuple(tx, tuple, schema, out, false, false);
    }

    /**
     * Writes {@link Tuple}.
     *
     * @param tuple Tuple.
     * @param schema Schema.
     * @param out Out.
     * @param keyOnly Key only.
     */
    void writeTuple(
            @Nullable Transaction tx,
            @NotNull Tuple tuple,
            ClientSchema schema,
            PayloadOutputChannel out,
            boolean keyOnly
    ) {
        writeTuple(tx, tuple, schema, out, keyOnly, false);
    }

    /**
     * Writes {@link Tuple}.
     *
     * @param tuple Tuple.
     * @param schema Schema.
     * @param out Out.
     * @param keyOnly Key only.
     * @param skipHeader Skip header.
     */
    void writeTuple(
            @Nullable Transaction tx,
            @NotNull Tuple tuple,
            ClientSchema schema,
            PayloadOutputChannel out,
            boolean keyOnly,
            boolean skipHeader
    ) {
        if (!skipHeader) {
            out.out().packUuid(tableId);
            writeTx(tx, out);
            out.out().packInt(schema.version());
        }

        writeTupleRaw(tuple, schema, out, keyOnly);
    }

    /**
     * Writes {@link Tuple} without header.
     *
     * @param tuple Tuple.
     * @param schema Schema.
     * @param out Out.
     * @param keyOnly Key only.
     */
    public static void writeTupleRaw(@NotNull Tuple tuple, ClientSchema schema, PayloadOutputChannel out, boolean keyOnly) {
        var columns = schema.columns();
        var count = keyOnly ? schema.keyColumnCount() : columns.length;

        var builder = new BinaryTupleBuilder(count, true);
        var noValueSet = new BitSet(count);

        for (var i = 0; i < count; i++) {
            var col = columns[i];
            Object v = tuple.valueOrDefault(col.name(), NO_VALUE);

            appendValue(builder, noValueSet, col, v);
        }

        out.out().packBinaryTuple(builder, noValueSet);
    }

    /**
     * Writes key and value {@link Tuple}.
     *
     * @param key Key tuple.
     * @param val Value tuple.
     * @param schema Schema.
     * @param out Out.
     * @param skipHeader Skip header.
     */
    void writeKvTuple(
            @Nullable Transaction tx,
            @NotNull Tuple key,
            @Nullable Tuple val,
            ClientSchema schema,
            PayloadOutputChannel out,
            boolean skipHeader
    ) {
        if (!skipHeader) {
            out.out().packUuid(tableId);
            writeTx(tx, out);
            out.out().packInt(schema.version());
        }

        var columns = schema.columns();
        var noValueSet = new BitSet(columns.length);
        var builder = new BinaryTupleBuilder(columns.length, true);

        for (var i = 0; i < columns.length; i++) {
            var col = columns[i];

            Object v = col.key()
                    ? key.valueOrDefault(col.name(), NO_VALUE)
                    : val != null
                            ? val.valueOrDefault(col.name(), NO_VALUE)
                            : NO_VALUE;

            appendValue(builder, noValueSet, col, v);
        }

        out.out().packBinaryTuple(builder, noValueSet);
    }

    /**
     * Writes pairs {@link Tuple}.
     *
     * @param pairs Key tuple.
     * @param schema Schema.
     * @param out Out.
     */
    void writeKvTuples(@Nullable Transaction tx, Map<Tuple, Tuple> pairs, ClientSchema schema, PayloadOutputChannel out) {
        out.out().packUuid(tableId);
        writeTx(tx, out);
        out.out().packInt(schema.version());
        out.out().packInt(pairs.size());

        for (Map.Entry<Tuple, Tuple> pair : pairs.entrySet()) {
            writeKvTuple(tx, pair.getKey(), pair.getValue(), schema, out, true);
        }
    }

    /**
     * Writes {@link Tuple}'s.
     *
     * @param tuples Tuples.
     * @param schema Schema.
     * @param out Out.
     * @param keyOnly Key only.
     */
    void writeTuples(
            @Nullable Transaction tx,
            @NotNull Collection<Tuple> tuples,
            ClientSchema schema,
            PayloadOutputChannel out,
            boolean keyOnly
    ) {
        out.out().packUuid(tableId);
        writeTx(tx, out);
        out.out().packInt(schema.version());
        out.out().packInt(tuples.size());

        for (var tuple : tuples) {
            writeTuple(tx, tuple, schema, out, keyOnly, true);
        }
    }

    static Tuple readTuple(ClientSchema schema, ClientMessageUnpacker in, boolean keyOnly) {
        var tuple = new ClientTuple(schema);

        var colCnt = keyOnly ? schema.keyColumnCount() : schema.columns().length;

        var binTuple = new BinaryTupleReader(colCnt, in.readBinaryUnsafe());

        for (var i = 0; i < colCnt; i++) {
            ClientColumn column = schema.columns()[i];
            ClientBinaryTupleUtils.readAndSetColumnValue(binTuple, i, tuple, column.name(), column.type(), column.scale());
        }

        return tuple;
    }

    static Tuple readValueTuple(ClientSchema schema, ClientMessageUnpacker in, Tuple keyTuple) {
        var tuple = new ClientTuple(schema);
        var binTuple = new BinaryTupleReader(schema.columns().length - schema.keyColumnCount(), in.readBinaryUnsafe());

        for (var i = 0; i < schema.columns().length; i++) {
            ClientColumn col = schema.columns()[i];

            if (i < schema.keyColumnCount()) {
                tuple.setInternal(i, keyTuple.value(col.name()));
            } else {
                ClientBinaryTupleUtils.readAndSetColumnValue(
                        binTuple, i - schema.keyColumnCount(), tuple, col.name(), col.type(), col.scale());
            }
        }

        return tuple;
    }

    static Tuple readValueTuple(ClientSchema schema, ClientMessageUnpacker in) {
        var keyColCnt = schema.keyColumnCount();
        var colCnt = schema.columns().length;

        var valTuple = new ClientTuple(schema, keyColCnt, schema.columns().length - 1);
        var binTupleReader = new BinaryTupleReader(colCnt - keyColCnt, in.readBinaryUnsafe());

        for (var i = keyColCnt; i < colCnt; i++) {
            ClientColumn col = schema.columns()[i];
            ClientBinaryTupleUtils.readAndSetColumnValue(
                    binTupleReader, i - keyColCnt, valTuple, col.name(), col.type(), col.scale());
        }

        return valTuple;
    }

    static IgniteBiTuple<Tuple, Tuple> readKvTuple(ClientSchema schema, ClientMessageUnpacker in) {
        var keyColCnt = schema.keyColumnCount();
        var colCnt = schema.columns().length;

        var keyTuple = new ClientTuple(schema, 0, keyColCnt - 1);
        var valTuple = new ClientTuple(schema, keyColCnt, schema.columns().length - 1);

        var binTuple = new BinaryTupleReader(colCnt, in.readBinaryUnsafe());

        for (var i = 0; i < colCnt; i++) {
            ClientColumn col = schema.columns()[i];
            var targetTuple = i < keyColCnt ? keyTuple : valTuple;

            ClientBinaryTupleUtils.readAndSetColumnValue(binTuple, i, targetTuple, col.name(), col.type(), col.scale());
        }

        return new IgniteBiTuple<>(keyTuple, valTuple);
    }

    /**
     * Reads {@link Tuple} pairs.
     *
     * @param schema Schema.
     * @param in In.
     * @return Tuple pairs.
     */
    static Map<Tuple, Tuple> readKvTuplesNullable(ClientSchema schema, ClientMessageUnpacker in) {
        var cnt = in.unpackInt();
        Map<Tuple, Tuple> res = new HashMap<>(cnt);

        for (int i = 0; i < cnt; i++) {
            var hasValue = in.unpackBoolean();

            if (hasValue) {
                var pair = readKvTuple(schema, in);

                res.put(pair.get1(), pair.get2());
            }
        }

        return res;
    }

    static Collection<Tuple> readTuples(ClientSchema schema, ClientMessageUnpacker in) {
        return readTuples(schema, in, false);
    }

    static Collection<Tuple> readTuples(ClientSchema schema, ClientMessageUnpacker in, boolean keyOnly) {
        var cnt = in.unpackInt();
        var res = new ArrayList<Tuple>(cnt);

        for (int i = 0; i < cnt; i++) {
            res.add(readTuple(schema, in, keyOnly));
        }

        return res;
    }

    static Collection<Tuple> readTuplesNullable(ClientSchema schema, ClientMessageUnpacker in) {
        var cnt = in.unpackInt();
        var res = new ArrayList<Tuple>(cnt);

        for (int i = 0; i < cnt; i++) {
            var tuple = in.unpackBoolean()
                    ? readTuple(schema, in, false)
                    : null;

            res.add(tuple);
        }

        return res;
    }

    private static void appendValue(BinaryTupleBuilder builder, BitSet noValueSet, ClientColumn col, Object v) {
        if (v == null) {
            builder.appendNull();
            return;
        }

        if (v == NO_VALUE) {
            noValueSet.set(col.schemaIndex());
            builder.appendDefault();
            return;
        }

        try {
            switch (col.type()) {
                case ClientDataType.INT8:
                    builder.appendByte((byte) v);
                    return;

                case ClientDataType.INT16:
                    builder.appendShort((short) v);
                    return;

                case ClientDataType.INT32:
                    builder.appendInt((int) v);
                    return;

                case ClientDataType.INT64:
                    builder.appendLong((long) v);
                    return;

                case ClientDataType.FLOAT:
                    builder.appendFloat((float) v);
                    return;

                case ClientDataType.DOUBLE:
                    builder.appendDouble((double) v);
                    return;

                case ClientDataType.DECIMAL:
                    builder.appendDecimalNotNull((BigDecimal) v, col.scale());
                    return;

                case ClientDataType.UUID:
                    builder.appendUuidNotNull((UUID) v);
                    return;

                case ClientDataType.STRING:
                    builder.appendStringNotNull((String) v);
                    return;

                case ClientDataType.BYTES:
                    builder.appendBytesNotNull((byte[]) v);
                    return;

                case ClientDataType.BITMASK:
                    builder.appendBitmaskNotNull((BitSet) v);
                    return;

                case ClientDataType.DATE:
                    builder.appendDateNotNull((LocalDate) v);
                    return;

                case ClientDataType.TIME:
                    builder.appendTimeNotNull((LocalTime) v);
                    return;

                case ClientDataType.DATETIME:
                    builder.appendDateTimeNotNull((LocalDateTime) v);
                    return;

                case ClientDataType.TIMESTAMP:
                    builder.appendTimestampNotNull((Instant) v);
                    return;

                case ClientDataType.NUMBER:
                    builder.appendNumberNotNull((BigInteger) v);
                    return;

                default:
                    throw new IllegalArgumentException("Unsupported type: " + col.type());
            }
        } catch (ClassCastException e) {
            throw new IgniteException(PROTOCOL_ERR, "Incorrect value type for column '" + col.name() + "': " + e.getMessage(), e);
        }
    }

    @Nullable
    static Function<ClientSchema, Integer> getHashFunction(@Nullable Transaction tx, @NotNull Tuple rec) {
        // Disable partition awareness when transaction is used: tx belongs to a default connection.
        return tx != null ? null : schema -> getColocationHash(schema, rec);
    }

    @Nullable
    static Function<ClientSchema, Integer> getHashFunction(@Nullable Transaction tx, Mapper<?> mapper, @NotNull Object rec) {
        // Disable partition awareness when transaction is used: tx belongs to a default connection.
        return tx != null ? null : schema -> getColocationHash(schema, mapper, rec);
    }

    private static Integer getColocationHash(ClientSchema schema, Tuple rec) {
        var hashCalc = new HashCalculator();

        for (ClientColumn col : schema.colocationColumns()) {
            Object value = rec.valueOrDefault(col.name(), null);
            hashCalc.append(value);
        }

        return hashCalc.hash();
    }

    private static Integer getColocationHash(ClientSchema schema, Mapper<?> mapper, Object rec) {
        // Colocation columns are always part of the key - https://cwiki.apache.org/confluence/display/IGNITE/IEP-86%3A+Colocation+Key.
        var hashCalc = new HashCalculator();
        var marsh = schema.getMarshaller(mapper, TuplePart.KEY);

        for (ClientColumn col : schema.colocationColumns()) {
            Object value = marsh.value(rec, col.schemaIndex());
            hashCalc.append(value);
        }

        return hashCalc.hash();
    }
}
