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

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import net.wildfyre.areas.Areas;
import net.wildfyre.descriptors.Descriptor;
import net.wildfyre.descriptors.NoSuchEntityException;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;
import net.wildfyre.users.Users;
import net.wildfyre.utils.InvalidCredentialsException;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static net.wildfyre.http.Method.POST;

/**
 * This class represents the internals of the WildFyre Java Client Library. This class was written to give lower-level
 * access to developers. When possible, prefer using the WildFyre class.
 * <p>Note that you should call {@link #init()} after any modification of the token, otherwise the behavior of this
 * class is not defined.</p>
 */
public class Internal {

    private static String token;

    //region Cache content

    /**
     * Fully clears the cache, but keeps the token.
     * @see #clean() Only remove the data that has expired
     */
    public static void clear(){
        Users.clear();
        Areas.INSTANCE.clear();
    }

    /**
     * Cleans the cache -- that is, removes every object that is not valid anymore from it (see
     * {@link Descriptor#isValid()}).
     * @see #clear() Remove all data, not only the expired data
     */
    public static void clean(){
        Users.clean();
        Areas.INSTANCE.clean();
    }

    /**
     * Resets the API.
     * <p>This means that the user's token is removed, and the cache is fully cleared.</p>
     */
    public static void reset(){
        clear();
        token = null;
        Users.reset();
        // No need to reset Areas, as they are already cleared by the Internal#clear() call above.
    }

    //endregion
    //region Getters

    /**
     * Returns the token used for authentication.
     * @return The token.
     */
    public static String token(){
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

    //region NoSuchEntityException

    private static Consumer<NoSuchEntityException> noSuchEntityHandler;

    static void throwNoSuchEntity(NoSuchEntityException e){
        if(noSuchEntityHandler != null)
            noSuchEntityHandler.accept(e);
        else throw new RuntimeException("Warning: no handler was specified for NoSuchEntityException, but one was" +
            "needed. See Internal.setNoSuchEntityHandler().", e);
    }

    /**
     * Registers a handler for the case where an Entity that was queried for update does not exist.
     * @param consumer the handler.
     */
    public static void setNoSuchEntityHandler(Consumer<NoSuchEntityException> consumer){
        if(noSuchEntityHandler != null) {
            System.err.println("Warning: Internal.setNoSuchEntityHandler() was called a second time. The elder consumer"
                + " will be kept.");
            return;
        }

        if(consumer == null)
            throw new NullPointerException("The consumer cannot be null.");

        noSuchEntityHandler = consumer;
    }

    //endregion
    //region CantConnectException

    private static Consumer<Request.CantConnectException> cantConnectHandler;

    /**
     * Call this method if you need to tell the client of this API that the server is unreachable at the moment, if you
     * are in a concurrent context.
     * @param e the exception that was thrown during the request.
     */
    public static void throwCantConnect(Request.CantConnectException e){
        if(cantConnectHandler != null)
            cantConnectHandler.accept(e);
        else throw new RuntimeException("Warning: no handler was specified for CantConnectException, but one was" +
            "needed. See Internal.setCantConnectHandler().", e);
    }

    /**
     * Registers a handler for the case where the API cannot connect to the server.
     * @param consumer the handler.
     */
    public static void setCantConnectHandler(Consumer<Request.CantConnectException> consumer){
        if(cantConnectHandler != null) {
            System.err.println("Warning: Internal.setCantConnectHandler() was called a second time. The elder consumer"
                + " will be kept.");
            return;
        }

        if(consumer == null)
            throw new NullPointerException("The consumer cannot be null.");

        cantConnectHandler = consumer;
    }

    //endregion
    //region Submit

    /**
     * Submits a new task to be executed concurrently.
     * @param task the task to be executed concurrently.
     */
    public static void submit(Runnable task){
        executor.submit(task);
    }

    /**
     * Submits a new task to be executed concurrently, that updates a Descriptor.
     * @param descriptor the descriptor to be updated concurrently.
     */
    public static <D extends Descriptor> void submitUpdate(D descriptor) {
        executor.submit(() -> {
            try {
                descriptor.update();
            } catch (NoSuchEntityException e) {
                throwNoSuchEntity(e);
            } catch (Request.CantConnectException e) {
                throwCantConnect(e);
            }
        });
    }

    //endregion
    //endregion
    //region Authentication

    /**
     * Requests a token using a username and a password.
     * This request is NOT executed concurrently (since the client cannot do anything until connected).
     * @param username the user's username
     * @param password the user's password
     * @throws Request.CantConnectException if the API cannot connect to the server
     */
    public static void requestToken(String username, String password)
    throws Request.CantConnectException, InvalidCredentialsException {
        try {
            JsonObject json = new Request(POST, "/account/auth/")
                .addJson(new JsonObject()
                    .add("username", username)
                    .add("password", password))
                .getJson()
                .asObject();

            token = json.getString("token", null);
            if(token == null)
                throw new RuntimeException("Could not find the token in the request body!\n"
                    + json.toString(WriterConfig.PRETTY_PRINT));

            Internal.clear();

        } catch (IssueInTransferException e) {
            JsonValue j = e.getJson();
            if(j != null) {
                if (j.asObject()
                    .get("non_field_errors")
                    .asArray()
                    .get(0)
                    .asString()
                    .equals("Unable to log in with provided credentials."))
                    throw new InvalidCredentialsException("Unable to log in with provided credentials.", e);
            }
        }
    }

    /**
     * Sets the token used by the API. Note that no verification regarding the token's validity is done by this method,
     * use with {@link #init()} for that purpose.
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
        Internal.clear();
        Users.init();
        Areas.INSTANCE.init();
    }

    //endregion

}
