package sleepapp.backend.auth;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

public class UserAuthService {

    private static final String TOKEN_HEADER = "X-Auth-Token";
    private static final int TOKEN_LENGTH_BYTES = 64;
    public static final int TOKEN_LENGTH_STRING = 88;
    public static final long LOGIN_RATE_LIMIT_WAIT_MILLIS = 250;

    Connection db;
    SecureRandom rand;
    Base64.Encoder base64Encoder = Base64.getEncoder();
    Base64.Decoder base64Decoder = Base64.getDecoder();
    byte[] tokenBuffer = new byte[TOKEN_LENGTH_BYTES];

    public UserAuthService(Connection db) {
        this.db = db;

        try {
            rand = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("SecureRandom.getInstanceStrong() failed");
        }
    }

    public LoginResult logIn(String username, String password, int accessLevel) {
        if (!UserAuth.validAccessLevel(accessLevel))
            throw new IllegalArgumentException("Invalid accessLevel");

        try {
            Thread.sleep(LOGIN_RATE_LIMIT_WAIT_MILLIS);
        } catch (InterruptedException e) {
            System.out.println("Sleeping interrupted!");
            Thread.currentThread().interrupt();
        }

        try {
            String dbPassword;
            {
                PreparedStatement stmt = db.prepareStatement(
                        "SELECT (password) FROM users WHERE username = ?"
                );
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    return LoginResult.UNRECOGNISED;
                }

                dbPassword = rs.getString("password");
                String dbPasswordDecoded = decodePassword(dbPassword);

                if (!dbPasswordDecoded.equals(password)) {
                    return LoginResult.UNRECOGNISED;
                }
            }

            rand.nextBytes(tokenBuffer);
            String token = base64Encoder.encodeToString(tokenBuffer);
            Instant now = Instant.now();
            Instant expiryTime = now.plus(UserAuth.defaultValidityPeriod(accessLevel));

            PreparedStatement stmt = db.prepareStatement(
                    "INSERT INTO authenticated_clients(uid, token, access_level, expire)" +
                            "VALUES (" +
                            "(SELECT (uid) FROM users WHERE username = ? AND password = ?), " +
                            "(?), " +
                            "(?), " +
                            "(?)" +
                            ")"
            );
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setBytes(3, tokenBuffer);
            stmt.setInt(4, accessLevel);
            stmt.setTimestamp(5, Timestamp.from(expiryTime));

            int success = stmt.executeUpdate();
            if (success > 0) {
                return new LoginResult(LoginResult.Status.SUCCESS, token, now, expiryTime);
            } else {
                System.err.println("Add authenticated client update failed");
                return LoginResult.DB_ERROR_HAPPENED;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return LoginResult.DB_ERROR_HAPPENED;
        }
    }

    public UserAuth authRequest(HttpExchange exchange) {
        Headers requestHeaders = exchange.getRequestHeaders();
        if (!requestHeaders.containsKey(TOKEN_HEADER))
            return UserAuth.RESULT_NO_TOKEN;

        String token = requestHeaders.getFirst(TOKEN_HEADER).trim();
        if (token.length() != TOKEN_LENGTH_STRING)
            return UserAuth.RESULT_INVALID_TOKEN;

        byte[] tokenBytes = base64Decoder.decode(token);

        try {
            PreparedStatement stmt = db.prepareStatement(
                    "SELECT uid, access_level, expire FROM authenticated_clients WHERE token = ?"
            );

            stmt.setBytes(1, tokenBytes);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                return UserAuth.RESULT_INVALID_TOKEN;

            Timestamp expire = rs.getTimestamp("expire");
            Date now = new Date();
            if (expire.compareTo(now) < 0) {
                PreparedStatement updateStmt = db.prepareStatement(
                        "DELETE FROM authenticated_clients WHERE token = ?"
                );

                updateStmt.setBytes(1, tokenBytes);
                int success = updateStmt.executeUpdate();
                if (success > 0) {
                    System.out.println("Yeeted expired token " + base64Encoder.encodeToString(tokenBytes));
                } else {
                    System.err.println("Removing expired token " + base64Encoder.encodeToString(tokenBytes) + "failed!");
                }

                return UserAuth.RESULT_EXPIRED_TOKEN;
            }

            long userid = rs.getLong("uid");
            int accessLevel = rs.getInt("access_level");

            return new UserAuth(userid, accessLevel);
        } catch (SQLException e) {
            e.printStackTrace();
            return UserAuth.RESULT_UNKNOWN_ERROR;
        }
    }

    private String decodePassword(String password) {
        return password;
    }
}
