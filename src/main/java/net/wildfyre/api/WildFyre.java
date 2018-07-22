package net.wildfyre.api;

import net.wildfyre.descriptors.Internal;
import net.wildfyre.descriptors.LoggedUser;
import net.wildfyre.http.Request;

/**
 * The primary means of interaction with the API.
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // it's an API, so of course some stuff is unused/can be private
public class WildFyre {

    /**
     * Connects to the server with the specified user.
     * This method is NOT executed concurrently, because no action can be taken by the client while the cache is empty
     * anyway.
     * @param username the user's username
     * @param password the user's password
     * @return Your own user. See also {@link WildFyre#getMe()}.
     */
    public static LoggedUser connect(String username, String password) throws Request.CantConnectException {
        Internal.requestToken(username, password);
        Internal.init();

        return Internal.getMe();
    }

    /**
     * Connects to the server with the specified token.
     * This method is NOT executed concurrently, because no action can be taken by the client while the cache is empty
     * anyway.
     * @param token the token
     * @return Your own user. See also {@link WildFyre#getMe()}.
     */
    public static LoggedUser connect(String token) throws Request.CantConnectException {
        if(token == null)
            throw new NullPointerException("Cannot connect to the token 'null'");

        Internal.setToken(token);
        Internal.init();

        return Internal.getMe();
    }

    /**
     * Disconnects the API from the currently-logged-in user.
     */
    public static void disconnect(){
        Internal.reset();
    }

    /**
     * Returns the logged-in user of the API, that is, the user corresponding to the saved-token.
     * @return The logged-in user.
     */
    public static LoggedUser getMe(){
        return Internal.getMe();
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
        return Internal.getToken() != null;
    }
}
