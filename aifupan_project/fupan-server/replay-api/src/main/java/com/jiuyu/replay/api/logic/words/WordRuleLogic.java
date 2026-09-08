package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.WordRuleListVo;
import com.jiuyu.replay.words.vo.WordRuleInfoVo;
import com.jiuyu.replay.words.bo.WordRuleBo;
import com.jiuyu.replay.words.bo.WordRuleListBo;


/**
 * 词语匹配规则
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
public interface WordRuleLogic {


    /**
     * 词语匹配规则列表
     * @param wordRuleListBo 词语匹配规则列表查询参数
     * @return
     */
    R<PageUtils<WordRuleListVo>> queryPage(WordRuleListBo wordRuleListBo);

    /**
    * 词语匹配规则信息
    * @param id 词语匹配规则id
    * @return
    */
    R<WordRuleInfoVo> info(Long id);

    /**
     * 新增词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    R<String> save(WordRuleBo wordRuleBo);

    /**
     * 修改词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    R<String> update(WordRuleBo wordRuleBo);

    /**
     * 删除词语匹配规则
     * @param id 词语匹配规则id
     * @return
     */
    R<String> delete(Long id);


}

