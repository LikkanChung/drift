package sleepapp.backend.auth;

public class LoginResult {
    private Status status;
    private String token;

    protected static LoginResult UNRECOGNISED = new LoginResult(Status.NOT_RECOGNISED, null);

    protected LoginResult(Status status, String token) {
        this.status = status;
        this.token = token;
    }

    public Status getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public enum Status {
        SUCCESS,
        NOT_RECOGNISED;

        public String getReason() {
            switch (this) {
                case SUCCESS:
                    return "Success";
                case NOT_RECOGNISED:
                    return "Username and password combination not recognised";
                default:
                    return "Unknown Error";
            }
        }
    }
}
