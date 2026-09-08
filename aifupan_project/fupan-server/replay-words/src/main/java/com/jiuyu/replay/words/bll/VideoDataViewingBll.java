package com.jiuyu.replay.words.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.common.producer.SystemConfigProducer;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.common.vo.SystemConfigInfoVo;
import com.jiuyu.replay.generic.bo.third.ChanmamaQueryBo;
import com.jiuyu.replay.generic.dto.third.ChanmamaRevisionDto;
import com.jiuyu.replay.generic.dto.words.viewing.FlowSourceDto;
import com.jiuyu.replay.generic.dto.words.viewing.UserPortraitDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.third.ChanmamaFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.aiagent.VideoWatchHasVO;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.viewing.OceanEngineDataInfoVo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineDataBo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.bo.viewing.UpdateConfuseDataByVideoIdBo;
import com.jiuyu.replay.words.bo.viewing.*;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.entity.VideoDataViewingEntity;
import com.jiuyu.replay.words.entity.VideoDataViewingParagraphEntity;
import com.jiuyu.replay.words.enums.ChanmamaCallbackDataStatusEnum;
import com.jiuyu.replay.words.enums.DataViewingSourceTypeEnum;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import com.jiuyu.replay.words.rse.VideoDataViewingParagraphRse;
import com.jiuyu.replay.words.rse.VideoSliceRse;
import com.jiuyu.replay.words.vo.OceanEngineDataVo;
import com.jiuyu.replay.words.vo.VideoRoiVo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.viewing.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * 视频看盘数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Component
@Slf4j
public class VideoDataViewingBll {

    @Resource
    private VideoDataViewingProducer videoDataViewingProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private AnchorUrlProducer anchorUrlProducer;
    @Resource
    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;
    @Resource
    private SyncContrastProducer syncContrastProducer;
    @Resource
    private ChanmamaSendRecordProducer chanmamaSendRecordProducer;
    @Resource
    private SystemConfigProducer systemConfigProducer;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;
    @Resource
    private ChanmamaFeign chanmamaFeign;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private OceanEngineDataProducer oceanEngineDataProducer;
    @Resource
    private UserFeign userFeign;
    @Resource
    private AiOssUtils aiOssUtils;
    @Resource
    private OssUtils ossUtils;
    @Resource
    private VideoDataViewingParagraphRse videoDataViewingParagraphRse;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private VideoSliceRse videoSliceRse;
    @Resource
    private AnchorVideoService anchorVideoService;
    @Resource
    private AnchorUrlUserService anchorUrlUserService;


    /**
     * 视频看盘数据列表
     *
     * @param videoDataViewingListBo 视频看盘数据列表查询参数
     * @return
     */
    public R<PageUtils<VideoDataViewingListVo>> queryPage(VideoDataViewingListBo videoDataViewingListBo) {

        return R.ok("获取成功", videoDataViewingProducer.queryPage(videoDataViewingListBo));
    }

    /**
     * 视频看盘数据信息
     *
     * @param id 视频看盘数据id
     * @return
     */
    public R<VideoDataViewingInfoVo> info(Long id) {

        VideoDataViewingInfoVo videoDataViewingInfoVo = videoDataViewingProducer.info(id);
        return R.ok("获取成功", videoDataViewingInfoVo);
    }

    /**
     * 新增视频看盘数据
     *
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
    public R<String> save(VideoDataViewingBo videoDataViewingBo) {

        VideoDataViewingInfoVo videoDataViewingInfoVo = videoDataViewingProducer.save(videoDataViewingBo);
        return R.ok("添加成功");
    }

    /**
     * 修改视频看盘数据
     *
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
    public R<String> update(VideoDataViewingBo videoDataViewingBo) {

        videoDataViewingProducer.update(videoDataViewingBo);
        return R.ok("修改成功");
    }

    /**
     * 删除视频看盘数据
     *
     * @param id 视频看盘数据id
     * @return
     */
    public R<String> delete(Long id) {

        videoDataViewingProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 查询本地数据库有没有对应的数据，如果有就直接创建数据看盘记录
     *
     * @param videoId  视频id
     * @param userId   用户id
     * @param tenantId 租户id
     * @return
     */
    public R<Boolean> checkLocalDBExistCreate(String videoId, Long userId, Long tenantId) {

        AnchorVideoInfoVo videoInfoVo = this.anchorVideoProducer.getByVideoId(videoId);
        if (videoInfoVo != null) {
            AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlProducer.infoBySecUid(videoInfoVo.getSecUid());
            if (anchorUrlInfoVo != null) {

                // 结束时间默认减180秒
                long endTimeDifference = 180 * 1000;
                SystemConfigInfoVo systemConfigInfoVo = systemConfigProducer.info(1L);
                if (systemConfigInfoVo != null) {
                    endTimeDifference = systemConfigInfoVo.getDataViewingTimeDifference();
                }

                // 获取开始和结束时间戳，
                long startTime = videoInfoVo.getStartTime() != null ? videoInfoVo.getStartTime().getTime() : 0;
                long endTime = videoInfoVo.getEndTime() != null ? videoInfoVo.getEndTime().getTime() - endTimeDifference : 0;

                if (endTime < startTime) {
                    endTime = startTime;
                }

                boolean exist = videoDataViewingProducer.checkLocalDBExistCreate(videoId, anchorUrlInfoVo.getAnchorNumber(), startTime, endTime, videoInfoVo.getBatchNumber().toString(), userId, tenantId);
                return R.ok(exist);
            }
        }

        return R.ok(false);
    }

    /**
     * 根据视频id获取看盘数据（优先取本段视频数据）
     * @param videoId 视频id
     * @return
     */
    public R<VideoDataViewingConfuseInfoVo> infoByVideoIdPriorityParagraph(String videoId) {
        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = this.videoDataViewingConfuseProducer.infoByVideoIdAndUser(videoId, null, null);
        if(videoDataViewingConfuseInfoVo != null) {
            if(Objects.equals(videoDataViewingConfuseInfoVo.getDataSourceType(), DataViewingSourceTypeEnum.JULIANGBAIYING.getType())) {
                // 获取本段视频看板数据
                VideoDataViewingConfuseInfoVo paragraphDataViewingInfoVo = this.videoDataViewingParagraphRse.infoByVideoId(videoId);
                if(paragraphDataViewingInfoVo != null) {
                    videoDataViewingConfuseInfoVo = paragraphDataViewingInfoVo;
                }
            }
        }
        return R.ok(videoDataViewingConfuseInfoVo);
    }

    /**
     * 根据视频id获取看盘数据
     *
     * @param videoId 视频id
     * @return
     */
    public R<VideoDataViewingConfuseInfoVo> infoByVideoId(String videoId) {

        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = this.videoDataViewingConfuseProducer.infoByVideoIdAndUser(videoId, null, null);

        packageVideoDataViewingConfuseTime(videoDataViewingConfuseInfoVo, 0);

        return R.ok(videoDataViewingConfuseInfoVo);
    }

    /**
     * 封装数据看板的开始时间结束时间
     * @param videoDataViewingConfuseInfoVo 数据看板信息
     * @param type 类型 0：整场数据 1：本段数据
     */
    private void packageVideoDataViewingConfuseTime(VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo, int type) {
        if(videoDataViewingConfuseInfoVo == null) {
            return;
        }
        // 封装开始时间结束时间
        AnchorVideoInfoVo videoInfoVo = this.anchorVideoProducer.getByVideoId(videoDataViewingConfuseInfoVo.getVideoId());
        if(videoInfoVo == null) {
            return;
        }

        if(type == 0) {
            videoDataViewingConfuseInfoVo.setDataStartTime("开播时间");
        }else {
            LocalDateTime localDateTime = videoInfoVo.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            videoDataViewingConfuseInfoVo.setDataStartTime(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        LocalDateTime localDateTime = videoInfoVo.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        videoDataViewingConfuseInfoVo.setDataEndTime(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

//        if(VideoSliceTypeEnum.VIDEO.getCode().equals(videoInfoVo.getVideoSliceType())) {
//            // 原视频
//            if(type == 0) {
//                videoDataViewingConfuseInfoVo.setDataStartTime("开播时间");
//            }else {
//                LocalDateTime localDateTime = videoInfoVo.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//                videoDataViewingConfuseInfoVo.setDataStartTime(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//            }
//            LocalDateTime localDateTime = videoInfoVo.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//            videoDataViewingConfuseInfoVo.setDataEndTime(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//        }else {
//            // 切片视频，获取原视频的时间
//            VideoSliceVo videoSliceVo = this.videoSliceRse.getByVideoId(videoInfoVo.getVideoId());
//            if(videoSliceVo == null) {
//                return;
//            }
//
//            AnchorVideoInfoVo sliceVideoInfoVo = this.anchorVideoProducer.getByVideoId(videoSliceVo.getSourceParentId());
//            if(sliceVideoInfoVo == null) {
//                return;
//            }
//
//            if(type == 0) {
//                videoDataViewingConfuseInfoVo.setDataStartTime("开播时间");
//            }else {
//                LocalDateTime localDateTime = sliceVideoInfoVo.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//                videoDataViewingConfuseInfoVo.setDataStartTime(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//            }
//
//            LocalDateTime localDateTime = sliceVideoInfoVo.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//            videoDataViewingConfuseInfoVo.setDataEndTime(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//        }
    }

    /**
     * 根据对比id获取看盘数据
     *
     * @param contrastId 对比id
     * @return
     */
    public R<VideoDataViewingContrastVo> infoByContrastId(String contrastId) {

        // 获取对比信息
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);

        if (syncContrastInfoVo != null && syncContrastInfoVo.getContrastType() == 0) {

            VideoDataViewingContrastVo videoDataViewingContrastVo = new VideoDataViewingContrastVo();

            // 视频1看盘数据
            R<VideoDataViewingConfuseInfoVo> oneR = this.infoByVideoIdPriorityParagraph(syncContrastInfoVo.getVideoOneId());
            if (oneR.getCode() == 0 && oneR.getData() != null) {
                videoDataViewingContrastVo.setVideoDataViewing1(oneR.getData());
            }

            // 视频2看盘数据
            R<VideoDataViewingConfuseInfoVo> twoR = this.infoByVideoIdPriorityParagraph(syncContrastInfoVo.getVideoTwoId());
            if (twoR.getCode() == 0 && twoR.getData() != null) {
                videoDataViewingContrastVo.setVideoDataViewing2(twoR.getData());
            }

            return R.ok(videoDataViewingContrastVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "对比信息不存在");
    }

    /**
     * 处理第三方数据平台回调数据
     *
     * @param videoDataViewingCallbackBo 第三方数据平台回调数据
     */
    public R<ChanmamaSendRecordInfoVo> handleDataViewingCallback(VideoDataViewingCallbackBo videoDataViewingCallbackBo) {

//        log.debug("第三方数据平台回调内容===={}", JSON.toJSONString(videoDataViewingCallbackBo));

        if (videoDataViewingCallbackBo == null || StringUtils.isEmpty(videoDataViewingCallbackBo.getRequestId())) {
            log.warn("第三方数据平台回调内容为空或请求id为空");
            return R.error(500, "回调内容为空或请求id为空");

        }

        // 获取请求记录
        ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = null;
        // 为避免回调早于事务提交，导致获取不到数据，尝试5次
        int num = 5;
        while (num > 0) {
            chanmamaSendRecordInfoVo = this.chanmamaSendRecordProducer.infoByRequestId(videoDataViewingCallbackBo.getRequestId());
            if (chanmamaSendRecordInfoVo == null) {
                log.warn("第三方数据平台回调记录为空，正在尝试重新获取===={}", videoDataViewingCallbackBo.getRequestId());
                num--;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                break;
            }
        }

        if (chanmamaSendRecordInfoVo == null || chanmamaSendRecordInfoVo.getId() == 0) {
            log.warn("第三方数据平台回调请求id查询不到数据==={}", videoDataViewingCallbackBo.getRequestId());
            return R.error(500, "请求id查询不到数据===" + videoDataViewingCallbackBo.getRequestId());
        }

        if(!StringUtils.isEmpty(chanmamaSendRecordInfoVo.getCallbackStatus())
                && !chanmamaSendRecordInfoVo.getDataStatus().equals(ChanmamaCallbackDataStatusEnum.ABNORMAL.getStatus())
                && !chanmamaSendRecordInfoVo.getDataStatus().equals(ChanmamaCallbackDataStatusEnum.NOT_CHECK.getStatus())) {

            // 回调状态不为空且数据状态不是异常状态，说明已经处理过了，忽略
            log.info("第三方数据平台回调状态回调状态不为空且数据状态不是异常状态，说明已经处理过了，忽略==={}", chanmamaSendRecordInfoVo.getRequestId());
            return R.ok(chanmamaSendRecordInfoVo.getId().toString());
        }

//        if(!StringUtils.isEmpty(chanmamaSendRecordInfoVo.getCallbackStatus())) {
//            // 回调状态不为空，说明已经处理过了，忽略
//            log.info("回调状态不为空，说明已经处理过了，忽略==={}", chanmamaSendRecordInfoVo.getRequestId());
//            return R.ok(chanmamaSendRecordInfoVo.getId().toString());
//        }

        return R.ok(chanmamaSendRecordInfoVo);

    }

    /**
     * 回调之后的数据库处理
     *
     * @param videoDataViewingCallbackBo 回调信息
     * @param chanmamaSendRecordInfoVo   发送记录
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<List<UseDataViewingPropertyBo>> handleDataViewingCallbackData(VideoDataViewingCallbackBo videoDataViewingCallbackBo, ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo) {

        List<UseDataViewingPropertyBo> propertyResult = new LinkedList<>();

        if (videoDataViewingCallbackBo.getCode() == 0) {
            List<LiveRoomBo> liveRoomBoList = videoDataViewingCallbackBo.getData();
            if (liveRoomBoList != null && liveRoomBoList.size() > 0) {
                for (LiveRoomBo liveRoomBo : liveRoomBoList) {

                    // 更新请求记录的回调内容
                    this.chanmamaSendRecordProducer.updateCallbackBody(
                            chanmamaSendRecordInfoVo,
                            videoDataViewingCallbackBo.getCode(),
                            JSON.toJSONString(videoDataViewingCallbackBo),
                            liveRoomBo.getIsVerified(),
                            liveRoomBo.getAccountType(),
                            liveRoomBo.getRoomId());

                    if(liveRoomBo.getIsVerified() == null) {
                        liveRoomBo.setIsVerified(0);
                    }
                    // 保存原始看板数据
                    VideoDataViewingEntity videoDataViewingEntity = this.videoDataViewingProducer.saveDataViewingCallbackData(liveRoomBo, chanmamaSendRecordInfoVo.getBatchNumber(), videoDataViewingCallbackBo.getRequestId());
                    // 查询正在拉取状态的数据，匹配上则修改状态
                    List<UseDataViewingPropertyBo> useDataViewingPropertyBos = this.videoDataViewingProducer.checkLocalDbExistUpdateStatus(
                            videoDataViewingEntity,
                            liveRoomBo.getLiveAuthorVo().getUniqueId(),
                            chanmamaSendRecordInfoVo.getBatchNumber(),
                            liveRoomBo.getIsVerified());

                    if(useDataViewingPropertyBos != null && useDataViewingPropertyBos.size() > 0
                            && !ChanmamaCallbackDataStatusEnum.ABNORMAL_REPAIR.getStatus().equals(liveRoomBo.getIsVerified())) {
                        // 当回调的数据状态不是修正后的状态，才使用资产
                        propertyResult.addAll(useDataViewingPropertyBos);
                    }
                }
            }
        }else {

            // 更新请求记录的回调内容
            this.chanmamaSendRecordProducer.updateCallbackBody(chanmamaSendRecordInfoVo,
                    videoDataViewingCallbackBo.getCode(),
                    JSON.toJSONString(videoDataViewingCallbackBo),
                    null,
                    null,
                    null);

            // 查询数据异常，更新请求id对应的数据状态
            this.videoDataViewingConfuseProducer.callbackCodeErrorHandle(videoDataViewingCallbackBo.getRequestId(), videoDataViewingCallbackBo.getCode());

        }

        return R.ok(propertyResult);
    }

    /**
     * 后台根据视频id重新拉取看盘数据
     * @param videoId 视频id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> backRePullData(String videoId) {

        VideoDataViewingConfuseInfoVo viewingConfuseInfoVo = this.videoDataViewingConfuseProducer.getByVideoId(videoId);
        if(viewingConfuseInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前视频没有看板数据");
        }
//        if(viewingConfuseInfoVo.getDataStatus().equals(DataViewingStatusEnum.DATA_ORGANIZE.getStatus())) {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前视频的正确看板数据正在整理中，请等待");
//        }
        if(viewingConfuseInfoVo.getDataSourceType().equals(DataViewingSourceTypeEnum.JULIANGBAIYING.getType())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前看板数据的来源渠道是巨量百应，无需重新获取");
        }

        if(!StringUtils.hasText(viewingConfuseInfoVo.getBatchNumber())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前看板数据场次ID为空");
        }

        String requestId = viewingConfuseInfoVo.getRequestId();
        if(!StringUtils.hasText(requestId) || requestId.equals("0")) {
            // 当前视频没有请求id，拿相同视频的请求id
            List<VideoDataViewingConfuseInfoVo> videoDataViewingConfuseInfoVos = this.videoDataViewingConfuseProducer.listByBatchNumberAndDataType(viewingConfuseInfoVo.getBatchNumber(), DataViewingSourceTypeEnum.CHANMAMA.getType());
            if(videoDataViewingConfuseInfoVos != null && videoDataViewingConfuseInfoVos.size() > 0) {
                for(VideoDataViewingConfuseInfoVo vo : videoDataViewingConfuseInfoVos) {
                    if(StringUtils.hasText(vo.getRequestId()) && !vo.getRequestId().equals("0")) {
                        requestId = vo.getRequestId();
                        break;
                    }
                }
            }
        }

        if(!StringUtils.hasText(requestId) || requestId.equals("0")) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频请求发送记录为空");
        }

        ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = this.chanmamaSendRecordProducer.infoByRequestId(requestId);
        if(chanmamaSendRecordInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频请求发送记录为空");
        }

        // 构建请求体
        ChanmamaQueryBo chanmamaQueryBo = JSON.parseObject(chanmamaSendRecordInfoVo.getRequestBody(), ChanmamaQueryBo.class);
        ChanmamaRevisionDto chanmamaRevisionDto = new ChanmamaRevisionDto();
        chanmamaRevisionDto.setRequestId(chanmamaSendRecordInfoVo.getRequestId());
        chanmamaRevisionDto.setPlatformType(chanmamaSendRecordInfoVo.getAccountType());
        if(!StringUtils.isEmpty(chanmamaSendRecordInfoVo.getRoomId())) {
            chanmamaRevisionDto.setRoomId(chanmamaSendRecordInfoVo.getRoomId());
        }
        chanmamaRevisionDto.setBeginDate(chanmamaQueryBo.getBeginDate());
        chanmamaRevisionDto.setEndDate(chanmamaQueryBo.getEndDate());

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDateTime dateTime = LocalDateTime.parse(chanmamaRevisionDto.getEndDate(), inputFormatter);
        String date = dateTime.format(outputFormatter);
        chanmamaRevisionDto.setDate(date);

        // 发送请求
        R<Boolean> result = chanmamaFeign.sendRevisionQuery(chanmamaRevisionDto);
        if(result.getCode() != 0 || result.getData() == null || !result.getData()) {
            // 修改蝉妈妈发送记录
            this.chanmamaSendRecordProducer.saveRevisionData(
                    chanmamaSendRecordInfoVo.getId(),
                    chanmamaSendRecordInfoVo.getDataStatus(),
                    JSON.toJSONString(chanmamaRevisionDto),
                    JSON.toJSONString(result));

            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请求第三方失败");
        }

        // 修改视频数据看板的状态为”正确数据整理中“
        this.videoDataViewingConfuseProducer.updateDataStatusByBatchNumber(viewingConfuseInfoVo.getBatchNumber(), DataViewingStatusEnum.DATA_ORGANIZE.getStatus());

        // 修改蝉妈妈发送记录回调数据状态
        this.chanmamaSendRecordProducer.saveRevisionData(
                chanmamaSendRecordInfoVo.getId(),
                ChanmamaCallbackDataStatusEnum.ABNORMAL.getStatus(),
                JSON.toJSONString(chanmamaRevisionDto),
                JSON.toJSONString(result));

        return R.ok();

    }

    /**
     * 新增或更新巨量百应数据
     * @param oceanEngineDataBo 巨量百应数据
     * @return
     */
//    @CustomRedissonLock(key = "T(com.jiuyu.replay.common.constant.LockKeyPrefix).ANCHOR.getLockKey('oceanEngineDataMarkAdd:' + #args[0].videoId)", waitTime = 10)
    @Transactional(rollbackFor = Exception.class)
//    @CustomRedissonLock(key = "'create_viewing_lock:' + #args[0].videoId")
    public R<Long> saveOrUpdateOceanEngine(OceanEngineDataBo oceanEngineDataBo) {

        Long oldDataViewingId = null;

        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = this.videoDataViewingConfuseProducer.getByVideoId(oceanEngineDataBo.getVideoId());
        if(videoDataViewingConfuseInfoVo != null) {
            // 已经存在第三方数据，删除旧数据
            if(!videoDataViewingConfuseInfoVo.getDataSourceType().equals(DataViewingSourceTypeEnum.JULIANGBAIYING.getType())) {
                this.videoDataViewingConfuseProducer.deleteById(videoDataViewingConfuseInfoVo.getId());
            }else {
                oldDataViewingId = videoDataViewingConfuseInfoVo.getId();
            }

            // 如果上传的oss地址为空，用旧的
            if(StringUtils.isEmpty(oceanEngineDataBo.getOssPath())) {
                oceanEngineDataBo.setOssPath(videoDataViewingConfuseInfoVo.getOssPath());
            }
        }

        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        oceanEngineDataBo.setUserId(user.getId());
        oceanEngineDataBo.setCreateUserId(user.getId());
        oceanEngineDataBo.setUpdateUserId(user.getId());
        oceanEngineDataBo.setTenantId(user.getActiveTenantId());

        AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(oceanEngineDataBo.getSecUid());
        if(anchorUrlInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播信息不存在");
        }
        oceanEngineDataBo.setAnchorNumber(anchorUrlInfoVo.getAnchorNumber());

        // 保存数据看板数据
        VideoDataViewingConfuseInfoVo newDataViewingConfuseInfoVo = this.videoDataViewingConfuseProducer.saveOceanEngine(oceanEngineDataBo, oldDataViewingId);

        // 保存本段录制视频的看板数据
        if(!StringUtils.isEmpty(newDataViewingConfuseInfoVo.getOssPath())) {
            // 获取过程实时数据
            List<OceanEngineProcessBo> realTimeDataList = videoDataViewingConfuseProducer.getOceanEngineFileDataNotCache(newDataViewingConfuseInfoVo.getVideoId(), newDataViewingConfuseInfoVo.getOssPath());
            // 保存或更新本段录制的看板数据
            this.videoDataViewingParagraphRse.saveOrUpdateParagraphData(newDataViewingConfuseInfoVo, realTimeDataList);
        }

        // 删除数据看盘关联用户的昨日数据缓存
        this.redisTemplate.delete(wordsProperties.getYesterdayRecordRedisKey() + user.getActiveTenantId() + ":" + user.getId());

        return R.ok(newDataViewingConfuseInfoVo.getId());
    }

//    public VideoDataViewingConfuseInfoVo getOceanEngineDetailsByVideoId(String videoId) {
//        return videoDataViewingConfuseProducer.getOceanEngineByVideoId(videoId);
//    }

    /**
     * 根据视频id获取巨量百应数据
     * @param videoId 视频id
     * @return
     */
    public R<OceanEngineDataVo> getOceanEngineByVideoId(String videoId) {

        VideoDataViewingConfuseInfoVo viewingConfuseVo = videoDataViewingConfuseProducer.getOceanEngineByVideoId(videoId);
        if(viewingConfuseVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "不存在巨量百应数据");
        }

        return R.ok(buildOceanEngineDataVo(viewingConfuseVo));
    }

    /**
     * 根据视频id获取数据看板数据（不限数据源，字段级混合补齐）。
     * <p>以巨量百应为主，巨量为空的字段用蝉妈妈对应字段补齐，供 AI Agent 等只关心看板指标、
     * 不关心来源的场景使用，避免硬锁定巨量百应导致蝉妈妈源场次整块「数据缺失」。
     * 投放类字段（roi/launchRoiAmount 等）两源均无时仍为空，属正常。</p>
     *
     * @param videoId 视频id
     * @return 混合补齐后的看板数据（巨量百应优先），不存在返回错误 R
     */
    public R<OceanEngineDataVo> getDashboardByVideoId(String videoId) {

        VideoDataViewingConfuseInfoVo viewingConfuseVo = videoDataViewingConfuseProducer.getDashboardByVideoId(videoId);
        if(viewingConfuseVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "不存在数据看板数据");
        }


        return R.ok(buildOceanEngineDataVo(viewingConfuseVo));
    }

    /**
     * 把看板混淆行映射为对外的 {@link OceanEngineDataVo}（含 dataJson + 签名后的原始文件下载地址）。
     */
    private OceanEngineDataVo buildOceanEngineDataVo(VideoDataViewingConfuseInfoVo viewingConfuseVo) {

        // 整理数据
        OceanEngineDataInfoVo oceanEngineDataInfoVo = new OceanEngineDataInfoVo();
        oceanEngineDataInfoVo.setVideoId(viewingConfuseVo.getVideoId());
        oceanEngineDataInfoVo.setSecUid(viewingConfuseVo.getSecUid());
        oceanEngineDataInfoVo.setBatchNumber(viewingConfuseVo.getBatchNumber());
        oceanEngineDataInfoVo.setWatchFlowList(StringUtils.isEmpty(viewingConfuseVo.getWatchFlowList()) ? null : JSON.parseArray(viewingConfuseVo.getWatchFlowList(), FlowSourceDto.class));
        oceanEngineDataInfoVo.setPayFlowList(StringUtils.isEmpty(viewingConfuseVo.getPayFlowList()) ? null : JSON.parseArray(viewingConfuseVo.getPayFlowList(), FlowSourceDto.class));
        oceanEngineDataInfoVo.setWatchUserPortrait(StringUtils.isEmpty(viewingConfuseVo.getWatchUserPortrait()) ? null : JSON.parseObject(viewingConfuseVo.getWatchUserPortrait(), UserPortraitDto.class));
        oceanEngineDataInfoVo.setPayUserPortrait(StringUtils.isEmpty(viewingConfuseVo.getPayUserPortrait()) ? null : JSON.parseObject(viewingConfuseVo.getPayUserPortrait(), UserPortraitDto.class));
        oceanEngineDataInfoVo.setIsTakeProduct(viewingConfuseVo.getIsTakeProduct());
        oceanEngineDataInfoVo.setAnchorNumber(viewingConfuseVo.getAnchorNumber());
        oceanEngineDataInfoVo.setTotalWatchNum(viewingConfuseVo.getTotalWatchNum());
        oceanEngineDataInfoVo.setAverageOnlineNum(viewingConfuseVo.getAverageOnlineNum());
        oceanEngineDataInfoVo.setAverageResidenceTime(viewingConfuseVo.getAverageResidenceTime());
        oceanEngineDataInfoVo.setIncrementFollowerCount(viewingConfuseVo.getIncrementFollowerCount());
        oceanEngineDataInfoVo.setConvertFanRate(viewingConfuseVo.getConvertFanRate());
        oceanEngineDataInfoVo.setInteractionPercent(viewingConfuseVo.getInteractionPercent());
        oceanEngineDataInfoVo.setVolume(viewingConfuseVo.getVolumeStart());
        oceanEngineDataInfoVo.setPurchaseCount(viewingConfuseVo.getPurchaseCountStart());
        oceanEngineDataInfoVo.setCustomerUnitPrice(viewingConfuseVo.getCustomerUnitPriceStart());
        oceanEngineDataInfoVo.setUvValue(viewingConfuseVo.getUvValueStart());
        oceanEngineDataInfoVo.setGoodsConvertRate(viewingConfuseVo.getGoodsConvertRateStart());
        // 上面五个「精确值」字段取的其实是区间下界（平台对未授权账号只给「1w~2.5w」这类区间）。
        // 此前区间端点未随 dataJson 下发，OceanEngineDataInfoVo 上已声明的 *Start/*End 恒为 null，
        // 下游（含 AI Agent 看板接口）无从分辨拿到的是精确值还是下界，故在此补齐——纯增量，不改动原有字段。
        oceanEngineDataInfoVo.setVolumeStart(viewingConfuseVo.getVolumeStart());
        oceanEngineDataInfoVo.setVolumeEnd(viewingConfuseVo.getVolumeEnd());
        oceanEngineDataInfoVo.setPurchaseCountStart(viewingConfuseVo.getPurchaseCountStart());
        oceanEngineDataInfoVo.setPurchaseCountEnd(viewingConfuseVo.getPurchaseCountEnd());
        oceanEngineDataInfoVo.setCustomerUnitPriceStart(viewingConfuseVo.getCustomerUnitPriceStart());
        oceanEngineDataInfoVo.setCustomerUnitPriceEnd(viewingConfuseVo.getCustomerUnitPriceEnd());
        oceanEngineDataInfoVo.setUvValueStart(viewingConfuseVo.getUvValueStart());
        oceanEngineDataInfoVo.setUvValueEnd(viewingConfuseVo.getUvValueEnd());
        oceanEngineDataInfoVo.setGoodsConvertRateStart(viewingConfuseVo.getGoodsConvertRateStart());
        oceanEngineDataInfoVo.setGoodsConvertRateEnd(viewingConfuseVo.getGoodsConvertRateEnd());
        oceanEngineDataInfoVo.setRoi(viewingConfuseVo.getRoi());
        oceanEngineDataInfoVo.setLaunchRoiAmount(viewingConfuseVo.getLaunchRoiAmount());
        oceanEngineDataInfoVo.setRefundAmount(viewingConfuseVo.getRefundAmount());
        oceanEngineDataInfoVo.setOverallCostRoi(viewingConfuseVo.getOverallCostRoi());
        oceanEngineDataInfoVo.setNetTransactionRoi(viewingConfuseVo.getNetTransactionRoi());
        oceanEngineDataInfoVo.setShowWatchCntRatio(viewingConfuseVo.getShowWatchCntRatio());
        OceanEngineDataVo oceanEngineDataVo = new OceanEngineDataVo();

        oceanEngineDataVo.setDataJson(JSON.toJSONString(oceanEngineDataInfoVo));
        oceanEngineDataVo.setRawOssPath(viewingConfuseVo.getOssPath());
        if (ObjectUtil.isNotEmpty(viewingConfuseVo.getOssPath())) {
            oceanEngineDataVo.setOssPath(aiOssUtils.getSignDownloadUrl(viewingConfuseVo.getOssPath()));
        }

        return oceanEngineDataVo;
    }

    /**
     * 根据视频id获取巨量百应数据-带实时数据
     *
     * @param videoId 视频id
     * @return
     */
    public VideoDataViewingConfuseInfoVo getOceanEngineDetailsByVideoId(String videoId) {
        VideoDataViewingConfuseInfoVo viewingConfuseVo = videoDataViewingConfuseProducer.getOceanEngineByVideoId(videoId);
        if (viewingConfuseVo == null) return null;
        if (ObjectUtil.isNotEmpty(viewingConfuseVo.getOssPath())) {
            viewingConfuseVo.setOceanEngineProcessList(videoDataViewingConfuseProducer.getOceanEngineFileData(videoId, viewingConfuseVo.getOssPath()));
        }
        return viewingConfuseVo;
    }

    /**
     * 从zip字节数组中提取文本内容（仅处理UTF-8编码的txt文件）
     *
     * @param zipBytes zip文件的字节数组
     * @return 提取的文本内容
     */
    private String extractTextFromZipBytes(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            log.warn("zip字节数组为空");
            return "";
        }

        log.info("开始解析zip文件，字节数组大小: {}", zipBytes.length);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes); ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            // 遍历zip中的每个条目
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                log.info("发现zip条目: {}", fileName);

                // 只处理.txt文件
                if (fileName.toLowerCase().endsWith(".txt")) {
                    log.info("开始读取txt文件: {}", fileName);

                    // 使用UTF-8编码直接读取
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8))) {
                        StringBuilder contentBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            contentBuilder.append(line).append(System.lineSeparator());
                        }
                        String content = contentBuilder.toString();
                        log.info("成功读取文件内容，长度: {}", content.length());
                        return content;
                    }
                }
                zis.closeEntry();
            }

            log.warn("zip文件中未找到txt文件");
            return "";

        } catch (IOException e) {
            log.error("从字节流解压zip文件失败", e);
            return "";
        }
    }

    /**
     * 根据视频id获取巨量数据上传的预签名链接
     * @param videoId 视频id
     * @return
     */
    public R<SignUploadUrlVo> getOceanEngineUploadUrlByVideoId(String videoId) {

        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
        if (video == null) {
            return R.error(StatusCode.DATA_NOT_EXIST.getCode(), "视频不存在，获取预上传连接失败");
        }
        String currentDay = DateUtil.format(video.getCreateDate(), "yyyy/MM/dd");
        String ossKey = CharSequenceUtil.format("{}/{}/{}.zip", "oceanEngineData", currentDay, video.getVideoId());
        SignUploadUrlVo signUploadUrl = aiOssUtils.getSignUploadUrl(ossKey);
        return R.ok(signUploadUrl);
    }

    /**
     * 查询同租户下有没有对应的巨量百应数据，如果有就直接创建数据看盘
     * @param videoId 视频id
     * @return
     */
    public R<Boolean> checkJlbyDataExistCreate(String videoId) {

        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
        if (video == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频不存在");
        }

        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = videoDataViewingConfuseProducer.checkJlbyDataExist(videoId);
        if(videoDataViewingConfuseInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有同场次的视频");
        }

        VideoDataViewingConfuseBo videoDataViewingConfuseBo = BeanConvertUtils.convert(videoDataViewingConfuseInfoVo, VideoDataViewingConfuseBo.class);

        this.videoDataViewingConfuseProducer.copyTenantJlbyData(videoDataViewingConfuseBo, video.getUserId(), video.getVideoId());

        return R.ok(true);
    }

    /**
     * 查询同租户下有没有对应的巨量百应数据
     * @param videoId 视频id
     * @return
     */
    public R<VideoDataViewingConfuseInfoVo> checkJlbyDataExist(String videoId) {

        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
        if (video == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频不存在");
        }

        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = videoDataViewingConfuseProducer.checkJlbyDataExist(videoId);

        if(videoDataViewingConfuseInfoVo != null) {
            return R.ok(videoDataViewingConfuseInfoVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有同场次的视频");
    }

    /**
     * 拷贝对应的巨量百应数据
     * @param videoDataViewingConfuseBo 拷贝源数据
     * @param userId 用户id
     * @param videoId 视频videoId
     * @return
     */
    public R<String> copyTenantJlbyData(VideoDataViewingConfuseBo videoDataViewingConfuseBo, Long userId, String videoId) {

        this.videoDataViewingConfuseProducer.copyTenantJlbyData(videoDataViewingConfuseBo, userId, videoId);

        return R.ok();
    }

    /**
     * 根据视频id获取本段视频的数据看板数据
     * @param videoId 视频id
     * @return
     */
    public R<VideoDataViewingConfuseInfoVo> paragraphInfoByVideoId(String videoId) {

        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = this.videoDataViewingParagraphRse.infoByVideoId(videoId);

        packageVideoDataViewingConfuseTime(videoDataViewingConfuseInfoVo, 1);

        return R.ok(videoDataViewingConfuseInfoVo);
    }

    /**
     * 根据视频id获取数据看盘数据
     * <p>
     * 优先返回本段数据，如果本段数据无效或不存在则返回混淆数据
     * </p>
     *
     * @param videoId 视频id
     * @return 数据看盘数据
     */
    public R<VideoDataViewingConfuseInfoVo> dataViewingByVideoId(String videoId) {
        // 1. 先查询本段数据
        VideoDataViewingConfuseInfoVo paragraphData = this.videoDataViewingParagraphRse.infoByVideoId(videoId);

        // 2. 判断本段数据是否有效（dataStatus = 1 或 8）
        if (paragraphData != null && isDataValid(paragraphData.getDataStatus())) {
            return R.ok(paragraphData);
        }

        // 3. 本段数据无效或不存在，查询混淆数据
        VideoDataViewingConfuseInfoVo confuseData = this.videoDataViewingConfuseProducer.getByVideoId(videoId);

        // 4. 判断混淆数据是否有效
        if (confuseData != null && isDataValid(confuseData.getDataStatus())) {
            return R.ok(confuseData);
        }

        // 5. 都无效或不存在，返回null
        return R.ok(null);
    }

    /**
     * 判断数据状态是否有效
     *
     * @param dataStatus 数据状态
     * @return true-有效, false-无效
     */
    private boolean isDataValid(Integer dataStatus) {
        if (dataStatus == null) {
            return false;
        }
        return DataViewingStatusEnum.PULL_SUCCESS.getStatus().equals(dataStatus)
                || DataViewingStatusEnum.DATA_ORGANIZE.getStatus().equals(dataStatus);
    }

    /**
     * 后台根据视频ID批量修改关联的混淆数据
     * <p>
     * 业务流程：
     * 1. 根据videoId查询混淆表获取该视频的混淆数据
     * 2. 从查询结果中获取videoDataViewingId（关联的数据看盘ID）
     * 3. 如果videoDataViewingId有值，查询所有具有相同videoDataViewingId的混淆数据
     * 4. 批量更新所有关联的混淆数据记录（仅更新非空字段）
     * </p>
     *
     * @param bo 更新参数，包含videoId和需要修改的数据字段
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateByVideoId(UpdateConfuseDataByVideoIdBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getVideoId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频ID不能为空");
        }

        return this.videoDataViewingConfuseProducer.updateByVideoId(bo);
    }


    /**
     * 获取视频是否有数据看盘
     *
     * @param videoIds 视频ID列表
     * @return 视频ID和是否有数据看盘的映射关系
     */
    public Map<String, Boolean> getVideoHasDashboardMap(Collection<String> videoIds) {
        return videoDataViewingConfuseProducer.getVideoHasDashboardMap(videoIds);
    }

    /**
     * 批量查询视频观看次数
     * @param videoIds 视频ID集合
     * @return 视频ID和观看次数的映射关系
     */
    public Map<String, VideoWatchHasVO> getVideoWatchNum(Collection<String> videoIds) {
        return videoDataViewingConfuseProducer.getVideoWatchNum(videoIds);
    }

    /**
     * 查询视频ROI信息（段落表优先，confuse表兜底）
     */
    public VideoRoiVo getVideoRoi(String videoId) {
        VideoRoiVo vo = new VideoRoiVo();
        vo.setJlbyAuthStatus(0);
        vo.setQcAuthStatus(0);
        vo.setHasData(false);

        // 查询视频获取userId/tenantId
        AnchorVideoEntity video = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getVideoId, videoId)
                .eq(AnchorVideoEntity::getIsDeleted, 0));
        if (video != null) {
            // 查询授权状态
            AnchorUrlUserEntity anchorUser = anchorUrlUserService.getOne(new LambdaQueryWrapper<AnchorUrlUserEntity>()
                    .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, video.getSecUid())
                    .eq(AnchorUrlUserEntity::getUserId, video.getUserId())
                    .eq(AnchorUrlUserEntity::getTenantId, video.getTenantId())
                    .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
            );
            if (anchorUser != null) {
                vo.setJlbyAuthStatus(anchorUser.getAuthJlbyStatus());
                vo.setQcAuthStatus(anchorUser.getAuthQcStatus());
            }
        }

        // 查询ROI数据
        VideoDataViewingConfuseInfoVo info = videoDataViewingParagraphRse.infoByVideoId(videoId);
        if (info == null) {
            info = videoDataViewingConfuseProducer.getByVideoId(videoId);
        }
        if (info != null && ObjectUtil.equal(info.getDataSourceType(), DataViewingSourceTypeEnum.JULIANGBAIYING.getType())) {
            vo.setHasData(true);
            vo.setLaunchRoiAmount(info.getLaunchRoiAmount());
            vo.setSalesAmount(info.getVolumeEnd());
            vo.setOverallCostRoi(info.getOverallCostRoi());
            vo.setNetTransactionRoi(info.getNetTransactionRoi());
            vo.setPayCount(info.getPurchaseCountEnd());
            vo.setGpm(info.getGpmEnd());
            Double refund = info.getRefundAmount() != null ? info.getRefundAmount() : 0.0;
            Integer volume = info.getVolumeEnd() != null ? info.getVolumeEnd() : 0;
            vo.setNetTransactionAmount(volume - refund);
        }

        return vo;
    }
}

