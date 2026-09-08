package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.words.HistoryParagraphLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.HistoryParagraphBll;
import com.jiuyu.replay.words.bo.HistoryParagraphBo;
import com.jiuyu.replay.words.bo.HistoryParagraphListBo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.vo.HistoryParagraphListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * ai问答的历史分析段落记录
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Service
public class HistoryParagraphLogicImpl implements HistoryParagraphLogic {

    @Resource
    private HistoryParagraphBll historyParagraphBll;


    @Override
    public R<PageUtils<HistoryParagraphListVo>> queryPage(HistoryParagraphListBo historyParagraphListBo) {

        return historyParagraphBll.queryPage(historyParagraphListBo);
    }

    @Override
    public R<HistoryParagraphInfoVo> info(Long id) {

        return historyParagraphBll.info(id);
    }

    @Override
    public R<HistoryParagraphInfoVo> save(HistoryParagraphBo historyParagraphBo) {

        return historyParagraphBll.save(historyParagraphBo);
    }

    @Override
    public R<String> update(HistoryParagraphBo historyParagraphBo) {

        return historyParagraphBll.update(historyParagraphBo);
    }

    @Override
    public R<String> delete(Long id) {

        return historyParagraphBll.delete(id);
    }

    @Override
    public R<List<HistoryParagraphListVo>> historyParagraphList(HistoryParagraphListBo paragraphBo) {
        paragraphBo.setLimit(-1);
        R<PageUtils<HistoryParagraphListVo>> pageUtilsR = queryPage(paragraphBo);
        if (pageUtilsR.getCode() == 0 && pageUtilsR.getData() != null && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())){
            return R.ok(pageUtilsR.getData().getList());
        }
        return R.ok(new ArrayList<>());
    }

    @Override
    public R<String> deleteHistoryParagraph(HistoryParagraphBo paragraphBo) {
        return historyParagraphBll.deleteHistoryParagraph(paragraphBo);
    }

    @Override
    public R<HistoryParagraphInfoVo> getByCode(Integer type, String sourceId, Integer sourceType, String code) {
        return historyParagraphBll.getByCode(type, sourceId, sourceType, code);
    }
}

