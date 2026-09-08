package com.jiuyu.replay.generic.vo.words;

import com.jiuyu.replay.generic.dto.words.BasicSettingsBaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author ：lujie
 * @description：基础设置视图对象
 * @date ：2025/1/7
 */
@Data
@Schema(description = "基础设置视图对象")
public class BasicSettingsVo extends BasicSettingsBaseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "来源类型 0：主播，1：视频，2：文件，")
    private Integer sourceType;

    @Schema(description = "用户表id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createDate;

    @Schema(description = "更新时间")
    private LocalDateTime updateDate;
}