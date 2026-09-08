package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import com.jiuyu.replay.words.bll.SensitiveWordsBll;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.vo.video.AiAnalyzeTradePromptVo;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author ：liwj
 * &#064;description：ai相关的接口
 * @date ：2025/9/17 10:03
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("replay/words/aiWords")
@Tag(name = "ai相关的控制器")
public class AiWordsController {

    private final AnchorUrlBll anchorUrlBll;
    private final AnchorVideoBll anchorVideoBll;
    private final SensitiveWordsBll sensitiveWordsBll;


    @GetMapping("getAiAnchorPrompt")
    @Schema(description = "获取主播在ai问答中的提示词")
    public R<String> getAiAnchorPrompt(String sourceId, Integer sourceType) {
        return R.ok("", anchorUrlBll.getAiAnchorPrompt(sourceId, sourceType));
    }

    @PostMapping("getAiAnchorPrompt2")
    @Schema(description = "获取主播在ai问答中的提示词")
    public R<String> getAiAnchorPrompt2(@RequestBody AskRequestBo askRequestBo) {
        return R.ok("", anchorUrlBll.getAiAnchorPrompt(askRequestBo));
    }

    @GetMapping("aiAnalyzeTradePrompt")
    @Schema(description = "获取推荐的行业提示词")
    public R<AiAnalyzeTradePromptVo> aiAnalyzeTradePrompt(Integer sourceType, String sourceId) {
        return R.ok("", anchorVideoBll.getAiAnalyzeTradePrompt(sourceType, sourceId, (type, uuid) -> ResultUtil.getResult(sensitiveWordsBll.getAnalysisData(type, uuid))));
    }

    @GetMapping("updateSuggestTradeId")
    @Schema(description = "修改推荐行业的id")
    public R<Boolean> updateSuggestTradeId(Integer sourceType, String sourceId, Long tradeId) {
        return R.ok(anchorVideoBll.updateVideoOrFileDetails(sourceType, sourceId, null, tradeId));
    }

    @GetMapping("updateSuggestTrade")
    @Schema(description = "修改推荐的行业为已推荐")
    public R<Boolean> updateSuggestTrade(Integer sourceType, String sourceId) {
        return R.ok(anchorVideoBll.updateVideoOrFileDetails(sourceType, sourceId, 1, null));
    }

}
