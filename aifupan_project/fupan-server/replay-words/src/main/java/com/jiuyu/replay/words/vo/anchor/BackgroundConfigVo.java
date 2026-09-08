package com.jiuyu.replay.words.vo.anchor;

import com.jiuyu.replay.generic.dto.words.BasicSettingsBaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/3 17:46
 */
@Data
public class BackgroundConfigVo extends BasicSettingsBaseDto {

    @Schema(description = "是否传入优化动作 0：不传 1：传")
    private Integer optimizeActions;

    @Schema(description = "语速")
    private Integer speechRate;

    @Schema(description = "源ID")
    private String sourceId;

    @Schema(description = "源类型 0视频，1文件")
    private Integer sourceType;

}
