package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.CueWordsDescVo;
import com.jiuyu.replay.generic.vo.words.CueWordsNameVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.words.producer.CueWordsProducer;
import com.jiuyu.replay.words.producer.TradeProducer;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  ai智能体 系统内置问题列表
 * @author HeHui
 * @date 2026-07-22 19:40
 */
@Slf4j
@Validated
@RestController
@APIKey
@AllArgsConstructor
@RequestMapping("/internal/ai-agent/cue-word")
@Tag(name = "AI Agent 智能体问题 开放接口")
public class AiAgentCueWordController {

    private final CueWordsProducer cueWordsProducer;

    private final TradeProducer tradeProducer;


    /**
     * 获取行业下的智能体问题列表
     * @param queryBo 查询参数
     * @return 智能体问题列表
     */
    @PostMapping("/list")
    public R<List<CueWordsNameVo>> list(@RequestBody CueWordsQueryBo queryBo) {
        if (queryBo.getTradeId() == null) {
            return R.error("请选择行业");
        }
        List<TradeInfoVo> list = tradeProducer.listParentsByTradeId(
            queryBo.getTradeId(), Constant.GeneralEnum.GENERAL_NO.getCode());
        if (EmptyUtil.isEmpty(list)) {
            return R.error("行业无效");
        }
        List<Long> tradeIds = list.stream().map(TradeInfoVo::getId).toList();
        List<CueWordsNameVo> cueWordsNameVos = cueWordsProducer.listTradeWordNames(tradeIds, queryBo.getCueType(), queryBo.getApplyTo(), queryBo.getAccountType(), queryBo.getSyncScene(), queryBo.getLimit());
        return R.ok(cueWordsNameVos);
    }


    /**
     * 获取智能体问题描述
     * @param id 智能体问题id
     * @return 智能体问题描述
     */
    @GetMapping("/desc")
    public R<CueWordsDescVo> getDesc(@RequestParam Long id) {
        CueWordsDescVo cueWordsDescVo = cueWordsProducer.getWordDesc(id);
        return R.ok(cueWordsDescVo);
    }
}
