package sleepapp.backend.datamap;

import java.time.Instant;

public enum SqlType {
    INTEGER,
    LONG,
    STRING,
    TIMESTAMP,
    BYTEA;

    public boolean checkJavaType(Object obj) {
        return getJavaType().isInstance(obj);
    }

    public Class<?> getJavaType() {
        switch(this) {
            case INTEGER:
                return Integer.class;
            case LONG:
                return Long.class;
            case STRING:
                return String.class;
            case TIMESTAMP:
                return Instant.class;
            case BYTEA:
                return byte[].class;
            default:
                throw new RuntimeException("Umm what");
        }
    }
}
