package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class SyncLocalDataToServerBo {

    /**
     * 视频id集合
     */
    @Schema(description = "视频id集合")
    private List<String> videoIds;
    /**
     * 文件id集合
     */
    @Schema(description = "文件id集合")
    private List<String> fileIds;
    /**
     * 对比id集合
     */
    @Schema(description = "对比id集合")
    private List<String> contrastIds;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
}
