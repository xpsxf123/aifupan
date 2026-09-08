package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.WordRuleRelevanceListVo;
import com.jiuyu.replay.words.vo.WordRuleRelevanceInfoVo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceBo;
import com.jiuyu.replay.words.bo.WordRuleRelevanceListBo;
import com.jiuyu.replay.words.producer.WordRuleRelevanceProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 规则关联词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Component
public class WordRuleRelevanceBll {

    @Resource
    private WordRuleRelevanceProducer wordRuleRelevanceProducer;


    /**
     * 规则关联词列表
     * @param wordRuleRelevanceListBo 规则关联词列表查询参数
     * @return
     */
    public R<PageUtils<WordRuleRelevanceListVo>> queryPage(WordRuleRelevanceListBo wordRuleRelevanceListBo) {

        return R.ok("获取成功", wordRuleRelevanceProducer.queryPage(wordRuleRelevanceListBo));
    }

    /**
    * 规则关联词信息
    * @param id 规则关联词id
    * @return
    */
    public R<WordRuleRelevanceInfoVo> info(Long id) {

        WordRuleRelevanceInfoVo wordRuleRelevanceInfoVo = wordRuleRelevanceProducer.info(id);
        return R.ok("获取成功", wordRuleRelevanceInfoVo);
    }

    /**
     * 新增规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
    public R<String> save(WordRuleRelevanceBo wordRuleRelevanceBo) {

        WordRuleRelevanceInfoVo wordRuleRelevanceInfoVo = wordRuleRelevanceProducer.save(wordRuleRelevanceBo);
        return R.ok("添加成功");
    }

    /**
     * 修改规则关联词
     * @param wordRuleRelevanceBo 规则关联词对象
     * @return
     */
    public R<String> update(WordRuleRelevanceBo wordRuleRelevanceBo) {

        wordRuleRelevanceProducer.update(wordRuleRelevanceBo);
        return R.ok("修改成功");
    }

    /**
     * 删除规则关联词
     * @param id 规则关联词id
     * @return
     */
    public R<String> delete(Long id) {

        wordRuleRelevanceProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

