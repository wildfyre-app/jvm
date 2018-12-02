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

package net.wildfyre.http

import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.WriterConfig.PRETTY_PRINT
import net.wildfyre.api.WildFyre
import net.wildfyre.http.Method.GET
import net.wildfyre.http.Method.POST
import net.wildfyre.users.LoggedUser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class RequestTest {

    @Test
    fun connect() {
        try {
            val j = Request(POST, "/account/auth/")
                .addJson(JsonObject()
                    .add("username", "user")
                    .add("password", "password123"))
                .getJson()

            assertNotNull(j.asObject().getString("token", null))
        } catch (e: IssueInTransferException) {
            if (e.json.isPresent)
                fail("Issue in transfer: " + e.json.get().toString(PRETTY_PRINT))
            else
                fail("Unknown issue in transfer.")
        }
    }

    @Test
    fun getOwnPage() {
        val j = Request(GET, "/users/")
            .addToken(token)
            .getJson()
            .asObject()

        assertNotEquals(-1, j.getInt("user", -1).toLong())
        assertEquals("user", j.getString("name", "not found"))
        assertNotNull(j.getString("bio", null))
        assertFalse(j.getBoolean("banned", true))
    }

    @Test
    fun testJSON() {
        val input = ByteArrayInputStream("{\"data\": 2}".toByteArray(charset(Request.CHARSET)))

        val json = Request.readJson(input)
        assertEquals(JsonObject().add("data", 2), json)
    }

    @Test
    fun testAvailabilityOfDatabase() {
        assertNotEquals("default", System.getProperty("org.gradle.test.worker", "default"))
        assertTrue(Request.isTesting)
    }

    companion object {
        internal val token: String

        init {
            token = Request(POST, "/account/auth/")
                .addJson(JsonObject()
                    .add("username", "user")
                    .add("password", "password123"))
                .getJson().asObject()
                .getString("token", null)
        }

        /**
         * Connects to the test database. If the database is not running on this device, this will obviously fail.
         * @return The logged user.
         */
        fun connectToTestDB(): LoggedUser {
            return WildFyre.connect(token)
        }
    }

}
