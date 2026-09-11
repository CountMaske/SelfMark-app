package com.nortidart.selfmark.common.context;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    public static CurrentUser getRequired() {
        CurrentUser currentUser = CURRENT_USER.get();
        if (currentUser == null) {
            throw new IllegalStateException("Current user is not available");
        }
        return currentUser;
    }

    public static Long getUserId() {
        return getRequired().userId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
