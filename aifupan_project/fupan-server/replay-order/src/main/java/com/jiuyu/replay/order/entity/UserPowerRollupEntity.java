package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户算力消耗汇总表
 * 由 XXL-Job timingUpdatePowerConsume 增量累加刷新，在线列表只读不写
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Data
@TableName("tb_user_power_rollup")
public class UserPowerRollupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 用户ID(实际消费者)
     */
    private Long userId;

    /**
     * 租户id(tb_tenant.id，主账号拥有的租户)
     */
    private Long tenantId;

    /**
     * 用户累计算力消耗(token数, 净额 = 扣减 - 失败回退)
     */
    private Long userPowerConsume;

    /**
     * 是否已删除 0否 1是
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 最后刷新时间
     */
    private Date updateDate;
}
