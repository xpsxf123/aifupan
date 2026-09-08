package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.words.bo.script.ConfirmStandardScriptBo;
import com.jiuyu.replay.words.bo.script.GenerateStandardScriptBo;
import com.jiuyu.replay.words.entity.StandardScriptEntity;
import com.jiuyu.replay.words.repository.service.StandardScriptService;
import com.jiuyu.replay.words.vo.script.StandardScriptConfirmVo;
import com.jiuyu.replay.words.vo.script.StandardScriptDetailVo;
import com.jiuyu.replay.words.vo.script.StandardScriptVo;
import com.jiuyu.replay.words.vo.script.TimeAxisItemVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 话术还原度标准稿 Bll（T21/T23/T24 主流程）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>T21 generateStandardScript：同步调用 AI（60s 超时），解析 JSON 数组，不落库；</li>
 *   <li>T23 confirmStandardScript：accountType 校验 + UPDATE 旧稿 / INSERT 新稿（同一 secuid 永远只一行）；</li>
 *   <li>T24 standardScriptDetail：按 (tenantId, userId, secUid) 查详情。</li>
 * </ul>
 *
 * <p><b>注意：</b>本类不直接 import AnchorUrlBll，也不被 AnchorUrlBll 直接注入，
 * 两者通过 {@link StandardScriptService} 共享数据访问层，避免 Bll-to-Bll 互调。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@Slf4j
@Component
@AllArgsConstructor
public class ScriptMonitorStandardScriptBll {

    /**
     * 标准稿生成提示词 systemKv key（用户已确认预配置）。
     */
    private static final String STANDARD_SCRIPT_AI_MODEL_KEY = "script_monitor_standard_script_ai_model";

    /**
     * 标准稿生成 cueType（{@link AiEnums.askType#STANDARD_SCRIPT_GENERATE}=27）。
     */
    private static final int STANDARD_SCRIPT_CUE_TYPE = AiEnums.askType.STANDARD_SCRIPT_GENERATE.getCode();

    /**
     * AI 台账 assistantType：话术还原度业务类型（{@link AiEnums.askType#FIDELITY_MONITOR}=24）。
     */
    private static final int ASSISTANT_TYPE_FIDELITY = AiEnums.askType.FIDELITY_MONITOR.getCode();

    /**
     * 通用行业 tradeId 兜底值。
     */
    private static final long TRADE_ID_GENERAL = 1L;

    /**
     * JSON 解析失败重试次数上限（硬编码，禁止通过配置或环境变量调高）。
     * 最多 2 次 AI 调用：首次 + 1 次重试。
     */
    private static final int MAX_JSON_PARSE_RETRY_COUNT = 1;

    /**
     * 去除 AI 输出可能附带的 ```json ... ``` 代码块包裹。
     *
     * <p>cueType=27 提示词明确要求 AI 直接输出 JSON 数组（不加代码块），但实际 AI
     * 偶尔会包一层 markdown 代码块；解析前主动剥离避免 JSON.parseArray 失败。</p>
     */
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
            "^\\s*```(?:json)?\\s*([\\s\\S]*?)\\s*```\\s*$");

    private final StandardScriptService standardScriptService;
    private final CueWordsFeign cueWordsFeign;
    private final AiChatFeign aiChatFeign;
    private final AiModelFeign aiModelFeign;
    private final AiTokenWithholdFeign aiTokenWithholdFeign;
    private final SystemKvProducer systemKvProducer;

    // ====== T21 generateStandardScript ======

    /**
     * T21：同步 AI 生成标准稿时间轴（不落库）。
     *
     * <p>调用顺序：预扣 Token → 加载 AI 模型 → 取提示词 → AI 同步调用（60s 超时）
     * → 解析 JSON 数组（parseJsonOutput） → settle Token → 返回 StandardScriptVo。
     * 任何步骤失败必须调 returnAiToken。</p>
     *
     * @param bo       生成请求入参
     * @param userId   当前用户 ID（从 JWT 取）
     * @param tenantId 当前租户 ID（从 JWT 取）
     * @return 生成结果（不含 standardScriptId）
     * @throws BusinessException 70001 Token 不足 / 70008 AI 调用或解析失败 / 70013 参数校验失败
     */
    public StandardScriptVo generateStandardScript(GenerateStandardScriptBo bo, Long userId, Long tenantId) {
        // 业务参数校验：循环模式必须传 cycleDurationMinutes
        if (Objects.equals(bo.getSpeechMode(), 1) && bo.getCycleDurationMinutes() == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "循环话术模式（speechMode=1）必须传 cycleDurationMinutes");
        }

        log.info("[T21 标准稿生成] 开始 userId={} tenantId={} speechMode={} speechSpeed={}",
                userId, tenantId, bo.getSpeechMode(), bo.getSpeechSpeed());

        // 预扣 Token
        RedisWithholdVo withhold = aiTokenWithholdFeign.withholdAiToken(userId, 20000L);
        log.info("[T21 标准稿生成] 预扣 Token 成功 userId={} tenantId={}", userId, tenantId);

        try {
            // 加载 AI 模型
            AiModelBo modelConfig = loadAiModelByKvKey(STANDARD_SCRIPT_AI_MODEL_KEY);

            // 取提示词（cueType=27，通用行业兜底，tenantId 定制优先）
            CueWordsInfoVo cue = loadSingleCueWord(STANDARD_SCRIPT_CUE_TYPE, "标准稿生成提示词", TRADE_ID_GENERAL, tenantId);

            // 填充占位符
            String prompt = fillPrompt(cue.getProblem(), bo);
            log.info("[T21 标准稿生成] AI 调用开始 userId={} promptLen={}", userId, prompt.length());

            // AI 同步调用（60s 超时由 Feign 客户端配置保证）
            AiMessageBo message = buildAiMessage(prompt, bo.getReferenceScript());
            AiReturnDataVo aiResult = aiChatFeign.chatCompletion(modelConfig, message, userId);
            if (aiResult == null || Objects.equals(aiResult.getStatus(), 1)) {
                log.error("[T21 标准稿生成] AI 返回失败 userId={} tenantId={}", userId, tenantId);
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode(),
                        StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getMsg());
            }
            log.info("[T21 标准稿生成] AI 返回成功 userId={} totalTokens={} contentLen={}",
                    userId, aiResult.getTotalTokens(),
                    aiResult.getContent() == null ? 0 : aiResult.getContent().length());

            // 解析 JSON 数组（精确 catch：仅包裹 parseJsonOutput 这一行）
            // AI 偶尔输出未转义的 "，导致 fastjson2 抛 JsonParseRetryableException(70008)
            // 此时触发最多 MAX_JSON_PARSE_RETRY_COUNT 次重试（每次重新调 AI，共最多 1+MAX_JSON_PARSE_RETRY_COUNT 次调用）
            // content 为空 / list 为空抛的普通 BusinessException(70008) 不触发重试
            AiReturnDataVo lastAiResult = aiResult;
            List<TimeAxisItemVo> items;
            try {
                items = parseJsonOutput(aiResult.getContent(), userId, tenantId);
            } catch (BusinessException e) {
                // 仅重试 fastjson 解析失败（用 instanceof 判类型，比 message.contains 稳健）
                if (e instanceof JsonParseRetryableException) {
                    // for 循环让 MAX_JSON_PARSE_RETRY_COUNT 真正控制重试次数（当前值 = 1，即最多 2 次 AI 调用）
                    int accumulatedRawTokens = safeInt(aiResult.getTotalTokens());
                    AiReturnDataVo retryResult = null;
                    items = null;
                    for (int retryCount = 1; retryCount <= MAX_JSON_PARSE_RETRY_COUNT; retryCount++) {
                        log.warn("[T21 标准稿生成] 解析失败触发重试 userId={} 第{}次", userId, retryCount);
                        // 重发一次 AI 调用（相同参数，无 sleep/backoff）
                        retryResult = aiChatFeign.chatCompletion(modelConfig, message, userId);
                        if (retryResult == null || Objects.equals(retryResult.getStatus(), 1)) {
                            log.warn("[T21 标准稿生成] 重试 AI 调用失败 userId={} retryCount={}", userId, retryCount);
                            throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode(),
                                    StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getMsg());
                        }
                        accumulatedRawTokens += safeInt(retryResult.getTotalTokens());
                        try {
                            // 二次解析（若再失败则 catch 后继续下一轮或抛出，由外层 catch 归还 Token）
                            items = parseJsonOutput(retryResult.getContent(), userId, tenantId);
                            // 以最后一次 AI 结果为基础对象，手动 set 累加后的原始 totalTokens
                            lastAiResult = buildSettleResult(retryResult, accumulatedRawTokens);
                            log.info("[T21 标准稿生成] 解析重试成功 userId={} retryCount={} totalTokens={}",
                                    userId, retryCount, accumulatedRawTokens);
                            break;
                        } catch (BusinessException retryEx) {
                            log.warn("[T21 标准稿生成] 重试后解析仍失败 userId={} 已尝试{}次 errMsg={}",
                                    userId, retryCount, retryEx.getMessage());
                            if (retryCount >= MAX_JSON_PARSE_RETRY_COUNT) {
                                // 已达重试上限，上抛原始异常
                                throw retryEx;
                            }
                        }
                    }
                } else {
                    // 非解析失败（普通 BusinessException / 其他 code）直接上抛
                    throw e;
                }
            }

            // 按 model 系数（×1.5×consumeMultiple）算实扣 token；lastAiResult.totalTokens 已包含重试路径的累加量
            int totalTokens = (int) AiUtils.aiTokenConsumeMultiple(
                    (long) safeInt(lastAiResult.getTotalTokens()), modelConfig.getConsumeMultiple());
            AiReturnDataVo settleResult = buildSettleResult(lastAiResult, totalTokens);
            AiTokenUseRecordBo recordBo = AiTokenUseRecordBo.builder(lastAiResult, modelConfig.getModelName());
            recordBo.setTenantId(tenantId);
            recordBo.setAssistantType(ASSISTANT_TYPE_FIDELITY);
            aiTokenWithholdFeign.settleAiToken(withhold, userId, settleResult, recordBo);
            log.info("[T21 标准稿生成] Token settle 完成 userId={} totalTokens={}", userId, totalTokens);

            // 组装响应
            StandardScriptVo vo = new StandardScriptVo();
            vo.setSpeechMode(bo.getSpeechMode());
            vo.setSpeechSpeed(bo.getSpeechSpeed());
            vo.setCycleDurationMinutes(bo.getCycleDurationMinutes());
            vo.setReferenceScript(bo.getReferenceScript());
            vo.setTimeAxisScript(items);
            return vo;

        } catch (BusinessException e) {
            // 业务异常：归还预扣 Token 后上抛
            safeReturnToken(withhold, userId, tenantId);
            throw e;
        } catch (Exception e) {
            // 未知异常（含超时）：归还预扣 Token 后抛生成失败
            log.error("[T21 标准稿生成] 异常 userId={} tenantId={}", userId, tenantId, e);
            safeReturnToken(withhold, userId, tenantId);
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode(),
                    StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getMsg());
        }
    }

    // ====== T23 confirmStandardScript ======

    /**
     * T23：确认标准稿落库（同一 (tenant, user, secuid) 永远只一行）。
     *
     * <p>流程：findValid 命中则 updateById 内容字段保留原 id；未命中则 INSERT 新稿。
     * standardScriptId 跨 confirm 稳定（首次分配后不变），下游 (tb_anchor_url_user.standard_script_id 等) 无需重新回填。
     * 任意失败整体回滚。</p>
     *
     * @param bo       确认请求入参
     * @param userId   当前用户 ID（从 JWT 取）
     * @param tenantId 当前租户 ID（从 JWT 取）
     * @return 确认结果（含 standardScriptId，首次 INSERT 时新分配，UPDATE 路径返回既存 id）
     */
    @Transactional(rollbackFor = Exception.class)
    public StandardScriptConfirmVo confirmStandardScript(ConfirmStandardScriptBo bo, Long userId, Long tenantId) {
        // 业务参数校验：循环模式必须传 cycleDurationMinutes
        if (Objects.equals(bo.getSpeechMode(), 1) && bo.getCycleDurationMinutes() == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "循环话术模式（speechMode=1）必须传 cycleDurationMinutes");
        }

        log.info("[T23 确认标准稿] 开始 userId={} tenantId={} secUid={}", userId, tenantId, bo.getSecUid());

        // accountType 校验（查到 accountType=1 → 70004；查不到 = 新增直播间场景，允许）
        standardScriptService.validateAccountAndLoadAnchor(tenantId, userId, bo.getSecUid());

        // 同一 (tenant, user, secuid) 永远只保留一行：有就 UPDATE，没有就 INSERT。
        // 不再走"软删旧稿 + INSERT 新稿"的版本快照模式（standardScriptId 在当前代码无下游版本引用——
        // 报告生成走 findValid 按 secuid 重查活跃稿，不按 ID 取历史版本）。
        String timeAxisJson = JSON.toJSONString(bo.getTimeAxisScript());
        StandardScriptEntity existing = standardScriptService.findValid(tenantId, userId, bo.getSecUid());
        Long resultId;
        if (existing != null) {
            existing.setSpeechMode(bo.getSpeechMode());
            existing.setSpeechSpeed(bo.getSpeechSpeed());
            existing.setCycleDurationMinutes(bo.getCycleDurationMinutes());
            existing.setReferenceScript(bo.getReferenceScript());
            existing.setTimeAxisScript(timeAxisJson);
            existing.setUpdateDate(new Date());
            boolean updated = standardScriptService.updateById(existing);
            if (!updated) {
                // updateById 返 false 罕见（entity.id 不存在等），抛 RuntimeException 触发 @Transactional 回滚
                throw new RuntimeException("[T23] updateById 返回 false，id=" + existing.getId());
            }
            resultId = existing.getId();
            log.info("[T23 确认标准稿] UPDATE 完成 id={} userId={} tenantId={} secUid={}",
                    resultId, userId, tenantId, bo.getSecUid());
        } else {
            resultId = SnowflakeManager.nextValue();
            StandardScriptEntity entity = new StandardScriptEntity();
            entity.setId(resultId);
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setSecUid(bo.getSecUid());
            entity.setSpeechMode(bo.getSpeechMode());
            entity.setSpeechSpeed(bo.getSpeechSpeed());
            entity.setCycleDurationMinutes(bo.getCycleDurationMinutes());
            entity.setReferenceScript(bo.getReferenceScript());
            entity.setTimeAxisScript(timeAxisJson);
            entity.setCreateDate(new Date());
            entity.setUpdateDate(new Date());
            entity.setIsDeleted(0);
            standardScriptService.save(entity);
            log.info("[T23 确认标准稿] INSERT 完成 newId={} userId={} tenantId={} secUid={}",
                    resultId, userId, tenantId, bo.getSecUid());
        }

        StandardScriptConfirmVo vo = new StandardScriptConfirmVo();
        vo.setStandardScriptId(resultId);
        return vo;
    }

    // ====== T24 standardScriptDetail ======

    /**
     * T24：按 (tenantId, userId, secUid) 查标准稿详情。
     *
     * <p>无稿返回 hasScript=false + speechSpeed=280，其余字段 null。</p>
     *
     * @param tenantId 当前租户 ID
     * @param userId   当前用户 ID
     * @param secUid   主播唯一标识（Query 参数）
     * @return 详情 Vo
     */
    public StandardScriptDetailVo standardScriptDetail(Long tenantId, Long userId, String secUid) {
        log.info("[T24 标准稿详情] 查询 userId={} tenantId={} secUid={}", userId, tenantId, secUid);

        StandardScriptDetailVo vo = new StandardScriptDetailVo();
        StandardScriptEntity entity = standardScriptService.findValid(tenantId, userId, secUid);

        if (entity == null) {
            vo.setHasScript(false);
            vo.setSpeechSpeed(280);
            log.info("[T24 标准稿详情] 无已确认稿 userId={} secUid={}", userId, secUid);
            return vo;
        }

        // 有稿：反序列化时间轴 JSON
        List<TimeAxisItemVo> items = null;
        if (StrUtil.isNotBlank(entity.getTimeAxisScript())) {
            try {
                items = JSON.parseArray(entity.getTimeAxisScript(), TimeAxisItemVo.class);
            } catch (Exception e) {
                log.warn("[T24 标准稿详情] timeAxisScript 反序列化失败 id={} e={}", entity.getId(), e.getMessage());
            }
        }

        vo.setHasScript(true);
        vo.setStandardScriptId(entity.getId());
        vo.setSecUid(entity.getSecUid());
        vo.setSpeechMode(entity.getSpeechMode());
        vo.setSpeechSpeed(entity.getSpeechSpeed() != null ? entity.getSpeechSpeed() : 280);
        vo.setCycleDurationMinutes(entity.getCycleDurationMinutes());
        vo.setReferenceScript(entity.getReferenceScript());
        vo.setTimeAxisScript(items);
        // createDate ISO-8601 格式化
        if (entity.getCreateDate() != null) {
            vo.setCreateDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(entity.getCreateDate()));
        }
        return vo;
    }

    // ====== 私有辅助方法 ======

    /**
     * 解析 AI 输出的 JSON 数组，反序列化为 {@code List<TimeAxisItemVo>}。
     *
     * <p>cueType=27 提示词约定 AI 直接输出 JSON 数组（{@code [{"timeRange","title","content"}, ...]}）；
     * 解析前主动剥离 {@code ```json ... ```} 代码块包裹（AI 偶尔附带）；
     * 解析失败仅抛 {@link BusinessException}；Token 归还由外层 {@code generateStandardScript}
     * 的 {@code catch (BusinessException)} 统一处理，避免双重 returnAiToken。</p>
     *
     * @param aiContent AI 输出全文
     * @param userId    用户 ID（日志用）
     * @param tenantId  租户 ID（日志用）
     * @return 时间轴条目列表（非空）
     */
    private List<TimeAxisItemVo> parseJsonOutput(String aiContent, Long userId, Long tenantId) {
        if (StrUtil.isBlank(aiContent)) {
            log.error("[T21 parseJsonOutput] AI 输出为空 userId={} tenantId={}", userId, tenantId);
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode(),
                    StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getMsg());
        }

        // 剥离可能的 ```json ... ``` 代码块包裹
        String jsonText = aiContent.trim();
        Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(jsonText);
        if (fenceMatcher.find()) {
            jsonText = fenceMatcher.group(1).trim();
        }

        List<TimeAxisItemVo> items;
        try {
            items = JSON.parseArray(jsonText, TimeAxisItemVo.class);
        } catch (Exception e) {
            log.error("[T21 parseJsonOutput] JSON 反序列化失败 userId={} tenantId={} content前200字={} err={}",
                    userId, tenantId, StrUtil.subPre(jsonText, 200), e.getMessage());
            // 抛 JsonParseRetryableException（BusinessException 子类），让外层 catch instanceof 稳健识别"可重试的 fastjson 解析失败"
            // 与其他两处普通 BusinessException(70008)（content 空 / list 空）区分，后两者不触发重试
            throw new JsonParseRetryableException(
                    StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode(),
                    "标准稿 AI 输出 JSON 解析失败: " + e.getMessage());
        }

        if (items == null || items.isEmpty()) {
            log.error("[T21 parseJsonOutput] 解析到 0 条时间轴条目，AI 输出 JSON 数组为空 userId={} content前200字={}",
                    userId, StrUtil.subPre(jsonText, 200));
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getCode(),
                    StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL.getMsg());
        }

        log.info("[T21 parseJsonOutput] 解析完成 userId={} itemCount={}", userId, items.size());
        return items;
    }

    /**
     * 填充提示词占位符（#{trade} / #{referenceScript} / #{speechSpeed}）。
     *
     * <p>T21 为纯 AI 调用，不查直播间归属，tradeId 用通用兜底（1）。
     * 提示词中 #{referenceScript} 直接嵌入原文，#{speechSpeed} 用语速数值。</p>
     *
     * @param promptTemplate 原始提示词模板（含 #{...} 占位符）
     * @param bo             生成请求入参
     * @return 替换占位符后的提示词
     */
    private String fillPrompt(String promptTemplate, GenerateStandardScriptBo bo) {
        // trade 为 T21 通用，不依赖直播间行业（T21 无归属）；使用通用兜底"通用"
        return promptTemplate
                .replace("#{trade}", "通用")
                .replace("#{speechSpeed}", String.valueOf(bo.getSpeechSpeed()))
                .replace("#{referenceScript}", bo.getReferenceScript());
    }

    /**
     * 构造 AI 调用消息体。
     *
     * @param systemPrompt 系统提示词（已填充占位符的完整提示词）
     * @param userContent  用户内容（参考脚本原文）
     * @return AiMessageBo
     */
    private AiMessageBo buildAiMessage(String systemPrompt, String userContent) {
        AiMessageBo params = new AiMessageBo();
        params.setSystem(List.of(Map.of("text", systemPrompt)));
        params.setUser(List.of(Map.of("text", userContent)));
        return params;
    }

    /**
     * 按 systemKv key 加载 AI 模型，兜底回退到 listDiagnosisModel list[0]。
     *
     * @param kvKey systemKv key
     * @return AiModelBo
     */
    private AiModelBo loadAiModelByKvKey(String kvKey) {
        SystemKvInfoVo kv = systemKvProducer.getByKey(kvKey);
        if (kv == null || StrUtil.isBlank(kv.getKvValue())) {
            log.warn("[标准稿] systemKv 未配置 {}, 回退字典 listDiagnosisModel.get(0)", kvKey);
            return loadFallbackAiModel();
        }
        String modelCode = kv.getKvValue();
        AiModelInfoVo model = aiModelFeign.getByCode(modelCode);
        if (model == null) {
            log.warn("[标准稿] systemKv 配置的 modelCode 在 tb_ai_model 找不到, 回退 list[0]. kvKey={} code={}",
                    kvKey, modelCode);
            return loadFallbackAiModel();
        }
        AiModelBo bo = new AiModelBo();
        BeanUtils.copyProperties(model, bo);
        return bo;
    }

    /**
     * 兜底：取 listDiagnosisModel 字典中第一个模型。
     *
     * @return AiModelBo
     * @throws BusinessException 无可用模型时抛业务异常
     */
    private AiModelBo loadFallbackAiModel() {
        List<AiModelInfoVo> models = aiModelFeign.listDiagnosisModel();
        if (models == null || models.isEmpty()) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "未找到可用 AI 模型配置，请联系管理员");
        }
        AiModelInfoVo info = models.get(0);
        AiModelBo bo = new AiModelBo();
        BeanUtils.copyProperties(info, bo);
        return bo;
    }

    /**
     * 按 cueType + 行业 + 租户加载单条提示词。
     *
     * @param cueType    提示词类型
     * @param promptName 日志中使用的名称
     * @param tradeId    行业 ID
     * @param tenantId   租户 ID
     * @return 命中的提示词
     * @throws BusinessException 无配置时抛业务异常
     */
    private CueWordsInfoVo loadSingleCueWord(int cueType, String promptName, Long tradeId, Long tenantId) {
        CueWordsQueryBo bo = new CueWordsQueryBo();
        bo.setTradeId(tradeId);
        bo.setTenantId(tenantId);
        bo.setCueType(cueType);
        CueWordsInfoVo cue = cueWordsFeign.getCueWordByTradeAndType(bo);
        if (cue == null) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    promptName + "未配置（tradeId=" + tradeId + ", tenantId=" + tenantId + "），请联系管理员");
        }
        return cue;
    }

    /**
     * 构造结算用 AiReturnDataVo（totalTokens 设为本次全流程累积量）。
     *
     * @param lastResult  最后一次 AI 调用返回（复制元数据）
     * @param totalTokens 本次全流程消耗的 Token 总量
     * @return 结算用结果
     */
    private AiReturnDataVo buildSettleResult(AiReturnDataVo lastResult, int totalTokens) {
        AiReturnDataVo settle = new AiReturnDataVo();
        BeanUtils.copyProperties(lastResult, settle);
        settle.setTotalTokens(totalTokens);
        return settle;
    }

    /**
     * 安全归还预扣 Token（失败时仅记录 error 日志，不吞原始异常）。
     *
     * @param withhold 预扣凭据
     * @param userId   用户 ID（日志）
     * @param tenantId 租户 ID（日志）
     */
    private void safeReturnToken(RedisWithholdVo withhold, Long userId, Long tenantId) {
        try {
            aiTokenWithholdFeign.returnAiToken(withhold);
        } catch (Exception re) {
            log.error("[标准稿] returnAiToken 失败 userId={} tenantId={}", userId, tenantId, re);
        }
    }

    /**
     * 安全转换 Integer 为 int（null 返回 0）。
     *
     * @param val Integer 值
     * @return int 值，null 时返回 0
     */
    private int safeInt(Integer val) {
        return val == null ? 0 : val;
    }

    /**
     * 标准稿 AI 输出 JSON 解析失败（可重试）。
     *
     * <p>继承 {@link BusinessException} 保持外层错误契约（getCode() 返回 70008 / 前端收同一错误码），
     * 但通过子类型让 outer catch 用 {@code instanceof} 稳健识别"fastjson 解析失败可重试"路径，
     * 避免 {@code message.contains("JSON parse")} 字符串匹配的脆弱性。</p>
     *
     * <p>content 为空 / list 为空抛的是普通 {@link BusinessException}，不属本类，不触发重试。</p>
     */
    static class JsonParseRetryableException extends BusinessException {
        private static final long serialVersionUID = 1L;

        /**
         * @param code 错误码（通常为 70008 SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL）
         * @param msg  错误消息
         */
        public JsonParseRetryableException(int code, String msg) {
            super(code, msg);
        }
    }
}
