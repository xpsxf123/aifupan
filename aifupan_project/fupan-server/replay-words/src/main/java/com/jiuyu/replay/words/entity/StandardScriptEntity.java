package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标准直播稿已确认表
 */
@Data
@TableName("tb_standard_script")
public class StandardScriptEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 租户ID
     */
    private Long tenantId;
    /**
     * 确认者用户ID
     */
    private Long userId;
    /**
     * 主播唯一标识（secUid）；标准稿按 (tenant_id, user_id, sec_uid) 联合定位
     */
    private String secUid;
    /**
     * 话术模式 0非循环 1循环
     */
    private Integer speechMode;
    /**
     * 语速 字/分钟 100-500
     */
    private Integer speechSpeed;
    /**
     * 循环话术预估时长分钟
     */
    private Integer cycleDurationMinutes;
    /**
     * 参考直播脚本原文
     */
    private String referenceScript;
    /**
     * 时间轴 JSON [{"timeRange","title","content"}]
     */
    private String timeAxisScript;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 最后修改时间
     */
    private Date updateDate;
    /**
     * 是否已删除 0否 1是
     */
    private Integer isDeleted;
}
