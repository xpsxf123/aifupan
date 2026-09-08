package com.jiuyu.replay.ai.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.ai.rse.SceneSliceRse;
import com.jiuyu.replay.ai.bo.SaveSceneSliceBo;
import com.jiuyu.replay.ai.vo.SceneSliceStatusVo;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 场景切片 BLL
 *
 * @author lj
 * @date 2026-07-06
 */
@Slf4j
@Component
@AllArgsConstructor
public class SceneSliceBll {

    private static final String IMG_BUCKET = "replay-images";
    private static final String DEFAULT_MODEL_CODE = "dataScreenshot-qwen-vl-max";

    private final SceneSliceRse sceneSliceRse;
    private final UserFeign userFeign;
    private final SystemKvProducer systemKvProducer;
    private final AiModelFeign aiModelFeign;
    private final AiChatFeign aiChatFeign;
    private final OssUtils ossUtils;

    /**
     * JDK 21 虚拟线程 executor（{@code VirtualThreadExecutorConfig#scriptMonitorVtExecutor}）。
     */
    private final Executor scriptMonitorVtExecutor;

    /**
     * 获取场景切片状态（仅查询，不创建记录）
     */
    public R<SceneSliceStatusVo> getStatus(String videoId) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        SceneSliceStatusVo vo = sceneSliceRse.getStatus(
                videoId, user.getId(), user.getActiveTenantId());
        return R.ok(vo);
    }

    /**
     * 保存客户端上传完成后的记录并异步触发AI分析
     */
    public R<String> save(SaveSceneSliceBo bo) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        var entity = sceneSliceRse.createRecord(bo, user.getId(), user.getActiveTenantId());

        CompletableFuture.runAsync(() -> triggerAiAnalysis(entity.getId(), entity.getOssKey()),
                scriptMonitorVtExecutor);

        return R.ok("保存成功");
    }

    /**
     * 执行AI图像分析（虚拟线程异步执行）
     */
    public void triggerAiAnalysis(Long sliceId, String ossKey) {
        log.info("[场景切片] 开始AI分析，sliceId={}", sliceId);
        try {
            sceneSliceRse.updateAnalysisStartTime(sliceId);

            String imageUrl = ossUtils.getSignDownloadUrl(IMG_BUCKET, ossKey, true);

            String modelCode = systemKvProducer.getValueByKey("scene_slicing_ai_model_code", DEFAULT_MODEL_CODE);
            AiModelInfoVo modelInfo = aiModelFeign.getByCode(modelCode);
            if (modelInfo == null) {
                throw new RuntimeException("AI模型配置获取失败，modelCode=" + modelCode);
            }

            AiModelBo modelConfig = BeanUtil.copyProperties(modelInfo, AiModelBo.class);

            String sceneSlicingAiPrompt = systemKvProducer.getValueByKey("scene_slicing_ai_prompt", "你是一个直播场景分析助手。请分析这张直播结束时的截图，描述场景内容、画面特征和关键信息。请分析这张直播截图。");
            AiMessageBo params = new AiMessageBo();
            // 多模态 vision 消息：图片 URL + 文本
            params.setUser(List.of(
                    Map.of("image", imageUrl),
                    Map.of("text", sceneSlicingAiPrompt)
            ));

            AiReturnDataVo result = aiChatFeign.chatCompletion(modelConfig, params, 0L);
            if (result.getStatus() != 0) {
                throw new RuntimeException("AI调用失败，status=" + result.getStatus());
            }

            log.info("[场景切片] AI分析完成，sliceId={}", sliceId);
            sceneSliceRse.updateAiResult(sliceId, result.getContent());
        } catch (Exception e) {
            log.error("[场景切片] AI分析失败，sliceId={}", sliceId, e);
            String failReason = e.getMessage();
            if (failReason == null || failReason.isEmpty()) {
                failReason = e.getClass().getSimpleName();
            }
            if (failReason.length() > 1024) {
                failReason = failReason.substring(0, 1024);
            }
            sceneSliceRse.updateFail(sliceId, failReason);
        }
    }

    /**
     * 根据视频ID查询场景切片AI结果（供Placeholder解析使用）
     */
    public String getAiResultByVideoId(String videoId) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        if (user == null) return null;
        var entity = sceneSliceRse.getByVideoId(videoId, user.getActiveTenantId());
        return entity != null ? entity.getAiResult() : null;
    }

}
