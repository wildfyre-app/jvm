package net.wildfyre.http;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import sun.net.www.protocol.http.HttpURLConnection;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.net.URLConnection;

import static net.wildfyre.http.Request.CantConnectException;

/**
 * This class is a wrapper around a {@link java.net.URLConnection}. It is necessary because some methods like
 * {@code setRequestMethod} or {@code getErrorStream} are available in both {@link java.net.HttpURLConnection} and
 * {@link javax.net.ssl.HttpsURLConnection} but not in their common supertype. Because the API should handle both HTTP
 * and HTTPS, this class was added to serve as a bridge.
 *
 * This class is NOT part of the public API.
 */
class Connection {

	final URLConnection connection;
	final boolean isHttp;

	Connection(URLConnection conn){
		if(!(conn instanceof HttpURLConnection) && !(conn instanceof HttpsURLConnectionImpl))
			throw new IllegalArgumentException("The connection should be either a HttpURLConnection or a " +
				"HttpsURLConnection, found: " + conn.getClass().getName());

		this.connection = conn;
		this.isHttp = conn instanceof HttpURLConnection;
	}

	/**
	 * Sets the requested method.
	 * @param method the HTTP method.
	 */
	void setRequestMethod(Method method) {
		try {
			if (isHttp)
				((HttpURLConnection) connection).setRequestMethod(method.toString());
			else
				((HttpsURLConnectionImpl) connection).setRequestMethod(method.toString());

		} catch (ProtocolException ex) {
			throw new IllegalArgumentException("Cannot set the method to " + method, ex);
		}
	}

	/**
	 * The error stream sent by the server on failure of the request.
	 * @return The error stream.
	 */
	InputStream getErrorStream() {
		return isHttp ? ((HttpURLConnection)  	  connection).getErrorStream()
			          : ((HttpsURLConnectionImpl) connection).getErrorStream();
	}

    /**
     * Write some data to the server.
     * @param bytes the data that will be sent to the server.
     * @throws CantConnectException If an I/O exception occurs when trying to get the OutputStream.
     */
	void write(byte[] bytes) throws CantConnectException {
        try {
            connection.getOutputStream().write(bytes);

        } catch (IOException e) {
            throw new CantConnectException("Cannot open stream to the connection.", e);
        }
    }

    /**
     * Reads the server's response.
     * @return The server's response.
     * @throws IssueInTransferException If the server refuses the request, see
     * {@link IssueInTransferException#getJson() getJson()} to get the eventual error message.
     */
    InputStream read() throws IssueInTransferException {
        try {
            return connection.getInputStream();

        } catch (IOException e) {
            throw new IssueInTransferException("The server refused the request.", getErrorStream());
        }
    }

    /**
     * Reads the JSON from an InputStream.
     * @param input the written JSON
     * @return The parsed JSON value.
     * @throws IssueInTransferException If a I/O error occurs during parsing.
     */
    static JsonValue readJson(InputStream input) throws IssueInTransferException {
        try {
            return Json.parse(new InputStreamReader(input, Request.CHARSET));

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("There was an encoding error. Since the encoding is hardcoded in Request.CHARSET," +
                "this error should never occur.", e);

        } catch (IOException e) {
            throw new IssueInTransferException("There was an I/O error while parsing the JSON data.", e);
        }
    }

}
