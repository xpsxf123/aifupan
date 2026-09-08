package com.jiuyu.replay.ai.vo;

import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/19 15:39
 */
@Data
@Schema(description = "数据诊断配置信息项")
public class DataDiagnosisConfigVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "基础设置")
    private BasicSettingsVo basicSettingsVo;

    @Schema(description = "行业id")
    private Long tradeId;

    @Schema(description = "模型id")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "是否有数据截图 0没有，1有")
    private Integer hasDataScreenshot;

    @Schema(description = "是否有数据看版 0没有，1有")
    private Integer hasBoard;

    @Schema(description = "是否有数据截图 0没有，1有")
    private Integer selectDataScreenshot;

    @Schema(description = "是否有数据看版 0没有，1有")
    private Integer selectBoard;

    @Schema(description = "问题id")
    private Long cueWordsId;

    @Schema(description = "新问题id")
    private Long newCueWordsId;

    @Schema(description = "生成状态 null: 没有生成过， 0准备开始，1进行中，2已完成， 3失败")
    private Integer qaStatus;

    @Schema(description = "错误内容")
    private String errorContent;

    @Schema(description = "来源id")
    @NotNull(message = "来源id不能为空")
    private String sourceId;

    @Schema(description = "来源类型 0：视频，1：文件，")
    @NotNull(message = "来源类型不能为空")
    private Integer sourceType;

    @Schema(description = "平台类型 0：抖音 1：快手 2：视频号")
    private Integer platform;
}
