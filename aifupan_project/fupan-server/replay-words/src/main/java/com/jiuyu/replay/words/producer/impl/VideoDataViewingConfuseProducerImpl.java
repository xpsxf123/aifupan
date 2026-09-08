package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.JYStreamUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.words.video.SaveSliceCorrelationDataBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.aiagent.VideoWatchHasVO;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineDataBo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.bo.viewing.UpdateConfuseDataByVideoIdBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseListBo;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.entity.VideoDataViewingConfuseEntity;
import com.jiuyu.replay.words.entity.VideoDataViewingParagraphEntity;
import com.jiuyu.replay.words.enums.DataViewingSourceTypeEnum;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.producer.VideoDataViewingConfuseProducer;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import com.jiuyu.replay.words.repository.service.VideoDataViewingConfuseService;
import com.jiuyu.replay.words.repository.service.VideoDataViewingParagraphService;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseListVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 视频看盘混淆后的数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Service
@Slf4j
public class VideoDataViewingConfuseProducerImpl implements VideoDataViewingConfuseProducer {

    @Resource
    private VideoDataViewingConfuseService videoDataViewingConfuseService;
    @Resource
    private AnchorVideoService anchorVideoService;
    @Resource
    private OssUtils ossUtils;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private VideoDataViewingParagraphService videoDataViewingParagraphService;


    @Override
    public PageUtils<VideoDataViewingConfuseListVo> queryPage(VideoDataViewingConfuseListBo videoDataViewingConfuseListBo) {
        QueryWrapper<VideoDataViewingConfuseEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(videoDataViewingConfuseListBo.getKeyword())){
            wrapper.like("name", videoDataViewingConfuseListBo.getKeyword());
        }

        IPage<VideoDataViewingConfuseEntity> iPage = videoDataViewingConfuseService.page(new Query<VideoDataViewingConfuseEntity>().getPage(videoDataViewingConfuseListBo.getPage(), videoDataViewingConfuseListBo.getLimit()), wrapper);

        PageUtils<VideoDataViewingConfuseListVo> pageUtils = new PageUtils<>(videoDataViewingConfuseListBo.getPage(), videoDataViewingConfuseListBo.getLimit(), iPage);

        List<VideoDataViewingConfuseEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<VideoDataViewingConfuseListVo> vos = records.stream().map(item -> {
                VideoDataViewingConfuseListVo videoDataViewingConfuseVo = new VideoDataViewingConfuseListVo();
                BeanUtils.copyProperties(item, videoDataViewingConfuseVo);
                return videoDataViewingConfuseVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public VideoDataViewingConfuseInfoVo info(Long id) {

        VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = videoDataViewingConfuseService.getById(id);
        if(videoDataViewingConfuseEntity != null) {
            VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = new VideoDataViewingConfuseInfoVo();
            BeanUtils.copyProperties(videoDataViewingConfuseEntity, videoDataViewingConfuseInfoVo);
            return videoDataViewingConfuseInfoVo;
        }

        return null;
    }

    @Override
    public VideoDataViewingConfuseInfoVo save(VideoDataViewingConfuseBo videoDataViewingConfuseBo) {

         VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = new VideoDataViewingConfuseEntity();
         BeanUtils.copyProperties(videoDataViewingConfuseBo, videoDataViewingConfuseEntity);
         videoDataViewingConfuseEntity.setId(SnowflakeManager.nextValue());
         videoDataViewingConfuseEntity.setCreateDate(new Date());
         videoDataViewingConfuseEntity.setUpdateDate(new Date());

         videoDataViewingConfuseService.save(videoDataViewingConfuseEntity);

         VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = new VideoDataViewingConfuseInfoVo();
         BeanUtils.copyProperties(videoDataViewingConfuseEntity, videoDataViewingConfuseInfoVo);

         return videoDataViewingConfuseInfoVo;
     }

    @Override
    public void update(VideoDataViewingConfuseBo videoDataViewingConfuseBo) {

        VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = new VideoDataViewingConfuseEntity();
        BeanUtils.copyProperties(videoDataViewingConfuseBo, videoDataViewingConfuseEntity);
        videoDataViewingConfuseEntity.setUpdateDate(new Date());

        videoDataViewingConfuseService.updateById(videoDataViewingConfuseEntity);
    }

    @Override
    public void deleteById(Long id) {

        videoDataViewingConfuseService.removeById(id);
    }

    @Override
    public VideoDataViewingConfuseInfoVo infoByVideoIdAndUser(String videoId, Long userId, Long tenantId) {

        QueryWrapper<VideoDataViewingConfuseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        if(!StringUtils.isEmpty(userId)) {
            wrapper.eq("user_id", userId);
        }
        if(!StringUtils.isEmpty(tenantId)) {
            wrapper.eq("tenant_id", tenantId);
        }
        VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = this.videoDataViewingConfuseService.getOne(wrapper);

        if(videoDataViewingConfuseEntity != null) {
            VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = new VideoDataViewingConfuseInfoVo();
            BeanUtils.copyProperties(videoDataViewingConfuseEntity, videoDataViewingConfuseInfoVo);
            return videoDataViewingConfuseInfoVo;
        }

        return null;
    }

    @Override
    public VideoDataViewingConfuseInfoVo getByVideoId(String videoId) {

        VideoDataViewingConfuseEntity entity = this.videoDataViewingConfuseService.getOne(new LambdaUpdateWrapper<VideoDataViewingConfuseEntity>()
                .eq(VideoDataViewingConfuseEntity::getVideoId, videoId)
                .last("limit 1")
        );
        if (entity != null){
            return BeanUtil.copyProperties(entity, VideoDataViewingConfuseInfoVo.class);
        }

        return null;
    }

    @Override
    public Map<String, Boolean> hasVideo(List<String> videoIds) {
        // 有视频id，查询视频id对应的看板数据是否存在，只要有一条就是存在
        // 返回一个map，key是视频id，value是是否存在截图数据
        if (CollUtil.isEmpty(videoIds)) {
            return new HashMap<>();
        }

        // 查询所有存在截图的视频ID
        List<String> existVideoIds = videoDataViewingConfuseService.list(new LambdaQueryWrapper<VideoDataViewingConfuseEntity>()
                        .select(VideoDataViewingConfuseEntity::getVideoId)
                        .in(VideoDataViewingConfuseEntity::getVideoId, videoIds)
                        .groupBy(VideoDataViewingConfuseEntity::getVideoId))
                .stream()
                .map(VideoDataViewingConfuseEntity::getVideoId)
                .toList();

        // 构建返回结果
        Map<String, Boolean> result = new HashMap<>();
        for (String videoId : videoIds) {
            result.put(videoId, existVideoIds.contains(videoId));
        }

        return result;
    }

    @Override
    public void callbackCodeErrorHandle(String requestId, Integer code) {

        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getRequestId, requestId);
        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseList = this.videoDataViewingConfuseService.list(wrapper);

        if(videoDataViewingConfuseList != null && !videoDataViewingConfuseList.isEmpty()) {
            for (VideoDataViewingConfuseEntity videoDataViewingConfuseEntity : videoDataViewingConfuseList) {
                if(videoDataViewingConfuseEntity.getDataStatus().equals(DataViewingStatusEnum.PULLING.getStatus())) {
                    if(code == 3003) {
                        // 第三方数据平台没有收录当前主播
                        videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.NOT_INCLUDE_ANCHOR.getStatus());
                    }else if(code == 3004) {
                        videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.LIVE_IS_NULL.getStatus());
                    }else {
                        videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.PULL_FAIL.getStatus());
                    }
                    videoDataViewingConfuseEntity.setUpdateDate(new Date());
                }
            }
            this.videoDataViewingConfuseService.updateBatchById(videoDataViewingConfuseList);
        }

    }

    @Override
    public List<VideoDataViewingConfuseInfoVo> listByVideoIds(Collection<String> videoIds) {

        QueryWrapper<VideoDataViewingConfuseEntity> wrapper = new QueryWrapper<>();
        wrapper.in("video_id", videoIds);
        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);

        if(videoDataViewingConfuseEntities != null && videoDataViewingConfuseEntities.size() > 0) {
            List<VideoDataViewingConfuseInfoVo> viewingConfuseInfoVos = videoDataViewingConfuseEntities.stream().map(item -> {
                VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = new VideoDataViewingConfuseInfoVo();
                BeanUtils.copyProperties(item, videoDataViewingConfuseInfoVo);
                return videoDataViewingConfuseInfoVo;
            }).collect(Collectors.toList());

            return viewingConfuseInfoVos;
        }

        return null;
    }

    @Override
    public void createDataViewingConfuse(String videoId, Long userId, Long tenantId, String anchorNumber, String requestId, Integer dataStatus, String batchNumber) {

        VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = new VideoDataViewingConfuseEntity();
        videoDataViewingConfuseEntity.setId(SnowflakeManager.nextValue());
        videoDataViewingConfuseEntity.setVideoId(videoId);
        videoDataViewingConfuseEntity.setUserId(userId);
        videoDataViewingConfuseEntity.setTenantId(tenantId);
        videoDataViewingConfuseEntity.setCrawlTime(new Date());
        videoDataViewingConfuseEntity.setCreateDate(new Date());
        videoDataViewingConfuseEntity.setUpdateDate(new Date());
        videoDataViewingConfuseEntity.setVideoDataViewingId(0L);
        videoDataViewingConfuseEntity.setAnchorNumber(anchorNumber);
        videoDataViewingConfuseEntity.setDataStatus(dataStatus);
        if(!StringUtils.isEmpty(requestId)) {
            videoDataViewingConfuseEntity.setRequestId(requestId);
        }
        videoDataViewingConfuseEntity.setBatchNumber(batchNumber);

        this.videoDataViewingConfuseService.save(videoDataViewingConfuseEntity);

    }

    @Override
    public List<VideoDataViewingConfuseInfoVo> listByVideoIdsAndStatus(List<String> videoIds, Integer dataStatus) {
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VideoDataViewingConfuseEntity::getVideoId, videoIds);
        if(DataViewingStatusEnum.PULL_SUCCESS.getStatus().equals(dataStatus)) {
            wrapper.in(VideoDataViewingConfuseEntity::getDataStatus,
                    DataViewingStatusEnum.PULL_SUCCESS.getStatus(),
                    DataViewingStatusEnum.DATA_ORGANIZE.getStatus());
        }else {
            wrapper.eq(VideoDataViewingConfuseEntity::getDataStatus, dataStatus);
        }

        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);

        return BeanConvertUtils.convertList(videoDataViewingConfuseEntities, VideoDataViewingConfuseInfoVo.class);
    }


    @Override
    public List<VideoDataViewingConfuseInfoVo> listByObj(VideoDataViewingConfuseBo videoDataViewingConfuseBo) {
        List<VideoDataViewingConfuseEntity> list = videoDataViewingConfuseService.lambdaQuery()
                .eq(VideoDataViewingConfuseEntity::getDataStatus, videoDataViewingConfuseBo.getDataStatus())
                .ge(ObjectUtil.isNotNull(videoDataViewingConfuseBo.getTotalWatchNum())
                        , VideoDataViewingConfuseEntity::getTotalWatchNum, videoDataViewingConfuseBo.getTotalWatchNum())
                .ge(ObjectUtil.isNotNull(videoDataViewingConfuseBo.getVolumeStart())
                        , VideoDataViewingConfuseEntity::getVolumeStart, videoDataViewingConfuseBo.getVolumeStart())
                .select(VideoDataViewingConfuseEntity::getId,VideoDataViewingConfuseEntity::getVideoId, VideoDataViewingConfuseEntity::getTotalWatchNum
                        , VideoDataViewingConfuseEntity::getVolumeStart, VideoDataViewingConfuseEntity::getVolumeEnd)
                .list();
        return BeanUtil.copyToList(list, VideoDataViewingConfuseInfoVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoDataViewingConfuseInfoVo saveOceanEngine(OceanEngineDataBo oceanEngineDataBo, Long oldDataViewingId) {

        // 封装数据
        VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = new VideoDataViewingConfuseEntity();
        if(oldDataViewingId != null) {
            // 修改
            videoDataViewingConfuseEntity.setId(oldDataViewingId);
        }else {
            // 新增
            videoDataViewingConfuseEntity.setId(SnowflakeManager.nextValue());
            videoDataViewingConfuseEntity.setCreateDate(new Date());
        }

        videoDataViewingConfuseEntity.setVideoId(oceanEngineDataBo.getVideoId());
        videoDataViewingConfuseEntity.setUserId(oceanEngineDataBo.getUserId());
        videoDataViewingConfuseEntity.setTenantId(oceanEngineDataBo.getTenantId());
        videoDataViewingConfuseEntity.setTotalWatchNum(oceanEngineDataBo.getTotalWatchNum());
        videoDataViewingConfuseEntity.setAverageOnlineNum(oceanEngineDataBo.getAverageOnlineNum());
        videoDataViewingConfuseEntity.setAverageResidenceTime(oceanEngineDataBo.getAverageResidenceTime());
        videoDataViewingConfuseEntity.setIncrementFollowerCount(oceanEngineDataBo.getIncrementFollowerCount());
        videoDataViewingConfuseEntity.setConvertFanRate(oceanEngineDataBo.getConvertFanRate());
        if(oceanEngineDataBo.getInteractionPercent() != null) {
            videoDataViewingConfuseEntity.setInteractionPercent(oceanEngineDataBo.getInteractionPercent() * 100);
        }
        if(oceanEngineDataBo.getVolume() != null) {
            videoDataViewingConfuseEntity.setVolumeStart(oceanEngineDataBo.getVolume() / 100);
            videoDataViewingConfuseEntity.setVolumeEnd(oceanEngineDataBo.getVolume() / 100);
        }
        videoDataViewingConfuseEntity.setPurchaseCountStart(oceanEngineDataBo.getPurchaseCount());
        videoDataViewingConfuseEntity.setPurchaseCountEnd(oceanEngineDataBo.getPurchaseCount());
        if(oceanEngineDataBo.getCustomerUnitPrice() != null) {
            videoDataViewingConfuseEntity.setCustomerUnitPriceStart(oceanEngineDataBo.getCustomerUnitPrice() / 100);
            videoDataViewingConfuseEntity.setCustomerUnitPriceEnd(oceanEngineDataBo.getCustomerUnitPrice() / 100);
        }
        videoDataViewingConfuseEntity.setUvValueStart(oceanEngineDataBo.getUvValue());
        videoDataViewingConfuseEntity.setUvValueEnd(oceanEngineDataBo.getUvValue());
        if(oceanEngineDataBo.getGoodsConvertRate() != null) {
            videoDataViewingConfuseEntity.setGoodsConvertRateStart(oceanEngineDataBo.getGoodsConvertRate());
            videoDataViewingConfuseEntity.setGoodsConvertRateEnd(oceanEngineDataBo.getGoodsConvertRate());
        }
        if(oceanEngineDataBo.getGpm() != null) {
            videoDataViewingConfuseEntity.setGpmStart(oceanEngineDataBo.getGpm() / 100);
            videoDataViewingConfuseEntity.setGpmEnd(oceanEngineDataBo.getGpm() / 100);
        }

        videoDataViewingConfuseEntity.setDataStatus(1);
        videoDataViewingConfuseEntity.setDataSourceType(DataViewingSourceTypeEnum.JULIANGBAIYING.getType());
        videoDataViewingConfuseEntity.setCrawlTime(new Date());
        videoDataViewingConfuseEntity.setUpdateDate(new Date());
        videoDataViewingConfuseEntity.setVideoDataViewingId(0L);
        videoDataViewingConfuseEntity.setAnchorNumber(oceanEngineDataBo.getAnchorNumber());
        videoDataViewingConfuseEntity.setBatchNumber(oceanEngineDataBo.getBatchNumber());
        videoDataViewingConfuseEntity.setRequestId("0");
        videoDataViewingConfuseEntity.setIsTakeProduct(oceanEngineDataBo.getIsTakeProduct());
        if(oceanEngineDataBo.getWatchFlowList() != null && oceanEngineDataBo.getWatchFlowList().size() > 0) {
            videoDataViewingConfuseEntity.setWatchFlowList(JSON.toJSONString(oceanEngineDataBo.getWatchFlowList()));
        }
        if(oceanEngineDataBo.getPayFlowList() != null && oceanEngineDataBo.getPayFlowList().size() > 0) {
            videoDataViewingConfuseEntity.setPayFlowList(JSON.toJSONString(oceanEngineDataBo.getPayFlowList()));
        }
        if(oceanEngineDataBo.getWatchUserPortrait() != null) {
            videoDataViewingConfuseEntity.setWatchUserPortrait(JSON.toJSONString(oceanEngineDataBo.getWatchUserPortrait()));
        }
        if(oceanEngineDataBo.getPayUserPortrait() != null) {
            videoDataViewingConfuseEntity.setPayUserPortrait(JSON.toJSONString(oceanEngineDataBo.getPayUserPortrait()));
        }
        // 仅当 ossPath 非空时才更新，避免多次上传时覆盖已有的过程数据文件路径
        if (ObjectUtil.isNotEmpty(oceanEngineDataBo.getOssPath())) {
            videoDataViewingConfuseEntity.setOssPath(oceanEngineDataBo.getOssPath());
        }
        videoDataViewingConfuseEntity.setSecUid(oceanEngineDataBo.getSecUid());
        videoDataViewingConfuseEntity.setShowWatchCntRatio(oceanEngineDataBo.getShowWatchCntRatio());
        videoDataViewingConfuseEntity.setRoi(oceanEngineDataBo.getRoi());
        if(oceanEngineDataBo.getLaunchRoiAmount() != null) {
            videoDataViewingConfuseEntity.setLaunchRoiAmount(oceanEngineDataBo.getLaunchRoiAmount() / 100);
        }
        if(oceanEngineDataBo.getRefundAmount() != null) {
            videoDataViewingConfuseEntity.setRefundAmount(oceanEngineDataBo.getRefundAmount() / 100);
        }
        videoDataViewingConfuseEntity.setOverallCostRoi(oceanEngineDataBo.getOverallCostRoi());
        videoDataViewingConfuseEntity.setNetTransactionRoi(oceanEngineDataBo.getNetTransactionRoi());

        // 保存数据看板数据
        if(oldDataViewingId != null) {
            // 修改
            this.videoDataViewingConfuseService.updateById(videoDataViewingConfuseEntity);
        }else {
            // 新增
            this.videoDataViewingConfuseService.save(videoDataViewingConfuseEntity);
        }

        // 级联回刷同一直播间的其他视频：同一 batchNumber + secUid 下，除当前视频以外，
        // 所有兄弟视频的核心指标都刷新为最新的全量累计值（后面录制的视频拿到了更完整的数据）
        cascadeRefreshSiblingVideos(oceanEngineDataBo);

        return BeanConvertUtils.convert(videoDataViewingConfuseEntity, VideoDataViewingConfuseInfoVo.class);
    }

    /**
     * 用最新数据回刷同一场直播的其他视频数据。
     * 同一 batchNumber + secUid 下的视频属于同一直播，后面的视频结束时平台指标已累积到更完整的状态，
     * 将最新的核心指标回填到之前的视频记录上。
     *
     * @param bo 最新的巨量百应数据（已含全量累计指标）
     */
    private void cascadeRefreshSiblingVideos(OceanEngineDataBo bo) {
        String batchNumber = bo.getBatchNumber();
        String secUid = bo.getSecUid();
        String videoId = bo.getVideoId();
        if (batchNumber == null || secUid == null || videoId == null) {
            return;
        }

        LambdaUpdateWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getBatchNumber, batchNumber)
                .eq(VideoDataViewingConfuseEntity::getSecUid, secUid)
                .eq(VideoDataViewingConfuseEntity::getTenantId, bo.getTenantId())
                .eq(VideoDataViewingConfuseEntity::getDataSourceType, DataViewingSourceTypeEnum.JULIANGBAIYING.getType())
                .ne(VideoDataViewingConfuseEntity::getVideoId, videoId);

        boolean hasField = false;

        // 基础指标（有值才 set，避免将已有值刷成 null）
        if (bo.getTotalWatchNum() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getTotalWatchNum, bo.getTotalWatchNum());
            hasField = true;
        }
        if (bo.getAverageOnlineNum() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getAverageOnlineNum, bo.getAverageOnlineNum());
            hasField = true;
        }
        if (bo.getAverageResidenceTime() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getAverageResidenceTime, bo.getAverageResidenceTime());
            hasField = true;
        }
        if (bo.getIncrementFollowerCount() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getIncrementFollowerCount, bo.getIncrementFollowerCount());
            hasField = true;
        }
        if (bo.getConvertFanRate() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getConvertFanRate, bo.getConvertFanRate());
            hasField = true;
        }
        if (bo.getInteractionPercent() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getInteractionPercent, bo.getInteractionPercent() * 100);
            hasField = true;
        }
        if (bo.getVolume() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getVolumeStart, bo.getVolume() / 100);
            wrapper.set(VideoDataViewingConfuseEntity::getVolumeEnd, bo.getVolume() / 100);
            hasField = true;
        }
        if (bo.getPurchaseCount() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getPurchaseCountStart, bo.getPurchaseCount());
            wrapper.set(VideoDataViewingConfuseEntity::getPurchaseCountEnd, bo.getPurchaseCount());
            hasField = true;
        }
        if (bo.getCustomerUnitPrice() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getCustomerUnitPriceStart, bo.getCustomerUnitPrice() / 100);
            wrapper.set(VideoDataViewingConfuseEntity::getCustomerUnitPriceEnd, bo.getCustomerUnitPrice() / 100);
            hasField = true;
        }
        if (bo.getUvValue() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getUvValueStart, bo.getUvValue());
            wrapper.set(VideoDataViewingConfuseEntity::getUvValueEnd, bo.getUvValue());
            hasField = true;
        }
        if (bo.getGoodsConvertRate() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getGoodsConvertRateStart, bo.getGoodsConvertRate());
            wrapper.set(VideoDataViewingConfuseEntity::getGoodsConvertRateEnd, bo.getGoodsConvertRate());
            hasField = true;
        }
        if (bo.getGpm() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getGpmStart, bo.getGpm() / 100);
            wrapper.set(VideoDataViewingConfuseEntity::getGpmEnd, bo.getGpm() / 100);
            hasField = true;
        }
        if (bo.getIsTakeProduct() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getIsTakeProduct, bo.getIsTakeProduct());
            hasField = true;
        }
        if (bo.getShowWatchCntRatio() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getShowWatchCntRatio, bo.getShowWatchCntRatio());
            hasField = true;
        }
        if (bo.getRoi() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getRoi, bo.getRoi());
            hasField = true;
        }
        if (bo.getLaunchRoiAmount() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getLaunchRoiAmount, bo.getLaunchRoiAmount() / 100);
            hasField = true;
        }
        if (bo.getRefundAmount() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getRefundAmount, bo.getRefundAmount() / 100);
            hasField = true;
        }
        if (bo.getOverallCostRoi() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getOverallCostRoi, bo.getOverallCostRoi());
            hasField = true;
        }
        if (bo.getNetTransactionRoi() != null) {
            wrapper.set(VideoDataViewingConfuseEntity::getNetTransactionRoi, bo.getNetTransactionRoi());
            hasField = true;
        }

        if (hasField) {
            wrapper.set(VideoDataViewingConfuseEntity::getDataStatus, 1);
            wrapper.set(VideoDataViewingConfuseEntity::getCrawlTime, new Date());
            wrapper.set(VideoDataViewingConfuseEntity::getUpdateDate, new Date());
            this.videoDataViewingConfuseService.update(wrapper);
        }
    }

    @Override
    public VideoDataViewingConfuseInfoVo getOceanEngineByVideoId(String videoId) {

        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getVideoId, videoId);
        wrapper.eq(VideoDataViewingConfuseEntity::getDataSourceType, DataViewingSourceTypeEnum.JULIANGBAIYING.getType());
        VideoDataViewingConfuseEntity viewingConfuseEntity = this.videoDataViewingConfuseService.getOne(wrapper);
        if(viewingConfuseEntity != null) {

            return BeanConvertUtils.convert(viewingConfuseEntity, VideoDataViewingConfuseInfoVo.class);
        }

        return null;
    }

    @Override
    public VideoDataViewingConfuseInfoVo getDashboardByVideoId(String videoId) {

        // 字段级混合补齐：以巨量百应为主，巨量为空的字段用蝉妈妈对应字段补（不覆盖巨量已有非空值）。
        Map<Integer, VideoDataViewingConfuseEntity> map = getByVideoIdAndSource(videoId, List.of(DataViewingSourceTypeEnum.JULIANGBAIYING.getType(), DataViewingSourceTypeEnum.CHANMAMA.getType()));
        VideoDataViewingConfuseEntity primary = map.get(DataViewingSourceTypeEnum.JULIANGBAIYING.getType());
        VideoDataViewingConfuseEntity secondary = map.get(DataViewingSourceTypeEnum.CHANMAMA.getType());

        if(primary == null && secondary == null) {
            return null;
        }
        if(primary == null) {
            return BeanConvertUtils.convert(secondary, VideoDataViewingConfuseInfoVo.class);
        }
        videoDataViewingParagraphService.lambdaQuery().eq(VideoDataViewingParagraphEntity::getDataViewingConfuseId, primary.getId())
            .last("limit 1").oneOpt().filter(paragraph -> !paragraph.empty())
            .ifPresent(paragraph -> {
                primary.setTotalWatchNum(paragraph.getTotalWatchNum());
                primary.setAverageOnlineNum(paragraph.getAverageOnlineNum());
                primary.setAverageResidenceTime(paragraph.getAverageResidenceTime());
                primary.setIncrementFollowerCount(paragraph.getIncrementFollowerCount());
                primary.setConvertFanRate(paragraph.getConvertFanRate());
                primary.setInteractionPercent(paragraph.getInteractionPercent());
                primary.setVolumeStart(paragraph.getVolumeStart());
                primary.setVolumeEnd(paragraph.getVolumeEnd());
                primary.setPurchaseCountStart(paragraph.getPurchaseCountStart());
                primary.setPurchaseCountEnd(paragraph.getPurchaseCountEnd());
                primary.setCustomerUnitPriceStart(paragraph.getCustomerUnitPriceStart());
                primary.setCustomerUnitPriceEnd(paragraph.getCustomerUnitPriceEnd());
                primary.setUvValueStart(paragraph.getUvValueStart());
                primary.setUvValueEnd(paragraph.getUvValueEnd());
                primary.setGoodsConvertRateStart(paragraph.getGoodsConvertRateStart());
                primary.setGoodsConvertRateEnd(paragraph.getGoodsConvertRateEnd());
                primary.setDataStatus(paragraph.getDataStatus());
                primary.setIsTakeProduct(paragraph.getIsTakeProduct());
                primary.setBatchNumber(paragraph.getBatchNumber());
                primary.setAnchorNumber(paragraph.getAnchorNumber());
                primary.setWatchFlowList(paragraph.getWatchFlowList());
                primary.setPayFlowList(paragraph.getPayFlowList());
                primary.setPayUserPortrait(paragraph.getPayUserPortrait());
                primary.setWatchUserPortrait(paragraph.getWatchUserPortrait());
                primary.setSecUid(paragraph.getSecUid());
                primary.setGpmStart(paragraph.getGpmStart());
                primary.setGpmEnd(paragraph.getGpmEnd());
                primary.setShowWatchCntRatio(paragraph.getShowWatchCntRatio());
                primary.setRoi(paragraph.getRoi());
                primary.setLaunchRoiAmount(paragraph.getLaunchRoiAmount());
                primary.setRefundAmount(paragraph.getRefundAmount());
                primary.setOverallCostRoi(paragraph.getOverallCostRoi());
                primary.setNetTransactionRoi(paragraph.getNetTransactionRoi());
            });
        if(secondary != null) {
            // 蝉妈妈用 -1/-1.0（未采集到）做哨兵，是非 null 值会绕过 ignoreNullValue 灌进巨量空字段，
            // 故先把负数哨兵归一为 null，避免「成交额/UV价值 = -1」这类假数补进看板。
            nullifyNegativeSentinels(secondary);
            // 两源同名列量纲一致（金额=元、停留=秒、互动率=百分数、转粉/转化=小数比率），可直接互补。
            // ignoreNullValue：蝉妈妈空字段不参与；override=false：仅填充巨量为 null 的字段，巨量已有值不被覆盖。
            BeanUtil.copyProperties(secondary, primary,
                    CopyOptions.create().ignoreNullValue().setOverride(false));
        }
        return BeanConvertUtils.convert(primary, VideoDataViewingConfuseInfoVo.class);
    }

    /**
     * 把蝉妈妈看板行里的「未采集到」负数哨兵（-1/-1.0/-2）归一为 null，使其在字段级补齐时被 ignoreNullValue 跳过。
     * 仅作用于金额/比率等不会取负的指标列；计数/比率为 0 属合法值（如本场无销售），不动。
     */
    private void nullifyNegativeSentinels(VideoDataViewingConfuseEntity e) {
        if(e.getVolumeStart() != null && e.getVolumeStart() < 0) { e.setVolumeStart(null); }
        if(e.getVolumeEnd() != null && e.getVolumeEnd() < 0) { e.setVolumeEnd(null); }
        if(e.getPurchaseCountStart() != null && e.getPurchaseCountStart() < 0) { e.setPurchaseCountStart(null); }
        if(e.getPurchaseCountEnd() != null && e.getPurchaseCountEnd() < 0) { e.setPurchaseCountEnd(null); }
        if(e.getCustomerUnitPriceStart() != null && e.getCustomerUnitPriceStart() < 0) { e.setCustomerUnitPriceStart(null); }
        if(e.getCustomerUnitPriceEnd() != null && e.getCustomerUnitPriceEnd() < 0) { e.setCustomerUnitPriceEnd(null); }
        if(e.getUvValueStart() != null && e.getUvValueStart() < 0) { e.setUvValueStart(null); }
        if(e.getUvValueEnd() != null && e.getUvValueEnd() < 0) { e.setUvValueEnd(null); }
        if(e.getGoodsConvertRateStart() != null && e.getGoodsConvertRateStart() < 0) { e.setGoodsConvertRateStart(null); }
        if(e.getGoodsConvertRateEnd() != null && e.getGoodsConvertRateEnd() < 0) { e.setGoodsConvertRateEnd(null); }
    }

    /**
     * 取指定视频指定数据源的看板行；同源多行时不抛异常、取首条。
     */
    private Map<Integer, VideoDataViewingConfuseEntity> getByVideoIdAndSource(String videoId, List<Integer> sourceTypes) {
        List<VideoDataViewingConfuseEntity> list = videoDataViewingConfuseService.lambdaQuery()
            .eq(VideoDataViewingConfuseEntity::getVideoId, videoId)
            .in(VideoDataViewingConfuseEntity::getDataSourceType, sourceTypes)
            .eq(VideoDataViewingConfuseEntity::getDataStatus, 1)
            .eq(VideoDataViewingConfuseEntity::getIsDeleted, 0)
            .orderByDesc(VideoDataViewingConfuseEntity::getId)
            .list();
        if (EmptyUtil.isEmpty(list)) {
            return Map.of();
        }
        return list.stream().collect(Collectors.toMap(VideoDataViewingConfuseEntity::getDataSourceType, e -> e, (e1, e2) -> e1));
    }

    /**
     * 从巨量文件里获取数据
     *
     * @param videoId 视频id
     * @param ossPath oss的key
     * @return 巨量文件数据
     */
    @Override
    public List<OceanEngineProcessBo> getOceanEngineFileData(String videoId, String ossPath) {
        String redisKey = RedisCacheKey.getRedisKey(RedisCacheKey.oceanEngineDataCacheKey, videoId);
        Object strO = redisTemplate.opsForValue().get(redisKey);
        if (ObjectUtil.isNotEmpty(strO)) {
            return JSON.parseArray(strO.toString(), OceanEngineProcessBo.class);
        }
        return getOceanEngineFileDataNotCache(videoId, ossPath);
    }

    @Override
    public List<OceanEngineProcessBo> getOceanEngineFileDataNotCache(String videoId, String ossPath) {
        if (ObjectUtil.isNotEmpty(ossPath)) {
            try {
                byte[] object = ossUtils.getObject(AiOssUtils.bucketNameAi, ossPath);
                String txt = JYStreamUtils.extractTextFromZipBytes(object);
                List<OceanEngineProcessBo> result = new ArrayList<>();
                if (ObjectUtil.isNotEmpty(txt)) {
                    result = Stream.of(txt.trim().split("\n"))
                            .filter(ObjectUtil::isNotEmpty)
                            .map(s -> {
                                return JSONObject.parseObject(s.trim(), OceanEngineProcessBo.class);
                            })
                            .toList();
                }
                redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.oceanEngineDataCacheKey, videoId), JSONUtil.toJsonStr(result), 2, TimeUnit.DAYS);
                return result;
            } catch (Exception e) {
                log.error("读取巨量内容失败，路径为: {}", ossPath, e);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public VideoDataViewingConfuseInfoVo copyDataViewingToSlice(SaveSliceCorrelationDataBo saveSliceCorrelationDataBo) {
        // 获取原视频数据看板
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getVideoId, saveSliceCorrelationDataBo.getSourceVideoId());
        wrapper.eq(VideoDataViewingConfuseEntity::getUserId, saveSliceCorrelationDataBo.getUserId());
        wrapper.eq(VideoDataViewingConfuseEntity::getTenantId, saveSliceCorrelationDataBo.getTenantId());
        VideoDataViewingConfuseEntity dataViewingConfuseEntity = this.videoDataViewingConfuseService.getOne(wrapper);

        if(dataViewingConfuseEntity != null) {
            // 保存切片视频数据看板
            dataViewingConfuseEntity.setId(SnowflakeManager.nextValue());
            dataViewingConfuseEntity.setVideoId(saveSliceCorrelationDataBo.getSliceVideoId());
            dataViewingConfuseEntity.setCreateDate(new Date());
            dataViewingConfuseEntity.setUpdateDate(new Date());
            this.videoDataViewingConfuseService.save(dataViewingConfuseEntity);

            return BeanConvertUtils.convert(dataViewingConfuseEntity, VideoDataViewingConfuseInfoVo.class);

            // 获取原视频本段看板数据，保存到切片本段看板数据
//            LambdaQueryWrapper<VideoDataViewingParagraphEntity> paragraphWrapper = new LambdaQueryWrapper<>();
//            paragraphWrapper.eq(VideoDataViewingParagraphEntity::getVideoId, saveSliceCorrelationDataBo.getSourceVideoId());
//            paragraphWrapper.eq(VideoDataViewingParagraphEntity::getUserId, saveSliceCorrelationDataBo.getUserId());
//            paragraphWrapper.eq(VideoDataViewingParagraphEntity::getTenantId, saveSliceCorrelationDataBo.getTenantId());
//            VideoDataViewingParagraphEntity dataViewingParagraphEntity = this.videoDataViewingParagraphService.getOne(paragraphWrapper);
//            if(dataViewingParagraphEntity != null) {
//                dataViewingParagraphEntity.setId(SnowflakeManager.nextValue());
//                dataViewingParagraphEntity.setVideoId(saveSliceCorrelationDataBo.getSliceVideoId());
//                dataViewingParagraphEntity.setCreateDate(new Date());
//                dataViewingParagraphEntity.setUpdateDate(new Date());
//                this.videoDataViewingParagraphService.save(dataViewingParagraphEntity);
//            }

        }

        return null;
    }

    @Override
    public void updateDataStatusByDataViewingId(Long videoDataViewingId, Integer status) {
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getVideoDataViewingId, videoDataViewingId);
        wrapper.eq(VideoDataViewingConfuseEntity::getDataSourceType, DataViewingSourceTypeEnum.CHANMAMA.getType());
        List<VideoDataViewingConfuseEntity> viewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);

        if(viewingConfuseEntities != null && viewingConfuseEntities.size() > 0) {
            for(VideoDataViewingConfuseEntity entity : viewingConfuseEntities) {
                entity.setDataStatus(status);
                entity.setUpdateDate(new Date());
            }
            this.videoDataViewingConfuseService.updateBatchById(viewingConfuseEntities);
        }
    }

    @Override
    public void updateDataStatusByBatchNumber(String batchNumber, Integer status) {
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getBatchNumber, batchNumber);
        wrapper.eq(VideoDataViewingConfuseEntity::getDataSourceType, DataViewingSourceTypeEnum.CHANMAMA.getType());
        List<VideoDataViewingConfuseEntity> viewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);

        if(viewingConfuseEntities != null && viewingConfuseEntities.size() > 0) {
            for(VideoDataViewingConfuseEntity entity : viewingConfuseEntities) {
                entity.setDataStatus(status);
                entity.setUpdateDate(new Date());
            }
            this.videoDataViewingConfuseService.updateBatchById(viewingConfuseEntities);
        }
    }

    @Override
    public List<VideoDataViewingConfuseInfoVo> listByDataViewingIdAndDataType(Long videoDataViewingId, Integer type) {

        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getVideoDataViewingId, videoDataViewingId);
        wrapper.eq(VideoDataViewingConfuseEntity::getDataSourceType, type);
        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);
        if(videoDataViewingConfuseEntities != null && !videoDataViewingConfuseEntities.isEmpty()) {
            return BeanConvertUtils.convertList(videoDataViewingConfuseEntities, VideoDataViewingConfuseInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public List<VideoDataViewingConfuseInfoVo> listByBatchNumberAndDataType(String batchNumber, Integer type) {

        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getBatchNumber, batchNumber);
        wrapper.eq(VideoDataViewingConfuseEntity::getDataSourceType, type);
        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);
        if(videoDataViewingConfuseEntities != null && !videoDataViewingConfuseEntities.isEmpty()) {
            return BeanConvertUtils.convertList(videoDataViewingConfuseEntities, VideoDataViewingConfuseInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public void updateDataStatus(Long videoDataViewingConfuseId, Integer status) {

        VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = this.videoDataViewingConfuseService.getById(videoDataViewingConfuseId);
        if(videoDataViewingConfuseEntity != null) {
            videoDataViewingConfuseEntity.setDataStatus(status);
            videoDataViewingConfuseEntity.setUpdateDate(new Date());
            this.videoDataViewingConfuseService.updateById(videoDataViewingConfuseEntity);
        }

    }

    @Override
    public List<VideoDataViewingConfuseInfoVo> listByVideoIdsAndStatusAndSourceType(List<String> videoIds, Integer dataStatus, Integer sourceType) {
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VideoDataViewingConfuseEntity::getVideoId, videoIds);
        if(dataStatus != null) {
            wrapper.eq(VideoDataViewingConfuseEntity::getDataStatus, dataStatus);
        }
        if(sourceType != null) {
            wrapper.eq(VideoDataViewingConfuseEntity::getDataSourceType, sourceType);
        }

        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);

        return BeanConvertUtils.convertList(videoDataViewingConfuseEntities, VideoDataViewingConfuseInfoVo.class);
    }

    @Override
    public void copyTenantJlbyData(VideoDataViewingConfuseBo videoDataViewingConfuseBo, Long userId, String videoId) {

        VideoDataViewingConfuseEntity dataViewingConfuseEntity = BeanConvertUtils.convert(videoDataViewingConfuseBo, VideoDataViewingConfuseEntity.class);

        dataViewingConfuseEntity.setId(SnowflakeManager.nextValue());
        dataViewingConfuseEntity.setVideoId(videoId);
        dataViewingConfuseEntity.setUserId(userId);
        dataViewingConfuseEntity.setCreateDate(new Date());
        dataViewingConfuseEntity.setUpdateDate(new Date());

        this.videoDataViewingConfuseService.save(dataViewingConfuseEntity);
    }

    @Override
    public List<VideoDataViewingConfuseInfoVo> listByRequestId(String requestId) {

        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getRequestId, requestId);
        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);
        if(videoDataViewingConfuseEntities != null) {
            return BeanConvertUtils.convertList(videoDataViewingConfuseEntities, VideoDataViewingConfuseInfoVo.class);
        }

        return null;
    }

    @Override
    public VideoDataViewingConfuseInfoVo checkJlbyDataExist(String videoId) {

        LambdaQueryWrapper<AnchorVideoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorVideoEntity::getVideoId, videoId);
        AnchorVideoEntity video = this.anchorVideoService.getOne(wrapper);
        if(video == null) {
            return null;
        }

        // 取出结束时间的前后5分钟
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant instant = video.getEndTime().toInstant();
        LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        // 计算前5分钟
        LocalDateTime fiveMinutesBefore = dateTime.minusMinutes(5);
        String beforeStr = fiveMinutesBefore.format(formatter);
        // 计算后5分钟
        LocalDateTime fiveMinutesAfter = dateTime.plusMinutes(5);
        String afterStr = fiveMinutesAfter.format(formatter);

        // 获取同租户同场次并且结束时间差值在5分钟内的视频videoId集合
        LambdaQueryWrapper<AnchorVideoEntity> listVideoWrapper = new LambdaQueryWrapper<>();
        listVideoWrapper.eq(AnchorVideoEntity::getTenantId, video.getTenantId());
        listVideoWrapper.eq(AnchorVideoEntity::getBatchNumber, video.getBatchNumber());
        listVideoWrapper.ge(AnchorVideoEntity::getEndTime, beforeStr);
        listVideoWrapper.le(AnchorVideoEntity::getEndTime, afterStr);
        listVideoWrapper.select(AnchorVideoEntity::getVideoId);
        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(listVideoWrapper);
        if(anchorVideoEntities == null || anchorVideoEntities.isEmpty()) {
            return null;
        }
        List<String> videoIds = anchorVideoEntities.stream().map(AnchorVideoEntity::getVideoId).collect(Collectors.toList());

        // 根据视频id获取巨量百应数据
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> viewingWrapper = new LambdaQueryWrapper<>();
        viewingWrapper.in(VideoDataViewingConfuseEntity::getVideoId, videoIds);
        viewingWrapper.eq(VideoDataViewingConfuseEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus());
        viewingWrapper.eq(VideoDataViewingConfuseEntity::getDataSourceType, DataViewingSourceTypeEnum.JULIANGBAIYING.getType());
        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(viewingWrapper);
        if(videoDataViewingConfuseEntities == null || videoDataViewingConfuseEntities.isEmpty()) {
            return null;
        }

        return BeanConvertUtils.convert(videoDataViewingConfuseEntities.get(0), VideoDataViewingConfuseInfoVo.class);
    }

    @Override
    public void deleteByVideoId(String videoId) {
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getVideoId, videoId);

        this.videoDataViewingConfuseService.remove(wrapper);
    }

    @Override
    public R<String> updateByVideoId(UpdateConfuseDataByVideoIdBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getVideoId())) {
            return R.error("视频ID不能为空");
        }

        // 1. 根据videoId查询混淆表获取该视频的混淆数据
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getVideoId, bo.getVideoId());
        VideoDataViewingConfuseEntity confuseEntity = this.videoDataViewingConfuseService.getOne(wrapper);

        if (confuseEntity == null) {
            return R.error("视频不存在数据看板");
        }

        // 2. 获取关联的数据看盘ID
        Long videoDataViewingId = confuseEntity.getVideoDataViewingId();

        List<VideoDataViewingConfuseEntity> updateList;

        if (videoDataViewingId != null && videoDataViewingId > 0) {
            // 3. 根据videoDataViewingId查询所有关联的混淆数据
            LambdaQueryWrapper<VideoDataViewingConfuseEntity> listWrapper = new LambdaQueryWrapper<>();
            listWrapper.eq(VideoDataViewingConfuseEntity::getVideoDataViewingId, videoDataViewingId);
            updateList = this.videoDataViewingConfuseService.list(listWrapper);
        } else {
            // 如果videoDataViewingId为空或0，只更新当前记录
            updateList = new ArrayList<>();
            updateList.add(confuseEntity);
        }

        if (CollUtil.isEmpty(updateList)) {
            return R.error("不存在数据看板修改");
        }

        // 4. 批量更新所有关联的混淆数据记录（仅更新非空字段）
        Date now = new Date();
        for (VideoDataViewingConfuseEntity entity : updateList) {
            updateEntityFromBo(entity, bo);
            entity.setDataStatus(DataViewingStatusEnum.PULL_SUCCESS.getStatus());
            entity.setUpdateDate(now);
        }

        this.videoDataViewingConfuseService.updateBatchById(updateList);

        log.info("updateByVideoId: 批量更新混淆数据成功, videoId={}, videoDataViewingId={}, 更新记录数={}",
                bo.getVideoId(), videoDataViewingId, updateList.size());

        return R.ok();
    }

    @Override
    public Map<String, Boolean> getVideoHasDashboardMap(Collection<String> videoIds) {
        if (CollUtil.isEmpty(videoIds)) {
            return new HashMap<>();
        }
        return videoDataViewingConfuseService.lambdaQuery()
                .select(VideoDataViewingConfuseEntity::getVideoId)
                .in(VideoDataViewingConfuseEntity::getVideoId, videoIds)
                .eq(VideoDataViewingConfuseEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus())
                .list()
                .stream().map(VideoDataViewingConfuseEntity::getVideoId)
                .collect(Collectors.toMap(Function.identity(), videoId -> true));
    }

    /**
     * 批量查询视频观看次数
     *
     * @param videoIds 视频ID集合
     *
     * @return 视频ID和观看次数的映射关系
     */
    @Override
    public Map<String, VideoWatchHasVO> getVideoWatchNum(Collection<String> videoIds) {
        if (CollUtil.isEmpty(videoIds)) {
            return new HashMap<>();
        }
        Map<String, VideoWatchHasVO> videoWatchNumMap = videoDataViewingParagraphService.lambdaQuery()
            .select(VideoDataViewingParagraphEntity::getVideoId, VideoDataViewingParagraphEntity::getTotalWatchNum, VideoDataViewingParagraphEntity::getLaunchRoiAmount)
            .in(VideoDataViewingParagraphEntity::getVideoId, videoIds)
            .eq(VideoDataViewingParagraphEntity::getIsDeleted, 0)
            .eq(VideoDataViewingParagraphEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus())
            .list().stream().filter(paragraph -> paragraph.getTotalWatchNum() != null).collect(Collectors.toMap(VideoDataViewingParagraphEntity::getVideoId, v -> {
                VideoWatchHasVO videoWatchHasVO = new VideoWatchHasVO();
                videoWatchHasVO.setWatchNum(v.getTotalWatchNum());
                videoWatchHasVO.setHasRoi(v.getLaunchRoiAmount() != null && v.getLaunchRoiAmount() > 0);
                return videoWatchHasVO;
            }));


        Map<String, VideoWatchHasVO> map = videoDataViewingConfuseService.lambdaQuery()
            .select(VideoDataViewingConfuseEntity::getVideoId, VideoDataViewingConfuseEntity::getTotalWatchNum, VideoDataViewingConfuseEntity::getLaunchRoiAmount)
            .in(VideoDataViewingConfuseEntity::getVideoId, videoIds)
            .eq(VideoDataViewingConfuseEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus())
            .eq(VideoDataViewingConfuseEntity::getIsDeleted, 0)
            .list()
            .stream().filter(paragraph -> paragraph.getTotalWatchNum() != null)
            .collect(Collectors.toMap(VideoDataViewingConfuseEntity::getVideoId, v -> {
                VideoWatchHasVO videoWatchHasVO = new VideoWatchHasVO();
                videoWatchHasVO.setWatchNum(v.getTotalWatchNum());
                videoWatchHasVO.setHasRoi(v.getLaunchRoiAmount() != null && v.getLaunchRoiAmount() > 0);
                return videoWatchHasVO;
            }));

        if (EmptyUtil.isEmpty(videoWatchNumMap) && EmptyUtil.isEmpty(map)) {
            return new HashMap<>();
        }
        return videoIds.stream().filter(videoId -> videoWatchNumMap.containsKey(videoId) || map.containsKey(videoId)).collect(Collectors.toMap(Function.identity(), videoId -> {
            VideoWatchHasVO paragraph = videoWatchNumMap.get(videoId);
            VideoWatchHasVO confuse = map.get(videoId);
            if (paragraph == null) {
                return confuse;
            }
            if (confuse == null) {
                return paragraph;
            }
            VideoWatchHasVO hasVO = new VideoWatchHasVO();
            hasVO.setWatchNum(confuse.getWatchNum());
            hasVO.setHasRoi(confuse.getHasRoi());
            if (paragraph.getWatchNum() != null && paragraph.getWatchNum() > 0) {
                hasVO.setWatchNum(paragraph.getWatchNum());
            }
            if (paragraph.getHasRoi() != null) {
                hasVO.setHasRoi(paragraph.getHasRoi());
            }
            return hasVO;
        }));

    }

    /**
     * 将BO中的非空字段更新到Entity
     *
     * @param entity 实体对象
     * @param bo     请求参数
     */
    private void updateEntityFromBo(VideoDataViewingConfuseEntity entity, UpdateConfuseDataByVideoIdBo bo) {
        if (bo.getTotalWatchNum() != null) {
            entity.setTotalWatchNum(bo.getTotalWatchNum());
        }
        if (bo.getAverageOnlineNum() != null) {
            entity.setAverageOnlineNum(bo.getAverageOnlineNum());
        }
        if (bo.getAverageResidenceTime() != null) {
            entity.setAverageResidenceTime(bo.getAverageResidenceTime());
        }
        if (bo.getIncrementFollowerCount() != null) {
            entity.setIncrementFollowerCount(bo.getIncrementFollowerCount());
        }
        if (bo.getConvertFanRate() != null) {
            entity.setConvertFanRate(bo.getConvertFanRate());
        }
        if (bo.getInteractionPercent() != null) {
            entity.setInteractionPercent(bo.getInteractionPercent());
        }
        if (bo.getVolumeStart() != null) {
            entity.setVolumeStart(bo.getVolumeStart());
        }
        if (bo.getVolumeEnd() != null) {
            entity.setVolumeEnd(bo.getVolumeEnd());
        }
        if (bo.getPurchaseCountStart() != null) {
            entity.setPurchaseCountStart(bo.getPurchaseCountStart());
        }
        if (bo.getPurchaseCountEnd() != null) {
            entity.setPurchaseCountEnd(bo.getPurchaseCountEnd());
        }
        if (bo.getCustomerUnitPriceStart() != null) {
            entity.setCustomerUnitPriceStart(bo.getCustomerUnitPriceStart());
        }
        if (bo.getCustomerUnitPriceEnd() != null) {
            entity.setCustomerUnitPriceEnd(bo.getCustomerUnitPriceEnd());
        }
        if (bo.getUvValueStart() != null) {
            entity.setUvValueStart(bo.getUvValueStart());
        }
        if (bo.getUvValueEnd() != null) {
            entity.setUvValueEnd(bo.getUvValueEnd());
        }
        if (bo.getGoodsConvertRateStart() != null) {
            entity.setGoodsConvertRateStart(bo.getGoodsConvertRateStart());
        }
        if (bo.getGoodsConvertRateEnd() != null) {
            entity.setGoodsConvertRateEnd(bo.getGoodsConvertRateEnd());
        }
        if (bo.getGpmStart() != null) {
            entity.setGpmStart(bo.getGpmStart());
        }
        if (bo.getGpmEnd() != null) {
            entity.setGpmEnd(bo.getGpmEnd());
        }
        if (bo.getShowWatchCntRatio() != null) {
            entity.setShowWatchCntRatio(bo.getShowWatchCntRatio());
        }
    }

}

