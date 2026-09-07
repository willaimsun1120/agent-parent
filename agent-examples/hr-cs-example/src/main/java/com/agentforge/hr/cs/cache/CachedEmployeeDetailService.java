package com.agentforge.hr.cs.cache;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.CacheType;
import com.agentforge.agent.core.cache.AgentCacheConstants;
import com.agentforge.hr.cs.employee.EmployeeDetail;
import com.agentforge.hr.cs.employee.EmployeeDetailLoader;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class CachedEmployeeDetailService {

    private final EmployeeDetailLoader employeeDetailLoader;
    private final CachedEmployeeDetailService self;

    public CachedEmployeeDetailService(EmployeeDetailLoader employeeDetailLoader,
                                       @Lazy CachedEmployeeDetailService self) {
        this.employeeDetailLoader = employeeDetailLoader;
        this.self = self;
    }

    public EmployeeDetail getEmployeeDetail(String empNo) {
        return self.cachedGetEmployeeDetail(empNo);
    }

    @CacheInvalidate(name = HrCacheConstants.EMPLOYEE_DETAIL_KEY, key = "#empNo")
    public void evict(String empNo) {
    }

    @Cached(
        name = HrCacheConstants.EMPLOYEE_DETAIL_KEY,
        key = "#empNo",
        expire = HrCacheConstants.EMPLOYEE_EXPIRE_SECONDS,
        localExpire = HrCacheConstants.EMPLOYEE_LOCAL_EXPIRE_SECONDS,
        localLimit = AgentCacheConstants.LOCAL_LIMIT,
        timeUnit = TimeUnit.SECONDS,
        cacheType = CacheType.LOCAL,
        syncLocal = true,
        cacheNullValue = false,
        serialPolicy = AgentCacheConstants.SERIAL_POLICY
    )
    @CachePenetrationProtect(timeout = AgentCacheConstants.PENETRATION_PROTECT_SECONDS)
    @CacheRefresh(
        refresh = HrCacheConstants.EMPLOYEE_REFRESH_SECONDS,
        refreshLockTimeout = 1,
        stopRefreshAfterLastAccess = 5,
        timeUnit = TimeUnit.MINUTES
    )
    public EmployeeDetail cachedGetEmployeeDetail(String empNo) {
        return employeeDetailLoader.load(empNo);
    }
}
