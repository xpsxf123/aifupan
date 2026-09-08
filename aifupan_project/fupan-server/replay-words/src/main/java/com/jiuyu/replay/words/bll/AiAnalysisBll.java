package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.AiAnalysisBo;
import com.jiuyu.replay.words.bo.AiAnalysisListBo;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.producer.AiAnalysisProducer;
import com.jiuyu.replay.words.producer.SensitiveWordsProducer;
import com.jiuyu.replay.generic.vo.words.AiAnalysisInfoVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisListVo;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * AI分析表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
@Component
public class AiAnalysisBll {

    @Resource
    private AiAnalysisProducer aiAnalysisProducer;
    @Resource
    private SensitiveWordsProducer sensitiveWordsProducer;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private AiOssUtils aiOssUtils;

    /**
     * AI分析表列表
     * @param aiAnalysisListBo AI分析表列表查询参数
     * @return
     */
    public R<PageUtils<AiAnalysisListVo>> queryPage(AiAnalysisListBo aiAnalysisListBo) {
        return R.ok("获取成功", aiAnalysisProducer.queryPage(aiAnalysisListBo));
    }

    /**
    * AI分析表信息
    * @param id AI分析表id
    * @return
    */
    public R<AiAnalysisInfoVo> info(Long id) {
        AiAnalysisInfoVo aiAnalysisInfoVo = aiAnalysisProducer.info(id);
        return R.ok("获取成功", aiAnalysisInfoVo);
    }

    /**
     * 新增AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
    public R<String> save(AiAnalysisBo aiAnalysisBo) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date());

        if (aiAnalysisBo.getTextType() == 0){
            // 获取最后一条记录
            int sort =  aiAnalysisProducer.LastSort(aiAnalysisBo.getUuid());
            Long SfID = SnowflakeManager.nextValue();
            String storePath = "";
            if (aiAnalysisBo.getType() == 0){
                storePath = "video/" + dateStr + "/" +  aiAnalysisBo.getUuid() + "/" + sort + ".txt";
            }else if (aiAnalysisBo.getType() == 1){
                storePath = "file/" + dateStr + "/" +  aiAnalysisBo.getUuid() + "/" + sort + ".txt";
            }
            String jsonString = JSON.toJSONString(aiAnalysisBo.getPromptWords());
//            String fileName = sensitiveWordsProducer.AiContentFileName(storePath,jsonString);
//            aiAnalysisBo.setStoreFileName(fileName);
            String ossKey = aiOssUtils.uploadToString(storePath, jsonString);
            aiAnalysisBo.setOssKey(ObjectUtil.defaultIfNull(ossKey, ""));
            aiAnalysisBo.setId(SfID);
            aiAnalysisBo.setSort(sort);
            AiAnalysisInfoVo aiAnalysisInfoVo = aiAnalysisProducer.save(aiAnalysisBo);
        }
        Integer textType = aiAnalysisBo.getTextType();
        textType++;
        if (textType == 1){
            int sort =  aiAnalysisProducer.LastSort(aiAnalysisBo.getUuid());
            Long SfID = SnowflakeManager.nextValue();
            String storePath = "";
            if (aiAnalysisBo.getType() == 0){
                storePath = "video/" + dateStr + "/" +  aiAnalysisBo.getUuid() + "/" + sort + ".txt";
            }else if (aiAnalysisBo.getType() == 1){
                storePath = "file/" + dateStr + "/" +  aiAnalysisBo.getUuid() + "/" + sort + ".txt";
            }
            String jsonString = JSON.toJSONString(aiAnalysisBo.getAnswerList());
//            String fileName = sensitiveWordsProducer.AiContentFileName(storePath,jsonString);
//            aiAnalysisBo.setStoreFileName(fileName);
            String ossKey = aiOssUtils.uploadToString(storePath, jsonString);
            aiAnalysisBo.setOssKey(ObjectUtil.defaultIfNull(ossKey, ""));
            aiAnalysisBo.setId(SfID);
            aiAnalysisBo.setSort(sort);
            aiAnalysisBo.setTextType(textType);
            AiAnalysisInfoVo aiAnalysisInfoVo = aiAnalysisProducer.save(aiAnalysisBo);
        }
        return R.ok("添加成功");
    }

    /**
     * 修改AI分析表
     * @param aiAnalysisBo AI分析表对象
     * @return
     */
    public R<String> update(AiAnalysisBo aiAnalysisBo) {

        aiAnalysisProducer.update(aiAnalysisBo);
        return R.ok("修改成功");
    }

    /**
     * 删除AI分析表
     * @param id AI分析表id
     * @return
     */
    public R<String> delete(Long id) {

        aiAnalysisProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据唯一标识查询所有AI分析记录
     * @param uuid 唯一标识
     * @return
     */
    public R<List<List<String>>> listAiAnalysisByUuid(String uuid) {
        return aiAnalysisProducer.listAiAnalysisByUuid(uuid);
    }

    /**
     * 根据唯一标识去判断是否有正在进行AI分析的文本;返回true代表正在分析,false则没有
     * @param uuid 唯一标识
     * @return
     */
    public R<String> analysisStatusByUuid(String uuid, String modelId) {
//        String allSessionId = (String) redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey));
        return R.ok("",aiAnalysisProducer.analysisStatusByUuid(uuid, modelId));
    }
}

