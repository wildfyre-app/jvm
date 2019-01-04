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

import java.net.HttpURLConnection
import java.net.ProtocolException

/**
 * The HTTP method used to connect to the server.
 *
 * This class is NOT part of the public API.
 */
enum class Method {
    /** Change parts of the resource.  */
    PATCH {
        override fun setMethod(conn: HttpURLConnection, req: Request) {
            PUT.setMethod(conn, req)

            req.headers["X-HTTP-Method-Override"] = name
        }
    },

    /** Get a resource. */
    GET,

    /** Put a new resource. */
    PUT,

    /** Get the list of options from the request. */
    OPTIONS,

    /** Delete a resource. */
    DELETE,

    /** Send a new resource. */
    POST;

    override fun toString(): String = name

    /**
     * Sets the method of a [HttpURLConnection] to one of the methods supported by this enum.
     */
    open fun setMethod(conn: HttpURLConnection, req: Request){
        try {
            conn.requestMethod = this.name

        } catch (e: ProtocolException) {
            throw IllegalArgumentException("Cannot set the method to $name", e)
        }
    }
}
