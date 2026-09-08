package com.jiuyu.replay.api.controller.ai;

import com.jiuyu.replay.api.logic.ai.AiConfigLogic;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lujie
 * @date 2026-06-01
 */
@RestController
@CrossOrigin
@RequestMapping("replay/ai")
@Tag(name = "AI配置")
public class AiConfigController {

    @Resource
    private AiConfigLogic aiConfigLogic;

    @GetMapping("/getContentCorrectionConfig")
    @Operation(summary = "获取不同场景的模型和提示词配置")
    public R<AiContentCorrectionConfigVo> getContentCorrectionConfig(
            @Parameter(description = "场景类型 0-AI问答助手的纠正检查 1-AI问答助手的纠正 2-自然、优化原文的纠正检查 3-自然、优化原文的纠正", required = true)
            @RequestParam Integer sceneType) {
        return R.ok(aiConfigLogic.getContentCorrectionConfig(sceneType));
    }

}
