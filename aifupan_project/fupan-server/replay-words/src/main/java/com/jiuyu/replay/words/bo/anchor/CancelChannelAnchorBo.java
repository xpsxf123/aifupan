package com.jiuyu.replay.words.bo.anchor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 视频号取消授权
 *
 * @author HeHui
 * @date 2025-11-11 11:26
 */
@Getter
@Setter
public class CancelChannelAnchorBo {

    /**
     * 视频号授权信息ID
     */
    @NotBlank(message = "视频号授权信息ID不能为空")
    private String authorizerInfoId;

    /**
     * 用户ID
     */
    @NotEmpty(message = "用户ID不能为空")
    private List<Long> userIds;
}
