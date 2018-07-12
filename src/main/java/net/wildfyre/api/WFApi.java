package net.wildfyre.api;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Method;
import net.wildfyre.http.Request;

/**
 * The primary means of interaction with the API.
 */
public class WFApi {

    private static String token;

    /**
     * Connects to the server with the specified user.
     * @param username the user's username
     * @param password the user's password
     */
    public static void connect(String username, String password) throws Request.CantConnectException {
        try {
            Request.request (
                Method.POST,
                null,
                "/account/auth/",
                new JsonObject()
                    .add("username", username)
                    .add("password", password)
            );
        } catch (IssueInTransferException e) {
            throw new IllegalArgumentException("Cannot login to the desired user.", e);
        }

        // TODO: Return the LoggedUser, see T228
    }

    /**
     * Connects to the server with the specified token.
     * @param token the token
     */
    public static void connect(String token){
        if(token == null)
            throw new NullPointerException("Cannot connect to the token 'null'");
        WFApi.token = token;

        throw new UnsupportedOperationException();
        // TODO: Return the LoggedUser, see T228
    }

    /**
     * Disconnects the API from the currently-logged-in user.
     */
    public static void disconnect(){
        token = null;
        // TODO: Clear the cache
    }

    /**
     * Is the user connected?
     *
     * <p>Note that this method does not specify whether the connection is <i>still</i> valid, it only specifies whether
     * either methods {@link #connect(String) connect(token)} or {@link #connect(String, String) connect(username,
     * password)} where successful.</p>
     *
     * @return {@code true} if the user is registered.
     */
    public static boolean isConnected(){
        return token != null;
    }

    /**
     * Wrapper around {@link Request#request(Method, String, String, JsonObject)}.
     * @param method the HTTP method
     * @param address the address you're requesting
     * @param params the JSON parameters that are specified
     * @return The reply from the server.
     * @throws Request.CantConnectException if the connection cannot be established
     * @throws IssueInTransferException if the server doesn't accept the connection (wrong credentials, wrong method...)
     * @see Request#request(Method, String, String, JsonObject) The original method
     */
    private static JsonValue authRequest(Method method, String address, JsonObject params)
        throws Request.CantConnectException, IssueInTransferException {
        return Request.request(method, token, address, params);
    }
}
