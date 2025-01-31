package sleepapp.backend.auth;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.security.SecureRandom;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class UserAuthService {

    private static final String TOKEN_HEADER = "X-Auth-Token";
    private static final int TOKEN_LENGTH_BYTES = 64;
    public static final int TOKEN_LENGTH_STRING = 88;
    public static final long LOGIN_RATE_LIMIT_WAIT_MILLIS = 250;

    public static final long GC_INTERVAL_MILLIS = 1000L * 60 * 15; //TODO Release build value suggestion: 2 hours
    public static final Duration GC_LEEWAY = Duration.ofHours(12); //TODO Release build value suggestion: 30 days

    Connection db;
    SecureRandom rand = new SecureRandom();
    Base64.Encoder base64Encoder = Base64.getEncoder();
    Base64.Decoder base64Decoder = Base64.getDecoder();
    private Timer gcTimer = new Timer("UserAuthService gcTimer", true);

    public UserAuthService(Connection db) {
        this.db = db;

        gcTimer.schedule(garbageCollector, 0, GC_INTERVAL_MILLIS);
    }

    public LoginResult logIn(String username, String password, int accessLevel) {
        return logIn(username, null, password, accessLevel);
    }

    public LoginResult logIn(long uid, String password, int accessLevel) {
        return logIn(null, uid, password, accessLevel);
    }

    private LoginResult logIn(String username, Long uid, String password, int accessLevel) {
        if (!UserAuth.validAccessLevel(accessLevel))
            throw new IllegalArgumentException("Invalid accessLevel");

        try {
            Thread.sleep(LOGIN_RATE_LIMIT_WAIT_MILLIS);
        } catch (InterruptedException e) {
            System.out.println("Sleeping interrupted!");
            Thread.currentThread().interrupt();
        }

        try {
            String dbPassword; long obtainedUid;
            {
                ResultSet rs;
                if (uid != null) {
                    PreparedStatement stmt = db.prepareStatement(
                            "SELECT uid, password FROM users WHERE uid = ?"
                    );
                    stmt.setLong(1, uid);
                    rs = stmt.executeQuery();
                } else {
                    PreparedStatement stmt = db.prepareStatement(
                            "SELECT uid, password FROM users WHERE username = ?"
                    );
                    stmt.setString(1, username);
                    rs = stmt.executeQuery();
                }

                if (!rs.next()) {
                    return LoginResult.UNRECOGNISED;
                }

                dbPassword = rs.getString("password");
                String dbPasswordDecoded = decodePassword(dbPassword);

                if (!dbPasswordDecoded.equals(password)) {
                    return LoginResult.UNRECOGNISED;
                }

                obtainedUid = rs.getLong("uid");
            }

            return createLogin(accessLevel, obtainedUid);
        } catch (SQLException e) {
            e.printStackTrace();
            return LoginResult.DB_ERROR_HAPPENED;
        }
    }

    public LoginResult createLogin(int accessLevel, long uid) {
        byte[] tokenBuffer = new byte[TOKEN_LENGTH_BYTES];

        try {
            rand.nextBytes(tokenBuffer);

            Instant now = Instant.now();
            Instant expiryTime = now.plus(UserAuth.defaultValidityPeriod(accessLevel));

            PreparedStatement putStmt = db.prepareStatement(
                    "INSERT INTO authenticated_clients(uid, token, access_level, expire)" +
                            "VALUES (" +
                            "(SELECT (uid) FROM users WHERE uid = ?), " +
                            "(?), " +
                            "(?), " +
                            "(?)" +
                            ")"
            );
            putStmt.setLong(1, uid);
            putStmt.setBytes(2, tokenBuffer);
            putStmt.setInt(3, accessLevel);
            putStmt.setTimestamp(4, Timestamp.from(expiryTime));

            int success = putStmt.executeUpdate();
            if (success > 0) {
                return new LoginResult(LoginResult.Status.SUCCESS, uid, tokenBuffer, now, expiryTime);
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

    private final TimerTask garbageCollector = new TimerTask () {
        @Override
        public void run() {
            try {
                PreparedStatement stmt = db.prepareStatement(
                        "DELETE FROM authenticated_clients WHERE expire < ?"
                );
                Instant timeAfterLeeway = Instant.now().minus(GC_LEEWAY);
                stmt.setTimestamp(1, Timestamp.from(timeAfterLeeway));
                int result = stmt.executeUpdate();
                if (result > 0)
                    System.out.println("UserAuthService GC yeeted " + result + " expired tokens");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    };
}
