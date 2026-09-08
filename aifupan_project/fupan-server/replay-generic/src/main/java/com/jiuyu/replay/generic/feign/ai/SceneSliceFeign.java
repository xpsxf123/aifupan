package com.jiuyu.replay.generic.feign.ai;

/**
 * 场景切片 Feign 接口（供 PlaceholderContext 跨模块访问）
 *
 * @author lj
 * @date 2026-07-06
 */
public interface SceneSliceFeign {

    /**
     * 根据视频ID查询场景切片AI分析结果
     *
     * @param videoId 视频ID
     * @return AI分析结果文本，无记录时返回 null
     */
    String getSceneSliceResultByVideoId(String videoId);
}
