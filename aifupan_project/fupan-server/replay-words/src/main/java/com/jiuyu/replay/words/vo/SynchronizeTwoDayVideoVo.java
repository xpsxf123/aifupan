package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/4 下午6:21
 */
@Data
@Schema(description = "用户的场观和累计人数")
public class SynchronizeTwoDayVideoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "以场次维度")
    private List<TotalSocketMessageVo> totalSocketMessageList;

    @Schema(description = "以视频维度")
    private List<SocketCollectMessageVo> socketCollectMessageList;
}
