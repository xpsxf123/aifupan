package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.HistoryParagraphListVo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.bo.HistoryParagraphBo;
import com.jiuyu.replay.words.bo.HistoryParagraphListBo;


/**
 * ai问答的历史分析段落记录
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
public interface HistoryParagraphProducer {


    /**
     * ai问答的历史分析段落记录列表
     * @param historyParagraphListBo ai问答的历史分析段落记录列表查询参数
     * @return
     */
    PageUtils<HistoryParagraphListVo> queryPage(HistoryParagraphListBo historyParagraphListBo);

    /**
    * ai问答的历史分析段落记录信息
    * @param id ai问答的历史分析段落记录id
    * @return
    */
    HistoryParagraphInfoVo info(Long id);

    /**
     * 新增ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
     HistoryParagraphInfoVo save(HistoryParagraphBo historyParagraphBo);

    /**
     * 修改ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
    void update(HistoryParagraphBo historyParagraphBo);

    /**
     * 删除ai问答的历史分析段落记录
     * @param id ai问答的历史分析段落记录id
     * @return
     */
    void deleteById(Long id);

    /**
     * 删除ai问答的历史分析段落记录
     * @param paragraphBo
     */
    void deleteHistoryParagraph(HistoryParagraphBo paragraphBo);

    /**
     * 根据code查询ai问答的历史分析段落记录
     * @param type
     * @param sourceId
     * @param sourceType
     * @param code
     * @return
     */
    HistoryParagraphInfoVo getByCode(Integer type, String sourceId, Integer sourceType, String code);
}

