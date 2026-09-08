package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入任务进度，供前端轮询。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "导入任务进度")
public class SeoImportProgressVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务 ID */
    @Schema(description = "任务ID")
    private String taskId;

    /** 任务状态：0 处理中 1 已完成 2 失败 */
    @Schema(description = "任务状态：0 处理中 1 已完成 2 失败")
    private Integer taskStatus;

    /** 待处理文件总数 */
    @Schema(description = "待处理文件总数")
    private Integer total;

    /** 已处理数 */
    @Schema(description = "已处理数")
    private Integer processed;

    /** 成功篇数 */
    @Schema(description = "成功篇数")
    private Integer successCount;

    /** 失败篇数 */
    @Schema(description = "失败篇数")
    private Integer failCount;

    /** 任务级失败原因（status=2 时），与单篇失败无关 */
    @Schema(description = "任务级失败原因")
    private String errorMsg;

    /** 逐篇结果。处理中时只含已完成的部分 */
    @Schema(description = "逐篇结果")
    private List<SeoImportItemVo> results = new ArrayList<>();
}
