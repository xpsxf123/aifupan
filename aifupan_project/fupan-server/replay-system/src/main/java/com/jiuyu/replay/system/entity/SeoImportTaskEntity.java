package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO 文章批量导入任务。
 *
 * <p>导入走异步，结果落 resultJson 以便运营刷新页面后仍能查回本次导入结果。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@TableName("tb_seo_import_task")
public class SeoImportTaskEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 对外任务 ID（UUID），前端据此轮询进度 */
    private String taskId;

    /** 任务状态：0 处理中 1 已完成 2 失败 */
    private Integer taskStatus;

    /** 待处理文件总数 */
    private Integer total;

    /** 已处理数 */
    private Integer processed;

    /** 成功篇数 */
    private Integer successCount;

    /** 失败篇数 */
    private Integer failCount;

    /** 逐篇结果 JSON：[{file,title,success,notes[]}] */
    private String resultJson;

    /** 任务级失败原因（status=2 时） */
    private String errorMsg;

    /** 发起导入的后台用户 ID */
    private Long createUserId;

    /** 创建时间 */
    private LocalDateTime createDate;

    /** 完成时间 */
    private LocalDateTime finishDate;
}
