package com.jiuyu.replay.words.producer.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.entity.SystemConfigEntity;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemConfigService;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.words.viewing.FlowSourceDto;
import com.jiuyu.replay.generic.dto.words.viewing.UserPortraitDto;
import com.jiuyu.replay.generic.dto.words.viewing.UserPortraitItemDto;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.viewing.*;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.enums.ChanmamaCallbackDataStatusEnum;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.producer.VideoDataViewingProducer;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingListVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 视频看盘数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Service
@Slf4j
public class VideoDataViewingProducerImpl implements VideoDataViewingProducer {

    @Resource
    private VideoDataViewingService videoDataViewingService;
    @Resource
    private VideoDataViewingConfuseService videoDataViewingConfuseService;
    @Resource
    private VideoDataViewingRatioService videoDataViewingRatioService;
    @Resource
    private AnchorVideoService anchorVideoService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private SocketCollectMessageService socketCollectMessageService;
    @Resource
    private SystemKvService systemKvService;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public PageUtils<VideoDataViewingListVo> queryPage(VideoDataViewingListBo videoDataViewingListBo) {
        QueryWrapper<VideoDataViewingEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(videoDataViewingListBo.getKeyword())){
            wrapper.like("name", videoDataViewingListBo.getKeyword());
        }

        IPage<VideoDataViewingEntity> iPage = videoDataViewingService.page(new Query<VideoDataViewingEntity>().getPage(videoDataViewingListBo.getPage(), videoDataViewingListBo.getLimit()), wrapper);

        PageUtils<VideoDataViewingListVo> pageUtils = new PageUtils<>(videoDataViewingListBo.getPage(), videoDataViewingListBo.getLimit(), iPage);

        List<VideoDataViewingEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<VideoDataViewingListVo> vos = records.stream().map(item -> {
                VideoDataViewingListVo videoDataViewingVo = new VideoDataViewingListVo();
                BeanUtils.copyProperties(item, videoDataViewingVo);
                return videoDataViewingVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public VideoDataViewingInfoVo info(Long id) {

        VideoDataViewingEntity videoDataViewingEntity = videoDataViewingService.getById(id);
        if(videoDataViewingEntity != null) {
            VideoDataViewingInfoVo videoDataViewingInfoVo = new VideoDataViewingInfoVo();
            BeanUtils.copyProperties(videoDataViewingEntity, videoDataViewingInfoVo);
            return videoDataViewingInfoVo;
        }

        return null;
    }

    @Override
    public VideoDataViewingInfoVo save(VideoDataViewingBo videoDataViewingBo) {

         VideoDataViewingEntity videoDataViewingEntity = new VideoDataViewingEntity();
         BeanUtils.copyProperties(videoDataViewingBo, videoDataViewingEntity);
         videoDataViewingEntity.setId(SnowflakeManager.nextValue());
         videoDataViewingEntity.setCreateDate(new Date());
         videoDataViewingEntity.setUpdateDate(new Date());

         videoDataViewingService.save(videoDataViewingEntity);

         VideoDataViewingInfoVo videoDataViewingInfoVo = new VideoDataViewingInfoVo();
         BeanUtils.copyProperties(videoDataViewingEntity, videoDataViewingInfoVo);

         return videoDataViewingInfoVo;
     }

    @Override
    public void update(VideoDataViewingBo videoDataViewingBo) {

        VideoDataViewingEntity videoDataViewingEntity = new VideoDataViewingEntity();
        BeanUtils.copyProperties(videoDataViewingBo, videoDataViewingEntity);
        videoDataViewingEntity.setUpdateDate(new Date());

        videoDataViewingService.updateById(videoDataViewingEntity);
    }

    @Override
    public void deleteById(Long id) {

        videoDataViewingService.removeById(id);
    }

    @Override
    public Boolean checkLocalDBExistCreate(String videoId, String anchorNumber, long startTime, long endTime, String batchNumber, Long userId, Long tenantId) {

        QueryWrapper<VideoDataViewingEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("anchor_number", anchorNumber);
        wrapper.eq("batch_number", batchNumber);
        wrapper.le("live_start_date_stamp", startTime);
        wrapper.ge("live_end_date_stamp", endTime);
        wrapper.eq("live_status", DataViewingStatusEnum.PULL_SUCCESS.getStatus());
        wrapper.orderByDesc("id");
        List<VideoDataViewingEntity> videoDataViewingEntities = this.videoDataViewingService.list(wrapper);
        if(videoDataViewingEntities != null && videoDataViewingEntities.size() > 0) {
            VideoDataViewingEntity videoDataViewingEntity = videoDataViewingEntities.get(0);

            VideoDataViewingConfuseEntity videoDataViewingConfuseEntity = new VideoDataViewingConfuseEntity();
            videoDataViewingConfuseEntity.setId(SnowflakeManager.nextValue());
            videoDataViewingConfuseEntity.setVideoId(videoId);
            videoDataViewingConfuseEntity.setUserId(userId);
            videoDataViewingConfuseEntity.setTenantId(tenantId);
            videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.PULL_SUCCESS.getStatus());
            videoDataViewingConfuseEntity.setCrawlTime(new Date());
            videoDataViewingConfuseEntity.setCreateDate(new Date());
            videoDataViewingConfuseEntity.setUpdateDate(new Date());
            videoDataViewingConfuseEntity.setVideoDataViewingId(videoDataViewingEntity.getId());
            videoDataViewingConfuseEntity.setRequestId("0");
            videoDataViewingConfuseEntity.setBatchNumber(videoDataViewingEntity.getBatchNumber());
            videoDataViewingConfuseEntity.setAnchorNumber(videoDataViewingEntity.getAnchorNumber());
            videoDataViewingConfuseEntity.setIsTakeProduct(videoDataViewingEntity.getIsTakeProduct());

            // 混淆数据
            confuseData(videoDataViewingEntity, videoDataViewingConfuseEntity);

            this.videoDataViewingConfuseService.save(videoDataViewingConfuseEntity);

            return true;
        }

        return false;
    }

    @Override
    public VideoDataViewingEntity saveDataViewingCallbackData(LiveRoomBo liveRoomBo, String batchNumber, String requestId) {

        // 获取主播抖音号
        String anchorNumber = liveRoomBo.getLiveAuthorVo().getUniqueId();
        int liveStatus = liveRoomBo.getStatus() == 4 ? 1 : 0;

        // 获取直播时间
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startLocalDateTime = LocalDateTime.parse(liveRoomBo.getBeginTime(), dtf);
        Date startDate = Date.from(startLocalDateTime.atZone(TimeZone.getDefault().toZoneId()).toInstant());
        long startDateStamp = startDate.getTime();

        LocalDateTime endLocalDateTime = LocalDateTime.parse(liveRoomBo.getRoomFinishTime(), dtf);
//        LocalDateTime endLocalDateTime = startLocalDateTime.plusSeconds(liveRoomBo.getLiveDuration());
        Date endDate = Date.from(endLocalDateTime.atZone(TimeZone.getDefault().toZoneId()).toInstant());
        long endDateStamp = endDate.getTime();

        boolean isAdd = false;

        // 查询是否已经有数据
        VideoDataViewingEntity videoDataViewingEntity = this.videoDataViewingService.getOne(new QueryWrapper<VideoDataViewingEntity>().eq("request_id", requestId));
        if(videoDataViewingEntity == null) {
            isAdd = true;
            videoDataViewingEntity = new VideoDataViewingEntity();
            videoDataViewingEntity.setId(SnowflakeManager.nextValue());
            videoDataViewingEntity.setCreateDate(new Date());
        }

        // 新增数据
        videoDataViewingEntity.setUpdateDate(new Date());
        videoDataViewingEntity.setAnchorNumber(anchorNumber);
        videoDataViewingEntity.setLiveStatus(liveStatus);
        videoDataViewingEntity.setLiveStartDate(startDate);
        videoDataViewingEntity.setLiveEndDate(endDate);
        videoDataViewingEntity.setLiveStartDateStamp(startDateStamp);
        videoDataViewingEntity.setLiveEndDateStamp(endDateStamp);
        videoDataViewingEntity.setBatchNumber(batchNumber);
        videoDataViewingEntity.setRequestId(requestId);
        if(liveRoomBo.getIsTakeProduct() != null && liveRoomBo.getIsTakeProduct()) {
            videoDataViewingEntity.setIsTakeProduct(1);
        }else {
            videoDataViewingEntity.setIsTakeProduct(0);
        }
        // 填充第三方数据平台原始数据
        fillDataViewingData(videoDataViewingEntity, liveRoomBo);
        // 填充用户画像和流量结构数据
        fillFlowAndPortrait(videoDataViewingEntity, liveRoomBo);
        // 填充混淆后的数据
        fillDataViewingConfuseData(videoDataViewingEntity);

        if(isAdd) {
            this.videoDataViewingService.save(videoDataViewingEntity);
        }else {
            this.videoDataViewingService.updateById(videoDataViewingEntity);
        }

        return videoDataViewingEntity;

    }

    /**
     * 填充用户画像和流量结构数据
     * @param videoDataViewingEntity 数据看板对象
     * @param liveRoomBo 回调数据对象
     */
    private void fillFlowAndPortrait(VideoDataViewingEntity videoDataViewingEntity, LiveRoomBo liveRoomBo) {
        // 填充流量结构
        if(liveRoomBo.getLiveUserSourceVos() != null && liveRoomBo.getLiveUserSourceVos().size() > 0) {
            List<FlowSourceDto> flowSourceDtoList = liveRoomBo.getLiveUserSourceVos().stream().map(item -> {
                FlowSourceDto flowSourceDto = new FlowSourceDto();
                flowSourceDto.setChannelName(item.getTitle());
                flowSourceDto.setRatio(item.getRate());
                return flowSourceDto;
            }).collect(Collectors.toList());

            videoDataViewingEntity.setWatchFlowList(JSON.toJSONString(flowSourceDtoList));
        }

        // 填充用户画像
        if((liveRoomBo.getLivePortraitAgeVos() != null && liveRoomBo.getLivePortraitAgeVos().size() > 0) ||
                (liveRoomBo.getLivePortraitProvinceVos() != null && liveRoomBo.getLivePortraitProvinceVos().size() > 0) ||
                (liveRoomBo.getLivePortraitGenderVos() != null && liveRoomBo.getLivePortraitGenderVos().size() > 0)) {
            UserPortraitDto userPortraitDto = new UserPortraitDto();
            // 填充年龄分布信息
            if(liveRoomBo.getLivePortraitAgeVos() != null && liveRoomBo.getLivePortraitAgeVos().size() > 0) {
                List<UserPortraitItemDto> ageList = liveRoomBo.getLivePortraitAgeVos().stream().map(item -> {
                    UserPortraitItemDto userPortraitItemDto = new UserPortraitItemDto();
                    userPortraitItemDto.setLabel(item.getTitle());
                    userPortraitItemDto.setValue((item.getRate() / 100) + "");
                    return userPortraitItemDto;
                }).sorted(Comparator.comparingInt(item -> {
                    String label = item.getLabel();
                    try {
                        if (label.startsWith(">")) {
                            // 处理格式：">50"，返回51确保排在50之后
                            return Integer.parseInt(label.substring(1)) + 1;
                        } else if (label.contains("-")) {
                            // 处理格式："18-23"，返回下限值
                            return Integer.parseInt(label.split("-")[0]);
                        } else if (label.matches("\\d+")) {
                            // 如纯数字，直接解析
                            return Integer.parseInt(label);
                        } else {
                            // 其他场景，取第一个数字 + 1
                            Pattern pattern = Pattern.compile("\\d+");
                            Matcher matcher = pattern.matcher(label);
                            if (matcher.find()) {
                                return Integer.parseInt(matcher.group()) + 1;
                            }
                            return 100;
                        }
                    }catch (Exception e) {
                        log.warn("第三方数据回调处理--解析年龄分布信息发生异常{}", label, e);
                    }
                    return 100;
                })).collect(Collectors.toList());

                userPortraitDto.setAgePortrait(ageList);
            }
            // 填充省份分布信息
            if(liveRoomBo.getLivePortraitProvinceVos() != null && liveRoomBo.getLivePortraitProvinceVos().size() > 0) {
                List<UserPortraitItemDto> provinceList = liveRoomBo.getLivePortraitProvinceVos().stream().map(item -> {
                    UserPortraitItemDto userPortraitItemDto = new UserPortraitItemDto();
                    userPortraitItemDto.setLabel(item.getTitle());
                    userPortraitItemDto.setValue((item.getRate() / 100) + "");
                    return userPortraitItemDto;
                }).collect(Collectors.toList());
                userPortraitDto.setProvincePortrait(provinceList);
            }
            // 填充性别分布信息
            if(liveRoomBo.getLivePortraitGenderVos() != null && liveRoomBo.getLivePortraitGenderVos().size() > 0) {
                List<UserPortraitItemDto> provinceList = liveRoomBo.getLivePortraitGenderVos().stream().map(item -> {
                    UserPortraitItemDto userPortraitItemDto = new UserPortraitItemDto();
                    userPortraitItemDto.setLabel(item.getTitle());
                    userPortraitItemDto.setValue((item.getRate() / 100) + "");
                    return userPortraitItemDto;
                }).collect(Collectors.toList());
                userPortraitDto.setGenderPortrait(provinceList);
            }

            videoDataViewingEntity.setWatchUserPortrait(JSON.toJSONString(userPortraitDto));
        }
    }

    private void fillDataViewingConfuseData(VideoDataViewingEntity videoDataViewingEntity) {

        // 设置混淆比例
        double totalWatchNumRatio = 1.0;
        double averageOnlineNumRatio = 1.0;
        double averageResidenceTimeRatio = 1.0;
        double incrementFollowerCountRatio = 1.0;
        double purchaseCountRatio = 1.0;
        double volumeRatio = 1.0;

        List<VideoDataViewingRatioEntity> videoDataViewingRatioEntities = this.videoDataViewingRatioService.list();
        if(videoDataViewingRatioEntities != null) {
            Map<String, VideoDataViewingRatioEntity> ratioMap = videoDataViewingRatioEntities.stream().collect(Collectors.toMap(VideoDataViewingRatioEntity::getRatioCode, item -> item));

            totalWatchNumRatio = ratioMap.get("total_watch_num") == null ? 1.0 : computeRatio(ratioMap.get("total_watch_num").getRatioStart(), ratioMap.get("total_watch_num").getRatioEnd());
            averageOnlineNumRatio = ratioMap.get("average_online_num") == null ? 1.0 : computeRatio(ratioMap.get("average_online_num").getRatioStart(), ratioMap.get("average_online_num").getRatioEnd());
            averageResidenceTimeRatio = ratioMap.get("average_residence_time") == null ? 1.0 : computeRatio(ratioMap.get("average_residence_time").getRatioStart(), ratioMap.get("average_residence_time").getRatioEnd());
            incrementFollowerCountRatio = ratioMap.get("increment_follower_count") == null ? 1.0 : computeRatio(ratioMap.get("increment_follower_count").getRatioStart(), ratioMap.get("increment_follower_count").getRatioEnd());
            purchaseCountRatio = ratioMap.get("purchase_count") == null ? 1.0 : computeRatio(ratioMap.get("purchase_count").getRatioStart(), ratioMap.get("purchase_count").getRatioEnd());
            volumeRatio = ratioMap.get("volume") == null ? 1.0 : computeRatio(ratioMap.get("volume").getRatioStart(), ratioMap.get("volume").getRatioEnd());
        }

        videoDataViewingEntity.setTotalWatchNumConfuse((int) (videoDataViewingEntity.getTotalWatchNum() * totalWatchNumRatio));
        videoDataViewingEntity.setAverageOnlineNumConfuse((int) (videoDataViewingEntity.getAverageOnlineNum() * averageOnlineNumRatio));
        videoDataViewingEntity.setAverageResidenceTimeConfuse((int) (videoDataViewingEntity.getAverageResidenceTime() * averageResidenceTimeRatio));
        videoDataViewingEntity.setIncrementFollowerCountConfuse((int) (videoDataViewingEntity.getIncrementFollowerCount() * incrementFollowerCountRatio));
        if(videoDataViewingEntity.getTotalWatchNumConfuse() > 0) {
            videoDataViewingEntity.setConvertFanRateConfuse((double) videoDataViewingEntity.getIncrementFollowerCountConfuse() / videoDataViewingEntity.getTotalWatchNumConfuse());
        }else {
            videoDataViewingEntity.setConvertFanRateConfuse(0.0);
        }
        videoDataViewingEntity.setInteractionPercentConfuse(videoDataViewingEntity.getInteractionPercent());

        // 混淆销售额
        if(videoDataViewingEntity.getVolumeStart() != -1) {
            videoDataViewingEntity.setVolumeStartConfuse((int)(videoDataViewingEntity.getVolumeStart() * volumeRatio));
            videoDataViewingEntity.setVolumeEndConfuse((int)(videoDataViewingEntity.getVolumeEnd() * volumeRatio));
        }else {
            videoDataViewingEntity.setVolumeStartConfuse(-1);
            videoDataViewingEntity.setVolumeEndConfuse(-1);
        }


        // 混淆销量
        if(videoDataViewingEntity.getPurchaseCountStart() != -1) {
            videoDataViewingEntity.setPurchaseCountStartConfuse((int)(videoDataViewingEntity.getPurchaseCountStart() * purchaseCountRatio));
            videoDataViewingEntity.setPurchaseCountEndConfuse((int)(videoDataViewingEntity.getPurchaseCountEnd() * purchaseCountRatio));
        }else {
            videoDataViewingEntity.setPurchaseCountStartConfuse(-1);
            videoDataViewingEntity.setPurchaseCountEndConfuse(-1);
        }

        // 设置客单价
        videoDataViewingEntity.setCustomerUnitPriceStartConfuse(videoDataViewingEntity.getCustomerUnitPriceStart());
        videoDataViewingEntity.setCustomerUnitPriceEndConfuse(videoDataViewingEntity.getCustomerUnitPriceEnd());

        // 设置uv价值
        if(videoDataViewingEntity.getVolumeStartConfuse() != -1) {
            videoDataViewingEntity.setUvValueStartConfuse(0.0);
            videoDataViewingEntity.setUvValueEndConfuse(0.0);
            if(videoDataViewingEntity.getTotalWatchNumConfuse() > 0) {
                videoDataViewingEntity.setUvValueStartConfuse((double)videoDataViewingEntity.getVolumeStartConfuse() / videoDataViewingEntity.getTotalWatchNumConfuse());
                videoDataViewingEntity.setUvValueEndConfuse((double)videoDataViewingEntity.getVolumeEndConfuse() / videoDataViewingEntity.getTotalWatchNumConfuse());
            }
        }else {
            videoDataViewingEntity.setUvValueStartConfuse(-1.0);
            videoDataViewingEntity.setUvValueEndConfuse(-1.0);
        }

        // 设置带货转换率
        if(videoDataViewingEntity.getPurchaseCountStartConfuse() != -1) {
            videoDataViewingEntity.setGoodsConvertRateStartConfuse(0.0);
            videoDataViewingEntity.setGoodsConvertRateEndConfuse(0.0);
            if(videoDataViewingEntity.getTotalWatchNumConfuse() > 0) {
                videoDataViewingEntity.setGoodsConvertRateStartConfuse((double)videoDataViewingEntity.getPurchaseCountStartConfuse() / videoDataViewingEntity.getTotalWatchNumConfuse());
                videoDataViewingEntity.setGoodsConvertRateEndConfuse((double)videoDataViewingEntity.getPurchaseCountEndConfuse() / videoDataViewingEntity.getTotalWatchNumConfuse());

            }
        }else {
            videoDataViewingEntity.setGoodsConvertRateStartConfuse(-1.0);
            videoDataViewingEntity.setGoodsConvertRateEndConfuse(-1.0);
        }
    }

    @Override
    public List<UseDataViewingPropertyBo> checkLocalDbExistUpdateStatus(VideoDataViewingEntity videoDataViewingEntity, String anchorNumber, String batchNumber, Integer dataStatus) {

        // 获取当前主播正在拉取的数据记录
        QueryWrapper<VideoDataViewingConfuseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("anchor_number", anchorNumber);
        wrapper.in("data_status",
                DataViewingStatusEnum.PULLING.getStatus(),
                DataViewingStatusEnum.PULL_FAIL.getStatus(),
                DataViewingStatusEnum.DATA_ORGANIZE.getStatus());
        wrapper.eq("batch_number", batchNumber);
        List<VideoDataViewingConfuseEntity> videoDataViewingConfuseEntities = this.videoDataViewingConfuseService.list(wrapper);
        if(videoDataViewingConfuseEntities == null || videoDataViewingConfuseEntities.isEmpty()) {
            return null;
        }

        // 获取看盘数据记录的视频信息
        List<String> videoIds = videoDataViewingConfuseEntities.stream().map(VideoDataViewingConfuseEntity::getVideoId).toList();
        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>().in("video_id", videoIds));
        if(anchorVideoEntities == null || anchorVideoEntities.isEmpty()) {
            return null;
        }

        List<VideoDataViewingConfuseEntity> updateList = new LinkedList<>();

        SystemConfigEntity systemConfigEntity = systemConfigService.getById(1);

        // 判断视频录制时间是否在直播时间内
        for (VideoDataViewingConfuseEntity videoDataViewingConfuseEntity : videoDataViewingConfuseEntities) {
            for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
                if(videoDataViewingConfuseEntity.getVideoId().equals(anchorVideoEntity.getVideoId())) {
                    // 获取录制开始时间
                    long startStamp = anchorVideoEntity.getStartTime().getTime();
                    long startTimeDelayed = 60 * 1000;
                    SystemKvEntity systemKvEntity = systemKvService.getByKey("chanmama_match_start_time_delayed");
                    if(systemKvEntity != null) {
                        try {
                            startTimeDelayed = Long.parseLong(systemKvEntity.getKvValue()) * 1000;
                        }catch (Exception e) {
                            log.warn("匹配第三方结果数据===系统键值对转换异常：{}", JSON.toJSONString(systemKvEntity));
                        }
                    }
                    startStamp += startTimeDelayed;
                    // 获取结束时间
                    long time = 180 * 1000;
                    if(systemConfigEntity != null) {
                        time = systemConfigEntity.getDataViewingTimeDifference();
                    }
                    long endStamp = anchorVideoEntity.getEndTime().getTime() - time;
                    if(endStamp < startStamp) {
                        endStamp = startStamp;
                    }
                    if(startStamp >= videoDataViewingEntity.getLiveStartDateStamp() && endStamp <= videoDataViewingEntity.getLiveEndDateStamp()) {
                        // 封装混淆数据
                        confuseData(videoDataViewingEntity, videoDataViewingConfuseEntity);

                        // 设置数据状态
                        if(ChanmamaCallbackDataStatusEnum.ABNORMAL.getStatus().equals(dataStatus)) {
                            videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.DATA_ORGANIZE.getStatus());
                        }else if(ChanmamaCallbackDataStatusEnum.NOT_CHECK.getStatus().equals(dataStatus)) {
                            // 检查本地数据库当前主播是否已经获取过数据看板
                            Long count = countAnchorViewingNum(videoDataViewingConfuseEntity.getAnchorNumber());
                            if(count > 0) {
                                videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.PULL_SUCCESS.getStatus());
                            }else {
                                videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.DATA_ORGANIZE.getStatus());
                            }
                        }else {
                            videoDataViewingConfuseEntity.setDataStatus(DataViewingStatusEnum.PULL_SUCCESS.getStatus());
                        }

                        videoDataViewingConfuseEntity.setIsTakeProduct(videoDataViewingEntity.getIsTakeProduct());
                        videoDataViewingConfuseEntity.setUpdateDate(new Date());
                        videoDataViewingConfuseEntity.setCrawlTime(new Date());
                        videoDataViewingConfuseEntity.setVideoDataViewingId(videoDataViewingEntity.getId());
                        updateList.add(videoDataViewingConfuseEntity);
                    }
                    break;
                }
            }

        }

        // 统计每个用户需要消耗的资源
        if(updateList.size() > 0) {
            this.videoDataViewingConfuseService.updateBatchById(videoDataViewingConfuseEntities);

            // 统计每个用户需要消耗的资源
            List<UseDataViewingPropertyBo> useDataViewingPropertyBos = new LinkedList<>();
            for (VideoDataViewingConfuseEntity videoDataViewingConfuseEntity : videoDataViewingConfuseEntities) {

                // 删除数据看盘关联用户的昨日数据缓存
                this.redisTemplate.delete(wordsProperties.getYesterdayRecordRedisKey() +
                        videoDataViewingConfuseEntity.getTenantId() + ":" + videoDataViewingConfuseEntity.getUserId());

                boolean exist = false;
                for (UseDataViewingPropertyBo useDataViewingPropertyBo : useDataViewingPropertyBos) {
                    if(videoDataViewingConfuseEntity.getUserId().equals(useDataViewingPropertyBo.getUserId())) {
                        exist = true;
                        useDataViewingPropertyBo.setNumber(useDataViewingPropertyBo.getNumber() - 1);
                        break;
                    }
                }
                if(!exist) {
                    UseDataViewingPropertyBo useDataViewingPropertyBo = new UseDataViewingPropertyBo();
                    useDataViewingPropertyBo.setUserId(videoDataViewingConfuseEntity.getUserId());
                    useDataViewingPropertyBo.setNumber(-1L);
                    useDataViewingPropertyBos.add(useDataViewingPropertyBo);
                }
            }

            return useDataViewingPropertyBos;
        }

        return null;
    }

    /**
     * 根据主播号统计数据看板的数量
     * @param anchorNumber 主播号
     */
    private Long countAnchorViewingNum(String anchorNumber) {
        LambdaQueryWrapper<VideoDataViewingConfuseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingConfuseEntity::getAnchorNumber, anchorNumber);
        wrapper.eq(VideoDataViewingConfuseEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus());

        return this.videoDataViewingConfuseService.count(wrapper);
    }

    /**
     * 填充第三方数据平台数据
     * @param videoDataViewingEntity 数据看盘数据
     * @param liveRoomBo 第三方数据平台数据
     */
    private void fillDataViewingData(VideoDataViewingEntity videoDataViewingEntity, LiveRoomBo liveRoomBo) {
        videoDataViewingEntity.setTotalWatchNum(liveRoomBo.getTotalUser());
        videoDataViewingEntity.setAverageOnlineNum(liveRoomBo.getAverageUserCount());
        videoDataViewingEntity.setAverageResidenceTime(liveRoomBo.getAverageResidenceTime());
        videoDataViewingEntity.setIncrementFollowerCount(liveRoomBo.getLiveTrafficVo().getIncrementFollowerCount());
        videoDataViewingEntity.setConvertFanRate(liveRoomBo.getLiveTrafficVo().getConvertFanRate());
        videoDataViewingEntity.setInteractionPercent(liveRoomBo.getLiveTrafficVo().getInteractionPercent());

        // 设置销售额
        videoDataViewingEntity.setVolumeStart(0);
        videoDataViewingEntity.setVolumeEnd(0);
        List<LiveProductBo> liveProductVos = liveRoomBo.getLiveProductVos();
        if(liveProductVos != null && liveProductVos.size() > 0) {
            for (LiveProductBo liveProductVo : liveProductVos) {
                String amountText = liveProductVo.getAmountText();
                if(!StringUtils.isEmpty(amountText)) {
                    if("-".equals(amountText)) {
                        // 没有采集到数据
                        videoDataViewingEntity.setVolumeStart(-1);
                        videoDataViewingEntity.setVolumeEnd(-1);
                    }else {
                        if(amountText.contains("亿")) {
                            String[] arr = amountText.split("亿");
                            videoDataViewingEntity.setVolumeStart((int)(Double.parseDouble(arr[0]) * 100000000));
                            videoDataViewingEntity.setVolumeEnd((int)(Double.parseDouble(arr[0]) * 100000000));
                        }else {
                            String[] arr = amountText.split("~");
                            if(arr.length == 2) {
                                if(arr[0].contains("w") || arr[0].contains("W")) {
                                    arr[0] = arr[0].replaceAll("w", "").replaceAll("W", "");
                                    videoDataViewingEntity.setVolumeStart((int)(Double.parseDouble(arr[0]) * 10000));
                                }else {
                                    videoDataViewingEntity.setVolumeStart((int)Double.parseDouble(arr[0]));
                                }

                                if(arr[1].contains("w") || arr[1].contains("W")) {
                                    arr[1] = arr[1].replaceAll("w", "").replaceAll("W", "");
                                    videoDataViewingEntity.setVolumeEnd((int)(Double.parseDouble(arr[1]) * 10000));
                                }else {
                                    videoDataViewingEntity.setVolumeEnd((int)Double.parseDouble(arr[1]));
                                }
                            }
                        }
                    }

                }
            }
        }

        // 设置销量
        videoDataViewingEntity.setPurchaseCountStart(0);
        videoDataViewingEntity.setPurchaseCountEnd(0);
        if(liveProductVos != null && liveProductVos.size() > 0) {
            for (LiveProductBo liveProductVo : liveProductVos) {
                String volumeText = liveProductVo.getVolumeText();
                if(!StringUtils.isEmpty(volumeText)) {
                    if("-".equals(volumeText)) {
                        // 没有采集到数据
                        videoDataViewingEntity.setPurchaseCountStart(-1);
                        videoDataViewingEntity.setPurchaseCountEnd(-1);
                    }else {
                        String[] arr = volumeText.split("~");
                        if(arr.length == 2) {
                            if(arr[0].contains("w") || arr[0].contains("W")) {
                                arr[0] = arr[0].replaceAll("w", "").replaceAll("W", "");
                                videoDataViewingEntity.setPurchaseCountStart((int)(Double.parseDouble(arr[0]) * 10000));
                            }else {
                                videoDataViewingEntity.setPurchaseCountStart((int)Double.parseDouble(arr[0]));
                            }

                            if(arr[1].contains("w") || arr[1].contains("W")) {
                                arr[1] = arr[1].replaceAll("w", "").replaceAll("W", "");
                                videoDataViewingEntity.setPurchaseCountEnd((int)(Double.parseDouble(arr[1]) * 10000));
                            }else {
                                videoDataViewingEntity.setPurchaseCountEnd((int)Double.parseDouble(arr[1]));
                            }
                        }
                    }
                }
            }
        }

        // 设置客单价 = 销售额 / 销量
        videoDataViewingEntity.setCustomerUnitPriceStart(0.0);
        videoDataViewingEntity.setCustomerUnitPriceEnd(0.0);
        if(liveProductVos != null && liveProductVos.size() > 0) {
            for (LiveProductBo liveProductVo : liveProductVos) {
                String perOrderAmountText = liveProductVo.getPerOrderAmountText();
                if(!StringUtils.isEmpty(perOrderAmountText)) {
                    if("-".equals(perOrderAmountText)) {
                        // 没有采集到数据
                        videoDataViewingEntity.setCustomerUnitPriceStart(-1.0);
                        videoDataViewingEntity.setCustomerUnitPriceEnd(-1.0);
                    }else {
                        String[] arr = perOrderAmountText.split("~");
                        if(arr.length == 2) {
                            if(arr[0].contains("w") || arr[0].contains("W")) {
                                arr[0] = arr[0].replaceAll("w", "").replaceAll("W", "");
                                videoDataViewingEntity.setCustomerUnitPriceStart((Double.parseDouble(arr[0]) * 10000));
                            }else {
                                videoDataViewingEntity.setCustomerUnitPriceStart(Double.parseDouble(arr[0]));
                            }

                            if(arr[1].contains("w") || arr[1].contains("W")) {
                                arr[1] = arr[1].replaceAll("w", "").replaceAll("W", "");
                                videoDataViewingEntity.setCustomerUnitPriceEnd((Double.parseDouble(arr[1]) * 10000));
                            }else {
                                videoDataViewingEntity.setCustomerUnitPriceEnd(Double.parseDouble(arr[1]));
                            }
                        }
                    }
                }
            }
        }
//        if(videoDataViewingEntity.getPurchaseCountStart() != -1) {
//            videoDataViewingEntity.setCustomerUnitPriceStart(0.0);
//            videoDataViewingEntity.setCustomerUnitPriceEnd(0.0);
//        }else {
//            videoDataViewingEntity.setCustomerUnitPriceStart(-1.0);
//            videoDataViewingEntity.setCustomerUnitPriceEnd(-1.0);
//        }
//        if(videoDataViewingEntity.getPurchaseCountStart() > 0 && videoDataViewingEntity.getPurchaseCountEnd() > 0) {
//            double a = (double)videoDataViewingEntity.getVolumeStart() / videoDataViewingEntity.getPurchaseCountStart();
//            double b = (double)videoDataViewingEntity.getVolumeEnd() / videoDataViewingEntity.getPurchaseCountEnd();
//
//            videoDataViewingEntity.setCustomerUnitPriceStart(Math.min(a, b));
//            videoDataViewingEntity.setCustomerUnitPriceEnd(Math.max(a, b));
//        }

        // 设置UV价值
        videoDataViewingEntity.setUvValueStart(0.0);
        videoDataViewingEntity.setUvValueEnd(0.0);
        if(videoDataViewingEntity.getVolumeStart() != -1 && videoDataViewingEntity.getTotalWatchNum() > 0) {
            videoDataViewingEntity.setUvValueStart((double)videoDataViewingEntity.getVolumeStart() / videoDataViewingEntity.getTotalWatchNum());
            videoDataViewingEntity.setUvValueEnd((double)videoDataViewingEntity.getVolumeEnd() / videoDataViewingEntity.getTotalWatchNum());
        }

        // 设置带货转化率
        videoDataViewingEntity.setGoodsConvertRateStart(0.0);
        videoDataViewingEntity.setGoodsConvertRateEnd(0.0);
        if(videoDataViewingEntity.getPurchaseCountStart() != -1 && videoDataViewingEntity.getTotalWatchNum() > 0) {
            videoDataViewingEntity.setGoodsConvertRateStart((double)videoDataViewingEntity.getPurchaseCountStart() / videoDataViewingEntity.getTotalWatchNum());
            videoDataViewingEntity.setGoodsConvertRateEnd((double)videoDataViewingEntity.getPurchaseCountEnd() / videoDataViewingEntity.getTotalWatchNum());
        }

    }

    /**
     * 封装互动率
     * @param videoDataViewingConfuseEntity 数据看盘数据
     * @return
     */
    private void packageInteractionPercent(VideoDataViewingConfuseEntity videoDataViewingConfuseEntity) {
        if(videoDataViewingConfuseEntity != null) {
            if(videoDataViewingConfuseEntity.getInteractionPercent() == null || videoDataViewingConfuseEntity.getInteractionPercent() <= 0) {
                // 互动率为0，判断有没有弹幕，有弹幕则用弹幕/观看人次=互动率
                if(videoDataViewingConfuseEntity.getTotalWatchNum() != null && videoDataViewingConfuseEntity.getTotalWatchNum() > 0) {
                    // 获取弹幕
                    SocketCollectMessageEntity socketCollectMessageEntity = socketCollectMessageService.getOne(new LambdaQueryWrapper<SocketCollectMessageEntity>()
                            .eq(SocketCollectMessageEntity::getBatchNumber, videoDataViewingConfuseEntity.getBatchNumber())
                            .eq(SocketCollectMessageEntity::getUserId, videoDataViewingConfuseEntity.getUserId())
                            .eq(SocketCollectMessageEntity::getVideoId, videoDataViewingConfuseEntity.getVideoId())
                    );

                    if(socketCollectMessageEntity != null && socketCollectMessageEntity.getTotalBarrageNum() > 0) {
                        videoDataViewingConfuseEntity.setInteractionPercent(((double)socketCollectMessageEntity.getTotalBarrageNum() / videoDataViewingConfuseEntity.getTotalWatchNum()) * 100);
                        if(videoDataViewingConfuseEntity.getTotalWatchNum() > 50000) {
                            // 超过五万人观看，将互动率乘以倍率
                            double magnification = 10;
                            SystemKvEntity systemKvEntity = systemKvService.getByKey("interaction_magnification");
                            if(systemKvEntity != null) {
                                try {
                                    magnification = Double.parseDouble(systemKvEntity.getKvValue());
                                }catch (Exception e) {
                                    log.info("互动率倍率转成double失败=={}", systemKvEntity.getKvValue());
                                    e.printStackTrace();
                                }
                            }
                            videoDataViewingConfuseEntity.setInteractionPercent(videoDataViewingConfuseEntity.getInteractionPercent() * magnification);
                        }
                        // 如果大于9.8，随机取9.5-9.8
                        if(videoDataViewingConfuseEntity.getInteractionPercent() > 9.8) {
                            videoDataViewingConfuseEntity.setInteractionPercent(Math.random() * 0.3 + 9.5);
                        }
                    }
                }
            }
        }
    }

    /**
     * 混淆数据
     * @param videoDataViewingEntity 源数据
     * @param videoDataViewingConfuseEntity 混淆后的数据
     */
    private void confuseData(VideoDataViewingEntity videoDataViewingEntity, VideoDataViewingConfuseEntity videoDataViewingConfuseEntity) {

        videoDataViewingConfuseEntity.setTotalWatchNum(videoDataViewingEntity.getTotalWatchNumConfuse());
        videoDataViewingConfuseEntity.setAverageOnlineNum(videoDataViewingEntity.getAverageOnlineNumConfuse());
        videoDataViewingConfuseEntity.setAverageResidenceTime(videoDataViewingEntity.getAverageResidenceTimeConfuse());
        videoDataViewingConfuseEntity.setIncrementFollowerCount(videoDataViewingEntity.getIncrementFollowerCountConfuse());
        videoDataViewingConfuseEntity.setConvertFanRate(videoDataViewingEntity.getConvertFanRateConfuse());
        videoDataViewingConfuseEntity.setInteractionPercent(videoDataViewingEntity.getInteractionPercentConfuse());
        videoDataViewingConfuseEntity.setVolumeStart(videoDataViewingEntity.getVolumeStartConfuse());
        videoDataViewingConfuseEntity.setVolumeEnd(videoDataViewingEntity.getVolumeEndConfuse());
        videoDataViewingConfuseEntity.setPurchaseCountStart(videoDataViewingEntity.getPurchaseCountStartConfuse());
        videoDataViewingConfuseEntity.setPurchaseCountEnd(videoDataViewingEntity.getPurchaseCountEndConfuse());
        videoDataViewingConfuseEntity.setCustomerUnitPriceStart(videoDataViewingEntity.getCustomerUnitPriceStartConfuse());
        videoDataViewingConfuseEntity.setCustomerUnitPriceEnd(videoDataViewingEntity.getCustomerUnitPriceEndConfuse());
        videoDataViewingConfuseEntity.setUvValueStart(videoDataViewingEntity.getUvValueStartConfuse());
        videoDataViewingConfuseEntity.setUvValueEnd(videoDataViewingEntity.getUvValueEndConfuse());
        videoDataViewingConfuseEntity.setGoodsConvertRateStart(videoDataViewingEntity.getGoodsConvertRateStartConfuse());
        videoDataViewingConfuseEntity.setGoodsConvertRateEnd(videoDataViewingEntity.getGoodsConvertRateEndConfuse());
        // 封装互动率
        packageInteractionPercent(videoDataViewingConfuseEntity);

        // 用户画像和流量结构
        videoDataViewingConfuseEntity.setWatchFlowList(videoDataViewingEntity.getWatchFlowList());
        videoDataViewingConfuseEntity.setWatchUserPortrait(videoDataViewingEntity.getWatchUserPortrait());

//        // 设置混淆比例
//        double totalWatchNumRatio = 1.0;
//        double averageOnlineNumRatio = 1.0;
//        double averageResidenceTimeRatio = 1.0;
//        double incrementFollowerCountRatio = 1.0;
//        double purchaseCountRatio = 1.0;
//        double volumeRatio = 1.0;
//
//        List<VideoDataViewingRatioEntity> videoDataViewingRatioEntities = this.videoDataViewingRatioService.list();
//        if(videoDataViewingRatioEntities != null) {
//            Map<String, VideoDataViewingRatioEntity> ratioMap = videoDataViewingRatioEntities.stream().collect(Collectors.toMap(VideoDataViewingRatioEntity::getRatioCode, item -> item));
//
//            totalWatchNumRatio = ratioMap.get("total_watch_num") == null ? 1.0 : computeRatio(ratioMap.get("total_watch_num").getRatioStart(), ratioMap.get("total_watch_num").getRatioEnd());
//            averageOnlineNumRatio = ratioMap.get("average_online_num") == null ? 1.0 : computeRatio(ratioMap.get("average_online_num").getRatioStart(), ratioMap.get("average_online_num").getRatioEnd());
//            averageResidenceTimeRatio = ratioMap.get("average_residence_time") == null ? 1.0 : computeRatio(ratioMap.get("average_residence_time").getRatioStart(), ratioMap.get("average_residence_time").getRatioEnd());
//            incrementFollowerCountRatio = ratioMap.get("increment_follower_count") == null ? 1.0 : computeRatio(ratioMap.get("increment_follower_count").getRatioStart(), ratioMap.get("increment_follower_count").getRatioEnd());
//            purchaseCountRatio = ratioMap.get("purchase_count") == null ? 1.0 : computeRatio(ratioMap.get("purchase_count").getRatioStart(), ratioMap.get("purchase_count").getRatioEnd());
//            volumeRatio = ratioMap.get("volume") == null ? 1.0 : computeRatio(ratioMap.get("volume").getRatioStart(), ratioMap.get("volume").getRatioEnd());
//        }
//
//        videoDataViewingConfuseEntity.setTotalWatchNum((int) (videoDataViewingEntity.getTotalWatchNum() * totalWatchNumRatio));
//        videoDataViewingConfuseEntity.setAverageOnlineNum((int) (videoDataViewingEntity.getAverageOnlineNum() * averageOnlineNumRatio));
//        videoDataViewingConfuseEntity.setAverageResidenceTime((int) (videoDataViewingEntity.getAverageResidenceTime() * averageResidenceTimeRatio));
//        videoDataViewingConfuseEntity.setIncrementFollowerCount((int) (videoDataViewingEntity.getIncrementFollowerCount() * incrementFollowerCountRatio));
//        if(videoDataViewingConfuseEntity.getTotalWatchNum() > 0) {
//            videoDataViewingConfuseEntity.setConvertFanRate((double) videoDataViewingConfuseEntity.getIncrementFollowerCount() / videoDataViewingConfuseEntity.getTotalWatchNum());
//        }else {
//            videoDataViewingConfuseEntity.setConvertFanRate(0.0);
//        }
//        videoDataViewingConfuseEntity.setInteractionPercent(videoDataViewingEntity.getInteractionPercent());
//
//        // 混淆销售额
//        if(videoDataViewingEntity.getVolumeStart() != -1) {
//            videoDataViewingConfuseEntity.setVolumeStart((int)(videoDataViewingEntity.getVolumeStart() * volumeRatio));
//            videoDataViewingConfuseEntity.setVolumeEnd((int)(videoDataViewingEntity.getVolumeEnd() * volumeRatio));
//        }else {
//            videoDataViewingConfuseEntity.setVolumeStart(-1);
//            videoDataViewingConfuseEntity.setVolumeEnd(-1);
//        }
//
//
//        // 混淆销量
//        if(videoDataViewingEntity.getPurchaseCountStart() != -1) {
//            videoDataViewingConfuseEntity.setPurchaseCountStart((int)(videoDataViewingEntity.getPurchaseCountStart() * purchaseCountRatio));
//            videoDataViewingConfuseEntity.setPurchaseCountEnd((int)(videoDataViewingEntity.getPurchaseCountEnd() * purchaseCountRatio));
//        }else {
//            videoDataViewingConfuseEntity.setPurchaseCountStart(-1);
//            videoDataViewingConfuseEntity.setPurchaseCountEnd(-1);
//        }
//
//
//        // 设置客单价
//        videoDataViewingConfuseEntity.setCustomerUnitPriceStart(videoDataViewingEntity.getCustomerUnitPriceStart());
//        videoDataViewingConfuseEntity.setCustomerUnitPriceEnd(videoDataViewingEntity.getCustomerUnitPriceEnd());
//
//        // 设置uv价值
//        if(videoDataViewingConfuseEntity.getVolumeStart() != -1) {
//            videoDataViewingConfuseEntity.setUvValueStart(0.0);
//            videoDataViewingConfuseEntity.setUvValueEnd(0.0);
//            if(videoDataViewingConfuseEntity.getTotalWatchNum() > 0) {
//                videoDataViewingConfuseEntity.setUvValueStart((double)videoDataViewingConfuseEntity.getVolumeStart() / videoDataViewingConfuseEntity.getTotalWatchNum());
//                videoDataViewingConfuseEntity.setUvValueEnd((double)videoDataViewingConfuseEntity.getVolumeEnd() / videoDataViewingConfuseEntity.getTotalWatchNum());
//            }
//        }else {
//            videoDataViewingConfuseEntity.setUvValueStart(-1.0);
//            videoDataViewingConfuseEntity.setUvValueEnd(-1.0);
//        }
//
//        // 设置带货转换率
//        if(videoDataViewingConfuseEntity.getPurchaseCountStart() != -1) {
//            videoDataViewingConfuseEntity.setGoodsConvertRateStart(0.0);
//            videoDataViewingConfuseEntity.setGoodsConvertRateEnd(0.0);
//            if(videoDataViewingConfuseEntity.getTotalWatchNum() > 0) {
//                videoDataViewingConfuseEntity.setGoodsConvertRateStart((double)videoDataViewingConfuseEntity.getPurchaseCountStart() / videoDataViewingConfuseEntity.getTotalWatchNum());
//                videoDataViewingConfuseEntity.setGoodsConvertRateEnd((double)videoDataViewingConfuseEntity.getPurchaseCountEnd() / videoDataViewingConfuseEntity.getTotalWatchNum());
//
//            }
//        }else {
//            videoDataViewingConfuseEntity.setGoodsConvertRateStart(-1.0);
//            videoDataViewingConfuseEntity.setGoodsConvertRateEnd(-1.0);
//        }

    }

    /**
     * 计算比例值
     * @param startRatio 比例区间-起始值
     * @param endRatio 比例区间-结束值
     * @return
     */
    private double computeRatio(double startRatio, double endRatio) {

        return (startRatio + Math.random() * (endRatio - startRatio));
    }

}

