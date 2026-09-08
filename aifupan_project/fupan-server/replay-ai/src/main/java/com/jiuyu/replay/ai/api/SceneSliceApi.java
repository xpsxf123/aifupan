package com.jiuyu.replay.ai.api;

import com.jiuyu.replay.ai.bll.SceneSliceBll;
import com.jiuyu.replay.generic.feign.ai.SceneSliceFeign;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 场景切片 Feign 实现
 *
 * @author lj
 * @date 2026-07-06
 */
@Service
@AllArgsConstructor
public class SceneSliceApi implements SceneSliceFeign {

    private final SceneSliceBll sceneSliceBll;

    @Override
    public String getSceneSliceResultByVideoId(String videoId) {
        return sceneSliceBll.getAiResultByVideoId(videoId);
    }
}
