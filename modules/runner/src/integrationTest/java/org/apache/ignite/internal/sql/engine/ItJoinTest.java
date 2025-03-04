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

package org.apache.ignite.internal.sql.engine;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ignite.internal.sql.engine.util.QueryChecker;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Check JOIN on basic cases.
 */
public class ItJoinTest extends AbstractBasicIntegrationTest {
    @BeforeAll
    public static void beforeTestsStarted() {
        sql("CREATE TABLE t1 (id INT PRIMARY KEY, c1 INT NOT NULL, c2 INT, c3 INT)");
        sql("CREATE TABLE t2 (id INT PRIMARY KEY, c1 INT NOT NULL, c2 INT, c3 INT)");

        // TODO: support indexes. https://issues.apache.org/jira/browse/IGNITE-17304
        // sql("create index t1_idx on t1 (c3, c2, c1)");
        // sql("create index t2_idx on t2 (c3, c2, c1)");

        insertData("t1", List.of("ID", "C1", "C2", "C3"),
                new Object[] {0, 1, 1, 1},
                new Object[] {1, 2, null, 2},
                new Object[] {2, 2, 2, 2},
                new Object[] {3, 3, 3, null},
                new Object[] {4, 3, 3, 3},
                new Object[] {5, 4, 4, 4}
        );

        insertData("t2", List.of("ID", "C1", "C2", "C3"),
                new Object[] {0, 1, 1, 1},
                new Object[] {1, 2, 2, null},
                new Object[] {2, 2, 2, 2},
                new Object[] {3, 3, null, 3},
                new Object[] {4, 3, 3, 3},
                new Object[] {5, 4, 4, 4}
        );
    }

    /**
     * Test verifies result of inner join with different ordering.
     */
    @ParameterizedTest
    @EnumSource
    public void testInnerJoin(JoinType joinType) {
        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1, t1.c2, t1.c3",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1, t1.c2, t1.c3 nulls first",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1, t1.c2, t1.c3 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, null, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1 desc, t1.c2, t1.c3",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1 desc, t1.c2, t1.c3 nulls first",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1 desc, t1.c2, t1.c3 nulls last",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, null, 3, 3)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t2.c3 c23, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c3 = t2.c3 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3, t1.c2",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1)
                .returns(2, 2, 2, 2)
                .returns(3, 3, 3, 3)
                .returns(4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c3 = t2.c3 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3 nulls first, t1.c2 nulls first, t1.c1 nulls first",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3 nulls first, t1.c2 nulls first, t1.c1 nulls first",
                joinType
        )
                .ordered()
                .returns(null, 3, 3, 3, 3)
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3 nulls last, t1.c2 nulls last, t1.c1 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .returns(null, 3, 3, 3, 3)
                .check();
    }

    /**
     * Test verifies result of left join with different ordering.
     */
    @ParameterizedTest
    @EnumSource
    public void testLeftJoin(JoinType joinType) {
        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1, t1.c2, t1.c3",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, null, 2, null, null)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1, t1.c2 nulls first, t1.c3 nulls first",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, null, 2, null, null)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1, t1.c2 nulls last, t1.c3 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(2, null, 2, null, null)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, null, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1 desc, t1.c2, t1.c3",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(2, null, 2, null, null)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1 desc, t1.c2, t1.c3 nulls first",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(3, 3, null, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(2, null, 2, null, null)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t1.c3 c13, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c1 desc, t1.c2, t1.c3 nulls last",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, null, 3, 3)
                .returns(2, null, 2, null, null)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t2.c3 c23, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c3 = t2.c3 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3, t1.c2",
                joinType
        )
                .ordered()
                .returns(null, 3, null, null)
                .returns(1, 1, 1, 1)
                .returns(2, null, null, null)
                .returns(2, 2, 2, 2)
                .returns(3, 3, 3, 3)
                .returns(4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c3 = t2.c3 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3 nulls first, t1.c2 nulls first, t1.c1 nulls first",
                joinType
        )
                .ordered()
                .returns(null, 3, 3, null, null)
                .returns(1, 1, 1, 1, 1)
                .returns(2, null, 2, null, null)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c3 = t2.c3 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3 nulls last, t1.c2 nulls last, t1.c1 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, null, 2, null, null)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .returns(null, 3, 3, null, null)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3 nulls first, t1.c2 nulls first, t1.c1 nulls first",
                joinType
        )
                .ordered()
                .returns(null, 3, 3, 3, 3)
                .returns(1, 1, 1, 1, 1)
                .returns(2, null, 2, null, null)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22 "
                + "  from t1 "
                + "  left join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t1.c3 nulls last, t1.c2 nulls last, t1.c1 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 2)
                .returns(2, null, 2, null, null)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .returns(null, 3, 3, 3, 3)
                .check();
    }

    /**
     * Test verifies result of right join with different ordering.
     */
    @ParameterizedTest
    @EnumSource
    public void testRightJoin(JoinType joinType) {
        Assumptions.assumeTrue(joinType != JoinType.CORRELATED);

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c1, t2.c2, t2.c3",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, null)
                .returns(2, 2, 2, 2, 2)
                .returns(null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c1, t2.c2 nulls first, t2.c3 nulls first",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, null)
                .returns(2, 2, 2, 2, 2)
                .returns(null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c1, t2.c2 nulls last, t2.c3 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, null)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(null, null, 3, null, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c1 desc, t2.c2, t2.c3",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(2, 2, 2, 2, null)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c1 desc, t2.c2, t2.c3 nulls first",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(2, 2, 2, 2, null)
                .returns(2, 2, 2, 2, 2)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c1 c11, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c1 desc, t2.c2, t2.c3 nulls last",
                joinType
        )
                .ordered()
                .returns(4, 4, 4, 4, 4)
                .returns(null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, null)
                .returns(1, 1, 1, 1, 1)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t2.c3 c23, t2.c2 c22 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c3 = t2.c3 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c3, t2.c2",
                joinType
        )
                .ordered()
                .returns(null, null, null, 2)
                .returns(1, 1, 1, 1)
                .returns(2, 2, 2, 2)
                .returns(null, null, 3, null)
                .returns(3, 3, 3, 3)
                .returns(4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + " right join t2 "
                + "    on t1.c3 = t2.c3 "
                + "   and t1.c2 = t2.c2 "
                + " order by t2.c3 nulls first, t2.c2 nulls first, t2.c1 nulls first",
                joinType
        )
                .ordered()
                .returns(null, null, 2, 2, null)
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + "  right join t2 "
                + "    on t1.c3 = t2.c3 "
                + "    and t1.c2 = t2.c2 "
                + " order by t2.c3 nulls last, t2.c2 nulls last, t2.c1 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(null, null, 3, null, 3)
                .returns(4, 4, 4, 4, 4)
                .returns(null, null, 2, 2, null)
                .check();

        assertQuery(""
                + "select t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + "  right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "    and t1.c2 = t2.c2 "
                + " order by t2.c3 nulls first, t2.c2 nulls first, t2.c1 nulls first",
                joinType
        )
                .ordered()
                .returns(2, 2, 2, 2, null)
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + "  right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "    and t1.c2 = t2.c2 "
                + " order by t2.c3 nulls last, t2.c2 nulls last, t2.c1 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3)
                .returns(3, 3, 3, 3, 3)
                .returns(null, null, 3, null, 3)
                .returns(4, 4, 4, 4, 4)
                .returns(2, 2, 2, 2, null)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + "  right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "    and t1.c2 = t2.c2 "
                + "   and t1.c3 = t2.c3 "
                + " order by t2.c3 nulls first, t2.c2 nulls first, t2.c1 nulls first",
                joinType
        )
                .ordered()
                .returns(null, null, null, 2, 2, null)
                .returns(1, 1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2, 2)
                .returns(null, null, null, 3, null, 3)
                .returns(3, 3, 3, 3, 3, 3)
                .returns(4, 4, 4, 4, 4, 4)
                .check();

        assertQuery(""
                + "select t1.c3 c13, t1.c2 c12, t1.c1 c11, t2.c1 c21, t2.c2 c22, t2.c3 c23 "
                + "  from t1 "
                + "  right join t2 "
                + "    on t1.c1 = t2.c1 "
                + "    and t1.c2 = t2.c2 "
                + "    and t1.c3 = t2.c3 "
                + " order by t2.c3 nulls last, t2.c2 nulls last, t2.c1 nulls last",
                joinType
        )
                .ordered()
                .returns(1, 1, 1, 1, 1, 1)
                .returns(2, 2, 2, 2, 2, 2)
                .returns(3, 3, 3, 3, 3, 3)
                .returns(null, null, null, 3, null, 3)
                .returns(4, 4, 4, 4, 4, 4)
                .returns(null, null, null, 2, 2, null)
                .check();
    }

    /**
     * Tests JOIN with USING clause.
     */
    @ParameterizedTest
    @EnumSource
    public void testJoinWithUsing(JoinType joinType) {
        // Select all join columns.
        assertQuery("SELECT * FROM t1 JOIN t2 USING (c1, c2)", joinType)
                .returns(1, 1, 0, 1, 0, 1)
                .returns(2, 2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 1, null)
                .returns(3, 3, 3, null, 4, 3)
                .returns(3, 3, 4, 3, 4, 3)
                .returns(4, 4, 5, 4, 5, 4)
                .check();

        // Select all table columns explicitly.
        assertQuery("SELECT t1.*, t2.* FROM t1 JOIN t2 USING (c1, c2)")
                .returns(0, 1, 1, 1, 0, 1, 1, 1)
                .returns(2, 2, 2, 2, 2, 2, 2, 2)
                .returns(2, 2, 2, 2, 1, 2, 2, null)
                .returns(3, 3, 3, null, 4, 3, 3, 3)
                .returns(4, 3, 3, 3, 4, 3, 3, 3)
                .returns(5, 4, 4, 4, 5, 4, 4, 4)
                .check();

        // Select explicit columns. Columns from using - not ambiguous.
        assertQuery("SELECT c1, c2, t1.c3, t2.c3 FROM t1 JOIN t2 USING (c1, c2) ORDER BY c1, c2")
                .returns(1, 1, 1, 1)
                .returns(2, 2, 2, null)
                .returns(2, 2, 2, 2)
                .returns(3, 3, null, 3)
                .returns(3, 3, 3, 3)
                .returns(4, 4, 4, 4)
                .check();
    }

    /**
     * Tests NATURAL JOIN.
     */
    @ParameterizedTest
    @EnumSource
    public void testNatural(JoinType joinType) {
        // Select all join columns.
        assertQuery("SELECT * FROM t1 NATURAL JOIN t2", joinType)
                .returns(0, 1, 1, 1)
                .returns(2, 2, 2, 2)
                .returns(4, 3, 3, 3)
                .returns(5, 4, 4, 4)
                .check();

        // Select all tables columns explicitly.
        assertQuery("SELECT t1.*, t2.* FROM t1 NATURAL JOIN t2", joinType)
                .returns(0, 1, 1, 1, 0, 1, 1, 1)
                .returns(2, 2, 2, 2, 2, 2, 2, 2)
                .returns(4, 3, 3, 3, 4, 3, 3, 3)
                .returns(5, 4, 4, 4, 5, 4, 4, 4)
                .check();

        // Select explicit columns.
        assertQuery("SELECT t1.c1, t2.c2, t1.c3, t2.c3 FROM t1 NATURAL JOIN t2", joinType)
                .returns(1, 1, 1, 1)
                .returns(2, 2, 2, 2)
                .returns(3, 3, 3, 3)
                .returns(4, 4, 4, 4)
                .check();

        // Columns - not ambiguous.
        // TODO https://issues.apache.org/jira/browse/CALCITE-4915
        //assertQuery("SELECT c1, c2, c3 FROM t1 NATURAL JOIN t2 ORDER BY c1, c2, c3")
        //    .returns(1, 1, 1)
        //    .returns(2, 2, 2)
        //    .returns(3, 3, 3)
        //    .returns(4, 4, 4)
        //    .check();
    }

    protected QueryChecker assertQuery(String qry, JoinType joinType) {
        return AbstractBasicIntegrationTest.assertQuery(qry.replace("select", "select "
            + Arrays.stream(joinType.disabledRules).collect(Collectors.joining("','", "/*+ DISABLE_RULE('", "') */"))));
    }

    enum JoinType {
        NESTED_LOOP(
            "CorrelatedNestedLoopJoin",
            "JoinCommuteRule",
            "MergeJoinConverter"
        ),

        MERGE(
            "CorrelatedNestedLoopJoin",
            "JoinCommuteRule",
            "NestedLoopJoinConverter"
        ),

        CORRELATED(
            "MergeJoinConverter",
            "JoinCommuteRule",
            "NestedLoopJoinConverter"
        );

        private final String[] disabledRules;

        JoinType(String... disabledRules) {
            this.disabledRules = disabledRules;
        }
    }
}
