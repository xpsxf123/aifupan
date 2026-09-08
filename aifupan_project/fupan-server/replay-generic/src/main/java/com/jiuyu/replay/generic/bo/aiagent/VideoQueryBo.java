package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频单标识查询入参（音频段落 / 数据看板共用）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VideoQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 视频唯一标识
     */
    @NotBlank(message = "videoId不能为空")
    private String videoId;
}
