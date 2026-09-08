package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 算力汇总增量水位线
 * 记录已处理到的 tb_user_property_details 最大 id，保证增量累加恰好一次
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Data
@TableName("tb_power_rollup_watermark")
public class PowerRollupWatermarkEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 业务标识，本轮固定 aiTokenNum
     */
    private String bizCode;

    /**
     * 已处理到的 tb_user_property_details 最大id(含)
     */
    private Long lastDetailId;

    /**
     * 是否已删除 0否 1是
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 最后推进时间
     */
    private Date updateDate;
}
