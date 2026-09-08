package com.jiuyu.replay.generic.vo.power;

import com.jiuyu.replay.generic.vo.order.UserResourceConsumptionVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class SubUserListVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 登录账号
     */
    @Schema(description = "登录账号")
    private String username;
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;
    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    @Schema(description = "冻结状态 0：未冻结 1：已冻结")
    private Integer status;
    /**
     * 上级用户id
     */
    @Schema(description = "上级用户id")
    private Long parentId;
    /**
     * 用户类型 0：普通用户 1：后台管理员 2：子账号
     */
    @Schema(description = "用户类型 0：普通用户 1：后台管理员 2：子账号")
    private Integer userType;
    /**
     * 当前激活的租户id
     */
    @Schema(description = "当前激活的租户id")
    private Long activeTenantId;
    /**
     * 邀请链接的code
     */
    @Schema(description = "邀请链接的code")
    private String inviteUrlCode;

    /**
     * 主播数量
     */
    @Schema(description = "主播数量")
    private Integer anchorCount;

    /**
     * 本月录制视频数量
     */
    @Schema(description = "本月录制视频数量")
    private Integer videoCount;

    /**
     * 昨日录制视频数量
     */
    @Schema(description = "昨日录制视频数量")
    private Integer yesterdayVideoCount;

    /**
     * 昨日小结数量
     */
    @Schema(description = "昨日小结数量")
    private Integer yesterdayNotesCount;

    /**
     * 昨日资源消耗列表
     */
    @Schema(description = "昨日资源消耗列表")
    private List<UserResourceConsumptionVo> yesterdayResourceConsumption;

    /**
     * 本月资源消耗列表
     */
    @Schema(description = "本月资源消耗列表")
    private List<UserResourceConsumptionVo> monthlyResourceConsumption;
}
