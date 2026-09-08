package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.HistoryParagraphListVo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.bo.HistoryParagraphBo;
import com.jiuyu.replay.words.bo.HistoryParagraphListBo;

import java.util.List;


/**
 * ai问答的历史分析段落记录
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
public interface HistoryParagraphLogic {


    /**
     * ai问答的历史分析段落记录列表
     * @param historyParagraphListBo ai问答的历史分析段落记录列表查询参数
     * @return
     */
    R<PageUtils<HistoryParagraphListVo>> queryPage(HistoryParagraphListBo historyParagraphListBo);

    /**
    * ai问答的历史分析段落记录信息
    * @param id ai问答的历史分析段落记录id
    * @return
    */
    R<HistoryParagraphInfoVo> info(Long id);

    /**
     * 新增ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
    R<HistoryParagraphInfoVo> save(HistoryParagraphBo historyParagraphBo);

    /**
     * 修改ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
    R<String> update(HistoryParagraphBo historyParagraphBo);

    /**
     * 删除ai问答的历史分析段落记录
     * @param id ai问答的历史分析段落记录id
     * @return
     */
    R<String> delete(Long id);


    /**
     * ai问答的历史分析段落记录列表
     * @param paragraphBo
     * @return
     */
    R<List<HistoryParagraphListVo>> historyParagraphList(HistoryParagraphListBo paragraphBo);

    /**
     * 删除ai问答的历史分析段落记录
     * @param paragraphBo
     * @return
     */
    R<String> deleteHistoryParagraph(HistoryParagraphBo paragraphBo);

    /**
     * 根据code查询ai问答的历史分析段落记录
     * @param type 助手类型
     * @param sourceId 资源id
     * @param sourceType 资源类型
     * @param code 段落code
     * @return
     */
    R<HistoryParagraphInfoVo> getByCode(Integer type, String sourceId, Integer sourceType, String code);
}

