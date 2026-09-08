package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordListVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordListBo;

import java.util.List;


/**
 * 文件的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
public interface UploadFileAnalysisRecordLogic {


    /**
     * 文件的分析记录列表
     * @param uploadFileAnalysisRecordListBo 文件的分析记录列表查询参数
     * @return
     */
    R<PageUtils<UploadFileAnalysisRecordListVo>> queryPage(UploadFileAnalysisRecordListBo uploadFileAnalysisRecordListBo);

    /**
    * 文件的分析记录信息
    * @param id 文件的分析记录id
    * @return
    */
    R<UploadFileAnalysisRecordInfoVo> info(Long id);

    /**
     * 新增文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    R<String> save(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo);

    /**
     * 修改文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    R<String> update(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo);

    /**
     * 删除文件的分析记录
     * @param id 文件的分析记录id
     * @return
     */
    R<String> delete(Long id);


    R<List<SentenceMarkVo>> selectAnalysisByFileId(String fileId, Long tradeId);
}

