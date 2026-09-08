package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.entity.UploadFileAnalysisRecordEntity;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordListVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordListBo;
import com.jiuyu.replay.words.producer.UploadFileAnalysisRecordProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 文件的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Component
public class UploadFileAnalysisRecordBll {

    @Resource
    private UploadFileAnalysisRecordProducer uploadFileAnalysisRecordProducer;


    /**
     * 文件的分析记录列表
     * @param uploadFileAnalysisRecordListBo 文件的分析记录列表查询参数
     * @return
     */
    public R<PageUtils<UploadFileAnalysisRecordListVo>> queryPage(UploadFileAnalysisRecordListBo uploadFileAnalysisRecordListBo) {

        return R.ok("获取成功", uploadFileAnalysisRecordProducer.queryPage(uploadFileAnalysisRecordListBo));
    }

    /**
    * 文件的分析记录信息
    * @param id 文件的分析记录id
    * @return
    */
    public R<UploadFileAnalysisRecordInfoVo> info(Long id) {

        UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = uploadFileAnalysisRecordProducer.info(id);
        return R.ok("获取成功", uploadFileAnalysisRecordInfoVo);
    }

    /**
     * 新增文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    public R<String> save(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo) {

        UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = uploadFileAnalysisRecordProducer.save(uploadFileAnalysisRecordBo);
        return R.ok("添加成功");
    }

    /**
     * 修改文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    public R<String> update(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo) {

        uploadFileAnalysisRecordProducer.update(uploadFileAnalysisRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除文件的分析记录
     * @param id 文件的分析记录id
     * @return
     */
    public R<String> delete(Long id) {

        uploadFileAnalysisRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据文件id和行业id获取最后一条分析记录
     * @param fileId 文件id
     * @param tradeId 行业id
     * @return
     */
    public R<UploadFileAnalysisRecordInfoVo> infoLastByFileIdAndTradeId(String fileId, Long tradeId) {

        return R.ok(uploadFileAnalysisRecordProducer.infoLastByFileIdAndTradeId(fileId, tradeId));
    }

    public R<List<SentenceMarkVo>> selectAnalysisByFileId(String fileId, Long tradeId) {
        return uploadFileAnalysisRecordProducer.selectAnalysisByFileId(fileId,tradeId);
    }

    /**
     * 获取昨天的文件分析记录
     * @return
     */
    public List<UploadFileAnalysisRecordEntity> yesterdayDateRecord() {
        return uploadFileAnalysisRecordProducer.yesterdayDateRecord();
    }
}

