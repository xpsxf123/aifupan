package com.jiuyu.replay.api.logic.words.impl;

import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.api.logic.words.VideoDataViewingLogic;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.bll.SystemConfigBll;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaQueryBo;
import com.jiuyu.replay.third.chanmama.ThirdDataUtils;
import com.jiuyu.replay.third.constant.ChanmamaProperties;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.viewing.*;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.vo.SocketCollectMessageInfoVo;
import com.jiuyu.replay.words.vo.viewing.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
public class VideoDataViewingLogicImpl implements VideoDataViewingLogic {

    @Resource
    private VideoDataViewingBll videoDataViewingBll;
    @Resource
    private UserPropertyBll userPropertyBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private AnchorUrlBll anchorUrlBll;
    @Resource
    private ThirdDataUtils chanmamaUtils;
    @Resource
    private VideoDataViewingConfuseBll videoDataViewingConfuseBll;
    @Resource
    private ChanmamaProperties chanmamaProperties;
    @Resource
    private ChanmamaSendRecordBll chanmamaSendRecordBll;
    @Resource
    private UserBll userBll;
    @Resource
    private SystemConfigBll systemConfigBll;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SocketCollectMessageBll socketCollectMessageBll;
    @Resource
    private SystemKvBll systemKvBll;
    @Resource
    private WordsProperties wordsProperties;


    @Override
    public R<PageUtils<VideoDataViewingListVo>> queryPage(VideoDataViewingListBo videoDataViewingListBo) {

        return videoDataViewingBll.queryPage(videoDataViewingListBo);
    }

    @Override
    public R<VideoDataViewingInfoVo> info(Long id) {

        return videoDataViewingBll.info(id);
    }

    @Override
    public R<String> save(VideoDataViewingBo videoDataViewingBo) {

        return videoDataViewingBll.save(videoDataViewingBo);
    }

    @Override
    public R<String> update(VideoDataViewingBo videoDataViewingBo) {

        return videoDataViewingBll.update(videoDataViewingBo);
    }

    @Override
    public R<String> delete(Long id) {

        return videoDataViewingBll.delete(id);
    }

    @Override
    @CustomRedissonLock(key = "'create_viewing_lock:' + #args[0]")
    @Transactional(rollbackFor = Exception.class)
    public R<String> createDataViewing(String videoId, Integer isAuto, Integer anchorOnlineStatus) {

        if(isAuto == null) {
            isAuto = 0;
        }
        if(anchorOnlineStatus == null) {
            anchorOnlineStatus = 0;
        }

        UserCacheVo user = GlobalObject.getLocalUser();

        // 获取视频信息
        R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.clientGetVideoByVideoId(videoId, null, user.getActiveTenantId());
        if(anchorVideoInfoVoR.getCode() != 0 || anchorVideoInfoVoR.getData() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
        }
        AnchorVideoInfoVo videoInfo = anchorVideoInfoVoR.getData();

        // 只允许操作当前租户下的数据
        if(!user.getActiveTenantId().equals(videoInfo.getTenantId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有操作权限");
        }

        // 校验视频开始录制时间是否超过72小时
        long hours72InMillis = 72L * 60 * 60 * 1000;
        long timeDifference = System.currentTimeMillis() - videoInfo.getStartTime().getTime();
        if(timeDifference > hours72InMillis) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频开始录制时间已超过72小时，无法生成看盘数据");
        }

        String batchNumber = videoInfo.getBatchNumber().toString();
        // 获取主播信息
        R<AnchorUrlInfoVo> anchorUrlInfoVoR = this.anchorUrlBll.infoByCondition(videoInfo.getSecUid(), null, null);
        if(anchorUrlInfoVoR.getCode() != 0 || anchorUrlInfoVoR.getData() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播信息不存在");
        }
        AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlInfoVoR.getData();

//        // 查询资产是否足够
//        UserPropertyTypeInfoVo userPropertyTypeInfoVo = userPropertyBll.getUserPropertyByCode(user.getId(), "dataBoardNum");
//        long surplus = userPropertyTypeInfoVo.getTotalQuantity() - userPropertyTypeInfoVo.getUseQuantity();
//        if(surplus <= 0) {
//            // 创建一条视频时长的不发请求的记录
//            this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), null, 5, batchNumber);
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "资源不足，请充值");
//        }

        // 获取系统配置
        R<SystemConfigInfoVo> systemConfigInfoVoR = systemConfigBll.info(1L);
        SystemConfigInfoVo systemConfigInfoVo = systemConfigInfoVoR.getData();

        // 查询是否有正在查询的记录
        R<VideoDataViewingConfuseInfoVo> videoDataViewingConfuseInfoVoR = this.videoDataViewingConfuseBll.getByVideoId(videoId);
        if(videoDataViewingConfuseInfoVoR.getCode().equals(StatusCode.SUCCESS.getCode()) && videoDataViewingConfuseInfoVoR.getData() != null) {

            VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo = videoDataViewingConfuseInfoVoR.getData();
            if(dataViewingConfuseInfoVo.getDataStatus().equals(DataViewingStatusEnum.PULLING.getStatus())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "正在获取数据中，请等待");
            }else if(dataViewingConfuseInfoVo.getDataStatus().equals(DataViewingStatusEnum.PULL_SUCCESS.getStatus())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "已存在数据，拉取失败");
            }else {
                // 删除旧纪录
                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
            }
//            if(dataViewingConfuseInfoVo.getDataStatus() == 0) {
//                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "正在获取数据中，请等待");
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 1) {
//                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "已存在数据，拉取失败");
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 2) {
//                // 存在拉取失败的记录，删除旧记录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 3) {
//                // 存在未收录主播的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 4) {
//                // 存在自动生成但是视频没有超过50分钟的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 5) {
//                // 存在资源不足的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 6) {
//                // 存在主播未下播的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 7) {
//                // 存在主播收录但直播记录为空的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }
        }

        // 查询同租户下有没有对应的巨量百应数据，如果有就直接创建数据看盘
        R<Boolean> jlbyExistR = this.videoDataViewingBll.checkJlbyDataExistCreate(videoId);
        if(jlbyExistR.getCode() == StatusCode.SUCCESS.getCode() && jlbyExistR.getData()) {
            // 删除数据看盘关联用户的昨日数据缓存
            this.redisTemplate.delete(wordsProperties.getYesterdayRecordRedisKey() + user.getActiveTenantId() + ":" + user.getId());
            return R.ok("操作成功");
        }

        // 是自动发起的请求
        if(isAuto == 1) {
            // 如果主播未下播，创建一条不发请求的记录
            if(anchorOnlineStatus == 0) {
                this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), null, 6, batchNumber);
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播未下播");
            }

            // 判断视频时长是否大于50分钟
            int duration = 3000;
            if(systemConfigInfoVo != null) {
                duration = systemConfigInfoVo.getAutoCreateDataViewingSecond();
            }
            if(videoInfo.getDuration() < duration) {
                // 创建一条视频时长不足的不发请求的记录
                this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), null, 4, batchNumber);
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频时长不足50分钟");
            }
        }

        // 查询本地数据库有没有对应的第三方数据，如果有就直接创建数据看盘
        R<Boolean> localDBExistR = this.videoDataViewingBll.checkLocalDBExistCreate(videoId, user.getId(), user.getActiveTenantId());
        if(localDBExistR.getCode() == 0 && localDBExistR.getData()) {

            // 删除数据看盘关联用户的昨日数据缓存
            this.redisTemplate.delete(wordsProperties.getYesterdayRecordRedisKey() + user.getActiveTenantId() + ":" + user.getId());

            // 使用数据看盘资产
            AssetsMinusOrPlusBo assetsMinusOrPlusBo = new AssetsMinusOrPlusBo();
            assetsMinusOrPlusBo.setUserId(user.getId());
            assetsMinusOrPlusBo.setUserName(user.getNickName());
            assetsMinusOrPlusBo.setCode("dataBoardNum");
            assetsMinusOrPlusBo.setNum(-1L);
            UserPropertyImpl.use(assetsMinusOrPlusBo);

            return R.ok("操作成功");
        }

        // 判断当前主播场次3分钟内是否已经发送过第三方数据平台请求
        if(isAuto == 1) {
            Object obj = redisTemplate.opsForValue().get(chanmamaProperties.getAnchorRedisKey() + anchorUrlInfoVo.getAnchorNumber() + "_" + batchNumber);
            if(obj != null) {
                // 创建一条不需要发送请求的记录
                String requestId = null;
                String objStr = (String) obj;
                try {
                    String[] strArr = objStr.split("_");

                    if(strArr.length >= 3) {
                        requestId = strArr[2];
                    }
                }catch (Exception e) {
                    log.info("当前主播场次3分钟内已经发送过第三方数据平台请求，解析requestId时出现异常：{}", objStr);
                }

                this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), requestId, 0, batchNumber);
                return R.ok("操作成功");
            }
        }


        // ==发送第三方数据平台查询请求==

        // 构建查询参数
        ChanmamaQueryBo chanmamaQueryBo = new ChanmamaQueryBo();
        chanmamaQueryBo.setLiveName(anchorUrlInfoVo.getAnchorNumber());
        chanmamaQueryBo.setSecUid(anchorUrlInfoVo.getSecUid());

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 查询开始时间
        long startTimeStamp = videoInfo.getStartTime().getTime();
        // 往后推一分钟
        int startTimeDelayed = 60 * 1000;
        R<SystemKvInfoVo> delayedR = systemKvBll.getByKey("chanmama_query_start_time_delayed");
        if(delayedR.getCode() == 0 && delayedR.getData() != null) {
            SystemKvInfoVo systemKvInfoVo = delayedR.getData();
            try {
                startTimeDelayed = Integer.parseInt(systemKvInfoVo.getKvValue()) * 1000;
            }catch (Exception e) {
                log.warn("查询第三方平台数据请求===系统键值对转换异常：{}", JSON.toJSONString(systemKvInfoVo));
            }
        }
        startTimeStamp += startTimeDelayed;
        LocalDateTime startLocalDate = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(startTimeStamp),  // 通过时间戳创建 Instant
                ZoneId.systemDefault()            // 使用系统默认时区
        );

        // 查询结束时间
        long endTimeStamp = videoInfo.getEndTime().getTime();
        long endDifference = 180 * 1000;
        if(systemConfigInfoVo != null) {
            endDifference = systemConfigInfoVo.getDataViewingTimeDifference();
        }
        endTimeStamp = endTimeStamp - endDifference;
        if(endTimeStamp < startTimeStamp) {
            endTimeStamp = startTimeStamp;
        }
        LocalDateTime endLocalDate = Instant.ofEpochMilli(endTimeStamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

        chanmamaQueryBo.setBeginDate(startLocalDate.format(dateTimeFormatter));
        chanmamaQueryBo.setEndDate(endLocalDate.format(dateTimeFormatter));
        chanmamaQueryBo.setCallBack(chanmamaProperties.getCallbackUrl());
        chanmamaQueryBo.setRequestId(UUID.randomUUID().toString().replaceAll("-", ""));

        // 保存数据看盘记录到数据库
        R<String> resultR = this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), chanmamaQueryBo.getRequestId(), 0, batchNumber);
        if(resultR.getCode() == 0) {
            // 保存发送记录到redis
            int second = 180;
            if(systemConfigInfoVo != null) {
                second = systemConfigInfoVo.getDataViewingTimeSend();
            }
            redisTemplate.opsForValue().set(
                    chanmamaProperties.getAnchorRedisKey() + anchorUrlInfoVo.getAnchorNumber() + "_" + batchNumber,
                    anchorUrlInfoVo.getAnchorNumber() + "_" + batchNumber + "_" + chanmamaQueryBo.getRequestId(),
                    Duration.ofSeconds(second));

            // 构建查询记录
            ChanmamaSendRecordBo chanmamaSendRecordBo = new ChanmamaSendRecordBo();
            chanmamaSendRecordBo.setUserId(user.getId());
            chanmamaSendRecordBo.setTenantId(user.getActiveTenantId());
            chanmamaSendRecordBo.setVideoId(videoId);
            chanmamaSendRecordBo.setAnchorNumber(anchorUrlInfoVo.getAnchorNumber());
            chanmamaSendRecordBo.setRequestId(chanmamaQueryBo.getRequestId());
            chanmamaSendRecordBo.setRequestBody(JSON.toJSONString(chanmamaQueryBo));
            chanmamaSendRecordBo.setBatchNumber(batchNumber);

            // 缓存起来，10分钟后再发送
            DelaySendChanmamaBo delaySendChanmamaBo = new DelaySendChanmamaBo();
            delaySendChanmamaBo.setChanmamaQueryBoJson(JSON.toJSONString(chanmamaQueryBo));
            delaySendChanmamaBo.setChanmamaSendRecordBoJson(JSON.toJSONString(chanmamaSendRecordBo));
            delaySendChanmamaBo.setCreateTime(new Date().getTime());

            this.redisTemplate.opsForHash().put("replay:chanmama:send", chanmamaQueryBo.getRequestId(), JSON.toJSONString(delaySendChanmamaBo));
            this.redisTemplate.expire("replay:chanmama:send", Duration.ofMinutes(60));

            return resultR;
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前服务器繁忙，请稍后再试");

    }

    /**
     * 延迟10分钟发送第三方数据平台查询
     * @param chanmamaQueryBo 查询请求数据
     * @param chanmamaSendRecordBo 查询记录数据
     */
    private void delaySendChanmama(ChanmamaQueryBo chanmamaQueryBo, ChanmamaSendRecordBo chanmamaSendRecordBo) {

        log.info("==发起查询start=={}", chanmamaQueryBo.getRequestId());
        try {
            Thread.sleep(10 * 60 * 1000);
        }catch (Exception e) {
            log.error("延迟3分钟发送第三方数据平台查询==sleep出错");
            e.printStackTrace();
        }

        // 发起查询
        R<String> queryR = chanmamaUtils.queryData(chanmamaQueryBo);
        log.info("==发起查询end=={}", chanmamaQueryBo.getRequestId());

        // 保存查询记录
        chanmamaSendRecordBo.setResponseStatus(queryR.getCode().toString());
        chanmamaSendRecordBo.setResponseBody(queryR.getData());
        this.chanmamaSendRecordBll.save(chanmamaSendRecordBo);
    }

//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    @CustomRedissonLock(key = "'create_viewing_lock:' + #args[0]")
//    public R<String> createDataViewing(String videoId, Integer isAuto) {
//
//        if(isAuto == null) {
//            isAuto = 0;
//        }
//
//        UserCacheVo user = GlobalObject.getLocalUser();
//
//        // 获取视频信息
//        R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.clientGetVideoByVideoId(videoId, null, user.getActiveTenantId());
//        if(anchorVideoInfoVoR.getCode() != 0 || anchorVideoInfoVoR.getData() == null) {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
//        }
//        AnchorVideoInfoVo videoInfo = anchorVideoInfoVoR.getData();
//
//        // 只允许操作当前租户下的数据
//        if(!user.getActiveTenantId().equals(videoInfo.getTenantId())) {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有操作权限");
//        }
//
//        String batchNumber = videoInfo.getBatchNumber().toString();
//        // 获取主播信息
//        R<AnchorUrlInfoVo> anchorUrlInfoVoR = this.anchorUrlBll.infoByCondition(videoInfo.getSecUid(), null, null);
//        if(anchorUrlInfoVoR.getCode() != 0 || anchorUrlInfoVoR.getData() == null) {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播信息不存在");
//        }
//        AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlInfoVoR.getData();
//
//        // 查询资产是否足够
//        UserPropertyTypeInfoVo userPropertyTypeInfoVo = userPropertyBll.getUserPropertyByCode(user.getId(), "dataBoardNum");
//        long surplus = userPropertyTypeInfoVo.getTotalQuantity() - userPropertyTypeInfoVo.getUseQuantity();
//        if(surplus <= 0) {
//            // 创建一条视频时长的不发请求的记录
//            this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), null, 5, batchNumber);
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "资源不足，请充值");
//        }
//
//        // 获取系统配置
//        R<SystemConfigInfoVo> systemConfigInfoVoR = systemConfigBll.info(1L);
//        SystemConfigInfoVo systemConfigInfoVo = systemConfigInfoVoR.getData();
//
//        // 查询是否有正在查询的记录
//        R<VideoDataViewingConfuseInfoVo> videoDataViewingConfuseInfoVoR = this.videoDataViewingConfuseBll.getByVideoId(videoId);
//        if(videoDataViewingConfuseInfoVoR.getCode() == 0 && videoDataViewingConfuseInfoVoR.getData() != null) {
//
//            VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo = videoDataViewingConfuseInfoVoR.getData();
//            if(dataViewingConfuseInfoVo.getDataStatus() == 0) {
//                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "正在获取数据中，请等待");
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 1) {
//                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "已存在数据，拉取失败");
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 2) {
//                // 存在拉取失败的记录，删除旧记录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 3) {
//                // 存在未收录主播的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 4) {
//                // 存在自动生成但是视频没有超过50分钟的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }else if(dataViewingConfuseInfoVo.getDataStatus() == 5) {
//                // 存在资源不足的记录，删除旧纪录
//                this.videoDataViewingConfuseBll.delete(dataViewingConfuseInfoVo.getId());
//            }
//        }
//
//        // 判断视频时长是否大于50分钟
//        if(isAuto == 1) {
//            int duration = 3000;
//            if(systemConfigInfoVo != null) {
//                duration = systemConfigInfoVo.getAutoCreateDataViewingSecond();
//            }
//            if(videoInfo.getDuration() < duration) {
//                // 创建一条视频时长不足的不发请求的记录
//                this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), null, 4, batchNumber);
//                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频时长不足50分钟");
//            }
//        }
//
//        // 查询本地数据库有没有对应的数据，如果有就直接创建数据看盘
//        R<Boolean> localDBExistR = this.videoDataViewingBll.checkLocalDBExistCreate(videoId, user.getId(), user.getActiveTenantId());
//        if(localDBExistR.getCode() == 0 && localDBExistR.getData()) {
//
//            // 使用数据看盘资产
//            AssetsMinusOrPlusBo assetsMinusOrPlusBo = new AssetsMinusOrPlusBo();
//            assetsMinusOrPlusBo.setUserId(user.getId());
//            assetsMinusOrPlusBo.setUserName(user.getNickName());
//            assetsMinusOrPlusBo.setCode("dataBoardNum");
//            assetsMinusOrPlusBo.setNum(-1L);
//            userProperty.use(assetsMinusOrPlusBo);
//
//            return R.ok("操作成功");
//        }
//
//        // 判断当前主播场次3分钟内是否已经发送过第三方数据平台请求
//        if(isAuto == 1) {
//            Object obj = redisTemplate.opsForValue().get(chanmamaProperties.getAnchorRedisKey() + anchorUrlInfoVo.getAnchorNumber() + "_" + batchNumber);
//            if(obj != null) {
//                // 创建一条不需要发送请求的记录
//                this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), null, 0, batchNumber);
//                return R.ok("操作成功");
//            }
//        }
//
//        // 发送第三方数据平台查询请求
//
//        // 构建查询参数
//        ChanmamaQueryBo chanmamaQueryBo = new ChanmamaQueryBo();
//        chanmamaQueryBo.setLiveName(anchorUrlInfoVo.getAnchorNumber());
//
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//        // 查询开始时间
//        long startTimeStamp = videoInfo.getStartTime().getTime();
//        LocalDateTime startLocalDate = videoInfo.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//
//        // 查询结束时间
//        long endTimeStamp = videoInfo.getEndTime().getTime();
//        long endDifference = 180 * 1000;
//        if(systemConfigInfoVo != null) {
//            endDifference = systemConfigInfoVo.getDataViewingTimeDifference();
//        }
//        endTimeStamp = endTimeStamp - endDifference;
//        if(endTimeStamp < startTimeStamp) {
//            endTimeStamp = startTimeStamp;
//        }
//        LocalDateTime endLocalDate = Instant.ofEpochMilli(endTimeStamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
//
//        chanmamaQueryBo.setBeginDate(startLocalDate.format(dateTimeFormatter));
//        chanmamaQueryBo.setEndDate(endLocalDate.format(dateTimeFormatter));
//        chanmamaQueryBo.setCallBack(chanmamaProperties.getCallbackUrl());
//        chanmamaQueryBo.setRequestId(UUID.randomUUID().toString().replaceAll("-", ""));
//
//        // 保存查询记录
//        ChanmamaSendRecordBo chanmamaSendRecordBo = new ChanmamaSendRecordBo();
//        chanmamaSendRecordBo.setUserId(user.getId());
//        chanmamaSendRecordBo.setTenantId(user.getActiveTenantId());
//        chanmamaSendRecordBo.setVideoId(videoId);
//        chanmamaSendRecordBo.setAnchorNumber(anchorUrlInfoVo.getAnchorNumber());
//        chanmamaSendRecordBo.setRequestId(chanmamaQueryBo.getRequestId());
//        chanmamaSendRecordBo.setRequestBody(JSON.toJSONString(chanmamaQueryBo));
//        chanmamaSendRecordBo.setBatchNumber(batchNumber);
//
//        // 发起查询
//        log.info("==发起查询start=={}", chanmamaQueryBo.getRequestId());
//        R<String> queryR = chanmamaUtils.queryData(chanmamaQueryBo);
//        log.info("==发起查询end=={}", chanmamaQueryBo.getRequestId());
//
//        // 保存查询记录
//        chanmamaSendRecordBo.setResponseStatus(queryR.getCode().toString());
//        chanmamaSendRecordBo.setResponseBody(queryR.getData());
//        this.chanmamaSendRecordBll.save(chanmamaSendRecordBo);
//
//        if(queryR.getCode() == 0) {
//            // 保存记录到数据库
//            R<String> resultR = this.videoDataViewingConfuseBll.createDataViewing(videoId, user.getId(), user.getActiveTenantId(), anchorUrlInfoVo.getAnchorNumber(), chanmamaQueryBo.getRequestId(), 0, batchNumber);
//            // 保存发送记录到redis
//            int second = 180;
//            if(systemConfigInfoVo != null) {
//                second = systemConfigInfoVo.getDataViewingTimeSend();
//            }
//            redisTemplate.opsForValue().set(
//                    chanmamaProperties.getAnchorRedisKey() + anchorUrlInfoVo.getAnchorNumber() + "_" + batchNumber,
//                    anchorUrlInfoVo.getAnchorNumber() + "_" + batchNumber,
//                    Duration.ofSeconds(second));
//
//            log.info("==创建第三方数据平台请求成功=={}", chanmamaQueryBo.getRequestId());
//
//            return resultR;
//        }else {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前服务器繁忙，请稍后再试");
//        }
//
//    }

    @Override
    @CustomRedissonLock(key = "'viewing_callback_lock:' + #args[0]?.requestId ?: 'null'")
    public R<String> handleDataViewingCallback(VideoDataViewingCallbackBo videoDataViewingCallbackBo) {

        // 处理回调记录
        R<ChanmamaSendRecordInfoVo> handleDataViewingCallbackR = this.videoDataViewingBll.handleDataViewingCallback(videoDataViewingCallbackBo);
        if(handleDataViewingCallbackR.getCode() != StatusCode.SUCCESS.getCode() || handleDataViewingCallbackR.getData() == null) {
            return R.error(handleDataViewingCallbackR.getCode(), handleDataViewingCallbackR.getMsg());
        }

        // 处理回调数据
        R<List<UseDataViewingPropertyBo>> UseDataViewingPropertyR = this.videoDataViewingBll.handleDataViewingCallbackData(videoDataViewingCallbackBo, handleDataViewingCallbackR.getData());

        // 消耗用户的资源
        if(UseDataViewingPropertyR.getCode() == 0) {
            List<UseDataViewingPropertyBo> useDataViewingPropertyBos = UseDataViewingPropertyR.getData();
            if(useDataViewingPropertyBos != null && useDataViewingPropertyBos.size() > 0) {
                // 获取用户列表
                List<Long> userIds = useDataViewingPropertyBos.stream().map(UseDataViewingPropertyBo::getUserId).collect(Collectors.toList());
                R<List<UserListVo>> userListR = this.userBll.listByIds(userIds);
                if(userListR.getCode() == 0) {
                    List<UserListVo> userList = userListR.getData();
                    if(userList != null && userList.size() > 0) {
                        for (UseDataViewingPropertyBo useDataViewingPropertyBo : useDataViewingPropertyBos) {
                            for (UserListVo user : userList) {
                                if(useDataViewingPropertyBo.getUserId().equals(user.getId())) {
                                    // 使用资产
                                    try {
                                        AssetsMinusOrPlusBo assetsMinusOrPlusBo = new AssetsMinusOrPlusBo();
                                        assetsMinusOrPlusBo.setUserId(user.getId());
                                        assetsMinusOrPlusBo.setUserName(user.getNickName());
                                        assetsMinusOrPlusBo.setCode("dataBoardNum");
                                        assetsMinusOrPlusBo.setNum(useDataViewingPropertyBo.getNumber());
                                        UserPropertyImpl.use(assetsMinusOrPlusBo);
                                    }catch (Exception e) {
                                        log.warn("数据看板使用资源时发生异常", e);
                                    }

                                    break;
                                }
                            }
                        }
                    }
                }
            }

            return R.ok("接收成功", videoDataViewingCallbackBo.getRequestId());
        }

        return R.error(handleDataViewingCallbackR.getCode(), handleDataViewingCallbackR.getMsg());

    }

    @Override
    public R<VideoDataViewingConfuseInfoVo> infoByVideoId(String videoId) {

        return this.videoDataViewingBll.infoByVideoId(videoId);

//        R<VideoDataViewingConfuseInfoVo> videoDataViewingConfuseInfoVoR = this.videoDataViewingBll.infoByVideoId(videoId);
//        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = videoDataViewingConfuseInfoVoR.getData();
//        // 封装互动率
//        packageInteractionPercent(videoDataViewingConfuseInfoVo);
//
//        return videoDataViewingConfuseInfoVoR;
    }

    /**
     * 封装互动率
     * @param videoDataViewingConfuseInfoVo 数据看板对象
     */
    private void packageInteractionPercent(VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo) {
        if(videoDataViewingConfuseInfoVo != null) {
            if(videoDataViewingConfuseInfoVo.getInteractionPercent() == null || videoDataViewingConfuseInfoVo.getInteractionPercent() <= 0) {
                // 互动率为0，判断有没有弹幕，有弹幕则用弹幕/观看人次=互动率
                if(videoDataViewingConfuseInfoVo.getTotalWatchNum() != null && videoDataViewingConfuseInfoVo.getTotalWatchNum() > 0) {
                    R<SocketCollectMessageInfoVo> socketCollectMessageInfoVoR = this.socketCollectMessageBll.getByBatch(videoDataViewingConfuseInfoVo.getBatchNumber(),
                            videoDataViewingConfuseInfoVo.getUserId(),
                            videoDataViewingConfuseInfoVo.getVideoId());
                    SocketCollectMessageInfoVo socketCollectMessageInfoVo = socketCollectMessageInfoVoR.getData();
                    if(socketCollectMessageInfoVo != null && socketCollectMessageInfoVo.getTotalBarrageNum() > 0) {
                        videoDataViewingConfuseInfoVo.setInteractionPercent(((double)socketCollectMessageInfoVo.getTotalBarrageNum() / videoDataViewingConfuseInfoVo.getTotalWatchNum()) * 100);
                        if(videoDataViewingConfuseInfoVo.getTotalWatchNum() > 50000) {
                            // 超过五万人观看，将互动率乘以倍率
                            double magnification = 10;
                            R<SystemKvInfoVo> kvInfoVoR = systemKvBll.getByKey("interaction_magnification");
                            if(kvInfoVoR != null && kvInfoVoR.getData() != null) {
                                SystemKvInfoVo kvInfoVo = kvInfoVoR.getData();
                                try {
                                    magnification = Double.parseDouble(kvInfoVo.getKvValue());
                                }catch (Exception e) {
                                    log.info("互动率倍率转成double失败=={}", kvInfoVo.getKvValue());
                                    e.printStackTrace();
                                }
                            }
                            videoDataViewingConfuseInfoVo.setInteractionPercent(videoDataViewingConfuseInfoVo.getInteractionPercent() * magnification);
                        }
                        // 如果大于9.8，随机取9.5-9.8
                        if(videoDataViewingConfuseInfoVo.getInteractionPercent() > 9.8) {
                            videoDataViewingConfuseInfoVo.setInteractionPercent(Math.random() * 0.3 + 9.5);
                        }
                    }
                }
            }
        }
    }

    @Override
    public R<VideoDataViewingContrastVo> infoByContrastId(String contrastId) {

        return this.videoDataViewingBll.infoByContrastId(contrastId);

//        R<VideoDataViewingContrastVo> videoDataViewingContrastVoR = this.videoDataViewingBll.infoByContrastId(contrastId);
//        VideoDataViewingContrastVo videoDataViewingContrastVo = videoDataViewingContrastVoR.getData();
//        if(videoDataViewingContrastVo != null) {
//            packageInteractionPercent(videoDataViewingContrastVo.getVideoDataViewing1());
//            packageInteractionPercent(videoDataViewingContrastVo.getVideoDataViewing2());
//        }
//
//        return videoDataViewingContrastVoR;
    }


}

