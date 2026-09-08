package com.jiuyu.replay.words.vo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对比的看盘数据")
public class VideoDataViewingContrastVo {

    /**
     * 视频1的看盘数据
     */
    @Schema(description = "视频1的看盘数据")
    private VideoDataViewingConfuseInfoVo videoDataViewing1;
    /**
     * 视频2的看盘数据
     */
    @Schema(description = "视频2的看盘数据")
    private VideoDataViewingConfuseInfoVo videoDataViewing2;
}
