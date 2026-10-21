package com.familymemory.security;

/**
 * 请求线程的当前登录用户ID上下文，请求结束后必须清理。
 */
public final class UserContext {
    /**
     * 当前请求线程用户ID，来源表：fm_user.id，请求结束必须清理。
     */
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    /**
     * 禁止实例化请求用户上下文工具类。
     */
    private UserContext() {
    }

    /**
     * 设置当前请求线程登录用户ID。
     * @param userId 当前用户ID，来源表：fm_user.id
     */
    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    /**
     * 读取当前线程用户ID，未登录返回null。
     * @return 当前请求用户ID，来源表：fm_user.id；get方法未登录时可返回null
     */
    public static Long get() {
        return HOLDER.get();
    }

    /**
     * 读取必需的登录用户ID，未登录抛出401异常。
     * @return 已登录用户ID，来源表：fm_user.id
     * @throws com.familymemory.common.BusinessException 未登录时抛出401业务异常
     */
    public static Long require() {
        Long userId = HOLDER.get();
        if (userId == null) {
            throw new com.familymemory.common.BusinessException(401, "请先登录");
        }
        return userId;
    }

    /**
     * 清理线程用户ID，防止线程复用串号。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
