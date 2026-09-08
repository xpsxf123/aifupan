package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "用户信息")
public class UserInfoVo extends UserVo {

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "套餐等级")
    private Integer packageLevel;

    @Schema(description = "有效期时间")
    private Date expirationDate;

    @Schema(description = "资源更新时间")
    private Date resourceUpdateTime;

    /**
     * logo地址
     */
    private String logoImgAddress;

    /**
     * 角色id集合
     */
    @Schema(description = "角色id集合")
    private List<Long> roleIdList;
    /**
     * 头像地址
     */
    @Schema(description = "头像地址")
    private String avatar;
    /**
     * 未脱敏的手机号
     */
    @Schema(description = "未脱敏的手机号")
    private String normalPhone;

    @Schema(description = "子账号数量")
    private Integer childAccountCount;

    @Schema(description = "子账号id集合")
    private List<UserVo> childUserList;
}
