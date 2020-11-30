package com.danikula.test;

import com.danikula.videocache.Pinger;
import com.danikula.videocache.api.HttpProxyCacheServer;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.net.Socket;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Pinger}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class PingerTest extends BaseTest {

    @Test
    public void testPingFail() throws Exception {
        Pinger pinger = new Pinger("127.0.0.1", 33);
        boolean pinged = pinger.ping(3, 70);
        assertThat(pinged).isFalse();
    }

    @Test
    public void testIsPingRequest() throws Exception {
        Pinger pinger = new Pinger("127.0.0.1", 1);
        assertThat(pinger.isPingRequest("ping")).isTrue();
        assertThat(pinger.isPingRequest("notPing")).isFalse();
    }

    @Test
    public void testResponseToPing() throws Exception {
        Pinger pinger = new Pinger("127.0.0.1", 1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(out);
        pinger.responseToPing(socket);
        assertThat(out.toString()).isEqualTo("HTTP/1.1 200 OK\n\nping ok");
    }
}
