package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import sleepapp.backend.Server;
import sleepapp.backend.auth.LoginResult;
import sleepapp.backend.auth.UserAuth;
import sleepapp.backend.datamap.JsonToDatabaseMapper;
import sleepapp.backend.datamap.SqlType;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PairEndpoint extends Endpoint {

    private static final char[] CODE_CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D'};
    static {assert(CODE_CHARACTERS.length == 14);}

    private static final int SYN_CODE_LENGTH = 4;
    private static final int ACK_CODE_LENGTH = 4;
    private static final int MAX_GEN_CODE_RETRIES = 1;
    private static final Duration TRANSACTION_VALIDITY_PERIOD = Duration.ofMinutes(6);

    private static final long GC_INTERVAL_MILLIS = 1000L * 60 * 15; //TODO release build values
    private static final Duration GC_LEEWAY = Duration.ofMinutes(20);
    private Timer gcTimer = new Timer("pairing system gcTimer", true);

    private Connection db;
    private SecureRandom rand = new SecureRandom();
    private byte[] genBuf = new byte[2];

    private JsonToDatabaseMapper masterTransactionMapper;
    private JsonToDatabaseMapper slaveTransactionMapper;

    public PairEndpoint(Connection db) {
        this.db = db;
        masterTransactionMapper = new JsonToDatabaseMapper(
                db, "pairing_transactions", SqlType.STRING, "syn_code");
        masterTransactionMapper.defineReadOnlyStringColumn("syn_code");
        masterTransactionMapper.defineReadOnlyLongColumn("uid");
        masterTransactionMapper.defineReadOnlyIntegerColumn("access_level");
        masterTransactionMapper.defineReadOnlyStringColumn("ack_code");
        masterTransactionMapper.defineReadOnlyTimestampColumn("expire");

        masterTransactionMapper.setAuthKey("uid", SqlType.LONG);

        slaveTransactionMapper = new JsonToDatabaseMapper(
                db, "pairing_transactions", SqlType.STRING, "syn_code");
        slaveTransactionMapper.defineReadOnlyLongColumn("uid");
        slaveTransactionMapper.defineReadOnlyIntegerColumn("access_level");
        slaveTransactionMapper.defineReadOnlyStringColumn("ack_code");
        slaveTransactionMapper.defineReadOnlyTimestampColumn("expire");
        slaveTransactionMapper.defineReadOnlyByteaColumn("auth_token");
        slaveTransactionMapper.defineReadOnlyTimestampColumn("token_expire");

        slaveTransactionMapper.setAuthKey("ack_code", SqlType.STRING);

        gcTimer.schedule(garbageCollector, 0, GC_INTERVAL_MILLIS);
    }

    @Override
    public String getPath() {
        return "/pair";
    }

    @Override
    public int getMinimumAccessLevel() {
        return UserAuth.ACCESS_LEVEL_NONE;
    }

    @Override
    protected boolean post(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        if (userAuth.getAccessLevel() <= UserAuth.ACCESS_LEVEL_NONE) {
            writeResponse(exchange, "Access token required for this operation", HttpsURLConnection.HTTP_UNAUTHORIZED);
            return true;
        }

        String synCode = null;
        for (int i = 0; i < MAX_GEN_CODE_RETRIES + 1; i++) {
            synCode = genCode(SYN_CODE_LENGTH);

            PreparedStatement select = db.prepareStatement(
                    "SELECT syn_code FROM pairing_transactions WHERE syn_code = ?"
            );
            select.setString(1, synCode);

            ResultSet rs = select.executeQuery();
            if (rs.next())
                synCode = null;
            else
                break;
        }
        if (synCode == null) {
            writeResponse(exchange, "Code generation failure", HttpsURLConnection.HTTP_INTERNAL_ERROR);
            return true;
        }

        Instant expire = Instant.now().plus(TRANSACTION_VALIDITY_PERIOD);

        PreparedStatement insert = db.prepareStatement(
                "INSERT INTO pairing_transactions(syn_code, uid, access_level, expire) VALUES (?, ?, ?, ?)"
        );
        insert.setString(1, synCode);
        insert.setLong(2, userAuth.getUserId());
        insert.setInt(3, userAuth.getAccessLevel());
        insert.setTimestamp(4, Timestamp.from(expire));

        int result = insert.executeUpdate();
        if (result < 1) {
            writeResponse(exchange, "Unknown error", HttpsURLConnection.HTTP_INTERNAL_ERROR);
            return true;
        }

        JSONObject response = masterTransactionMapper.fetch(synCode);
        if (response == null) {
            writeResponse(exchange, "Error when building a response. Try a GET", HttpsURLConnection.HTTP_INTERNAL_ERROR);
            System.err.println("masterClientGetMapper " + masterTransactionMapper.explainError());
            return true;
        }

        writeResponse(exchange, response.toString(), HttpsURLConnection.HTTP_CREATED);
        return true;
    }

    @Override
    protected boolean patch(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        JSONObject json = getBodyAsJson(exchange);

        String synCode = json.optString("syn_code", null);
        if (synCode == null) {
            writeResponse(exchange, "syn code missing", HttpsURLConnection.HTTP_BAD_REQUEST);
            return true;
        }

        if (userAuth.getAccessLevel() <= UserAuth.ACCESS_LEVEL_NONE) {
            //Slave mode
            PreparedStatement select = db.prepareStatement(
                    "SELECT * FROM pairing_transactions " +
                            "WHERE syn_code = ? AND ack_code IS NULL"

            , ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            select.setString(1, synCode);
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                writeResponse(exchange, "Open pairing transaction not found", HttpsURLConnection.HTTP_NOT_FOUND);
                return true;
            }

            Instant expire = rs.getTimestamp("expire").toInstant();
            if (expire.isBefore(Instant.now())) {
                rs.deleteRow();
                System.out.println("Yeeted requested out of date pairing transaction"); //TODO remove
                writeResponse(exchange, "Code expired", HttpsURLConnection.HTTP_GONE);
                return true;
            }

            String ackCode = genCode(ACK_CODE_LENGTH);
            rs.updateString("ack_code", ackCode);
            rs.updateRow();

            JSONObject response = slaveTransactionMapper.fromResultSetRow(rs);
            writeResponse(exchange, response.toString(), HttpsURLConnection.HTTP_OK);
            return true;
        } else {
            //Master mode

            String ackCode = json.optString("ack_code", null);
            if (ackCode == null) {
                writeResponse(exchange, "ack_code missing", HttpsURLConnection.HTTP_BAD_REQUEST);
                return true;
            }

            PreparedStatement select = db.prepareStatement(
                    "SELECT * " +
                            "FROM pairing_transactions WHERE syn_code = ? AND uid = ? AND ack_code = ? AND auth_token IS NULL",

            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

            select.setString(1, synCode);
            select.setLong(2, userAuth.getUserId());
            select.setString(3, ackCode);

            ResultSet rs = select.executeQuery();
            if (!rs.next()) {
                writeResponse(exchange, "no matching unconfirmed pairing request found", HttpsURLConnection.HTTP_NOT_FOUND);
                return true;
            }

            Instant expire = rs.getTimestamp("expire").toInstant();
            if (expire.isBefore(Instant.now())) {
                rs.deleteRow();
                System.out.println("Yeeted expired token instead of patch by master client"); //TODO
                writeResponse(exchange, "expired", HttpsURLConnection.HTTP_GONE);
                return true;
            }

            int accessLevel = rs.getInt("access_level");
            LoginResult login = Server.getUserAuthService().createLogin(accessLevel, userAuth.getUserId());

            if (login.getStatus() != LoginResult.Status.SUCCESS) {
                writeResponse(exchange, "Internal error", HttpsURLConnection.HTTP_INTERNAL_ERROR);
                System.err.println("Login creation from pairing failed: " + login.getStatus().getReason());
                return true;
            }

            rs.updateBytes("auth_token", login.getRawToken());
            rs.updateTimestamp("token_expire", Timestamp.from(login.expires()));
            rs.updateRow();
            writeResponse(exchange, "OK", HttpsURLConnection.HTTP_OK);
            return true;
        }
    }

    @Override
    protected boolean get(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        JSONObject json = getBodyAsJson(exchange);

        if (userAuth.getAccessLevel() >= UserAuth.ACCESS_LEVEL_READ_WRITE) {
            //Master mode
            JSONObject response = masterTransactionMapper.fetch(json, userAuth.getUserId());
            if (response == null) {
                writeResponse(exchange, masterTransactionMapper.explainError(), HttpsURLConnection.HTTP_BAD_REQUEST);
                return true;
            }

            writeResponse(exchange, response.toString(), HttpsURLConnection.HTTP_OK);
            return true;
        } else {
            //Slave mode
            String ackCode = json.optString("ack_code");
            if (ackCode == null) {
                writeResponse(exchange, "request not authorised: missing ack_code", HttpsURLConnection.HTTP_UNAUTHORIZED);
                return true;
            }

            JSONObject response = slaveTransactionMapper.fetch(json, ackCode);
            if (response == null) {
                if (slaveTransactionMapper.getErrorFlag() == JsonToDatabaseMapper.Flag.INTERNAL_ERROR)
                    writeResponse(exchange, slaveTransactionMapper.explainError(), HttpsURLConnection.HTTP_INTERNAL_ERROR);
                else
                    writeResponse(exchange, slaveTransactionMapper.explainError(), HttpsURLConnection.HTTP_UNAUTHORIZED);

                return true;
            }

            writeResponse(exchange, response.toString(), HttpsURLConnection.HTTP_OK);
            return true;
        }
    }

    public String genCode(int length) {
        StringBuilder code = new StringBuilder(length);

        int unsigned;
        do {
            rand.nextBytes(genBuf);
            ShortBuffer sb = ByteBuffer.wrap(genBuf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            unsigned = Short.toUnsignedInt(sb.get());
        } while (unsigned > 38416); //n*8*14

        for (int i = 0; i < length; i++) {
            code.append(CODE_CHARACTERS[unsigned % 14]);
            unsigned /= 14;
        }

        return code.toString();
    }

    private final TimerTask garbageCollector = new TimerTask () {
        @Override
        public void run() {
            try {
                PreparedStatement stmt = db.prepareStatement(
                        "DELETE FROM pairing_transactions WHERE expire < ?"
                );
                Instant timeAfterLeeway = Instant.now().minus(GC_LEEWAY);
                stmt.setTimestamp(1, Timestamp.from(timeAfterLeeway));
                int result = stmt.executeUpdate();
                if (result > 0)
                    System.out.println("Pairing system GC yeeted " + result + " expired transactions "); //TODO
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    };
}
