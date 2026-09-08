package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.WordRuleRelevanceLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.WordRuleRelevanceBll;
import com.jiuyu.replay.words.bo.WordRuleRelevanceBo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceListBo;
import com.jiuyu.replay.words.vo.WordRuleRelevanceInfoVo;
import com.jiuyu.replay.words.vo.WordRuleRelevanceListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 规则关联词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Service
public class WordRuleRelevanceLogicImpl implements WordRuleRelevanceLogic {

    @Resource
    private WordRuleRelevanceBll wordRuleRelevanceBll;


    @Override
    public R<PageUtils<WordRuleRelevanceListVo>> queryPage(WordRuleRelevanceListBo wordRuleRelevanceListBo) {

        return wordRuleRelevanceBll.queryPage(wordRuleRelevanceListBo);
    }

    @Override
    public R<WordRuleRelevanceInfoVo> info(Long id) {

        return wordRuleRelevanceBll.info(id);
    }

    @Override
    public R<String> save(WordRuleRelevanceBo wordRuleRelevanceBo) {

        return wordRuleRelevanceBll.save(wordRuleRelevanceBo);
    }

    @Override
    public R<String> update(WordRuleRelevanceBo wordRuleRelevanceBo) {

        return wordRuleRelevanceBll.update(wordRuleRelevanceBo);
    }

    @Override
    public R<String> delete(Long id) {

        return wordRuleRelevanceBll.delete(id);
    }


}

