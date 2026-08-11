package com.familymemory.security;

public final class UserContext {
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    public static Long require() {
        Long userId = HOLDER.get();
        if (userId == null) {
            throw new com.familymemory.common.BusinessException(401, "请先登录");
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
