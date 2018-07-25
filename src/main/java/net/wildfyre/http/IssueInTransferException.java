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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.*;
import java.util.Optional;

/**
 * Signifies that the server refused the data. More information about the problem can be found using {@link #getJson()}.
 */
@SuppressWarnings("WeakerAccess") // Part of the API
public class IssueInTransferException extends IOException {
    private JsonValue json;

    /**
     * Creates a new IssueInTransferException.
     * @param msg the message
     * @param in the response from the server, that will be parsed as JSON if possible
     */
    public IssueInTransferException(String msg, InputStream in) {
        super(msg);

        String out = "";
        try {

            Reader input = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder output = new StringBuilder();

            for (int c; (c = input.read()) >= 0; ) {
                output.append((char) c);
            }
            out = output.toString();

            if (out.length() != 0)
                this.json = Json.parse(out);

        } catch (IOException | NullPointerException e) { //  No data found.
        } catch (ParseException e){
            this.json = new JsonObject()
                .add("Server", out);
        }
    }

    /**
     * Creates a new IssueInTransferException.
     * @param msg the message
     * @param cause what caused the issue
     */
    public IssueInTransferException(String msg, Exception cause) {
        super(msg, cause);

        this.json = null;
    }

    /**
     * Get the JSON sent by the server (if any).
     * @return the JSON sent by the server, or an empty Optional if none was sent.
     */
    public Optional<JsonValue> getJson(){
        return Optional.ofNullable(json);
    }
}
