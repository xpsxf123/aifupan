package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 批量短视频明细入参（按 tb_video_info 主键 id 直查完整明细，全库、不校验归属）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VideoDetailBatchBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * tb_video_info 主键 id 集合，支持 in（1~500 个）
     */
    @NotEmpty(message = "videoIdList 不能为空")
    @Size(max = 500, message = "videoIdList 最多 500 个")
    private List<Long> videoIdList;

    /**
     * 是否加载原文/AI 音频转文字，默认 false
     */
    private Boolean withAudioText = false;
}
