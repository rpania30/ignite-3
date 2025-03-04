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

package org.apache.ignite.sql;

import java.util.List;

/**
 * ResultSet metadata.
 */
public interface ResultSetMetadata {
    /**
     * Returns metadata with description for every column in result set.
     *
     * @return Columns metadata.
     */
    List<ColumnMetadata> columns();

    /**
     * Returns column index in columns list by given column name.
     *
     * @param columnName Columns name which index is resolving.
     * @return Column index, or {@code -1} when a column with given name is not present.
     */
    int indexOf(String columnName);
}
