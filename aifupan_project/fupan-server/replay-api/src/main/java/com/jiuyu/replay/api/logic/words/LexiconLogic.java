package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
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
public interface LexiconLogic {


    /**
     * 词库列表
     * @param lexiconListBo 词库列表查询参数
     * @return
     */
    R<PageUtils<LexiconListVo>> queryPage(LexiconListBo lexiconListBo);

    /**
    * 词库信息
    * @param id 词库id
    * @return
    */
    R<LexiconInfoVo> info(Long id);

    /**
     * 新增词库
     * @param lexiconBo 词库对象
     * @return
     */
    R<String> save(LexiconBo lexiconBo);

    /**
     * 修改词库
     * @param lexiconBo 词库对象
     * @return
     */
    R<String> update(LexiconBo lexiconBo);

    /**
     * 删除词库
     * @param id 词库id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取词库的词语列表
     * @param lexiconWordListBo 列表查询参数
     * @return
     */
    R<PageUtils<SensitiveWordsClientListVo>> getWordsList(LexiconWordListBo lexiconWordListBo);
}

