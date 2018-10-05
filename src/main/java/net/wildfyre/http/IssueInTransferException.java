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

import com.eclipsesource.json.*;
import net.wildfyre.descriptors.NoSuchEntityException;

import java.io.*;
import java.util.Optional;

/**
 * Signifies that the server refused the data. More information about the problem can be found using {@link #getJson()}.
 */
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

            Reader input = new BufferedReader(new InputStreamReader(in, Request.CHARSET));
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

    //region Details analyzer

    /**
     * Performs an action for some details of this exception.
     *
     * @param detail the message expected to be in the 'details' field of the JSON data
     * @param action what to do if the message matches
     * @throws NoSuchEntityException if the action throws a NoSuchEntityException, it is relayed to the caller
     * @see #ifDetailsAre(String, ExceptionRunnable, ExceptionRunnable) Do something on a failure
     */
    public void ifDetailsAre(String detail, ExceptionRunnable action) throws NoSuchEntityException {
        ifDetailsAre(detail, action, null);
    }

    /**
     * Performs an action for some details of this exception.
     *
     * @param detail the message expected to be in the 'details' field of the JSON data
     * @param action what to do if the message matches
     * @param otherwiseDo what to do if the message doesn't match
     * @throws NoSuchEntityException if any action throws a NoSuchEntityException, it is relayed to the caller
     * @see #ifDetailsAre(String, ExceptionRunnable) Do nothing on a failure
     */
    public void ifDetailsAre(String detail, ExceptionRunnable action, ExceptionRunnable otherwiseDo)
    throws NoSuchEntityException {
        if(json != null)
            if(json.asObject().getString("detail", null).equals(detail))
                action.run();
            else if(otherwiseDo != null)
                otherwiseDo.run();
    }

    /**
     * An action that may throw a {@link NoSuchEntityException}.
     */
    public interface ExceptionRunnable {

        /**
         * The action of this object.
         * @throws NoSuchEntityException If there's a missing entity at some point.
         */
        void run() throws NoSuchEntityException;
    }

    //endregion

    @Override
    public String getMessage(){
        return json != null ? super.getMessage() + "\n" + json.toString(WriterConfig.PRETTY_PRINT) : super.getMessage();
    }
}
