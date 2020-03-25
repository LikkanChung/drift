package sleepapp.backend.auth;

import com.sun.net.httpserver.HttpExchange;

import java.sql.Connection;

public class UserAuthService {

    Connection db;

    public UserAuthService(Connection db) {
        this.db = db;
    }

    public LoginResult logIn(String username, String password) {
        return LoginResult.UNRECOGNISED; //TODO
    }

    public UserAuth authRequest(HttpExchange exchange) {
        return UserAuth.EXPIRED_TOKEN; //TODO
    }
}
