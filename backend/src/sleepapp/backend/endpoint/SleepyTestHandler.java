package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpExchange;
import sleepapp.backend.auth.UserAuth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.Map;

public class SleepyTestHandler extends Endpoint {

    private static final String MY_PATH = "/sleepyok";

    @Override
    public String getPath() {
        return MY_PATH;
    }

    @Override
    public int minimumAccessLevel() {
        return UserAuth.ACCESS_LEVEL_NONE;
    }

    @Override
    protected boolean get(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        writeResponse(exchange, "Sleepy OK!", HttpURLConnection.HTTP_OK);
        return true;
    }

    @Override
    protected boolean post(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean put(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean head(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean delete(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean patch(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean options(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

}
