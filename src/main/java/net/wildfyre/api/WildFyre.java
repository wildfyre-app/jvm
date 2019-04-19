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

package net.wildfyre.api;

import net.wildfyre.http.Request;
import net.wildfyre.users.LoggedUser;
import net.wildfyre.users.Users;
import net.wildfyre.utils.InvalidCredentialsException;

/**
 * The primary means of interaction with the API.
 */
public class WildFyre {

    /**
     * Connects to the server with the specified user.
     * This method is NOT executed concurrently, because no action can be taken by the client while the cache is empty
     * anyway.
     * @param username the user's username
     * @param password the user's password
     * @return Your own user. See also {@link Users#me()}.
     */
    public static LoggedUser connect(String username, String password)
    throws Request.CantConnectException, InvalidCredentialsException {
        Internal.requestToken(username, password);
        Internal.init();

        return Users.me();
    }

    /**
     * Connects to the server with the specified token.
     * This method is NOT executed concurrently, because no action can be taken by the client while the cache is empty
     * anyway.
     * @param token the token
     * @return Your own user. See also {@link Users#me()}.
     */
    public static LoggedUser connect(String token) throws Request.CantConnectException {
        if(token == null)
            throw new NullPointerException("Cannot connect to the token 'null'");

        Internal.setToken(token);
        Internal.init();

        return Users.me();
    }

    /**
     * Disconnects the API from the currently-logged-in user.
     */
    public static void disconnect(){
        Internal.reset();
    }

    /**
     * Is the user connected?
     *
     * <p>Note that this method does not specify whether the connection is <i>still</i> valid, it only specifies whether
     * either methods {@link #connect(String) connect(token)} or {@link #connect(String, String) connect(username,
     * password)} where successful.</p>
     * @return {@code true} if the user is registered.
     */
    public static boolean isConnected(){
        return Internal.token() != null;
    }
}
