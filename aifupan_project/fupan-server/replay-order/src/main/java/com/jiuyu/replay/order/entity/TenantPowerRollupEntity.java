package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户算力消耗汇总表
 * 租户标识 = tenant_id(tb_tenant.id)，由主账号(台账 parent_user_id 归一化) JOIN tb_tenant 取得
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Data
@TableName("tb_tenant_power_rollup")
public class TenantPowerRollupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 租户id(tb_tenant.id)
     */
    private Long tenantId;

    /**
     * 租户全体累计算力消耗(token数, 净额 = 扣减 - 失败回退)
     */
    private Long tenantPowerConsume;

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
