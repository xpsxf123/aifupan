package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

import java.util.Objects;

/**
 * AI 监控业务场景
 */
@Getter
public enum MonitorSceneTypeEnum {

    /**
     * 复盘场景，适用 sourceType=0
     */
    REPLAY(0, "复盘场景"),
    /**
     * 视频分析，适用 sourceType=1
     */
    VIDEO_ANALYSIS(1, "视频分析"),
    /**
     * 文案预审，适用 sourceType=1
     */
    SCRIPT_PRE_AUDIT(2, "文案预审");

    private final Integer code;
    private final String remarks;

    MonitorSceneTypeEnum(Integer code, String remarks) {
        this.code = code;
        this.remarks = remarks;
    }

    /**
     * 校验 sourceType 与 sceneType 的合法组合（防御前端传非法组合污染数据库）。
     *
     * <p>合法组合：</p>
     * <ul>
     *   <li>sourceType=0 录制视频 + sceneType=0 REPLAY 复盘场景</li>
     *   <li>sourceType=1 上传文件 + sceneType=1 VIDEO_ANALYSIS 视频分析</li>
     *   <li>sourceType=1 上传文件 + sceneType=2 SCRIPT_PRE_AUDIT 文案预审</li>
     * </ul>
     *
     * @param sourceType 资源类型（0 录制视频 / 1 上传文件）
     * @param sceneType  业务场景（0 / 1 / 2）
     * @return true 合法 / false 非法（含 null / 越界值）
     */
    public static boolean isLegalCombination(Integer sourceType, Integer sceneType) {
        if (sourceType == null || sceneType == null) {
            return false;
        }
        // sourceType=0 录制视频 → 仅 sceneType=0 复盘场景
        if (sourceType == 0) {
            return Objects.equals(sceneType, REPLAY.getCode());
        }
        // sourceType=1 上传文件 → sceneType=1 视频分析 或 sceneType=2 文案预审
        if (sourceType == 1) {
            return Objects.equals(sceneType, VIDEO_ANALYSIS.getCode())
                    || Objects.equals(sceneType, SCRIPT_PRE_AUDIT.getCode());
        }
        return false;
    }
}
