package net.wildfyre.http;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.Test;

import static com.eclipsesource.json.WriterConfig.PRETTY_PRINT;
import static net.wildfyre.http.Method.POST;
import static org.junit.Assert.fail;

public class RequestTest {

	@Test
	public void request(){
		try {
			JsonValue j = Request.request(POST, null, "/account/auth/", new JsonObject()
				.add("username", "libtester")
				.add("password", "thisisnotatest"));

			System.out.println(j.toString(PRETTY_PRINT));
		} catch (Request.CantConnectException e) {
			throw new IllegalArgumentException(e);
		} catch (IssueInTransferException e) {
			if(e.getJson().isPresent())
				fail("Issue in transfer: " + e.getJson().get().toString(PRETTY_PRINT));
			else
				fail("Unknown issue in transfer.");
		}
	}

}
