package com.jiuyu.replay.api.task;

import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.third.vo.AiContentListVo;
import com.jiuyu.replay.third.zijie.ZiJieUtils;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.AiAnalysisBo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordBo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.UploadFileAnalysisRecordEntity;
import com.jiuyu.replay.words.entity.VideoAnalysisRecordEntity;
import com.jiuyu.replay.words.vo.AiAnalysisRecordVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class AiAnalysisScheduledTasks {

    @Resource
    private VideoAnalysisRecordBll videoAnalysisRecordBll;
    @Resource
    private UploadFileAnalysisRecordBll uploadFileAnalysisRecordBll;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private ZiJieUtils ziJieUtils;
    @Resource
    private SensitiveWordsBll sensitiveWordsBll;
    @Resource
    private AiAnalysisBll aiAnalysisBll;
    @Resource
    private AiAnalysisRecordBll aiAnalysisRecordBll;
    @Resource
    private TradeBll tradeBll;
    @Resource
    private AiAnalysisSensitiveRelaBll aiAnalysisSensitiveRelaBll;

    /**
     * 定时给录制或文件记录进行AI分析
     * @return
     * @throws ParseException
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0 0 2 * * ?")
    @XxlJob("timingAiAnalysis")
    public R<String> timingAiAnalysis()  throws ParseException {
//        log.info("========定时给录制或文件记录进行AI分析=========");
        List<VideoAnalysisRecordEntity> videoAnalysisRecords =  videoAnalysisRecordBll.yesterdayDateRecord(); // 查询昨天的视频分析记录
        List<UploadFileAnalysisRecordEntity> FileAnalysisRecords = uploadFileAnalysisRecordBll.yesterdayDateRecord(); // 查询昨天的文件分析记录
        List<TradeVo> tradeVoList = tradeBll.listAll().getData();
        Map<Long, TradeVo> tradeVoMap = tradeVoList.stream().collect(Collectors.toMap(TradeVo::getId, t -> t));
        String aiModelRole = "你的身份是顶尖的直播运营操盘手";

        if (videoAnalysisRecords != null && videoAnalysisRecords.size() > 0) {
//            log.info("========视频数量{}=========", videoAnalysisRecords.size());
            for (VideoAnalysisRecordEntity videoAnalysisRecord : videoAnalysisRecords) {
                try {
                    // 获取当前时间
                    LocalTime now = LocalTime.now();
                    // 判断当前时间是否为凌晨5点
                    if (now.getHour() >= 6) {
                        break; // 时间为凌晨5点结束循环
                    }else{
                        if (videoAnalysisRecord != null){
                            // 查询是否已经有ai分析记录
//                        log.info("========查询是否有分析记录{}=========", videoAnalysisRecord.getStoreFileName());
                            R<List<List<String>>> listR = this.aiAnalysisBll.listAiAnalysisByUuid(videoAnalysisRecord.getVideoId());
                            List<List<String>> data = listR.getData();
                            if(data != null && data.size() > 0) {
                                continue;
                            }
//                        log.info("========开始分析{}=========", videoAnalysisRecord.getStoreFileName());

                            // 给提示词拼接上行业
                            String promptWords = "假如你是一个直播脚本信息提取专家，你将仔细分析这场直播的话术脚本，找出其中的关键词和出现的次数。根据以下规则一步步执行：1. 仔细阅读整场直播的直播话术脚本 2. 逐句分析，尽可能多的输出与" + tradeVoMap.get(videoAnalysisRecord.getTradeId()).getName() + "行业的直播内容和直播运营有关的关键词和次数3.再给出关键词前后的观点，以及为哪些内容做铺垫；要求：1、用表格的形式展示，表头为：关键词、出现次数、关键词前观点，关键词后观点2、给出的观点不要太精简，也不要太复杂，每个观点，控制在150字以内";
                            // 获取当条分析记录的json
                            String dataJsonStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecord.getStoreFileName());
                            if(!StringUtils.isEmpty(dataJsonStr)) {
                                // 组装当条分析记录的文本
                                List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(dataJsonStr, SentenceMarkVo.class);
                                StringBuilder aiAnalysisText = new StringBuilder();
                                for (SentenceMarkVo sentenceMarkVo : sentenceMarkVos) {
                                    aiAnalysisText.append(sentenceMarkVo.getContent()).append("\n");
                                }
                                AiContentListVo aiContentListVo = new AiContentListVo();
                                // 判断文本字数是否大于下限和超过上限
                                if (aiAnalysisText.length() > 5000 && aiAnalysisText.length() < 44000){
//                                log.info("========字数小于44000{}=========", videoAnalysisRecord.getStoreFileName());
                                    aiContentListVo = ziJieUtils.douBaoAI(aiModelRole, aiAnalysisText.toString(), promptWords,videoAnalysisRecord.getVideoId());
                                }else if(aiAnalysisText.length() >= 44000){
//                                log.info("========字数大于44000{}=========", videoAnalysisRecord.getStoreFileName());
                                    aiContentListVo = ziJieUtils.douBaoAI(aiModelRole, aiAnalysisText.substring(0,44000), promptWords,videoAnalysisRecord.getVideoId());
                                }

                                if (aiContentListVo.getAiContentList() != null){
//                                log.info("========分析成功{}=========", aiContentListVo.getAiContentList().size());
                                    // 将分析后的关键词数量保存进数据库
                                    AiAnalysisRecordVo aiAnalysisRecordVo =  sensitiveWordsBll.aiAnswerAndSensitiveTotal(aiContentListVo.getAiContentList(), videoAnalysisRecord.getTradeId());
                                    AiAnalysisRecordBo analysisRecordBo = new AiAnalysisRecordBo();
                                    aiAnalysisRecordVo.setRecordType(0);
                                    aiAnalysisRecordVo.setUuid(videoAnalysisRecord.getVideoId());
                                    BeanUtils.copyProperties(aiAnalysisRecordVo,analysisRecordBo);
                                    aiAnalysisRecordBll.save(analysisRecordBo);

                                    // 将分析后的关键词详情存进数据库
                                    List<AiAnalysisSensitiveRelaBo> notMarkWordList = aiAnalysisRecordVo.getNotMarkWordList();
                                    if(notMarkWordList != null && notMarkWordList.size() > 0) {
//                                    log.info("========未存在的关键词集合{}=========", notMarkWordList.size());
                                        for (AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo : notMarkWordList) {
                                            aiAnalysisSensitiveRelaBo.setUuid(videoAnalysisRecord.getVideoId());
                                            aiAnalysisSensitiveRelaBo.setRecordType(0);
                                            aiAnalysisSensitiveRelaBo.setTradeId(videoAnalysisRecord.getTradeId());
                                            aiAnalysisSensitiveRelaBo.setSensitiveType(1);
                                        }
                                        aiAnalysisSensitiveRelaBll.saveBatch(notMarkWordList);
                                    }

                                    // 将分析后的结果保存进数据库
                                    AiAnalysisBo analysisBo = new AiAnalysisBo();
                                    analysisBo.setPromptWords(promptWords);
                                    analysisBo.setAnswerList(aiAnalysisRecordVo.getAiAnswerList());
                                    analysisBo.setContextId(aiContentListVo.getContextResultId());
                                    analysisBo.setUuid(videoAnalysisRecord.getVideoId());
                                    analysisBo.setTextType(0);
                                    analysisBo.setType(0);
                                    aiAnalysisBll.save(analysisBo);
                                }

                            }
                        }
                    }
                }catch (Exception e) {
                    log.info("定时给录制或文件记录进行AI分析发生异常");
                    e.printStackTrace();
                }

            }
        }

        if (FileAnalysisRecords != null && FileAnalysisRecords.size() > 0) {
            for (UploadFileAnalysisRecordEntity fileAnalysisRecord : FileAnalysisRecords) {

                try {
                    // 获取当前时间
                    LocalTime now = LocalTime.now();
                    // 判断当前时间是否为凌晨5点
                    if (now.getHour() >= 6) {
                        break; // 时间为凌晨5点结束循环
                    }else{
                        if (fileAnalysisRecord != null){

                            // 查询是否已经有ai分析记录
                            R<List<List<String>>> listR = this.aiAnalysisBll.listAiAnalysisByUuid(fileAnalysisRecord.getFileId());
                            List<List<String>> data = listR.getData();
                            if(data != null && data.size() > 0) {
                                continue;
                            }

                            // 给提示词拼接上行业
                            String promptWords = "假如你是一个直播脚本信息提取专家，你将仔细分析这场直播的话术脚本，找出其中的关键词和出现的次数。根据以下规则一步步执行：1. 仔细阅读整场直播的直播话术脚本 2. 逐句分析，尽可能多的输出与" + tradeVoMap.get(fileAnalysisRecord.getTradeId()).getName() + "行业的直播内容和直播运营有关的关键词和次数3.再给出关键词前后的观点，以及为哪些内容做铺垫；要求：1、用表格的形式展示，表头为：关键词、出现次数、关键词前观点，关键词后观点2、给出的观点不要太精简，也不要太复杂，每个观点，控制在150字以内";
                            String dataJsonStr = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + fileAnalysisRecord.getStoreFileName());
                            if(!StringUtils.isEmpty(dataJsonStr)) {
                                // 组装当条分析记录的文本
                                List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(dataJsonStr, SentenceMarkVo.class);
                                StringBuilder aiAnalysisText = new StringBuilder();
                                for (SentenceMarkVo sentenceMarkVo : sentenceMarkVos) {
                                    aiAnalysisText.append(sentenceMarkVo.getContent()).append("\n");
                                }

                                AiContentListVo aiContentListVo = new AiContentListVo();
                                // 判断文本字数是否大于下限和超过上限
                                if (aiAnalysisText.length() > 5000 && aiAnalysisText.length() < 44000){
                                    aiContentListVo = ziJieUtils.douBaoAI(aiModelRole, aiAnalysisText.toString(), promptWords,fileAnalysisRecord.getFileId());
                                }else if(aiAnalysisText.length() >= 44000){
                                    aiContentListVo = ziJieUtils.douBaoAI(aiModelRole, aiAnalysisText.substring(0,44000), promptWords,fileAnalysisRecord.getFileId());
                                }

                                if (aiContentListVo.getAiContentList() != null){
                                    // 将分析后的关键词数量保存进数据库
                                    AiAnalysisRecordVo aiAnalysisRecordVo =  sensitiveWordsBll.aiAnswerAndSensitiveTotal(aiContentListVo.getAiContentList(), fileAnalysisRecord.getTradeId());
                                    AiAnalysisRecordBo analysisRecordBo = new AiAnalysisRecordBo();
                                    aiAnalysisRecordVo.setRecordType(1);
                                    aiAnalysisRecordVo.setUuid(fileAnalysisRecord.getFileId());
                                    BeanUtils.copyProperties(aiAnalysisRecordVo,analysisRecordBo);
                                    aiAnalysisRecordBll.save(analysisRecordBo);

                                    // 将分析后的关键词详情存进数据库
                                    List<AiAnalysisSensitiveRelaBo> notMarkWordList = aiAnalysisRecordVo.getNotMarkWordList();
                                    if(notMarkWordList != null && notMarkWordList.size() > 0) {
                                        for (AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo : notMarkWordList) {
                                            aiAnalysisSensitiveRelaBo.setUuid(fileAnalysisRecord.getFileId());
                                            aiAnalysisSensitiveRelaBo.setRecordType(1);
                                            aiAnalysisSensitiveRelaBo.setTradeId(fileAnalysisRecord.getTradeId());
                                            aiAnalysisSensitiveRelaBo.setSensitiveType(1);
                                        }
                                        aiAnalysisSensitiveRelaBll.saveBatch(notMarkWordList);
                                    }

                                    // // 将分析后的结果保存进数据库
                                    AiAnalysisBo analysisBo = new AiAnalysisBo();
                                    analysisBo.setPromptWords(promptWords);
                                    analysisBo.setAnswerList(aiAnalysisRecordVo.getAiAnswerList());
                                    analysisBo.setContextId(aiContentListVo.getContextResultId());
                                    analysisBo.setUuid(fileAnalysisRecord.getFileId());
                                    analysisBo.setTextType(0);
                                    analysisBo.setType(0);
                                    aiAnalysisBll.save(analysisBo);
                                }

                            }
                        }
                    }
                }catch (Exception e) {
                    log.info("定时给录制或文件记录进行AI分析发生异常");
                    e.printStackTrace();
                }


            }
        }

        return R.ok("");
    }
}
