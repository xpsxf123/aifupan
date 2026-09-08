package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordListBo;
import com.jiuyu.replay.words.entity.UploadFileAnalysisRecordEntity;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordListVo;

import java.util.List;


/**
 * 文件的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
public interface UploadFileAnalysisRecordProducer {


    /**
     * 文件的分析记录列表
     * @param uploadFileAnalysisRecordListBo 文件的分析记录列表查询参数
     * @return
     */
    PageUtils<UploadFileAnalysisRecordListVo> queryPage(UploadFileAnalysisRecordListBo uploadFileAnalysisRecordListBo);

    /**
    * 文件的分析记录信息
    * @param id 文件的分析记录id
    * @return
    */
    UploadFileAnalysisRecordInfoVo info(Long id);

    /**
     * 新增文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
     UploadFileAnalysisRecordInfoVo save(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo);

    /**
     * 修改文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    void update(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo);

    /**
     * 删除文件的分析记录
     * @param id 文件的分析记录id
     * @return
     */
    void deleteById(Long id);

    /**
     * 获取视频分析最新的版本号
     * @param fileId 文件id
     * @return
     */
    int getLastVersion(String fileId);

    /**
     * 获取文件分析最新的分析信息
     * @param fileId 文件id
     * @return
     */
    UploadFileAnalysisRecordInfoVo getLastInfo(String fileId);

    /**
     * 根据文件id和行业id获取最后一条分析记录
     * @param fileId 文件id
     * @param tradeId 行业id
     * @return
     */
    UploadFileAnalysisRecordInfoVo infoLastByFileIdAndTradeId(String fileId, Long tradeId);

    /**
     * 根据文件id和版本号获取分析记录
     * @param fileId 文件id
     * @param version 版本
     * @return
     */
    UploadFileAnalysisRecordInfoVo infoByVideoIdAndVersion(String fileId, int version);

    R<List<SentenceMarkVo>> selectAnalysisByFileId(String fileId, Long tradeId);

    /**
     * 将新路径保存到数据库
     * @param id 记录id
     * @param storeFileNameNew 新路径
     */
    void saveNewFilePath(Long id, String storeFileNameNew);

    UploadFileAnalysisRecordInfoVo listByFileIdAndTradeId(String fileId, Long tradeId);

    /**
     * 获取昨天的文件分析记录
     * @return
     */
    List<UploadFileAnalysisRecordEntity> yesterdayDateRecord();

    /**
     * 将cosKey保存到数据库
     * @param id 记录id
     * @param cosSaveKey cosKey
     */
    void saveCosKey(Long id, String cosSaveKey);

    /**
     * 根据文件id集合获取分析记录
     * @param fileIds 文件id集合
     * @return
     */
    List<UploadFileAnalysisRecordInfoVo> listByFileIds(List<String> fileIds);
}

