package sleepapp.backend.auth;

import java.time.Instant;

public class LoginResult {
    private Status status;
    private String token;
    private Long uid;
    private Instant begins;
    private Instant expires;

    protected static LoginResult UNRECOGNISED = new LoginResult(Status.NOT_RECOGNISED, null);
    protected static LoginResult DB_ERROR_HAPPENED = new LoginResult(Status.DB_ERROR, null);

    private LoginResult(Status status, String token) {
        this.status = status;
        this.token = token;

        begins = Instant.EPOCH;
        expires = Instant.EPOCH;
    }

    protected LoginResult(Status status, long uid, String token, Instant begins, Instant expires) {
        this.status = status;
        this.token = token;
        this.uid = uid;
        this.begins = begins;
        this.expires = expires;
    }

    public Status getStatus() {
        return status;
    }

    public Instant begins() {
        return begins;
    }

    public Instant expires() {
        return expires;
    }

    public String getToken() {
        return token;
    }

    public Long getUid() { return uid; }

    public enum Status {
        SUCCESS,
        NOT_RECOGNISED,
        DB_ERROR;

        public String getReason() {
            switch (this) {
                case SUCCESS:
                    return "Success";
                case NOT_RECOGNISED:
                    return "Username/uid and password combination not recognised";
                default:
                    return "Unknown Error";
            }
        }
    }
}
