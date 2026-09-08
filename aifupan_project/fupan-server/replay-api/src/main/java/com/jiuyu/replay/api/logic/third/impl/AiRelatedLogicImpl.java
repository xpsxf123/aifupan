package com.jiuyu.replay.api.logic.third.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.jiuyu.replay.ai.bll.AiRelatedBll;
import com.jiuyu.replay.ai.bll.ConversationBll;
import com.jiuyu.replay.ai.bll.CustPromptBll;
import com.jiuyu.replay.ai.bll.GlobalProblemBll;
import com.jiuyu.replay.ai.factory.ModelFactoryUtils;
import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.ai.vo.AiPromptWordVo;
import com.jiuyu.replay.ai.vo.GlobalProblemListVo;
import com.jiuyu.replay.api.logic.ai.SentenceMark;
import com.jiuyu.replay.api.logic.ai.factory.AiFactoryUtils;
import com.jiuyu.replay.api.logic.ai.impl.BarrageSentenceImpl;
import com.jiuyu.replay.api.logic.ai.placeholder.PlaceholderContext;
import com.jiuyu.replay.api.logic.ai.placeholder.PlaceholderEnum;
import com.jiuyu.replay.api.logic.ai.placeholder.PlaceholderResolver;
import com.jiuyu.replay.api.logic.ai.placeholder.PlaceholderToken;
import com.jiuyu.replay.api.logic.third.AiRelatedLogic;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.feign.words.VideoDataViewingFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.AiTokenUseRecordBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.IsPropertyHaveBo;
import com.jiuyu.replay.order.vo.IsPropertyHaveVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.bo.ChatStreamProxyBo;
import com.jiuyu.replay.words.vo.AiOptionConfigVo;
import com.jiuyu.replay.words.vo.AskResponseVo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.vo.PromptAssemblyResultVo;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/22 下午4:49
 */
@Service
@Slf4j
public class AiRelatedLogicImpl implements AiRelatedLogic {

    public static final String propertyError = "AI分析算力包余量不足，请联系产品顾问进行套餐外购买";
    public static final String aiError = "当前人数分析过多，请稍后再试，或切换智能模型再试";
    private static final String aiTokenKey = "aiTokenNum";


    @Resource
    private UserPropertyBll userPropertyBll;
    @Resource
    private TradeBll tradeBll;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private AiModelBll aiModelBll;
    @Resource
    private HistoryParagraphBll historyParagraphBll;
    @Resource
    private AiTokenUseRecordBll aiTokenUseRecordBll;
    @Resource
    private DataScreenshotBll dataScreenshotBll;
    @Resource
    private VideoDataViewingConfuseBll videoDataViewingConfuseBll;
    @Resource
    private VideoDataViewingBll videoDataViewingBll;
    @Resource
    private DictDataBll dictDataBll;
    @Resource
    private ConversationBll conversationBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private AnchorUrlBll anchorUrlBll;
    @Autowired
    private SystemKvProducer systemKvProducer;
    @Autowired
    private CueWordsBll cueWordsBll;
    @Autowired
    private CustPromptBll custPromptBll;
    @Autowired
    private AiRelatedBll aiRelatedBll;
    @Resource
    private GlobalProblemBll globalProblemBll;
    @Autowired
    private VideoDataViewingFeign videoDataViewingFeign;
    @Autowired
    private SystemKvBll systemKvBll;
    @Resource
    private PlaceholderResolver placeholderResolver;


    @Override
    @Transactional
    public void ask(AskRequestBo askRequestBo, CustomizeSseEmitter emitter, String webVersion) {
        // 补齐ask数据
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (ObjectUtil.isEmpty(askRequestBo.getUserId())) askRequestBo.setUserId(localUser.getId());
        if (ObjectUtil.isEmpty(askRequestBo.getTenantId())) askRequestBo.setTenantId(localUser.getActiveTenantId());
        if (ObjectUtil.isEmpty(askRequestBo.getNikeName())) askRequestBo.setNikeName(localUser.getNickName());
        if (ObjectUtil.isEmpty(askRequestBo.getOtherObj())) askRequestBo.setOtherObj(new HashMap<>());

        // 校验资产
        change(askRequestBo, emitter);

        // 组装提示词（内部完成模型配置获取、提示词拼装、上下文缓存创建）
        PromptAssemblyResultVo promptAssembly = assemblePromptV2(askRequestBo, webVersion);
        log.info("【ask】assemblePrompt完成: contextId={}, useModelWay={}, contextRedisKey={}, webVersion={}, model={}",
                promptAssembly.getContextId(), promptAssembly.getUseModelWay(),
                promptAssembly.getContextRedisKey(), webVersion, askRequestBo.getAiModel());

        // 获取ai模型配置
        AiModelBo aiModelBo = getAiModelBo(askRequestBo);
        String contextId = promptAssembly.getContextId();

        // 预扣
        IsPropertyHaveVo propertyHave = withhold(askRequestBo.getUserId(), 20000L);

        try {
            ExecutorUtil.customPool.execute(() -> {
                try {
                    AiReturnDataVo aiReturnDataVo = null;

                    if (askRequestBo.getUseModelWay() == 0) {
                        aiReturnDataVo = contextChatCompletionStream(aiModelBo, askRequestBo, contextId, emitter);
                        log.info("【ask】Responses API返回: 旧contextId={}, 新contextId={}, promptTokens={}, completionTokens={}, totalTokens={}, cachedTokens={}, reasoningTokens={}",
                                contextId, aiReturnDataVo.getContextId(),
                                aiReturnDataVo.getPromptTokens(), aiReturnDataVo.getCompletionTokens(),
                                aiReturnDataVo.getTotalTokens(), aiReturnDataVo.getCachedTokens(),
                                aiReturnDataVo.getReasoningTokens());
                        // Responses API 流程：contextId 在每次回答后会变（UUID → resp_xxx → resp_yyy），更新 Redis
                        if (StrUtil.isNotEmpty(promptAssembly.getContextRedisKey())
                                && StrUtil.isNotEmpty(aiReturnDataVo.getContextId())
                                && !aiReturnDataVo.getContextId().equals(contextId)) {
                            log.info("【ask】更新Redis contextId: {} → {}, key={}",
                                    contextId, aiReturnDataVo.getContextId(), promptAssembly.getContextRedisKey());
                            redisTemplate.opsForValue().set(promptAssembly.getContextRedisKey(),
                                    aiReturnDataVo.getContextId(),
                                    AiEnums.CONTEXT_TIMEOUT - AiEnums.CONTEXT_TTL_MARGIN, TimeUnit.SECONDS);
                        } else if (StrUtil.isNotEmpty(promptAssembly.getContextRedisKey())) {
                            log.info("【ask】刷新Redis TTL: contextId未变={}, key={}", contextId, promptAssembly.getContextRedisKey());
                            redisTemplate.expire(promptAssembly.getContextRedisKey(), AiEnums.CONTEXT_TIMEOUT - AiEnums.CONTEXT_TTL_MARGIN, TimeUnit.SECONDS);
                        }
                    } else if (askRequestBo.getUseModelWay() == 1) {
                        aiReturnDataVo = chatCompletionStream(aiModelBo, askRequestBo, emitter);
                    } else {
                        RRException.create("ai模型未知");
                    }

                    // 计算token数量（含缓存折扣）
                    Long tempToken = Long.valueOf(aiReturnDataVo.getRealTotalTokens());
                    Double cacheRate = Double.parseDouble(systemKvProducer.getValueByKey("ai_cache_token_rate", "1"));

                    // 获取对应的模型消耗的aiToken倍数
                    tempToken = AiUtils.aiTokenConsumeMultiple(tempToken,
                            ObjectUtil.isEmpty(aiReturnDataVo.getCachedTokens()) ? null : Long.valueOf(aiReturnDataVo.getCachedTokens()),
                            aiModelBo.getConsumeMultiple(), cacheRate);

                    // 发送最后一条消息
                    lastSendMessage(emitter, askRequestBo, aiReturnDataVo);

                    // 扣ai-token
                    useProperty(askRequestBo, aiReturnDataVo, propertyHave, tempToken);
                } catch (RRException e) {
                    log.error("调用ai问答错误：RRException error: {}, 堆栈={}", e.getMsg(), CommonUtils.getExceptionStack(e));
                    emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", e.getCode(), "msg", e.getMsg())), "error");
                } catch (Exception e) {
                    log.error("调用ai问答错误：Exception error: {}, 堆栈={}", e.getMessage(), CommonUtils.getExceptionStack(e));
                    emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", 7005, "msg", aiError)), "error");
                } finally {
                    // 取消预扣
                    if (ObjectUtil.isNotEmpty(propertyHave.getWithholdId()) || ObjectUtil.isNotEmpty(propertyHave.getRedisId())) {
                        userPropertyBll.removeTempUserProperty(propertyHave.getWithholdId(), propertyHave.getRedisId());
                    }
                    // 结束sse
                    emitter.sendStop2();
                }
            });
        } catch (Exception e) {
            log.error("启动线程调用ai错误：Exception error: {}, 堆栈={}", e.getMessage(), CommonUtils.getExceptionStack(e));
            // 取消预扣
            if (ObjectUtil.isNotEmpty(propertyHave.getWithholdId()) || ObjectUtil.isNotEmpty(propertyHave.getRedisId())) {
                userPropertyBll.removeTempUserProperty(propertyHave.getWithholdId(), propertyHave.getRedisId());
            }
            emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", 7005, "msg", AiRelatedLogicImpl.aiError)), "error");
            // 结束sse
            emitter.sendStop2();
        }
    }

    /**
     * V1 版提示词组裝（旧流程，保留兼容）：通过 SentenceMark 机制生成提示词。
     * 新调用统一走 assemblePromptV2。
     */
    @Override
    public PromptAssemblyResultVo assemblePrompt(AskRequestBo askRequestBo, String webVersion) {
        // 获取ai模型配置
        AiModelBo aiModelBo = getAiModelBo(askRequestBo);
        // 模型的使用模式
        int useModelWay = aiModelBll.getUseModelWay(aiModelBo.getModelCode());
        // >=2.6.0 新客户端走 Responses API
        if (VersionUtil.compareVersion(webVersion, "2.6.0.5") < 0) {
            useModelWay = 1;
        }
        askRequestBo.setUseModelWay(useModelWay);

        // 获取提示词
        setPrompt(askRequestBo);

        // 设置额外要求
        if (askRequestBo.getAdditionalList() == null) askRequestBo.setAdditionalList(new ArrayList<>());
        if (ObjectUtil.isNotEmpty(aiModelBo) && aiModelBo.getOutWordNum() != null && aiModelBo.getOutWordNum() > 0) {
            askRequestBo.getAdditionalList().add(StrUtil.format("输出内容最多在{}字以内", aiModelBo.getOutWordNum()));
        }

        // 获取基础数据
        SentenceMark sentenceMark = AiFactoryUtils.getSentenceMark(askRequestBo.getSourceType(), askRequestBo.getType());
        sentenceMark.init(askRequestBo.getSourceId(), askRequestBo.getType(), askRequestBo.getVideoTimeOneList(), askRequestBo.getVideoTimeTwoList());

        // 处理语速
        sentenceMark.setSpeed(askRequestBo);

        // 处理数据截图和数据看板
        String aiOptionConfigData = setAiOptionConfigData(askRequestBo, sentenceMark);

        // 字典中的额外要求
        if (VersionUtil.compareVersion(webVersion, "2.5.9") >= 0) {
            setAskRequireAdditional(askRequestBo);
        }

        // 处理额外要求
        setQuestion(askRequestBo, sentenceMark, aiOptionConfigData);

        // 添加其他参数
        sentenceMark.setOtherParams(askRequestBo.getOtherObj());

        // 主播的ai提示词
        String anchorPrompt = getAnchorPrompt(askRequestBo);

        int wordsNum = -1;
        if (aiModelBo.getWordsNum() > 0) {
            wordsNum = aiModelBo.getWordsNum() - anchorPrompt.length() - askRequestBo.getRealContent().length() - aiOptionConfigData.length() - sentenceMark.getTextParamsContent().length() - 100;
        }
        Map<String, Object> otherObj = askRequestBo.getOtherObj();
        if (otherObj == null) otherObj = new HashMap<>();
        otherObj.putAll(Map.of(
                "singleMaxNum", wordsNum,
                "askType", askRequestBo.getType()));
        // 设置最大的字数
        sentenceMark.setOtherParams(otherObj);

        // 设置ai问题
        String question = sentenceMark.setAskQuestion(askRequestBo);

        // 设置占位符
        String realContent = aiOptionConfigData + anchorPrompt + question + "\n\n问题：" + askRequestBo.getRealContent();
        realContent = setPlaceholder(realContent, sentenceMark, askRequestBo.getReasonViolation());

        // 设置额外的提示词（主要是重新提问时）
        if (useModelWay != 0) {
            realContent += extraBuildLastOutString(askRequestBo);
        }

        askRequestBo.setRealContent(realContent);
        log.debug("【assemblePrompt】组装后的提示词 realContent (长度={}): {}", realContent.length(), realContent);

        // 上下文缓存处理（仅 useModelWay == 0）
        String contextId = null;
        String contextRedisKey = null;
        String systemPrompt = null;
        if (useModelWay == 0) {
            contextRedisKey = sentenceMark.getContextRedisKey(askRequestBo);
            contextId = (String) redisTemplate.opsForValue().get(contextRedisKey);

            AiModel aiModel = ModelFactoryUtils.getAiModel(aiModelBo.getResourceType());
            if (aiModel == null) {
                RRException.create("ai模型未知");
            }
            if (ObjectUtil.isEmpty(contextId)) {
                log.info("【assemblePrompt】contextId不存在，开始创建新上下文: contextRedisKey={}, resourceType={}",
                        contextRedisKey, aiModelBo.getResourceType());
                String allContent = setPlaceholder(sentenceMark.getAllContent(), sentenceMark, askRequestBo.getReasonViolation());
                log.debug("【assemblePrompt】allContent (长度={}): {}", allContent.length(), allContent);
                AiReturnDataVo context = aiModel.createContext(aiModelBo, allContent, null, contextRedisKey, AiEnums.CONTEXT_TIMEOUT);
                if (context.getStatus() == 0) {
                    contextId = context.getContextId();
                    log.info("【assemblePrompt】创建上下文成功: contextId={}, allContent长度={}", contextId, allContent.length());
                    redisTemplate.opsForValue().set(contextRedisKey, contextId, (AiEnums.CONTEXT_TIMEOUT - AiEnums.CONTEXT_TTL_MARGIN), TimeUnit.SECONDS);
                    // 记录创建上下文时的token消耗
                    saveAiTokenUseRecord(askRequestBo, aiModelBo, context, "创建上下文使用的token");
                    systemPrompt = allContent;
                } else {
                    RRException.isNotEmpty(contextId, "contextId获取失败");
                }
            } else {
                log.info("【assemblePrompt】复用已有contextId: contextId={}, contextRedisKey={}", contextId, contextRedisKey);
                systemPrompt = aiModel.getSystemPrompt(contextId);
            }
        }

        // 构建返回结果
        PromptAssemblyResultVo result = new PromptAssemblyResultVo();
        result.setAssembledPrompt(realContent);
        result.setIdentity(askRequestBo.getIdentity());
        result.setContextId(contextId);
        result.setUseModelWay(useModelWay);
        result.setContextRedisKey(contextId != null ? contextRedisKey : null);
        result.setSystemPrompt(systemPrompt);
        // 获取模型配置返回给调用方（含临时token等C#客户端调用Volcengine所需信息）
        result.setModelConfig(aiModelBll.getAnalysisTempToken(askRequestBo.getAiModel(), null));

        return result;
    }

    /**
     * 替换 prompt 中的占位符（如 #{trade}、#{platform}）。
     * 委托 PlaceholderResolver 完成 extract → DB 配置查询 → 按优先级解析。
     */
    private String resolvePlaceholders(String prompt, AskRequestBo askRequestBo, PlaceholderContext ctx) {
        if (StrUtil.isBlank(prompt)) return prompt;
        return placeholderResolver.resolve(prompt, askRequestBo, ctx);
    }

    /**
     * V2 版提示词组裝：DB 提示词模板已含全部内容（角色设定、分析指令、数据占位符、用户问题），
     * 运行时只做一件事 — 调用 resolvePlaceholders() 把模板里的占位符替换成实际值。
     * SentenceMark 仅用于上下文缓存取 Redis key，系统提示词由 buildSystemPrompt() 硬编码。
     */
    public PromptAssemblyResultVo assemblePromptV2(AskRequestBo askRequestBo, String webVersion) {
        if (askRequestBo.getOtherObj() == null) {
            askRequestBo.setOtherObj(new HashMap<>());
        }
        // 1. 获取模型配置
        AiModelBo aiModelBo = getAiModelBo(askRequestBo);
        int useModelWay = aiModelBll.getUseModelWay(aiModelBo.getModelCode());
        if (VersionUtil.compareVersion(webVersion, "2.6.0.5") < 0) {
            useModelWay = 1;
        }
        askRequestBo.setUseModelWay(useModelWay);

        // 2. 从 DB 取提示词模板（含占位符），填充 askRequestBo.realContent
        setPrompt(askRequestBo);

        // 2.1 组装占位符
        assemblingPlaceholders(askRequestBo);

        // 2.5 提取模板中的 system prompt 占位符 key（用于上下文缓存 key + 系统提示词构建）
        List<String> systemPromptKeys = placeholderResolver.extractContextCacheKeys(askRequestBo.getRealContent());
        askRequestBo.setSystemPromptKeys(systemPromptKeys);

        // 字典中的额外要求
        if (askRequestBo.getAdditionalList() == null) askRequestBo.setAdditionalList(new ArrayList<>());
        if (VersionUtil.compareVersion(webVersion, "2.5.9") >= 0) {
            setAskRequireAdditional(askRequestBo);
        }

        // 处理额外要求
        setQuestion(askRequestBo);

        // 3. 创建 PlaceholderContext（仅一次，后续复用避免重复查 BLL）
        PlaceholderContext ctx = new PlaceholderContext(askRequestBo);

        // 4. 解析占位符（上下文缓存模式下 systemPrompt=true 的占位符自动跳过）
        String realContent = resolvePlaceholders(askRequestBo.getRealContent(), askRequestBo, ctx);

        // 4.5 段落范围限定（paragraphCode / paragraphContent）
        String paragraphScope = buildParagraphScope(askRequestBo);
        if (StrUtil.isNotBlank(paragraphScope)) {
            realContent += "\n" + paragraphScope;
        }

        // 5. 简单对话模式附加"重新提问"上下文
        if (useModelWay != 0) {
            realContent += extraBuildLastOutString(askRequestBo);
        }
        askRequestBo.setRealContent(realContent);
        log.debug("【assemblePromptV2】组装后的提示词 realContent (长度={}): {}", realContent.length(), realContent);

        // 6. 上下文缓存处理（仅 useModelWay == 0）
        String contextId = null;
        String contextRedisKey = null;
        String systemPrompt = null;
        if (useModelWay == 0) {
            SentenceMark sentenceMark = AiFactoryUtils.getSentenceMark(askRequestBo.getSourceType(), askRequestBo.getType());
            contextRedisKey = sentenceMark.getContextRedisKey(askRequestBo);
            contextId = (String) redisTemplate.opsForValue().get(contextRedisKey);

            AiModel aiModel = ModelFactoryUtils.getAiModel(aiModelBo.getResourceType());
            if (aiModel == null) {
                RRException.create("ai模型未知");
            }
            if (ObjectUtil.isEmpty(contextId)) {
                log.info("【assemblePromptV2】contextId不存在，开始创建新上下文: contextRedisKey={}, resourceType={}",
                        contextRedisKey, aiModelBo.getResourceType());
                // 系统提示词：代码硬编码格式，数据从 ctx 取（已在步骤 4 解析时缓存，无重复查询）
                String allContent = buildSystemPrompt(ctx, systemPromptKeys);
                log.debug("【assemblePromptV2】allContent (长度={}): {}", allContent.length(), allContent);
                AiReturnDataVo context = aiModel.createContext(aiModelBo, allContent, null, contextRedisKey, AiEnums.CONTEXT_TIMEOUT);
                if (context.getStatus() == 0) {
                    contextId = context.getContextId();
                    log.info("【assemblePromptV2】创建上下文成功: contextId={}, allContent长度={}", contextId, allContent.length());
                    redisTemplate.opsForValue().set(contextRedisKey, contextId, (AiEnums.CONTEXT_TIMEOUT - AiEnums.CONTEXT_TTL_MARGIN), TimeUnit.SECONDS);
                    saveAiTokenUseRecord(askRequestBo, aiModelBo, context, "创建上下文使用的token");
                    systemPrompt = allContent;
                } else {
                    RRException.isNotEmpty(contextId, "contextId获取失败");
                }
            } else {
                log.info("【assemblePromptV2】复用已有contextId: contextId={}, contextRedisKey={}", contextId, contextRedisKey);
                systemPrompt = aiModel.getSystemPrompt(contextId);
            }
        }

        // 7. 构建返回结果（与 V1 相同结构）
        PromptAssemblyResultVo result = new PromptAssemblyResultVo();
        result.setAssembledPrompt(realContent);
        result.setIdentity(askRequestBo.getIdentity());
        result.setContextId(contextId);
        result.setUseModelWay(useModelWay);
        result.setContextRedisKey(contextId != null ? contextRedisKey : null);
        result.setSystemPrompt(systemPrompt);
        result.setSystemPromptKeys(systemPromptKeys);
        result.setModelConfig(aiModelBll.getAnalysisTempToken(askRequestBo.getAiModel(), null));

        return result;
    }

    /**
     * 组装占位符
     *
     * @param askRequestBo 参数
     */
    private void assemblingPlaceholders(AskRequestBo askRequestBo) {
        // 提示词
        String realContent = askRequestBo.getRealContent();

        List<String> placeholderKeys = new ArrayList<>();

        if (!ObjUtil.equal(askRequestBo.getCueWordsType(), AiEnums.cueWordsType.SYSTEM.getCode())) {
            // 占位符列表，如：#{anchorData}，#{minuteSegment}
            Object temp = askRequestBo.getOtherObj().get("placeholderKeys");
            if (temp != null && !ObjectUtil.equal(askRequestBo.getCueWordsType(), 0)) {
                placeholderKeys = JSONUtil.toList(JSONUtil.toJsonPrettyStr(temp), String.class);
            }

            // 如果都有分钟段落和弹幕就正常分钟段落+弹幕
            if (placeholderKeys.contains(PlaceholderEnum.MINUTE_SEGMENT.getFullKey()) && placeholderKeys.contains(PlaceholderEnum.DANMAKU.getFullKey())) {
                placeholderKeys.remove(PlaceholderEnum.MINUTE_SEGMENT.getFullKey());
                placeholderKeys.remove(PlaceholderEnum.DANMAKU.getFullKey());
                placeholderKeys.add(PlaceholderEnum.MINUTE_SEGMENT_DANMAKU.getFullKey());
            }
        }

        // 现在提示词中的占位符
        List<PlaceholderToken> extract = placeholderResolver.extract(realContent);

        // 把提示词中的占位符设置到placeholderKeys中
        for (PlaceholderToken item : extract) {
            placeholderKeys.add(item.getFullKey());
        }

        List<String> strings = placeholderResolver.extractContextCacheKeys(StrUtil.join(", ", placeholderKeys));
        if (ObjectUtil.isEmpty(strings)) {
            if (ObjUtil.equal(askRequestBo.getType(), AiEnums.askType.BARRAGE.getCode())) {
                placeholderKeys.add(PlaceholderEnum.DANMAKU.getFullKey());
            } else {
                placeholderKeys.add(PlaceholderEnum.MINUTE_SEGMENT.getFullKey());
            }
        }

        // 如果是简单的对话就把主播信息加上
        if (ObjUtil.equal(askRequestBo.getUseModelWay(), 1)) {
            placeholderKeys.add(PlaceholderEnum.ANCHOR_DATA.getFullKey());
        }

        // 根据助手类型追加知识库占位符
        if (ObjUtil.equal(askRequestBo.getType(), AiEnums.askType.OPERATION.getCode())
                || ObjUtil.equal(askRequestBo.getType(), AiEnums.askType.DATA_DIAGNOSIS.getCode())) {
            placeholderKeys.add(PlaceholderEnum.ANCHOR_KNOWLEDGE.getFullKey());
        }
        if (ObjUtil.equal(askRequestBo.getType(), AiEnums.askType.VIOLATION.getCode())) {
            placeholderKeys.add(PlaceholderEnum.ANCHOR_SENSITIVE_KNOWLEDGE.getFullKey());
        }

        placeholderKeys.add(PlaceholderEnum.LIVE_DATA.getFullKey());

        // 去重 + 按 promptOrder 升序排序，匹配不上的丢弃
        placeholderKeys = placeholderKeys.stream()
                .distinct()
                .filter(key -> PlaceholderEnum.fullOf(key) != null)
                .sorted(Comparator.comparingInt(key -> PlaceholderEnum.fullOf(key).getPromptOrder()))
                .collect(Collectors.toList());
        // 倒序遍历，确保 promptOrder 小的最终在最前面
        Collections.reverse(placeholderKeys);

        // 提示词中已有的占位符 key（精确匹配，避免子串误命中）
        Set<String> existingKeys = extract.stream()
                .map(PlaceholderToken::getKey)
                .collect(Collectors.toSet());

        // 前置缺失的占位符到提示词最前面
        String str = "";
        for (String fullKey : placeholderKeys) {
            PlaceholderEnum pe = PlaceholderEnum.fullOf(fullKey);
            if (pe != null && !existingKeys.contains(pe.getKey())) {
                str = pe.getDisplayName() + "：" + fullKey + "\n\n" + str;
            }
        }
        askRequestBo.setRealContent(str + "\n" + realContent);
    }

    /**
     * 构建上下文缓存的系统提示词（固定格式，硬编码）。
     * 数据来自 PlaceholderContext（已在 resolve 时缓存，无重复查询）。
     */
    private String buildSystemPrompt(PlaceholderContext ctx, List<String> systemPromptKeys) {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.getAnchorData()).append("\n");

        if (CollectionUtil.isNotEmpty(systemPromptKeys)) {
            for (String key : systemPromptKeys) {
                PlaceholderEnum pe = PlaceholderEnum.of(key);
                if (pe == null) continue;
                String value = pe.resolveForSystemPrompt(ctx);
                sb.append(pe.getDisplayName()).append("：\n");
                sb.append(StrUtil.isNotBlank(value) ? value : "（暂无" + pe.getDisplayName() + "数据）");
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建段落范围限定指令，用于告诉 AI 只看指定段落而非全文。
     * paragraphContent 优先；其次按 paragraphCode 查 DB；code 为空或 "0" 视为全文，不追加。
     */
    private String buildParagraphScope(AskRequestBo askRequestBo) {
        String paragraphCode = askRequestBo.getParagraphCode();
        String paragraphContent = askRequestBo.getParagraphContent();

        if (StrUtil.isNotBlank(paragraphContent)) {
            return StrUtil.format("本次回答内容不用考虑全文，只需要根据以下内容进行回答：\n{}", paragraphContent);
        }

        if (StrUtil.isNotBlank(paragraphCode) && !"0".equals(paragraphCode)) {
            R<HistoryParagraphInfoVo> result = historyParagraphBll.getByCode(
                    askRequestBo.getType(), askRequestBo.getSourceId(),
                    askRequestBo.getSourceType(), paragraphCode);
            if (result != null && result.getData() != null
                    && StrUtil.isNotBlank(result.getData().getContent())) {
                return StrUtil.format("本次回答内容不用考虑全文，只需要根据以下内容进行回答：\n{}",
                        result.getData().getContent());
            }
        }

        return "";
    }

    /**
     * 扣ai-token
     *
     * @param askRequestBo   问答入参
     * @param aiReturnDataVo ai返回数据
     * @param propertyHave   预扣的资产
     * @param tempToken      模型消耗的aiToken倍数
     */
    private void useProperty(AskRequestBo askRequestBo, AiReturnDataVo aiReturnDataVo, IsPropertyHaveVo propertyHave, Long tempToken) {
        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
        assets.setUserId(askRequestBo.getUserId());
        assets.setUserName(askRequestBo.getNikeName());
        assets.setCode(aiTokenKey);
        assets.setNum(tempToken);
        assets.setAiTokenIds(aiReturnDataVo.getAiTokenId());
        assets.setRedisId(propertyHave.getRedisId());
        assets.setWithholdId(propertyHave.getWithholdId());
        // 扣ai-token
        aiTokenUse(assets);
        propertyHave.setRedisId(null);
        propertyHave.setWithholdId(null);
    }

    @Override
    public String extraBuildLastOutString(AskRequestBo askRequestBo) {

        String lastConversationId = askRequestBo.getLastConversationId();
        String optimizeText = getOtherObjValueByKey(askRequestBo, "optimizeText");

        if (StrUtil.isEmpty(lastConversationId)) {
            return "";
        }

        ConversationVo tempVo = conversationBll.getById(lastConversationId);
        if (tempVo == null) {
            throw new RRException("没有找到重新提问的内容");
        }

        List<ConversationVo> conversationVoList = conversationBll.listByQaCodes(List.of(tempVo.getQaCode()));

        if (conversationVoList.isEmpty()) {
            return "";
        }

        ConversationVo conversationVo = conversationVoList.stream().filter(item -> ObjectUtil.equals(item.getType(), "Q")).findFirst().orElse(null);
        if (conversationVo == null) {
            return "";
        }

        String content = AiUtils.deleteDeepThinking(conversationVo.getContent());

        if (StrUtil.isEmpty(content)) {
            return "";
        }

        String res = StrUtil.format("\n\n你给我的上次分析的回答结果是以下内容\n{}", content);

        String optStr = "";

        if (StrUtil.isNotEmpty(optimizeText)) {
            optStr += StrUtil.format("\n我对你上次分析的回答结果不满意的地方是：{}", optimizeText);
        }

        String extraRequire = getOtherObjValueByKey(askRequestBo, "extraRequire");
        if (StrUtil.isNotEmpty(extraRequire)) {
            optStr += StrUtil.format("\n我这次的额外要求：{}", extraRequire);
        }

        if (StrUtil.isNotEmpty(optStr)) {

            // 添加系统的额外要求
            String systemExtraRequire = systemKvProducer.getValueByKey("again_ask_system_extra_prompt", "");
            if (StrUtil.isNotEmpty(systemExtraRequire)) {
                optStr += "\n\n" + systemExtraRequire;
            }

            optStr += "\n\n请结合你上一次的回答结果和我不满意的地方，给我重新输出一份分析报告";
        }

        return res + "\n" + optStr;
    }

    /**
     * 获取提示词
     *
     * @param askRequestBo 问答入参
     * @return 提示词
     */
    private void setPrompt(AskRequestBo askRequestBo) {

        if (ObjectUtil.isEmpty(askRequestBo.getCueWordsId())) {
            if (ObjectUtil.isEmpty(askRequestBo.getRealContent())) {
                throw new RRException("提示词为空，操作失败");
            }
            return;
        }
        AiPromptWordVo vo = aiRelatedBll.getAiPromptWord2(askRequestBo.getCueWordsId(), askRequestBo.getCueWordsType(), askRequestBo.getLastConversationId(), false);
        askRequestBo.setRealContent(vo.getPrompt());
        if (vo.getReason() != null && ObjectUtil.isEmpty(askRequestBo.getReasonViolation())) {
            askRequestBo.setReasonViolation(vo.getReason());
        }
        if (ObjectUtil.isNotEmpty(askRequestBo.getLastConversationId()) && vo.getOptimizeActions() != null) {
            Object backgroundConfig = askRequestBo.getOtherObj().getOrDefault("backgroundConfigOne", null);
            // 判断是否是map<string,object>类型的
            if (backgroundConfig instanceof Map) {
                Map<String, Object> backgroundConfigMap = (Map<String, Object>) backgroundConfig;
                backgroundConfigMap.put("optimizeActions", vo.getOptimizeActions());
            }
        }
    }

    /**
     * 获取主播对应的ai提示词
     *
     * @param askRequestBo 问答入参
     * @return 提示词
     */
    private String getAnchorPrompt(AskRequestBo askRequestBo) {
        String aiAnchorPrompt = anchorUrlBll.getAiAnchorPrompt(askRequestBo);
        if (ObjectUtil.isNotEmpty(aiAnchorPrompt)) {
            return StrUtil.format("\n{}\n", aiAnchorPrompt);
        }
        return "";
    }

    /**
     * 查询并设置全局提示词作为额外要求。
     * 跳过条件：提示词类型为系统且租户级自定义提示词（tenantId > 0）。
     */
    private void setAskRequireAdditional(AskRequestBo askRequestBo) {
        // 只有一种情况没有全局提示词：提示词类型是系统并且提示词的租户大于0
        if (ObjectUtil.isNotEmpty(askRequestBo.getCueWordsId()) && ObjectUtil.equals(askRequestBo.getCueWordsType(), AiEnums.cueWordsType.SYSTEM.getCode())) {
            CueWordsInfoVo words = ResultUtil.getResult(cueWordsBll.info(askRequestBo.getCueWordsId()));
            if (words != null && words.getTenantId() != null && words.getTenantId() > 0) {
                return;
            }
        }

        // 确定查询参数
        Integer cueType = askRequestBo.getType();
        Integer applyTo = ObjectUtil.equals(askRequestBo.getSourceType(), 2) ? 1 : 0;
        Integer problemType = (ObjectUtil.equals(askRequestBo.getCueWordsType(), 1)
                || ObjectUtil.isEmpty(askRequestBo.getCueWordsId())) ? 1 : 0;

        // 查询全局提示词
        List<GlobalProblemListVo> globalProblems = globalProblemBll.listByConditions(cueType, applyTo, problemType);
        if (CollectionUtil.isEmpty(globalProblems)) {
            return;
        }

        List<String> result = globalProblems.stream()
                .map(GlobalProblemListVo::getProblemContent)
                .filter(ObjectUtil::isNotEmpty)
                .toList();
        if (!result.isEmpty()) {
            askRequestBo.getAdditionalList().addAll(result);
        }
    }


    @Override
    public R<String> getContextId(AskRequestBo askRequestBo) {
        SentenceMark bean = (SentenceMark) ApplicationContextUtil.getBean("defaultSentenceMarkImpl");
        String contextRedisKey = bean.getContextRedisKey(askRequestBo);
        String contextId = (String) redisTemplate.opsForValue().get(contextRedisKey);
        return R.ok("获取成功", contextId);
    }

    @Override
    public R<String> updateContextId(AskRequestBo askRequestBo) {
        SentenceMark bean = (SentenceMark) ApplicationContextUtil.getBean("defaultSentenceMarkImpl");
        String contextRedisKey = bean.getContextRedisKey(askRequestBo);
        redisTemplate.opsForValue().set(contextRedisKey, askRequestBo.getContent(), (AiEnums.CONTEXT_TIMEOUT - AiEnums.CONTEXT_TTL_MARGIN), TimeUnit.SECONDS);
        return R.ok("更新成功");
    }

    @Override
    public R<AiOptionConfigVo> aiOptionConfig(Integer sourceType, String sourceId) {
        AiOptionConfigVo result = new AiOptionConfigVo();
        R<Boolean> booleanR = dataScreenshotBll.existVideoDataScreenshot(sourceType, sourceId);
        result.setHasDataScreenshot(booleanR.getData() ? 1 : 0);
        result.setHasBoard(0);
        if (sourceType == 0){
            boolean hasBoard = videoDataViewingFeign.hasBoard(sourceId);
            result.setHasBoard(hasBoard ? 1 : 0);
        }

        return R.ok(result);
    }

    @Override
    public R<List<DanMuVo>> danMuCacheList(QueryDanMuBo queryDanMuBo) {
        BarrageSentenceImpl barrageSentenceImpl = ApplicationContextUtil.getBean(BarrageSentenceImpl.class);
        barrageSentenceImpl.init(queryDanMuBo.getVideoId(), 2, Arrays.asList(queryDanMuBo.getStartTime(), queryDanMuBo.getEndTime()), null);
        barrageSentenceImpl.setOtherParams(BeanUtil.beanToMap(queryDanMuBo));
        List<DanMuVo> danMuVos = barrageSentenceImpl.barrageBoList();
        return R.ok(danMuVos);
    }

    /**
     * 获取ai模型配置
     * @param askRequestBo
     * @return
     */
    public AiModelBo getAiModelBo(AskRequestBo askRequestBo){
        AiModelInfoVo aiModelByAiModel = aiModelBll.getAiModelByAiModel(askRequestBo.getAiModel(), null);
        if (aiModelByAiModel == null) {
            RRException.create("ai模型配置获取失败");
        }
        return BeanUtil.copyProperties(aiModelByAiModel, AiModelBo.class);
    }

    /**
     * 调用ai问答-有上下文缓存的、流式返回
     * @param aiModelBo
     * @param askRequestBo
     * @param contextId 已解析的上下文缓存ID（由assemblePrompt创建）
     * @param emitter
     * @return
     */
    public AiReturnDataVo contextChatCompletionStream(AiModelBo aiModelBo, AskRequestBo askRequestBo, String contextId, CustomizeSseEmitter emitter) {

        AiModel aiModel = ModelFactoryUtils.getAiModel(aiModelBo.getResourceType());
        if (aiModel == null) {
            RRException.create("ai模型未知");
        }

        AiMessageBo params = new AiMessageBo();
        // 设置身份
        if (ObjectUtil.isNotEmpty(askRequestBo.getIdentity())){
            params.setSystem(List.of(Map.of("text", askRequestBo.getIdentity())));
        }
        // 设置问题
        if (ObjectUtil.isNotEmpty(askRequestBo.getRealContent())){
            params.setUser(List.of(Map.of("text", askRequestBo.getRealContent())));
        }

        // 提问ai
        AiReturnDataVo aiReturnDataVo = aiModel.contextChatCompletionStream(
                aiModelBo,
                contextId,
                params,
                (choice) -> sendMessage(emitter, choice)
        );
        AiTokenUseRecordInfoVo aiTokenUseRecordInfoVo = saveAiTokenUseRecord(askRequestBo, aiModelBo, aiReturnDataVo, "");
        ArrayList<Long> arrayList = new ArrayList<>();
        arrayList.add(aiTokenUseRecordInfoVo.getId());
        aiReturnDataVo.setAiTokenId(arrayList);

        return aiReturnDataVo;
    }

    /**
     * 简单对话模式（无上下文缓存）流式调用 AI。
     *
     * @param aiModelBo    模型配置
     * @param askRequestBo 问答请求（含 identity 和 realContent）
     * @param emitter      SSE 回调
     * @return AI 返回数据（含 token 消耗）
     */
    public AiReturnDataVo chatCompletionStream(AiModelBo aiModelBo, AskRequestBo askRequestBo, CustomizeSseEmitter emitter){
        AiModel aiModel = ModelFactoryUtils.getAiModel(aiModelBo.getResourceType());

        AiMessageBo params = new AiMessageBo();
        // 设置身份
        if (ObjectUtil.isNotEmpty(askRequestBo.getIdentity())){
            params.setSystem(List.of(Map.of("text", askRequestBo.getIdentity())));
        }
        // 设置问题
        if (ObjectUtil.isNotEmpty(askRequestBo.getRealContent())){
            params.setUser(List.of(Map.of("text", askRequestBo.getRealContent())));
        }

        // 提问ai
        AiReturnDataVo aiReturnDataVo = aiModel.chatCompletionStream(aiModelBo, params, (choice) -> sendMessage(emitter, choice));

        aiReturnDataVo.setContextId(null);
        AiTokenUseRecordInfoVo aiTokenUseRecordInfoVo = saveAiTokenUseRecord(askRequestBo, aiModelBo, aiReturnDataVo, "");
        aiReturnDataVo.setAiTokenId(Arrays.asList(aiTokenUseRecordInfoVo.getId()));
        return aiReturnDataVo;
    }

    /**
     * 保存到aiToken消耗量记录表
     * @param askRequestBo
     * @param aiModelBo
     * @param aiReturnDataVo
     * @return
     */
    public AiTokenUseRecordInfoVo saveAiTokenUseRecord(AskRequestBo askRequestBo, AiModelBo aiModelBo, AiReturnDataVo aiReturnDataVo, String remarks){
        // 记录aiToken消耗量
        AiTokenUseRecordBo aiTokenUseRecordBo = AiTokenUseRecordBo.builder(aiReturnDataVo, aiModelBo.getModelName());
        aiTokenUseRecordBo.setTenantId(askRequestBo.getTenantId());
        aiTokenUseRecordBo.setUserId(askRequestBo.getUserId());
        aiTokenUseRecordBo.setUseSourceType(askRequestBo.getSourceType());
        aiTokenUseRecordBo.setUseSourceId(askRequestBo.getSourceId());
        aiTokenUseRecordBo.setAssistantType(askRequestBo.getType());
        aiTokenUseRecordBo.setRemarks(remarks);
        R<AiTokenUseRecordInfoVo> save = aiTokenUseRecordBll.save(aiTokenUseRecordBo);
        return save.getData();
    }

    /**
     * 在其他参数中获取对应的value
     *
     * @param askRequestBo 提问参数
     * @param key          键
     * @return 对应的value
     */
    private String getOtherObjValueByKey(AskRequestBo askRequestBo, String key) {
        if (askRequestBo.getOtherObj() == null) {
            return null;
        }

        Object values = askRequestBo.getOtherObj().get(key);
        if (values == null) {
            return null;
        }
        // 判断是否是string类型
        if (!(values instanceof String)) {
            return null;
        }
        return (String) values;
    }

    /**
     * 发送最后一条消息：封装问/答参数、保存到 MongoDB、返回资产余量 + AI 内容校验标记。
     */
    public void lastSendMessage(CustomizeSseEmitter emitter, AskRequestBo askRequestBo, AiReturnDataVo aiReturnDataVo){
        AskResponseVo result = new AskResponseVo();
        // 获取aiToken
        UserPropertyTypeInfoVo aiTokenNum = getAiTokenNum(askRequestBo.getUserId());
        if (aiTokenNum != null){
            AskResponseVo.PropertyDto property = new AskResponseVo.PropertyDto();
            long total = ObjectUtil.defaultIfNull(aiTokenNum.getTotalQuantity(), 0L);
            long used = ObjectUtil.defaultIfNull(aiTokenNum.getUseQuantity(), 0L);
            long surplus = Math.max(0, total - used);
            property.setSurplusNum((int) Math.min(surplus, Integer.MAX_VALUE));
            property.setCurrerntUseNum(aiReturnDataVo.getRealTotalTokens());
            result.setProperty(property);
        }
        // 获取优化文本
        String optimizeText = getOtherObjValueByKey(askRequestBo, "optimizeText");

        // 获取额外要求
        String extraRequire = getOtherObjValueByKey(askRequestBo, "extraRequire");
        // 问的参数封装
        String qaCode = UUID.randomUUID().toString().replaceAll("-", "");
        ConversationVo problem = new ConversationVo()
                .setSourceId(askRequestBo.getSourceId())
                .setSourceType(askRequestBo.getSourceType())
                .setUserId(askRequestBo.getUserId())
                .setTenantId(askRequestBo.getTenantId())
                .setAskType(askRequestBo.getType())
                .setCode(UUID.randomUUID().toString().replaceAll("-", ""))
                .setCueWordsId(askRequestBo.getCueWordsId())
                .setCueWordsType(askRequestBo.getCueWordsType())
                .setQaCode(qaCode)
                .setContextId(aiReturnDataVo.getContextId())
                .setCompletionId(aiReturnDataVo.getRequestId())
                .setType("A")
                .setContent(askRequestBo.getContent())
                .setRealContent(askRequestBo.getRealContent())
                .setCreateDate(DateUtil.now())
                .setLastConversationId(askRequestBo.getLastConversationId())
                .setOptimizeText(optimizeText)
                .setExtraRequire(extraRequire)
                .setQuestionType(ObjectUtil.isNotEmpty(askRequestBo.getLastConversationId()) ? 1 : 0)
                ;
        result.setProblem(problem);

        // 回答的参数封装
        ConversationVo answer = new ConversationVo()
                .setSourceId(askRequestBo.getSourceId())
                .setSourceType(askRequestBo.getSourceType())
                .setUserId(askRequestBo.getUserId())
                .setTenantId(askRequestBo.getTenantId())
                .setAskType(askRequestBo.getType())
                .setCode(UUID.randomUUID().toString().replaceAll("-", ""))
                .setCueWordsId(askRequestBo.getCueWordsId())
                .setCueWordsType(askRequestBo.getCueWordsType())
                .setQaCode(qaCode)
                .setContextId(aiReturnDataVo.getContextId())
                .setCompletionId(aiReturnDataVo.getRequestId())
                .setType("Q")
                .setContent(ObjectUtil.defaultIfEmpty(aiReturnDataVo.getContent(), aiError))
                .setRealContent("")
                .setCreateDate(DateUtil.now())
                ;
        result.setAnswer(answer);

        // 保存到mongodb中
        List<ConversationVo> conversationVos = conversationBll.saveConversationData(BeanUtil.copyToList(Arrays.asList(problem, answer), ConversationBo.class));
        if (ObjectUtil.isNotEmpty(conversationVos) && conversationVos.size() == 2) {
            problem.setId(conversationVos.get(0).getId());
            answer.setId(conversationVos.get(1).getId());
        }
        problem.setRealContent(null);
        answer.setRealContent(null);
        // 设置是否需要检查ai内容
        result.setCheckAiContent(getCheckAiContent(askRequestBo, aiReturnDataVo.getContent(), answer.getId(), AiEnums.useSourceType.CONVERSATION.getCode()));
        // 发送最后一条消息
        emitter.sendMessage(JSON.toJSONString(result, JSONWriter.Feature.WriteLongAsString), "returnData");

    }

    /**
     * 检查AI内容是否需要纠正
     *
     * @param askRequestBo 参数
     * @param content    内容
     * @param id         来源id
     * @param sourceType 来源类型
     * @return 是否需要纠正
     */
    public boolean getCheckAiContent(AskRequestBo askRequestBo, String content, String id, Integer sourceType) {
        if (StrUtil.isEmpty(content) || id == null) {
            log.info("[检查ai内容是否需要纠正] 内容为空，返回false， id = {}, type = {}", id, sourceType);
            return false;
        }

        // 获取模型code和提示词
        AiContentCorrectionConfigVo config = dictDataBll.getContentCorrectionConfig(AiEnums.correctionSceneType.CHECK_AI_ANSWER.getCode());
        if (config == null || StrUtil.isEmpty(config.getModelCode()) || StrUtil.isEmpty(config.getContentPrompt())) {
            log.info("[检查ai内容是否需要纠正] 获取对应的字典配置失败，返回false， id = {}, type = {}", id, sourceType);
            return false;
        }
        String code = config.getModelCode();
        String prompt = config.getContentPrompt();

        // 获取ai模型配置
        AiModelInfoVo aiModelInfoVo = aiModelBll.getByCode(code).getData();
        if (aiModelInfoVo == null) {
            log.error("[检查ai内容是否需要纠正] 获取ai模型配置失败，返回false， id = {}, type = {}", id, sourceType);
            return false;
        }
        AiModelBo aiModelBo = BeanUtil.copyProperties(aiModelInfoVo, AiModelBo.class);

        // 获取ai模型
        AiModel aiModel = ModelFactoryUtils.getAiModel(aiModelInfoVo.getResourceType());
        if (aiModel == null) {
            log.error("[检查ai内容是否需要纠正] 获取ai模型失败，返回false， id = {}, type = {}", id, sourceType);
            return false;
        }

        // 构建消息
        AiMessageBo params = new AiMessageBo();
        params.setSystem(List.of(Map.of("text", "你是一个文字整理专家")));
        String contentStr = content + "\n\n" + prompt;
        params.setUser(List.of(Map.of("text", contentStr)));

        log.debug("[检查ai内容是否需要纠正] 提示词 = {}, model = {}", params, aiModelBo.getModelCode());

        // 调用ai
        AiReturnDataVo aiReturnDataVo = aiModel.chatCompletion(aiModelBo, params, null);

        if (aiReturnDataVo != null && aiReturnDataVo.getStatus() != null && aiReturnDataVo.getStatus() == 0) {
            // 保存aiToken使用记录
            AiTokenUseRecordBo recordBo = AiTokenUseRecordBo.builder(aiReturnDataVo, aiModelBo.getModelCode());
            recordBo.setTenantId(askRequestBo.getTenantId());
            recordBo.setUserId(askRequestBo.getUserId());
            recordBo.setUseSourceType(sourceType);
            recordBo.setUseSourceId(id);
            recordBo.setAssistantType(AiEnums.askType.CHECK_AI_CORRECT.getCode());
            recordBo.setRemarks("判断AI内容格式是否正确");
            aiTokenUseRecordBll.save(recordBo);

            String onlyMessage = aiReturnDataVo.getContent();
            if (StrUtil.isEmpty(onlyMessage)) {
                return false;
            }

            log.info("[检查ai内容是否需要纠正] ai输出 = {}", onlyMessage);

            // 获取确认配置
            String confirmCode = systemKvBll.getValueByKey("check_ai_content_prompt_confirm", "");
            if (StrUtil.isEmpty(confirmCode)) {
                return false;
            }

            try {
                com.alibaba.fastjson2.JSONArray jsonArray = JSON.parseArray(confirmCode);
                if (jsonArray != null && !jsonArray.isEmpty()) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        com.alibaba.fastjson2.JSONObject item = jsonArray.getJSONObject(i);
                        String label = item.getString("label");
                        if (onlyMessage.contains(label)) {
                            return item.getBooleanValue("value");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[检查ai内容是否需要纠正] 解析确认配置失败", e);
            }
        }

        return false;
    }

    /**
     * aiToken使用
     * @param assets
     */
    private void aiTokenUse(AssetsMinusOrPlusBo assets) {
        // 获取当前的资产
        UserPropertyTypeInfoVo userPropertyTypeInfoVo = getAiTokenNum(assets.getUserId());
        if (ObjectUtil.isEmpty(userPropertyTypeInfoVo)){
            assets.setNum(0L);
            assets.setRemarks(StrUtil.format("本次扣款：{}，但当前AI分析文本数量为：0，本次不扣款", assets.getNum()));
        }else{
            long currentNum = userPropertyTypeInfoVo.getTotalQuantity() - userPropertyTypeInfoVo.getUseQuantity();
            currentNum = currentNum < 0 ? 0 : currentNum;
            if (Math.abs(assets.getNum()) > Math.abs(currentNum)){
                assets.setRemarks(StrUtil.format("本次扣款：{}，但当前AI分析文本数量为：{}，本次全部扣完", assets.getNum(), currentNum));
                assets.setNum(-currentNum);
            }else{
                assets.setNum(-(Math.abs(assets.getNum())));
            }
        }
        assets.setClearWithholdCache(userPropertyBll::removeTempUserProperty);
        UserPropertyImpl.use(assets);
    }

    /**
     * 预扣 AI Token 资产（防止并发超扣）。
     *
     * @param userId 用户 ID
     * @param token  预扣数量
     * @return 预扣结果（含 witholdId / redisId，后续实际扣款或取消时使用）
     */
    private IsPropertyHaveVo withhold(Long userId, Long token) {
        IsPropertyHaveBo bo = new IsPropertyHaveBo();
        bo.setUserId(userId);
        bo.setThisUseNum(token);
        bo.setCode(aiTokenKey);
        R<IsPropertyHaveVo> propertyHaveAiToken = userPropertyBll.isPropertyHaveAiToken(bo);
        if (propertyHaveAiToken.getCode() != 0 || !propertyHaveAiToken.getData().getIsHave()) {
            RRException.create(7001, propertyError);
        }
        return propertyHaveAiToken.getData();
    }

    /**
     * 校验资产
     *
     * @param askRequestBo
     * @param emitter
     */
    private void change(AskRequestBo askRequestBo, CustomizeSseEmitter emitter) {
        UserPropertyTypeInfoVo aiTokenNum = getAiTokenNum(askRequestBo.getUserId());
        if (ObjectUtil.isEmpty(aiTokenNum)) {
            long num = ObjectUtil.defaultIfNull(aiTokenNum.getTotalQuantity(), 0L) - ObjectUtil.defaultIfNull(aiTokenNum.getUseQuantity(), 0L);
            if (num <= 0){
                RRException.create(7001, propertyError);
            }
        }
    }

    /**
     * 获取aiToken
     * @param userId
     * @return
     */
    private UserPropertyTypeInfoVo getAiTokenNum(Long userId){
        List<UserPropertyTypeInfoVo> userProperty = userPropertyBll.getUserProperty(userId);
        return userProperty.stream()
                .filter(item -> item.getCommodityTypeCode().equals(aiTokenKey))
                .findFirst().orElse(null);
    }

    /**
     * 设置问题-额外要求
     * @param askRequestBo 参数
     * @param sentenceMark 数据源
     */
    private void setQuestion(AskRequestBo askRequestBo, SentenceMark sentenceMark, String aiOptionConfigData) {
        // 处理数据截图和数据看板
        if (ObjectUtil.isNotEmpty(aiOptionConfigData)){
            String str = "这是一个#{trade} 行业的直播间，是在#{platform}平台上直播的，结合全场直播相关数据中与以上问题有关的数据，在分析文档的未尾，围绕本次提问内容以及本赛道在直播中的行业数据情况，给出本场直播的在内容上的总结和优化建议，不超过300字，要求如下：" +
                    "1、先做行业直播要点概览，给出该赛道在#{platform}平台上直播，应该重点关注哪些数据，以及原因；" +
                    "2、再结合本问题，给出影响本问题的几个核心数据指标是什么，行业健康值是多少（给出数据来源，告诉我数据来自是哪个网站，或者哪个博主或者哪个平台，不要给我任何的假设数据！不要给我任何的虚构数据！不要给我任何的没有明确来源的数据！），针对本问题的本场的几个核心数据有什么问题；" +
                    "3、再做运营优化方案建议，给的优化方案要尽可能的详细、有逻辑、有条理、可执行；" +
                    "4、行业健康值或者参考值，要告诉我数据来源；" +
                    "5、我没有给到你的本场直播数据项，你不要凭空捏造，不要凭空猜想。";

            SystemKvInfoVo kv = systemKvProducer.getByKey("ai_option_config_data");
            if (ObjectUtil.isNotEmpty(kv) && ObjectUtil.isNotEmpty(kv.getKvValue())) {
                str = kv.getKvValue().trim();
            }
            askRequestBo.getAdditionalList().add(str);
        }

        // 获取对应助手的额外要求
        if (ObjectUtil.isNotEmpty(sentenceMark.getAdditionalList())) {
            askRequestBo.getAdditionalList().addAll(sentenceMark.getAdditionalList());
        }
        setQuestion(askRequestBo);
    }

    /**
     * 设置额外要求的提示词
     *
     * @param askRequestBo 参数
     */
    private void setQuestion(AskRequestBo askRequestBo) {
        StringBuilder result = new StringBuilder(askRequestBo.getRealContent());
        RRException.isNotEmpty(result.toString(), "问题不能为空", 7002);

        if (askRequestBo.getType() == 2) {
            askRequestBo.getAdditionalList().add("输出内容时UL替换为用户等级，FL替换为粉丝团等级");
        }


        // 额外要求
        List<String> additionalList = askRequestBo.getAdditionalList().stream().filter(item -> ObjUtil.isNotNull(item.trim())).collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(additionalList)) {
            result.append("\n额外要求:\n");
            for (int i = 0; i < askRequestBo.getAdditionalList().size(); i++) {
                result.append(StrUtil.format("{}、{}\n", i + 1, askRequestBo.getAdditionalList().get(i)));
            }
        }
        askRequestBo.setRealContent(result.toString());
    }

    /**
     * 处理占位符
     * @param question
     * @param sentenceMark
     * @param reasonViolation
     * @return
     */
    public String setPlaceholder(String question, SentenceMark sentenceMark, String reasonViolation) {
        // 处理占位符
        // #{行业}、#{违规原因}
        // 判断是否有行业，有要查询行业
        // 处理占位符
        Map<String, String> params = new HashMap<>();
        params.put("trade", ObjectUtil.defaultIfEmpty(sentenceMark.getTradeName(), "某行业"));
        params.put("platform", ObjectUtil.defaultIfEmpty(sentenceMark.getPlatform(), "抖音"));
        String replace = "reason";
        if (question.contains(CharSequenceUtil.format("#{{}}", replace))) {
            RRException.isNotEmpty(reasonViolation, "违规原因不能为空", 7002);
            params.put(replace, reasonViolation);
        }
        return CommonUtils.placeholderHandle(question, params);
    }

    /**
     * 处理数据截图和数据看板
     * @param askRequestBo
     * @param sentenceMark
     */
    private String setAiOptionConfigData(AskRequestBo askRequestBo, SentenceMark sentenceMark) {
        String str = "";
        if (askRequestBo.getType() == 0){
            if (askRequestBo.getUploadScreenshot()==null) askRequestBo.setUploadScreenshot(0);
            String dataScreenshot = "";
            if (askRequestBo.getUploadScreenshot() == 1){
                dataScreenshot = sentenceMark.getDataScreenshot();
            }
            if (askRequestBo.getUploadBoard()==null) askRequestBo.setUploadBoard(0);
            String board = "";
            if (askRequestBo.getUploadBoard() == 1) {
                board = sentenceMark.getBoard();
            }
            if (ObjectUtil.isNotEmpty(dataScreenshot) || ObjectUtil.isNotEmpty(board)){
                str += "全场直播相关数据如下：";
                if (ObjectUtil.isNotEmpty(dataScreenshot)){
                    str += "\n" + dataScreenshot;
                }
                if (ObjectUtil.isNotEmpty(board)){
                    str += "\n" + board;
                }
            }
        }
        return str;
    }

    /**
     * 服务端 AI 流式对话代理：C# 客户端传入完整参数，服务端转发到 AI 厂商并流式返回。
     * 支持上下文缓存模式（useModelWay=0 + contextId 非空）和简单对话模式。
     */
    @Override
    public void chatStreamProxy(ChatStreamProxyBo bo, CustomizeSseEmitter emitter) {
        // 获取模型配置
        AiModelInfoVo aiModelInfoVo = aiModelBll.info(bo.getModelId()).getData();
        if (aiModelInfoVo == null) {
            RRException.create("模型配置不存在");
        }
        AiModelBo aiModelBo = BeanUtil.copyProperties(aiModelInfoVo, AiModelBo.class);

        // 构建消息
        AiMessageBo params = new AiMessageBo();
        if (StrUtil.isNotEmpty(bo.getIdentity())) {
            params.setSystem(List.of(Map.of("text", bo.getIdentity())));
        }
        if (StrUtil.isNotEmpty(bo.getRealContent())) {
            params.setUser(List.of(Map.of("text", bo.getRealContent())));
        }

        // 异步执行AI调用，避免阻塞SSE响应
        ExecutorUtil.customPool.execute(() -> {
            try {
                AiModel aiModel = ModelFactoryUtils.getAiModel(aiModelBo.getResourceType());
                AiReturnDataVo aiReturnDataVo;
                if (ObjectUtil.equals(bo.getUseModelWay(), 0) && StrUtil.isNotEmpty(bo.getContextId())) {
                    // 上下文缓存模式
                    aiReturnDataVo = aiModel.contextChatCompletionStream(
                            aiModelBo, bo.getContextId(), params,
                            (choice) -> sendMessage(emitter, choice)
                    );
                } else {
                    // 简单对话模式
                    aiReturnDataVo = aiModel.chatCompletionStream(
                            aiModelBo, params,
                            (choice) -> sendMessage(emitter, choice)
                    );
                }
                emitter.sendMessage(JSONUtil.toJsonStr(aiReturnDataVo), "returnData");
            } catch (RRException e) {
                log.error("AI流式代理错误：RRException error: {}", e.getMsg());
                emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", e.getCode(), "msg", e.getMsg())), "error");
            } catch (Exception e) {
                log.error("AI流式代理错误：Exception error: {}", e.getMessage());
                emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", 7005, "msg", aiError)), "error");
            } finally {
                emitter.sendStop2();
            }
        });
    }

    /**
     * 将 AI 流式返回的 choice 通过 SSE 发送给客户端。
     * 区分推理内容（thinking）和文本内容（text）两种消息类型。
     */
    public void sendMessage(CustomizeSseEmitter emitter, ChatCompletionChoice choice){
        ChatMessage message = choice.getMessage();
        if (ObjectUtil.isNotEmpty(message.getReasoningContent())){
            emitter.sendMessage(JSONUtil.toJsonStr(Map.of("content", message.getReasoningContent(), "type", "thinking")), "message");

        }else if (ObjectUtil.isNotEmpty(message.getContent())){
            emitter.sendMessage(JSONUtil.toJsonStr(Map.of("content", message.getContent(), "type", "text")), "message");
        }
    }
}
