package net.wildfyre.http;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.Test;

import static com.eclipsesource.json.WriterConfig.PRETTY_PRINT;
import static net.wildfyre.http.Method.POST;
import static org.junit.Assert.*;

public class RequestTest {

	@Test
	public void request(){
		try {
			JsonValue j = Request.request(POST, null, "/account/auth/", new JsonObject()
				.add("username", "libtester")
				.add("password", "thisisnotatest"));

            assertNotNull(j.asObject().getString("token", null));
		} catch (Request.CantConnectException e) {
			throw new IllegalArgumentException(e);
		} catch (IssueInTransferException e) {
			if(e.getJson().isPresent())
				fail("Issue in transfer: " + e.getJson().get().toString(PRETTY_PRINT));
			else
				fail("Unknown issue in transfer.");
		}
	}

	@Test
	public void testAvailabilityOfDatabase(){
	    assertNotEquals("default", System.getProperty("org.gradle.test.worker", "default"));
        assertTrue(Request.isTesting);
    }

}
