package com.jiuyu.replay.common.vo.userGrayscale;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 16:53
 */
@Data
@Schema(description = "用户灰度")
public class UserGrayscaleVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;

    /**
     * 版本id
     */
    @Schema(description = "版本id")
    private Long versionId;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 用户手机号
     */
    @Schema(description = "用户手机号")
    private String phone;
    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickName;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Long updateDate;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Long createDate;
}
