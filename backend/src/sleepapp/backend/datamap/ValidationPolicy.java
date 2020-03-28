package sleepapp.backend.datamap;

public interface ValidationPolicy <T> {

    public static final String OK = null;

    public String evaluate(T value);

}
