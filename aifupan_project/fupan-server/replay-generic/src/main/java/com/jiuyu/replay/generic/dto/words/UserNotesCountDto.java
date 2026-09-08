package com.jiuyu.replay.generic.dto.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户小结数量DTO
 *
 * @author AI Assistant
 */
@Data
@Schema(description = "用户小结数量DTO")
public class UserNotesCountDto {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 小结数量
     */
    @Schema(description = "小结数量")
    private Integer notesCount;
}
