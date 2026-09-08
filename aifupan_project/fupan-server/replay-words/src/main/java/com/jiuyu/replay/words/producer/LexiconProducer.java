package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.LexiconBo;
import com.jiuyu.replay.words.bo.LexiconListBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.vo.LexiconInfoVo;
import com.jiuyu.replay.words.vo.LexiconListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;


/**
 * 词库
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
public interface LexiconProducer {


    /**
     * 词库列表
     * @param lexiconListBo 词库列表查询参数
     * @return
     */
    PageUtils<LexiconListVo> queryPage(LexiconListBo lexiconListBo);

    /**
    * 词库信息
    * @param id 词库id
    * @return
    */
    LexiconInfoVo info(Long id);

    /**
     * 新增词库
     * @param lexiconBo 词库对象
     * @return
     */
     LexiconInfoVo save(LexiconBo lexiconBo);

    /**
     * 修改词库
     * @param lexiconBo 词库对象
     * @return
     */
    void update(LexiconBo lexiconBo);

    /**
     * 删除词库
     * @param id 词库id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取词库的词语列表
     * @param lexiconWordListBo 列表查询参数
     * @return
     */
    PageUtils<SensitiveWordsClientListVo> getWordsList(LexiconWordListBo lexiconWordListBo);

    /**
     * 根据用户id和行业id获取词库
     * @param userId 用户id
     * @param tradeId 行业id
     * @return
     */
    LexiconInfoVo getByUserIdAndTradeId(Long userId, Long tradeId);
}

