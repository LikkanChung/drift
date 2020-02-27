package sleepapp.backend.endpoint;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SleepyTestHandler extends Endpoint {

    private static final String MY_PATH = "/sleepyok";
    private static final String RESPONSE = "Sleepy OK!";
    private static final byte[] RESPONSE_BYTES;

    static {
        ByteBuffer responseBuf = StandardCharsets.UTF_8.encode(RESPONSE);
        RESPONSE_BYTES = responseBuf.array();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers respHeaders = exchange.getResponseHeaders();
        respHeaders.add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, RESPONSE_BYTES.length);

        OutputStream out = exchange.getResponseBody();
        out.write(RESPONSE_BYTES);

        exchange.close();
    }

    @Override
    public String getPath() {
        return MY_PATH;
    }
}
