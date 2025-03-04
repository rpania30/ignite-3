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

package org.apache.ignite.internal.runner.app.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for ddl queries that contain multiply sql statements, separated by ";".
 */
@Disabled("https://issues.apache.org/jira/browse/IGNITE-16204")
public class ItJdbcMultiStatementSelfTest extends AbstractJdbcSelfTest {
    /**
     * Setup tables.
     */
    @BeforeEach
    public void setupTables() throws Exception {
        execute("DROP TABLE IF EXISTS TEST_TX; "
                + "DROP TABLE IF EXISTS PUBLIC.TRANSACTIONS; "
                + "DROP TABLE IF EXISTS ONE;"
                + "DROP TABLE IF EXISTS TWO;");

        execute("CREATE TABLE TEST_TX (ID INT PRIMARY KEY, AGE INT, NAME VARCHAR) ");

        execute("INSERT INTO TEST_TX VALUES "
                + "(1, 17, 'James'), "
                + "(2, 43, 'Valery'), "
                + "(3, 25, 'Michel'), "
                + "(4, 19, 'Nick');");
    }

    /**
     * Execute sql script using thin driver.
     */
    private void execute(String sql) throws Exception {
        stmt.executeUpdate(sql);
    }

    /**
     * Assert that script containing both h2 and non h2 (native) sql statements is handled correctly.
     */
    @Test
    public void testMixedCommands() throws Exception {
        execute("CREATE TABLE public.transactions (pk INT, id INT, k VARCHAR, v VARCHAR, PRIMARY KEY (pk, id)); "
                + "CREATE INDEX transactions_id_k_v ON public.transactions (id, k, v) INLINE_SIZE 150; "
                + "INSERT INTO public.transactions VALUES (1,2,'some', 'word') ; "
                + "CREATE INDEX transactions_k_v_id ON public.transactions (k, v, id) INLINE_SIZE 150; "
                + "CREATE INDEX transactions_pk_id ON public.transactions (pk, id) INLINE_SIZE 20;");
    }

    /**
     * Sanity test for scripts, containing empty statements are handled correctly.
     */
    @Test
    public void testEmptyStatements() throws Exception {
        execute(";; ;;;;");
        execute(" ;; ;;;; ");
        execute("CREATE TABLE ONE (id INT PRIMARY KEY, VAL VARCHAR);;"
                + "CREATE INDEX T_IDX ON ONE(val)"
                + ";;UPDATE ONE SET VAL = 'SOME';;;  ");
        execute("DROP INDEX T_IDX ;;  ;;"
                + "UPDATE ONE SET VAL = 'SOME'");
    }

    /**
     * Check multi-statement containing both h2 and native parser statements (having "?" args) works well.
     */
    @Test
    public void testMultiStatementTxWithParams() throws Exception {
        int leoAge = 28;

        String nickolas = "Nickolas";

        int gabAge = 84;
        String gabName = "Gab";

        int delYounger = 19;

        String complexQuery =
                "INSERT INTO TEST_TX VALUES (5, ?, 'Leo'); " // 1
                    + ";;;;"
                    + "BEGIN ; "
                    + "UPDATE TEST_TX  SET name = ? WHERE name = 'Nick' ;" // 2
                    + "INSERT INTO TEST_TX VALUES (6, ?, ?); "   // 3, 4
                    + "DELETE FROM TEST_TX WHERE age < ?; "   // 5
                    + "COMMIT;";

        try (PreparedStatement p = conn.prepareStatement(complexQuery)) {
            p.setInt(1, leoAge);
            p.setString(2, nickolas);
            p.setInt(3, gabAge);
            p.setString(4, gabName);
            p.setInt(5, delYounger);

            assertFalse(p.execute(), "Expected, that first result is an update count.");

            assertTrue(p.getUpdateCount() != -1, "Expected update count of the INSERT.");
            assertTrue(p.getMoreResults(), "More results are expected.");

            assertTrue(p.getUpdateCount() != -1, "Expected update count of an empty statement.");
            assertTrue(p.getMoreResults(), "More results are expected.");
            assertTrue(p.getUpdateCount() != -1, "Expected update count of an empty statement.");
            assertTrue(p.getMoreResults(), "More results are expected.");
            assertTrue(p.getUpdateCount() != -1, "Expected update count of an empty statement.");
            assertTrue(p.getMoreResults(), "More results are expected.");
            assertTrue(p.getUpdateCount() != -1, "Expected update count of an empty statement.");
            assertTrue(p.getMoreResults(), "More results are expected.");

            assertTrue(p.getUpdateCount() != -1, "Expected update count of the BEGIN");
            assertTrue(p.getMoreResults(), "More results are expected.");
            assertTrue(p.getUpdateCount() != -1, "Expected update count of the UPDATE");
            assertTrue(p.getMoreResults(), "More results are expected.");
            assertTrue(p.getUpdateCount() != -1, "Expected update count of the INSERT");
            assertTrue(p.getMoreResults(), "More results are expected.");
            assertTrue(p.getUpdateCount() != -1, "Expected update count of the DELETE");
            assertTrue(p.getMoreResults(), "More results are expected.");
            assertTrue(p.getUpdateCount() != -1, "Expected update count of the COMMIT");

            assertFalse(p.getMoreResults(), "There should have been no results.");
            assertFalse(p.getUpdateCount() != -1, "There should have been no update results.");
        }

        try (PreparedStatement sel = conn.prepareStatement("SELECT * FROM TEST_TX ORDER BY ID;")) {
            try (ResultSet pers = sel.executeQuery()) {
                assertTrue(pers.next());
                assertEquals(43, age(pers));
                assertEquals("Valery", name(pers));

                assertTrue(pers.next());
                assertEquals(25, age(pers));
                assertEquals("Michel", name(pers));

                assertTrue(pers.next());
                assertEquals(19, age(pers));
                assertEquals("Nickolas", name(pers));

                assertTrue(pers.next());
                assertEquals(28, age(pers));
                assertEquals("Leo", name(pers));

                assertTrue(pers.next());
                assertEquals(84, age(pers));
                assertEquals("Gab", name(pers));

                assertFalse(pers.next());
            }
        }
    }

    /**
     * Extract person's name from result set.
     */
    private static String name(ResultSet rs) throws SQLException {
        return rs.getString("NAME");
    }

    /**
     * Extract person's age from result set.
     */
    private static int age(ResultSet rs) throws SQLException {
        return rs.getInt("AGE");
    }

}
