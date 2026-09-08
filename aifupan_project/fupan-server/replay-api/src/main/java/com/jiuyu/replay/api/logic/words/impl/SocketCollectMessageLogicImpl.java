package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.logic.words.SocketCollectMessageLogic;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.third.tablestore.entity.BarrageBo;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.SocketCollectMessageBo;
import com.jiuyu.replay.words.bo.SocketCollectMessageListBo;
import com.jiuyu.replay.words.bo.UploadSocketDataBo;
import com.jiuyu.replay.words.producer.OceanEngineDataProducer;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.chart.CurveData;
import com.jiuyu.replay.words.vo.chart.CurveDoubleData;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * websocket采集的信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Slf4j
@Service
public class SocketCollectMessageLogicImpl implements SocketCollectMessageLogic {

    @Resource
    private SocketCollectMessageBll socketCollectMessageBll;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TableStoreBll tableStoreBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private BlessBagBll blessBagBll;
    @Resource
    private OceanEngineDataProducer oceanEngineDataProducer;
    @Resource
    private VideoDataViewingBll videoDataViewingBll;
    @Autowired
    private SensitiveWordsBll sensitiveWordsBll;


    @Override
    public R<PageUtils<SocketCollectMessageListVo>> queryPage(SocketCollectMessageListBo socketCollectMessageListBo) {

        return socketCollectMessageBll.queryPage(socketCollectMessageListBo);
    }

    @Override
    public R<SocketCollectMessageInfoVo> info(Long id) {

        return socketCollectMessageBll.info(id);
    }

    @Override
    public R<String> save(SocketCollectMessageBo socketCollectMessageBo) {

        return socketCollectMessageBll.save(socketCollectMessageBo);
    }

    @Override
    public R<String> update(SocketCollectMessageBo socketCollectMessageBo) {

        return socketCollectMessageBll.update(socketCollectMessageBo);
    }

    @Override
    public R<String> delete(Long id) {

        return socketCollectMessageBll.delete(id);
    }

    @Override
    public R<Boolean> socketDataExist(String batchNumber, Long userId, String videoId) {
        return socketCollectMessageBll.socketDataExist(batchNumber, userId, videoId);
    }

    @Override
    public R<List<OnlineNumInfoVo>> getOnlineNumList(String batchNumber, Long userId, String videoId) throws Exception {
        if(userId == null) {
            userId = GlobalObject.getLocalUser().getId();
        }

        return socketCollectMessageBll.getOnlineNumList(batchNumber, userId, videoId);
    }

    @Override
    public R<String> uploadSocketData(UploadSocketDataBo bo) {
        if(bo.getUserId() == null) {
            bo.setUserId(GlobalObject.getLocalUser().getId());
            bo.setTenantId(GlobalObject.getLocalUser().getActiveTenantId());
        }
        if (ObjectUtil.isEmpty(bo.getOnlineMaxNum())) bo.setOnlineMaxNum(-1);
        return socketCollectMessageBll.uploadSocketData(bo);
    }

    @Override
    public R<SynchronizeTwoDayVideoVo> synchronizeTwoDayVideo(Long userId) {
        if (ObjectUtil.isEmpty(userId)){
            UserCacheVo user = GlobalObject.getLocalUser();
            userId = user.getId();
        }
        return socketCollectMessageBll.synchronizeTwoDayVideo(userId);
    }

    @Override
    public R<SocketCollectMessageInfoVo> socketMessageInfoNotJson(Long userId, String videoId, String batchNumber) {
        return socketCollectMessageBll.socketMessageInfoNotJson(userId, videoId, batchNumber);
    }



    @Override
    public R<OnlineChartVo> onlineChartData(String videoId) {
        String redisKey = RedisCacheKey.getRedisKey(RedisCacheKey.onlineChartDataKey, videoId);
        Object o = redisTemplate.opsForValue().get(redisKey);
        if (ObjectUtil.isNotEmpty(o)){
            return R.ok("获取成功", JSONObject.parseObject((String) o, OnlineChartVo.class));
        }

        // 1. 获取视频信息
        AnchorVideoInfoVo data = ResultUtil.getResult(anchorVideoBll.GetByVideoId(videoId));
        if (data == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "获取视频失败");
        }
        String videoIdNew = data.getVideoId();

        // 2. 获取分析详情数据曲线
        R<OnlineChartVo> onlineChartBoR = socketCollectMessageBll.onlineChartData(videoIdNew);
        if (onlineChartBoR.fail() || Objects.isNull(onlineChartBoR.getData())) {
            return onlineChartBoR;
        }
        OnlineChartVo onlineChartVo = onlineChartBoR.getData();
        // 如果没有 进场人数折线数据和 在线人数折线数据 提前返回
        if (ObjectUtil.isEmpty(onlineChartVo.getOnlineDataList()) || ObjectUtil.isEmpty(onlineChartVo.getApproachDataList())) {
            return onlineChartBoR;
        }

        // 计算离场人数
        calculateExitPeopleData(onlineChartVo);

        // 计算语数列表
        calculateLanguageData(onlineChartVo, data);

        // 录制开始时间
        Date startTime = data.getStartTime();
        Long duration = data.getDuration();
        Date endTime = data.getEndTime();
        if (startTime == null || endTime == null || duration == null){
            throw new RRException("此视频没有录制开始结束时间");
        }
        R<List<BarrageBo>> barrageDataList = tableStoreBll.listVideoBarrageByVideoId(data.getUserId(), data.getTenantId(), data.getBatchNumber(), videoIdNew);
        // 计算弹幕曲线
        if (ObjectUtil.isNotEmpty(barrageDataList.getData())) {
            List<BarrageBo> barrageBoList = barrageDataList.getData();
            ArrayList<CurveData> barrageResult = new ArrayList<>(barrageBoList.size());
            List<CurveData> onlineDataList = onlineChartVo.getOnlineDataList();
            int j = 0;
            for (CurveData stringObjectMap : onlineDataList) {
                CurveData map = new CurveData();
                Long dateTime = stringObjectMap.getDateTime();
                Long dateTimeNew = stringObjectMap.getDateTimeNew();
                map.setDateTime(dateTime);
                map.setDateTimeNew(dateTimeNew);
                if (j < barrageBoList.size()) {
                    long newTime = dateTime + (1000 * 60);
                    // 跳过已经计算的j (此处逻辑到底是？待解释)
                    int valueNum = Long.valueOf(barrageBoList.stream().skip(j == 0  ? 0 : j - 1).filter(barrageBo -> {
                        long recordDate = barrageBo.getRecordDate();
                        return recordDate >= dateTime && recordDate < newTime;
                    }).count()).intValue();
                    j += valueNum;
                    map.setValueNum(valueNum);
                }
                barrageResult.add(map);
            }
            onlineChartVo.setBarrageDataList(ObjectUtil.isNotEmpty(barrageResult) ? barrageResult : null);
        }

        //当此曲线有值的时候，通过videoId去请求福袋信息
        R<List<BlessBagInfoVo>> listR = blessBagBll.infoByVideo(videoIdNew);
        if (ObjectUtil.isNotEmpty(listR.getData())){
            long startTimeLong = startTime.getTime();
            long endTimeLong = endTime.getTime();
            List<OnlineChartBlessBagVo> results = listR.getData().stream().filter(blessBag -> {
                if (blessBag.getStartTime() == null || blessBag.getDrawTime() == null) {
                    log.warn("[视频曲线] video bless bag start or end is null, videoId:{}, blessBagId:{}, start:{}, end:{}", videoIdNew, blessBag.getId(), blessBag.getStartTime(), blessBag.getDrawTime());
                    return false;
                }
                return blessBag.getDrawTime() * 1000 >= startTimeLong && blessBag.getStartTime() * 1000 <= endTimeLong;
            }).map(blessBag -> {
                //并且没有超过录制结束时间
                long relativeTime = blessBag.getStartTime() * 1000 - startTimeLong;
                OnlineChartBlessBagVo bean = BeanUtil.toBean(blessBag, OnlineChartBlessBagVo.class);
                if (relativeTime < 0) {
                    bean.setRelativeTime(0L);
                } else {
                    bean.setRelativeTime(relativeTime);
                }
                return bean;
            }).collect(Collectors.toList());
            onlineChartBoR.getData().setBlessBagList(results);
        }

        // 新增巨量曲线数据
        VideoDataViewingConfuseInfoVo engineDataVo = videoDataViewingBll.getOceanEngineDetailsByVideoId(videoIdNew);
        if (ObjectUtil.isNotEmpty(engineDataVo) && ObjectUtil.isNotEmpty(engineDataVo.getOceanEngineProcessList())) {
            oceanEngineDataProducer.processOceanEngineTimeSeriesData(engineDataVo.getOceanEngineProcessList(), onlineChartVo, videoIdNew, startTime, endTime);
        }
        redisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(onlineChartBoR.getData()), 7, TimeUnit.DAYS);
        return onlineChartBoR;
    }

    /**
     * 所有曲线数据列表在第0位插入一个0分钟的时间点（数据全为0）
     */
    private void insertZeroMinutePoint(OnlineChartVo vo, Long startTimeMs) {
        insertCurveDataZero(vo.getApproachDataList(), startTimeMs);
        insertCurveDataZero(vo.getExitPeopleDataList(), startTimeMs);
        insertCurveDataZero(vo.getOnlineDataList(), startTimeMs);
        insertCurveDataZero(vo.getBarrageDataList(), startTimeMs);
        insertCurveDataZero(vo.getLanguageDataList(), startTimeMs);
        insertCurveDataZero(vo.getPayComboCntDataList(), startTimeMs);
        insertCurveDataZero(vo.getFansClubJoinUcntDataList(), startTimeMs);
        insertCurveDataZero(vo.getFollowAnchorUcntDataList(), startTimeMs);
        insertCurveDoubleZero(vo.getPayAmtDataList(), startTimeMs);
        insertCurveDoubleZero(vo.getQianchuanCostDataList(), startTimeMs);
        insertCurveDoubleZero(vo.getRefundAmtDataList(), startTimeMs);
        insertCurveDoubleZero(vo.getNetTransactionRoiDataList(), startTimeMs);
    }

    private void insertCurveDataZero(List<CurveData> list, Long startTimeMs) {
        if (CollUtil.isNotEmpty(list)) {
            CurveData zero = new CurveData();
            zero.setDateTime(startTimeMs);
            zero.setDateTimeNew(0L);
            zero.setValueNum(0);
            list.add(0, zero);
        }
    }

    private void insertCurveDoubleZero(List<CurveDoubleData> list, Long startTimeMs) {
        if (CollUtil.isNotEmpty(list)) {
            CurveDoubleData zero = new CurveDoubleData();
            zero.setDateTime(startTimeMs);
            zero.setDateTimeNew(0L);
            zero.setValueNum(0.0);
            list.add(0, zero);
        }
    }

    /**
     * 计算语数的数据列表
     * 语数 = 这分钟的文字数量
     *
     * @param onlineChartVo 返回的数据
     * @param data          视频信息
     */
    private void calculateLanguageData(OnlineChartVo onlineChartVo, AnchorVideoInfoVo data) {
        try {
            // 获取视频ID
            String videoId = data.getVideoId();
            if (ObjectUtil.isEmpty(videoId)) {
                return;
            }

            // 获取分析结果
            R<AnalysisResultVo> analysisResultR = sensitiveWordsBll.getLastAnalysisListByOss(videoId, WordsEnum.sourceType.VIDEO.getCode());
            if (analysisResultR == null || analysisResultR.fail() || ObjectUtil.isEmpty(analysisResultR.getData())) {
                return;
            }

            AnalysisResultVo analysisResult = analysisResultR.getData();
            List<SentenceMarkVo> sentenceMarkVos = analysisResult.getSentenceMarkVos();
            if (ObjectUtil.isEmpty(sentenceMarkVos)) {
                return;
            }

            // 获取进场人数数据作为时间基准
            List<CurveData> approachDataList = onlineChartVo.getApproachDataList();
            if (ObjectUtil.isEmpty(approachDataList)) {
                return;
            }

            // 初始化语数列表
            List<CurveData> languageDataList = new ArrayList<>();

            // 获取每一秒中对应的文字，从0毫米开始
//            sentenceMarkVos.stream()
//                    .flatMap(item -> item.getItems()
//                            .stream()
//                            .filter(ObjectUtil::isNotEmpty)
//                            .map(temp -> Map.of(temp.getStartTime(), temp.getWord()))
//                    )

            // 遍历进场人数数据，计算对应的语数
            for (CurveData approachData : approachDataList) {
                CurveData languageData = new CurveData();
                languageData.setDateTime(approachData.getDateTime());
                languageData.setDateTimeNew(approachData.getDateTimeNew());

                // 计算这个时间段内的文字数量
                Integer languageCount = calculateLanguageCountByTime(sentenceMarkVos, approachData.getDateTimeNew(), data);
                languageData.setValueNum(languageCount);

                languageDataList.add(languageData);
            }

            // 设置语数数据到结果对象
            onlineChartVo.setLanguageDataList(languageDataList);
        } catch (Exception e) {
            log.error("计算语数数据失败", e);
        }
    }

    /**
     * 根据时间计算该时间段内的文字数量
     *
     * @param sentenceMarkVos 段落信息列表
     * @param relativeTime    相对时间（毫秒）
     * @param data            视频信息
     * @return 文字数量
     */
    private Integer calculateLanguageCountByTime(List<SentenceMarkVo> sentenceMarkVos, Long relativeTime, AnchorVideoInfoVo data) {
        if (ObjectUtil.isEmpty(sentenceMarkVos) || relativeTime == null) {
            return 0;
        }

        // 计算当前时间段（以分钟为单位）
        long currentMinute = relativeTime / (60 * 1000);
        long nextMinute = currentMinute + 1;
        long currentTimeStart = currentMinute * 60 * 1000;
        long currentTimeEnd = nextMinute * 60 * 1000;

        int languageCount = 0;

        // 遍历所有段落，计算在这个时间段内的文字数量
        for (SentenceMarkVo sentenceMarkVo : sentenceMarkVos) {
            String content = sentenceMarkVo.getContent();
            if (ObjectUtil.isEmpty(content)) {
                continue;
            }

            // 获取段落序号（从1开始）
            Integer currentSort = sentenceMarkVo.getCurrentSort();
            if (currentSort == null || currentSort < 1) {
                continue;
            }

            // 简单估算：假设每个段落均匀分布在整个直播时间内
            // 这里使用段落序号来估算时间位置
            // 实际应用中可能需要更精确的时间戳信息
            long estimatedTime = (long) (currentSort - 1) * 60 * 1000; // 每个段落约1分钟

            // 检查该段落是否在当前时间段内
            if (estimatedTime >= currentTimeStart && estimatedTime < currentTimeEnd) {
                // 计算文字数量（中文字符数）
                languageCount += content.length();
            }
        }

        return languageCount;
    }

    /**
     * 计算离场人数数据
     * 离场人数 = 进场人数-（这分钟在线-上一分钟在线）
     *
     * @param onlineChartVo 数据
     */
    private void calculateExitPeopleData(OnlineChartVo onlineChartVo) {
        // 获取进场人数和在线人数数据
        List<CurveData> approachDataList = onlineChartVo.getApproachDataList();
        List<CurveData> onlineDataList = onlineChartVo.getOnlineDataList();

        // 如果数据为空，直接返回
        if (ObjectUtil.isEmpty(approachDataList) || ObjectUtil.isEmpty(onlineDataList)) {
            return;
        }

        // 初始化离场人数列表
        List<CurveData> exitPeopleDataList = new ArrayList<>();

        // 遍历进场人数数据，计算对应的离场人数
        for (int i = 0; i < approachDataList.size(); i++) {
            CurveData approachData = approachDataList.get(i);
            Integer approachNum = approachData.getValueNum();

            // 计算这分钟在线人数与上一分钟在线人数的差值
            Integer onlineChange = 0;
            if (i < onlineDataList.size()) {
                Integer currentOnline = onlineDataList.get(i).getValueNum();
                Integer previousOnline = i > 0 ? onlineDataList.get(i - 1).getValueNum() : 0;
                onlineChange = currentOnline - previousOnline;
            }

            // 计算离场人数：离场人数 = 进场人数 - (这分钟在线 - 上一分钟在线)
            int exitNum = approachNum - onlineChange;
            // 离场人数不能为负数
            exitNum = Math.max(exitNum, 0);

            // 创建离场人数数据项
            CurveData exitData = new CurveData();
            exitData.setDateTime(approachData.getDateTime());
            exitData.setDateTimeNew(approachData.getDateTimeNew());
            exitData.setValueNum(exitNum);

            exitPeopleDataList.add(exitData);
        }

        // 设置离场人数数据到结果对象
        onlineChartVo.setExitPeopleDataList(exitPeopleDataList);
    }

    @Override
    public R<OnlineChartVo> onlineChartData(String videoId, Integer step) {
        R<OnlineChartVo> onlineChartBoR = onlineChartData(videoId);
        onlineChartPost(onlineChartBoR.getData());
        if (onlineChartBoR.getCode() == 0 && onlineChartBoR.getData() != null && step > 1) {
            onlineChartBoR = processOnlineChartData(onlineChartBoR.getData(), step);
        }
        // 所有曲线数据在最终返回前插入0分钟起点（聚合后再插，不影响刻度计算）
        OnlineChartVo data = onlineChartBoR != null ? onlineChartBoR.getData() : null;
        if (data != null && CollUtil.isNotEmpty(data.getApproachDataList())) {
            Long startTime = data.getApproachDataList().get(0).getDateTime();
            insertZeroMinutePoint(data, startTime);
        }
        return onlineChartBoR;
    }

    /**
     * 获取视频是否包含弹幕
     *
     * @param videoIds 视频id
     *
     * @return 视频是否包含弹幕
     */
    @Override
    public Map<String, Boolean> getVideoHasBarragesMap(Collection<String> videoIds) {
        return socketCollectMessageBll.getVideoHasBarragesMap(videoIds);
    }

    /**
     * 获取视频是否包含在线曲线
     *
     * @param videoIds 视频id
     *
     * @return 视频是否包含弹幕
     */
    @Override
    public Map<String, SocketCollectMessageVo> getVideoHasChartDataMap(Collection<String> videoIds) {
        return socketCollectMessageBll.getVideoHasChartDataMap(videoIds);
    }

    private void onlineChartPost(OnlineChartVo onlineChartVo) {
        if (ObjectUtil.isEmpty(onlineChartVo)) {
            return;
        }
        // 成交金额折线数据-转元
        exceptOneHundred(onlineChartVo.getPayAmtDataList());

        // 投放消耗折线数据-转元
        exceptOneHundred(onlineChartVo.getQianchuanCostDataList());

        // 退款金额折线数据-转元
        exceptOneHundred(onlineChartVo.getRefundAmtDataList());
    }

    private void exceptOneHundred(List<CurveDoubleData> item){
        if (ObjectUtil.isNotEmpty(item)) {
            for (CurveDoubleData curveData : item) {
                curveData.setValueNum(ObjectUtil.defaultIfNull(curveData.getValueNum(), 0.0) / 100);
            }
        }
    }

    /**
     * 处理在线曲线的刻度
     *
     * @param onlineChartVo
     * @param step
     * @return
     */
    private R<OnlineChartVo> processOnlineChartData(OnlineChartVo onlineChartVo, Integer step) {
        if (onlineChartVo == null || step == null || step <= 1) {
            return R.ok(onlineChartVo);
        }

        // 处理进场人数折线数据 - 累加
        if (onlineChartVo.getApproachDataList() != null) {
            onlineChartVo.setApproachDataList(processDataListWithSum(onlineChartVo.getApproachDataList(), step));
        }

        // 处理在线人数折线数据 - 求平均值
        if (onlineChartVo.getOnlineDataList() != null) {
            onlineChartVo.setOnlineDataList(processDataListWithAverage(onlineChartVo.getOnlineDataList(), step));
        }

        // 处理弹幕折线数据 - 累加
        if (onlineChartVo.getBarrageDataList() != null) {
            onlineChartVo.setBarrageDataList(processDataListWithSum(onlineChartVo.getBarrageDataList(), step));
        }

        // 处理成交折线数据 - 累加
        if (onlineChartVo.getPayComboCntDataList() != null) {
            onlineChartVo.setPayComboCntDataList(processDataListWithSum(onlineChartVo.getPayComboCntDataList(), step));
        }

        // 处理成交金额折线数据 - 累加
        if (onlineChartVo.getPayAmtDataList() != null) {
            onlineChartVo.setPayAmtDataList(processDataListWithSumDouble(onlineChartVo.getPayAmtDataList(), step));
        }

        // 处理新增直播团人数折线数据 - 累加
        if (onlineChartVo.getFansClubJoinUcntDataList() != null) {
            onlineChartVo.setFansClubJoinUcntDataList(processDataListWithSum(onlineChartVo.getFansClubJoinUcntDataList(), step));
        }

        // 处理新增粉丝数量折线数据 - 累加
        if (onlineChartVo.getFollowAnchorUcntDataList() != null) {
            onlineChartVo.setFollowAnchorUcntDataList(processDataListWithSum(onlineChartVo.getFollowAnchorUcntDataList(), step));
        }

        // 处理投放消耗折线数据 - 累加
        if (onlineChartVo.getQianchuanCostDataList() != null) {
            onlineChartVo.setQianchuanCostDataList(processDataListWithSumDouble(onlineChartVo.getQianchuanCostDataList(), step));
        }

        // 处理退款金额折线数据 - 累加
        if (onlineChartVo.getRefundAmtDataList() != null) {
            onlineChartVo.setRefundAmtDataList(processDataListWithSumDouble(onlineChartVo.getRefundAmtDataList(), step));
        }

        // 处理净成交ROI折线数据 - 从聚合后的消耗/成交/退款重新计算
        if (onlineChartVo.getNetTransactionRoiDataList() != null
                && onlineChartVo.getQianchuanCostDataList() != null
                && onlineChartVo.getPayAmtDataList() != null
                && onlineChartVo.getRefundAmtDataList() != null) {
            onlineChartVo.setNetTransactionRoiDataList(
                    recalculateRoiFromAggregatedData(
                            onlineChartVo.getQianchuanCostDataList(),
                            onlineChartVo.getPayAmtDataList(),
                            onlineChartVo.getRefundAmtDataList()));
        }

        return R.ok(onlineChartVo);
    }

    /**
     * 处理数据列表 - 累加方式
     *
     * @param dataList 原始数据列表
     * @param step     步长（分钟）
     * @return 处理后的数据列表
     */
    private List<CurveData> processDataListWithSum(List<CurveData> dataList, Integer step) {
        if (dataList == null || dataList.isEmpty() || step <= 1) {
            return dataList;
        }

        List<CurveData> result = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i += step) {
            CurveData aggregatedData = new CurveData();

            // dateTime 取第一个点（桶起点绝对时间）
            // dateTimeNew 取桶终点偏移量（毫秒）：(起始索引 + 步长) × 60秒
            // 例 step=5, i=0 → 300000ms(5分钟)，前端标签显示"5"
            // 不用 firstData.getDateTimeNew() 的原因：那是桶起点，聚合后标签会偏移
            CurveData firstData = dataList.get(i);
            aggregatedData.setDateTime(firstData.getDateTime());
            aggregatedData.setDateTimeNew((long)(i + step) * 60000L);

            // 累加step个数据点的值
            int sum = 0;
            int endIndex = Math.min(i + step, dataList.size());

            for (int j = i; j < endIndex; j++) {
                CurveData currentData = dataList.get(j);
                if (currentData.getValueNum() != null) {
                    sum += currentData.getValueNum();
                }
            }

            aggregatedData.setValueNum(sum);
            result.add(aggregatedData);
        }

        return result;
    }

    /**
     * 处理数据列表 - 累加方式（Double版本）
     */
    private List<CurveDoubleData> processDataListWithSumDouble(List<CurveDoubleData> dataList, Integer step) {
        if (dataList == null || dataList.isEmpty() || step <= 1) {
            return dataList;
        }

        List<CurveDoubleData> result = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i += step) {
            // dateTimeNew 取桶终点偏移量：聚合后标签显示桶终点分钟数
            // 不用 firstData.getDateTimeNew() 的原因：那是桶起点，聚合后标签会偏移
            CurveDoubleData aggregatedData = new CurveDoubleData();
            CurveDoubleData firstData = dataList.get(i);
            aggregatedData.setDateTime(firstData.getDateTime());
            aggregatedData.setDateTimeNew((long)(i + step) * 60000L);

            double sum = 0;
            int endIndex = Math.min(i + step, dataList.size());
            for (int j = i; j < endIndex; j++) {
                CurveDoubleData currentData = dataList.get(j);
                if (currentData.getValueNum() != null) {
                    sum += currentData.getValueNum();
                }
            }
            aggregatedData.setValueNum(sum);
            result.add(aggregatedData);
        }

        return result;
    }

    /**
     * 处理数据列表 - 平均值方式
     *
     * @param dataList 原始数据列表
     * @param step     步长（分钟）
     * @return 处理后的数据列表
     */
    private List<CurveData> processDataListWithAverage(List<CurveData> dataList, Integer step) {
        if (dataList == null || dataList.isEmpty() || step <= 1) {
            return dataList;
        }

        List<CurveData> result = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i += step) {
            CurveData aggregatedData = new CurveData();

            // dateTimeNew 取桶终点偏移量：聚合后标签显示桶终点分钟数
            // 不用 firstData.getDateTimeNew() 的原因：那是桶起点，聚合后标签会偏移
            CurveData firstData = dataList.get(i);
            aggregatedData.setDateTime(firstData.getDateTime());
            aggregatedData.setDateTimeNew((long)(i + step) * 60000L);

            // 计算step个数据点的平均值
            int sum = 0;
            int count = 0;
            int endIndex = Math.min(i + step, dataList.size());

            for (int j = i; j < endIndex; j++) {
                CurveData currentData = dataList.get(j);
                if (currentData.getValueNum() != null) {
                    sum += currentData.getValueNum();
                    count++;
                }
            }

            // 计算平均值，如果没有有效数据则设为0
            int average = count > 0 ? Math.round((float) sum / count) : 0;
            aggregatedData.setValueNum(average);
            result.add(aggregatedData);
        }

        return result;
    }

    /**
     * 从聚合后的消耗/成交/退款数据重新计算净成交ROI
     * ROI = (成交金额 - 退款金额) / 投放消耗，保留2位小数
     */
    private List<CurveDoubleData> recalculateRoiFromAggregatedData(
            List<CurveDoubleData> costList, List<CurveDoubleData> payAmtList, List<CurveDoubleData> refundList) {
        int size = Math.min(Math.min(costList.size(), payAmtList.size()), refundList.size());
        List<CurveDoubleData> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            double cost = ObjectUtil.defaultIfNull(costList.get(i).getValueNum(), 0.0);
            double payAmt = ObjectUtil.defaultIfNull(payAmtList.get(i).getValueNum(), 0.0);
            double refund = ObjectUtil.defaultIfNull(refundList.get(i).getValueNum(), 0.0);
            double netAmt = payAmt - refund;

            CurveDoubleData point = new CurveDoubleData();
            point.setDateTime(costList.get(i).getDateTime());
            point.setDateTimeNew(costList.get(i).getDateTimeNew());

            if (cost > 0 && netAmt > 0) {
                double roi = netAmt / cost;
                point.setValueNum(Math.round(roi * 100.0) / 100.0);
            } else {
                point.setValueNum(0.0);
            }
            result.add(point);
        }

        return result;
    }
}

