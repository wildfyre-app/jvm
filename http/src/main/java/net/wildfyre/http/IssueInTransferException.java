package net.wildfyre.http;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.*;
import java.util.Optional;

/**
 * Signifies that the server refused the data. More information about the problem can be found using {@link #getJson()}.
 */
public class IssueInTransferException extends IOException {
	private JsonValue json;

	/**
	 * Creates a new IssueInTransferException.
	 * @param msg the message
	 * @param in the response from the server, that will be parsed as JSON if possible
	 */
	public IssueInTransferException(String msg, InputStream in) {
		super(msg);

		String out = "";
		try {

			Reader input = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			StringBuilder output = new StringBuilder();

			for (int c; (c = input.read()) >= 0; ) {
				output.append((char) c);
			}
			out = output.toString();

			if (out.length() != 0)
				this.json = Json.parse(out);

		} catch (IOException | NullPointerException e) { //  No data found.
		} catch (ParseException e){
			this.json = new JsonObject()
				.add("Server", out);
		}
	}

    /**
     * Creates a new IssueInTransferException.
     * @param msg the message
     * @param cause what caused the issue
     */
	public IssueInTransferException(String msg, Exception cause) {
	    super(msg, cause);

	    this.json = null;
    }

	/**
	 * Get the JSON sent by the server (if any).
	 * @return the JSON sent by the server, or an empty Optional if none was sent.
	 */
	public Optional<JsonValue> getJson(){
		return Optional.ofNullable(json);
	}
}
