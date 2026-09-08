package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.LexiconWordListVo;
import com.jiuyu.replay.words.vo.LexiconWordInfoVo;
import com.jiuyu.replay.words.bo.LexiconWordBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.producer.LexiconWordProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 词库-词语关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Component
public class LexiconWordBll {

    @Resource
    private LexiconWordProducer lexiconWordProducer;


    /**
     * 词库-词语关联列表
     * @param lexiconWordListBo 词库-词语关联列表查询参数
     * @return
     */
    public R<PageUtils<LexiconWordListVo>> queryPage(LexiconWordListBo lexiconWordListBo) {

        return R.ok("获取成功", lexiconWordProducer.queryPage(lexiconWordListBo));
    }

    /**
    * 词库-词语关联信息
    * @param id 词库-词语关联id
    * @return
    */
    public R<LexiconWordInfoVo> info(Long id) {

        LexiconWordInfoVo lexiconWordInfoVo = lexiconWordProducer.info(id);
        return R.ok("获取成功", lexiconWordInfoVo);
    }

    /**
     * 新增词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
    public R<String> save(LexiconWordBo lexiconWordBo) {

        LexiconWordInfoVo lexiconWordInfoVo = lexiconWordProducer.save(lexiconWordBo);
        return R.ok("添加成功");
    }

    /**
     * 修改词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
    public R<String> update(LexiconWordBo lexiconWordBo) {

        lexiconWordProducer.update(lexiconWordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除词库-词语关联
     * @param id 词库-词语关联id
     * @return
     */
    public R<String> delete(Long id) {

        lexiconWordProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

