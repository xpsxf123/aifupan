package com.jiuyu.replay.ai.bll;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.ai.bo.DiagnosisSignUploadUrlBo;
import com.jiuyu.replay.ai.bo.SaveDiagnosisBo;
import com.jiuyu.replay.ai.bo.conversationByCueWordsIdsBo;
import com.jiuyu.replay.ai.rse.ConversationRse;
import com.jiuyu.replay.ai.rse.DiagnosisCueRse;
import com.jiuyu.replay.ai.rse.DiagnosisModelRse;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.redisOperate.GenerateHtmlRedisOPerate;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueListBo;
import com.jiuyu.replay.generic.bo.ai.SaveDiagnosisCueBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.third.PropertiesFeign;
import com.jiuyu.replay.generic.feign.words.*;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.*;
import com.jiuyu.replay.generic.vo.ai.DataDiagnosisStatusVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.words.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jiuyu.replay.common.utils.VersionUtil;
import java.util.*;
import java.util.stream.Collectors;


/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Slf4j
@Component
@AllArgsConstructor
public class DiagnosisCueBll {

    private final DiagnosisCueRse diagnosisCueRse;
    private final AiOssUtils aiOssUtils;
    private final UserFeign userFeign;
    private final AnchorVideoFeign anchorVideoFeign;
    private final AnchorVideoDetailFeign anchorVideoDetailFeign;
    private final DiagnosisModelRse diagnosisModelRse;
    private final AiModelFeign aiModelFeign;
    private final PropertiesFeign propertiesFeign;
    private final AnchorUrlFeign anchorUrlFeign;
    private final TradeFeign tradeFeign;
    private final CueWordsFeign cueWordsFeign;
    private final ConversationRse conversationRse;
    private final OssUtils ossUtils;
    private final SystemKvProducer systemKvProducer;
    private final DictDataFeign dictDataFeign;
    private final GenerateHtmlRedisOPerate generateHtmlRedisOPerate;
    private final VideoDataViewingFeign videoDataViewingFeign;

    public R<SignUploadUrlVo> getDiagnosisSignUploadUrl(DiagnosisSignUploadUrlBo urlBo) {
        return R.ok("获取成功", aiOssUtils.getSignUploadUrl(getDiagnosisOssKey(urlBo.getSourceId(), null, urlBo.getUploadType())));
    }

    /**
     * 获取诊断报告的下载地址
     *
     * @param videoId
     * @param sourceType
     * @param uploadType
     * @return
     */
    public R<String> getDiagnosisDownloadUrl(String videoId, Integer sourceType, Integer uploadType) {
        Date createDate = null;
        String fileName = null;
        if (ObjectUtil.equals(WordsEnum.sourceType.VIDEO.getCode(), sourceType)) {
            AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoId));
            RRException.isNotEmpty(video, "视频信息获取失败");
            RRException.isNotEmpty(video.getCreateDate(), "视频创建时间获取失败");

            AnchorVideoDetailInfoVo result = anchorVideoDetailFeign.getAndSave(video.getVideoId());
            RRException.isNotEmpty(result, "视频详情获取失败");

            createDate = result.getCreateDate();
            fileName = ObjectUtil.equals(uploadType, 2) ? result.getDataDiagnosisOssName() : result.getDiagnosisOssName();
        } else if (ObjectUtil.equals(WordsEnum.sourceType.FILE.getCode(), sourceType)) {
            throw new BusinessException("当前类型每诊断报告");
        }

        if (fileName == null || createDate == null) {
            RRException.create("诊断报告获取失败");
        }

        String diagnosisOssKey = getDiagnosisOssKey(videoId, createDate, uploadType);
        String signDownloadUrl = aiOssUtils.getSignDownloadUrl(ossUtils.setOssKey(diagnosisOssKey), fileName);
        RRException.isNotEmpty(signDownloadUrl, "诊断报告获取失败");
        return R.ok("获取成功", signDownloadUrl);
    }

    /**
     * 获取诊断报告的ossKey
     * @param videoId 视频id
     * @param createDate 创建时间
     * @param uploadType 类型 1-内容 2-数据
     * @return
     */
    public String getDiagnosisOssKey(String videoId, Date createDate, Integer uploadType) {
        if (createDate == null){
            AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoId));
            RRException.isNotEmpty(video, "视频信息获取失败");
            RRException.isNotEmpty(video.getCreateDate(), "视频创建时间获取失败");
            createDate = video.getCreateDate();
        }
        String prefix = AiOssUtils.diagnosisPrefix;
        if (ObjectUtil.equals(uploadType, 2)) {
            prefix = AiOssUtils.dataDiagnosisPrefix;
        }
        return prefix + DateUtil.format(createDate, "/yyyy/MM/dd/") + videoId + ".pdf";
    }

    /**
     * 获取要自动提问的诊断问题
     * @param videoId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<List<DiagnosisCueInfoVo>> getAutoDiagnosisQuestions(String videoId) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");

        AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoId));

        RRException.isNotEmpty(video, "视频不存在");

        List<DiagnosisCueInfoVo> result = new ArrayList<>();

        AnchorUrlUserVo userAnchorBySecUid = anchorUrlFeign.getUserAnchorBySecUid(video.getSecUid(), user.getId(), user.getActiveTenantId());

        if (ObjectUtil.equals(userAnchorBySecUid.getIsAutoDiagnosis(), 1)) {
            diagnosisModelRse.addVideoDiagnosisModel(video.getSecUid(), videoId, user.getId(), user.getActiveTenantId(), AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode());
            result.addAll(diagnosisCueRse.setAutoDiagnosisQuestions(videoId, video.getSecUid()));
        }

        Integer num = ObjectUtil.defaultIfNull(userAnchorBySecUid.getDiagnosisGenerateNum(), 0);
        if (ObjectUtil.equals(userAnchorBySecUid.getIsDataDiagnosis(), 1) && num > 0) {


            // 次数
            num = num - (int) generateHtmlRedisOPerate.size(video.getUserId(), video.getTenantId(), video.getSecUid());

            // 是否有看板
            boolean hasBoard = videoDataViewingFeign.hasBoard(videoId);

            if (num > 0 && hasBoard) {
                // 设置模型
                diagnosisModelRse.addVideoDiagnosisModel(video.getSecUid(), videoId, user.getId(), user.getActiveTenantId(), AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode());

                // 设置问题
                CueWordsPageBo listBo = new CueWordsPageBo();
                listBo.setSourceId(videoId);
                listBo.setSourceType(WordsEnum.sourceType.VIDEO.getCode());
                listBo.setCueType(AiEnums.askType.DATA_DIAGNOSIS.getCode());
                listBo.setTradeId(video.getTradeId());
                listBo.setPage(1);
                listBo.setLimit(systemKvProducer.getValueByKey("content_diagnosis_prompt_limit", 10));
                PageUtils<CueWordsListVo> pageUtils = cueWordsFeign.pageCueWords(listBo);
                if (ObjectUtil.isNotEmpty(pageUtils)) {
                    CueWordsListVo cueWordsListVo = pageUtils.getList().get(0);
                    List<DiagnosisCueInfoVo> list = diagnosisCueRse.setAutoDataDiagnosisQuestions(videoId, video.getSecUid(), List.of(cueWordsListVo.getId()));
                    if (list != null && !list.isEmpty()) {
                        list.forEach(item -> item.setCueType(AiEnums.askType.DATA_DIAGNOSIS.getCode()));
                        result.addAll(list);
                        // 设置生成标志
                        generateHtmlRedisOPerate.save(video.getUserId(), video.getTenantId(), video.getSecUid(), videoId);
                    }
                }
            }
        }


        return R.ok(result);
    }

    /**
     * 查询视频是否需要生成诊断报告
     *
     * @param sourceId
     * @param diagnosisType
     * @return
     */
    public R<String> isGenerateDiagnosisFile(String sourceId, Integer diagnosisType) {
        return R.ok("成功", diagnosisCueRse.isGenerateDiagnosisFile(sourceId, diagnosisType));
    }

    /**
     * 诊断分析-提示词添加分析
     * @param saveDiagnosisBo
     */
    public List<DiagnosisCueInfoVo> saveDiagnosis(SaveDiagnosisBo saveDiagnosisBo) {
        return diagnosisCueRse.saveDiagnosis(saveDiagnosisBo);
    }

    /**
     * 获取用户待分析和分析中的诊断报告
     * @return
     */
    public List<DiagnosisCueInfoVo> handleDiagnosisByUser() {
        return diagnosisCueRse.handleDiagnosisByUser();
    }

    public R<String> updateDiagnosisCueStatus(DiagnosisCueBo diagnosisCueBo) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        DiagnosisCueBo bo = new DiagnosisCueBo();
        bo.setId(diagnosisCueBo.getId());
        bo.setQaStatus(diagnosisCueBo.getQaStatus());
        bo.setErrorContent(diagnosisCueBo.getQaStatus() == 3 ? diagnosisCueBo.getErrorContent() : "");
        bo.setUpdateUserId(user.getId());
        if (diagnosisCueBo.getQaStatus() == 1) {
            bo.setQaHandleTime(new Date());
        }
        diagnosisCueRse.update(bo);

        if (diagnosisCueBo.getQaStatus() == 2) {
            minusDataDiagnosisGenerateNum(diagnosisCueBo);
        }

        return R.ok("更新成功");
    }

    private void minusDataDiagnosisGenerateNum(DiagnosisCueBo diagnosisCueBo) {
        DiagnosisCueInfoVo info = diagnosisCueRse.info(diagnosisCueBo.getId());
        // 查询是否是数据诊断-如果是数据诊断
        if (ObjectUtil.equals(info.getDiagnosisType(), 1)) {
            if (ObjectUtil.equals(info.getSourceType(), AiEnums.diagnosisSourceType.VIDEO.getCode())) {
                AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(info.getSourceId()));
                if (video != null) {
                    Long num = generateHtmlRedisOPerate.delete(video.getUserId(), video.getTenantId(), video.getSecUid(), info.getSourceId());
                    if (num != null && num > 0) {
                        // 减少数据诊断生成数量
                        anchorUrlFeign.minusDataDiagnosisGenerateNum(video.getSecUid(), info.getSourceId(), video.getUserId(), video.getTenantId(), num.intValue());
                    }
                }
            }
        }
    }

    public R<AiModelInfoVo> aiModelBySourceIdAndType(String sourceId, Integer sourceType, Integer diagnosisType, String webVersion) {
        AiModelInfoVo result = null;
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        DiagnosisModelInfoVo bySource = diagnosisModelRse.getBySource(sourceId, sourceType, user.getActiveTenantId(), user.getId(), diagnosisType);
        if (ObjectUtil.isNotEmpty(bySource)){
            Long modelId = bySource.getModelId();
            if (modelId != null){
                result = aiModelFeign.info(modelId);
            }
        }
        if (result == null){
            String modelCode = resolveDefaultModelCode(webVersion);
            AiModelInfoVo infoVoR = aiModelFeign.getByCode(modelCode);
            if (infoVoR == null){
                RRException.create("ai模型配置获取失败");
            }
            result = infoVoR;
        }
        result.setApiKey(null);
        result.setEndpointId(null);

        // 设置aiModel
        result.setAiModel(aiModelFeign.getAiModel(result.getModelCode()));
        return R.ok(result);
    }

    /**
     * ai诊断提示词配置列表
     * @param diagnosisCueListBo ai诊断提示词配置列表查询参数
     * @return
     */
    public R<PageUtils<DiagnosisCueListVo>> queryPage(DiagnosisCueListBo diagnosisCueListBo) {

        return R.ok("获取成功", diagnosisCueRse.queryPage(diagnosisCueListBo));
    }

    /**
    * ai诊断提示词配置信息
    * @param id ai诊断提示词配置id
    * @return
    */
    public R<DiagnosisCueInfoVo> info(Long id) {

        DiagnosisCueInfoVo diagnosisCueInfoVo = diagnosisCueRse.info(id);
        return R.ok("获取成功", diagnosisCueInfoVo);
    }

    /**
     * 新增ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    public R<String> save(DiagnosisCueBo diagnosisCueBo) {

        DiagnosisCueInfoVo diagnosisCueInfoVo = diagnosisCueRse.save(diagnosisCueBo);
        return R.ok("添加成功");
    }

    /**
     * 修改ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    public R<String> update(DiagnosisCueBo diagnosisCueBo) {

        diagnosisCueRse.update(diagnosisCueBo);
        return R.ok("修改成功");
    }

    /**
     * 删除ai诊断提示词配置
     * @param id ai诊断提示词配置id
     * @return
     */
    public R<String> delete(Long id) {

        diagnosisCueRse.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据客户端资源id和类型获取ai诊断提示词配置
     * @param sourceId
     * @param sourceType
     * @return
     */
    public List<DiagnosisCueInfoVo> listBySource(String sourceId, Integer sourceType, Integer diagnosisType, Long tenantId, Long userId) {

        return diagnosisCueRse.listDiagnosis(sourceId, sourceType, diagnosisType, tenantId, userId);
    }

    /**
     * 保存ai诊断中的模型设置
     * @param saveDiagnosisCueBo
     * @return
     */
    public R<String> saveDiagnosisCue(SaveDiagnosisCueBo saveDiagnosisCueBo) {
        RRException.isNotEmpty(saveDiagnosisCueBo.getSourceId(), "来源id不能为空");
        RRException.isNotEmpty(saveDiagnosisCueBo.getSourceType(), "来源类型不能为空");
        RRException.isNotEmpty(saveDiagnosisCueBo.getModelId(), "模型id不能为空");

        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        saveDiagnosisCueBo.setUserId(user.getId());
        saveDiagnosisCueBo.setTenantId(user.getActiveTenantId());
        saveDiagnosisCueBo.setUpdateUserId(user.getId());
        saveDiagnosisCueBo.setCreateUserId(user.getId());
        if (saveDiagnosisCueBo.getDiagnosisType() == null) {
            saveDiagnosisCueBo.setDiagnosisType(AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode());
        }

        diagnosisCueRse.saveDiagnosisCue(saveDiagnosisCueBo);
        return R.ok();
    }

    public void handleDiagnosis(String sourceId, Integer sourceType) {
        // 判断分析中的是否超时
        diagnosisCueRse.handleDiagnosis(sourceId, sourceType);
    }

    public R<AiDiagnosisCueVo> listDiagnosis(String sourceId, Integer sourceType, Long tradeId, String webVersion) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        if (user == null) return R.ok(new AiDiagnosisCueVo());

        // 判断分析中的是否超时
        diagnosisCueRse.handleDiagnosis(sourceId, sourceType);

        String secUid = null, videoId = null;
        // 查询主播或者视频信息
        if (ObjectUtil.isNotEmpty(sourceId)){
            if (sourceType == 0){
                secUid = sourceId;
                AnchorUrlUserVo data = anchorUrlFeign.getUserAnchorBySecUid(sourceId, user.getId(), user.getActiveTenantId());
                RRException.isNotEmpty(data, "获取主播信息失败");
            }else if (sourceType == 1){
                videoId = sourceId;
                R<AnchorVideoInfoVo> videoInfoVoR = anchorVideoFeign.GetByVideoId(sourceId);
                RRException.isNotEmpty(videoInfoVoR.getData(), "获取视频信息失败");
                AnchorVideoInfoVo data = videoInfoVoR.getData();
                secUid = data.getSecUid();
            }else{
                RRException.create("未知类型");
            }
        }
        // 查询主播的诊断提示词
        Map<Long, List<DiagnosisCueInfoVo>> anchorMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(secUid)){
            anchorMap = diagnosisCueRse.listDiagnosis(secUid, 0, AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode(), user.getActiveTenantId(), user.getId())
                    .stream().collect(Collectors.groupingBy(DiagnosisCueVo::getCueWordsId));
        }

        // 查询视频的诊断提示词
        Map<Long, List<DiagnosisCueInfoVo>> videoMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(videoId)){
            videoMap = diagnosisCueRse.listDiagnosis(videoId, 1, AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode(), user.getActiveTenantId(), user.getId())
                    .stream().collect(Collectors.groupingBy(DiagnosisCueVo::getCueWordsId));
        }

        List<DictDataListVo> diagnosisCueType = dictDataFeign.dictDataListByCode("diagnosis_cue_type");
        RRException.isNotEmpty(diagnosisCueType, "请先配置字典项：diagnosis_cue_type");
        ArrayList<ListDiagnosisCueVo> result = new ArrayList<>();
        for (DictDataListVo item : diagnosisCueType) {
            ListDiagnosisCueVo cueVo = new ListDiagnosisCueVo();
            cueVo.setCueType(NumberUtil.parseInt(item.getValue(), 0));
            cueVo.setTagName(item.getLabel());
            cueVo.setCueWordsList(new ArrayList<>());

            CueWordsPageBo listBo = new CueWordsPageBo();
            listBo.setSourceId(sourceId);
            listBo.setSourceType(WordsEnum.sourceType.VIDEO.getCode());
            listBo.setCueType(cueVo.getCueType());
            listBo.setPage(1);
            listBo.setLimit(systemKvProducer.getValueByKey("content_diagnosis_prompt_limit", 10));
            PageUtils<CueWordsListVo> pageUtils = cueWordsFeign.pageCueWords(listBo);
            if (ObjectUtil.isNotEmpty(pageUtils) && ObjectUtil.isNotEmpty(pageUtils.getList())){
                List<CueWordsListVo> list = pageUtils.getList();
                if (ObjectUtil.isNotEmpty(list)){
                    Map<Long, List<DiagnosisCueInfoVo>> tempAnchorMap = anchorMap;
                    Map<Long, List<DiagnosisCueInfoVo>> tempVideoMap = videoMap;

                    cueVo.setCueWordsList(list.stream().map(wordsListVo -> {
                        ListDiagnosisCueVo.CueWords cueWords = new ListDiagnosisCueVo.CueWords();
                        cueWords.setCueWordsId(wordsListVo.getId());
                        cueWords.setTradeId(wordsListVo.getTradeId());
                        cueWords.setCueWord(wordsListVo.getCueWord());
                        cueWords.setSort(wordsListVo.getSort());
                        cueWords.setAnchorSelect(0);
                        cueWords.setVideoSelect(0);

                        List<DiagnosisCueInfoVo> anchorSelectList = tempAnchorMap.get(wordsListVo.getId());
                        if (ObjectUtil.isNotEmpty(anchorSelectList)){
                            anchorSelectList.stream()
                                    .filter(val -> val.getSourceType() == 0)
                                    .findFirst()
                                    .ifPresent(diagnosisCueInfoVo2 -> cueWords.setAnchorSelect(ObjectUtil.equal(diagnosisCueInfoVo2.getIsSelected(), 1) ? 1 : 0));
                        }

                        List<DiagnosisCueInfoVo> videoSelectList = tempVideoMap.get(wordsListVo.getId());
                        if (ObjectUtil.isNotEmpty(videoSelectList)){
                            DiagnosisCueInfoVo diagnosisCueInfoVo = videoSelectList.stream()
                                    .filter(val -> val.getSourceType() == 1)
                                    .findFirst().orElse(null);
                            if (diagnosisCueInfoVo != null) {
                                cueWords.setVideoSelect(ObjectUtil.equal(diagnosisCueInfoVo.getIsSelected(), 1) && diagnosisCueInfoVo.getQaStatus() == 2 ? 1 : 0);
                                cueWords.setQaStatus(diagnosisCueInfoVo.getQaStatus());
                                cueWords.setErrorContent(diagnosisCueInfoVo.getErrorContent());
                            }
                        }
                        return cueWords;
                    }).toList());
                }
            }
            if (sourceType == 1){
                List<Long> cueWordsIds = cueVo.getCueWordsList().stream()
                        .filter(cueWords -> ObjectUtil.isEmpty(cueWords.getQaStatus()))
                        .map(ListDiagnosisCueVo.CueWords::getCueWordsId).toList();
                if (ObjectUtil.isNotEmpty(cueWordsIds)){
                    List<Long> wordsIds = conversationRse.existsCueWords(sourceId, 0, user.getId(), user.getActiveTenantId(), cueVo.getCueType(), cueWordsIds);
                    cueVo.getCueWordsList().forEach(cueWords -> {
                        if (wordsIds.contains(cueWords.getCueWordsId())) {
                            cueWords.setQaStatus(2);
                        }
                    });
                }
            }
            result.add(cueVo);
        }

        DiagnosisModelInfoVo bySource = diagnosisModelRse.getBySource(sourceId, sourceType, user.getActiveTenantId(), user.getId(), AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode());

        AiDiagnosisCueVo data = new AiDiagnosisCueVo();
        data.setList(result);
        if (ObjectUtil.isNotEmpty(bySource)){
            data.setModelId(bySource.getModelId());
            AiModelInfoVo info = aiModelFeign.info(bySource.getModelId());
            RRException.isNotEmpty(info, "ai模型配置获取失败");
            data.setModelName(info.getModelName());
        }else{
            String modelCode = resolveDefaultModelCode(webVersion);
            AiModelInfoVo infoVoR = aiModelFeign.getByCode(modelCode);
            if (infoVoR == null){
                RRException.create("ai模型配置获取失败");
            }
            data.setModelId(infoVoR.getId());
            data.setModelName(infoVoR.getModelName());
        }
        return R.ok(data);
    }

    public R<List<AiModelInfoVo>> listDiagnosisModel(String webVersion) {

        List<AiModelInfoVo> list = aiModelFeign.listDiagnosisModel();

        if (ObjectUtil.isNotEmpty(list)){
            // 从字典获取各模型的最低客户端版本要求
            List<DictDataListVo> versionDicts = dictDataFeign.dictDataListByCode("ai_model_min_web_version");
            Map<String, String> modelMinVersionMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(versionDicts)) {
                for (DictDataListVo dict : versionDicts) {
                    if (dict.getLabel() != null && dict.getValue() != null) {
                        modelMinVersionMap.put(dict.getLabel(), dict.getValue());
                    }
                }
            }

            List<AiModelInfoVo> filteredList = list.stream()
                    .filter(item -> shouldShowModel(item.getModelCode(), webVersion, modelMinVersionMap))
                    .collect(Collectors.toList());

            filteredList.forEach(item -> {
                item.setApiKey(null);
                item.setEndpointId(null);
            });
            return R.ok(filteredList);
        }

        return R.ok(new ArrayList<>());
    }

    /**
     * 判断模型是否应对当前客户端显示
     *
     * @param modelCode          模型编码
     * @param clientVersion      客户端版本号
     * @param modelMinVersionMap 模型最低版本要求映射
     */
    private boolean shouldShowModel(String modelCode, String clientVersion, Map<String, String> modelMinVersionMap) {
        String minVersion = modelMinVersionMap.get(modelCode);
        // 字典未配置该模型的最低版本 → 默认显示
        if (minVersion == null || minVersion.isEmpty()) {
            return true;
        }
        // 配置了最低版本但客户端未传版本号 → 不显示
        if (clientVersion == null || clientVersion.isEmpty()) {
            return false;
        }
        // 客户端版本 >= 最低版本才显示
        return VersionUtil.compareVersion(clientVersion, minVersion) >= 0;
    }

    /**
     * 根据客户端版本解析默认模型编码，版本不够时兜底返回 analysis-doubao-deep-seek-R1
     */
    public String resolveDefaultModelCode(String webVersion) {
        SystemKvInfoVo diagnosisAiModelDefault = systemKvProducer.getByKey("diagnosis_ai_model_default");
        if (ObjectUtil.isEmpty(diagnosisAiModelDefault)) {
            RRException.create("ai模型code配置获取失败");
        }
        String modelCode = diagnosisAiModelDefault.getKvValue();
        // 从字典查询该模型的最低版本要求
        DictDataListVo versionConfig = dictDataFeign.dictDataByLabel("ai_model_min_web_version", modelCode);
        if (ObjectUtil.isNotEmpty(versionConfig) && versionConfig.getValue() != null && !versionConfig.getValue().isEmpty()) {
            // 客户端未传版本号 或 版本低于最低要求 → 使用兜底模型
            if (webVersion == null || webVersion.isEmpty() || VersionUtil.compareVersion(webVersion, versionConfig.getValue()) < 0) {
                return "analysis-doubao-deep-seek-R1";
            }
        }
        return modelCode;
    }

    public List<ConversationVo> conversationByCueWordsIds(conversationByCueWordsIdsBo bo) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");

        bo.setUserId(user.getId());
        bo.setTenantId(user.getActiveTenantId());
        if (ObjectUtil.isEmpty(bo.getDiagnosisType())) {
            bo.setDiagnosisType(AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode());
        }
        if (ObjectUtil.isEmpty(bo.getCueWordsIds())){
            List<DiagnosisCueInfoVo> diagnosisCueInfoVos = diagnosisCueRse.listDiagnosis(bo.getVideoId(), 1, bo.getDiagnosisType(), user.getActiveTenantId(), user.getId());
            bo.setCueWordsIds(diagnosisCueInfoVos.stream().map(DiagnosisCueInfoVo::getCueWordsId).toList());
        }
        RRException.isNotEmpty(bo.getCueWordsIds(), "没有问题，不能获取问答记录");
        List<ConversationVo> conversationList = conversationRse.conversationByCueWordsIds(bo);
        // 把对应的诊断报告修改成已读
        if (ObjectUtil.isNotEmpty(conversationList)) {
            diagnosisCueRse.updateReadStatusBySourceAndCueWords(
                    bo.getVideoId(), bo.getCueWordsIds(), bo.getUserId(), bo.getTenantId(),
                    AiEnums.readStatus.READ.getCode());
        }
        return conversationList;
    }

    /**
     * 批量更新已读状态
     *
     * @param ids    诊断报告id列表
     * @param isRead 是否已读，默认1（已读）
     */
    public void updateReadStatus(List<Long> ids, Integer isRead) {
        if (isRead == null) {
            isRead = AiEnums.readStatus.READ.getCode();
        }
        diagnosisCueRse.updateReadStatus(ids, isRead);
    }

    /**
     * 根据id获取问答记录
     * @param sourceIds
     * @param sourceType
     * @param userId
     * @param tenantId
     * @return
     */
    public List<DiagnosisCueInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId) {
        return diagnosisCueRse.listBySourceIds(sourceIds, sourceType, userId, tenantId);
    }

    /**
     * 获取视频处理完成的诊断报告问题
     *
     * @param sourceId
     * @param diagnosisType
     * @return
     */
    public List<DiagnosisCueInfoVo> getHandelSuccessDiagnosis(String sourceId, Integer diagnosisType) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");

        List<DiagnosisCueInfoVo> diagnosisCueInfoVos = diagnosisCueRse.listDiagnosis(sourceId, 1, diagnosisType, user.getActiveTenantId(), user.getId());

        if (ObjectUtil.isNotEmpty(diagnosisCueInfoVos)){

            if (ObjectUtil.equals(diagnosisType, AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode())) {
                return diagnosisCueInfoVos.stream()
                        .filter(val -> val.getQaStatus() == 2).toList();
            }

            return diagnosisCueInfoVos.stream()
                    .filter(val -> ObjectUtil.equal(val.getIsSelected(), 1) && val.getQaStatus() == 2).toList();

        }
        return new ArrayList<>();
    }

    /**
     * 批量删除
     *
     * @param ids id列表
     */
    public void deleteBatch(List<Long> ids) {
        diagnosisCueRse.deleteBatch(ids);
    }

    /**
     * 获取数据诊断未读报告列表
     *
     * @return 未读报告列表
     */
    public R<List<UnreadDiagnosisReportVo>> listUnreadDataDiagnosis(Integer videoSliceType) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");

        List<UnreadDiagnosisReportVo> list = diagnosisCueRse.listUnreadDataDiagnosis(user.getId(), user.getActiveTenantId(), videoSliceType);
        if (ObjectUtil.isEmpty(list)) {
            return R.ok(new ArrayList<>());
        }

        // 收集所有去重的 secUid，批量查询主播信息
        List<String> secUids = list.stream()
                .map(UnreadDiagnosisReportVo::getSecUid)
                .filter(ObjectUtil::isNotEmpty)
                .distinct().toList();
        List<AnchorUrlInfoVo> anchorList = ObjectUtil.isEmpty(secUids)
                ? new ArrayList<>()
                : anchorUrlFeign.listBySecUids(secUids);
        Map<String, AnchorUrlInfoVo> anchorMap = ObjectUtil.isEmpty(anchorList)
                ? new HashMap<>()
                : anchorList.stream().collect(Collectors.toMap(AnchorUrlInfoVo::getSecUid, a -> a, (a, b) -> a));

        // 组装主播信息
        for (UnreadDiagnosisReportVo vo : list) {
            AnchorUrlInfoVo anchorInfo = anchorMap.get(vo.getSecUid());
            if (anchorInfo != null) {
                vo.setAnchorAvatar(anchorInfo.getAnchorAvatar());
                vo.setAnchorName(anchorInfo.getAnchorName());
            }
        }
        return R.ok(list);
    }

    /**
     * 获取视频数据诊断状态与内容
     *
     * @param videoId 视频id
     * @return 数据诊断状态与内容
     */
    public R<DataDiagnosisStatusVo> getDataDiagnosisStatus(String videoId) {
        // 获取视频信息
        AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(videoId));
        RRException.isNotEmpty(video, "视频信息获取失败");

        // 查询该视频的所有数据诊断 cue 记录
        List<DiagnosisCueInfoVo> diagnosisCueList = listBySource(
                videoId,
                AiEnums.diagnosisSourceType.VIDEO.getCode(),
                AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode(),
                video.getTenantId(),
                video.getUserId());

        if (ObjectUtil.isEmpty(diagnosisCueList)) {
            return R.ok(null);
        }

        // 按创建时间降序排序
        diagnosisCueList.sort(Comparator.comparing(DiagnosisCueInfoVo::getCreateDate).reversed());

        // 优先取状态为 生成中 或 生成完成 的第一条
        DiagnosisCueInfoVo matched = diagnosisCueList.get(0);
        for (DiagnosisCueInfoVo e : diagnosisCueList) {
            if (ObjectUtil.equals(e.getQaStatus(), AiEnums.qaStatus.GENERATING.getCode())
                    || ObjectUtil.equals(e.getQaStatus(), AiEnums.qaStatus.SUCCESS.getCode())) {
                matched = e;
                break;
            }
        }

        if (matched == null) {
            return R.ok(null);
        }

        DataDiagnosisStatusVo vo = new DataDiagnosisStatusVo();
        vo.setId(matched.getId());
        vo.setQaStatus(matched.getQaStatus());
        vo.setIsRead(matched.getIsRead());
        vo.setCueWordsId(matched.getCueWordsId());

        // 生成完成时获取诊断回答内容
        if (ObjectUtil.equals(matched.getQaStatus(), AiEnums.qaStatus.SUCCESS.getCode())) {
            String content = conversationRse.getDataDiagnosisContent(
                    videoId, video.getUserId(), video.getTenantId(), matched.getCueWordsId());
            vo.setContent(content);
        }

        return R.ok(vo);
    }
}

