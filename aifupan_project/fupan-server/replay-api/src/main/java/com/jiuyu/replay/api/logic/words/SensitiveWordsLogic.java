package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.video.BarrageDataVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * 敏感词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
public interface SensitiveWordsLogic {


    /**
     * 敏感词列表
     * @param sensitiveWordsListBo 敏感词列表查询参数
     * @return
     */
    R<PageUtils<SensitiveWordsListVo>> queryPage(SensitiveWordsListBo sensitiveWordsListBo);

    /**
    * 敏感词信息
    * @param id 敏感词id
    * @return
    */
    R<SensitiveWordsInfoVo> info(Long id);

    /**
     * 新增敏感词
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    R<List<String>> save(SensitiveWordsBo sensitiveWordsBo);

    /**
     * 修改敏感词
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    R<String> update(SensitiveWordsBo sensitiveWordsBo);

    /**
     * 删除敏感词
     * @param id 敏感词id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 批量新增敏感词
     * @param sensitiveWordsBatchBo 敏感词对象
     * @return
     */
    R<List<String>> saveBatch(SensitiveWordsBatchBo sensitiveWordsBatchBo);

    /**
     * 文字关键词/敏感词标识
     * @return
     */
    R<List<SentenceMarkVo>> wordsMark(List<WordsMarkBo> wordsMarkBoList);

    /**
     * 文本内容关键词/敏感词标识
     * @return
     */
    R<SentenceMarkVo> wordsMarkByText(WordsMarkBo wordsMarkBo);
    /**
     * 文本内容关键词/敏感词标识2_0
     * @return
     */
    R<AnalysisResultVo> wordsMarkByText2_0(WordsMarkBo wordsMarkBo);

    /**
     * 敏感词导入
     * @param excel excel文件
     * @return
     */
    R<List<String>> importExcel(MultipartFile excel);

    /**
     * 获取在线复盘分析信息
     * @param fileId 文件唯一标识 uuid，传其中一个
     * @param videoId 视频唯一标识 uuid，传其中一个
     * @return
     */
    R<OnlineAnalysisInfoVo> getOnlineAnalysisInfo(String fileId, String videoId) throws Exception;

    /**
     * 获取在线复盘分析信息
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    R<OnlineContrastAnalysisInfoVo> getOnlineContrastAnalysisInfo(String contrastId) throws Exception;

    /**
     * 二次分析
     * @return
     */
    R<List<SentenceMarkVo>> wordsMarkReAnalysis(WordsMarkReAnalysisBo wordsMarkReAnalysisBo);

    /**
     * 二次分析2_0
     * @return
     */
    R<AnalysisResultVo> wordsMarkReAnalysis2_0(WordsMarkReAnalysisBo wordsMarkReAnalysisBo);

    /**
     * 下载分析文件
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    byte[] downloadAnalysisFile(Integer type, String uuid);

    /**
     * 下载分析文件2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    byte[] downloadAnalysisFile2_0(Integer type, String uuid);

    /**
     * 获取在线复盘分析信息，以zip形式返回
     * @param fileId 文件唯一标识 uuid，传其中一个
     * @param videoId 视频唯一标识 uuid，传其中一个
     * @return
     */
    byte[] getOnlineAnalysisZip(String fileId, String videoId) throws Exception;

    /**
     * 获取在线复盘分析信息，以zip形式返回2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    byte[] getOnlineAnalysisZip2_0(Integer type, String uuid) throws Exception;

    /**
     * 获取在线对比复盘分析信息，以zip形式返回
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    byte[] getOnlineContrastAnalysisZip(String contrastId) throws Exception;
    /**
     * 获取在线对比复盘分析信息，以zip形式返回2_0
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    byte[] getOnlineContrastAnalysisZip2_0(String contrastId) throws Exception ;

    /**
     * 文字关键词/敏感词标识2_0
     * @param wordsMarkBoList 段落列表数据
     * @return
     */
    R<AnalysisResultVo> wordsMark2_0(List<WordsMarkBo> wordsMarkBoList) throws Exception;

    /**
     * 获取在线复盘分析信息2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    R<AnalysisResultCloudVo> getOnlineAnalysis2_0(Integer type, String uuid) throws Exception;

    /**
     * 获取在线对比复盘分析信息2_0
     * @param contrastId 对比id
     * @return
     */
    R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis2_0(String contrastId) throws Exception;

    /**
     * 获取分析文件的下载链接地址
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    R<String> getAnalysisDownloadUrl(Integer type, String uuid);

    /**
     * 获取在线复盘分析信息，以zip形式返回2_1
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    byte[] getOnlineAnalysisZip2_1(Integer type, String uuid) throws Exception;

    /**
     * 获取在线对比复盘分析信息，以zip形式返回2_1
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    byte[] getOnlineContrastAnalysisZip2_1(String contrastId) throws Exception;

    /**
     * 获取在线复盘分析信息2_1
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    R<AnalysisResultCloudVo> getOnlineAnalysis2_1(Integer type, String uuid) throws Exception;

    /**
     * 获取在线对比复盘分析信息2_1
     * @param contrastId 对比id
     * @return
     */
    R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis2_1(String contrastId) throws Exception;

    /**
     * 获取视频弹幕数据
     * @param videoId
     * @return
     */
    R<BarrageDataVo> videoBarrageData(String videoId);

    /**
     * 获取分析结果
     * @param type
     * @param uuid
     * @return
     */
    R<AnalysisResultCloudVo> getAnalysis(Integer type, String uuid);

    /**
     * 获取对比分析结果
     * @param contrastId
     * @return
     */
    R<AnalysisContractResultCloudVo> getContrastAnalysis(String contrastId);

    /**
     * 查询视频ROI信息
     * @param videoId 视频唯一标识
     * @return
     */
    R<VideoRoiVo> getVideoRoi(String videoId);
}

