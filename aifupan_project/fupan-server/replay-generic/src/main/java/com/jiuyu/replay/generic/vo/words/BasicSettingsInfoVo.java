package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：基础设置视图对象
 * @date ：2025/1/7
 */
@Data
@Schema(description = "基础设置视图对象")
public class BasicSettingsInfoVo extends BasicSettingsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否更新")
    private Boolean isUpdate;
}