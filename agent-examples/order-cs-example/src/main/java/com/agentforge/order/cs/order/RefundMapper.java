package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 退款表 MyBatis-Plus Mapper。
 *
 * <p>提供 {@link Refund} 的 CRUD 能力，供退款查询与
 * {@link com.agentforge.order.cs.action.SubmitRefundActionExecutor} 写入使用。
 */
public interface RefundMapper extends BaseMapper<Refund> {
}
