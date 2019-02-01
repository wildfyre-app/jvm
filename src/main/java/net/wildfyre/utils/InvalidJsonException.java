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

package net.wildfyre.utils;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import java.util.Optional;

/**
 * This exception represents a problem that occurred because the JSON data provided contained an error.
 */
public class InvalidJsonException extends Exception {

    //region Constructors

    private InvalidJsonException(String message){
        super(message);
    }

    //endregion
    //region Helpers & Generators

    /**
     * Creates and returns an exception symbolizing that a required field was missing in some JSON data.
     * @param field the missing field
     * @param json the JSON data that did not contain the field
     * @return The exception created by this class.
     */
    public static InvalidJsonException missingField(String field, JsonValue json){
        return new InvalidJsonException("Missing field: '"+field+"' in JSON.\n"
            + json.toString(WriterConfig.PRETTY_PRINT));
    }

    /**
     * Requires the existence of a field in a JSON object.
     * @param json the JSON object
     * @param name the name of the field
     * @return The value found in that field (see {@link JsonObject#get(String)}).
     * @throws InvalidJsonException If the field is not found.
     */
    public static JsonValue requireField(JsonObject json, String name) throws InvalidJsonException {
        JsonValue ret = json.get(name);

        if(ret != null && !ret.isNull())
            return ret;
        else throw missingField(name, json);
    }

    /**
     * Tries to access a field of a JSON object.
     * @param json the JSON object
     * @param name the name of the field
     * @return The value found in that field, or an empty Optional.
     */
    public static Optional<JsonValue> optionalField(JsonObject json, String name) {
        JsonValue ret = json.get(name);

        if(ret != null && !ret.isNull())
            return Optional.of(ret);
        else return Optional.empty();
    }

    //endregion

}
