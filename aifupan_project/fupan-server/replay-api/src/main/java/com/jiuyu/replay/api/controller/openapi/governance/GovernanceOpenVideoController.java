package com.jiuyu.replay.api.controller.openapi.governance;

import com.jiuyu.replay.api.interceptor.FeatureSignature;
import com.jiuyu.replay.api.logic.words.AnchorVideoLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import org.springframework.web.bind.annotation.*;

/**
 * 企业后台管理 - 视频相关API
 *
 * @author AI Assistant
 * @date 2026-08-03
 */
@FeatureSignature(client = "governance")
@RestController
@RequestMapping("/replay/openapi/governance/video")
public class GovernanceOpenVideoController {

    private final AnchorVideoLogic anchorVideoLogic;

    public GovernanceOpenVideoController(AnchorVideoLogic anchorVideoLogic) {
        this.anchorVideoLogic = anchorVideoLogic;
    }

    /**
     * 根据视频唯一标识获取视频信息（含 tenantId）
     *
     * @param videoId 视频唯一标识
     * @return 视频信息，不存在返回错误
     */
    @GetMapping("/infoByVideoId")
    public R<AnchorVideoInfoVo> infoByVideoId(@RequestParam String videoId) {
        return anchorVideoLogic.infoByVideoId(videoId);
    }
}
