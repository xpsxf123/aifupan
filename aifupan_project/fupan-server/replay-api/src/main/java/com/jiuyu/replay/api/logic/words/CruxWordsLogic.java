package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.CruxWordsBatchBo;
import com.jiuyu.replay.words.bo.CruxWordsBo;
import com.jiuyu.replay.words.bo.CruxWordsListBo;
import com.jiuyu.replay.words.vo.CruxWordsInfoVo;
import com.jiuyu.replay.words.vo.CruxWordsListVo;


/**
 * 关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
public interface CruxWordsLogic {


    /**
     * 关键词列表
     * @param cruxWordsListBo 关键词列表查询参数
     * @return
     */
    R<PageUtils<CruxWordsListVo>> queryPage(CruxWordsListBo cruxWordsListBo);

    /**
    * 关键词信息
    * @param id 关键词id
    * @return
    */
    R<CruxWordsInfoVo> info(Long id);

    /**
     * 新增关键词
     * @param cruxWordsBo 关键词对象
     * @return
     */
    R<String> save(CruxWordsBo cruxWordsBo);

    /**
     * 修改关键词
     * @param cruxWordsBo 关键词对象
     * @return
     */
    R<String> update(CruxWordsBo cruxWordsBo);

    /**
     * 删除关键词
     * @param id 关键词id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 批量新增关键词
     * @param cruxWordsBatchBo 关键词对象
     * @return
     */
    R<String> saveBatch(CruxWordsBatchBo cruxWordsBatchBo);
}

