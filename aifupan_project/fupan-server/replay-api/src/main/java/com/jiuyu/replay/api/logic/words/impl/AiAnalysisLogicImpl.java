package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.words.AiAnalysisLogic;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RedisOperationUtils;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import com.jiuyu.replay.third.ai.AiGeneralUtils;
import com.jiuyu.replay.third.constant.VolcengineProperties;
import com.jiuyu.replay.third.vo.AiContentListVo;
import com.jiuyu.replay.third.zijie.ZiJieUtils;
import com.jiuyu.replay.words.bll.AiAnalysisBll;
import com.jiuyu.replay.words.bll.SensitiveWordsBll;
import com.jiuyu.replay.words.bo.AiAnalysisBo;
import com.jiuyu.replay.words.bo.AiAnalysisListBo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisInfoVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisListVo;
import com.volcengine.ApiException;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * AI分析表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
@Service
public class AiAnalysisLogicImpl implements AiAnalysisLogic {

    @Resource
    private AiAnalysisBll aiAnalysisBll;
    @Resource
    private ZiJieUtils ziJieUtils;
    @Resource
    private AiGeneralUtils AIGeneralUtils;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SensitiveWordsBll sensitiveWordsBll;
    @Resource
    private DictDataBll dictDataBll;
    @Resource
    private VolcengineProperties volcengineProperties;


    @Override
    public R<PageUtils<AiAnalysisListVo>> queryPage(AiAnalysisListBo aiAnalysisListBo) {

        return aiAnalysisBll.queryPage(aiAnalysisListBo);
    }

    @Override
    public R<AiAnalysisInfoVo> info(Long id) {

        return aiAnalysisBll.info(id);
    }

    @Override
    public R<String> save(AiAnalysisBo aiAnalysisBo) {

        return aiAnalysisBll.save(aiAnalysisBo);
    }

    @Override
    public R<String> update(AiAnalysisBo aiAnalysisBo) {

        return aiAnalysisBll.update(aiAnalysisBo);
    }

    @Override
    public R<String> delete(Long id) {

        return aiAnalysisBll.delete(id);

    }

    /**
     * AI分析内容
     * @param analysisBo Ai分析类
     * @return
     */
    @Override
    public R<List<String>> aiAnalysis(AiAnalysisBo analysisBo) {

        //根据不同的modelId来获取不同的ai模型

//        AiContentListVo aiContentListVo = ziJieUtils.douBaoAI(analysisBo.getAiModelRole(), analysisBo.getAnalysisContent(), analysisBo.getPromptWords(),analysisBo.getUuid());

        AiContentListVo aiContentListVo = AIGeneralUtils.douBaoAI(analysisBo.getAiModelRole(), analysisBo.getAnalysisContent(), analysisBo.getPromptWords(), analysisBo.getUuid(), analysisBo.getModelId());

        // 根据前端传回来的outPutFormat判断是否需要去匹配运营关键词库；0：不需要，1：需要
        if (aiContentListVo != null){
            if (ObjectUtil.isEmpty(analysisBo.getOutPutFormat()) || analysisBo.getOutPutFormat() == 0){
                analysisBo.setAnswerList(aiContentListVo.getAiContentList());
            }else{
                List<String> aiAnswerList =  sensitiveWordsBll.aiAnswerAndSensitiveList(aiContentListVo.getAiContentList());
                analysisBo.setAnswerList(aiAnswerList);
            }
            analysisBo.setContextId(aiContentListVo.getContextResultId());
            aiAnalysisBll.save(analysisBo);
            return R.ok(analysisBo.getAnswerList());
        }else{
            return R.error(45001,"分析超时，结束本次分析");
        }
    }

    /**
     * 根据唯一标识查询所有AI分析记录
     * @param uuid 唯一标识
     * @return
     */
    @Override
    public R<List<List<String>>> listAiAnalysisByUuid(String uuid) {

        return aiAnalysisBll.listAiAnalysisByUuid(uuid);
    }

    /**
     * 根据唯一标识去判断是否有正在进行AI分析的文本;返回true代表正在分析,false则没有
     * @param uuid 唯一标识
     * @return
     */
    @Override
    public R<String> analysisStatusByUuid(String uuid, String modelId) {
        return aiAnalysisBll.analysisStatusByUuid(uuid, modelId);
    }

    @Override
    public R<Set<String>> getAllSessionId() {
        Set<String> ses = RedisOperationUtils.scanKeys("replay:ai-session-id:*");
        return R.ok("",ses);
    }

    @Override
    public R<AiTempTokenVo> getArkTempToken() throws ApiException {
        String arkTempToken = ziJieUtils.getArkTempToken();
        AiTempTokenVo result = new AiTempTokenVo();
        result.setToken(arkTempToken);
        result.setModel(volcengineProperties.getModel());
        result.setMode(volcengineProperties.getMode());
        result.setModelName(volcengineProperties.getModelName());
        result.setTruncationStrategyType(volcengineProperties.getTruncationStrategyType());
        result.setLastHistoryTokens(volcengineProperties.getLastHistoryTokens());
        result.setRollingTokens(volcengineProperties.getRollingTokens());
        result.setContextSaveTime(volcengineProperties.getContextSaveTime());
        result.setMaxSendMessageLength(volcengineProperties.maxSendMessageLength);
        result.setTempTokenSaveTime(redisTemplate.getExpire(RedisCacheKey.aiTempArkTokenCacheKey, TimeUnit.SECONDS));
        return R.ok(result);
    }
}