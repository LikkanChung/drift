package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import sleepapp.backend.Server;
import sleepapp.backend.auth.LoginResult;
import sleepapp.backend.auth.UserAuth;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

public class LoginHandler extends Endpoint {
    @Override
    public String getPath() {
        return "/login";
    }

    @Override
    public int minimumAccessLevel() {
        return UserAuth.ACCESS_LEVEL_NONE;
    }

    @Override
    protected boolean get(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean post(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        JSONObject json = getBodyAsJson(exchange);
        String username = json.optString("username", null);
        String password = json.optString("password", null);
        String accessLevelString = json.optString("access_level", null);

        if (username == null || password == null) {
            writeResponse(exchange, "Username or password missing", HttpsURLConnection.HTTP_BAD_REQUEST);
            return true;
        }

        int accessLevel = UserAuth.ACCESS_LEVEL_NONE;
        if (accessLevelString == null) {
            accessLevel = UserAuth.ACCESS_LEVEL_READ_WRITE;
        } else {
            switch (accessLevelString) {
                case "account":
                    accessLevel = UserAuth.ACCESS_LEVEL_ACCOUNT;
                    break;
                default:
                    writeResponse(exchange, "Invalid access level", HttpsURLConnection.HTTP_BAD_REQUEST);
                    return true;
            }
        }

        LoginResult result = Server.getUserAuthService().logIn(username, password, accessLevel);
        switch (result.getStatus()) {
            case SUCCESS:
                JSONObject response = new JSONObject();
                response.put("token", result.getToken());
                response.put("begins", result.begins());
                response.put("expires", result.expires());
                writeResponse(exchange, response.toString(), HttpsURLConnection.HTTP_CREATED);
                return true;
            case NOT_RECOGNISED:
                writeResponse(exchange, "Username and password not recognised", HttpsURLConnection.HTTP_UNAUTHORIZED);
                return true;
            default:
                writeResponse(exchange, "Internal error", HttpsURLConnection.HTTP_INTERNAL_ERROR);
                return true;
        }
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
