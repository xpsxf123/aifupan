package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改视频云空间重命名参数")
public class UpdateVideoCloudRenameBo {

    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String videoId;

    /**
     * 云空间重命名
     */
    @Schema(description = "云空间重命名")
    private String cloudRename;
}
