package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.api.logic.words.DataScreenshotLogic;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.CustomizeSseEmitter;
import com.jiuyu.replay.common.utils.ExecutorUtil;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.AiTokenUseRecordBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.IsPropertyHaveBo;
import com.jiuyu.replay.order.vo.IsPropertyHaveVo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.system.bll.DictDataBll;
import com.jiuyu.replay.ai.factory.ModelFactoryUtils;
import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import com.jiuyu.replay.words.bll.DataScreenshotBll;
import com.jiuyu.replay.words.bll.TradeBll;
import com.jiuyu.replay.words.bll.UploadFileBll;
import com.jiuyu.replay.words.bo.DataScreenshotBo;
import com.jiuyu.replay.words.bo.DataScreenshotListBo;
import com.jiuyu.replay.words.bo.DataScreenshotUploadBo;
import com.jiuyu.replay.words.bo.ScreenshotAnalysisBo;
import com.jiuyu.replay.words.vo.DataScreenshotInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotListVo;
import com.jiuyu.replay.words.vo.DataScreenshotVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 数据截图记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Service
@Slf4j
public class DataScreenshotLogicImpl implements DataScreenshotLogic {

    @Resource
    private DataScreenshotBll dataScreenshotBll;
    @Resource
    private UserPropertyBll userPropertyBll;
    @Resource
    private AiModelBll aiModelBll;
    @Resource
    private ImgOssUtils imgOssUtils;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private AiTokenUseRecordBll aiTokenUseRecordBll;
    @Resource
    private UserBll userBll;
    @Resource
    private DictDataBll dictDataBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private UploadFileBll uploadFileBll;
    @Resource
    private TradeBll tradeBll;

    @Override
    public R<PageUtils<DataScreenshotListVo>> queryPage(DataScreenshotListBo dataScreenshotListBo) {

        return dataScreenshotBll.queryPage(dataScreenshotListBo);
    }

    @Override
    public R<DataScreenshotInfoVo> info(Long id) {

        return dataScreenshotBll.info(id);
    }

    @Override
    public R<String> save(DataScreenshotBo dataScreenshotBo) {

        return dataScreenshotBll.save(dataScreenshotBo);
    }

    @Override
    public R<String> update(DataScreenshotBo dataScreenshotBo) {

        return dataScreenshotBll.update(dataScreenshotBo);
    }

    @Override
    public R<String> delete(Long id) {

        return dataScreenshotBll.delete(id);
    }

    @Override
    public R<DataScreenshotInfoVo> screenshotUpload(DataScreenshotUploadBo uploadBo) {
        DataScreenshotBo data = BeanUtil.copyProperties(uploadBo, DataScreenshotBo.class);
        return dataScreenshotBll.screenshotUpload(data);
    }

    @Override
    public void screenshotAnalysis(CustomizeSseEmitter emitter, ScreenshotAnalysisBo analysisBo) {
        String aiImgCode = "imgIdentifyNum";
        // 校验
        if (ObjectUtil.isEmpty(analysisBo.getIds())) {
            emitter.sendMessage("ids不能为空", "error");
            emitter.sendStop();
            return;
        }
        R<List<DataScreenshotInfoVo>> dataRes = dataScreenshotBll.getByIds(analysisBo.getIds());
        if (dataRes.getCode() != 0 || ObjectUtil.isEmpty(dataRes.getData()) || dataRes.getData().size() != analysisBo.getIds().size()){
            emitter.sendMessage("存在不存在的数据", "error");
            emitter.sendStop();
            return;
        }
        // 获取数据
        List<DataScreenshotInfoVo> dataList = dataRes.getData();
        Map<Long, DataScreenshotInfoVo> dataMap = dataList.stream().collect(Collectors.toMap(DataScreenshotInfoVo::getId, Function.identity(), (a, b) -> b));

        dataScreenshotBll.setUserIdAndTenantId(analysisBo);
        R<UserInfoVo> info = userBll.info(analysisBo.getUserId(), false);
        if (info.getCode() == 0 && info.getData() != null){
            analysisBo.setNickName(info.getData().getNickName());
        }

        // 预扣
        IsPropertyHaveBo bo = new IsPropertyHaveBo();
        bo.setUserId(analysisBo.getUserId());
        bo.setCode(aiImgCode);
        bo.setThisUseNum((long) analysisBo.getIds().size());
        R<IsPropertyHaveVo> have = userPropertyBll.isHave(bo);
        if (have.getCode() != 0 || !have.getData().getIsHave()){
            emitter.sendMessage("图片分析数量不足，预扣失败", "error");
            emitter.sendStop();
            return;
        }

        // 修改数据额截图的状态
        dataScreenshotBll.updateStatusByIds(analysisBo.getIds(), 2);
        if (ObjectUtil.isNotEmpty(analysisBo.getIds())){
            for (Long id : analysisBo.getIds()) {
                // 发生消息到前端
                Map<String, Object> send = new HashMap<>();
                send.put("id", id.toString());
                send.put("status", 2);
                emitter.sendMessage(JSONUtil.toJsonStr(send), "message");
            }
        }

        // 获取AI身份设置
        String cueWord;

        R<List<DictDataListVo>> cueWordR = dictDataBll.listByTypeLogo("data_screenshot_cue_word");
        if (cueWordR.getCode() == 0 && ObjectUtil.isNotEmpty(cueWordR.getData())  && !cueWordR.getData().isEmpty()){
            cueWord = cueWordR.getData().get(0).getValue();
        } else {
            cueWord = "整理图片中的数据，不需要有额外说明";
        }

        // 处理行业暂位符
        String replace = "trade";
        if (cueWord.contains(StrUtil.format("#{{}}", replace))) {
            Long tradeId = null;
            if (ObjectUtil.isNotEmpty(analysisBo.getSourceType()) && analysisBo.getSourceType() == 0){
                // 视频
                R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.GetByVideoId(analysisBo.getSourceId());
                if (anchorVideoInfoVoR.getCode() == 0 && anchorVideoInfoVoR.getData() != null){
                    tradeId = anchorVideoInfoVoR.getData().getTradeId();
                }
            }else if (ObjectUtil.isNotEmpty(analysisBo.getSourceType()) && analysisBo.getSourceType() == 1){
                // 文件
                R<UploadFileInfoVo> uploadFileInfoVoR = uploadFileBll.infoByFileId(analysisBo.getSourceId());
                if (ObjectUtil.isNotEmpty(uploadFileInfoVoR) && ObjectUtil.isNotEmpty(uploadFileInfoVoR.getData())) {
                    tradeId = uploadFileInfoVoR.getData().getTradeId();
                }
            }
            String tradeName = null;
            if (tradeId != null){
                R<TradeInfoVo> info1 = tradeBll.info(tradeId);
                if (info1.getCode() == 0 && info1.getData() != null){
                    tradeName = info1.getData().getName();
                }
            }
            String trade = ObjectUtil.defaultIfEmpty(tradeName, "某行业");
            cueWord = cueWord.replaceAll(StrUtil.format("#\\{{}}", replace), trade);
        }


        // 异步调用
        try {
            String finalCueWord = cueWord;
            ExecutorUtil.customPool.execute(()  -> {
                try{
                    log.info("开始异步调用, id={}", Thread.currentThread().getId());
                    // 获取模型配置
                    R<AiModelInfoVo> voR = aiModelBll.getByCode("dataScreenshot-qwen-vl-max");
                    if (voR.getCode() != 0){
                        log.info("错误：查询qwen-vl-max模型配置失败，msg={}, data={}", voR.getMsg(), voR.getData());
                        emitter.sendMessage("图片分析失败，请稍后重试", "error");
                        return;
                    }

                    AiModelInfoVo modelConfig = voR.getData();

                    AiModel aiModel = ModelFactoryUtils.getAiModel(modelConfig.getResourceType());

                    List<CompletableFuture<AiReturnDataVo>> futureList = new ArrayList<>();

                    // 使用多线程调用视觉理解
                    for (Long id : analysisBo.getIds()) {
                        // 获取数据
                        DataScreenshotInfoVo screenshotInfoVo = dataMap.get(id);

                        AiMessageBo params = new AiMessageBo();
                        params.setSystem(List.of(Map.of("text", "你是个图片识别高手。")));
                        params.setUser(Arrays.asList(
                                Map.of("image", imgOssUtils.getUrl(screenshotInfoVo.getSourceImagesAddress())),
                                Map.of("text", finalCueWord)
                        ));

                        futureList.add(CompletableFuture.supplyAsync(()  -> {
                            AiReturnDataVo result = aiModel.chatCompletion(BeanUtil.copyProperties(modelConfig, AiModelBo.class), params, id);

                            // 记录aiToken消耗量
                            AiTokenUseRecordBo aiTokenUseRecordBo = BeanUtil.copyProperties(result, AiTokenUseRecordBo.class);
                            aiTokenUseRecordBo.setId(null);
                            aiTokenUseRecordBo.setTenantId(analysisBo.getTenantId());
                            aiTokenUseRecordBo.setUserId(analysisBo.getUserId());
                            aiTokenUseRecordBo.setModelName(modelConfig.getModelName());
                            aiTokenUseRecordBo.setRequestSourceType(1);
                            aiTokenUseRecordBo.setUseSourceType(AiEnums.useSourceType.DATA_SCREENSHOT.getCode());
                            aiTokenUseRecordBo.setUseSourceId(id.toString());
                            aiTokenUseRecordBo.setAssistantType(AiEnums.askType.SCREENSHOT.getCode());
                            R<AiTokenUseRecordInfoVo> save = aiTokenUseRecordBll.save(aiTokenUseRecordBo);
                            if (save.getCode() == 0 && ObjectUtil.isNotEmpty(save.getData())) {
                                result.setAiTokenId(Collections.singletonList(save.getData().getId()));
                            }


                            Integer status = result.getStatus();
                            // 添加到结果中
                            DataScreenshotBo screenshotBo = new DataScreenshotBo();
                            screenshotBo.setId(result.getSourceId());
                            screenshotBo.setScreenshotStatus(status == 0 ? 3 : 4);
                            if (status == 0) screenshotBo.setAiContent(result.getContent());
                            dataScreenshotBll.update(screenshotBo);

                            // 发生消息到前端
                            Map<String, Object> send = new HashMap<>();
                            send.put("id", screenshotBo.getId().toString());
                            send.put("status", screenshotBo.getScreenshotStatus());
                            if (status == 0) send.put("aiContent", screenshotBo.getAiContent());
                            emitter.sendMessage(JSONUtil.toJsonStr(send), "message");
                            return result;
                        }));

                    }

                    // 等待所有任务完成
                    CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new  CompletableFuture[0]));

                    CompletableFuture<List<AiReturnDataVo>> allResults = allFutures.thenApply(v  ->
                            futureList.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList())
                    );

                    // 获取所有任务的返回结果
                    List<AiReturnDataVo> results = null;
                    try {
                        results = allResults.get();
                    } catch (InterruptedException e) {
                        log.info("调用视觉理解失败InterruptedException，message={}, 堆栈={}", e.getMessage(), CommonUtils.getExceptionStack(e));
                        sendErrorMessage(emitter, analysisBo.getIds());
                        return;
                    } catch (ExecutionException e) {
                        log.info("调用视觉理解失败ExecutionException，message={}, 堆栈={}", e.getMessage(), CommonUtils.getExceptionStack(e));
                        sendErrorMessage(emitter, analysisBo.getIds());
                        return;
                    }

                    // 预扣减成功，则减去aiImgCode
                    List<AiReturnDataVo> successData = results.stream().filter(item -> item.getStatus() == 0).toList();
                    List<AiReturnDataVo> errorData = results.stream().filter(item -> item.getStatus() == 1).toList();
                    if (ObjectUtil.isNotEmpty(successData)){
                        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
                        assets.setCode(aiImgCode);
                        assets.setNum((long) -(successData.size()));
                        assets.setRedisId(have.getData().getRedisId());
                        assets.setWithholdId(have.getData().getWithholdId());
                        assets.setUserId(analysisBo.getUserId());
                        assets.setUserName(analysisBo.getNickName());
                        assets.setAiTokenIds(successData.stream().flatMap(item -> item.getAiTokenId().stream()).distinct().toList());
                        assets.setClearWithholdCache(userPropertyBll::removeTempUserProperty);
                        UserPropertyImpl.use(assets);
                    }else{
                        // 如果没有一个是成功的，则删除redis缓存
                        IsPropertyHaveVo data = have.getData();
                        userPropertyBll.removeTempUserProperty(data.getWithholdId(), data.getRedisId());
//                        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeCacheKey, have.getData().getRedisId(), "*", "*"));
                    }
                    if (ObjectUtil.isNotEmpty(errorData)){
                        sendErrorMessage(emitter, analysisBo.getIds());
                        log.info("数据截图分析失败{}张图片", errorData.size());
                    }
                }catch (Exception e){
                    sendErrorMessage(emitter, analysisBo.getIds());
                    log.info("数据截图分析报错，message={}, 堆栈={}", e.getMessage(), CommonUtils.getExceptionStack(e));
                }finally {
                    // 结束sse发送
                    emitter.sendStop();
                    log.info("结束异步调用, id={}", Thread.currentThread().getId());
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            userPropertyBll.removeTempUserProperty(have.getData().getWithholdId(), have.getData().getRedisId());
            sendErrorMessage(emitter, analysisBo.getIds());
            emitter.sendStop();
        }
    }

    private void sendErrorMessage(CustomizeSseEmitter emitter, List<Long> ids) {
        R<List<DataScreenshotInfoVo>> screenshotBllByIds = dataScreenshotBll.getByIds(ids);
        if (screenshotBllByIds.getCode() == 0 && ObjectUtil.isNotEmpty(screenshotBllByIds.getData())){
            List<Long> listIds = screenshotBllByIds.getData().stream().filter(item -> item.getScreenshotStatus() == 2).map(DataScreenshotVo::getId).toList();
            if (ObjectUtil.isNotEmpty(listIds)){
                // 修改数据额截图的状态
                dataScreenshotBll.updateStatusByIds(listIds, 4);
                if (ObjectUtil.isNotEmpty(listIds)){
                    for (Long id : listIds) {
                        // 发生消息到前端
                        Map<String, Object> send = new HashMap<>();
                        send.put("id", id.toString());
                        send.put("status", 4);
                        emitter.sendMessage(JSONUtil.toJsonStr(send), "message");
                    }
                }
            }
        }
    }

    /**
     * 根据视频查询数据截图列表
     * @param batchNumber
     * @param videoId
     * @return
     */
    @Override
    public R<List<DataScreenshotListVo>> dataScreenshotList(Integer batchNumber, String videoId) {
        return dataScreenshotBll.dataScreenshotList(batchNumber, videoId);
    }

    /**
     * 根据batchNumber和videoId查询数据截图列表
     * @param sourceType
     * @param sourceId
     * @return
     */
    @Override
    public R<List<DataScreenshotListVo>> getExistDataScreenshotList(Integer sourceType, String sourceId) {
        return dataScreenshotBll.getExistDataScreenshotList(sourceType, sourceId);
    }
}

