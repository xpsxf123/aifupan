package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.WordRuleRelevanceListVo;
import com.jiuyu.replay.words.vo.WordRuleRelevanceInfoVo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceBo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceListBo;


/**
 * 规则关联词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
public interface WordRuleRelevanceLogic {


    /**
     * 规则关联词列表
     * @param wordRuleRelevanceListBo 规则关联词列表查询参数
     * @return
     */
    R<PageUtils<WordRuleRelevanceListVo>> queryPage(WordRuleRelevanceListBo wordRuleRelevanceListBo);

    /**
    * 规则关联词信息
    * @param id 规则关联词id
    * @return
    */
    R<WordRuleRelevanceInfoVo> info(Long id);

    /**
     * 新增规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
    R<String> save(WordRuleRelevanceBo wordRuleRelevanceBo);

    /**
     * 修改规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
    R<String> update(WordRuleRelevanceBo wordRuleRelevanceBo);

    /**
     * 删除规则关联词
     * @param id 规则关联词id
     * @return
     */
    R<String> delete(Long id);


}

