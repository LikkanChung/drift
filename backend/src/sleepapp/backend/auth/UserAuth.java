package sleepapp.backend.auth;

import java.time.Duration;

public class UserAuth {
    public static long ID_UNAUTHENTICATED = 0;

    public static final int ACCESS_LEVEL_NONE = 0;
    public static final int ACCESS_LEVEL_READ_WRITE = 5;
    public static final int ACCESS_LEVEL_ACCOUNT = 10;

    protected static UserAuth RESULT_EXPIRED_TOKEN = new UserAuth(Reason.EXPIRED_TOKEN);
    protected static UserAuth RESULT_INVALID_TOKEN = new UserAuth(Reason.INVALID_TOKEN);
    protected static UserAuth RESULT_NO_TOKEN = new UserAuth(Reason.NO_TOKEN);
    protected static UserAuth RESULT_UNKNOWN_ERROR = new UserAuth(Reason.UNKNOWN_ERROR);

    private long userid = ID_UNAUTHENTICATED;
    private int accessLevel = ACCESS_LEVEL_NONE;
    private Reason reason;

    public static boolean validAccessLevel(int al) {
        return (al == ACCESS_LEVEL_NONE || al == ACCESS_LEVEL_READ_WRITE || al == ACCESS_LEVEL_ACCOUNT);
    }

    public static Duration defaultValidityPeriod(int accessLevel) {
        switch(accessLevel) {
            case ACCESS_LEVEL_NONE:
                return Duration.ZERO;
            case ACCESS_LEVEL_ACCOUNT:
                return Duration.ofMinutes(10); //Non-debug value: Duration.ofHours(1)
            case ACCESS_LEVEL_READ_WRITE:
                return Duration.ofHours(1); //Non-debug value: Duration.ofDays(60)
            default:
                throw new IllegalArgumentException("Invalid access level");
        }
    }

    public static enum Reason {
        EXPIRED_TOKEN,
        INVALID_TOKEN,
        NO_TOKEN,
        UNKNOWN_ERROR;
    }

    private UserAuth(Reason failReason) {
        this.reason = failReason;
    }

    protected UserAuth(long userid, int accessLevel) {
        this.userid = userid;
        this.accessLevel = accessLevel;
    }

    public boolean success() {
        return (userid != ID_UNAUTHENTICATED);
    }

    public Reason getReason() {
        return reason;
    }

    public Long getUserId() {
        if (userid != ID_UNAUTHENTICATED) {
            return userid;
        } else {
            return null;
        }
    }

    public int getAccessLevel() {
        return accessLevel;
    }
}
