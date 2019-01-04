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

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue
import com.eclipsesource.json.WriterConfig
import net.wildfyre.descriptors.NoSuchEntityException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Signifies that the server refused the data. More information about the problem can be found using [json].
 */
class IssueInTransferException : IOException {
    /**
     * The JSON data sent by the server (if any).
     */
    var json: JsonValue? = null
        private set

    /**
     * Creates a new IssueInTransferException.
     * @param msg the message
     * @param input the response from the server, that will be parsed as JSON if possible
     */
    constructor(msg: String, input: InputStream) : super(msg) {
        json = Json.parse(InputStreamReader(input, Request.CHARSET))
    }

    /**
     * Creates a new IssueInTransferException.
     * @param msg the message
     * @param cause what caused the issue
     */
    constructor(msg: String, cause: Exception) : super(msg, cause) {
        json = null
    }

    /**
     * Performs an action for some details of this exception.
     *
     * @param detail the message expected to be in the 'details' field of the JSON data
     * @param action what to do if the message matches
     * @param otherwiseDo what to do if the message doesn't match
     * @throws NoSuchEntityException if any action throws a NoSuchEntityException, it is relayed to the caller
     * @see ifDetailsAre
     */
    @Throws(NoSuchEntityException::class)
    @JvmOverloads
    fun ifDetailsAre(detail: String, action: ExceptionRunnable, otherwiseDo: ExceptionRunnable? = null) {
        if (json != null)
            if (json!!.asObject().getString("detail", null) == detail)
                action.run()
            else otherwiseDo?.run()
    }

    /**
     * An action that may throw a [NoSuchEntityException].
     */
    interface ExceptionRunnable {

        /**
         * The action of this object.
         * @throws NoSuchEntityException If there's a missing entity at some point.
         */
        @Throws(NoSuchEntityException::class)
        fun run()
    }

    override val message: String?
        get() = if (json != null)
            "${super.message}\n${json!!.toString(WriterConfig.PRETTY_PRINT)}"
        else
            super.message
}
