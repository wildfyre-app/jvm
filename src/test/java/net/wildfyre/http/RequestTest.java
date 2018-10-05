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

package net.wildfyre.http;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.wildfyre.api.WildFyre;
import net.wildfyre.users.LoggedUser;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static com.eclipsesource.json.WriterConfig.PRETTY_PRINT;
import static net.wildfyre.http.Method.GET;
import static net.wildfyre.http.Method.POST;
import static org.junit.Assert.*;

public class RequestTest {

    public static final String token;

    static {
        try {
            token = new Request(POST, "/account/auth/")
                .addJson(new JsonObject()
                    .add("username", "user")
                    .add("password", "password123"))
                .get().asObject()
                .getString("token", null);

        } catch (IssueInTransferException | Request.CantConnectException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Connects to the test database. If the database is not running on this device, this will obviously fail.
     * @return The logged user.
     * @throws Request.CantConnectException if the API cannot connect to the server
     */
    public static LoggedUser connectToTestDB() throws Request.CantConnectException {
        return WildFyre.connect(token);
    }

    @Test
    public void connect(){
        try {

            JsonValue j = new Request(POST, "/account/auth/")
                .addJson(new JsonObject()
                    .add("username", "user")
                    .add("password", "password123"))
                .get();

            assertNotNull(j.asObject().getString("token", null));

        } catch (Request.CantConnectException e) {
            throw new IllegalArgumentException(e);

        } catch (IssueInTransferException e) {
            if(e.getJson().isPresent())
                fail("Issue in transfer: " + e.getJson().get().toString(PRETTY_PRINT));
            else
                fail("Unknown issue in transfer.");
        }
    }

    @Test
    public void getOwnPage(){
        try {
            JsonObject j = new Request(GET, "/users/")
                .addToken(token)
                .get()
                .asObject();

            assertNotEquals(-1, j.getInt("user", -1));
            assertEquals("user", j.getString("name", "not found"));
            //assertNotNull(j.getString("avatar", null)); TODO: Fix in T235
            assertNotNull(j.getString("bio", null));
            assertFalse(j.getBoolean("banned", true));

        } catch (Request.CantConnectException | IssueInTransferException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testJSON() {
        InputStream input = null;
        try {
            input = new ByteArrayInputStream("{\"data\": 2}".getBytes(Request.CHARSET));
        } catch (UnsupportedEncodingException e) {
            fail("Could not read Stream.");
        }

        assertNotNull(input);
        try {
            JsonValue json = Request.read(input);

            assertEquals(new JsonObject().add("data", 2), json);
        } catch (IssueInTransferException e) {
            fail("Could not parse to JSON.");
        }
    }

    @Test
    public void testAvailabilityOfDatabase(){
        assertNotEquals("default", System.getProperty("org.gradle.test.worker", "default"));
        assertTrue(Request.isTesting);
    }

}
