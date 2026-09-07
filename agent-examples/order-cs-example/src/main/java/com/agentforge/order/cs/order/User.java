package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 用户实体，映射 MySQL {@code users} 表。
 *
 * <p>Demo 模拟用户数据，供下单与权益名称推断使用，不参与 Agent SPI 直接交互。
 */
@TableName("users")
public class User {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务用户编号，如 U1001 */
    private String userNo;
    /** 用户昵称 */
    private String nickname;
    /** 会员等级名称，如 VIP、SVIP，影响权益类型 */
    private String levelName;
    /** 记录创建时间 */
    private LocalDateTime createdAt;

    /** @return 主键 */
    public Long getId() { return id; }
    /** @return 业务用户编号 */
    public String getUserNo() { return userNo; }
    /** @return 用户昵称 */
    public String getNickname() { return nickname; }
    /** @return 会员等级名称 */
    public String getLevelName() { return levelName; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
}
