package com.agentforge.hr.cs.cache;

/**
 * HR 业务 JetCache 缓存名与 TTL 常量。
 */
public final class HrCacheConstants {

    private HrCacheConstants() {
    }

    public static final String EMPLOYEE_DETAIL_KEY = "agent:employee-detail";

    public static final int EMPLOYEE_EXPIRE_SECONDS = 60;
    public static final int EMPLOYEE_LOCAL_EXPIRE_SECONDS = 30;
    public static final int EMPLOYEE_REFRESH_SECONDS = 30;
}
