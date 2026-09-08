package com.jiuyu.replay.api.vo.datahub;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Hub 成员登录状态响应
 */
@Data
@Schema(description = "Data Hub 成员登录状态响应")
public class DataHubUserLoginStatsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实际生效的统计窗口起（未传窗口时为空）
     */
    @Schema(description = "实际生效的统计窗口起")
    private Date windowStart;

    /**
     * 实际生效的统计窗口止
     */
    @Schema(description = "实际生效的统计窗口止")
    private Date windowEnd;

    /**
     * 每用户一项（与入参 userIds 一一对应）
     */
    @Schema(description = "登录状态列表")
    private List<Item> items = new ArrayList<>();

    /**
     * 单用户登录状态
     */
    @Data
    @Schema(description = "单用户登录状态")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "用户ID")
        private Long userId;

        /**
         * 缺失原因：null=命中 / NOT_FOUND=用户不存在
         */
        @Schema(description = "缺失原因 null / NOT_FOUND")
        private String missingReason;

        /**
         * 是否登录过（历史任一次成功登录）
         */
        @Schema(description = "是否登录过")
        private Boolean hasLoggedIn;

        /**
         * 首次成功登录时间
         */
        @Schema(description = "首次成功登录时间")
        private Date firstLoginAt;

        /**
         * 最近一次成功登录时间
         */
        @Schema(description = "最近一次成功登录时间")
        private Date lastLoginAt;

        /**
         * 累计成功登录次数
         */
        @Schema(description = "累计成功登录次数")
        private Long totalLoginCount;

        /**
         * 窗口内成功登录次数（未传窗口时为空）
         */
        @Schema(description = "窗口内成功登录次数")
        private Long windowLoginCount;
    }
}
