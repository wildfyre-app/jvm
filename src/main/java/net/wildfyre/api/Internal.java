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

package net.wildfyre.api;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import net.wildfyre.descriptors.Descriptor;
import net.wildfyre.users.LoggedUser;
import net.wildfyre.users.User;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static net.wildfyre.http.Method.GET;
import static net.wildfyre.http.Method.POST;

/**
 * This class represents the internals of the WildFyre Java Client Library. This class was written to give lower-level
 * access to developers. When possible, prefer using the WildFyre class.
 * <p>Note that you should call {@link #init()} after any modification of the token, otherwise the behavior of this
 * class is not defined.</p>
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // it's an API, so of course some stuff is unused/can be private
public class Internal {

    private static String token;
    private static int userId = -1;

    //region Cache content
    private static Map<Integer, User> users = new HashMap<>();

    /**
     * Fully clears the cache, but keeps the token.
     * @see #clean() Only remove the data that has expired
     */
    public static void clear(){
        users.clear();
    }

    /**
     * Cleans the cache -- that is, removes every object that is not valid anymore from it (see
     * {@link Descriptor#isValid()}).
     * @see #clear() Remove all data, not only the expired data
     */
    public static void clean(){
        long currentTime = System.currentTimeMillis();

        users.values().removeIf(u -> !u.isValid(currentTime));
    }

    /**
     * Resets the API.
     * <p>This means that the user's token is removed, and the cache is fully cleared.</p>
     */
    public static void reset(){
        clear();
        token = null;
        userId = -1;
    }

    //endregion
    //region Getters

    /**
     * Gets the <u>cached version</u> of a user. Note that this will NEVER query the server, no matter what happens. You
     * should probably user {@link User#query(int)} instead.
     * @param id the user's ID
     * @return The user found for the specified ID in the cache, or the default value.
     */
    public static Optional<User> getCachedUser(int id){
        return Optional.ofNullable(users.get(id));
    }

    /**
     * Gets the current user, that is, the user that corresponds to the saved token.
     * @return The current user.
     */
    public static LoggedUser getMe(){
        return User.query(getMyId()).asLogged();
    }

    /**
     * Checks whether a user ID is the ID of the current user.
     * @return {@code true} if the provided ID is the ID of the logged-in user.
     */
    public static boolean isMyId(int id){
        if(userId == -1)
            throw new IllegalStateException("Cannot call this method without specifying an ID. Call Internal#init.");

        return userId == id;
    }

    /**
     * The ID of the logged-in user.
     * @return The ID of the logged-in user.
     */
    public static int getMyId(){
        if(userId == -1)
            throw new IllegalStateException("Cannot call this method without specifying an ID. Call Internal#init.");

        return userId;
    }

    /**
     * Returns the token used for authentication.
     * @return The token.
     */
    public static String getToken(){
        return token;
    }

    //endregion
    //region Concurrent execution

    /**
     * Contains the ThreadPool that will execute any refresh request (when applicable).
     * <p>
     * The executor is configured with {@link Executors#newCachedThreadPool()}, which means it'll spawn new threads
     * as needed.
     */
    static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    /**
     * Submits a new task to be executed concurrently.
     * @param task the task to be executed concurrently.
     */
    public static void submit(Runnable task){
        executor.submit(task);
    }

    //endregion
    //region Authentication

    /**
     * Requests a token using a username and a password.
     * This request is NOT executed concurrently (since the client cannot do anything until connected).
     * @param username the user's username
     * @param password the user's password
     * @throws Request.CantConnectException if the API cannot connect to the server
     */
    public static void requestToken(String username, String password) throws Request.CantConnectException {
//TODO: Throw an exception 'InvalidCredentialsException extends IllegalArgumentException' if the password is incorrect

        try {
            JsonObject json = new Request(POST, "/account/auth/")
                .addJson(new JsonObject()
                    .add("username", username)
                    .add("password", password))
                .get()
                .asObject();

            token = json.getString("token", null);
            if(token == null)
                throw new RuntimeException("Could not find the token in the request body!\n"
                    + json.toString(WriterConfig.PRETTY_PRINT));

            Internal.clear();

        } catch (IssueInTransferException e) {
            throw new IllegalArgumentException("Cannot login to the desired user.", e);
        }
    }

    /**
     * Sets the token used by the API. Note that no verification regarding the token's validity is done by this method.
     * @param token the new token
     */
    public static void setToken(String token) {
        if(token == null)
            throw new NullPointerException("The token should not be 'null'.");
        else if(token.isEmpty())
            throw new IllegalArgumentException("The token should not be empty, size:" + token.length());

        Internal.token = token;
        Internal.clear(); // Must clear the cache if any modification to the token is done
    }

    /**
     * Connects to the server & tries to access the logged-user's ID. This is needed so the API can identify whether a
     * post is owned or not by the user.
     * @throws Request.CantConnectException if the API cannot connect to the server
     */
    public static void init() throws Request.CantConnectException {
        try {
            JsonObject json = new Request(GET, "/users/")
                .addToken(token)
                .get()
                .asObject();

            userId = json.getInt("user", -1);
            if(userId == -1)
                throw new RuntimeException("Couldn't find the ID of the logged-in user!\n"
                    + json.toString(WriterConfig.PRETTY_PRINT));

            Internal.clear();

        } catch (IssueInTransferException e) {
            throw new IllegalStateException("Server denied the request.", e);
        }
    }

    //endregion

}
