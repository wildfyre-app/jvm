/*
 * Copyright 2018 Wildfyre.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wildfyre.areas;

import net.wildfyre.descriptors.NoSuchEntityException;
import net.wildfyre.http.Request;
import net.wildfyre.http.RequestTest;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

public class AreaTest {

    @BeforeClass
    static public void before() {
        RequestTest.Companion.connectToTestDB();
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(2);

    @Test
    public void testQuery() throws Request.CantConnectException {
        Areas.load();

        assertTrue(Areas.get("sample").isPresent());
        assertEquals("sample", Areas.get("sample").get().getID());
        assertEquals("Sample", Areas.get("sample").get().getName());
    }

    @Test
    public void testUpdate() throws Request.CantConnectException, NoSuchEntityException {
        Area a = new Area("sample", null);
        a.update(); // this test fails if any exception is thrown, success if no exception is thrown
    }

    @Test
    public void noArea() throws Request.CantConnectException {
        Areas.clear();
        Areas.load();

        assertFalse( Areas
            .get("this-area-doesn't-exist-5465454")
            .isPresent()
        );
    }

    @Test
    public void testRemovedArea() throws NoSuchFieldException, IllegalAccessException, Request.CantConnectException{
        // This test checks the case where an Area has been removed, and is then deleted server-side.
        // Since this is not possible to do natively, I load an Area with a good ID, then use reflection to modify
        // that ID, then reload.

        Area area = Areas.get("sample")
            .orElseThrow(RuntimeException::new);

        Field id = area
            .getClass()
            .getDeclaredField("ID");

        id.setAccessible(true);

        Field modifiers = id
            .getClass()
            .getDeclaredField("modifiers");

        modifiers.setAccessible(true);
        modifiers.setInt(id, id.getModifiers() & ~Modifier.FINAL);

        id.set(area, "this-is-a-wrong-id-that-area-doesnt-exist-1564864");

        try {
            area.update();
            fail("This should have thrown a NoSuchEntityException, since the Area cannot update.");

        } catch (NoSuchEntityException e) {
            assertEquals (
                area,
                e.asArea()
                    .orElseThrow(NullPointerException::new)
            );
        }
    }

}
