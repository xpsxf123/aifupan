package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
/**
 * 用户上传视频申述
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Data
@Schema(description = "用户上传视频申述")
public class UserVideoAppealClientBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 直播间账号昵称
     */
    @Schema(description = "直播间账号昵称")
    private String liveUserName;
    /**
     * 公司名称
     */
    @Schema(description = "公司名称")
    private String companyName;
    /**
     * 联系人
     */
    @Schema(description = "联系人")
    private String contacts;
    /**
     * 手机号码
     */
    @Schema(description = "手机号码")
    private String phone;
    /**
     * 申述原因
     */
    @Schema(description = "申述原因")
    private String appealReason;
    /**
     * 申述的主播id
     */
    @Schema(description = "申述的主播id")
    private String anchorUrlId;
    /**
     * 申述的视频id
     */
    @Schema(description = "申述的视频id")
    private String anchorVideoId;
}
