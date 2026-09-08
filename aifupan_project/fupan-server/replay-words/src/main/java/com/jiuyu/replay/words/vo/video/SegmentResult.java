package com.jiuyu.replay.words.vo.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 单段生成结果 VO。
 *
 * <h3>用途</h3>
 * 由 {@code SegmentTask.call()} 返回，供 {@code VideoContentGenerator} 汇总判断批次整体成功/失败。
 *
 * <h3>汇总规则</h3>
 * <ul>
 *   <li>全部成功 → 批次状态 SUCCESS</li>
 *   <li>部分成功 → 批次状态 SUCCESS（已成功的分段保留，失败的保持 generateStatus=0 不会被重试）</li>
 *   <li>全部失败 → 批次状态 FAILED + 重置 set_job=0（允许定时器整批重试）</li>
 * </ul>
 *
 * @author lujie
 * @date 2025/6/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SegmentResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 分段 MongoDB 文档 _id */
    private String segmentId;

    /** 是否生成成功 */
    private boolean success;

    /** 失败时的错误信息 */
    private String errorMsg;

    /** 工厂方法：生成成功的分段结果 */
    public static SegmentResult success(String segmentId) {
        return new SegmentResult(segmentId, true, null);
    }

    /** 工厂方法：生成失败的分段结果 */
    public static SegmentResult fail(String segmentId, String errorMsg) {
        return new SegmentResult(segmentId, false, errorMsg);
    }
}
