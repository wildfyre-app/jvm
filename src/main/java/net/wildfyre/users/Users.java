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

package net.wildfyre.users;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import net.wildfyre.api.Internal;
import net.wildfyre.descriptors.CacheManager;
import net.wildfyre.descriptors.NoSuchEntityException;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;
import net.wildfyre.utils.LazyMap;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static net.wildfyre.http.Method.GET;

public class Users {

    //region Attributes

    static Map<Integer, User> users = Collections.emptyMap(); // package instead of private, to enable access from User
    private static int userId;

    //endregion
    //region Private Constructor

    private Users(){} // private constructor so nobody calls it

    //endregion
    //region Queries

    /**
     * Retrieves a user from the database.
     *
     * <p>If the user is in the cache, it is returned. If the user is not in the cache, this method stalls and queries
     * the server. If the user is in the cache but has expired, it is returned and a concurrent update job starts.</p>
     *
     * <p>Note that this API is written so that IDs are not needed for the typical user.</p>
     *
     * <p>This method intentionally doesn't throw any exceptions, because it is designed to be used in a concurrent
     * fashion. If any occurs, they are sent to the exceptions handlers, see
     * {@link Internal#setCantConnectHandler(Consumer)}.</p>
     *
     * @param id the ID of the user.
     * @return The user, if any was found.
     * @see #getCached(int) Same, but without querying the server
     */
    public static Optional<User> get(int id){
        User user = Users
            .getCached(id)
            .orElseGet(() -> User.create(id));

        // There is no user in the cache, stall & query server
        try {
            if(user.isNew()) {
                users.put(id, user);
                user.update(); // in this thread
            }

        } catch (NoSuchEntityException e) {
            return Optional.empty(); // there is no such user server-side

        } catch (Request.CantConnectException e) {
            Internal.throwCantConnect(e);
            return Optional.empty();
        }

        // There is a user in the cache, but it's expired
        if(!user.isValid())
            Internal.submitUpdate(user); // in a new thread

        user.use();

        return Optional.of(user);
    }

    /**
     * Retrieves the user corresponding to a provided ID <b>from the cache</b>. This method is not designed to be used
     * often, as it is only of any use in the rare case where the lib needs to access the cache itself. Therefore, it
     * has a few shortcomings, listed below. If this does not exactly fit what you're searching for, you should see
     * {@link #get(int) this method}.
     *
     * <p>This method will not query the server in any way. It only returns the contents of the cache for that specific
     * ID.</p>
     *
     * <p>As a side effect, this method will not update the user either. This means that the User's inherent timer
     * (see the {@link net.wildfyre.descriptors.Descriptor Descriptor} class for more information) will not be reset, as
     * this does not count as a usage of the User.</p>
     *
     * @param id the ID of the user.
     * @return The user, or an empty optional if it is not found in the cache.
     * @see #get(int) Same, but queries the server is the user is not found in the cache.
     */
    public static Optional<User> getCached(int id){
        return Optional.ofNullable(users.get(id));
    }

    /**
     * The user that is connected to the API.
     * @return The user that is connected to the API.
     */
    public static LoggedUser me(){
        return get(userId)
            .orElseThrow(RuntimeException::new) // It is not possible that the internal ID used to connect to the server
            .asLogged();                        // does not correspond to a User.
    }

    /**
     * The full User cache is cleared, no User is kept.
     */
    public static void clear(){
        users.clear();
    }

    /**
     * Cleans the internal cache, by removing the users that have expired.
     */
    public static void clean(){
        long time = System.currentTimeMillis(); // calling curentTimeMillis once, instead of calling it for every User.

        users.values().removeIf(u -> !u.isValid(time));
    }

    /**
     * Resets the stored data of this class, that is dependent on the logged-in user. It is necessary to call this
     * method if you'd like to disconnect and reconnect as an other user.
     */
    public static void reset(){
        userId = -1;
        users.clear();
    }

    /**
     * Prepares this class to serve Users. This method must be called after the token is set, but before any work is
     * done on Areas or Posts. This method must be called again if you modify the token.
     * @throws Request.CantConnectException if no connection to the server can be established.
     */
    public static void init() throws Request.CantConnectException {
        try {
            JsonObject json = new Request(GET, "/users/")
                .addToken(Internal.token())
                .getJson()
                .asObject();

            userId = json.getInt("user", -1);
            if(userId == -1)
                throw new RuntimeException("Couldn't find the ID of the logged-in user!\n"
                    + json.toString(WriterConfig.PRETTY_PRINT));

            users = new LazyMap<>();

        } catch (IssueInTransferException e) {
            throw new RuntimeException("Couldn't find the ID of the logged-in user.", e);
        }
    }

    //endregion
    //region Getters

    public static boolean isMyID(int id){
        if(userId == -1)
            System.err.println("Warning: calling Users#isMyID without initializing. Call Users#init or Internal#init.");

        return userId == id;
    }

    public static OptionalInt myID(){
        return userId != -1 ? OptionalInt.of(userId) : OptionalInt.empty();
    }

    //endregion
    //region Expiration

    private final static CacheManager manager = new CacheManager().setExpirationTime(1000 * 60 * 30); // 30 minutes

    /**
     * The Cache Manager that handles Users.
     * @return The User Cache Manager.
     */
    public static CacheManager cacheManager(){
        return manager;
    }

    //endregion

}
