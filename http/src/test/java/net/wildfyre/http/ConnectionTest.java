package net.wildfyre.http;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.Test;
import sun.net.www.protocol.http.HttpURLConnection;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.*;

public class ConnectionTest {

	@Test
	public void testConnection(){
		try {
			URLConnection ftp = new URL("ftp://clovis.online").openConnection();
			URLConnection http = new URL("http://google.com").openConnection();
			URLConnection https = new URL("https://google.com").openConnection();

			try{
				new Connection(ftp);
				fail("Creating a Connection object on something else than a HTTP of HTTPS connection should fail!");
			} catch(IllegalArgumentException e) {}

			assertEquals(HttpURLConnection.class, http.getClass());
			assertEquals(HttpsURLConnectionImpl.class, https.getClass());

			try {
                Connection httpConn = new Connection(http);
                Connection httpsConn = new Connection(https);
            } catch(IllegalArgumentException e){
			    fail("Creating a http or https connection should not fail; " + e.getLocalizedMessage());
			}

			assertTrue(true); // All tests are done.

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testJSON(){
        InputStream input = null;
	    try {
            input = new ByteArrayInputStream("{\"data\": 2}".getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            fail("Could not read Stream.");
        }

        assertNotNull(input);
        try {
            JsonValue json = Connection.readJson(input);

            assertEquals(new JsonObject().add("data", 2), json);
        } catch (IssueInTransferException e) {
            fail("Could not parse to JSON.");
        }
    }

}
