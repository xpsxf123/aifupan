package com.jiuyu.replay.generic.dto.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户视频数量DTO
 *
 * @author AI Assistant
 */
@Data
@Schema(description = "用户视频数量DTO")
public class UserVideoCountDto {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 本月录制视频数量
     */
    @Schema(description = "本月录制视频数量")
    private Integer videoCount;
}
