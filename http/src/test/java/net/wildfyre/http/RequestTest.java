package net.wildfyre.http;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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
    public void testJSON() {
        InputStream input = null;
        try {
            input = new ByteArrayInputStream("{\"data\": 2}".getBytes(Request.CHARSET));
        } catch (UnsupportedEncodingException e) {
            fail("Could not read Stream.");
        }

        assertNotNull(input);
        try {
            JsonValue json = Request.read(input);

            assertEquals(new JsonObject().add("data", 2), json);
        } catch (IssueInTransferException e) {
            fail("Could not parse to JSON.");
        }
    }

    @Test
    public void testAvailabilityOfDatabase(){
        assertNotEquals("default", System.getProperty("org.gradle.test.worker", "default"));
        assertTrue(Request.isTesting);
    }

}
