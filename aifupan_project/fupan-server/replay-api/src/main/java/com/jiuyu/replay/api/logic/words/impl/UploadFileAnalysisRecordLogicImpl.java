package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.UploadFileAnalysisRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.UploadFileAnalysisRecordBll;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordListBo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 文件的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Service
public class UploadFileAnalysisRecordLogicImpl implements UploadFileAnalysisRecordLogic {

    @Resource
    private UploadFileAnalysisRecordBll uploadFileAnalysisRecordBll;


    @Override
    public R<PageUtils<UploadFileAnalysisRecordListVo>> queryPage(UploadFileAnalysisRecordListBo uploadFileAnalysisRecordListBo) {

        return uploadFileAnalysisRecordBll.queryPage(uploadFileAnalysisRecordListBo);
    }

    @Override
    public R<UploadFileAnalysisRecordInfoVo> info(Long id) {

        return uploadFileAnalysisRecordBll.info(id);
    }

    @Override
    public R<String> save(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo) {

        return uploadFileAnalysisRecordBll.save(uploadFileAnalysisRecordBo);
    }

    @Override
    public R<String> update(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo) {

        return uploadFileAnalysisRecordBll.update(uploadFileAnalysisRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return uploadFileAnalysisRecordBll.delete(id);
    }

    @Override
    public R<List<SentenceMarkVo>> selectAnalysisByFileId(String fileId, Long tradeId) {
        return uploadFileAnalysisRecordBll.selectAnalysisByFileId(fileId,tradeId);
    }

}

