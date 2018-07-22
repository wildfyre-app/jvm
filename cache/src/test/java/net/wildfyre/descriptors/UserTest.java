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

package net.wildfyre.descriptors;

import net.wildfyre.http.Request;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserTest {

    private static final String token = "9d36a784f7bc641b9d0f7a000a96b6563b987956";

    @Before
    public void before() throws Request.CantConnectException {
        Internal.setToken(token);
        Internal.init();
    }

    @Test
    public void createTest() {
        int id = Internal.getMyId();
        User me = User.create(id);
        assertTrue(me.toString(), me instanceof LoggedUser);
        assertTrue(me.toString(), me.canEdit());

        int wrongId = id+1;
        User wrong = User.create(wrongId);
        assertFalse(wrong.toString(), wrong instanceof LoggedUser);
        assertFalse(wrong.toString(), wrong.canEdit());
    }

    @Test(timeout = 1000L)
    public void query() throws MalformedURLException {
        User me = User.query(Internal.getMyId());

        assertEquals("libtester", me.getName());
        assertEquals(3, me.getID());
        assertEquals("http://localhost:8000/media/avatar/3.png", me.getAvatar());
        assertEquals(new URL("http://localhost:8000/media/avatar/3.png"), me.getAvatarUrl());
        assertEquals("This is libtester's bio.", me.getBio());
    }
}
