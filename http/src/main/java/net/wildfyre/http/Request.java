package net.wildfyre.http;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.stream.Collectors;

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
    //region Object

    final HttpURLConnection conn;

    /**
     * Creates a new request to the server, and starts to execute it.
     * @param method the HTTP method required by the API
     * @param address the address you'd like to access (see the API documentation)
     * @throws CantConnectException if the connection to the server fails
     */
    public Request(Method method, String address) throws CantConnectException {
        try {
            URL url = new URL(getURL() + address);

            conn = connect(url);

            setRequestedMethod(conn, method);

            conn.setRequestProperty("From", "lib-java");
            conn.setRequestProperty("Host", url.getHost());

            // Default value, might be overridden later
            conn.setRequestProperty("Accept", DataType.JSON.toString());

        } catch (MalformedURLException e) {
            throw new CantConnectException("The provided address " + address + " at " + getURL() + " is malformed: "
                + getURL() + address, e);
        }
    }

    /**
     * Makes the request authenticated by adding the token of the user.
     * @param token the token
     * @return This request itself, to allow method-chaining.
     */
    public Request addToken(String token) {
        conn.setRequestProperty("Authorization", "token " + token);

        return this;
    }

    /**
     * Adds JSON parameters to this request.
     * @param params the parameters
     * @return This request itself, to allow method-chaining.
     * @throws CantConnectException if the lib cannot connect to the server
     */
    public Request addJson(JsonValue params) throws CantConnectException {
        try {
            conn.setRequestProperty("Content-Type", DataType.JSON.toString());
            conn.setDoOutput(true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            params.writeTo(bw);
            bw.close();

            return this;

        } catch (IOException e) {
            throw new CantConnectException("Cannot get output stream to the connection.", e);
        }
    }

    /**
     * Accesses the JSON response from the server.
     * @return The JSON response from the server.
     * @throws IssueInTransferException if there is problem with the connection or the data
     */
    public JsonValue get() throws IssueInTransferException {
        return read(getInputStream(conn));
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
            connection.setRequestMethod(method.name());

        } catch (ProtocolException e) {
            throw new IllegalArgumentException("Cannot set the method to " + method, e);
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

        } catch (ParseException e) {
            String content = new BufferedReader(new InputStreamReader(input)).lines().collect(Collectors.joining("\n"));
            throw new RuntimeException("The content of the InputStream was not a JSON object:\n"
                + content + "\nSize: " + content.length(), e);
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
