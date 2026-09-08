package com.jiuyu.replay.common.bo.userGrayscale;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 16:44
 */
@Data
@Schema(description = "用户灰度表分页参数")
public class UserGrayscaleListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 关键字
     */
    @Schema(description = " 用户手机号或者昵称")
    private String keyword;

}
