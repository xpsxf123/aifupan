package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.producer.SocketCollectMessageProducer;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.chart.CurveData;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * websocket采集的信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocketCollectMessageBll {

    private final WordsProperties wordsProperties;


    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TableStoreFeign tableStoreFeign;


    /**
     * websocket采集的信息列表
     * @param socketCollectMessageListBo websocket采集的信息列表查询参数
     * @return
     */
    public R<PageUtils<SocketCollectMessageListVo>> queryPage(SocketCollectMessageListBo socketCollectMessageListBo) {

        return R.ok("获取成功", socketCollectMessageProducer.queryPage(socketCollectMessageListBo));
    }

    /**
    * websocket采集的信息信息
    * @param id websocket采集的信息id
    * @return
    */
    public R<SocketCollectMessageInfoVo> info(Long id) {

        SocketCollectMessageInfoVo socketCollectMessageInfoVo = socketCollectMessageProducer.info(id);
        return R.ok("获取成功", socketCollectMessageInfoVo);
    }

    /**
     * 新增websocket采集的信息
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
    public R<String> save(SocketCollectMessageBo socketCollectMessageBo) {

        SocketCollectMessageInfoVo socketCollectMessageInfoVo = socketCollectMessageProducer.save(socketCollectMessageBo);
        return R.ok("添加成功");
    }

    /**
     * 修改websocket采集的信息
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
    public R<String> update(SocketCollectMessageBo socketCollectMessageBo) {

        socketCollectMessageProducer.update(socketCollectMessageBo);
        return R.ok("修改成功");
    }

    public R<String> updateTotalBarrageNumByVideoId(String batchNumber, Long userId, String videoId, Integer totalBarrageNum){
        socketCollectMessageProducer.updateTotalBarrageNumByVideoId(batchNumber, userId, videoId, totalBarrageNum);
        return R.ok();
    }

    /**
     * 删除websocket采集的信息
     * @param id websocket采集的信息id
     * @return
     */
    public R<String> delete(Long id) {

        socketCollectMessageProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取websocket采集的信息
     * @param batchNumber
     * @param userId
     * @param videoId
     * @return
     */
    public R<SocketCollectMessageInfoVo> getByBatch(String batchNumber, Long userId, String videoId){
        SocketCollectMessageInfoVo byBatch = socketCollectMessageProducer.getByBatch(batchNumber, userId, videoId);
        return R.ok(byBatch);
    }

    public R<SocketCollectMessageInfoVo> socketMessageInfoNotJson(Long userId, String videoId, String batchNumber) {
        RRException.isNotEmpty(videoId, "视频id不能为空");
        AnchorVideoInfoVo byVideoId = anchorVideoProducer.getByVideoId(videoId);
        if (batchNumber == null){
            RRException.isNotEmpty(byVideoId, "视频信息不存在");
            batchNumber = byVideoId.getBatchNumber().toString();
        }
        if (userId == null){
            RRException.isNotEmpty(byVideoId, "视频信息不存在");
            userId = byVideoId.getUserId();
        }
        SocketCollectMessageListBo bo = new SocketCollectMessageListBo();
        bo.setLimit(-1);
        bo.setUserId(userId);
        bo.setVideoId(videoId);
        bo.setBatchNumber(batchNumber);
        PageUtils<SocketCollectMessageListVo> page = socketCollectMessageProducer.queryPage(bo);
        if (ObjectUtil.isNotEmpty(page.getList())){
            SocketCollectMessageListVo socketCollectMessageListVo = page.getList().get(0);
            SocketCollectMessageInfoVo result = BeanUtil.copyProperties(socketCollectMessageListVo, SocketCollectMessageInfoVo.class);
            return R.ok("获取成功", result);
        }
        return R.ok();
    }

    /**
     * 判断是否已经存在
     * @param batchNumber
     * @param userId
     * @param videoId
     * @return
     */
    public R<Boolean> socketDataExist(String batchNumber, Long userId, String videoId) {
        return R.ok(socketCollectMessageProducer.socketDataExist(batchNumber, userId, videoId));
    }

    /**
     * 获取在线人数列表
     * @param batchNumber 场次号
     * @param userId 用户id
     * @param videoId 视频唯一标识
     * @return
     */
    public R<List<OnlineNumInfoVo>> getOnlineNumList(String batchNumber, Long userId, String videoId) {
        List<OnlineNumInfoVo> onlineNumInfoVoList = socketCollectMessageProducer.onlineNumByUserIdAndBatchNumber(userId, batchNumber, videoId);

        return R.ok(onlineNumInfoVoList);
    }

    public R<String> uploadSocketData(UploadSocketDataBo bo) {
        socketCollectMessageProducer.uploadSocketData(bo);
        return R.ok("成功");
    }

    /**
     * 获取前两天的累计观看人数和场观
     * @param userId
     * @return
     */
    public R<SynchronizeTwoDayVideoVo> synchronizeTwoDayVideo(Long userId) {
        return R.ok(socketCollectMessageProducer.synchronizeTwoDayVideo(userId));
    }

    public R<List<TotalSocketMessageVo>> queryTimeByBatchNumber(List<String> batchNumberList){
        return R.ok(socketCollectMessageProducer.queryTimeByBatchNumber(batchNumberList));
    }

    /**
     * 获取websocket数据
     * @param videoId
     * @param cosKey
     * @return
     * @throws Exception
     */
    public R<SocketDataBo> getWebsocketData(String videoId, String cosKey) throws Exception {
        return R.ok(socketCollectMessageProducer.getWebsocketData(videoId, cosKey));
    }

    /**
     * 设置websocket数据
     * @param videoId
     * @param value
     * @return
     */
    public R<String> setWebsocketRedisData(String videoId, String value){
        socketCollectMessageProducer.setWebsocketRedisData(videoId, value);
        return R.ok();
    }

    /**
     * 获取socket的数据
     *
     * @param videoId 视频id
     * @return socket数据
     */
    public SocketCollectMessageInfoVo getByVideoId(String videoId) {
        return socketCollectMessageProducer.getByVideoId(videoId);
    }

    /**
     * 分析详情页的数据曲线
     * @param videoId
     * @return
     */
    public R<OnlineChartVo> onlineChartData(String videoId) {
        OnlineChartVo result = new OnlineChartVo();
        result.setMaxOnlineNum(0);
        result.setTotalViewersNum(0);
        result.setTotalBarrageNum(0);
        result.setApproachDataList(new ArrayList<>());
        result.setOnlineDataList(new ArrayList<>());

        SocketCollectMessageInfoVo video = socketCollectMessageProducer.getByVideoId(videoId);
        if (ObjectUtil.isNotEmpty(video) && ObjectUtil.isNotEmpty(video.getCosKey())){
            SocketDataBo websocketData = socketCollectMessageProducer.getWebsocketData(videoId, video.getCosKey());
            if (ObjectUtil.isEmpty(websocketData) || Double.parseDouble(websocketData.getVersion()) < 2){
                return  R.ok("暂无数据", null);
            }
            if (ObjectUtil.isNotEmpty(websocketData) && ObjectUtil.isNotEmpty(websocketData.getDatas())){
                Date startTime = websocketData.getServiceStartTime();
                Date endTime = websocketData.getServiceEndTime();

                Date oldTime = startTime;
                Date currentTime = startTime;

                List<SocketProcessDataBo> tempList = new ArrayList<>();
                for (int i = 0; i < websocketData.getDatas().size(); i++) {

                    SocketProcessDataBo item = websocketData.getDatas().get(i);

                    // 比较结束时间和当前时间-如果当前的时间大于结束时间，则跳出循环
                    if (endTime.compareTo(DateUtil.parse(item.getTime())) < 0) break;

                    Date dateTime = DateUtil.parse(item.getTime());


                    // 增加 60 秒
                    if (i != 0 && oldTime.compareTo(currentTime) == 0)
                    {
                        currentTime = DateUtil.offsetSecond(oldTime, 60);
                    }

                    // 计算时间差对应的value
                    if ((i != 0 && dateTime.compareTo(currentTime) >= 0) || (i == websocketData.getDatas().size() -1 && !tempList.isEmpty() && result.getOnlineDataList().isEmpty())){
                        long time = oldTime.getTime();
                        long time2 = DateUtil.between(oldTime, startTime, DateUnit.MS) + 60000;
                        oldTime = dateTime;

                        // 在线人数折线数据
                        List<Integer> onlineList = tempList.stream()
                                .filter(v -> ObjectUtil.isNotEmpty(v.getRenshu()))
                                .map(v -> NumberUtil.parseInt(v.getRenshu(), 0)).toList();
                        int value = 0;
                        if (ObjectUtil.isNotEmpty(onlineList)){
                            value = onlineList.stream().max(Integer::compareTo).orElse(0);
                        }else{
                            value = ObjectUtil.isNotEmpty(result.getOnlineDataList()) ? result.getOnlineDataList().get(result.getOnlineDataList().size() - 1).getValueNum() : 0;
                        }
                        CurveData onlineMap = new CurveData();
                        onlineMap.setDateTime(time);
                        onlineMap.setDateTimeNew(time2);
                        onlineMap.setValueNum(value);
                        result.getOnlineDataList().add(onlineMap);

                        tempList.add(item);

                        // 进场人数折线数据
                        List<Integer> list = tempList.stream()
                                .filter(v -> ObjectUtil.isNotEmpty(v.getLeijiguankanrenshu()))
                                .map(v -> NumberUtil.parseInt(v.getLeijiguankanrenshu(), 0)).toList();
                        // 如果流为空，则返回默认值0
                        Integer min = list.stream().min(Integer::compareTo).orElse(0);
                        Integer max = list.stream().max(Integer::compareTo).orElse(0);
                        CurveData approachData = new CurveData();
                        approachData.setDateTime(time);
                        approachData.setDateTimeNew(time2);
                        approachData.setValueNum(max - min);
                        result.getApproachDataList().add(approachData);

                        // 清空临时集合
                        tempList.clear();
                    }

                    tempList.add(item);
                }

                List<SocketProcessDataBo> datas = websocketData.getDatas();

                if (ObjectUtil.isEmpty(datas)){
                    return R.ok("暂无数据", null);
                }

                List<Integer> leijiList = datas.stream()
                        .filter(v -> ObjectUtil.isNotEmpty(v.getLeijiguankanrenshu()))
                        .map(v -> NumberUtil.parseInt(v.getLeijiguankanrenshu(), 0))
                        .toList();
                int minTotal = leijiList.stream().min(Integer::compareTo).orElse(0);
                int maxTotal = leijiList.stream().max(Integer::compareTo).orElse(0);

                //最大在线人数
                result.setMaxOnlineNum(result.getOnlineDataList()
                        .stream()
                        .filter(v -> ObjectUtil.isNotEmpty(v.getValueNum()))
                        .map(CurveData::getValueNum).max(Integer::compareTo).orElse(0));

                // 累计观看人数
                result.setTotalViewersNum(CommonUtils.conversionObservationNum(maxTotal - minTotal, result.getMaxOnlineNum()));
                // TODO 查询弹幕数量
//                result.setTotalBarrageNum(tableStoreFeign.totalBarrageNum(videoId, null));
            }

            if (result.getOnlineDataList().isEmpty() || result.getApproachDataList().isEmpty()){
                return R.ok("暂无数据", null);
            }

            if(result.getTotalViewersNum() == 0) result.setTotalViewersNum(null);
            if(result.getMaxOnlineNum() == 0) result.setMaxOnlineNum(null);
            if(result.getTotalBarrageNum() == 0) result.setTotalBarrageNum(null);

        }
        return R.ok(result);
    }

    /**
     * 根据视频id列表查询数据，可能会用重复的videoId数据
     *
     * @param videoIds videoId列表
     * @return List<DataScreenshotEntity>
     */
    public List<SocketCollectMessageInfoVo> listByVideoIds(List<String> videoIds) {
        return socketCollectMessageProducer.listByVideoIds(videoIds);
    }

    public Map<String, Boolean> getVideoHasBarragesMap(Collection<String> videoIds) {
        return socketCollectMessageProducer.getVideoHasBarragesMap(videoIds);
    }

    public Map<String, SocketCollectMessageVo> getVideoHasChartDataMap(Collection<String> videoIds) {
        return socketCollectMessageProducer.getVideoHasChartDataMap(videoIds);
    }
}

