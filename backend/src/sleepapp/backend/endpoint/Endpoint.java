package sleepapp.backend.endpoint;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class Endpoint implements HttpHandler {

    public abstract String getPath();
    protected abstract boolean get(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean post(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean put(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean head(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean delete(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean patch(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean options(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException;

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Map<String, String> params = queryToMap(ex.getRequestURI().getQuery());
        long userid = 0;
        String useridString = params.get("userid");
        if (useridString != null) {
            try {
                userid = Long.parseLong(useridString);
            } catch (NumberFormatException e) {

            }
        }

        System.out.println("Resolving request for userid " + userid);

        String method = ex.getRequestMethod().toUpperCase();
        boolean response = false;
        try {
            switch (method) {
                case "GET":
                    response = get(ex, userid, params);
                    break;
                case "POST":
                    response = post(ex, userid, params);
                    break;
                case "PUT":
                    response = put(ex, userid, params);
                    break;
                case "HEAD":
                    response = head(ex, userid, params);
                    break;
                case "DELETE":
                    response = delete(ex, userid, params);
                    break;
                case "PATCH":
                    response = patch(ex, userid, params);
                    break;
                case "OPTIONS":
                    response = options(ex, userid, params);
                    break;
            }
        } catch (IOException e) {
            System.err.println("IOException in request handler");
            e.printStackTrace();
            ex.sendResponseHeaders(HttpsURLConnection.HTTP_INTERNAL_ERROR, 0);
            return;
        } catch (SQLException e) {
            System.err.println("SQLException in request handler");
            e.printStackTrace();
            ex.sendResponseHeaders(HttpsURLConnection.HTTP_INTERNAL_ERROR, 0);
            return;
        }

        if (!response)
            ex.sendResponseHeaders(HttpsURLConnection.HTTP_BAD_METHOD, 0);

        ex.close();
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
        }
        return result;
    }

    protected void writeResponse(HttpExchange ex, String resp, int status) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = StandardCharsets.UTF_8.encode(resp).array();
        ex.sendResponseHeaders(status, responseBytes.length);

        ex.getResponseBody().write(responseBytes);
    }

    public static Endpoint[] initialiseAll(Connection dbConn) {
        return new Endpoint[] {
                new SleepyTestHandler(),
                new AlarmsHandler(dbConn)
        };
    }

}
