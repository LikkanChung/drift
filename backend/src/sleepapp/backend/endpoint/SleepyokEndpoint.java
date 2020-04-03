package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpExchange;
import sleepapp.backend.auth.UserAuth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.Map;

public class SleepyokEndpoint extends Endpoint {

    @Override
    public String getPath() {
        return "/sleepyok";
    }

    @Override
    public int getMinimumAccessLevel() {
        return UserAuth.ACCESS_LEVEL_NONE;
    }

    @Override
    protected boolean get(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        writeResponse(exchange, "Sleepy OK!", HttpURLConnection.HTTP_OK);
        return true;
    }

}
