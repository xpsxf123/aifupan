package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.UploadFileAnalysisBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisListBo;
import com.jiuyu.replay.words.vo.OnlineAnalysisItemVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisVo;

import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
public interface UploadFileAnalysisProducer {
    /**
     * 保存文件分析内容
     * @param uploadFileAnalysisVo
     * @return
     */
    R<String> saveuploadFileAnalysis(List<UploadFileAnalysisVo> uploadFileAnalysisVo);

    /**
     * 根据文件Id查询分析内容
     * @param fileId
     * @return
     */
    R<List<UploadFileAnalysisVo>> seletByFileId(String fileId);

    /**
     * 客户端获取复盘文件分析内容
     * @param uploadFileAnalysisListBo
     * @return
     */
    R<List<UploadFileAnalysisVo>> seletContent(UploadFileAnalysisListBo uploadFileAnalysisListBo);

    /**
     * 保存复盘上传文件分析记录及内容
     * @param uploadFileRecodBo
     * @return
     */
    R<String> saveFileAnalysis(List<UploadFileAnalysisBo> uploadFileRecodBo);

    /**
     * 清除文件的分析数据
     * @param fileId 文件唯一标识
     * @return
     */
    void clearAnalysis(String fileId);

    /**
     * 根据文件唯一标识和行业id获取文件的分析数据
     * @param fileId 文件唯一标识uuid
     * @param tradeId 行业id
     * @return
     */
    List<OnlineAnalysisItemVo> listByFileIdAndTradeId(String fileId, Long tradeId);

    /**
     * 获取文件的第一批段落列表
     * @param fileId 文件唯一标识
     * @return
     */
    List<UploadFileAnalysisVo> listAnalysisOneByFileId(String fileId);
}
