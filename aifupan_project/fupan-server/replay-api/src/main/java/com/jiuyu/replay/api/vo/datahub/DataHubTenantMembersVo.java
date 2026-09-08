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
 * Data Hub 租户成员名单响应
 */
@Data
@Schema(description = "Data Hub 租户成员名单响应")
public class DataHubTenantMembersVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 每租户一项（与入参 tenantIds 一一对应）
     */
    @Schema(description = "租户成员列表")
    private List<Item> items = new ArrayList<>();

    /**
     * 单租户成员名单
     */
    @Data
    @Schema(description = "单租户成员名单")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 租户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "租户ID")
        private Long tenantId;

        /**
         * 缺失原因：null=命中 / NOT_FOUND=租户不存在
         */
        @Schema(description = "缺失原因 null / NOT_FOUND")
        private String missingReason;

        /**
         * 成员列表（含当前成员与已解绑成员）
         */
        @Schema(description = "成员列表")
        private List<Member> members = new ArrayList<>();
    }

    /**
     * 成员项
     */
    @Data
    @Schema(description = "成员项")
    public static class Member implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "用户ID")
        private Long userId;

        /**
         * 角色 MAIN主账号 / SUB子账号
         */
        @Schema(description = "角色 MAIN / SUB")
        private String role;

        /**
         * 状态 NORMAL正常 / FROZEN冻结 / DELETED已删除 / UNBOUND已解绑
         */
        @Schema(description = "状态 NORMAL / FROZEN / DELETED / UNBOUND")
        private String status;

        /**
         * 昵称
         */
        @Schema(description = "昵称")
        private String displayName;

        /**
         * 脱敏手机号
         */
        @Schema(description = "脱敏手机号")
        private String maskedPhone;

        /**
         * 是否登录过
         */
        @Schema(description = "是否登录过")
        private Boolean hasLoggedIn;

        /**
         * 注册时间
         */
        @Schema(description = "注册时间")
        private Date registeredAt;

        /**
         * 加入租户时间（主账号=注册时间；子账号=绑定时间，早期数据可能为空）
         */
        @Schema(description = "加入租户时间")
        private Date joinedAt;

        /**
         * 离开租户时间（仅已解绑成员）
         */
        @Schema(description = "离开租户时间")
        private Date leftAt;
    }
}
