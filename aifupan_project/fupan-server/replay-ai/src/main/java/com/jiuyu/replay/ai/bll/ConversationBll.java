package com.jiuyu.replay.ai.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.ai.bo.UpdateCorrectStatusBo;
import com.jiuyu.replay.ai.bo.UpdateHtmlStatusBo;
import com.jiuyu.replay.ai.bo.UpdateLikesStatusBo;
import com.jiuyu.replay.ai.entity.ConversationEntity;
import com.jiuyu.replay.ai.rse.ConversationRse;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.FileUploadVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.bo.ai.ConversationListBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.order.AiTokenUseRecordFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.feign.third.AiFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoDetailFeign;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.ai.ConversationPage;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/22 下午3:42
 */
@Component
@AllArgsConstructor
@Slf4j
public class ConversationBll {

    @Resource(name = "generateHtmlExecutor")
    private Executor generateHtmlExecutor;

    private final ConversationRse conversationRse;
    private final UserFeign userFeign;
    private final AiTokenUseRecordFeign aiTokenUseRecordFeign;
    private final SensitiveWordsFeign sensitiveWordsFeign;
    private final CueWordsFeign cueWordsFeign;
    private final AiModelFeign aiModelFeign;
    private final SystemKvProducer systemKvProducer;
    private final AiFeign aiFeign;
    private final AiChatFeign aiChatFeign;
    private final ImgOssUtils imgOssUtils;
    private final AnchorVideoDetailFeign anchorVideoDetailFeign;
    private final DictDataFeign dictDataFeign;

    public static final String htmlPrefix = "ai-html";

    /**
     * 保存ai问答记录数据
     * @param dataList
     * @return
     */
    public List<ConversationVo> saveConversationData(List<ConversationBo> dataList) {
        List<Long> cueWordsIds = dataList.stream().map(ConversationBo::getCueWordsId).filter(ObjectUtil::isNotEmpty).distinct().toList();
        Map<Long, Integer> analysisTypeMap = cueWordsIds.isEmpty() ? Map.of() :
                cueWordsFeign.listByIds(cueWordsIds).stream().collect(Collectors.toMap(CueWordsVo::getId, v -> ObjectUtil.defaultIfNull(v.getAnalysisType(), 0)));
        dataList.forEach(bo -> bo.setAnalysisType(bo.getCueWordsId() != null ? analysisTypeMap.getOrDefault(bo.getCueWordsId(), 0) : 0));

        List<ConversationVo> conversationVos = conversationRse.saveAll(dataList);

        // 问答记录完成后的后置处理
        saveConversationDataPost(conversationVos);

        return conversationVos;
    }

    /**
     * 问答记录完成后的后置处理
     *
     * @param list 问答记录数据
     */
    public void saveConversationDataPost(List<ConversationVo> list) {

        if (ObjectUtil.isEmpty(list)) {
            return;
        }

        // 1 获取list中的sourceId，只要sourceType为0的，去重
        // 1.1 内容诊断：askType为字典diagnosis_cue_type中对应的value相等就是内容诊断
        List<DictDataListVo> diagnosisCueType = dictDataFeign.dictDataListByCode("diagnosis_cue_type");
        List<Integer> contentDiagnosisAskTypes = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(diagnosisCueType)) {
            contentDiagnosisAskTypes = diagnosisCueType.stream()
                    .map(item -> NumberUtil.parseInt(item.getValue(), -1))
                    .filter(item -> item != null && item >= 0)
                    .distinct().toList();
        }

        // 获取提示词详情列表
        List<Long> cueWordsIds = list.stream().map(ConversationVo::getCueWordsId).distinct().toList();
        Map<Long, CueWordsInfoVo> cueWordsMap = ObjectUtil.isEmpty(cueWordsIds) ? new HashMap<>() : cueWordsFeign.listByIds(cueWordsIds)
                .stream().collect(Collectors.toMap(CueWordsVo::getId, item -> item, (existing, replacement) -> existing));

        Set<String> contentDiagnosisVideoIds = new HashSet<>();
        Set<String> dataDiagnosisVideoIds = new HashSet<>();

        for (ConversationVo vo : list) {
            // 只处理sourceType为0的（视频）
            if (!ObjectUtil.equals(vo.getSourceType(), WordsEnum.sourceType.VIDEO.getCode()) || ObjectUtil.isEmpty(vo.getSourceId())) {
                continue;
            }
            // cueWordsId不能为空,cueWordsType为系统
            if (vo.getCueWordsId() == null || !ObjectUtil.equals(ObjectUtil.defaultIfNull(vo.getCueWordsType(), AiEnums.cueWordsType.SYSTEM.getCode()), AiEnums.cueWordsType.SYSTEM.getCode())) {
                continue;
            }
            // 判断提示词类型, 不能是定制提示词
            CueWordsInfoVo wordsInfoVo = cueWordsMap.get(vo.getCueWordsId());
            if (wordsInfoVo == null || !ObjectUtil.equals(wordsInfoVo.getTenantId(), 0L)) {
                continue;
            }
            // 1.1 内容诊断
            if (vo.getAskType() != null && contentDiagnosisAskTypes.contains(vo.getAskType())) {
                contentDiagnosisVideoIds.add(vo.getSourceId());
            }
            // 1.2 数据诊断：askType等于AiEnums.askType.AI_CORRECT_CONTENT就数据诊断
            if (ObjectUtil.equals(vo.getAskType(), AiEnums.askType.DATA_DIAGNOSIS.getCode())) {
                dataDiagnosisVideoIds.add(vo.getSourceId());
            }
        }

        // 2 for接口anchorVideoDetailFeign.updateHasDiagnosisReport，sourceId是videoId
        // 2.1 只修改状态，修改成1，name就传null
        for (String videoId : contentDiagnosisVideoIds) {
            try {
                anchorVideoDetailFeign.updateHasDiagnosisReport(videoId, 1, null, null, null);
            } catch (Exception e) {
                log.error("更新内容诊断报告状态失败，videoId={}", videoId, e);
            }
        }

        for (String videoId : dataDiagnosisVideoIds) {
            try {
                anchorVideoDetailFeign.updateHasDiagnosisReport(videoId, null, null, 1, null);
            } catch (Exception e) {
                log.error("更新数据诊断报告状态失败，videoId={}", videoId, e);
            }
        }
    }



    /**
     * 分页查询ai问答记录数据
     * @param listBo
     * @return
     */
    public R<ConversationPage<ConversationVo>> conversationPage(ConversationListBo listBo) {
        ConversationPage<ConversationVo> data = conversationRse.conversationPage(listBo);
        if (ObjectUtil.isNotEmpty(data) && ObjectUtil.isNotEmpty(data.getList())) {
            // 设置html域名
            setHtmlDomainName(data.getList());

            // 检查生成html过期了没
            changeHtmlStatus(data.getList());

            // 检查生成ai纠正过期了没
            changeCorrectStatus(data.getList());
        }
        return R.ok(data);
    }

    /**
     * 检查html生成状态
     *
     * @param list 列表
     */
    private void changeHtmlStatus(List<ConversationVo> list) {
        if (ObjectUtil.isEmpty(list)) {
            return;
        }

        Integer htmlGenerateExpirationTime = systemKvProducer.getValueByKey("html_generate_expiration_time", 20);

        List<ConversationVo> updateList = new ArrayList<>();

        list.forEach(item -> {
            if (ObjectUtil.equals(AiEnums.htmlStatus.GENERATING.getCode(), item.getHtmlStatus()) && ObjectUtil.isNotEmpty(item.getHtmlCreateDate())) {
                DateTime parse = DateUtil.parse(item.getHtmlCreateDate());
                if (parse != null && DateUtil.between(parse, new Date(), DateUnit.MINUTE) > htmlGenerateExpirationTime) {
                    updateList.add(item);
                }
            }
        });

        if (ObjectUtil.isEmpty(updateList)) {
            return;
        }

        List<String> ids = updateList.stream().map(ConversationVo::getId).toList();
        log.info("html生成过期了，准备更新状态：ids={}", ids);

        String errorMsg = "生成超时，状态设为失败";
        conversationRse.updateHtmlStatus(ids, AiEnums.htmlStatus.FAIL.getCode(), errorMsg);

        // 值回显
        updateList.forEach(item -> {
            item.setHtmlStatus(AiEnums.htmlStatus.FAIL.getCode());
            item.setHtmlCreateError(errorMsg);
        });
    }

    /**
     * 检查AI纠正状态
     *
     * @param list 列表
     */
    private void changeCorrectStatus(List<ConversationVo> list) {
        if (ObjectUtil.isEmpty(list)) {
            return;
        }

        Integer correctGenerateExpirationTime = systemKvProducer.getValueByKey("correct_generate_expiration_time", 20);

        List<ConversationVo> updateList = new ArrayList<>();

        list.forEach(item -> {
            if (ObjectUtil.equals(AiEnums.correctStatus.CORRECTING.getCode(), item.getAiCorrectStatus()) && ObjectUtil.isNotEmpty(item.getAiCorrectCreateTime())) {
                long minutesDiff = (System.currentTimeMillis() - item.getAiCorrectCreateTime()) / (1000 * 60);
                if (minutesDiff > correctGenerateExpirationTime) {
                    updateList.add(item);
                }
            }
        });

        if (ObjectUtil.isEmpty(updateList)) {
            return;
        }

        List<String> ids = updateList.stream().map(ConversationVo::getId).toList();
        log.info("ai纠正生成过期了，准备更新状态：ids={}", ids);

        String errorMsg = "纠正超时，状态设为失败";
        conversationRse.updateCorrectStatus(ids, AiEnums.correctStatus.FAIL.getCode(), errorMsg);

        // 值回显
        updateList.forEach(item -> {
            item.setAiCorrectStatus(AiEnums.correctStatus.FAIL.getCode());
            item.setAiCorrectError(errorMsg);
        });
    }

    /**
     * 根据id查询ai问答记录数据
     * @param ids
     * @return
     */
    public R<List<ConversationVo>> listByIds(List<String> ids){
        return R.ok(conversationRse.listByIds(ids));
    }

    public R<String> updateLikesStatus(UpdateLikesStatusBo updateLikesStatusBo) {
        conversationRse.updateLikesStatus(updateLikesStatusBo);
        return R.ok("更新成功");
    }

    public Boolean isExist(ConversationBo conversationBo) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        if (ObjectUtil.isEmpty(conversationBo.getUserId())) {
            conversationBo.setUserId(user.getId());
        }
        if (ObjectUtil.isEmpty(conversationBo.getTenantId())) {
            conversationBo.setTenantId(user.getActiveTenantId());
        }

        return conversationRse.isExist(conversationBo);
    }

    public void exportByQaCode(HttpServletResponse response, List<String> qaCodes) throws IOException {

        List<ConversationEntity> conversationList = conversationRse.listByQaCodes(qaCodes);

        if (ObjectUtil.isEmpty(conversationList)) {
            throw new BusinessException("没有找到数据源，无法导出");
        }

        Map<String, List<ConversationEntity>> listMap = conversationList.stream().collect(Collectors.groupingBy(ConversationEntity::getQaCode));
        Map<String, String> exportMap = new HashMap<>();
        Set<String> keys = listMap.keySet();
        AtomicInteger i = new AtomicInteger();
        listMap.forEach((k, v) -> {
            int temp = i.getAndIncrement();
            ConversationEntity conversationA = v.stream().filter(item -> "A".equals(item.getType())).findFirst().orElse(null);
            if (conversationA != null) {
                String e = exportByData(v);
                String fileName = FileUtil.cleanInvalid((temp + 1) + "、" + conversationA.getContent());
                fileName = fileName.length() > 100 ? fileName.substring(0, 100) : fileName;
                if (ObjectUtil.isNotEmpty(e)) {
                    exportMap.put(fileName + ".md", e);
                }
            }
        });
        if (exportMap.isEmpty()) {
            throw new BusinessException("没有要导出的数据");
        }

        // -------------------------
        // 单文件导出
        // -------------------------
        if (exportMap.size() == 1) {
            Map.Entry<String, String> entry = exportMap.entrySet().iterator().next();

            String fileName = entry.getKey();
            String content = entry.getValue();

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLUtil.encode(fileName));

            // 输出内容
            IoUtil.write(response.getOutputStream(), CharsetUtil.UTF_8, false, content);
            return;
        }

        // -------------------------
        // 多文件 ZIP 打包导出
        // -------------------------
        // 多文件：打包为 zip 流式输出
        String zipName = "export_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".zip";
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLUtil.encode(zipName) + "\"");

        // 注意：使用 java.util.zip.ZipOutputStream
        OutputStream out = response.getOutputStream();
        // 不要在这里用 try-with-resources 去关闭 out（因为 servlet container 管理它），只关闭 ZipOutputStream 将刷新底层流

        try (ZipOutputStream zipOut = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> e : exportMap.entrySet()) {
                String entryName = e.getKey();
                String content = e.getValue();
                ZipEntry zipEntry = new ZipEntry(entryName);
                zipOut.putNextEntry(zipEntry);
                // 写入内容（使用 UTF-8）
                byte[] data = content.getBytes(StandardCharsets.UTF_8);
                zipOut.write(data);
                zipOut.closeEntry();
            }
            zipOut.finish(); // 完成压缩流
            zipOut.flush();
        } catch (IOException ignore) {
        }
        // 只关闭 zipOut（会把底层流 flush，但不一定 close servlet output；大多数 container 可以安全关闭）
    }

    public String exportByData(List<ConversationEntity> conversationList) {
        if (ObjectUtil.isEmpty(conversationList)) {
            return null;
        }
        if (conversationList.size() != 2) {
            throw new BusinessException("数据异常，qaCode查询出来的数据不是2条，size = " + conversationList.size());
        }

        ConversationEntity conversationQ = conversationList.stream().filter(item -> "Q".equals(item.getType())).findFirst().orElse(null);
        if (conversationQ == null) {
            throw new BusinessException("没有找到回到记录");
        }
        if (conversationQ.getCompletionId() == null) {
            throw new BusinessException("completionId为空");
        }
        // 查询reqId
        AiTokenUseRecordInfoVo aiTokenUseRecord = aiTokenUseRecordFeign.getByRequestId(conversationQ.getCompletionId());
        if (aiTokenUseRecord == null) {
            throw new BusinessException("没有找到对应的ai-token使用记录，requestId = " + conversationQ.getCompletionId());
        }
        ConversationEntity conversationA = conversationList.stream().filter(item -> "A".equals(item.getType())).findFirst().orElse(null);
        if (conversationA == null) {
            throw new BusinessException("没有找到提问记录");
        }
        String str = "# 接口调用记录 \n";
        str += "## 一、模型说明 \n";
        str += "| 模型别名          | 模型真实名称                        | \n";
        str += "|  ----  | ----  | \n";
        str += "| Doubao Pro       | doubao1.5-pro-32k-250115         | \n";
        str += "| deepseek R1      | deep-seek-R1                      | \n";
        str += "| Doubao 思考       | doubao-1.5-thinking-pro         | \n";
        str += "| Doubao 综合       | doubao-seed-1.6                   | \n";
        str += "| Doubao 综合新       | doubao-seed-1.8                   | \n";
        str += "\n";

        str += "## 二、调用基础信息 \n";
        str += "| 项目          | 详情                        | \n";
        str += "|  ----  | ----  | \n";
        str += StrUtil.format("| Request ID    | {}         | \n", aiTokenUseRecord.getRequestId());
        str += StrUtil.format("| 调用模型    | {}         | \n", aiTokenUseRecord.getModelName());
        str += StrUtil.format("| 调用时间       | {}          | \n", conversationA.getCreateDate());
        str += "\n";

        str += "## 三、入参 \n";
        str += "```text\n";
        str += StrUtil.format("""
                {
                    model = xxxxx,
                    messages = { role = "user", content = "{}" },
                    thinking = new
                    {
                        type = "enabled"
                    },
                    stream = true,
                    max_tokens = 12288,
                    frequency_penalty = 0.01,
                    stream_options = new
                    {
                        include_usage = true
                    }
                } \n
                """, conversationA.getRealContent());
        str += "```";
        str += "\n";

        str += "## 三、回答 \n";
        str += "在div中包裹的是深度思考内容 如：```<div class='deepThinking'>xxxx</div>``` \n";
        str += "```text \n";
        str += conversationQ.getContent();
        str += "\n```";
        str += "\n";
        return str;
    }

    /**
     * 根据id查询
     *
     * @param id id
     * @return ConversationVo
     */
    public ConversationVo getById(String id) {
        return conversationRse.getById(id);
    }

    /**
     * 更新HTML生成状态
     *
     * @param updateHtmlStatusBo 更新参数
     */
    public void updateHtmlStatus(UpdateHtmlStatusBo updateHtmlStatusBo) {
        conversationRse.updateHtmlStatus(updateHtmlStatusBo);
    }

    /**
     * 获取HTML生成状态
     *
     * @param ids ids
     * @return ConversationVo
     */
    public List<ConversationVo> getHtmlStatus(List<String> ids) {
        List<ConversationVo> data = conversationRse.getHtmlStatus(ids);
        // 设置html域名
        setHtmlDomainName(data);
        return data;
    }

    private void setHtmlDomainName(List<ConversationVo> list) {
        if (ObjectUtil.isNotEmpty(list)) {
            list.forEach(item -> {
                if (ObjectUtil.isNotEmpty(item.getHtmlSavePath())) {
                    item.setHtmlDomainName(imgOssUtils.getAccessUrl());
                }
            });
        }
    }

    /**
     * 服务端生成html-接口
     *
     * @param id id
     */
    public ConversationVo serviceGenerateHtml(String id) {
        ConversationVo conversation = proGenerateHtml(id, AiEnums.htmlType.SERVER.getCode());
        final ConversationVo conversationVo = JSONObject.parseObject(JSONObject.toJSONString(conversation), ConversationVo.class);
        generateHtmlExecutor.execute(() -> serviceGenerateHtml(conversationVo));
        return conversation;
    }

    /**
     * html生成
     *
     * @param conversation 参数
     */
    public void serviceGenerateHtml(ConversationVo conversation) {
        UpdateHtmlStatusBo updateHtmlStatusBo = new UpdateHtmlStatusBo();
        updateHtmlStatusBo.setId(conversation.getId());
        try {
            if (ObjectUtil.isEmpty(conversation.getContent())) {
                log.error("输出内容为空，id={}", conversation.getId());
                throw new BusinessException("输出内容为空，不能生成对应的图表");
            }

            // 获取ai模型配置
            SystemKvInfoVo kv = systemKvProducer.getByKey("html_ai_model_default");
            if (kv == null) {
                log.error("没有设置对应的ai模型，kv={}", "html_ai_model_default");
                throw new BusinessException("没有设置对应的ai模型，请联系管理员");
            }
            AiModelInfoVo aiModel = aiModelFeign.getByCode(kv.getKvValue());
            if (aiModel == null) {
                log.error("没有找到对应的ai模型配置，code={}", kv.getKvValue());
                throw new BusinessException("没有找到对应的ai模型配置，请联系管理员");
            }

            // 调用ai
            AiModelBo modelConfig = BeanUtil.copyProperties(aiModel, AiModelBo.class);
            AiMessageBo params = new AiMessageBo();
            params.setSystem(List.of(Map.of("text", "你是一个html前端专家")));
            params.setUser(List.of(Map.of("text", conversation.getContent())));
            AiReturnDataVo aiReturnDataVo = aiChatFeign.chatCompletion(modelConfig, params, 0L);

            if (aiReturnDataVo.getStatus() != 0) {
                log.error("ai调用失败，code={}", modelConfig.getModelCode());
                throw new BusinessException("ai调用失败");
            }

            // 记录aiToken消耗量
            AiTokenUseRecordBo aiTokenUseRecordBo = AiTokenUseRecordBo.builder(aiReturnDataVo, modelConfig.getModelName());
            aiTokenUseRecordBo.setTenantId(conversation.getTenantId());
            aiTokenUseRecordBo.setUserId(conversation.getUserId());
            aiTokenUseRecordBo.setUseSourceType(AiEnums.useSourceType.CONVERSATION.getCode());
            aiTokenUseRecordBo.setUseSourceId(conversation.getId());
            aiTokenUseRecordBo.setAssistantType(AiEnums.askType.AI_HTML_PROMPT.getCode());
            aiTokenUseRecordFeign.saveAiTokenUseRec(aiTokenUseRecordBo);

            String content = aiReturnDataVo.getContent();

            FileUploadVo fileUploadVo = imgOssUtils.uploadToString(StrUtil.format("{}/{}.html", htmlPrefix, SnowflakeManager.nextValue()), content);
            if (fileUploadVo == null) {
                log.error("上传oss失败");
                throw new BusinessException("上传html内容失败");
            }
            updateHtmlStatusBo.setHtmlSavePath(fileUploadVo.getKey());
            updateHtmlStatusBo.setHtmlStatus(AiEnums.htmlStatus.SUCCESS.getCode());
        } catch (Exception ex) {
            log.error("生成html失败", ex);
            updateHtmlStatusBo.setHtmlStatus(AiEnums.htmlStatus.FAIL.getCode());
            updateHtmlStatusBo.setHtmlCreateError(ex.getMessage());
        } finally {
            updateHtmlStatus(updateHtmlStatusBo);
        }
    }

    /**
     * html生成前的检查和修改状态
     *
     * @param id       id
     * @param htmlType html生成类型
     * @return ConversationVo
     */
    public ConversationVo proGenerateHtml(String id, Integer htmlType) {
        ConversationVo conversation = conversationRse.getById(id);

        if (conversation == null) {
            log.warn("[生成html] 没有找到对应的会话，id = {}", id);
            throw new BusinessException("没有找到对应的会话");
        }

        if (!"Q".equals(conversation.getType()) || ObjectUtil.isEmpty(conversation.getContent())) {
            log.warn("[生成html] 不符合生成html的条件， id = {}", id);
            throw new BusinessException("不符合生成html的条件");
        }

        // 获取行业
        Long tradeId = sensitiveWordsFeign.getTradeId(conversation.getSourceType(), conversation.getSourceId());
        if (tradeId == null) {
            log.warn("[生成html] 没有找到对应的行业，使用全行业，id = {}", id);
            tradeId = 1L;
        }

        // 判断状态
        if (conversation.getHtmlStatus() != null && conversation.getHtmlStatus() == AiEnums.htmlStatus.GENERATING.getCode()) {
            log.warn("[生成html] 不符合生成html的条件， id = {}, status = {}", id, conversation.getHtmlStatus());
            throw new BusinessException("不符合生成html的条件");
        }

        // 获取提示词
        CueWordsInfoVo htmlPrompt = cueWordsFeign.getHtmlPrompt(tradeId, conversation.getSourceId(), conversation.getSourceType());

        if (htmlPrompt == null) {
            log.warn("[生成html] 没有找到对应的提示词，使用默认提示词，id = {}", id);
            throw new BusinessException("没有找到对应的提示词, 请联系管理员");
        }

        // 使用 Hutool 的正则工具
        String str = AiUtils.deleteDeepThinking(conversation.getContent());
        if (ObjectUtil.isEmpty(str)) {
            throw new BusinessException("去掉思考内容后，内容为空");
        }
        // 拼接提示词
        String prompt = getPrompt(htmlPrompt.getProblem(), str);

        conversation.setContent(prompt);

        // 修改状态为生成中
        UpdateHtmlStatusBo updateHtmlStatusBo = new UpdateHtmlStatusBo();
        updateHtmlStatusBo.setId(id);
        updateHtmlStatusBo.setHtmlType(htmlType);
        updateHtmlStatusBo.setHtmlStatus(AiEnums.htmlStatus.GENERATING.getCode());
        updateHtmlStatus(updateHtmlStatusBo);
        conversation.setHtmlStatus(AiEnums.htmlStatus.GENERATING.getCode());
        return conversation;
    }

    /**
     * 平接提示词
     *
     * @param prompt  提示词
     * @param content 内容
     * @return 提示词
     */
    private String getPrompt(String prompt, String content) {
        SystemKvInfoVo kv = systemKvProducer.getByKey("html_system_extra_prompt");
        String temp = "";
        if (kv != null) {
            temp = "\n" + kv.getKvValue();
        }
        return StrUtil.format("{}{} \n\n以下为数据来源：\n{}", prompt, temp, content);
    }

    /**
     * 获取html的模型配置
     *
     * @return AiTempTokenVo
     */
    public AiTempTokenVo getHtmlTempToken() {

        // 获取ai模型配置
        SystemKvInfoVo kv = systemKvProducer.getByKey("html_ai_model_default");
        if (kv == null) {
            throw new BusinessException("没有设置对应的ai模型，请联系管理员");
        }

        return aiFeign.getAnalysisTempToken(kv.getKvValue());
    }

    public void updateUserHtmlFail(Long userId, Long tenantId) {

        conversationRse.updateUserHtmlFail(userId, tenantId);

    }

    public void updateUserCorrectFail(Long userId, Long tenantId) {

        conversationRse.updateUserCorrectFail(userId, tenantId);

    }

    /**
     * 判断提示词是否存在
     *
     * @param sourceId    源id
     * @param sourceType  源类型
     * @param userId      用户id
     * @param tenantId    租户id
     * @param cueType     提示词类型
     * @param cueWordsIds 提示词id
     * @return 提示词id
     */
    public List<Long> existsCueWords(String sourceId, Integer sourceType, Long userId, Long tenantId, Integer cueType, List<Long> cueWordsIds) {
        return conversationRse.existsCueWords(sourceId, sourceType, userId, tenantId, cueType, cueWordsIds);
    }

    /**
     * 根据qaCodes列表查询会话列表
     *
     * @param qaCodes qaCodes
     * @return 会话列表
     */
    public List<ConversationVo> listByQaCodes(List<String> qaCodes) {
        List<ConversationEntity> conversationVoList = conversationRse.listByQaCodes(qaCodes);
        if (ObjectUtil.isEmpty(conversationVoList)) {
            return List.of();
        }
        return BeanUtil.copyToList(conversationVoList, ConversationVo.class);
    }

    // ==================== AI纠正内容相关 ====================

    /**
     * 更新AI纠正状态（客户端调用）
     *
     * @param bo 更新参数
     */
    public void updateCorrectStatus(UpdateCorrectStatusBo bo) {
        // 如果纠错完成且客户端传了纠正后的内容，需要合并思考内容
        if (bo.getAiCorrectStatus() != null
                && bo.getAiCorrectStatus() == AiEnums.correctStatus.CORRECTED.getCode()
                && ObjectUtil.isNotEmpty(bo.getContent())) {
            ConversationVo original = conversationRse.getById(bo.getId());
            if (original != null && ObjectUtil.isNotEmpty(original.getContent())) {
                String thinkingContent = extractDeepThinking(original.getContent());
                if (ObjectUtil.isNotEmpty(thinkingContent)) {
                    bo.setContent(thinkingContent + "\n" + bo.getContent());
                }
            }
        }
        conversationRse.updateCorrectStatus(bo);
    }

    /**
     * 服务端纠正AI内容-接口
     *
     * @param id id
     * @return ConversationVo
     */
    public ConversationVo serviceCorrectAiContent(String id) {
        ConversationVo conversation = proCorrectAiContent(id, AiEnums.correctType.SERVER.getCode());
        final ConversationVo conversationVo = JSONObject.parseObject(JSONObject.toJSONString(conversation), ConversationVo.class);
        generateHtmlExecutor.execute(() -> doServiceCorrectAiContent(conversationVo));
        return conversation;
    }

    /**
     * 服务端纠正AI内容-实际执行
     *
     * @param conversation 参数
     */
    private void doServiceCorrectAiContent(ConversationVo conversation) {
        UpdateCorrectStatusBo updateBo = new UpdateCorrectStatusBo();
        updateBo.setId(conversation.getId());
        try {
            if (ObjectUtil.isEmpty(conversation.getContent())) {
                log.error("[纠正AI内容] 内容为空，id={}", conversation.getId());
                throw new BusinessException("内容为空，不能纠正");
            }

            // 获取ai模型配置（场景类型1：AI问答助手纠正）
            AiContentCorrectionConfigVo config = dictDataFeign.getContentCorrectionConfig(AiEnums.correctionSceneType.CORRECT_AI_ANSWER.getCode());
            if (config == null || StrUtil.isEmpty(config.getModelCode())) {
                log.error("[纠正AI内容] 没有设置对应的ai模型，sceneType=1");
                throw new BusinessException("没有设置对应的ai模型，请联系管理员");
            }
            AiModelInfoVo aiModel = aiModelFeign.getByCode(config.getModelCode());
            if (aiModel == null) {
                log.error("[纠正AI内容] 没有找到对应的ai模型配置，code={}", config.getModelCode());
                throw new BusinessException("没有找到对应的ai模型配置，请联系管理员");
            }

            // 调用ai
            AiModelBo modelConfig = BeanUtil.copyProperties(aiModel, AiModelBo.class);
            AiMessageBo params = new AiMessageBo();
            params.setUser(List.of(Map.of("text", conversation.getContent())));
            log.debug("[纠正AI内容] 调用ai，modelCode={}, params={}", modelConfig.getModelCode(), params);
            AiReturnDataVo aiReturnDataVo = aiChatFeign.chatCompletion(modelConfig, params, 0L);

            if (aiReturnDataVo.getStatus() != 0) {
                log.error("[纠正AI内容] ai调用失败，code={}", modelConfig.getModelCode());
                throw new BusinessException("ai调用失败");
            }

            // 记录aiToken消耗量
            AiTokenUseRecordBo tokenBo = AiTokenUseRecordBo.builder(aiReturnDataVo, modelConfig.getModelName());
            tokenBo.setTenantId(conversation.getTenantId());
            tokenBo.setUserId(conversation.getUserId());
            tokenBo.setUseSourceType(AiEnums.useSourceType.CONVERSATION.getCode());
            tokenBo.setUseSourceId(conversation.getId());
            tokenBo.setAssistantType(AiEnums.askType.AI_CORRECT_CONTENT.getCode());
            aiTokenUseRecordFeign.saveAiTokenUseRec(tokenBo);

            String correctedContent = aiReturnDataVo.getContent();

            // 从数据库获取原始内容，提取思考内容
            ConversationVo originalConversation = conversationRse.getById(conversation.getId());
            String thinkingContent = "";
            if (originalConversation != null && ObjectUtil.isNotEmpty(originalConversation.getContent())) {
                thinkingContent = extractDeepThinking(originalConversation.getContent());
            }

            // 合并思考内容 + 纠正后的内容
            String finalContent = ObjectUtil.isNotEmpty(thinkingContent)
                    ? thinkingContent + "\n" + correctedContent
                    : correctedContent;

            updateBo.setContent(finalContent);
            updateBo.setAiCorrectStatus(AiEnums.correctStatus.CORRECTED.getCode());
        } catch (Exception ex) {
            log.error("[纠正AI内容] 失败", ex);
            updateBo.setAiCorrectStatus(AiEnums.correctStatus.FAIL.getCode());
            updateBo.setAiCorrectError(ex.getMessage());
        } finally {
            conversationRse.updateCorrectStatus(updateBo);
        }
    }

    /**
     * AI纠正前的检查和修改状态
     *
     * @param id          id
     * @param correctType 纠正来源类型
     * @return ConversationVo
     */
    public ConversationVo proCorrectAiContent(String id, Integer correctType) {
        ConversationVo conversation = conversationRse.getById(id);

        if (conversation == null) {
            log.warn("[纠正AI内容] 没有找到对应的会话，id = {}", id);
            throw new BusinessException("没有找到对应的会话");
        }

        if (!"Q".equals(conversation.getType()) || ObjectUtil.isEmpty(conversation.getContent())) {
            log.warn("[纠正AI内容] 不符合纠正条件，id = {}", id);
            throw new BusinessException("不符合纠正的条件");
        }

        // 判断状态
        if (conversation.getAiCorrectStatus() != null && conversation.getAiCorrectStatus() == AiEnums.correctStatus.CORRECTING.getCode()) {
            log.warn("[纠正AI内容] 正在纠错中，id = {}, status = {}", id, conversation.getAiCorrectStatus());
            throw new BusinessException("正在纠错中，请勿重复操作");
        }

        // 从字典获取纠正提示词（场景类型1：AI问答助手纠正）
        AiContentCorrectionConfigVo config = dictDataFeign.getContentCorrectionConfig(AiEnums.correctionSceneType.CORRECT_AI_ANSWER.getCode());
        if (config == null || ObjectUtil.isEmpty(config.getContentPrompt())) {
            log.warn("[纠正AI内容] 没有设置纠正提示词");
            throw new BusinessException("没有设置纠正提示词，请联系管理员");
        }

        // 去掉思考内容
        String contentWithoutThinking = AiUtils.deleteDeepThinking(conversation.getContent());
        if (ObjectUtil.isEmpty(contentWithoutThinking)) {
            throw new BusinessException("去掉思考内容后，内容为空");
        }

        // 拼接提示词 + 内容
        String combinedContent = contentWithoutThinking + "\n\n" + config.getContentPrompt();
        conversation.setContent(combinedContent);

        // 修改状态为纠错中
        UpdateCorrectStatusBo updateBo = new UpdateCorrectStatusBo();
        updateBo.setId(id);
        updateBo.setAiCorrectType(correctType);
        updateBo.setAiCorrectStatus(AiEnums.correctStatus.CORRECTING.getCode());
        conversationRse.updateCorrectStatus(updateBo);
        conversation.setAiCorrectStatus(AiEnums.correctStatus.CORRECTING.getCode());

        return conversation;
    }

    /**
     * 获取纠正的模型配置
     *
     * @return AiTempTokenVo
     */
    public AiTempTokenVo getCorrectTempToken() {
        // 获取ai模型配置（场景类型1：AI问答助手纠正）
        AiContentCorrectionConfigVo config = dictDataFeign.getContentCorrectionConfig(AiEnums.correctionSceneType.CORRECT_AI_ANSWER.getCode());
        if (config == null || StrUtil.isEmpty(config.getModelCode())) {
            throw new BusinessException("没有设置对应的ai模型，请联系管理员");
        }
        return aiFeign.getAnalysisTempToken(config.getModelCode());
    }

    /**
     * 获取AI纠正状态
     *
     * @param ids ids
     * @return ConversationVo
     */
    public List<ConversationVo> getCorrectStatus(List<String> ids) {
        return conversationRse.getCorrectStatus(ids);
    }

    /**
     * 提取思考内容
     *
     * @param content 原始内容
     * @return 思考内容
     */
    private String extractDeepThinking(String content) {
        if (ObjectUtil.isEmpty(content)) {
            return "";
        }
        String regex = "<div\\s+class=['\"]deepThinking['\"][^>]*>.*?</div>";
        List<String> thinkingParts = ReUtil.findAll(regex, content, 0);
        return String.join("", thinkingParts);
    }
}
