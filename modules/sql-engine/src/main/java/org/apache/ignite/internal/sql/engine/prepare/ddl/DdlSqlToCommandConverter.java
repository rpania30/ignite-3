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

package org.apache.ignite.internal.sql.engine.prepare.ddl;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.apache.ignite.configuration.schemas.store.UnknownDataStorageConfigurationSchema.UNKNOWN_DATA_STORAGE;
import static org.apache.ignite.internal.util.CollectionUtils.nullOrEmpty;
import static org.apache.ignite.lang.ErrorGroups.Sql.PRIMARY_KEYS_MULTIPLE_ERR;
import static org.apache.ignite.lang.ErrorGroups.Sql.PRIMARY_KEY_MISSING_ERR;
import static org.apache.ignite.lang.ErrorGroups.Sql.QUERY_INVALID_ERR;
import static org.apache.ignite.lang.ErrorGroups.Sql.SCHEMA_NOT_FOUND_ERR;
import static org.apache.ignite.lang.ErrorGroups.Sql.SQL_TO_REL_CONVERSION_ERR;
import static org.apache.ignite.lang.ErrorGroups.Sql.STORAGE_ENGINE_NOT_VALID_ERR;
import static org.apache.ignite.lang.ErrorGroups.Sql.TABLE_OPTION_ERR;
import static org.apache.ignite.lang.ErrorGroups.Sql.UNSUPPORTED_DDL_OPERATION_ERR;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.ColumnStrategy;
import org.apache.calcite.sql.SqlBasicTypeNameSpec;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlDdl;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.ddl.SqlColumnDeclaration;
import org.apache.calcite.sql.ddl.SqlDdlNodes;
import org.apache.calcite.sql.ddl.SqlDropTable;
import org.apache.calcite.sql.ddl.SqlKeyConstraint;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.DateString;
import org.apache.calcite.util.TimeString;
import org.apache.calcite.util.TimestampString;
import org.apache.ignite.internal.sql.engine.prepare.IgnitePlanner;
import org.apache.ignite.internal.sql.engine.prepare.PlanningContext;
import org.apache.ignite.internal.sql.engine.prepare.ddl.CreateIndexCommand.Type;
import org.apache.ignite.internal.sql.engine.schema.IgniteIndex.Collation;
import org.apache.ignite.internal.sql.engine.sql.IgniteSqlAlterTableAddColumn;
import org.apache.ignite.internal.sql.engine.sql.IgniteSqlAlterTableDropColumn;
import org.apache.ignite.internal.sql.engine.sql.IgniteSqlCreateIndex;
import org.apache.ignite.internal.sql.engine.sql.IgniteSqlCreateTable;
import org.apache.ignite.internal.sql.engine.sql.IgniteSqlCreateTableOption;
import org.apache.ignite.internal.sql.engine.sql.IgniteSqlDropIndex;
import org.apache.ignite.internal.sql.engine.sql.IgniteSqlIndexType;
import org.apache.ignite.internal.sql.engine.util.Commons;
import org.apache.ignite.internal.util.ArrayUtils;
import org.apache.ignite.lang.IgniteException;
import org.apache.ignite.sql.SqlException;
import org.jetbrains.annotations.Nullable;

/**
 * DdlSqlToCommandConverter.
 */
// TODO: IGNITE-15859 Add documentation
public class DdlSqlToCommandConverter {
    private final Supplier<String> defaultDataStorageSupplier;

    /**
     * Mapping: Data storage ID -> data storage name.
     *
     * <p>Example for "rocksdb": {@code Map.of("ROCKSDB", "rocksdb")}.
     */
    private final Map<String, String> dataStorageNames;

    /**
     * Mapping: Table option ID -> table option info.
     *
     * <p>Example for "replicas": {@code Map.of("REPLICAS", TableOptionInfo@123)}.
     */
    private final Map<String, TableOptionInfo<?>> tableOptionInfos;

    /**
     * Like {@link #tableOptionInfos}, but for each data storage name.
     */
    private final Map<String, Map<String, TableOptionInfo<?>>> dataStorageOptionInfos;

    /**
     * Constructor.
     *
     * @param dataStorageFields Data storage fields. Mapping: Data storage name -> field name -> field type.
     * @param defaultDataStorageSupplier Default data storage supplier.
     */
    public DdlSqlToCommandConverter(
            Map<String, Map<String, Class<?>>> dataStorageFields,
            Supplier<String> defaultDataStorageSupplier
    ) {
        this.defaultDataStorageSupplier = defaultDataStorageSupplier;

        this.dataStorageNames = collectDataStorageNames(dataStorageFields.keySet());

        this.tableOptionInfos = collectTableOptionInfos(
                new TableOptionInfo<>("replicas", Integer.class, this::checkPositiveNumber, CreateTableCommand::replicas),
                new TableOptionInfo<>("partitions", Integer.class, this::checkPositiveNumber, CreateTableCommand::partitions)
        );

        this.dataStorageOptionInfos = dataStorageFields.entrySet()
                .stream()
                .collect(toUnmodifiableMap(
                        Entry::getKey,
                        e0 -> collectTableOptionInfos(
                                e0.getValue().entrySet().stream()
                                        .map(this::dataStorageFieldOptionInfo)
                                        .toArray(TableOptionInfo[]::new)
                        )
                ));

        dataStorageOptionInfos.forEach((k, v) -> checkDuplicates(v, tableOptionInfos));
    }

    /**
     * Converts a given ddl AST to a ddl command.
     *
     * @param ddlNode Root node of the given AST.
     * @param ctx     Planning context.
     */
    public DdlCommand convert(SqlDdl ddlNode, PlanningContext ctx) {
        if (ddlNode instanceof IgniteSqlCreateTable) {
            return convertCreateTable((IgniteSqlCreateTable) ddlNode, ctx);
        }

        if (ddlNode instanceof SqlDropTable) {
            return convertDropTable((SqlDropTable) ddlNode, ctx);
        }

        if (ddlNode instanceof IgniteSqlAlterTableAddColumn) {
            return convertAlterTableAdd((IgniteSqlAlterTableAddColumn) ddlNode, ctx);
        }

        if (ddlNode instanceof IgniteSqlAlterTableDropColumn) {
            return convertAlterTableDrop((IgniteSqlAlterTableDropColumn) ddlNode, ctx);
        }

        if (ddlNode instanceof IgniteSqlCreateIndex) {
            return convertAddIndex((IgniteSqlCreateIndex) ddlNode, ctx);
        }

        if (ddlNode instanceof IgniteSqlDropIndex) {
            return convertDropIndex((IgniteSqlDropIndex) ddlNode, ctx);
        }

        throw new SqlException(UNSUPPORTED_DDL_OPERATION_ERR, "Unsupported operation ["
                + "sqlNodeKind=" + ddlNode.getKind() + "; "
                + "querySql=\"" + ctx.query() + "\"]");
    }

    /**
     * Converts a given CreateTable AST to a CreateTable command.
     *
     * @param createTblNode Root node of the given AST.
     * @param ctx           Planning context.
     */
    private CreateTableCommand convertCreateTable(IgniteSqlCreateTable createTblNode, PlanningContext ctx) {
        CreateTableCommand createTblCmd = new CreateTableCommand();

        createTblCmd.schemaName(deriveSchemaName(createTblNode.name(), ctx));
        createTblCmd.tableName(deriveObjectName(createTblNode.name(), ctx, "tableName"));
        createTblCmd.ifTableExists(createTblNode.ifNotExists());
        createTblCmd.dataStorage(deriveDataStorage(createTblNode.engineName(), ctx));

        if (createTblNode.createOptionList() != null) {
            for (SqlNode optionNode : createTblNode.createOptionList().getList()) {
                IgniteSqlCreateTableOption option = (IgniteSqlCreateTableOption) optionNode;

                assert option.key().isSimple() : option.key();

                String optionKey = option.key().getSimple().toUpperCase();

                if (tableOptionInfos.containsKey(optionKey)) {
                    processTableOption(tableOptionInfos.get(optionKey), option, ctx, createTblCmd);
                } else if (dataStorageOptionInfos.get(createTblCmd.dataStorage()).containsKey(optionKey)) {
                    processTableOption(dataStorageOptionInfos.get(createTblCmd.dataStorage()).get(optionKey), option, ctx, createTblCmd);
                } else {
                    throw new IgniteException(
                            TABLE_OPTION_ERR, String.format("Unexpected table option [option=%s, query=%s]", optionKey, ctx.query()));
                }
            }
        }

        List<SqlKeyConstraint> pkConstraints = createTblNode.columnList().getList().stream()
                .filter(SqlKeyConstraint.class::isInstance)
                .map(SqlKeyConstraint.class::cast)
                .collect(Collectors.toList());

        if (pkConstraints.isEmpty() && Commons.implicitPkEnabled()) {
            SqlIdentifier colName = new SqlIdentifier(Commons.IMPLICIT_PK_COL_NAME, SqlParserPos.ZERO);

            pkConstraints.add(SqlKeyConstraint.primary(SqlParserPos.ZERO, null, SqlNodeList.of(colName)));

            SqlDataTypeSpec type = new SqlDataTypeSpec(new SqlBasicTypeNameSpec(SqlTypeName.VARCHAR, SqlParserPos.ZERO), SqlParserPos.ZERO);
            SqlNode col = SqlDdlNodes.column(SqlParserPos.ZERO, colName, type, null, ColumnStrategy.DEFAULT);

            createTblNode.columnList().add(0, col);
        }

        if (nullOrEmpty(pkConstraints)) {
            throw new SqlException(PRIMARY_KEY_MISSING_ERR, "Table without PRIMARY KEY is not supported");
        } else if (pkConstraints.size() > 1) {
            throw new SqlException(PRIMARY_KEYS_MULTIPLE_ERR, "Unexpected amount of primary key constraints ["
                    + "expected at most one, but was " + pkConstraints.size() + "; "
                    + "querySql=\"" + ctx.query() + "\"]");
        }

        Set<String> dedupSetPk = new HashSet<>();

        List<String> pkCols = pkConstraints.stream()
                .map(pk -> pk.getOperandList().get(1))
                .map(SqlNodeList.class::cast)
                .flatMap(l -> l.getList().stream())
                .map(SqlIdentifier.class::cast)
                .map(SqlIdentifier::getSimple)
                .filter(dedupSetPk::add)
                .collect(Collectors.toList());

        createTblCmd.primaryKeyColumns(pkCols);

        List<String> colocationCols = createTblNode.colocationColumns() == null
                ? null
                : createTblNode.colocationColumns().getList().stream()
                        .map(SqlIdentifier.class::cast)
                        .map(SqlIdentifier::getSimple)
                        .collect(Collectors.toList());

        createTblCmd.colocationColumns(colocationCols);

        List<SqlColumnDeclaration> colDeclarations = createTblNode.columnList().getList().stream()
                .filter(SqlColumnDeclaration.class::isInstance)
                .map(SqlColumnDeclaration.class::cast)
                .collect(Collectors.toList());

        IgnitePlanner planner = ctx.planner();

        List<ColumnDefinition> cols = new ArrayList<>(colDeclarations.size());

        for (SqlColumnDeclaration col : colDeclarations) {
            if (!col.name.isSimple()) {
                throw new SqlException(QUERY_INVALID_ERR, "Unexpected value of columnName ["
                        + "expected a simple identifier, but was " + col.name + "; "
                        + "querySql=\"" + ctx.query() + "\"]");
            }

            String name = col.name.getSimple();

            if (col.dataType.getNullable() != null && col.dataType.getNullable() && dedupSetPk.contains(name)) {
                throw new SqlException(QUERY_INVALID_ERR, "Primary key cannot contain nullable column [col=" + name + "]");
            }

            RelDataType relType = planner.convert(col.dataType, !dedupSetPk.contains(name));

            dedupSetPk.remove(name);

            DefaultValueDefinition dflt = convertDefault(col.expression, relType);

            cols.add(new ColumnDefinition(name, relType, dflt));
        }

        if (!dedupSetPk.isEmpty()) {
            throw new SqlException(QUERY_INVALID_ERR, "Primary key constraint contains undefined columns: [cols=" + dedupSetPk + "]");
        }

        createTblCmd.columns(cols);

        return createTblCmd;
    }

    /**
     * Converts a given IgniteSqlAlterTableAddColumn AST to a AlterTableAddCommand.
     *
     * @param alterTblNode Root node of the given AST.
     * @param ctx          Planning context.
     */
    private AlterTableAddCommand convertAlterTableAdd(IgniteSqlAlterTableAddColumn alterTblNode, PlanningContext ctx) {
        AlterTableAddCommand alterTblCmd = new AlterTableAddCommand();

        alterTblCmd.schemaName(deriveSchemaName(alterTblNode.name(), ctx));
        alterTblCmd.tableName(deriveObjectName(alterTblNode.name(), ctx, "table name"));
        alterTblCmd.ifTableExists(alterTblNode.ifExists());
        alterTblCmd.ifColumnNotExists(alterTblNode.ifNotExistsColumn());

        List<ColumnDefinition> cols = new ArrayList<>(alterTblNode.columns().size());

        for (SqlNode colNode : alterTblNode.columns()) {
            assert colNode instanceof SqlColumnDeclaration : colNode.getClass();

            SqlColumnDeclaration col = (SqlColumnDeclaration) colNode;

            assert col.name.isSimple();

            RelDataType relType = ctx.planner().convert(col.dataType, true);
            DefaultValueDefinition dflt = convertDefault(col.expression, relType);

            String name = col.name.getSimple();

            cols.add(new ColumnDefinition(name, relType, dflt));
        }

        alterTblCmd.columns(cols);

        return alterTblCmd;
    }

    private DefaultValueDefinition convertDefault(SqlNode expression, RelDataType relType) {
        if (expression instanceof SqlIdentifier) {
            return DefaultValueDefinition.functionCall(((SqlIdentifier) expression).getSimple());
        }

        Object val = null;

        if (expression instanceof SqlLiteral) {
            val = fromLiteral(relType, (SqlLiteral) expression);
        }

        return DefaultValueDefinition.constant(val);
    }

    /**
     * Converts a given IgniteSqlAlterTableDropColumn AST to a AlterTableDropCommand.
     *
     * @param alterTblNode Root node of the given AST.
     * @param ctx          Planning context.
     */
    private AlterTableDropCommand convertAlterTableDrop(IgniteSqlAlterTableDropColumn alterTblNode, PlanningContext ctx) {
        AlterTableDropCommand alterTblCmd = new AlterTableDropCommand();

        alterTblCmd.schemaName(deriveSchemaName(alterTblNode.name(), ctx));
        alterTblCmd.tableName(deriveObjectName(alterTblNode.name(), ctx, "table name"));
        alterTblCmd.ifTableExists(alterTblNode.ifExists());
        alterTblCmd.ifColumnExists(alterTblNode.ifExistsColumn());

        Set<String> cols = new HashSet<>(alterTblNode.columns().size());
        alterTblNode.columns().forEach(c -> cols.add(((SqlIdentifier) c).getSimple()));

        alterTblCmd.columns(cols);

        return alterTblCmd;
    }

    /**
     * Converts a given DropTable AST to a DropTable command.
     *
     * @param dropTblNode Root node of the given AST.
     * @param ctx         Planning context.
     */
    private DropTableCommand convertDropTable(SqlDropTable dropTblNode, PlanningContext ctx) {
        DropTableCommand dropTblCmd = new DropTableCommand();

        dropTblCmd.schemaName(deriveSchemaName(dropTblNode.name, ctx));
        dropTblCmd.tableName(deriveObjectName(dropTblNode.name, ctx, "tableName"));
        dropTblCmd.ifTableExists(dropTblNode.ifExists);

        return dropTblCmd;
    }

    /**
     * Converts create index to appropriate wrapper.
     */
    private CreateIndexCommand convertAddIndex(IgniteSqlCreateIndex sqlCmd, PlanningContext ctx) {
        CreateIndexCommand createIdxCmd = new CreateIndexCommand();

        createIdxCmd.schemaName(deriveSchemaName(sqlCmd.tableName(), ctx));
        createIdxCmd.tableName(deriveObjectName(sqlCmd.tableName(), ctx, "table name"));
        createIdxCmd.indexName(sqlCmd.indexName().getSimple());
        createIdxCmd.type(convertIndexType(sqlCmd.type()));

        List<String> columns = new ArrayList<>(sqlCmd.columnList().size());
        List<Collation> collations = new ArrayList<>(sqlCmd.columnList().size());

        for (SqlNode col : sqlCmd.columnList().getList()) {
            boolean desc = false;

            if (col.getKind() == SqlKind.DESCENDING) {
                col = ((SqlCall) col).getOperandList().get(0);

                desc = true;
            }

            columns.add(((SqlIdentifier) col).getSimple());
            collations.add(desc ? Collation.DESC_NULLS_FIRST : Collation.ASC_NULLS_LAST);
        }

        createIdxCmd.columns(columns);

        if (createIdxCmd.type() == Type.SORTED) {
            createIdxCmd.collations(collations);
        }

        createIdxCmd.ifNotExists(sqlCmd.ifNotExists());

        return createIdxCmd;
    }

    /**
     * Converts drop index to appropriate wrapper.
     */
    private DropIndexCommand convertDropIndex(IgniteSqlDropIndex sqlCmd, PlanningContext ctx) {
        DropIndexCommand dropCmd = new DropIndexCommand();

        String schemaName = deriveSchemaName(sqlCmd.indexName(), ctx);
        String indexName = deriveObjectName(sqlCmd.indexName(), ctx, "index name");

        dropCmd.schemaName(schemaName);
        dropCmd.indexName(indexName);
        dropCmd.ifNotExists(sqlCmd.ifExists());

        return dropCmd;
    }

    /** Derives a schema name from the compound identifier. */
    private String deriveSchemaName(SqlIdentifier id, PlanningContext ctx) {
        String schemaName;
        if (id.isSimple()) {
            schemaName = ctx.schemaName();
        } else {
            SqlIdentifier schemaId = id.skipLast(1);

            if (!schemaId.isSimple()) {
                throw new SqlException(QUERY_INVALID_ERR, "Unexpected value of schemaName ["
                        + "expected a simple identifier, but was " + schemaId + "; "
                        + "querySql=\"" + ctx.query() + "\"]");
            }

            schemaName = schemaId.getSimple();
        }

        ensureSchemaExists(ctx, schemaName);

        return schemaName;
    }

    /** Derives an object(a table, an index, etc) name from the compound identifier. */
    private String deriveObjectName(SqlIdentifier id, PlanningContext ctx, String objDesc) {
        if (id.isSimple()) {
            return id.getSimple();
        }

        SqlIdentifier objId = id.getComponent(id.skipLast(1).names.size());

        if (!objId.isSimple()) {
            throw new SqlException(QUERY_INVALID_ERR, "Unexpected value of " + objDesc + " ["
                    + "expected a simple identifier, but was " + objId + "; "
                    + "querySql=\"" + ctx.query() + "\"]");
        }

        return objId.getSimple();
    }

    private void ensureSchemaExists(PlanningContext ctx, String schemaName) {
        if (ctx.catalogReader().getRootSchema().getSubSchema(schemaName, true) == null) {
            throw new SqlException(SCHEMA_NOT_FOUND_ERR, "Schema with name " + schemaName + " not found");
        }
    }

    /**
     * Collects a mapping of the ID of the data storage to a name.
     *
     * <p>Example: {@code collectDataStorageNames(Set.of("rocksdb"))} -> {@code Map.of("ROCKSDB", "rocksdb")}.
     *
     * @param dataStorages Names of the data storages.
     * @throws IllegalStateException If there is a duplicate ID.
     */
    static Map<String, String> collectDataStorageNames(Set<String> dataStorages) {
        return dataStorages.stream().collect(toUnmodifiableMap(String::toUpperCase, identity()));
    }

    /**
     * Collects a mapping of the ID of the table option to a table option info.
     *
     * <p>Example for "replicas": {@code Map.of("REPLICAS", TableOptionInfo@123)}.
     *
     * @param tableOptionInfos Table option information's.
     * @throws IllegalStateException If there is a duplicate ID.
     */
    static Map<String, TableOptionInfo<?>> collectTableOptionInfos(TableOptionInfo<?>... tableOptionInfos) {
        return ArrayUtils.nullOrEmpty(tableOptionInfos) ? Map.of() : Stream.of(tableOptionInfos).collect(toUnmodifiableMap(
                tableOptionInfo -> tableOptionInfo.name.toUpperCase(),
                identity()
        ));
    }

    /**
     * Checks that there are no ID duplicates.
     *
     * @param tableOptionInfos0 Table options information.
     * @param tableOptionInfos1 Table options information.
     * @throws IllegalStateException If there is a duplicate ID.
     */
    static void checkDuplicates(Map<String, TableOptionInfo<?>> tableOptionInfos0, Map<String, TableOptionInfo<?>> tableOptionInfos1) {
        for (String id : tableOptionInfos1.keySet()) {
            if (tableOptionInfos0.containsKey(id)) {
                throw new IllegalStateException("Duplicate id:" + id);
            }
        }
    }

    private String deriveDataStorage(@Nullable SqlIdentifier engineName, PlanningContext ctx) {
        if (engineName == null) {
            String defaultDataStorage = defaultDataStorageSupplier.get();

            if (defaultDataStorage.equals(UNKNOWN_DATA_STORAGE)) {
                throw new SqlException(STORAGE_ENGINE_NOT_VALID_ERR, "Default data storage is not defined, query:" + ctx.query());
            }

            return defaultDataStorage;
        }

        assert engineName.isSimple() : engineName;

        String dataStorage = engineName.getSimple().toUpperCase();

        if (!dataStorageNames.containsKey(dataStorage)) {
            throw new SqlException(STORAGE_ENGINE_NOT_VALID_ERR, String.format(
                    "Unexpected data storage engine [engine=%s, expected=%s, query=%s]",
                    dataStorage, dataStorageNames, ctx.query()
            ));
        }

        return dataStorageNames.get(dataStorage);
    }

    private void processTableOption(
            TableOptionInfo tableOptionInfo,
            IgniteSqlCreateTableOption option,
            PlanningContext context,
            CreateTableCommand createTableCommand
    ) {
        assert option.value() instanceof SqlLiteral : option.value();

        Object optionValue;

        try {
            optionValue = ((SqlLiteral) option.value()).getValueAs(tableOptionInfo.type);
        } catch (AssertionError | ClassCastException e) {
            throw new IgniteException(TABLE_OPTION_ERR, String.format(
                    "Unsuspected table option type [option=%s, expectedType=%s, query=%s]",
                    option.key().getSimple(),
                    tableOptionInfo.type.getSimpleName(),
                    context.query())
            );
        }

        if (tableOptionInfo.validator != null) {
            try {
                tableOptionInfo.validator.accept(optionValue);
            } catch (Throwable e) {
                throw new IgniteException(TABLE_OPTION_ERR, String.format(
                        "Table option validation failed [option=%s, err=%s, query=%s]",
                        option.key().getSimple(),
                        e.getMessage(),
                        context.query()
                ), e);
            }
        }

        tableOptionInfo.setter.accept(createTableCommand, optionValue);
    }

    private void checkPositiveNumber(int num) {
        if (num < 0) {
            throw new IgniteException(TABLE_OPTION_ERR, "Must be positive:" + num);
        }
    }

    private TableOptionInfo<?> dataStorageFieldOptionInfo(Entry<String, Class<?>> e) {
        return new TableOptionInfo<>(e.getKey(), e.getValue(), null, (cmd, o) -> cmd.addDataStorageOption(e.getKey(), o));
    }

    private Type convertIndexType(IgniteSqlIndexType type) {
        switch (type) {
            case TREE:
            case IMPLICIT_TREE:
                return Type.SORTED;
            case HASH:
                return Type.HASH;
            default:
                throw new AssertionError("Unknown index type [type=" + type + "]");
        }
    }

    /**
     * Creates a value of required type from the literal.
     */
    private static Object fromLiteral(RelDataType columnType, SqlLiteral literal) {
        try {
            switch (columnType.getSqlTypeName()) {
                case VARCHAR:
                case CHAR:
                    return literal.getValueAs(String.class);
                case DATE:
                    return LocalDate.ofEpochDay(literal.getValueAs(DateString.class).getDaysSinceEpoch());
                case TIME:
                    return LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(literal.getValueAs(TimeString.class).getMillisOfDay()));
                case TIMESTAMP: {
                    var tsString = literal.getValueAs(TimestampString.class);

                    return LocalDateTime.ofEpochSecond(
                            TimeUnit.MILLISECONDS.toSeconds(tsString.getMillisSinceEpoch()),
                            (int) (TimeUnit.MILLISECONDS.toNanos(tsString.getMillisSinceEpoch() % 1000)),
                            ZoneOffset.UTC
                    );
                }
                case TIMESTAMP_WITH_LOCAL_TIME_ZONE: {
                    // TODO: IGNITE-17376
                    throw new UnsupportedOperationException("https://issues.apache.org/jira/browse/IGNITE-17376");
                }
                case INTEGER:
                    return literal.getValueAs(Integer.class);
                case BIGINT:
                    return literal.getValueAs(Long.class);
                case SMALLINT:
                    return literal.getValueAs(Short.class);
                case TINYINT:
                    return literal.getValueAs(Byte.class);
                case DECIMAL:
                    return literal.getValueAs(BigDecimal.class);
                case DOUBLE:
                    return literal.getValueAs(Double.class);
                case REAL:
                case FLOAT:
                    return literal.getValueAs(Float.class);
                case BINARY:
                case VARBINARY:
                    return literal.getValueAs(byte[].class);
                default:
                    throw new IllegalStateException("Unknown type [type=" + columnType + ']');
            }
        } catch (Throwable th) {
            // catch throwable here because literal throws an AssertionError when unable to cast value to a given class
            throw new SqlException(SQL_TO_REL_CONVERSION_ERR, "Unable co convert literal", th);
        }
    }
}
