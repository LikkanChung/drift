package sleepapp.backend.datamap;

public interface ValidationPolicy <T> {

    public static final String OK = null;
    public static <T> ValidationPolicy<T> acceptAll() {
        return (T value) -> {
            return OK;
        };
    }

    public String evaluate(T value);

}
