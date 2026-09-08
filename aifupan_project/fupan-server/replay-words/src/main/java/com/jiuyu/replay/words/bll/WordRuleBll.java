package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.WordRuleBo;
import com.jiuyu.replay.words.bo.WordRuleListBo;
import com.jiuyu.replay.words.producer.WordRuleProducer;
import com.jiuyu.replay.words.vo.WordRuleInfoVo;
import com.jiuyu.replay.words.vo.WordRuleListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 词语匹配规则
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Component
public class WordRuleBll {

    @Resource
    private WordRuleProducer wordRuleProducer;


    /**
     * 词语匹配规则列表
     * @param wordRuleListBo 词语匹配规则列表查询参数
     * @return
     */
    public R<PageUtils<WordRuleListVo>> queryPage(WordRuleListBo wordRuleListBo) {

        return R.ok("获取成功", wordRuleProducer.queryPage(wordRuleListBo));
    }

    /**
    * 词语匹配规则信息
    * @param id 词语匹配规则id
    * @return
    */
    public R<WordRuleInfoVo> info(Long id) {

        WordRuleInfoVo wordRuleInfoVo = wordRuleProducer.info(id);
        return R.ok("获取成功", wordRuleInfoVo);
    }

    /**
     * 新增词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    public R<String> save(WordRuleBo wordRuleBo) {

        WordRuleInfoVo wordRuleInfoVo = wordRuleProducer.save(wordRuleBo);
        return R.ok("添加成功");
    }

    /**
     * 修改词语匹配规则
     * @param wordRuleBo 词语匹配规则对象
     * @return
     */
    public R<String> update(WordRuleBo wordRuleBo) {

        wordRuleProducer.update(wordRuleBo);
        return R.ok("修改成功");
    }

    /**
     * 删除词语匹配规则
     * @param id 词语匹配规则id
     * @return
     */
    public R<String> delete(Long id) {

        wordRuleProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

