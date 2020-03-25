package sleepapp.backend.auth;

public class UserAuth {
    public static long ID_UNAUTHENTICATED = 0;

    public static int ACCESS_LEVEL_NONE = 0;
    public static int ACCESS_LEVEL_READ_WRITE = 5;
    public static int ACCESS_LEVEL_ACCOUNT = 10;

    protected static UserAuth EXPIRED_TOKEN = new UserAuth(true);
    protected static UserAuth INVALID_TOKEN = new UserAuth(false);

    private long userid = ID_UNAUTHENTICATED;
    private int accessLevel = ACCESS_LEVEL_NONE;
    private boolean expired = false;

    private UserAuth(boolean expired) {
        this.expired = expired;
    }

    protected UserAuth(long userid, int accessLevel) {
        this.userid = userid;
        this.accessLevel = accessLevel;
    }

    public boolean success() {
        return (userid != ID_UNAUTHENTICATED);
    }

    public boolean hasTokenExpired() {
        return expired;
    }

    public Long getUserId() {
        if (userid != ID_UNAUTHENTICATED) {
            return userid;
        } else {
            return null;
        }
    }

    public Integer getAccessLevel() {
        if (userid != ID_UNAUTHENTICATED) {
            return accessLevel;
        } else {
            return null;
        }
    }
}
