package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import sleepapp.backend.Server;
import sleepapp.backend.auth.LoginResult;
import sleepapp.backend.auth.UserAuth;
import sleepapp.backend.auth.UserAuthService;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class LoginEndpoint extends Endpoint {
    @Override
    public String getPath() {
        return "/login";
    }

    @Override
    public int getMinimumAccessLevel() {
        return UserAuth.ACCESS_LEVEL_NONE;
    }

    @Override
    protected boolean post(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        JSONObject json = getBodyAsJson(exchange);
        long uid = json.optLong("uid", -1);
        String username = json.optString("username", null);
        String password = json.optString("password", null);
        String accessLevelString = json.optString("access_level", null);

        if (password == null) {
            writeResponse(exchange, "No password given", HttpsURLConnection.HTTP_BAD_REQUEST);
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

        UserAuthService auth = Server.getUserAuthService();
        LoginResult result;
        if (uid < 1) {
            if (username == null) {
                writeResponse(exchange, "No username or uid specified", HttpsURLConnection.HTTP_BAD_REQUEST);
                return true;
            }

            result = auth.logIn(username, password, accessLevel);
        } else {
            result = auth.logIn(uid, password, accessLevel);
        }

        switch (result.getStatus()) {
            case SUCCESS:
                JSONObject response = new JSONObject();
                response.put("uid", result.getUid());
                response.put("token", result.getTokenAsString());
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
}
