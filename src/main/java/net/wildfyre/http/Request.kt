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
import com.eclipsesource.json.ParseException
import net.wildfyre.api.Internal
import net.wildfyre.http.Request.CantConnectException
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.stream.Collectors

/**
 * Wrapper around the HTTP requests.
 *
 * This class is NOT part of the public API.
 */
class Request
/**
 * Creates a new request to the server, which will be executed on a call such as [get].
 * @param method the HTTP method required by the API
 * @param address the address you'd like to access (see the API documentation)
 * @throws CantConnectException if the connection to the server fails
 */
@Throws(CantConnectException::class)
constructor(private val method: Method, private val address: String) {

    //region Initialization & Variables

    private val requestUrl = URL(url + address)
    internal val headers = HashMap<String, String>()

    private var jsonOutput: JsonValue? = null
    private var fileOutput: File? = null
    private var fileOutputName: String? = null

    private val id = reqId++

    init {
        headers["From"] = "lib-java"
        headers["Host"] = requestUrl.host
    }

    //endregion
    //region Methods

    private fun send() : HttpURLConnection {
        val conn = connect(requestUrl)
        conn.doInput = true // We always want input

        println("\n$id:$method $requestUrl HTTP/1.1")
        method.setMethod(conn, this)

        var i = 0
        for ((header, value) in headers) {
            println("$id:${i++} - $header: $value")
            conn.setRequestProperty(header, value)
        }

        if (fileOutput != null)
            multipart(conn)
        else jsonOutput?.let {
            conn.setRequestProperty("Content-Type", DataType.JSON.toString())
            println("$id:$i - Content-Type: ${DataType.JSON}")

            conn.doOutput = true
            val bw = BufferedWriter(OutputStreamWriter(conn.outputStream, CHARSET))
            it.writeTo(bw)
            bw.close()
        }

        println("$id: Done sending.")
        return conn
    }

    /**
     * Sends a multipart request to the connection.
     *
     * Adapted from [https://stackoverflow.com/a/11826317/5666171] and
     * [https://www.codejava.net/java-se/networking/upload-files-by-sending-multipart-request-programmatically]
     */
    private fun multipart(conn: HttpURLConnection) {
        println("$id: This is a multipart request")

        val endl = "\r\n"
        val hyphens = "--"
        val boundary = "===${System.currentTimeMillis()}==="

        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

        val output = conn.outputStream
        val writer = DataOutputStream(output)

        // Ignore if jsonOutput is null
        jsonOutput?.let {
            println("$id: There is some JSON attached")

            writer.writeUTF(hyphens + boundary + endl)
            writer.writeUTF("Content-Disposition: form-data; name: \"json\"$endl")
            writer.writeUTF("Content-Type: ${DataType.JSON}; charset= $CHARSET$endl")
            writer.writeUTF(endl)

            it.writeTo(PrintWriter(OutputStreamWriter(writer, CHARSET)))

            writer.writeUTF(endl)
        }

        // Throw NullPointerException if fileOutput is null
        fileOutput!!.let { file ->
            writer.writeUTF(hyphens + boundary + endl)
            writer.writeUTF("Content-Disposition: form-data; name: \"$fileOutputName\"; filename: \"${file.name}\"$endl")
            writer.writeUTF("Content-Type: ${URLConnection.guessContentTypeFromName(file.name)}" + endl)
            writer.writeUTF(endl)

            file.inputStream().run {
                readBytes().forEach { writer.writeByte(it.toInt()) }
                close()
            }

            writer.writeUTF(endl)
            writer.writeUTF(hyphens + boundary + hyphens + endl)
        }

        writer.flush()
        writer.close()
    }

    /**
     * Makes the request authenticated by adding the token of the user.
     * @param token the token
     * @return This request itself, to allow method-chaining.
     */
    fun addToken(token: String = Internal.token()): Request {
        headers["Authorization"] = "token $token"

        return this
    }

    /**
     * Adds JSON parameters to this request.
     * @param params the parameters
     * @return This request itself, to allow method-chaining.
     */
    fun addJson(params: JsonValue): Request {
        jsonOutput = params

        return this
    }

    /**
     * Adds a file that will be uploaded along the request.
     *
     * Only one file can be added to a request.
     * @param file the file
     * @return This request itself, to allow method-chaining.
     */
    fun addFile(name: String, file: File): Request {
        fileOutput = file
        fileOutputName = name

        return this
    }

    /**
     * Accesses the JSON response from the server.
     * @return The JSON response from the server.
     * @throws IssueInTransferException if there is problem with the connection or the data
     */
    @Deprecated(
        replaceWith = ReplaceWith("getJson"),
        message = "Deprecated because of implicit data type request, use Request.getJson instead."
    )
    @Throws(IssueInTransferException::class)
    fun get() = getJson()

    /**
     * Requests a JSON response from the server, and returns it.
     * @return The JSON response from the server.
     */
    @Throws(IssueInTransferException::class)
    fun getJson(): JsonValue {
        headers["Accept"] = DataType.JSON.toString()
        return readJson(getInputStream(send()))
    }

    //endregion
    //region Helpers

    /**
     * Connects to the provided URL.
     *
     * @param url the URL the API should connect to
     * @return The connection to the server, on success.
     * @throws CantConnectException Failure to connect to the server.
     */
    @Throws(CantConnectException::class)
    internal fun connect(url: URL): HttpURLConnection {
        try {
            return url.openConnection() as HttpURLConnection

        } catch (e: IOException) {
            throw CantConnectException("Cannot connect to the server.", e)
        }
    }

    /**
     * Converts the JSON parameters to an array of bytes that can be sent in the request. The charset used is
     * specified in [CHARSET].
     *
     * @param params the JSON parameters to be converted.
     * @return A byte array representing the provided parameters.
     */
    internal fun convertToByteArray(params: JsonValue): ByteArray {
        try {
            return params.toString().toByteArray(charset(CHARSET))

        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("There was a problem with the character encoding '" + CHARSET + "'." +
                "Because it is hard-written in the class, this error should never happen.", e)
        }
    }

    /**
     * Reads the server's response and handles eventual exceptions.
     * @return The server's response.
     * @throws IssueInTransferException If the server refuses the request, see
     * [getJson()][IssueInTransferException.getJson] to get the eventual error message.
     */
    @Throws(IssueInTransferException::class)
    internal fun getInputStream(connection: HttpURLConnection): InputStream {
        try {
            println("$id: Beginning reception... HTTP Status: ${connection.responseCode}")
            return connection.inputStream

        } catch (e: IOException) {
            throw IssueInTransferException("$id: The server refused the request.", connection.errorStream)
        }
    }

    //endregion

    /**
     * Thrown when the request failed because the requested could not connect to the server.
     * @param msg The message to be displayed
     * @param e The more detailed exception
     */
    class CantConnectException internal constructor(msg: String, e: Exception) : IOException(msg, e)

    companion object {

        //region Select the URL of the server depending on the building environment
        private const val API_URL = "https://api.wildfyre.net"
        private const val API_URL_TESTING = "http://localhost:8000"
        val isTesting = System.getProperty("org.gradle.test.worker", "default") != "default"
        internal val url: String
            get() = if (isTesting) API_URL_TESTING else API_URL
        //endregion

        /**
         * The charset that is used to readJson/write data to the server.
         */
        const val CHARSET = "UTF-8"

        //region Helpers

        private var reqId = 0

        /**
         * Reads the JSON data.
         * @param input the input
         * @return The JSON data contained by the server's response
         * @throws IssueInTransferException If any I/O occurs while trying to read the JSON data.
         */
        @Throws(IssueInTransferException::class)
        fun readJson(input: InputStream): JsonValue {
            try {
                return Json.parse(
                    InputStreamReader(
                        input,
                        Request.CHARSET
                    )
                )

            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException("There was an encoding error. Since the encoding is hardcoded in " +
                    "Request.CHARSET, this error should never occur. Please report to the developers with the full " +
                    "stacktrace.", e)

            } catch (e: IOException) {
                throw IssueInTransferException("There was an I/O error while parsing the JSON data, or the server " +
                    "refused the request.", e)

            } catch (e: ParseException) {
                try {
                    val content = BufferedReader(InputStreamReader(input, CHARSET))
                        .lines()
                        .collect(Collectors.joining("\n"))
                    throw RuntimeException("The content of the InputStream was not a JSON object:\n"
                        + content + "\nSize: " + content.length, e)

                } catch (e1: UnsupportedEncodingException) {
                    throw RuntimeException("This should never happen: Request.CHARSET is wrong.", e1)
                }
            }
        }
    }

}
