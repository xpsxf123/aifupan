package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.SocketCollectMessageEntity;
import com.jiuyu.replay.words.entity.TotalSocketMessageEntity;
import com.jiuyu.replay.words.producer.SocketCollectMessageProducer;
import com.jiuyu.replay.words.repository.service.SocketCollectMessageService;
import com.jiuyu.replay.words.repository.service.TotalSocketMessageService;
import com.jiuyu.replay.words.vo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
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
@Service
@AllArgsConstructor
@Slf4j
public class SocketCollectMessageProducerImpl implements SocketCollectMessageProducer {

    private final SocketCollectMessageService socketCollectMessageService;
    private final TotalSocketMessageService totalSocketMessageService;
    private final WordsProperties wordsProperties;
    private final RedisTemplate redisTemplate;
    private final TableStoreFeign tableStoreFeign;


    @Override
    public PageUtils<SocketCollectMessageListVo> queryPage(SocketCollectMessageListBo socketCollectMessageListBo) {
        LambdaQueryWrapper<SocketCollectMessageEntity> wrapper = new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(ObjectUtil.isNotEmpty(socketCollectMessageListBo.getBatchNumber()), SocketCollectMessageEntity::getBatchNumber, socketCollectMessageListBo.getBatchNumber())
                .eq(ObjectUtil.isNotEmpty(socketCollectMessageListBo.getSecUid()), SocketCollectMessageEntity::getSecUid, socketCollectMessageListBo.getSecUid())
                .eq(ObjectUtil.isNotEmpty(socketCollectMessageListBo.getUserId()), SocketCollectMessageEntity::getUserId, socketCollectMessageListBo.getUserId())
                .eq(ObjectUtil.isNotEmpty(socketCollectMessageListBo.getId()), SocketCollectMessageEntity::getId, socketCollectMessageListBo.getId())
                .eq(ObjectUtil.isNotEmpty(socketCollectMessageListBo.getVideoId()), SocketCollectMessageEntity::getVideoId, socketCollectMessageListBo.getVideoId())
                ;

        IPage<SocketCollectMessageEntity> iPage = socketCollectMessageService.page(new Query<SocketCollectMessageEntity>().getPage(socketCollectMessageListBo.getPage(), socketCollectMessageListBo.getLimit()), wrapper);

        PageUtils<SocketCollectMessageListVo> pageUtils = new PageUtils<>(socketCollectMessageListBo.getPage(), socketCollectMessageListBo.getLimit(), iPage);

        List<SocketCollectMessageEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<SocketCollectMessageListVo> vos = records.stream().map(item -> {
                SocketCollectMessageListVo socketCollectMessageVo = new SocketCollectMessageListVo();
                BeanUtils.copyProperties(item, socketCollectMessageVo);
                return socketCollectMessageVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public SocketCollectMessageInfoVo info(Long id) {

        SocketCollectMessageEntity socketCollectMessageEntity = socketCollectMessageService.getById(id);
        if (socketCollectMessageEntity != null) {
            SocketCollectMessageInfoVo socketCollectMessageInfoVo = new SocketCollectMessageInfoVo();
            BeanUtils.copyProperties(socketCollectMessageEntity, socketCollectMessageInfoVo);
            return socketCollectMessageInfoVo;
        }

        return null;
    }

    /**
     * 新增websocket采集的信息
     *
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
    public SocketCollectMessageInfoVo save(SocketCollectMessageBo socketCollectMessageBo) {

        SocketCollectMessageEntity socketCollectMessageEntity = new SocketCollectMessageEntity();
        BeanUtils.copyProperties(socketCollectMessageBo, socketCollectMessageEntity);
        socketCollectMessageEntity.setId(SnowflakeManager.nextValue());
        socketCollectMessageEntity.setCreateDate(new Date());
        socketCollectMessageEntity.setUpdateDate(new Date());

        socketCollectMessageService.save(socketCollectMessageEntity);

        SocketCollectMessageInfoVo socketCollectMessageInfoVo = new SocketCollectMessageInfoVo();
        BeanUtils.copyProperties(socketCollectMessageEntity, socketCollectMessageInfoVo);

        return socketCollectMessageInfoVo;
    }

    /**
     * 修改websocket采集的信息
     *
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
    public SocketCollectMessageInfoVo update(SocketCollectMessageBo socketCollectMessageBo) {

        SocketCollectMessageEntity socketCollectMessageEntity = new SocketCollectMessageEntity();
        BeanUtils.copyProperties(socketCollectMessageBo, socketCollectMessageEntity);
        socketCollectMessageEntity.setUpdateDate(new Date());

        socketCollectMessageService.updateById(socketCollectMessageEntity);
        return info(socketCollectMessageBo.getId());
    }

    /**
     * 删除websocket采集的信息
     *
     * @param id websocket采集的信息id
     * @return
     */
    public void deleteById(Long id) {

        socketCollectMessageService.removeById(id);
    }

    @Override
    public SocketCollectMessageInfoVo getByBatch(String batchNumber, Long userId, String videoId) {
        RRException.isNotEmpty(batchNumber, "批次号不能为空");
        RRException.isNotEmpty(userId, "用户id不能为空");
        RRException.isNotEmpty(videoId, "视频id不能为空");
        SocketCollectMessageEntity one = socketCollectMessageService.getOne(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getBatchNumber, batchNumber)
                .eq(SocketCollectMessageEntity::getUserId, userId)
                .eq(SocketCollectMessageEntity::getVideoId, videoId)
        );
        if (one != null) {
            return BeanUtil.copyProperties(one, SocketCollectMessageInfoVo.class);
        }
        return null;
    }

    @Override
    public SocketCollectMessageInfoVo getByBatch(String batchNumber, String videoId) {
        RRException.isNotEmpty(batchNumber, "批次号不能为空");
        RRException.isNotEmpty(videoId, "视频id不能为空");
        SocketCollectMessageEntity one = socketCollectMessageService.getOne(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getBatchNumber, batchNumber)
                .eq(SocketCollectMessageEntity::getVideoId, videoId)
        );
        if (one != null) {
            return BeanUtil.copyProperties(one, SocketCollectMessageInfoVo.class);
        }
        return null;
    }

    @Override
    public SocketCollectMessageInfoVo getByVideoId(String videoId) {
        RRException.isNotEmpty(videoId, "视频id不能为空");
        SocketCollectMessageEntity one = socketCollectMessageService.getOne(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getVideoId, videoId)
                .last("limit 1")
        );
        if (one != null) {
            return BeanUtil.copyProperties(one, SocketCollectMessageInfoVo.class);
        }
        return null;
    }

    @Override
    public List<OnlineNumInfoVo> onlineNumByUserIdAndBatchNumber(Long userId, String batchNumber, String videoId) {
        try {
            SocketCollectMessageInfoVo byBatch = this.getByBatch(batchNumber, userId, videoId);
            if (byBatch != null){
                if (ObjectUtil.isNotEmpty(byBatch.getCosKey())){
                    SocketDataBo socketDataBo = this.getWebsocketData(videoId, byBatch.getCosKey());
                    if (ObjectUtil.isNotEmpty(socketDataBo) && ObjectUtil.isNotEmpty(socketDataBo.getDatas())){
                        return socketDataBo.getDatas().stream().filter(data -> ObjectUtil.isNotEmpty(data.getRenshu())).map(data -> {
                            OnlineNumInfoVo onlineNumInfoVo = new OnlineNumInfoVo();
                            onlineNumInfoVo.setRecordDate(data.getTime());
                            onlineNumInfoVo.setPeopleNum(data.getRenshu());
                            return onlineNumInfoVo;
                        }).collect(Collectors.toList());
                    }
                }
            }
        }catch (Exception e) {

        }

        return new ArrayList<>();
    }

    @Override
    public Boolean socketDataExist(String batchNumber, Long userId, String videoId) {
        RRException.isNotEmpty(batchNumber, "批次号不能为空");
        RRException.isNotEmpty(userId, "用户id不能为空");
        RRException.isNotEmpty(videoId, "视频id不能为空");
        SocketCollectMessageEntity one = socketCollectMessageService.getOne(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getBatchNumber, batchNumber)
                .eq(SocketCollectMessageEntity::getUserId, userId)
                .eq(SocketCollectMessageEntity::getVideoId, videoId)
                .select(SocketCollectMessageEntity::getId)
        );
        return ObjectUtil.isNotEmpty(one) && (ObjectUtil.isNotEmpty(one.getCosKey()) || ObjectUtil.isNotEmpty(one.getFileAddress()));
    }

    @Override
    public void uploadSocketData(UploadSocketDataBo bo) {
        Date now = new Date();
        RRException.isNotEmpty(bo.getUserId(), "用户id不能为空");
        RRException.isNotEmpty(bo.getVideoId(), "视频id不能为空");
        RRException.isNotEmpty(bo.getSecUid(), "secUid不能为空");
        RRException.isNotEmpty(bo.getBatchNumber(), "批次号不能为空");
        RRException.isNotEmpty(bo.getVideoId(), "视频id不能为空");
        RRException.isNotEmpty(bo.getCosKey(), "cosKey不能为空");
        RRException.isNotEmpty(bo.getStartDate(), "开始时间不能为空");
        RRException.isNotEmpty(bo.getEndDate(), "结束时间不能为空");

        // 存场观的人数，以video的维度
        SocketCollectMessageEntity one = socketCollectMessageService.getOne(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getUserId, bo.getUserId())
                .eq(SocketCollectMessageEntity::getVideoId, bo.getVideoId())
                .last("limit 1")
        );
        if (ObjectUtil.isNotEmpty(one)){
            one.setUpdateDate(now);
            one.setCosKey(bo.getCosKey());
            one.setObservationNum(bo.getObservationNum());
            one.setTotalBarrageNum(bo.getTotalBarrageNum());
            one.setOnlineMaxNum(bo.getOnlineMaxNum());
            socketCollectMessageService.updateById(one);
        }else{
            one = new SocketCollectMessageEntity();
            one.setId(SnowflakeManager.nextValue());
            one.setUserId(bo.getUserId());
            one.setTenantId(bo.getTenantId());
            one.setSecUid(bo.getSecUid());
            one.setBatchNumber(bo.getBatchNumber());
            one.setVideoId(bo.getVideoId());
            one.setCosKey(bo.getCosKey());
            one.setStartDate(bo.getStartDate());
            one.setEndDate(bo.getEndDate());
            one.setTotalOnlineNum(bo.getTotalOnlineNum());
            one.setObservationNum(bo.getObservationNum());
            one.setTotalBarrageNum(bo.getTotalBarrageNum());
            one.setCreateDate(now);
            one.setUpdateDate(now);
            one.setOnlineMaxNum(bo.getOnlineMaxNum());
            socketCollectMessageService.save(one);
        }

        // 存累计观看人数-以batchNumber维度
        TotalSocketMessageEntity socketMessage = totalSocketMessageService.getOne(new LambdaQueryWrapper<TotalSocketMessageEntity>()
                .eq(TotalSocketMessageEntity::getUserId, bo.getUserId())
                .eq(TotalSocketMessageEntity::getBatchNumber, bo.getBatchNumber())
                .last("limit 1")
        );
        if (ObjectUtil.isNotEmpty(socketMessage)){
            socketMessage.setEndDate(bo.getEndDate());
            if (ObjectUtil.isNotEmpty(bo.getTotalOnlineNum()) && Integer.parseInt(bo.getTotalOnlineNum()) > Integer.parseInt(socketMessage.getTotalOnlineNum())){
                socketMessage.setTotalOnlineNum(bo.getTotalOnlineNum());
            }
            if (ObjectUtil.isNotEmpty(bo.getTotalBulletChatNum()) && Integer.parseInt(bo.getTotalBulletChatNum()) > Integer.parseInt(socketMessage.getTotalBulletChatNum())){
                socketMessage.setTotalBulletChatNum(bo.getTotalBulletChatNum());
            }
            socketMessage.setUpdateDate(now);
            totalSocketMessageService.updateById(socketMessage);
        }else{
            socketMessage = new TotalSocketMessageEntity();
            socketMessage.setId(SnowflakeManager.nextValue());
            socketMessage.setSecUid(bo.getSecUid());
            socketMessage.setUserId(bo.getUserId());
            socketMessage.setTenantId(bo.getTenantId());
            socketMessage.setBatchNumber(bo.getBatchNumber());
            socketMessage.setStartDate(bo.getStartDate());
            socketMessage.setEndDate(bo.getEndDate());
            socketMessage.setTotalOnlineNum(bo.getTotalOnlineNum());
            socketMessage.setTotalBulletChatNum(bo.getTotalBulletChatNum());
            socketMessage.setCreateDate(now);
            socketMessage.setUpdateDate(now);
            totalSocketMessageService.save(socketMessage);
        }
    }

    @Override
    public SynchronizeTwoDayVideoVo synchronizeTwoDayVideo(Long userId) {
        String now = DateUtil.now();
        String twoDat = DateUtil.offsetDay(new Date(), -2).toString("yyyy-MM-dd 00:00:00");
        List<SocketCollectMessageEntity> socketList = socketCollectMessageService.list(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getUserId, userId)
                .ge(SocketCollectMessageEntity::getStartDate, twoDat)
                .le(SocketCollectMessageEntity::getStartDate, now)
        );
        List<TotalSocketMessageEntity> totalSocketList = totalSocketMessageService.list(new LambdaQueryWrapper<TotalSocketMessageEntity>()
                .eq(TotalSocketMessageEntity::getUserId, userId)
                .ge(TotalSocketMessageEntity::getStartDate, twoDat)
                .le(TotalSocketMessageEntity::getStartDate, now)
        );
        SynchronizeTwoDayVideoVo result = new SynchronizeTwoDayVideoVo();
        if (ObjectUtil.isNotEmpty(socketList)){
            result.setSocketCollectMessageList(BeanUtil.copyToList(socketList, SocketCollectMessageVo.class));
        }
        if (ObjectUtil.isNotEmpty(totalSocketList)){
            result.setTotalSocketMessageList(BeanUtil.copyToList(totalSocketList, TotalSocketMessageVo.class));
        }
        return result;
    }

    @Override
    public List<TotalSocketMessageVo> queryTimeByBatchNumber(List<String> batchNumberList) {
        List<TotalSocketMessageEntity> list = totalSocketMessageService.list(new LambdaQueryWrapper<TotalSocketMessageEntity>()
                .in(TotalSocketMessageEntity::getBatchNumber, batchNumberList)
                .select(TotalSocketMessageEntity::getBatchNumber, TotalSocketMessageEntity::getStartDate)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, TotalSocketMessageVo.class);
        }
        return List.of();
    }

    @Override
    public SocketDataBo getWebsocketData(String videoId, String cosKey) {
        Object strO = redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.websocketDataCacheKey, videoId));
        if (ObjectUtil.isNotEmpty(strO)){
            return JSONObject.parseObject(strO.toString(), SocketDataBo.class);
        }
        if (ObjectUtil.isNotEmpty(cosKey)){
            if (!FileUtil.exist(wordsProperties.getServerWebsocketPath() + cosKey)){
                if (!TencentCosUtils.downloadCosFile(TencentCosUtils.getPrivateCosBucketName(), wordsProperties.getServerWebsocketPath() + cosKey, cosKey)) {
                    log.error("下载cos文件失败");
                }
            }

            if (FileUtil.exist(wordsProperties.getServerWebsocketPath() + cosKey)){
                String path = wordsProperties.getServerWebsocketPath() + cosKey;
                if (FileUtil.exist(path)){
                    String txt = ReplayFileUtils.readFirstTxtFromZip(path);

                    // 保存到redis中-2天的时间
                    redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.websocketDataCacheKey, videoId), txt, 2, TimeUnit.DAYS);

                    // 删除文件
                    FileUtil.del(path);

                    SocketDataBo socketDataBo = JSONObject.parseObject(txt, SocketDataBo.class);
                    return socketDataBo;
                }
            }
        }
        return null;
    }

    @Override
    public void setWebsocketRedisData(String videoId, String value) {
        // 保存到redis中-2天的时间
        redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.websocketDataCacheKey, videoId), value, 2, TimeUnit.DAYS);
    }

    @Override
    public List<SocketCollectMessageInfoVo> listByVideoIds(List<String> videoIds) {

        List<SocketCollectMessageEntity> list = socketCollectMessageService.list(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .in(SocketCollectMessageEntity::getVideoId, videoIds)
        );
        return BeanConvertUtils.convertList(list, SocketCollectMessageInfoVo.class);
    }

    @Override
    public void updateTotalBarrageNumByVideoId(String batchNumber, Long userId, String videoId, Integer totalBarrageNum) {
        socketCollectMessageService.update(new LambdaUpdateWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getBatchNumber, batchNumber)
                .eq(SocketCollectMessageEntity::getUserId, userId)
                .eq(SocketCollectMessageEntity::getVideoId, videoId)
                .set(SocketCollectMessageEntity::getTotalBarrageNum, totalBarrageNum)
        );
    }

    @Override
    public void copyVideoDataToSlice(String sourceVideoId, String sliceVideoId, Long sliceStartTime, Long sliceEndTime, Long tenantId) {

        // 1.原视频socket数据记录
        SocketCollectMessageEntity sourceEntity = socketCollectMessageService.getOne(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                .eq(SocketCollectMessageEntity::getVideoId, sourceVideoId)
                .last("limit 1")
        );

        if (sourceEntity == null || ObjectUtil.isEmpty(sourceEntity.getCosKey())) {
            return;
        }

        try {
            // 2.从COS下载原视频socket数据
            SocketDataBo socketDataBo = this.getWebsocketData(sourceVideoId, sourceEntity.getCosKey());
            if (socketDataBo == null || ObjectUtil.isEmpty(socketDataBo.getDatas())) {
                return;
            }

            // 3.按切片开始时间和结束时间切原始socket过程数据
            Date startDate = new Date(sliceStartTime);
            Date endDate = new Date(sliceEndTime);
            List<SocketProcessDataBo> sliceDatas = socketDataBo.getDatas().stream().filter(data -> {
                if (ObjectUtil.isEmpty(data.getTime()) || ObjectUtil.isEmpty(data.getRenshu()) || ObjectUtil.isEmpty(data.getLeijiguankanrenshu())) {
                    return false;
                }
                Date dataTime = DateUtil.parse(data.getTime());
                return dataTime != null && !dataTime.before(startDate) && !dataTime.after(endDate);
            }).collect(Collectors.toList());

            // 整理切片的socket数据对象
            SocketDataBo sliceSocketDataBo = new SocketDataBo();
            sliceSocketDataBo.setUserId(socketDataBo.getUserId());
            sliceSocketDataBo.setSecUid(socketDataBo.getSecUid());
            sliceSocketDataBo.setBatchNumber(socketDataBo.getBatchNumber());
            sliceSocketDataBo.setVersion(socketDataBo.getVersion());
            sliceSocketDataBo.setVideoStartTime(startDate);
            sliceSocketDataBo.setVideoEndTime(endDate);
            sliceSocketDataBo.setServiceStartTime(startDate);
            sliceSocketDataBo.setServiceEndTime(endDate);
            sliceSocketDataBo.setDatas(sliceDatas);

            // 4.将整理好的socket数据上传到COS
            String jsonStr = JSONObject.toJSONString(sliceSocketDataBo);
            ByteArrayOutputStream zipStream = ReplayFileUtils.createZipStream(jsonStr, sliceVideoId + ".txt");
            String cosKey = "socketMessageFile/" + new SimpleDateFormat("yyyy/MM/dd/").format(new Date()) + UUID.randomUUID() + ".zip";
            Boolean uploadSuccess = TencentCosUtils.putStreamObject(TencentCosUtils.getPrivateCosBucketName(),
                    new ByteArrayInputStream(zipStream.toByteArray()), cosKey);

            if (Boolean.TRUE.equals(uploadSuccess)) {

                // 5.从切片socket过程数据计算人数
                String totalOnlineNum = sourceEntity.getTotalOnlineNum();
                String observationNum = "0";
                Integer onlineMaxNum = sourceEntity.getOnlineMaxNum();

                try {
                    if (!sliceDatas.isEmpty()) {
                        // 累计场观人数：最后一条数据的累计场观人数
                        SocketProcessDataBo lastData = sliceDatas.get(sliceDatas.size() - 1);
                        totalOnlineNum = lastData.getLeijiguankanrenshu();
                        // 场观人数：最后一条数据的累计场观人数 - 第一条数据的累计场观人数
                        SocketProcessDataBo firstData = sliceDatas.get(0);
                        if (ObjectUtil.isNotEmpty(lastData.getLeijiguankanrenshu()) && ObjectUtil.isNotEmpty(firstData.getLeijiguankanrenshu())) {
                            int diff = Integer.parseInt(lastData.getLeijiguankanrenshu()) - Integer.parseInt(firstData.getLeijiguankanrenshu());
                            observationNum = String.valueOf(Math.max(diff, 0));
                        }
                        // 最高在线人数：取过程数据中在线人数最高的一个
                        onlineMaxNum = sliceDatas.stream()
                                .filter(d -> ObjectUtil.isNotEmpty(d.getRenshu()))
                                .mapToInt(d -> Integer.parseInt(d.getRenshu()))
                                .max().orElse(0);
                    }
                }catch (Exception e) {
                    log.info("从切片socket过程数据计算人数失败, sourceVideoId={}, sliceVideoId={}", sourceVideoId, sliceVideoId);
                }

                // 6.为切片视频添加一条socket记录
                SocketCollectMessageEntity sliceEntity = new SocketCollectMessageEntity();
                BeanUtils.copyProperties(sourceEntity, sliceEntity);
                sliceEntity.setId(SnowflakeManager.nextValue());
                sliceEntity.setVideoId(sliceVideoId);
                sliceEntity.setCosKey(cosKey);
                sliceEntity.setStartDate(startDate);
                sliceEntity.setEndDate(endDate);
                sliceEntity.setTotalOnlineNum(totalOnlineNum);
                sliceEntity.setObservationNum(observationNum);
                sliceEntity.setOnlineMaxNum(onlineMaxNum);
                sliceEntity.setCreateDate(new Date());
                sliceEntity.setUpdateDate(new Date());

                // 7.查询弹幕数量
                QueryDanMuBo queryDanMuBo = new QueryDanMuBo();
                queryDanMuBo.setUserId(sourceEntity.getUserId());
                queryDanMuBo.setTenantId(tenantId);
                queryDanMuBo.setBatchNumber(sourceEntity.getBatchNumber());
                queryDanMuBo.setVideoId(sourceEntity.getVideoId());
                queryDanMuBo.setStartTime(sliceEntity.getStartDate().getTime());
                queryDanMuBo.setEndTime(sliceEntity.getEndDate().getTime());
                int count = Math.toIntExact(tableStoreFeign.queryDanMuSearchCount(queryDanMuBo));
                sliceEntity.setTotalBarrageNum(count);

                socketCollectMessageService.save(sliceEntity);
            } else {
                log.info("切片视频socket数据上传COS失败, sourceVideoId={}, sliceVideoId={}", sourceVideoId, sliceVideoId);
            }
        } catch (Exception e) {
            log.info("拷贝视频socket数据到切片失败, sourceVideoId={}, sliceVideoId={}", sourceVideoId, sliceVideoId, e);
        }
    }


    /**
     * 获取视频是否有弹幕
     *
     * @param videoIds
     *
     * @return
     */
    @Override
    public Map<String, Boolean> getVideoHasBarragesMap(Collection<String> videoIds) {
        if (EmptyUtil.isEmpty(videoIds)) {
            return Map.of();
        }
        return socketCollectMessageService.lambdaQuery()
            .in(SocketCollectMessageEntity::getVideoId, videoIds)
            .eq(SocketCollectMessageEntity::getIsDeleted, 0)
            .select(SocketCollectMessageEntity::getVideoId, SocketCollectMessageEntity::getTotalBarrageNum)
            .list().stream()
            .collect(Collectors.toMap(
                SocketCollectMessageEntity::getVideoId,
                entity -> entity.getTotalBarrageNum() != null && entity.getTotalBarrageNum() > 0
            , (entity1, entity2) -> entity1));
    }

    @Override
    public Map<String, SocketCollectMessageVo> getVideoHasChartDataMap(Collection<String> videoIds) {
        if (EmptyUtil.isEmpty(videoIds)) {
            return Map.of();
        }
        return socketCollectMessageService.lambdaQuery()
            .in(SocketCollectMessageEntity::getVideoId, videoIds)
            .eq(SocketCollectMessageEntity::getIsDeleted, 0)
            .select(SocketCollectMessageEntity::getVideoId,
                SocketCollectMessageEntity::getTotalOnlineNum,
                SocketCollectMessageEntity::getObservationNum,
                SocketCollectMessageEntity::getTotalBarrageNum,
                SocketCollectMessageEntity::getOnlineMaxNum)
            .list().stream()
            .collect(Collectors.toMap(
                SocketCollectMessageEntity::getVideoId,
                entity -> {
                    SocketCollectMessageVo vo = new SocketCollectMessageVo();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                }
            , (entity1, entity2) -> entity1));
    }
}

