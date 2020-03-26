package sleepapp.backend.endpoint;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import sleepapp.backend.Server;
import sleepapp.backend.auth.UserAuth;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class Endpoint implements HttpHandler {

    public abstract String getPath();
    public abstract int minimumAccessLevel();
    protected abstract boolean get(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean post(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean put(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean head(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean delete(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean patch(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException;
    protected abstract boolean options(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException;

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Map<String, String> params = queryToMap(ex.getRequestURI().getQuery());
//        long userid = 0;
//        String useridString = params.get("userid");
//        if (useridString != null) {
//            try {
//                userid = Long.parseLong(useridString);
//            } catch (NumberFormatException e) {
//
//            }
//        }

        UserAuth userAuth = Server.getUserAuthService().authRequest(ex);

        if (userAuth.getReason() == UserAuth.Reason.EXPIRED_TOKEN) {
            writeResponse(ex, "Token expired", HttpsURLConnection.HTTP_UNAUTHORIZED);
            ex.close();
            return;
        }

        if (userAuth.getAccessLevel() < minimumAccessLevel()) {
            ex.sendResponseHeaders(HttpsURLConnection.HTTP_UNAUTHORIZED, 0);
            ex.close();
            return;
        }

        System.out.println("Resolving request for userid " + userAuth.getUserId());

        String method = ex.getRequestMethod().toUpperCase();
        boolean response = false;
        try {
            switch (method) {
                case "GET":
                    response = get(ex, userAuth, params);
                    break;
                case "POST":
                    response = post(ex, userAuth, params);
                    break;
                case "PUT":
                    response = put(ex, userAuth, params);
                    break;
                case "HEAD":
                    response = head(ex, userAuth, params);
                    break;
                case "DELETE":
                    response = delete(ex, userAuth, params);
                    break;
                case "PATCH":
                    response = patch(ex, userAuth, params);
                    break;
                case "OPTIONS":
                    response = options(ex, userAuth, params);
                    break;
            }
        } catch (IOException e) {
            System.err.println("IOException in request handler");
            e.printStackTrace();
            ex.sendResponseHeaders(HttpsURLConnection.HTTP_INTERNAL_ERROR, 0);
            ex.close();
            return;
        } catch (SQLException e) {
            System.err.println("SQLException in request handler");
            e.printStackTrace();
            ex.sendResponseHeaders(HttpsURLConnection.HTTP_INTERNAL_ERROR, 0);
            ex.close();
            return;
        } catch (JSONException e) {
            System.err.println("JSONException in request handler");
            e.printStackTrace();
            ex.sendResponseHeaders(HttpsURLConnection.HTTP_INTERNAL_ERROR, 0);
            ex.close();
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
                new AlarmsHandler(dbConn),
                new LoginHandler()
        };
    }

}
