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
import net.wildfyre.http.Method.GET
import net.wildfyre.http.Method.POST
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException

class RequestTest {

    @Test
    fun connect() {
        try {

            val j = Request(POST, "/account/auth/")
                    .addJson(JsonObject()
                            .add("username", "libtester")
                            .add("password", "thisisnotatest"))
                    .get()

            assertNotNull(j.asObject().getString("token", null))
        } catch (e: Request.CantConnectException) {
            throw IllegalArgumentException(e)
        } catch (e: IssueInTransferException) {
            if (e.json.isPresent)
                fail("Issue in transfer: " + e.json.get().toString(PRETTY_PRINT))
            else
                fail("Unknown issue in transfer.")
        }

    }

    @Test
    fun getOwnPage() {
        try {
            val j = Request(GET, "/users/")
                    .addToken(token)
                    .get()
                    .asObject()

            assertNotEquals(-1, j.getInt("user", -1).toLong())
            assertEquals("libtester", j.getString("name", "not found"))
            assertNotNull(j.getString("avatar", null))
            assertNotNull(j.getString("bio", null))
            assertFalse(j.getBoolean("banned", true))

        } catch (e: Request.CantConnectException) {
            throw RuntimeException(e)
        } catch (e: IssueInTransferException) {
            throw RuntimeException(e)
        }

    }

    @Test
    fun testJSON() {
        var input: InputStream? = null
        try {
            input = ByteArrayInputStream("{\"data\": 2}".toByteArray(charset(Request.CHARSET)))
        } catch (e: UnsupportedEncodingException) {
            fail("Could not read Stream.")
        }

        assertNotNull(input)
        try {
            val json = Request.read(input!!)

            assertEquals(JsonObject().add("data", 2), json)
        } catch (e: IssueInTransferException) {
            fail("Could not parse to JSON.")
        }

    }

    @Test
    fun testAvailabilityOfDatabase() {
        assertNotEquals("default", System.getProperty("org.gradle.test.worker", "default"))
        assertTrue(Request.isTesting)
    }

    companion object {

        internal val token = "9d36a784f7bc641b9d0f7a000a96b6563b987956"
    }

}
