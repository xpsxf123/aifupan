package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.WordRuleLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.WordRuleBll;
import com.jiuyu.replay.words.bo.WordRuleBo;
import com.jiuyu.replay.words.bo.WordRuleListBo;
import com.jiuyu.replay.words.vo.WordRuleInfoVo;
import com.jiuyu.replay.words.vo.WordRuleListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 词语匹配规则
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Service
public class WordRuleLogicImpl implements WordRuleLogic {

    @Resource
    private WordRuleBll wordRuleBll;


    @Override
    public R<PageUtils<WordRuleListVo>> queryPage(WordRuleListBo wordRuleListBo) {

        return wordRuleBll.queryPage(wordRuleListBo);
    }

    @Override
    public R<WordRuleInfoVo> info(Long id) {

        return wordRuleBll.info(id);
    }

    @Override
    public R<String> save(WordRuleBo wordRuleBo) {

        return wordRuleBll.save(wordRuleBo);
    }

    @Override
    public R<String> update(WordRuleBo wordRuleBo) {

        return wordRuleBll.update(wordRuleBo);
    }

    @Override
    public R<String> delete(Long id) {

        return wordRuleBll.delete(id);
    }


}

