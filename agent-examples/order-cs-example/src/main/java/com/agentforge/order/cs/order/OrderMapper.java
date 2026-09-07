package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 订单表 MyBatis-Plus Mapper。
 *
 * <p>提供 {@link Order} 的 CRUD 能力，由 {@link OrderService}、{@link OrderDetailLoader}
 * 及模拟服务调用，与平台 SPI 无直接关系。
 */
public interface OrderMapper extends BaseMapper<Order> {
}
