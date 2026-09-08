package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.WordRuleBo;
import com.jiuyu.replay.words.bo.WordRuleListBo;
import com.jiuyu.replay.words.vo.WordRuleInfoVo;
import com.jiuyu.replay.words.vo.WordRuleListVo;

import java.util.List;


/**
 * 词语匹配规则
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
public interface WordRuleProducer {


    /**
     * 词语匹配规则列表
     * @param wordRuleListBo 词语匹配规则列表查询参数
     * @return
     */
    PageUtils<WordRuleListVo> queryPage(WordRuleListBo wordRuleListBo);

    /**
    * 词语匹配规则信息
    * @param id 词语匹配规则id
    * @return
    */
    WordRuleInfoVo info(Long id);

    /**
     * 新增词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
     WordRuleInfoVo save(WordRuleBo wordRuleBo);

    /**
     * 修改词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    void update(WordRuleBo wordRuleBo);

    /**
     * 删除词语匹配规则
     * @param id 词语匹配规则id
     * @return
     */
    void deleteById(Long id);


    /**
     * 保存词语的规则列表
     * @param wordId 词语id
     * @param ruleList 规则列表
     */
    void saveBatchByWordId(Long wordId, List<WordRuleBo> ruleList);

    /**
     * 根据词语id获取规则列表
     * @param wordId 词语id
     * @return
     */
    List<WordRuleInfoVo> listByWordId(Long wordId);

    /**
     * 更新词语规则
     * @param wordId 词语id
     * @param ruleList 规则列表
     */
    void updateBatchByWordId(Long wordId, List<WordRuleBo> ruleList);

    /**
     * 根据词语id删除规则，以及规则相关的规则关联词
     * @param wordId 词语id
     */
    void removeByWordId(Long wordId);

    /**
     * 根据词语id集合获取规则，以及规则相关的规则关联词
     * @param wordIds 词语id集合
     * @return
     */
    List<WordRuleInfoVo> listByWordIds(List<Long> wordIds);
}

