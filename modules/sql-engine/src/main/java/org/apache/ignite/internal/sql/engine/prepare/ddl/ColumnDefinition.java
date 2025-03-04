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

import java.util.Objects;
import org.apache.calcite.rel.type.RelDataType;

/** Defines a particular column within table. */
public class ColumnDefinition {
    private final String name;

    private final RelDataType type;

    private final DefaultValueDefinition defaultValueDefinition;

    /** Creates a column definition. */
    public ColumnDefinition(String name, RelDataType type, DefaultValueDefinition defaultValueDefinition) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValueDefinition = Objects.requireNonNull(defaultValueDefinition, "defaultValueDefinition");
    }

    /**
     * Get column's name.
     */
    public String name() {
        return name;
    }

    /**
     * Get column's type.
     */
    public RelDataType type() {
        return type;
    }

    /**
     * Returns default value definition.
     *
     * @param <T> Desired subtype of the definition.
     * @return Default value definition.
     */
    @SuppressWarnings("unchecked")
    public <T extends DefaultValueDefinition> T defaultValueDefinition() {
        return (T) defaultValueDefinition;
    }

    /**
     * Get nullable flag: {@code true} if this column accepts nulls.
     */
    public boolean nullable() {
        return type.isNullable();
    }

    /**
     * Get column's precision.
     */
    public Integer precision() {
        return type.getPrecision() != RelDataType.PRECISION_NOT_SPECIFIED ? type.getPrecision() : null;
    }

    /**
     * Get column's scale.
     */
    public Integer scale() {
        return type.getScale() != RelDataType.SCALE_NOT_SPECIFIED ? type.getScale() : null;
    }
}
