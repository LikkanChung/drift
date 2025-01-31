package sleepapp.backend;

import com.sun.net.httpserver.HttpServer;
import sleepapp.backend.auth.UserAuthService;
import sleepapp.backend.endpoint.Endpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;


public class Server {

    private static final int BACKLOG = 10;
    private static final int PORT = 3333;
    private static final String DB_URL = "jdbc:postgresql://localhost/sleep";
    private static final String DB_USERNAME = "jack";
    private static final String DB_PASSWORD = "21ysxqkcl1i3c3h8ou7l"; //TODO I'm uncomfortable

    private static UserAuthService userAuthService;

    public static void main(String[] args) throws IOException {
        Scanner stdin = new Scanner(System.in);
        Connection dbConn = openPostgresConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        userAuthService = new UserAuthService(dbConn);

        if (dbConn == null) {
            System.out.println("Opening db connection failed - terminating...");
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), BACKLOG);

        for (Endpoint e: Endpoint.initialiseAll(dbConn)) {
            server.createContext(e.getPath(), e);
        }

        server.start();
        System.out.println("Server running, stopping on input...");
        stdin.nextLine();
        server.stop(0);
        System.out.println("Bye then");
    }

    public static Connection openPostgresConnection(String dbUrl, String dbUsername, String dbPassword) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Program configuration error - failed to link postgresql drivers");
            return null;
        }

        try {
            return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        } catch (SQLException e) {
            System.out.println("Connection to the database failed! Reason: " + e.getMessage());
            System.out.println("Terminating.");
            return null;
        }

    }

    public static UserAuthService getUserAuthService() {
        return userAuthService;
    }

}
