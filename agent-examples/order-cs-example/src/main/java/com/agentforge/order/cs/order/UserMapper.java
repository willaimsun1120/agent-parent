package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 MyBatis-Plus Mapper。
 *
 * <p>提供 {@link User} 的 CRUD 能力，供模拟下单与用户列表查询使用。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
