package sleepapp.backend.datamap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class JsonToDatabaseMapper {

    public static final String VALIDATION_OK = null;

    private static class WritableColumn {
        protected SqlType type;
        protected ValidationPolicy<?> validationPolicy;

        protected WritableColumn(SqlType type, ValidationPolicy<?> policy) {
            this.type = type;
            this.validationPolicy = policy;
        }
    }

    private static class ColumnEdit {
        protected WritableColumn col;
        protected Object newVal;
        protected String colName;

        protected ColumnEdit(String colName, WritableColumn col, Object newVal) {
            this.col = col;
            this.newVal = newVal;
            this.colName = colName;
        }
    }


    private Connection db;
    private String tableName;
    private SqlType primaryKeyType;
    private String primaryKeyColumn;
    private SqlType authKeyType;
    private String authKeyColumn;
    private HashMap<String, WritableColumn> wColumns = new HashMap<>();
    private HashMap<String, SqlType> rColumns = new HashMap<>();
    private HashMap<String, SqlType> fkColumns = new HashMap<>();

    private Flag flag = Flag.OK;
    private String validationResult;
    private String errorColumn;

    public enum Flag {
        OK,
        RECORD_NOT_FOUND,
        INTERNAL_ERROR,
        VALUE_PARSE_FAILED,
        INT_OVERFLOW,
        VALIDATION_POLICY,
        UNKNOWN_JSON_KEY,
        JSON_VALUE_EXPECTED,
        TOO_MUCH_JSON;

        public boolean bad() {
            return (this != OK);
        }
    }

    public JsonToDatabaseMapper(Connection db, String tableName, SqlType primaryKeyType, String primaryKeyColumn) {
        this.db = db;
        this.tableName = tableName;
        this.primaryKeyType = primaryKeyType;
        this.primaryKeyColumn = primaryKeyColumn;
    }

    private Flag setErrorState(Flag flag, String validationResult, String errorColumn) {
        this.validationResult = validationResult;
        this.errorColumn = errorColumn;
        return (this.flag = flag);
    }

    public Flag getErrorFlag() {
        return flag;
    }

    public String getValidationError() {
        return validationResult;
    }

    public String getErrorColumn() {
        return errorColumn;
    }

    public String explainError() {
        switch (flag) {
            case OK:
                return "Success";
            case RECORD_NOT_FOUND:
                return "Specified record not found, or not accessible to this user";
            case INTERNAL_ERROR:
                return "Internal server error";
            case VALUE_PARSE_FAILED:
                return "Could not parse the value for member '" + errorColumn + "' Check that it is the correct type";
            case INT_OVERFLOW:
                return "Integer value for key '" + errorColumn + "' too large";
            case VALIDATION_POLICY:
                return "Validation failed for key '" + errorColumn + "': " + validationResult;
            case UNKNOWN_JSON_KEY:
                return "Unrecognised key: '" + errorColumn + "'";
            case JSON_VALUE_EXPECTED:
                return "Expected a value for key '" + errorColumn + "' - value not found or couldn't be converted";
            case TOO_MUCH_JSON:
                return "Request complexity / length limits exceeded";
            default:
                return "Unknown error";
        }
    }

    public JSONObject fetch(Object primaryKeyValue) {
        flag = Flag.OK;

        matchingOrThrowIllegalArg(primaryKeyValue, primaryKeyType);

        StringBuilder stmtString = new StringBuilder();
        stmtString.append(
                "SELECT * FROM "
        );
        stmtString.append(
                tableName
        );
        stmtString.append(
                "WHERE "
        );
        stmtString.append(
                primaryKeyColumn
        );
        stmtString.append(
                " = ?"
        );

        try {
            PreparedStatement stmt = db.prepareStatement(stmtString.toString());
            set(stmt, 1, primaryKeyType, primaryKeyValue);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                flag = Flag.RECORD_NOT_FOUND;
                return null;
            }

            return fromResultSetRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            flag = Flag.INTERNAL_ERROR;
            return null;
        }
    }

    public JSONObject fromResultSetRow(ResultSet rs) {
        flag = Flag.OK;
        JSONObject json = new JSONObject();

        try {
            for (Map.Entry<String, WritableColumn> e : wColumns.entrySet()) {
                String name = e.getKey();
                WritableColumn col = e.getValue();
                Object val = fetch(rs, name, col.type);
                json.put(name, val);
            }

            for (Map.Entry<String, SqlType> e : rColumns.entrySet()) {
                String name = e.getKey();
                SqlType type = e.getValue();
                Object val = fetch(rs, name, type);
                json.put(name, val);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            flag = Flag.INTERNAL_ERROR;
            return null;
        }

        return json;
    }

    public JSONArray fromResultSet(ResultSet rs) {
        flag = Flag.OK;
        try {
            JSONArray array = new JSONArray();

            while (rs.next()) {
                JSONObject result = fromResultSetRow(rs);
                if (result == null)
                    return null;

                array.put(result);
            }

            return array;
        } catch (SQLException e) {
            e.printStackTrace();
            flag = Flag.INTERNAL_ERROR;
            return null;
        }
    }

    public Flag create(JSONObject json) {
        return create(json, null);
    }

    public Flag create(JSONObject json, Object authKeyVal) {
        matchingOrThrowIllegalArg(authKeyVal, authKeyType);

        json.remove(primaryKeyColumn);
        json.remove(authKeyColumn);

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(tableName);
        query.append(" (");
        query.append(authKeyColumn);
        query.append(", ");

        ArrayList<ColumnEdit> members = new ArrayList<>(json.length());

        for (Map.Entry<String, WritableColumn> e: wColumns.entrySet()) {
            String colName = e.getKey();
            WritableColumn col = e.getValue();
            Optional<Object> optVal = coerceJsonMember(json, colName, col.type);
            if (optVal.isEmpty())
                return setErrorState(Flag.JSON_VALUE_EXPECTED, null, colName);

            Object val = optVal.get();
            String complaint = validate(val, col.validationPolicy, col.type);
            if (complaint != null)
                return setErrorState(flag, complaint, colName);

            members.add(new ColumnEdit(colName, col, val));
            query.append(colName);
            query.append(", ");
            json.remove(colName);
        }

        for (Map.Entry<String, SqlType> e: fkColumns.entrySet()) {
            String colName = e.getKey();
            SqlType colType = e.getValue();
            Optional<Object> optVal = coerceJsonMember(json, colName, colType);
            if (optVal.isEmpty())
                return setErrorState(Flag.JSON_VALUE_EXPECTED, null, colName);

            Object val = optVal.get();
            members.add(new ColumnEdit(colName, new WritableColumn(colType, ValidationPolicy.acceptAll()), val));
            query.append(colName);
            query.append(", ");
            json.remove(colName);
        }

        if (json.length() > 0)
            return setErrorState(Flag.UNKNOWN_JSON_KEY, null, json.keySet().iterator().next());

        query.delete(query.length() - 2, query.length()); //remove ", "
        query.append(") VALUES (?, ");
        for (int i = 0; i < members.size(); i++)
            query.append("?, ");

        query.delete(query.length() - 2, query.length()); //remove ", "
        query.append(")");

        int result = 0;
        try {
            PreparedStatement stmt = db.prepareStatement(query.toString());
            set(stmt, 1, authKeyType, authKeyVal);
            for (int i = 0; i < members.size(); i++)
                set(stmt, i + 2, members.get(i).col.type, members.get(i).newVal);

            result = stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return setErrorState(Flag.INTERNAL_ERROR, null, null);
        }

        if (result < 1)
            return setErrorState(Flag.INTERNAL_ERROR, null, null);

        return setErrorState(Flag.OK, null, null);
    }

    public Flag patch(JSONObject json) {
        return patch(json, null);
    }

    public Flag patch(JSONObject json, Object authKeyVal) {
        ArrayList<ColumnEdit> edits = new ArrayList<>(json.length());

        Optional<Object> primaryKeyOpt = coerceJsonMember(json, primaryKeyColumn, primaryKeyType);
        if (primaryKeyOpt.isEmpty())
            return flag;

        Object primaryKeyVal = primaryKeyOpt.get();
        json.remove(primaryKeyColumn);

        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            WritableColumn col = wColumns.get(key);
            if (col != null) {
                Optional<Object> converted = coerceJsonMember(json, key, col.type);
                if (converted.isEmpty())
                    return flag;

                edits.add(new ColumnEdit(key, col, converted.get()));
            } else {
                return setErrorState(Flag.UNKNOWN_JSON_KEY, null, key);
            }
        }

        StringBuilder query = new StringBuilder();
        query.append("UPDATE ");
        query.append(tableName);
        query.append("\n");
        query.append("SET ");

        for (ColumnEdit edit : edits) {
            String complaint = validate(edit.newVal, edit.col.validationPolicy, edit.col.type);
            if (complaint != VALIDATION_OK) {
                return setErrorState(Flag.VALIDATION_POLICY, complaint, edit.colName);
            }

            query.append(edit.colName);
            query.append(" = ?,\n");
        }

        query.delete(query.length() - 2, query.length()); //Remove "\n", ","
        query.append("\n WHERE ");
        query.append(primaryKeyColumn);
        query.append(" = ?");

        if (authKeyVal != null) {
            if (authKeyColumn != null) {
                if (authKeyType.checkJavaType(authKeyVal)) {
                    query.append(" AND ");
                    query.append(authKeyColumn);
                    query.append(" = ?");
                } else {
                    throw new IllegalArgumentException("Unexpected auth key type: expected " +
                            primaryKeyType.getJavaType() + ", got " + authKeyVal.getClass());
                }
            } else {
                throw new UnsupportedOperationException(
                        "Auth key value supplied, but no auth key column has been defined");
            }
        }

        int result = 0;
        try {
            PreparedStatement stmt = db.prepareStatement(query.toString());

            {
                int i = 0;
                for (; i < edits.size(); i++)
                    set(stmt, i + 1, edits.get(i).col.type, edits.get(i).newVal);

                set(stmt, i + 1, primaryKeyType, primaryKeyVal);

                if (authKeyVal != null)
                    set(stmt, i + 2, authKeyType, authKeyVal);
            }

            result = stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return setErrorState(Flag.INTERNAL_ERROR, null, null);
        }

        if (result < 1)
            return setErrorState(Flag.RECORD_NOT_FOUND, null, null);

        return setErrorState(Flag.OK, null, null);
    }

    public Flag delete(JSONObject idSpecifier) {
        return delete(idSpecifier, null);
    }

    public Flag delete(JSONObject idSpecifier, Object authKeyVal) {
        Optional<Object> primaryKeyValOpt = coerceJsonMember(idSpecifier, primaryKeyColumn, primaryKeyType);
        if (primaryKeyValOpt.isEmpty())
            return flag;

        return delete(primaryKeyValOpt.get(), authKeyVal);
    }

    public Flag delete(Object primaryKeyVal) {
        return delete(primaryKeyVal, null);
    }

    public Flag delete(Object primaryKeyVal, Object authKeyVal) {
        matchingOrThrowIllegalArg(primaryKeyVal, primaryKeyType);

        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append(tableName);
        query.append(" WHERE ");
        query.append(primaryKeyColumn);
        query.append(" = ?");
        if (authKeyVal != null) {
            if (authKeyColumn != null) {
                if (authKeyType.checkJavaType(authKeyVal)) {
                    query.append(" AND ");
                    query.append(authKeyColumn);
                    query.append(" = ?");
                } else {
                    throw new IllegalArgumentException("Unexpected auth key type: expected " +
                            primaryKeyType.getJavaType() + ", got " + authKeyVal.getClass());
                }
            } else {
                throw new UnsupportedOperationException(
                        "Auth key value supplied, but no auth key column has been defined");
            }
        }

        try {
            PreparedStatement stmt = db.prepareStatement(query.toString());
            set(stmt, 1, primaryKeyType, primaryKeyVal);
            if (authKeyVal != null)
                set(stmt, 2, authKeyType, authKeyVal);

            if (stmt.executeUpdate() < 1) {
                return setErrorState(Flag.RECORD_NOT_FOUND, null, null);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return setErrorState(Flag.INTERNAL_ERROR, null, null);
        }

        return setErrorState(Flag.OK, null, null);
    }

    private String validate(Object val, ValidationPolicy<?> vp, SqlType type) {
        matchingOrThrowIllegalArg(val, type);

        switch (type) {
            case INTEGER:
                return ((ValidationPolicy<Integer>) vp).evaluate((Integer) val);
            case LONG:
                return ((ValidationPolicy<Long>) vp).evaluate((Long) val);
            case STRING:
                return ((ValidationPolicy<String>) vp).evaluate((String) val);
            case TIMESTAMP:
                return ((ValidationPolicy<Instant>) vp).evaluate((Instant) val);
            default:
                throw new RuntimeException("umm what");
        }
    }

    private Optional<Object> coerceJsonMember(JSONObject json, String key, SqlType type) {
        switch (type) {
            case INTEGER:
                Number intAsNumber = json.optNumber(key);
                if (intAsNumber == null) {
                    setErrorState(Flag.VALUE_PARSE_FAILED, null, key);
                    return Optional.empty();
                } else if (intAsNumber.longValue() > Integer.MAX_VALUE || intAsNumber.longValue() < Integer.MIN_VALUE) {
                    setErrorState(Flag.INT_OVERFLOW, null, key);
                    return Optional.empty();
                }

                return Optional.of(intAsNumber.intValue());
            case LONG:
                Number longAsNumber = json.optNumber(key);
                if (longAsNumber == null) {
                    setErrorState(Flag.VALUE_PARSE_FAILED, null, key);
                    return Optional.empty();
                }

                return Optional.of(longAsNumber.longValue());
            case STRING:
                String s = json.optString(key, null);
                if (s == null) {
                    setErrorState(Flag.VALUE_PARSE_FAILED, null, key);
                    return Optional.empty();
                }

                return Optional.of(s);
            case TIMESTAMP:
                String timeString = json.optString(key, null);
                if (timeString == null) {
                    setErrorState(Flag.VALUE_PARSE_FAILED, null, key);
                    return Optional.empty();
                }

                Instant time = Instant.parse(timeString);
                if (time == null) {
                    setErrorState(Flag.VALUE_PARSE_FAILED, null, key);
                    return Optional.empty();
                }

                return Optional.of(time);
            default:
                throw new RuntimeException("Umm what");
        }
    }

    private void set(PreparedStatement stmt, int index, SqlType type, Object value) throws SQLException {
        matchingOrThrowIllegalArg(value, type);

        switch (type) {
            case INTEGER:
                Integer i = (Integer) value;
                stmt.setInt(index, i);
                break;
            case LONG:
                Long l = (Long) value;
                stmt.setLong(index, l);
                break;
            case STRING:
                String s = (String) value;
                stmt.setString(index, s);
                break;
            case TIMESTAMP:
                Instant instant = (Instant) value;
                stmt.setTimestamp(index, Timestamp.from(instant));
                break;
            default:
                throw new RuntimeException("umm what");
        }
    }

    private Object fetch(ResultSet rs, String col, SqlType type) throws SQLException {
        switch (type) {
            case INTEGER:
                return rs.getInt(col);
            case LONG:
                return rs.getLong(col);
            case STRING:
                return rs.getString(col);
            case TIMESTAMP:
                return Instant.ofEpochMilli(rs.getTimestamp(col).getTime());
            default:
                throw new RuntimeException("Umm what");
        }
    }

    public void matchingOrThrowIllegalArg(Object obj, SqlType type) {
        if (!type.checkJavaType(obj))
            throw new IllegalArgumentException("Bad primary key type: expected " + type.getJavaType()
                    + " , got " + obj.getClass());
    }

    public void setAuthKey(String columnName, SqlType type) {
        authKeyColumn = columnName;
        authKeyType = type;
    }

    public void defineForeignKeyColumn(String columnName, SqlType type) {
        fkColumns.put(columnName, type);
    }

    public void defineWriteableIntegerColumn(String columnName, ValidationPolicy<Integer> vp) {
        wColumns.put(columnName, new WritableColumn(SqlType.INTEGER, vp));
    }

    public void defineReadOnlyIntegerColumn(String columnName) {
        rColumns.put(columnName, SqlType.INTEGER);
    }

    public void defineWriteableLongColumn(String columnName, ValidationPolicy<Long> vp) {
        wColumns.put(columnName, new WritableColumn(SqlType.LONG, vp));
    }

    public void defineReadOnlyLongColumn(String columnName) {
        rColumns.put(columnName, SqlType.LONG);
    }

    public void defineWriteableStringColumn(String columnName, ValidationPolicy<String> vp) {
        wColumns.put(columnName, new WritableColumn(SqlType.STRING, vp));
    }

    public void defineReadOnlyStringColumn(String columnName) {
        rColumns.put(columnName, SqlType.STRING);
    }

    public void defineWriteableTimestampColumn(String columnName, ValidationPolicy<Instant> vp) {
        wColumns.put(columnName, new WritableColumn(SqlType.TIMESTAMP, vp));
    }

    public void defineReadOnlyTimestampColumn(String columnName) {
        rColumns.put(columnName, SqlType.TIMESTAMP);
    }
    /*
    mapper.defineWriteableTimestampColumn("time", (Instant value) -> {
        Date date = new Date(value);
        if (date < new Date())
            return "Date is before now!";
        else
            return JsonToDatabaseMapper.VALIDATION_OK;
    });
    */
}
