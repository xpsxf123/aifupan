package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 提交导入后的返回值：只回 taskId，前端据此轮询。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "导入任务提交结果")
public class SeoImportTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务 ID，前端据此轮询 importProgress */
    @Schema(description = "任务ID")
    private String taskId;

    /** 待处理文件总数 */
    @Schema(description = "待处理文件总数")
    private Integer total;
}
