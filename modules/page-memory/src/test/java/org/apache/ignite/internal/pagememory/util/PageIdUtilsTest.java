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

package org.apache.ignite.internal.pagememory.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.apache.ignite.internal.pagememory.PageIdAllocator;
import org.apache.ignite.internal.util.IgniteUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for methods of {@link PageIdUtils}.
 */
public class PageIdUtilsTest {
    @Test
    public void testRotatePageId() throws Exception {
        assertEquals(0x0102FFFFFFFFFFFFL, PageIdUtils.rotatePageId(0x0002FFFFFFFFFFFFL));
        assertEquals(0x0B02FFFFFFFFFFFFL, PageIdUtils.rotatePageId(0x0A02FFFFFFFFFFFFL));
        assertEquals(0x1002FFFFFFFFFFFFL, PageIdUtils.rotatePageId(0x0F02FFFFFFFFFFFFL));
        assertEquals(0x0102FFFFFFFFFFFFL, PageIdUtils.rotatePageId(0xFE02FFFFFFFFFFFFL));
        assertEquals(0x0102FFFFFFFFFFFFL, PageIdUtils.rotatePageId(0xFF02FFFFFFFFFFFFL));
    }

    @Test
    public void testEffectivePageId() throws Exception {
        assertEquals(0x0000FFFFFFFFFFFFL, PageIdUtils.effectivePageId(0x0002FFFFFFFFFFFFL));
        assertEquals(0x0000FFFFFFFFFFFFL, PageIdUtils.effectivePageId(0x0A02FFFFFFFFFFFFL));
        assertEquals(0x0000FFFFFFFFFFFFL, PageIdUtils.effectivePageId(0x0F02FFFFFFFFFFFFL));
        assertEquals(0x0000FFFFFFFFFFFFL, PageIdUtils.effectivePageId(0xFF02FFFFFFFFFFFFL));
    }

    @Test
    public void testLinkConstruction() throws Exception {
        assertEquals(0x00FFFFFFFFFFFFFFL, PageIdUtils.link(0xFFFFFFFFFFFFFFL, 0));
        assertEquals(0x01FFFFFFFFFFFFFFL, PageIdUtils.link(0xFFFFFFFFFFFFFFL, 1));

        assertEquals(0x0000000000000000L, PageIdUtils.link(0, 0));
        assertEquals(0x0100000000000000L, PageIdUtils.link(0, 1));

        assertEquals(0xF000000000000000L, PageIdUtils.link(0, 0xF0));
        assertEquals(0xF0FFFFFFFFFFFFFFL, PageIdUtils.link(0xFFFFFFFFFFFFFFL, 0xF0));

        assertEquals(0xFE00000000000000L, PageIdUtils.link(0, 0xFE));
        assertEquals(0xFEFFFFFFFFFFFFFFL, PageIdUtils.link(0xFFFFFFFFFFFFFFL, 0xFE));

        assertEquals(0x0F00000000000000L, PageIdUtils.link(0, 0xF));
        assertEquals(0x0FFFFFFFFFFFFFFFL, PageIdUtils.link(0xFFFFFFFFFFFFFFL, 0xF));
    }

    @Test
    public void testOffsetExtraction() throws Exception {
        assertEquals(0, PageIdUtils.itemId(0x00FFFFFFFFFFFFFFL));
        assertEquals(1, PageIdUtils.itemId(0x01FFFFFFFFFFFFFFL));

        assertEquals(0, PageIdUtils.itemId(0x0000000000000000L));
        assertEquals(1, PageIdUtils.itemId(0x0100000000000000L));

        assertEquals(0xFA, PageIdUtils.itemId(0xFA00000000000000L));
        assertEquals(0xFA, PageIdUtils.itemId(0xFAFFFFFFFFFFFFFFL));

        assertEquals(0xF, PageIdUtils.itemId(0x0F00000000000000L));
        assertEquals(0xF, PageIdUtils.itemId(0x0FFFFFFFFFFFFFFFL));

        assertEquals(0xF0, PageIdUtils.itemId(0xF000000000000000L));
        assertEquals(0xF0, PageIdUtils.itemId(0xF0FFFFFFFFFFFFFFL));
    }

    @Test
    public void testPageIdFromLink() throws Exception {
        assertEquals(0x00FFFFFFFFFFFFFFL, PageIdUtils.pageId(0x00FFFFFFFFFFFFFFL));

        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0x0001FFFFFFFFFFFFL));
        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0x1001FFFFFFFFFFFFL));
        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0x0101FFFFFFFFFFFFL));
        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0x1101FFFFFFFFFFFFL));
        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0x8001FFFFFFFFFFFFL));
        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0x8801FFFFFFFFFFFFL));
        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0x0801FFFFFFFFFFFFL));
        assertEquals(0x0001FFFFFFFFFFFFL, PageIdUtils.pageId(0xFF01FFFFFFFFFFFFL));

        assertEquals(0x0002FFFFFFFFFFFFL, PageIdUtils.pageId(0x0002FFFFFFFFFFFFL));
        assertEquals(0x1002FFFFFFFFFFFFL, PageIdUtils.pageId(0x1002FFFFFFFFFFFFL));
        assertEquals(0x0102FFFFFFFFFFFFL, PageIdUtils.pageId(0x0102FFFFFFFFFFFFL));
        assertEquals(0x1102FFFFFFFFFFFFL, PageIdUtils.pageId(0x1102FFFFFFFFFFFFL));
        assertEquals(0x8002FFFFFFFFFFFFL, PageIdUtils.pageId(0x8002FFFFFFFFFFFFL));
        assertEquals(0x8802FFFFFFFFFFFFL, PageIdUtils.pageId(0x8802FFFFFFFFFFFFL));
        assertEquals(0x0802FFFFFFFFFFFFL, PageIdUtils.pageId(0x0802FFFFFFFFFFFFL));
        assertEquals(0xFF02FFFFFFFFFFFFL, PageIdUtils.pageId(0xFF02FFFFFFFFFFFFL));

        assertEquals(0x0004FFFFFFFFFFFFL, PageIdUtils.pageId(0x0004FFFFFFFFFFFFL));
        assertEquals(0x1004FFFFFFFFFFFFL, PageIdUtils.pageId(0x1004FFFFFFFFFFFFL));
        assertEquals(0x0104FFFFFFFFFFFFL, PageIdUtils.pageId(0x0104FFFFFFFFFFFFL));
        assertEquals(0x1104FFFFFFFFFFFFL, PageIdUtils.pageId(0x1104FFFFFFFFFFFFL));
        assertEquals(0x8004FFFFFFFFFFFFL, PageIdUtils.pageId(0x8004FFFFFFFFFFFFL));
        assertEquals(0x8804FFFFFFFFFFFFL, PageIdUtils.pageId(0x8804FFFFFFFFFFFFL));
        assertEquals(0x0804FFFFFFFFFFFFL, PageIdUtils.pageId(0x0804FFFFFFFFFFFFL));
        assertEquals(0xFF04FFFFFFFFFFFFL, PageIdUtils.pageId(0xFF04FFFFFFFFFFFFL));

        assertEquals(0x0000000000000000L, PageIdUtils.pageId(0x0000000000000000L));
        assertEquals(0x1000000000000000L, PageIdUtils.pageId(0x1000000000000000L));
        assertEquals(0x0100000000000000L, PageIdUtils.pageId(0x0100000000000000L));
        assertEquals(0x8000000000000000L, PageIdUtils.pageId(0x8000000000000000L));
        assertEquals(0x0800000000000000L, PageIdUtils.pageId(0x0800000000000000L));
        assertEquals(0xFF00000000000000L, PageIdUtils.pageId(0xFF00000000000000L));
    }

    @Test
    public void testRandomIds() {
        Random rnd = new Random();

        for (int i = 0; i < 50_000; i++) {
            int off = rnd.nextInt(PageIdUtils.MAX_ITEM_ID_NUM + 1);
            int partId = rnd.nextInt(PageIdUtils.MAX_PART_ID + 1);
            int pageNum = rnd.nextInt();

            long pageId = PageIdUtils.pageId(partId, PageIdAllocator.FLAG_DATA, pageNum);

            String msg = "For values [offset=" + IgniteUtils.hexLong(off) + ", fileId=" + IgniteUtils.hexLong(partId)
                    + ", pageNum=" + IgniteUtils.hexLong(pageNum) + ']';

            assertEquals(pageId, PageIdUtils.pageId(pageId), msg);
            assertEquals(0, PageIdUtils.itemId(pageId), msg);

            long link = PageIdUtils.link(pageId, off);

            assertEquals(pageId, PageIdUtils.pageId(link), msg);
            assertEquals(off, PageIdUtils.itemId(link), msg);
            assertEquals(pageId, PageIdUtils.pageId(link), msg);
        }
    }
}
