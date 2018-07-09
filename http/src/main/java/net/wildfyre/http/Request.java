package net.wildfyre.http;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Wrapper around the HTTP requests.
 *
 * This class is NOT part of the public API.
 */
public class Request {

    //region Select the URL of the server depending on the building environment
	private static final String API_URL = "https://api.wildfyre.net";
	private static final String API_URL_TESTING = "http://localhost:8000";
	static final boolean isTesting = !System.getProperty("org.gradle.test.worker", "default").equals("default");
	static String getURL(){return isTesting ? API_URL_TESTING : API_URL;}
    //endregion

	public static final String CHARSET = "UTF-8";

	/**
	 * Sends a request to the API.
	 * @param method the HTTP method used by the request.
	 * @param token the token used to authenticate the user (can be {@code null} for un-authenticated requests).
	 * @param address the address you want to query (eg. /account/)
	 * @param params the parameters you provide
	 * @return The JSON response from the server.
	 */
	protected static JsonValue request(Method method, String token, String address, JsonObject params)
		throws CantConnectException, IssueInTransferException {
		try {

			// Creation of the URL
			URL url = new URL(getURL() + address);
			byte[] postDataBytes = convertToByteArray(params);

			// Connection
			Connection conn = connect(url);

			conn.setRequestMethod(method);

			conn.connection.setRequestProperty("Host", url.getHost());
			if(token != null)	conn.connection.setRequestProperty("Authorization", "token " + token);
			conn.connection.setRequestProperty("From", "lib-java");
			conn.connection.setRequestProperty("Content-Type", "application/json");
			conn.connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			conn.connection.setDoOutput(true);
			conn.write(postDataBytes);

			return Connection.readJson(conn.read());

		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("The provided address " + address + " at " + getURL() + " is malformed: "
					+ getURL()+address, e);
		}
	}

	/**
	 * Connects to the provided URL.
	 * @param url the URL the API should connect to
	 * @return The connection to the server, on success.
	 * @throws CantConnectException Failure to connect to the server.
	 */
	static Connection connect(URL url) throws CantConnectException {
		try {
			return new Connection(url.openConnection());

		} catch (IOException e) {
			throw new CantConnectException("Cannot connect to the server.", e);
		}
	}

	/**
	 * Converts the JSON parameters to an array of bytes that can be sent in the request. The charset used is specified
	 * in {@link #CHARSET} ({@value #CHARSET}).
	 * @param params the JSON parameters to be converted.
	 * @return A byte array representing the provided parameters.
	 */
	static byte[] convertToByteArray(JsonValue params){
		try {
			return params.toString().getBytes(CHARSET);

		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("There was a problem with the character encoding '"+CHARSET+"'. Because it is" +
				"hard-written in the class, this error should never happen.", e);
		}
	}

	/**
	 * Thrown when the request failed because the requested could not connect to the server.
	 */
	public static class CantConnectException extends IOException {
		CantConnectException(String msg, Exception e){
			super(msg, e);
		}
	}

}
