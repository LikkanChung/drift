package sleepapp.backend.test;

import sleepapp.backend.Server;
import sleepapp.backend.auth.LoginResult;
import sleepapp.backend.auth.UserAuth;
import sleepapp.backend.auth.UserAuthService;

import java.sql.Connection;

public class ApiLoginTest {

    private static final String DB_URL = "jdbc:postgresql://localhost/sleep";
    private static final String DB_USERNAME = "jack";
    private static final String DB_PASSWORD = "21ysxqkcl1i3c3h8ou7l";

    public static void main(String[] args) {
        Connection conn = Server.openPostgresConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        UserAuthService userAuth = new UserAuthService(conn);
        System.out.println(userAuth.logIn("jack1999", "nope", UserAuth.ACCESS_LEVEL_READ_WRITE).getStatus());
        System.out.println(userAuth.logIn("itsyaboi", "letm1ein0", UserAuth.ACCESS_LEVEL_READ_WRITE).getStatus());

        LoginResult result = userAuth.logIn("jack1999", "letmein0", UserAuth.ACCESS_LEVEL_READ_WRITE);
        System.out.println(result.getStatus());
        System.out.println(result.getToken());
    }

}
