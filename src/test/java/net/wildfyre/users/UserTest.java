/*
 * Copyright 2019 Wildfyre.net
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

package net.wildfyre.users;

import net.wildfyre.api.Internal;
import net.wildfyre.api.WildFyre;
import net.wildfyre.http.Request;
import net.wildfyre.http.RequestTest;
import net.wildfyre.utils.InvalidCredentialsException;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class UserTest {

    @Before
    public void before() throws Request.CantConnectException {
        Internal.setToken(RequestTest.Companion.getToken());
        Internal.init();
    }

    @Test
    public void createTest() {
        int id = Users.myID().orElseThrow(RuntimeException::new);
        User me = User.create(id);
        assertTrue(me.toString(), me instanceof LoggedUser);
        assertTrue(me.toString(), me.canEdit());

        int wrongId = id+1;
        User wrong = User.create(wrongId);
        assertFalse(wrong.toString(), wrong instanceof LoggedUser);
        assertFalse(wrong.toString(), wrong.canEdit());
    }

    @Test(timeout = 1000L)
    public void query() {
        User me = Users.me();

        assertEquals("user", me.name());
        assertEquals(2, me.getID());
        assertFalse(me.avatar().isPresent());
        assertEquals("", me.bio());
    }

    @Test(timeout = 1000L)
    public void testNonExistingUser() {
        Internal.setNoSuchEntityHandler(e -> {
            throw new RuntimeException("An exception was thrown.", e);
        });

        assertFalse(Users.get(20051561).isPresent());
    }

    @Test
    public void testWrongCredentials(){
        WildFyre.disconnect();
        try {
            WildFyre.connect("wrong", "password");
            fail();

        } catch (Request.CantConnectException e) {
            throw new RuntimeException(e);

        } catch (InvalidCredentialsException e) {
            assertTrue(true);
        }
    }
}
