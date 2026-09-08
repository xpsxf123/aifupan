package com.jiuyu.replay.power.vo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data Hub 登录聚合数据项（模块内传输对象）
 * 基于 tb_user_login_log 成功登录记录（opera_type=0 且 opera_status=0）按用户聚合
 */
@Data
@Schema(description = "Data Hub 登录聚合数据项")
public class DataHubLoginStatsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 首次成功登录时间
     */
    @Schema(description = "首次成功登录时间")
    private Date firstLoginDate;

    /**
     * 最近一次成功登录时间
     */
    @Schema(description = "最近一次成功登录时间")
    private Date lastLoginDate;

    /**
     * 成功登录次数
     */
    @Schema(description = "成功登录次数")
    private Long loginCount;
}
