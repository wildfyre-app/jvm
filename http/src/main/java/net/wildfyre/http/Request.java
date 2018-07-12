package net.wildfyre.http;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Wrapper around the HTTP requests.
 * <p>
 * This class is NOT part of the public API.
 */
public class Request {

    //region Select the URL of the server depending on the building environment
    private static final String API_URL = "https://api.wildfyre.net";
    private static final String API_URL_TESTING = "http://localhost:8000";
    static final boolean isTesting = !System.getProperty("org.gradle.test.worker", "default").equals("default");
    static String getURL(){return isTesting ? API_URL_TESTING : API_URL;}
    //endregion
    //region Variables

    public static final String CHARSET = "UTF-8";

    //endregion
    //region Requests

    /**
     * Sends a request to the API.
     *
     * @param method  the HTTP method used by the request.
     * @param token   the token used to authenticate the user (can be {@code null} for un-authenticated requests).
     * @param address the address you want to query (eg. /account/)
     * @param params  the parameters you provide
     * @return The JSON response from the server.
     */
    protected static JsonValue request(Method method, String token, String address, JsonObject params)
        throws CantConnectException, IssueInTransferException {
        try {

            // Creation of the URL
            URL url = new URL(getURL() + address);
            byte[] postDataBytes = convertToByteArray(params);

            // Connection
            HttpURLConnection conn = connect(url);

            setRequestedMethod(conn, method);

            conn.setRequestProperty("Host", url.getHost());
            if (token != null) conn.setRequestProperty("Authorization", "token " + token);
            conn.setRequestProperty("From", "lib-java");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            write(conn, postDataBytes);

            return read(getInputStream(conn));

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The provided address " + address + " at " + getURL() + " is malformed: "
                + getURL() + address, e);
        }
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
    static HttpURLConnection connect(URL url) throws CantConnectException {
        try {
            return (HttpURLConnection) url.openConnection();

        } catch (IOException e) {
            throw new CantConnectException("Cannot connect to the server.", e);
        }
    }

    /**
     * Converts the JSON parameters to an array of bytes that can be sent in the request. The charset used is specified
     * in {@link #CHARSET} ({@value #CHARSET}).
     *
     * @param params the JSON parameters to be converted.
     * @return A byte array representing the provided parameters.
     */
    static byte[] convertToByteArray(JsonValue params) {
        try {
            return params.toString().getBytes(CHARSET);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("There was a problem with the character encoding '" + CHARSET + "'. Because it is" +
                "hard-written in the class, this error should never happen.", e);
        }
    }

    /**
     * Sets the method for the request and handles eventual exceptions.
     * @param connection the connection that should be modified
     * @param method the HTTP method used for the request
     */
    static void setRequestedMethod(HttpURLConnection connection, Method method) {
        try {
            connection.setRequestMethod(method.toString());

        } catch (ProtocolException e) {
            throw new IllegalArgumentException("Cannot set the method to " + method, e);
        }
    }

    /**
     * Writes the bytes to the connection.
     * @param connection the connection
     * @param bytes the data that should be sent
     * @throws CantConnectException if the output stream of the connection is unreachable for some reason
     */
    static void write(HttpURLConnection connection, byte[] bytes) throws CantConnectException {
        try {
            connection.getOutputStream().write(bytes);

        } catch (IOException e) {
            throw new CantConnectException("Cannot get output stream to the connection.", e);
        }
    }

    /**
     * Reads the server's response and handles eventual exceptions.
     * @return The server's response.
     * @throws IssueInTransferException If the server refuses the request, see
     * {@link IssueInTransferException#getJson() getJson()} to get the eventual error message.
     */
    static InputStream getInputStream(HttpURLConnection connection) throws IssueInTransferException {
        try {
            return connection.getInputStream();

        } catch (IOException e) {
            throw new IssueInTransferException("The server refused the request.", connection.getErrorStream());
        }
    }

    /**
     * Reads the JSON data.
     * @param input the input
     * @return The JSON data contained by the server's response
     * @throws IssueInTransferException If any I/O occurs while trying to read the JSON data.
     */
    static JsonValue read(InputStream input) throws IssueInTransferException {
        try {
            return Json.parse (
                new BufferedReader (
                    new InputStreamReader (
                        input,
                        Request.CHARSET
                    )
                )
            );

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("There was an encoding error. Since the encoding is hardcoded in Request.CHARSET," +
                "this error should never occur. Please report to the developers with the full stacktrace.", e);

        } catch (IOException e) {
            throw new IssueInTransferException("There was an I/O error while parsing the JSON data, or the server " +
                "refused the request.", e);
        }
    }

    //endregion
    //region Exceptions

    /**
     * Thrown when the request failed because the requested could not connect to the server.
     */
    public static class CantConnectException extends IOException {
        CantConnectException(String msg, Exception e) {
            super(msg, e);
        }
    }

    //endregion
}
