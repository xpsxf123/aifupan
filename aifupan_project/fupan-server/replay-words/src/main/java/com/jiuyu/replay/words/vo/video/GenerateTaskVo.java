package com.jiuyu.replay.words.vo.video;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一生成任务 VO — 视频和文件表合并后的抽象任务。
 *
 * <h3>来源</h3>
 * 由 {@code mergeTasks} 方法从以下两表的数据合并生成：
 * <ul>
 *   <li>视频表：{@code tb_anchor_video_detail}</li>
 *   <li>文件表：{@code tb_upload_file_detail} JOIN {@code tb_upload_file}</li>
 * </ul>
 *
 * <h3>both=1 拆分</h3>
 * 一条 DB 记录如果自然和优化两个 status 都为 1（且 set_job 都为 0），
 * 会拆成两条 GenerateTaskVo，type 分别为 NATURE(1) 和 OPTIMIZE(2)。
 *
 * @author lujie
 * @date 2025/6/24
 */
@Data
public class GenerateTaskVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 来源 id（videoId 或 fileId） */
    private String sourceId;

    /** 来源类型：0=视频, 1=文件 */
    private Integer sourceType;

    /** 内容类型：1=自然原文, 2=优化原文 */
    private Integer type;

    /** 用户 id */
    private Long userId;

    /** 租户 id */
    private Long tenantId;

    /** 行业 id（从视频/文件信息中获取，取不到时用默认行业） */
    private Long tradeId;
}
