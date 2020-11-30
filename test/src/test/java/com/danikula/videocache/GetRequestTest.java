package com.danikula.videocache;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class GetRequestTest extends BaseTest {

    @Test
    public void testPartialHttpGet() throws Exception {
        HttpGetRequest getRequest = new HttpGetRequest("" +
                "GET /uri HTTP/1.1\n" +
                "Host: 127.0.0.1:44684\n" +
                "Range: bytes=9860723-" +
                "Accept-Encoding: gzip");
        assertThat(getRequest.rangeOffset).isEqualTo(9860723);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isTrue();
    }

    @Test
    public void testNotPartialHttpGet() throws Exception {
        HttpGetRequest getRequest = new HttpGetRequest("" +
                "GET /uri HTTP/1.1\n" +
                "Host: 127.0.0.1:44684\n" +
                "Accept-Encoding: gzip");
        assertThat(getRequest.rangeOffset).isEqualTo(0);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isFalse();
    }

    @Test
    public void testReadStream() throws Exception {
        String requestString = "GET /uri HTTP/1.1\nRange: bytes=9860723-\n";
        InputStream stream = new ByteArrayInputStream(requestString.getBytes());
        HttpGetRequest getRequest = HttpGetRequest.read(stream);
        assertThat(getRequest.rangeOffset).isEqualTo(9860723);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isTrue();
    }

    @Test
    public void testMinimal() throws Exception {
        HttpGetRequest getRequest = new HttpGetRequest("GET /uri HTTP/1.1");
        assertThat(getRequest.rangeOffset).isEqualTo(0);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() throws Exception {
        HttpGetRequest getRequest = new HttpGetRequest("");
        fail("Empty request");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid() throws Exception {
        HttpGetRequest getRequest = new HttpGetRequest("/uri HTTP/1.1\n");
        fail("Invalid request");
    }

}
