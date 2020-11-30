package com.danikula.videocache.core;

import android.text.TextUtils;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpGetRequest;
import com.danikula.videocache.source.HttpUrlSource;
import com.danikula.videocache.ProxyCacheException;
import com.danikula.videocache.file.FileCache;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Locale;

import static com.danikula.videocache.util.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;

/**
 * {@link ProxyCache} that read http url and writes data to {@link Socket}
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpProxyCache extends ProxyCache {

    private static final float NO_CACHE_BARRIER = .2f;

    private final HttpUrlSource httpSource;
    private final FileCache cache;
    private CacheListener listener;

    public HttpProxyCache(HttpUrlSource httpSource, FileCache cache) {
        super(httpSource, cache);
        this.cache = cache;
        this.httpSource = httpSource;
    }

    public void registerCacheListener(CacheListener cacheListener) {
        this.listener = cacheListener;
    }

    /**
     * 1.   有缓存
     *  1）  先从http连接中读取流到缓存中
     *  2）  然后从缓存中读取并输出到socket中
     * 2.   没有缓存
     *  直接从http连接中读取字节流并输出到socket
     * @param request
     * @param socket
     * @throws IOException
     * @throws ProxyCacheException
     */
    public void processRequest(HttpGetRequest request, Socket socket) throws IOException, ProxyCacheException {
        OutputStream socketOut = new BufferedOutputStream(socket.getOutputStream());
        String responseHeaders = newResponseHeaders(request);
        socketOut.write(responseHeaders.getBytes("UTF-8"));

        long offset = request.rangeOffset;
        if (isUseCache(request)) {
            responseWithCache(socketOut, offset);
        } else {
            responseWithoutCache(socketOut, offset);
        }
    }

    /**
     * 是否先缓存再播放
     * @param request
     * @return
     * @throws ProxyCacheException
     */
    private boolean isUseCache(HttpGetRequest request) throws ProxyCacheException {
        long sourceLength = httpSource.length();
        boolean sourceLengthKnown = sourceLength > 0;
        long cacheAvailable = cache.available();
        // do not use cache for partial requests which too far from available cache. It seems user seek video.
        return !sourceLengthKnown || !request.partial || request.rangeOffset <= cacheAvailable + sourceLength * NO_CACHE_BARRIER;
    }

    private String newResponseHeaders(HttpGetRequest request) throws IOException, ProxyCacheException {
        String mime = httpSource.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        long length = cache.isCompleted() ? cache.available() : httpSource.length();
        boolean lengthKnown = length >= 0;
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;
        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length - 1, length) : "")
                .append(mimeKnown ? format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();
    }

    private void responseWithCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int readBytes;
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            out.write(buffer, 0, readBytes);
            offset += readBytes;
        }
        out.flush();
    }

    private void responseWithoutCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        HttpUrlSource newSourceNoCache = new HttpUrlSource(this.httpSource);
        try {
            newSourceNoCache.open((int) offset);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = newSourceNoCache.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
                offset += readBytes;
            }
            out.flush();
        } finally {
            newSourceNoCache.close();
        }
    }

    private String format(String pattern, Object... args) {
        return String.format(Locale.US, pattern, args);
    }

    @Override
    protected void onCachePercentsAvailableChanged(int percents) {
        if (listener != null) {
            listener.onCacheAvailable(cache.file, httpSource.getUrl(), percents);
        }
    }
}
