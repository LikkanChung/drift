package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.awt.font.NumericShaper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class AlarmsHandler extends Endpoint {

    private static final String MY_PATH = "/alarms";

    private Connection db;

    public AlarmsHandler(Connection db) {
        this.db = db;
    }

    private JSONArray alarmsToJson(ResultSet rs) throws SQLException {
        JSONArray array = new JSONArray();
        while (rs.next()) {
            JSONObject obj = new JSONObject();
            obj.put("aid", rs.getLong("aid"));
            obj.put("time", rs.getTimestamp("time").toString());
            array.put(obj);
        }
        return array;
    }

    @Override
    public String getPath() {
        return MY_PATH;
    }

    @Override
    protected boolean get(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException {
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
        stmt.setLong(1, userid);

        if (max != null)
            stmt.setInt(2, max);

        ResultSet rs = stmt.executeQuery();
        writeResponse(exchange, alarmsToJson(rs).toString(), HttpsURLConnection.HTTP_OK);

        return true;
    }

    @Override
    protected boolean post(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean put(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean head(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean delete(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean patch(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException {
        return false;
    }

    @Override
    protected boolean options(HttpExchange exchange, long userid, Map<String, String> params) throws IOException, SQLException {
        return false;
    }
}
