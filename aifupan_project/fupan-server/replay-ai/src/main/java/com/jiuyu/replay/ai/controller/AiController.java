package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.AiRelatedBll;
import com.jiuyu.replay.ai.vo.AiPromptWordVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/25 19:23
 */
@RestController
@CrossOrigin
@RequestMapping("replay/ai/aiRelated")
@Tag(name = "ai相关的控制器")
@AllArgsConstructor
public class AiController {

    private final AiRelatedBll aiRelatedBll;

    @GetMapping("/getAiPromptWord")
    @Operation(summary = "获取ai提示词")
    public R<String> getAiPromptWord(Long cueWordsId, Integer cueWordsType) {
        return R.ok("返回成功", aiRelatedBll.getAiPromptWord(cueWordsId, cueWordsType, true));
    }

    @GetMapping("/getAiPromptWord2")
    @Operation(summary = "获取ai提示词")
    public R<AiPromptWordVo> getAiPromptWord2(Long cueWordsId, Integer cueWordsType, String lastConversationId) {
        return R.ok("返回成功", aiRelatedBll.getAiPromptWord2(cueWordsId, cueWordsType, lastConversationId, true));
    }


}
