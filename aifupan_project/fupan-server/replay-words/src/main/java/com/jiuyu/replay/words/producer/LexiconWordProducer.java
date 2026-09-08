package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.LexiconWordListVo;
import com.jiuyu.replay.words.vo.LexiconWordInfoVo;
import com.jiuyu.replay.words.bo.LexiconWordBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;


/**
 * 词库-词语关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
public interface LexiconWordProducer {


    /**
     * 词库-词语关联列表
     * @param lexiconWordListBo 词库-词语关联列表查询参数
     * @return
     */
    PageUtils<LexiconWordListVo> queryPage(LexiconWordListBo lexiconWordListBo);

    /**
    * 词库-词语关联信息
    * @param id 词库-词语关联id
    * @return
    */
    LexiconWordInfoVo info(Long id);

    /**
     * 新增词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
     LexiconWordInfoVo save(LexiconWordBo lexiconWordBo);

    /**
     * 修改词库-词语关联
     * @param lexiconWordBo 词库-词语关联对象
     * @return
     */
    void update(LexiconWordBo lexiconWordBo);

    /**
     * 删除词库-词语关联
     * @param id 词库-词语关联id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据词语id删除词库-词语关联
     * @param wordId
     */
    void deleteByWordId(Long wordId);
}

