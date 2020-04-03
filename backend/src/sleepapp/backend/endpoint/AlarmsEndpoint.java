package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import sleepapp.backend.auth.UserAuth;
import sleepapp.backend.datamap.JsonToDatabaseMapper;
import sleepapp.backend.datamap.SqlType;
import sleepapp.backend.datamap.ValidationPolicy;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

public class AlarmsEndpoint extends Endpoint {

    private Connection db;
    private JsonToDatabaseMapper mapper;

    public AlarmsEndpoint(Connection db) {
        this.db = db;
        mapper = new JsonToDatabaseMapper(db, "alarms", SqlType.LONG, "aid");
        mapper.setAuthKey("uid", SqlType.LONG);
        mapper.defineReadOnlyLongColumn("aid");
        mapper.defineWriteableTimestampColumn("time", (Instant val) -> {
            if (val.isBefore(Instant.now())) {
                return "Can't set an alarm for a time in the past";
            } else {
                return ValidationPolicy.OK;
            }
        });
    }

    @Override
    public String getPath() {
        return "/alarms";
    }

    @Override
    public int getMinimumAccessLevel() {
        return UserAuth.ACCESS_LEVEL_READ_WRITE;
    }

    @Override
    protected boolean get(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        StringBuilder query = new StringBuilder("\nSELECT aid, time FROM alarms " +
                "\nWHERE alarms.uid = ? ");

        if (params.containsKey("future")) {
            query.append("\nAND time > now()");
        }

        query.append("\nORDER BY time ");

        Integer max = null;
        if (params.containsKey("max")) {
            try {
                int maxParam = Integer.parseInt(params.get("max"));
                if (maxParam > 0) {
                    query.append("\nLIMIT ? ");
                    max = maxParam;
                }
            } catch (NumberFormatException ignored) {}
        }

        PreparedStatement stmt = db.prepareStatement(query.toString());
        stmt.setLong(1, userAuth.getUserId());

        if (max != null)
            stmt.setInt(2, max);

        ResultSet rs = stmt.executeQuery();
        JSONArray json = mapper.fromResultSet(rs);
        writeResponse(exchange, json.toString(), HttpsURLConnection.HTTP_OK);

        return true;
    }

    @Override
    protected boolean post(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return defaultPost(exchange, mapper, userAuth);
    }

    @Override
    protected boolean delete(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return defaultDelete(exchange, mapper, userAuth);
    }

    @Override
    protected boolean patch(HttpExchange exchange, UserAuth userAuth, Map<String, String> params) throws IOException, SQLException {
        return defaultPatch(exchange, mapper, userAuth);
    }

}
