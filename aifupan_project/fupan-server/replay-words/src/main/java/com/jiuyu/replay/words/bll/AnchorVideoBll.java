package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.constant.AnchorVideoEnums;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.http.governance.GovernanceHttpServer;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.words.ShareVideoCloudBo;
import com.jiuyu.replay.generic.bo.words.video.SubVideoListBo;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.dto.words.ShareVideoCloudDto;
import com.jiuyu.replay.generic.dto.words.UserVideoCountDto;
import com.jiuyu.replay.generic.enums.words.SourceStarTypeEnum;
import com.jiuyu.replay.generic.enums.words.VideoSliceTypeEnum;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.feign.ai.DiagnosisCueFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.words.video.SubUserVideoListVo;
import com.jiuyu.replay.generic.vo.words.video.SubUserYesterdayNotesVideoListVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.file.UploadFileDetailBo;
import com.jiuyu.replay.words.bo.governance.VideoProductRequestBo;
import com.jiuyu.replay.words.bo.governance.VideoPushRequestBo;
import com.jiuyu.replay.words.bo.video.*;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.SystemKeyConstant;
import com.jiuyu.replay.words.entity.AudioAnalysisEntity;
import com.jiuyu.replay.words.entity.ProductDetailsEntity;
import com.jiuyu.replay.words.entity.VideoTextNotes;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.repository.mongo.VideoTextNotesRepositoryService;
import com.jiuyu.replay.words.rse.*;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.anchor.HistoryBatchNumberVideoListVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.video.AiAnalyzeTradePromptVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AnchorVideoBll {
    @Resource
    AnchorVideoProducer anchorVideoProducer;
    @Resource
    AudioAnalysisProducer audioAnalysisProducer;
    @Resource
    AnchorVideoRecodProducer anchorVideoRecodProducer;
    @Resource
    private VideoAnalysisRecordProducer videoAnalysisRecordProducer;
    @Resource
    private SensitiveWordsProducer sensitiveWordsProducer;
    @Resource
    private AnchorUrlProducer anchorUrlProducer;
    @Resource
    private SyncContrastProducer syncContrastProducer;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;
    @Resource
    private ClientAiFavProducer clientAiFavProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;
    @Resource
    private AnchorVideoDetailProducer anchorVideoDetailProducer;
    @Resource
    private UserFeign userFeign;
    @Resource
    private AnchorVideoRse anchorVideoRse;
    @Resource
    private TradeProducer tradeProducer;
    @Resource
    private VideoTextNotesRepositoryService videoTextNotesRepositoryService;
    @Resource
    private VideoTextNotesRse videoTextNotesRse;
    @Resource
    private AnchorRse anchorRse;
    @Resource
    private SystemKvProducer systemKvProducer;
    @Resource
    private UploadFileDetailProducer uploadFileDetailProducer;
    @Resource
    private DictDataFeign dictDataFeign;
    @Resource
    private VideoSliceRse videoSliceRse;
    @Resource
    private AiOptimizePurposeRse aiOptimizePurposeRse;
    @Resource
    private SourceStarRse sourceStarRse;
    @Resource
    private VideoDataViewingParagraphRse videoDataViewingParagraphRse;
    @Resource
    private BasicSettingsProducer basicSettingsProducer;
    @Resource
    private GovernanceHttpServer governanceHttpServer;
    @Resource
    private ProductDetailsProducer productDetailsProducer;
    @Resource
    private DiagnosisCueFeign diagnosisCueFeign;

    /**
     * 录制时上传视频信息
     *
     * @return
     */
    public R<String> save(AnchorVideoInfoBo anchorVideoInfoBo) {
        return anchorVideoProducer.save(anchorVideoInfoBo);
    }

    /**
     * 查询所用的分享视频
     *
     * @param anchorVideoVO
     * @return
     */
    public R<PageUtils<AnchorVideoVO>> lists(@RequestBody AnchorVideoVO anchorVideoVO) {
        PageUtils<AnchorVideoVO> anchorVideoVOPageUtils = anchorVideoProducer.queryPage(anchorVideoVO);
        return R.ok("查询成功", anchorVideoVOPageUtils);
    }

    /**
     * 根据视频id查询该视频分析
     *
     * @param id
     * @return
     */
    public R<List<AudioAnalysisEntity>> selectByVideoId(AudioAnalysisBo id) {

        R<List<AudioAnalysisEntity>> listR = audioAnalysisProducer.selectByVideoId(id);
        return listR;
    }

    public void callback(@RequestBody String jsonBody) {
        JSONObject jsonObject = JSONObject.parseObject(jsonBody);

    }

    /**
     * 跟查询用户Id分页查询录制视频信息
     *
     * @param anchorVideoVO
     * @return
     */
    public R<PageUtils<AnchorVideoVO>> pageLists(AnchorVideoBo anchorVideoVO) {
        // 参数校验
        if (anchorVideoVO == null) {
            return buildEmptyResult();
        }

        // 查询行业（含子）
        if (StringUtil.isNotBlank(anchorVideoVO.getTradeId())) {
            try {
                String tradeId = anchorVideoVO.getTradeId();
                List<Long> childByTradeId = tradeProducer.getChildById(Long.valueOf(tradeId));
                if (childByTradeId.isEmpty()) {
                    return buildEmptyResult();
                }
                anchorVideoVO.setTradeList(childByTradeId);
            } catch (NumberFormatException e) {
                return buildEmptyResult();
            }
        }

        // 查询场观、销售额
        if (anchorVideoVO.getTotalWatchNum() != null || anchorVideoVO.getVolume() != null) {
            VideoDataViewingConfuseBo videoDataViewingConfuseBo = new VideoDataViewingConfuseBo();
            videoDataViewingConfuseBo.setTotalWatchNum(anchorVideoVO.getTotalWatchNum());
            videoDataViewingConfuseBo.setVolumeStart(anchorVideoVO.getVolume());
            videoDataViewingConfuseBo.setDataStatus(AnchorVideoEnums.videoDataViewingStatus.SUCCESS.getCode());

            List<VideoDataViewingConfuseInfoVo> viewingConfuseInfoVos = videoDataViewingConfuseProducer.listByObj(videoDataViewingConfuseBo);
            if (viewingConfuseInfoVos.isEmpty()) {
                return buildEmptyResult();
            }

            Set<String> filteredVideoIds = viewingConfuseInfoVos.stream()
                    .map(VideoDataViewingConfuseVo::getVideoId)
                    .filter(ObjectUtils::isNotEmpty)
                    .collect(Collectors.toSet());

            anchorVideoVO.setVideoIdList(new ArrayList<>(filteredVideoIds));
        }

        R<PageUtils<AnchorVideoVO>> pageUtilsR = anchorVideoProducer.pageLists(anchorVideoVO);

        //填充场观、销售额
        if (pageUtilsR.getData() != null && pageUtilsR.getData().getList() != null && !pageUtilsR.getData().getList().isEmpty()) {
            fillVideoViewingData(pageUtilsR.getData().getList());
        }
        return pageUtilsR;
    }

    /**
     * 填充视频的场观和销售额数据
     * 1. 先从 tb_video_data_viewing_paragraph 查询数据，填充观看人次和销售额
     * 2. 没有数据再从 tb_video_data_viewing_confuse 查询数据，填充观看人次和销售额
     * 3. 收集没有数据的视频，根据视频从 tb_socket_collect_message 查询，只填充观看人次
     *
     * @param videoList 视频列表
     */
    private void fillVideoViewingData(List<AnchorVideoVO> videoList) {
        if (videoList == null || videoList.isEmpty()) {
            return;
        }

        List<String> videoIds = videoList.stream()
                .map(AnchorVideoVO::getVideoId)
                .filter(ObjectUtils::isNotEmpty)
                .distinct()
                .toList();
        if (videoIds.isEmpty()) {
            return;
        }

        // 1. 优先从 tb_video_data_viewing_paragraph 查询数据
        List<VideoDataViewingConfuseInfoVo> paragraphInfoVos = videoDataViewingParagraphRse.listByVideoIds(videoIds);
        Map<String, VideoDataViewingConfuseInfoVo> paragraphVoMap = new HashMap<>();
        if (paragraphInfoVos != null && !paragraphInfoVos.isEmpty()) {
            paragraphVoMap = paragraphInfoVos.stream()
                    .collect(Collectors.toMap(VideoDataViewingConfuseVo::getVideoId, Function.identity(), (o1, o2) -> o2));
        }

        // 2. 填充数据并收集没有本段看板数据的视频
        List<AnchorVideoVO> noParagraphDataVideos = new ArrayList<>();
        for (AnchorVideoVO item : videoList) {
            VideoDataViewingConfuseInfoVo paragraphInfoVo = paragraphVoMap.get(item.getVideoId());
            if (paragraphInfoVo != null) {
                item.setTotalWatchNum(paragraphInfoVo.getTotalWatchNum());
                item.setVolumeStart(paragraphInfoVo.getVolumeStart());
                item.setVolumeEnd(paragraphInfoVo.getVolumeEnd());
            } else {
                noParagraphDataVideos.add(item);
            }
        }

        // 3. 对于没有本段看板数据的视频，从 tb_video_data_viewing_confuse 查询数据
        if (!noParagraphDataVideos.isEmpty()) {
            List<String> noParagraphVideoIds = noParagraphDataVideos.stream()
                    .map(AnchorVideoVO::getVideoId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            if (!noParagraphVideoIds.isEmpty()) {
                List<VideoDataViewingConfuseInfoVo> viewingConfuseInfoVos = videoDataViewingConfuseProducer
                        .listByVideoIdsAndStatus(noParagraphVideoIds, DataViewingStatusEnum.PULL_SUCCESS.getStatus());
                Map<String, VideoDataViewingConfuseInfoVo> confuseVoMap = new HashMap<>();
                if (viewingConfuseInfoVos != null && !viewingConfuseInfoVos.isEmpty()) {
                    confuseVoMap = viewingConfuseInfoVos.stream()
                            .collect(Collectors.toMap(VideoDataViewingConfuseVo::getVideoId, Function.identity(), (o1, o2) -> o2));
                }

                // 填充数据并收集没有confuse数据的视频
                List<AnchorVideoVO> noConfuseDataVideos = new ArrayList<>();
                for (AnchorVideoVO item : noParagraphDataVideos) {
                    VideoDataViewingConfuseInfoVo confuseInfoVo = confuseVoMap.get(item.getVideoId());
                    if (confuseInfoVo != null) {
                        item.setTotalWatchNum(confuseInfoVo.getTotalWatchNum());
                        item.setVolumeStart(confuseInfoVo.getVolumeStart());
                        item.setVolumeEnd(confuseInfoVo.getVolumeEnd());
                    } else {
                        noConfuseDataVideos.add(item);
                    }
                }

                // 4. 对于没有confuse数据的视频，从 tb_socket_collect_message 查询观看人次
                if (!noConfuseDataVideos.isEmpty()) {
                    List<String> noDataVideoIds = noConfuseDataVideos.stream()
                            .map(AnchorVideoVO::getVideoId)
                            .filter(ObjectUtils::isNotEmpty)
                            .distinct()
                            .toList();
                    if (!noDataVideoIds.isEmpty()) {
                        List<SocketCollectMessageInfoVo> socketCollectMessages = socketCollectMessageProducer
                                .listByVideoIds(noDataVideoIds);
                        if (socketCollectMessages != null && !socketCollectMessages.isEmpty()) {
                            Map<String, SocketCollectMessageInfoVo> socketMsgMap = socketCollectMessages.stream()
                                    .collect(Collectors.toMap(SocketCollectMessageVo::getVideoId, Function.identity(), (o1, o2) -> o2));
                            for (AnchorVideoVO item : noConfuseDataVideos) {
                                SocketCollectMessageInfoVo socketMsgVo = socketMsgMap.get(item.getVideoId());
                                if (socketMsgVo != null && ObjectUtils.isNotEmpty(socketMsgVo.getObservationNum())) {
                                    item.setTotalWatchNum(Integer.parseInt(socketMsgVo.getObservationNum()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 构建空结果
     *
     * @return
     */
    private R<PageUtils<AnchorVideoVO>> buildEmptyResult() {
        PageUtils<AnchorVideoVO> emptyPage = new PageUtils<>();
        emptyPage.setList(Collections.emptyList());
        emptyPage.setTotalCount(0);
        return R.ok(emptyPage);
    }

    /**
     * 新增直播视频分的内容
     *
     * @param udioAnalysisVo
     * @return
     */
    public R saveAudioAnalysis(List<AudioAnalysisVo> udioAnalysisVo) {
        return audioAnalysisProducer.saveAudioAnalysis(udioAnalysisVo);
    }

    /**
     * 更新视频信息
     *
     * @param anchorVideoInfoBo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateVideo(AnchorVideoInfoBo anchorVideoInfoBo) {

        AnchorVideoInfoVo oldVideo = this.anchorVideoProducer.getByVideoId(anchorVideoInfoBo.getVideoId());

        AnchorVideoInfoVo anchorVideoInfoVo = anchorVideoProducer.updateVideo(anchorVideoInfoBo);

        if (oldVideo != null) {
            if (oldVideo.getUploadStatus() == 1 && anchorVideoInfoVo.getUploadStatus() == 0) {
                // 查找视频相关的对比记录是否存在分享，存在则删除分享信息
                this.syncContrastProducer.deleteCloudByVideoId(anchorVideoInfoVo.getVideoId());
            }
        }

        return R.ok();
    }

    /**
     * 获取分析记录
     */
    public R<PageUtils<AnchorVideoVO>> list(AnchorVideoBo anchorVideoBo) {
        return anchorVideoRecodProducer.list(anchorVideoBo);
    }

    /**
     * 保存分析记录
     *
     * @param anchorVideoRecodVo
     * @return
     */
    public R<String> save(List<AudioAnalysisVo> anchorVideoRecodVo) {
        return anchorVideoRecodProducer.save(anchorVideoRecodVo);
    }

    /**
     * 客户端查询视频列表
     *
     * @param id
     * @return
     */
    public R<List<AnchorVideoVO>> selectByuserId(Long id) {
        return anchorVideoProducer.selectByuserId(id);
    }

    /**
     * 根据客户端上传的List<voidId>删除
     *
     * @param voidId
     * @return
     */
    public R<String> removeByVoidId(List<String> voidId) {
        return anchorVideoProducer.removeByVoidId(voidId);
    }

    /**
     * 客户端根据视频Id和行业ID查询该视频的分析内容
     *
     * @param videoId
     * @return
     */
    public R<List<AudioAnalysissVO>> selectByVideoIdOrTradeId(AudioAnalysisBo videoId) {

        return audioAnalysisProducer.selectByVideoIdOrTradeId(videoId);
    }

    /**
     * 服务端用户详情查询录制记录
     *
     * @param anchorVideoBo
     * @return
     */
    public R<PageUtils<AnchorVideoVO>> listByUserId(AnchorVideoBo anchorVideoBo) {
        return anchorVideoProducer.listByUserId(anchorVideoBo);
    }

    /**
     * 服务端用户详情查询分析记录
     *
     * @return
     */
    public R<PageUtils<AnchorVideoRecodListVo>> selectVideoRecod(AnchorVideoBo anchorVideoBo) {

        return anchorVideoRecodProducer.selectVideoRecod(anchorVideoBo);
    }

    /**
     * 清除视频的分析数据
     *
     * @param videoId 视频唯一标识
     * @return
     */
    public void clearAnalysis(String videoId) {

        this.audioAnalysisProducer.clearAnalysis(videoId);
    }

    /**
     * 根据主播sec_uid获取已录制该主播的视频
     *
     * @param anchorVideoBo
     * @return
     */
    public R<PageUtils<AnchorVideoVO>> selectVideoBySecUid(AnchorVideoBo anchorVideoBo) {

        R<PageUtils<AnchorVideoVO>> pageUtilsR = anchorVideoProducer.selectVideoBySecUid(anchorVideoBo);
        if(pageUtilsR.getCode() != StatusCode.SUCCESS.getCode() || pageUtilsR.getData() == null) {
            return pageUtilsR;
        }

        List<AnchorVideoVO> videoList = pageUtilsR.getData().getList();
        if(videoList == null || videoList.isEmpty()) {
            return pageUtilsR;
        }

        Set<Long> userIds = videoList.stream().map(AnchorVideoVO::getUserId).collect(Collectors.toSet());
        List<UserDto> userDtos = this.userFeign.listByIds(userIds);
        if(userDtos == null || userDtos.isEmpty()) {
            return pageUtilsR;
        }

        Map<Long, UserDto> userDtoMap = userDtos.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
        for (AnchorVideoVO anchorVideoVo : videoList) {
            anchorVideoVo.setUserName("");
            UserDto userDto = userDtoMap.get(anchorVideoVo.getUserId());
            if(userDto != null) {
                anchorVideoVo.setUserName(userDto.getNickName());
            }
        }

        return pageUtilsR;

    }

    /**
     * 根据视频ID获取分析记录内容
     *
     * @param videoId
     * @return
     */
    public R<AnchorVideoVO> videoAnalysisByVideoId(String videoId) {
        R<AnchorVideoVO> anchorVideoVOR = anchorVideoProducer.videoAnalysisByVideoId(videoId);
        if(anchorVideoVOR.getCode() != StatusCode.SUCCESS.getCode() || anchorVideoVOR.getData() == null) {
            return anchorVideoVOR;
        }

        AnchorVideoVO videoVo = anchorVideoVOR.getData();
        UserDto userDto = this.userFeign.userById(videoVo.getUserId());
        if(userDto != null) {
            videoVo.setUserName(userDto.getNickName());
        }

        return anchorVideoVOR;
    }

    /**
     * 根据user_id获取录制分析
     *
     * @param anchorVideoVO
     * @return
     */
    public R<PageUtils<AnchorVideoVO>> videoAnalysisByUserId(AnchorVideoVO anchorVideoVO) {

        R<PageUtils<AnchorVideoVO>> pageUtilsR = anchorVideoProducer.videoAnalysisByUserId(anchorVideoVO);
        if(pageUtilsR.getCode() != StatusCode.SUCCESS.getCode() || pageUtilsR.getData() == null) {
            return pageUtilsR;
        }

        List<AnchorVideoVO> videoList = pageUtilsR.getData().getList();
        if(videoList == null || videoList.isEmpty()) {
            return pageUtilsR;
        }

        Set<Long> userIds = videoList.stream().map(AnchorVideoVO::getUserId).collect(Collectors.toSet());
        List<UserDto> userDtos = this.userFeign.listByIds(userIds);
        if(userDtos == null || userDtos.isEmpty()) {
            return pageUtilsR;
        }

        Map<Long, UserDto> userDtoMap = userDtos.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
        for (AnchorVideoVO anchorVideoVo : videoList) {
            anchorVideoVo.setUserName("");
            UserDto userDto = userDtoMap.get(anchorVideoVo.getUserId());
            if(userDto != null) {
                anchorVideoVo.setUserName(userDto.getNickName());
            }
        }

        return pageUtilsR;
    }

    /**
     * 获取视频的第一批段落列表
     *
     * @param videoId 视频唯一标识
     * @return
     */
    public R<List<AudioAnalysisVo>> listAnalysisOneByVideoId(String videoId) {

        return R.ok(this.audioAnalysisProducer.listAnalysisOneByVideoId(videoId));
    }

    /**
     * 根据视频videoId获取视频信息
     *
     * @param videoId 视频videoId
     * @return
     */
    public R<AnchorVideoInfoVo> GetByVideoId(String videoId) {
        AnchorVideoInfoVo videoInfoVo = this.anchorVideoProducer.getByVideoId(videoId);

        return R.ok(videoInfoVo);
    }

    /**
     * 根据secUid查询上一场视频（startTime在本场之前且分析完成的最近一场）
     *
     * @param secUid          主播唯一标识
     * @param userId          用户id
     * @param tenantId        租户id
     * @param beforeStartTime 本场开始时间
     * @return 上一场视频信息，没有则返回null
     */
    public AnchorVideoInfoVo getPrevVideo(String secUid, Long userId, Long tenantId, Date beforeStartTime) {
        return this.anchorVideoProducer.getPrevBySecUid(secUid, userId, tenantId, beforeStartTime);
    }

    /**
     * 客户端同步分析数据到服务器
     *
     * @param syncVideoAnalysisBo 分析数据
     * @return
     */
    public R<String> syncVideoAnalysisToServer(SyncVideoAnalysisBo syncVideoAnalysisBo) {
        List<SentenceMarkVo> sentenceMarkVoList = syncVideoAnalysisBo.getSentenceMarkVoList();
        if (sentenceMarkVoList != null && sentenceMarkVoList.size() > 0) {
            int versionNum = this.videoAnalysisRecordProducer.getLastVersion(syncVideoAnalysisBo.getVideoId());
            Long id = SnowflakeManager.nextValue();
            String jsonStr = JSONObject.toJSONString(sentenceMarkVoList);
            String fileName = this.sensitiveWordsProducer.saveAnalysisDataToFile(syncVideoAnalysisBo.getVideoId(), 0, syncVideoAnalysisBo.getTradeId(), versionNum, jsonStr, id);
            // 保存分析记录
            VideoAnalysisRecordBo videoAnalysisRecordBo = new VideoAnalysisRecordBo();
            videoAnalysisRecordBo.setId(id);
            videoAnalysisRecordBo.setUserId(syncVideoAnalysisBo.getUserId());
            videoAnalysisRecordBo.setVideoId(syncVideoAnalysisBo.getVideoId());
            videoAnalysisRecordBo.setTradeId(syncVideoAnalysisBo.getTradeId());
            videoAnalysisRecordBo.setStoreFileName(fileName);
            videoAnalysisRecordBo.setVersion(versionNum);
            videoAnalysisRecordProducer.save(videoAnalysisRecordBo);

            return R.ok();
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "分析数据为空");
    }

    /**
     * 客户端根据租户id获取用户在服务器上的视频列表
     *
     * @param activeTenantId 租户id
     * @return
     */
    public R<List<AnchorVideoInfoVo>> getVideoListByTenantId(Long activeTenantId) {

        // 获取视频
        List<AnchorVideoInfoVo> anchorVideoList = this.anchorVideoProducer.getVideoListByTenantId(activeTenantId, 1);

        // 封装主播信息
        if (anchorVideoList != null && anchorVideoList.size() > 0) {

            for (AnchorVideoInfoVo anchorVideoInfoVo : anchorVideoList) {
                anchorVideoInfoVo.setId(0L);
            }

            List<String> secUids = anchorVideoList.stream().map(AnchorVideoInfoVo::getSecUid).toList();
            List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorUrlProducer.listBySecUids(secUids);

            if (anchorUrlInfoVos != null && anchorUrlInfoVos.size() > 0) {

                for (AnchorVideoInfoVo anchorVideoInfoVo : anchorVideoList) {
                    for (AnchorUrlInfoVo anchorUrlInfoVo : anchorUrlInfoVos) {
                        if (anchorUrlInfoVo.getSecUid().equals(anchorVideoInfoVo.getSecUid())) {
                            anchorUrlInfoVo.setId(0L);
                            anchorVideoInfoVo.setAnchorInfo(anchorUrlInfoVo);
                            break;
                        }
                    }
                }

            }
        }

        return R.ok(anchorVideoList);
    }

    /**
     * 根据视频唯一标识。获取视频信息
     *
     * @param videoId 视频唯一标识
     * @return
     */
    public R<AnchorVideoInfoVo> infoByVideoId(String videoId) {

        AnchorVideoInfoVo videoInfoVo = this.anchorVideoProducer.getByVideoId(videoId);
        if (videoInfoVo != null) {
            videoInfoVo.setId(0L);
            return R.ok(videoInfoVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频不存在");
    }

    /**
     * 根据视频id获取父视频信息
     *
     * @param videoId 视频id
     * @return 父视频信息
     */
    public AnchorVideoInfoVo getParentVideoByVideoId(String videoId) {
        return this.anchorVideoProducer.getParentVideoByVideoId(videoId);
    }

    /**
     * 根据视频唯一标识。获取视频信息
     *
     * @param videoIds
     * @return
     */
    public R<List<AnchorVideoInfoVo>> listByVideoIds(List<String> videoIds) {
        List<AnchorVideoInfoVo> anchorVideoInfoVos = this.anchorVideoProducer.listByVideoIds(videoIds);
        return R.ok(anchorVideoInfoVos);
    }

    /**
     * 查询场次中的第一条视频
     *
     * @param batchNumberIds
     * @return
     */
    public R<List<AnchorVideoInfoVo>> listOneVideoByBatchNumber(List<Long> batchNumberIds) {
        return R.ok(this.anchorVideoProducer.listOneVideoByBatchNumber(batchNumberIds));
    }

    /**
     * 获取云空间视频列表
     *
     * @param cloudVideoListBo 请求参数
     * @return
     */
    public R<PageUtils<AnchorVideoInfoVo>> listCloudVideo(CloudVideoListBo cloudVideoListBo) {

        PageUtils<AnchorVideoInfoVo> pageUtils = anchorVideoProducer.listCloudVideoPage(cloudVideoListBo);

        List<AnchorVideoInfoVo> anchorVideoInfoVoList = pageUtils.getList();

        if (anchorVideoInfoVoList != null && anchorVideoInfoVoList.size() > 0) {
            List<String> secUids = anchorVideoInfoVoList.stream().map(AnchorVideoInfoVo::getSecUid).toList();

            // 主播信息对象赋值
            List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorUrlProducer.listBySecUids(secUids);
            DataUtils.setFieldObject(anchorVideoInfoVoList, "secUid", "anchorInfo", anchorUrlInfoVos, "secUid");

            List<SocketCollectMessageInfoVo> socketCollectMessageInfoVos = this.socketCollectMessageProducer.listByVideoIds(anchorVideoInfoVoList.stream().map(AnchorVideoInfoVo::getVideoId).toList())
                    .stream().peek(temp -> temp.setTotalBarrageNum(NumberUtil.parseInt(temp.getObservationNum(), 0))).toList()
                    ;
            DataUtils.setFieldNameById(anchorVideoInfoVoList, "videoId", "viewersNum", socketCollectMessageInfoVos, "videoId", "totalBarrageNum");
            DataUtils.setFieldNameById(anchorVideoInfoVoList, "videoId", "onlineMaxNum", socketCollectMessageInfoVos, "videoId", "onlineMaxNum");


//            if(anchorUrlInfoVos != null && anchorUrlInfoVos.size() > 0) {
//                for (AnchorVideoInfoVo anchorVideoInfoVo : anchorVideoInfoVoList) {
//                    for (AnchorUrlInfoVo anchorUrlInfoVo : anchorUrlInfoVos) {
//                        if(anchorUrlInfoVo.getSecUid().equals(anchorVideoInfoVo.getSecUid())) {
//                            anchorVideoInfoVo.setAnchorInfo(anchorUrlInfoVo);
//                            break;
//                        }
//                    }
//                }
//            }

        }

        return R.ok("获取成功", pageUtils);
    }

    /**
     * 根据租户id统计存储量
     *
     * @param activeTenantId
     * @return
     */
    public R<Long> statisticsStoreByTenantId(Long activeTenantId) {
        return R.ok(anchorVideoProducer.statisticsStoreByTenantId(activeTenantId));
    }

    /**
     * 客户端获取视频列表
     *
     * @param clientVideoListBo 查询参数
     * @return
     */
    public R<PageUtils<AnchorVideoInfoVo>> clientVideoList(ClientVideoListBo clientVideoListBo) {

        if(clientVideoListBo.getVideoSliceType() == null) {
            clientVideoListBo.setVideoSliceType(VideoSliceTypeEnum.VIDEO.getCode());
        }

        PageUtils<AnchorVideoInfoVo> pageUtils = anchorVideoProducer.clientVideoList(clientVideoListBo);

        List<AnchorVideoInfoVo> list = pageUtils.getList();
        if (list != null && !list.isEmpty()) {
            // 获取主播信息
            Set<String> secUids = list.stream().map(AnchorVideoInfoVo::getSecUid).collect(Collectors.toSet());
            Set<Long> userIds = list.stream().map(AnchorVideoInfoVo::getUserId).collect(Collectors.toSet());
            List<AnchorUrlUserVo> anchorUrlUserVos = this.anchorUrlProducer.listUserAnchorByUserIdsAndSecUids(userIds, clientVideoListBo.getTenantId(), secUids);

            // 基础数据
            Map<String, BasicSettingsVo> basicMap = this.basicSettingsProducer.listBySourceUser(new ArrayList<>(secUids), WordsEnum.basicSettingsType.ANCHOR.getCode(), new ArrayList<>(userIds), clientVideoListBo.getTenantId())
                    .stream()
                    .collect(Collectors.toMap(item -> StrUtil.format("{}_{}_{}", item.getSourceId(), item.getUserId(), item.getTenantId()), Function.identity(), (o1, o2) -> o1));

            // 获取分析记录
            List<String> videoIds = list.stream().map(AnchorVideoInfoVo::getVideoId).toList();
            List<VideoAnalysisRecordInfoVo> videoAnalysisRecordInfoVos = this.videoAnalysisRecordProducer.listByVideoIds(videoIds);
            if(videoAnalysisRecordInfoVos != null && !videoAnalysisRecordInfoVos.isEmpty()) {
                videoAnalysisRecordInfoVos.sort(Comparator.comparing(VideoAnalysisRecordVo::getVersion).reversed());
            }

            // 最高在线人数和观看人数
            List<SocketCollectMessageInfoVo> socketCollectMessageInfoVos = socketCollectMessageProducer.listByVideoIds(videoIds);
            Map<String, SocketCollectMessageInfoVo> socketMap = new HashMap<>();
            if (socketCollectMessageInfoVos != null && !socketCollectMessageInfoVos.isEmpty()){
                socketMap = socketCollectMessageInfoVos.stream().collect(Collectors.toMap(SocketCollectMessageVo::getVideoId, Function.identity(), (a, b) -> a));
            }

            // 1. 优先获取本段看板数据（VideoDataViewingParagraph）
            List<VideoDataViewingConfuseInfoVo> paragraphInfoVoList = this.videoDataViewingParagraphRse.listByVideoIds(videoIds);
            Map<String, VideoDataViewingConfuseInfoVo> paragraphMap = new HashMap<>();
            if (paragraphInfoVoList != null && !paragraphInfoVoList.isEmpty()) {
                paragraphMap = paragraphInfoVoList.stream().collect(Collectors.toMap(VideoDataViewingConfuseVo::getVideoId, Function.identity(), (a, b) -> a));
            }

            // 2. 获取数据看板（VideoDataViewingConfuse）
            List<VideoDataViewingConfuseInfoVo> dataViewingConfuseInfoVoList = this.videoDataViewingConfuseProducer.listByVideoIdsAndStatus(videoIds, DataViewingStatusEnum.PULL_SUCCESS.getStatus());
            Map<String, VideoDataViewingConfuseInfoVo> dataViewingMap = new HashMap<>();
            if(dataViewingConfuseInfoVoList != null && dataViewingConfuseInfoVoList.size() > 0) {
                dataViewingMap = dataViewingConfuseInfoVoList.stream().collect(Collectors.toMap(VideoDataViewingConfuseVo::getVideoId, Function.identity(), (a, b) -> a));
            }

            // 是否上传诊断报告
            Map<String, AnchorVideoDetailInfoVo> anchorVideoDetailMap = anchorVideoDetailProducer.listByVideoIds(videoIds).stream()
                    .collect(Collectors.toMap(AnchorVideoDetailInfoVo::getVideoId, Function.identity(), (a, b) -> a));

            // 查询是否有AI优化目的记录
            Set<String> existAiOptimizePurposeIds = aiOptimizePurposeRse.listExistSourceIds(videoIds);

            // 查询星标信息
            List<SourceStarVo> sourceStarVos = sourceStarRse.listBySourceIds(videoIds, SourceStarTypeEnum.VIDEO.getCode());
            Map<String, SourceStarVo> sourceStarMap = sourceStarVos.stream().collect(Collectors.toMap(SourceStarVo::getSourceId, Function.identity(), (a, b) -> a));

            // 批量查询数据诊断已读状态
            Map<String, Integer> dataDiagnosisReadMap = new HashMap<>();
            List<DiagnosisCueInfoVo> diagnosisCueList = diagnosisCueFeign.listBySourceIds(
                    videoIds, AiEnums.diagnosisSourceType.VIDEO.getCode(),
                    clientVideoListBo.getUserId(), clientVideoListBo.getTenantId());
            if (ObjectUtil.isNotEmpty(diagnosisCueList)) {
                Map<String, List<DiagnosisCueInfoVo>> grouped = diagnosisCueList.stream()
                        .filter(e -> ObjectUtil.equals(e.getDiagnosisType(), AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode()))
                        .collect(Collectors.groupingBy(DiagnosisCueInfoVo::getSourceId));
                grouped.forEach((vid, cues) -> {
                    cues.sort(Comparator.comparing(DiagnosisCueInfoVo::getCreateDate).reversed());
                    for (DiagnosisCueInfoVo e : cues) {
                        if (ObjectUtil.equals(e.getQaStatus(), AiEnums.qaStatus.SUCCESS.getCode())) {
                            dataDiagnosisReadMap.put(vid, e.getIsRead());
                            break;
                        }
                    }
                });
            }

            for (AnchorVideoInfoVo anchorVideoInfoVo : list) {
                // 设置主播信息
                if (anchorUrlUserVos != null && !anchorUrlUserVos.isEmpty()) {
                    for (AnchorUrlUserVo anchorUrlUserVo : anchorUrlUserVos) {
                        AnchorUrlInfoVo anchorInfo = anchorUrlUserVo.getAnchorInfo();
                        if(anchorVideoInfoVo.getSecUid().equals(anchorUrlUserVo.getAnchorUrlSecUid()) && anchorVideoInfoVo.getUserId().equals(anchorUrlUserVo.getUserId())) {
                            anchorVideoInfoVo.setAnchorInfo(anchorInfo);
                            break;
                        }
                    }
                }

                // 设置基础设置
                anchorVideoInfoVo.setBasicSettingsVo(basicMap.get(StrUtil.format("{}_{}_{}", anchorVideoInfoVo.getSecUid(), anchorVideoInfoVo.getUserId(), anchorVideoInfoVo.getTenantId())));

                // 设置分析记录
                if (videoAnalysisRecordInfoVos != null && !videoAnalysisRecordInfoVos.isEmpty()) {
                    for (VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo : videoAnalysisRecordInfoVos) {
                        if (anchorVideoInfoVo.getVideoId().equals(videoAnalysisRecordInfoVo.getVideoId())) {
                            VideoAnalysisRecordInfoVo recordInfoVo = new VideoAnalysisRecordInfoVo();
                            recordInfoVo.setCruxWordNum(videoAnalysisRecordInfoVo.getCruxWordNum());
                            recordInfoVo.setSensitiveWordNum(videoAnalysisRecordInfoVo.getSensitiveWordNum());
                            recordInfoVo.setContentNum(videoAnalysisRecordInfoVo.getContentNum());
                            anchorVideoInfoVo.setRecordInfo(recordInfoVo);
                            break;
                        }
                    }
                }

                // 设置数据看板数据（优先级：VideoDataViewingParagraph > VideoDataViewingConfuse > SocketCollectMessage）
                int viewersNum = -1;
                // 1. 优先从VideoDataViewingParagraph获取数据
                VideoDataViewingConfuseInfoVo paragraphInfoVo = paragraphMap.get(anchorVideoInfoVo.getVideoId());
                if (paragraphInfoVo != null) {
                    viewersNum = ObjectUtil.defaultIfNull(paragraphInfoVo.getTotalWatchNum(), viewersNum);
                    anchorVideoInfoVo.setExistDataBoard(1);
                    anchorVideoInfoVo.setVolumeStart(paragraphInfoVo.getVolumeStart());
                    anchorVideoInfoVo.setVolumeEnd(paragraphInfoVo.getVolumeEnd());
                } else {
                    // 2. 如果没有本段数据，从VideoDataViewingConfuse获取
                    VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = dataViewingMap.get(anchorVideoInfoVo.getVideoId());
                    if (videoDataViewingConfuseInfoVo != null) {
                        viewersNum = ObjectUtil.defaultIfNull(videoDataViewingConfuseInfoVo.getTotalWatchNum(), viewersNum);
                        anchorVideoInfoVo.setExistDataBoard(1);
                        anchorVideoInfoVo.setVolumeStart(videoDataViewingConfuseInfoVo.getVolumeStart());
                        anchorVideoInfoVo.setVolumeEnd(videoDataViewingConfuseInfoVo.getVolumeEnd());
                    } else {
                        anchorVideoInfoVo.setExistDataBoard(0);
                        anchorVideoInfoVo.setVolumeStart(-1);
                        anchorVideoInfoVo.setVolumeEnd(-1);
                    }
                }

                // 3. 如果还没有观看人数，从SocketCollectMessage获取
                SocketCollectMessageInfoVo socketCollectMessageInfoVo = socketMap.get(anchorVideoInfoVo.getVideoId());
                if (socketCollectMessageInfoVo != null) {
                    if (viewersNum == -1) {
                        String observationNumTemp = ObjectUtil.defaultIfNull(socketCollectMessageInfoVo.getObservationNum(), "0");
                        viewersNum = CommonUtils.conversionObservationNum(NumberUtil.parseInt(observationNumTemp, 0), socketCollectMessageInfoVo.getOnlineMaxNum());
                    }
                    anchorVideoInfoVo.setOnlineMaxNum(socketCollectMessageInfoVo.getOnlineMaxNum());
                    int existBarrage = (socketCollectMessageInfoVo.getTotalBarrageNum() != null && socketCollectMessageInfoVo.getTotalBarrageNum() > 0) ? 1 : 0;
                    anchorVideoInfoVo.setExistBarrage(existBarrage);
                }else{
                    if (anchorVideoInfoVo.getViewersNum() == null) anchorVideoInfoVo.setViewersNum(-1);
                    anchorVideoInfoVo.setOnlineMaxNum(-1);
                    anchorVideoInfoVo.setExistBarrage(0);
                    anchorVideoInfoVo.setHasDiagnosisReport(0);
                }

                AnchorVideoDetailInfoVo videoDetailInfoVo = anchorVideoDetailMap.get(anchorVideoInfoVo.getVideoId());
                if (videoDetailInfoVo != null) {
                    anchorVideoInfoVo.setHasDiagnosisReport(videoDetailInfoVo.getHasDiagnosisReport());
                    anchorVideoInfoVo.setDiagnosisOssName(videoDetailInfoVo.getDiagnosisOssName());
                    anchorVideoInfoVo.setHasDataDiagnosisReport(videoDetailInfoVo.getHasDataDiagnosisReport());
                    anchorVideoInfoVo.setDataDiagnosisOssName(videoDetailInfoVo.getDataDiagnosisOssName());
                }
                anchorVideoInfoVo.setViewersNum(viewersNum);
                anchorVideoInfoVo.setObservationNum(anchorVideoInfoVo.getViewersNum());

                // 设置是否有AI优化目的
                anchorVideoInfoVo.setHasAiOptimizePurpose(existAiOptimizePurposeIds.contains(anchorVideoInfoVo.getVideoId()) ? 1 : 0);

                // 设置是否设置了星标
                anchorVideoInfoVo.setHasStar(0);
                SourceStarVo sourceStarVo = sourceStarMap.get(anchorVideoInfoVo.getVideoId());
                if(sourceStarVo != null) {
                    anchorVideoInfoVo.setHasStar(1);
                    anchorVideoInfoVo.setSourceStarInfo(sourceStarVo);
                }

                // 设置数据诊断已读状态
                anchorVideoInfoVo.setIsDataDiagnosisRead(dataDiagnosisReadMap.get(anchorVideoInfoVo.getVideoId()));
            }

        }

        // 设置客户端视频列表的值
        setPostValueClientVideoList(list);
        return R.ok("获取成功", pageUtils);
    }

    /**
     * 设置客户端视频列表的值
     *
     * @param list 视频列表
     */
    private void setPostValueClientVideoList(List<AnchorVideoInfoVo> list) {
        if (ObjectUtil.isEmpty(list)) {
            return;
        }

        // 获取多少天后弹幕消失
        Integer barrageMaxExpiredTime = null;
        SystemKvInfoVo kv = systemKvProducer.getByKey("barrage_max_expired_time");
        if (kv != null && ObjectUtil.isNotEmpty(kv.getKvValue())) {
            barrageMaxExpiredTime = NumberUtil.parseInt(kv.getKvValue(), barrageMaxExpiredTime);
        }
        for (AnchorVideoInfoVo item : list) {
            if (ObjectUtil.isNotEmpty(item.getEndTime()) && barrageMaxExpiredTime != null && DateUtil.between(item.getEndTime(), new Date(), DateUnit.DAY) >= barrageMaxExpiredTime) {
                item.setExistBarrage(0);
            }
        }

    }

    /**
     * 客户端删除视频
     *
     * @param ids      视频uuid集合
     * @param tenantId 租户id
     * @param userId   用户id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> clientDeleteVideo(List<String> ids, Long tenantId, Long userId) {

        if (ids == null || ids.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "输入的数据为空");
        }

        List<String> delIds = anchorVideoProducer.getAllowDeleteVideo(ids, tenantId, userId);
        if (delIds == null || delIds.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
        }

        List<AnchorVideoInfoVo> videoInfoVoList = this.anchorVideoProducer.listByVideoIds(delIds);
        if (videoInfoVoList == null || videoInfoVoList.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
        }

//        // 筛选出已上传云空间和未上传云空间的视频列表
//        List<AnchorVideoInfoVo> notUploadList = videoInfoVoList.stream().filter(item -> item.getUploadStatus() == 0).toList();
//        List<AnchorVideoInfoVo> uploadList = videoInfoVoList.stream().filter(item -> item.getUploadStatus() == 1).toList();
//
//        if(notUploadList.size() > 0) {
//            // 未上传云空间的
//            List<String> videoIds = notUploadList.stream().map(AnchorVideoInfoVo::getVideoId).toList();
//            anchorVideoProducer.removeByVoidId(videoIds);
//        }
//        if(uploadList.size() > 0) {
//            // 已上传云空间，改删除状态
//            List<String> videoIds = uploadList.stream().map(AnchorVideoInfoVo::getVideoId).toList();
//            anchorVideoProducer.batchUpdateVideoDelStatus(videoIds, 1);
//        }

        // 改删除状态
        anchorVideoProducer.batchUpdateVideoDelStatus(ids, 1);
        // 改关联的对比记录的删除状态
        syncContrastProducer.batchUpdateContrastDelStatusByVideoIds(ids, 1);

        // 删除关联的运营/违规助手信息
        this.clientAiFavProducer.removeByResourceIds(ids);

        // 删除关联的切片信息
        this.videoSliceRse.removeBySourceIds(ids);

        return R.ok("删除成功");
    }

    /**
     * 批量保存巨量拉取的视频记录
     *
     * @param videoList 视频列表
     * @param userId    用户id
     * @param tenantId  租户id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<List<AnchorVideoInfoVo>> savePulledVideos(List<AnchorVideoInfoBo> videoList, Long userId, Long tenantId) {
        List<AnchorVideoInfoVo> replacedVideos = anchorVideoProducer.savePulledVideos(videoList, userId, tenantId);
        if (CollUtil.isNotEmpty(replacedVideos)) {
            List<String> replacedVideoIds = replacedVideos.stream()
                .map(AnchorVideoInfoVo::getVideoId).toList();
            syncContrastProducer.batchUpdateContrastDelStatusByVideoIds(replacedVideoIds, 1);
        }
        return R.ok(replacedVideos);
    }

    /**
     * 客户端删除云空间视频
     *
     * @param videoId 视频uuid
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> clientDeleteCloudVideo(String videoId) {

        AnchorVideoInfoVo videoInfoVo = this.anchorVideoProducer.getByVideoId(videoId);
        if (videoInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
        }

        List<String> videoIds = new LinkedList<>();
        videoIds.add(videoId);

        // 修改视频的上传状态
        this.anchorVideoProducer.batchUpdateVideoUploadStatus(videoIds, 0);

        if (videoInfoVo.getDeleteStatus() == 1) {
            // 修改视频的删除状态
            this.anchorVideoProducer.batchUpdateVideoDelStatus(videoIds, 2);
        }

        // 删除关联的云空间对比复盘
        this.syncContrastProducer.deleteCloudByVideoId(videoId);

        return R.ok("删除成功");
    }

    /**
     * 客户端批量删除云空间视频。
     * 与 {@link #clientDeleteCloudVideo(String)} 的业务逻辑一致，但所有 DB 操作都按集合批量执行——
     * 与入参条数 N 无关，恒为常数级 SQL（无 N+1）。本方法仅做本地状态变更与对比记录清理；
     * 腾讯云 VOD 的源文件删除由上层 Logic 在调用本方法前完成。
     *
     * @param videoIds       已通过权限/上传状态过滤、且 VOD 源文件删除成功的视频 uuid 集合（不可为空）
     * @param hardDeleteIds  上述集合中原 deleteStatus==1 的子集（需推进到 2，可为空）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clientBatchDeleteCloudVideo(List<String> videoIds, List<String> hardDeleteIds) {

        if (videoIds == null || videoIds.isEmpty()) {
            return;
        }

        // 1) 一次 UPDATE IN：所有视频的 uploadStatus 改回 0（与单条版本一致：会顺带清空 play_url / share_url / cloud_store）
        this.anchorVideoProducer.batchUpdateVideoUploadStatus(videoIds, 0);

        // 2) 一次 UPDATE IN（仅当存在原 deleteStatus==1 的子集）：把这些视频的 deleteStatus 由 1 推进到 2
        if (hardDeleteIds != null && !hardDeleteIds.isEmpty()) {
            this.anchorVideoProducer.batchUpdateVideoDelStatus(hardDeleteIds, 2);
        }

        // 3) 一次 SELECT IN + 一次 updateBatchById：清理这些视频关联的云空间对比复盘分享
        this.syncContrastProducer.deleteCloudByVideoIds(videoIds);
    }

    /**
     * 获取用户视频列表
     *
     * @param listUserVideoByConditionBo 查询参数
     * @return
     */
    public R<List<AnchorVideoInfoVo>> listUserVideo(ListUserVideoByConditionBo listUserVideoByConditionBo) {

        List<AnchorVideoInfoVo> videoInfoVos = anchorVideoProducer.listUserVideo(listUserVideoByConditionBo);

        return R.ok(videoInfoVos);
    }

    /**
     * 查询最近直播场次（主播下播后平台数据补采集专用）。
     *
     * @param userId      用户id
     * @param tenantId    租户id
     * @param startTimeGe 开播时间下限（格式 yyyy-MM-dd HH:mm:ss），为空时不加时间过滤
     * @return 场次代表视频列表
     */
    public R<List<AnchorVideoInfoVo>> listRecentLiveSessions(Long userId, Long tenantId, String startTimeGe) {

        List<AnchorVideoInfoVo> videoInfoVos = anchorVideoProducer.listRecentLiveSessions(userId, tenantId, startTimeGe);

        return R.ok(videoInfoVos);
    }

    /**
     * 修改视频的分析状态
     *
     * @param updateVideoAnalysisStatusBo 修改参数
     * @return
     */
    public R<String> updateVideoAnalysisStatus(UpdateVideoAnalysisStatusBo updateVideoAnalysisStatusBo) {

        anchorVideoProducer.updateVideoAnalysisStatus(updateVideoAnalysisStatusBo);
        return R.ok();
    }

    /**
     * 修改视频的上传状态
     *
     * @param updateVideoUploadStatusBo 修改参数
     * @return
     */
    public R<String> updateVideoUploadStatus(UpdateVideoUploadStatusBo updateVideoUploadStatusBo) {

        anchorVideoProducer.updateVideoUploadStatus(updateVideoUploadStatusBo);
        return R.ok();
    }

    /**
     * 客户端根据视频唯一标识。获取视频信息
     *
     * @param videoId  视频唯一标识
     * @param userId   用户id
     * @param tenantId 租户id
     * @return
     */
    public R<AnchorVideoInfoVo> clientGetVideoByVideoId(String videoId, Long userId, Long tenantId) {

        AnchorVideoInfoVo videoInfoVo = anchorVideoProducer.clientGetVideoByVideoId(videoId, userId, tenantId);
        if (videoInfoVo != null) {
            // 设置是否有ai优化目的
            AiOptimizePurposeVo aiOptimizePurposeVo = aiOptimizePurposeRse.getBySourceId(videoInfoVo.getVideoId());
            videoInfoVo.setHasAiOptimizePurpose(aiOptimizePurposeVo == null ? 0 : 1);

            return R.ok(videoInfoVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频不存在");
    }

    /**
     * 根据 batchNumber 查询整场直播的第一条记录（paragraph=0），返回该场次的原始 startTime
     *
     * @param batchNumber 直播场次 room_id
     * @return 场次第一条视频信息（含 startTime/endTime 等）
     */
    public R<AnchorVideoInfoVo> getLiveSessionByBatchNumber(String batchNumber, Long tenantId) {
        AnchorVideoInfoVo vo = anchorVideoProducer.getLiveSessionByBatchNumber(batchNumber, tenantId);
        if (vo != null) {
            return R.ok(vo);
        }
        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "场次不存在");
    }

    /**
     * 批量修改视频的大小时长
     *
     * @param updateVideoUploadStatusBoList 视频大小时长集合
     * @return
     */
    public R<String> updateVideoSizeDuration(List<UpdateVideoSizeDurationBo> updateVideoUploadStatusBoList) {

        anchorVideoProducer.updateVideoSizeDuration(updateVideoUploadStatusBoList);

        return R.ok();
    }

    /**
     * 初始化视频表和文件，将分析中的改成分析失败
     *
     * @return
     */
    public R<String> initVideoAndFileAnalysisStatus(Long userId, Long tenantId) {

        this.anchorVideoProducer.initVideoAnalysisStatus(userId, tenantId);
        this.uploadFileProducer.initFileAnalysisStatus(userId, tenantId);

        return R.ok();
    }

    /**
     * 保存或修改视频信息
     *
     * @param anchorVideoInfoBo 视频信息
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> saveOrUpdateVideo(AnchorVideoInfoBo anchorVideoInfoBo) {

        // 保存或修改视频信息
        this.anchorVideoProducer.saveOrUpdate(anchorVideoInfoBo);

        // 新增视频详情
        AnchorVideoDetailBo detailBo = new AnchorVideoDetailBo();
        detailBo.setVideoId(anchorVideoInfoBo.getVideoId());
        anchorVideoDetailProducer.getAndSave(detailBo);
        return R.ok();
    }

    /**
     * 客户端根据视频id集合获取视频列表
     *
     * @param ids      视频uuid集合
     * @param userId   用户id
     * @param tenantId 租户id
     * @return
     */
    public R<List<AnchorVideoInfoVo>> clientListVideoByVideoIds(List<String> ids, Long userId, Long tenantId) {

        if(ids == null || ids.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频id集合不能为空");
        }

        List<AnchorVideoInfoVo> videoInfoVoList = this.anchorVideoProducer.clientListVideoByVideoIds(ids, userId, tenantId);

        return R.ok(videoInfoVoList);
    }

    /**
     * 同步本地视频、文件、对比到服务器
     *
     * @param syncLocalDataToServerBo id参数集合
     * @return
     */
    public R<String> syncLocalDataToServer(SyncLocalDataToServerBo syncLocalDataToServerBo) {

        this.anchorVideoProducer.syncLocalDataToServer(syncLocalDataToServerBo);

        this.redisTemplate.opsForValue().set("replay:temp:sync-local-data:" + syncLocalDataToServerBo.getUserId() + syncLocalDataToServerBo.getTenantId(), "1");

        return R.ok();
    }

    /**
     * 获取历史批次视频列表
     *
     * @param videoId  视频ID
     * @param dataType 数据类型:0-截图,1-看板
     * @param limit    返回条数限制
     * @return 历史批次视频列表
     */
    public R<List<HistoryBatchNumberVideoListVo>> historyBatchNumberVideoList(String videoId, Integer dataType, Integer uploadStatus, Integer limit) {
        // 参数校验
        RRException.isNotEmpty(dataType, "数据类型不能为空");
        if (dataType > 2) {
            RRException.create("数据类型错误");
        }

        List<HistoryBatchNumberVideoListVo> result = this.anchorVideoProducer.historyBatchNumberList(videoId, dataType, uploadStatus, limit);
        // 处理视频名称
        if (ObjectUtil.isNotEmpty(result)) {
            result.forEach(item -> {
                if (ObjectUtil.isNotEmpty(item.getVideoName())) {
                    String videoName = item.getVideoName();
                    videoName = videoName.substring(0, videoName.lastIndexOf("."));
                    String[] split = videoName.split("_");
                    if (split.length >= 2) {
                        videoName = split[split.length - 2] + "_" + split[split.length - 1];
                    }
                    item.setVideoName(videoName);
                }
            });
        }

        return R.ok(result);
    }

    /**
     * 判断是否第一个视频
     *
     * @param videoId
     * @return
     */
    public boolean hasOneVideo() {
        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if (userCacheVo == null) {
            return false;
        }
        return this.anchorVideoProducer.hasOneVideo(userCacheVo.getId(), userCacheVo.getActiveTenantId());
    }

    /**
     * 分析视频到云空间
     *
     * @param shareVideoCloudBo 视频信息
     * @return 分享地址
     */
    public R<String> shareVideoToCloud(ShareVideoCloudBo shareVideoCloudBo) {

        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if (userCacheVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户信息不存在");
        }

        AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoRse.infoUserVideoByVideoId(shareVideoCloudBo.getVideoId(), userCacheVo.getId());
        if(anchorVideoInfoVo == null || anchorVideoInfoVo.getDeleteStatus() != 0) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
        }
        if (anchorVideoInfoVo.getUploadStatus() == 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请勿重复上传");
        }

        ShareVideoCloudDto shareVideoCloudDto = BeanConvertUtils.convert(shareVideoCloudBo, ShareVideoCloudDto.class);
        shareVideoCloudDto.setId(anchorVideoInfoVo.getId());
        shareVideoCloudDto.setCloudRemarks(shareVideoCloudBo.getCloudRemarks());
        String videoRename = anchorVideoInfoVo.getVideoRename();
        if(StringUtils.hasText(videoRename)) {
            shareVideoCloudDto.setCloudRename(videoRename.substring(0, videoRename.lastIndexOf(".")));
        }
        String shareUrl = this.anchorVideoRse.shareVideoToCloud(shareVideoCloudDto);

//        // 推送视频数据到governance
//        try {
//            pushGovernanceVideo(shareVideoCloudBo.getVideoId());
//        } catch (Exception e) {
//            log.error("推送视频数据到governance失败", e);
//        }

        return R.ok("分享成功", shareUrl);
    }

    /**
     * 根据用户ID列表获取每个用户本月的录制视频数量
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户视频数量列表
     */
    public R<List<UserVideoCountDto>> getMonthlyVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        List<UserVideoCountDto> videoCountList = anchorVideoRse.getMonthlyVideoCountByUserIdsAndTenantId(userIds, tenantId);
        return R.ok(videoCountList);
    }

    /**
     * 根据用户ID列表获取每个用户昨日的录制视频数量
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户昨日视频数量列表
     */
    public R<List<UserVideoCountDto>> getYesterdayVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        List<UserVideoCountDto> videoCountList = anchorVideoRse.getYesterdayVideoCountByUserIdsAndTenantId(userIds, tenantId);
        return R.ok(videoCountList);
    }

    /**
     * 获取子账号录制视频列表
     *
     * @param subVideoListBo 子账号id
     * @return 录制视频列表
     */
    public R<PageUtils<SubUserVideoListVo>> getSubUserYesterdayVideoList(SubVideoListBo subVideoListBo) {

        if(subVideoListBo.getUserId() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "子账号id不能为空");
        }

        R<UserCacheVo> userR = this.userFeign.getLocalUser();
        if(userR.getData() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
        }
        subVideoListBo.setTenantId(userR.getData().getActiveTenantId());

        ClientVideoListBo clientVideoListBo = BeanConvertUtils.convert(subVideoListBo, ClientVideoListBo.class);
        // 设置昨日日期为查询条件
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String yesterdayStr = yesterday.format(formatter);
        clientVideoListBo.setRecordStartDate(yesterdayStr);
        clientVideoListBo.setRecordEndDate(yesterdayStr);
        clientVideoListBo.setVideoSliceType(VideoSliceTypeEnum.VIDEO.getCode());

        return getSubUserVideoList(clientVideoListBo);

    }

    private R<PageUtils<SubUserVideoListVo>> getSubUserVideoList(ClientVideoListBo clientVideoListBo) {

        // 分页获取视频列表
        PageUtils<AnchorVideoInfoVo> pageUtils = anchorVideoProducer.clientVideoList(clientVideoListBo);

        PageUtils<SubUserVideoListVo> resultPage = new PageUtils<>();
        BeanUtils.copyProperties(pageUtils, resultPage);

        List<AnchorVideoInfoVo> videoList = pageUtils.getList();
        if(videoList == null || videoList.isEmpty()) {
            return R.ok(resultPage);
        }

        // 获取视频的数据看板
        List<String> videoIds = videoList.stream().map(AnchorVideoInfoVo::getVideoId).toList();
        List<VideoDataViewingConfuseInfoVo> dataViewingList = this.videoDataViewingConfuseProducer.listByVideoIdsAndStatus(videoIds, DataViewingStatusEnum.PULL_SUCCESS.getStatus());
        Map<String, VideoDataViewingConfuseInfoVo> viewingMap = ObjectUtil.isEmpty(dataViewingList) ? new HashMap<>() : dataViewingList.stream().collect(Collectors.toMap(VideoDataViewingConfuseInfoVo::getVideoId, entity -> entity, (e1, e2) -> e1));

        // 获取视频的小结数据
        List<VideoTextNotes> notesEntities = this.videoTextNotesRse.listBySourceIdsAndNotesType(videoIds, 2);
        Map<String, Integer> notesMap = ObjectUtil.isEmpty(notesEntities) ? new HashMap<>() : notesEntities.stream().collect(Collectors.toMap(VideoTextNotes::getSourceId, notes -> 1, (e1, e2) -> e1));

        // 获取视频的主播信息
        Set<String> secUidList = videoList.stream().map(AnchorVideoInfoVo::getSecUid).collect(Collectors.toSet());
        List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorRse.listBySecUids(secUidList);
        Map<String, AnchorUrlInfoVo> anchorMap = ObjectUtil.isEmpty(anchorUrlInfoVos) ? new HashMap<>() : anchorUrlInfoVos.stream().collect(Collectors.toMap(AnchorUrlInfoVo::getSecUid, Function.identity()));

        // 封装数据
        List<SubUserVideoListVo> listVos = videoList.stream().map(item -> {
            SubUserVideoListVo vo = BeanConvertUtils.convert(item, SubUserVideoListVo.class);
            // 填充销售额区间和观看人次
            VideoDataViewingConfuseInfoVo viewingData = viewingMap.get(vo.getVideoId());
            if (viewingData != null) {
                vo.setVolumeStart(viewingData.getVolumeStart());
                vo.setVolumeEnd(viewingData.getVolumeEnd());
                vo.setTotalWatchNum(viewingData.getTotalWatchNum());
            }
            // 填充是否有小结
            vo.setHasNotes(notesMap.getOrDefault(vo.getVideoId(), 0));

            // 填充主播信息
            vo.setAnchorInfo(anchorMap.getOrDefault(vo.getSecUid(), null));

            return vo;
        }).collect(Collectors.toList());

        // 如果没有看板数据，从websocket数据中取观看人次
        List<String> notWatchNumVideoIds = listVos.stream().filter(item -> item.getTotalWatchNum() == null).map(AnchorVideoInfoVo::getVideoId).toList();
        if(!notWatchNumVideoIds.isEmpty()) {
            List<SocketCollectMessageInfoVo> socketCollectMessageInfoVos = this.socketCollectMessageProducer.listByVideoIds(notWatchNumVideoIds);
            if(socketCollectMessageInfoVos != null && !socketCollectMessageInfoVos.isEmpty()) {
                Map<String, SocketCollectMessageInfoVo> socketCollectMap = socketCollectMessageInfoVos.stream().collect(Collectors.toMap(SocketCollectMessageVo::getVideoId, Function.identity()));
                for (SubUserVideoListVo subUserVideoListVo : listVos) {
                    SocketCollectMessageInfoVo collectMessageInfoVo = socketCollectMap.get(subUserVideoListVo.getVideoId());
                    if(collectMessageInfoVo != null) {
                        subUserVideoListVo.setTotalWatchNum(Integer.valueOf(collectMessageInfoVo.getTotalOnlineNum()));
                    }
                }
            }
        }

        resultPage.setList(listVos);

        return R.ok(resultPage);
    }

    /**
     * 获取子账号昨日有小结数据的视频列表
     *
     * @param subVideoListBo 查询参数
     * @return 录制视频列表
     */
    public R<PageUtils<SubUserYesterdayNotesVideoListVo>> getSubUserYesterdayNotesVideoList(SubVideoListBo subVideoListBo) {
        if(subVideoListBo.getUserId() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "子账号id不能为空");
        }

        R<UserCacheVo> userR = this.userFeign.getLocalUser();
        UserCacheVo user = userR.getData();
        if(user == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
        }
        subVideoListBo.setTenantId(user.getActiveTenantId());

        // 查当前子账号昨日小结
        List<String> videoIds = this.videoTextNotesRepositoryService.getYesterdayReviewNotesSourceIdsByUserId(subVideoListBo.getUserId(), user.getActiveTenantId());
        if(videoIds == null || videoIds.isEmpty()) {
            PageUtils<SubUserYesterdayNotesVideoListVo> pageUtils = new PageUtils<>(subVideoListBo.getPage(), subVideoListBo.getLimit());
            return R.ok(pageUtils);
        }

        ClientVideoListBo clientVideoListBo = BeanConvertUtils.convert(subVideoListBo, ClientVideoListBo.class);
        clientVideoListBo.setVideoIdList(videoIds);
        // 分页获取视频列表
        PageUtils<AnchorVideoInfoVo> pageUtils = anchorVideoProducer.clientVideoList(clientVideoListBo);

        PageUtils<SubUserYesterdayNotesVideoListVo> resultPage = new PageUtils<>();
        BeanUtils.copyProperties(pageUtils, resultPage);

        List<AnchorVideoInfoVo> videoList = pageUtils.getList();
        if(videoList == null || videoList.isEmpty()) {
            return R.ok(resultPage);
        }

        // 获取视频的主播信息
        Set<String> secUidList = videoList.stream().map(AnchorVideoInfoVo::getSecUid).collect(Collectors.toSet());
        List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorRse.listBySecUids(secUidList);
        Map<String, AnchorUrlInfoVo> anchorMap = ObjectUtil.isEmpty(anchorUrlInfoVos) ? new HashMap<>() : anchorUrlInfoVos.stream().collect(Collectors.toMap(AnchorUrlInfoVo::getSecUid, Function.identity()));

        // 封装数据
        List<SubUserYesterdayNotesVideoListVo> listVos = videoList.stream().map(item -> {
            SubUserYesterdayNotesVideoListVo vo = BeanConvertUtils.convert(item, SubUserYesterdayNotesVideoListVo.class);
            // 填充主播信息
            vo.setAnchorInfo(anchorMap.getOrDefault(vo.getSecUid(), null));

            return vo;
        }).collect(Collectors.toList());

        resultPage.setList(listVos);

        return R.ok(resultPage);

    }

    /**
     * 批量将视频的本地视频删除标识改为已删除
     *
     * @param videoIds 视频videoId集合
     * @return 录制视频列表
     */
    public R<String> deleteLocalVideoByIds(List<String> videoIds) {

        this.anchorVideoRse.deleteLocalVideoByIds(videoIds);

        return R.ok();
    }

    /**
     * 获取推荐行业的提示词
     * @param sourceType 类型
     * @param sourceId 来源id
     * @param fun 视频内容
     * @return 提示词
     */
    public String getIndustryPrompt(Integer sourceType, String sourceId, BiFunction<Integer, String, AnalysisResultVo> fun, TreePrinterWithMap.TreePrintResult treePrintResult) {
        // 获取视频的音转义的文字
        AnalysisResultVo analysisResultVo = fun.apply(sourceType, sourceId);

        if (ObjectUtil.isEmpty(analysisResultVo) || ObjectUtil.isEmpty(analysisResultVo.getSentenceMarkVos())) {
            throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "内容不存在");
        }

        // 获取推荐行业的提示词字数
        int num = 20000;
        SystemKvInfoVo cueWordNum = systemKvProducer.getByKey("suggest_trade_cueWord_num");
        if (ObjectUtil.isNotEmpty(cueWordNum)) {
            num = NumberUtil.parseInt(cueWordNum.getKvValue(), num);
        }
        String template = """
                任务：根据以下直播内容描述，从 “行业分类树形结构” 中匹配最精准的最小行业（终端子行业），仅输出匹配结果的序号。
                内容输出的要求：仅输出行业对应的序号，不要添加其他文字。
                
                一、直播内容描述
                {}
                
                二、行业分类树形结构
                {}
                
                三、输出要求
                仅输出行业对应的序号，不要添加其他文字。
                """;
        num = num - template.length();

        if (num <= 0) {
            throw new BusinessException(StatusCode.OPERATION_EX.getCode(), "提示词字数设置过小，请联系管理员调大");
        }

        // 获取全文
        StringBuilder sb = new StringBuilder();
        for (SentenceMarkVo sentenceMarkVo : analysisResultVo.getSentenceMarkVos()) {
            if (ObjectUtil.isEmpty(sentenceMarkVo) || ObjectUtil.isEmpty(sentenceMarkVo.getContent())) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            if (sentenceMarkVo.getContent().length() > num && num > 0) {
                sb.append(sentenceMarkVo.getContent(), 0, num);
            }
            num -= sentenceMarkVo.getContent().length();
            if (num < 0) {
                break;
            }
            sb.append(sentenceMarkVo.getContent());
        }

        // 拼接
        if (sb.isEmpty()) {
            throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "视频内容不存在");
        }

        return StrUtil.format(template, sb.toString(), treePrintResult.treeText());
    }

    /**
     * 获取视频的行业推荐id
     *
     * @param sourceType 来源类型
     * @param sourceId   来源id
     * @return 推荐的行业id
     */
    private Long getTradeIdBySource(Integer sourceType, String sourceId) {
        Long tradeId = null;
        if (sourceType == 0) {
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(sourceId);
            if (video == null) {
                throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "视频不存在");
            }

            // 只能有一次行业推荐
            AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlProducer.infoBySecUid(video.getSecUid());
            if (anchorUrlInfoVo != null) {
                if (ObjectUtil.isNotEmpty(anchorUrlInfoVo.getSystemTradeId())) {
                    tradeId = anchorUrlInfoVo.getSystemTradeId();
                } else {
                    tradeId = anchorUrlInfoVo.getAiCorrectTradeId();
                }
            }
        } else if (sourceType == 1) {
            UploadFileInfoVo file = uploadFileProducer.getByFileId(sourceId);
            if (file == null) {
                throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "文件不存在");
            }

            // 只能有一次行业推荐
            UploadFileDetailBo bo = new UploadFileDetailBo();
            bo.setFileId(sourceId);
            UploadFileDetailInfoVo fileDetailInfoVo = uploadFileDetailProducer.getAndSave(bo);
            if (fileDetailInfoVo != null && ObjectUtil.equals(fileDetailInfoVo.getSuggestTrade(), 1)) {
                throw new BusinessException(StatusCode.OPERATION_EX.getCode(), "已经推荐过了，不能重复推荐");
            }
            tradeId = fileDetailInfoVo == null ? null : fileDetailInfoVo.getSuggestTradeId();
        }
        return tradeId;
    }

    /**
     * 获取视频推荐的行业提示词
     *
     * @param sourceId 视频id
     * @return 视频推荐的行业提示词
     */
    public AiAnalyzeTradePromptVo getAiAnalyzeTradePrompt(Integer sourceType, String sourceId, BiFunction<Integer, String, AnalysisResultVo> fun) {
        AiAnalyzeTradePromptVo result = new AiAnalyzeTradePromptVo();

        // 获取视频/文件对应的推荐行业id
        Long tradeId = getTradeIdBySource(sourceType, sourceId);

        // 已经推荐过就直接使用
        if (tradeId != null) {
            TradeInfoVo info = tradeProducer.info(tradeId);
            if (info != null) {
                result.setDefTradeId(tradeId);
                result.setDefTradeName(info.getName());
                return result;
            }
        }

        List<Tree<Long>> treeList = tradeProducer.listTreeTrade();
        TreePrinterWithMap.TreePrintResult treePrintResult = TreePrinterWithMap.printTreeWithMap(treeList);

        String industryPrompt = getIndustryPrompt(sourceType, sourceId, fun, treePrintResult);

        // 获取ai模型
        int aiModel = 0;
        SystemKvInfoVo kvInfoVo = systemKvProducer.getByKey("suggest_trade_ai_model");
        if (ObjectUtil.isNotEmpty(kvInfoVo)) {
            DictDataListVo dictDataListVo = dictDataFeign.dictDataByLabel("client_ai_model", kvInfoVo.getKvValue());
            if (ObjectUtil.isNotEmpty(dictDataListVo)) {
                aiModel = NumberUtil.parseInt(dictDataListVo.getValue(), aiModel);
            }
        }

        TradeInfoVo info = tradeProducer.info(1L);
        if (info != null) {
            result.setDefTradeId(info.getId());
            result.setDefTradeName(info.getName());
        }

        result.setTradeList(treePrintResult.tradeList());
        result.setCueWord(industryPrompt);
        result.setAiIdentity("你是一个顶尖行业分析师");
        result.setAiModel(aiModel);
        return result;
    }

    /**
     * 修改推荐的行业为已推荐
     *
     * @param sourceType   来源类型
     * @param sourceId     来源id
     * @param suggestTrade 修改的状态
     * @return 是否成功
     */
    public Boolean updateVideoOrFileDetails(Integer sourceType, String sourceId, Integer suggestTrade, Long tradeId) {
        if (sourceType == 0) {
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(sourceId);
            if (video == null) {
                return false;
            }

            // 修改状态
            AnchorVideoDetailBo update = new AnchorVideoDetailBo();
            update.setVideoId(sourceId);
            update.setSuggestTrade(suggestTrade);
            update.setSuggestTradeId(tradeId);
            anchorVideoDetailProducer.updateByVideoId(update);

            // 向ai推线行业字段设置值
            anchorUrlProducer.updateTrdeIdBySecUid(video.getSecUid(), tradeId, null);
            return true;
        } else if (sourceType == 1) {
            UploadFileDetailBo bo = new UploadFileDetailBo();
            bo.setFileId(sourceId);
            UploadFileDetailInfoVo fileDetailInfoVo = uploadFileDetailProducer.getAndSave(bo);
            if (fileDetailInfoVo == null) {
                return false;
            }
            UploadFileDetailBo update = new UploadFileDetailBo();
            update.setId(fileDetailInfoVo.getId());
            update.setSuggestTrade(suggestTrade);
            update.setSuggestTradeId(tradeId);
            return uploadFileDetailProducer.update(update);
        }

        return false;
    }

    /**
     * 获取子账号视频列表
     * @param clientVideoListBo 查询参数
     * @return
     */
    public R<PageUtils<SubUserVideoListVo>> getSubClientVideoList(ClientVideoListBo clientVideoListBo) {

        if(clientVideoListBo.getUserId() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "子账号id不能为空");
        }

        R<UserCacheVo> userR = this.userFeign.getLocalUser();
        if(userR.getData() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
        }
        clientVideoListBo.setTenantId(userR.getData().getActiveTenantId());

        return getSubUserVideoList(clientVideoListBo);
    }

    /**
     * 根据secUid和duration获取对应的视频
     *
     * @param secUids  主播
     * @param duration 时长
     * @return 视频
     */
    public AnchorVideoInfoVo getBySecUidAndGeDuration(String secUids, int duration) {
        return anchorVideoProducer.getBySecUidAndGeDuration(secUids, duration);
    }

    /**
     * 获取用户的服务时长和录制视频数
     * @return
     */
    public R<UserServiceDurationVo> getUserServiceDuration() {

        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if (userCacheVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户不存在");
        }

        UserServiceDurationVo userServiceDurationVo = new UserServiceDurationVo();

        long hours = Duration.between(userCacheVo.getCreateDate().toInstant(), Instant.now()).toHours();
        userServiceDurationVo.setDuration(hours);

        Long count = this.anchorVideoRse.countUserVideo(userCacheVo.getId(), userCacheVo.getActiveTenantId());
        userServiceDurationVo.setVideoCount(count);

        return R.ok(userServiceDurationVo);
    }

    /**
     * 更新视频的云空间重命名
     *
     * @param videoId     视频id
     * @param cloudRename 云空间重命名
     * @return 操作结果
     */
    public R<String> updateCloudRename(String videoId, String cloudRename) {
        if (videoId == null || videoId.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频id不能为空");
        }

        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if (userCacheVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户信息不存在");
        }

        // 校验视频是否属于当前租户
        AnchorVideoInfoVo videoInfo = this.anchorVideoProducer.getByVideoId(videoId);
        if (videoInfo == null || !videoInfo.getTenantId().equals(userCacheVo.getActiveTenantId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频不存在或无权限操作");
        }

        this.anchorVideoRse.updateCloudRename(videoId, cloudRename);

        return R.ok("重命名成功");
    }

    /**
     * 根据secUid获取ai推荐的视频
     *
     * @param secUid 主播
     * @return 视频
     */
    public AnchorVideoInfoVo getAiRecommendVideoBySecUid(String secUid) {
        Integer duration = systemKvProducer.getValueByKey(SystemKeyConstant.correctAnchorTradeVideoDuration, 1800);

        // 获取对应的视频，视频的时间大于duration秒
        return anchorVideoProducer.getBySecUidAndGeDuration(secUid, duration);
    }

    /**
     * 根据视频名称获取视频信息
     * @param videoName 视频名称
     * @return
     */
    public R<AnchorVideoInfoVo> getByVideoName(String videoName) {
        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if (userCacheVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户信息不存在");
        }
        AnchorVideoInfoVo videoInfoVo = anchorVideoProducer.getByVideoName(videoName, userCacheVo.getId(), userCacheVo.getActiveTenantId());
        if (videoInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
        }
        return R.ok(videoInfoVo);
    }


    /**
     * 推送视频数据到governance
     *
     * @param videoId 视频id
     */
    public void pushGovernanceVideo(String videoId) {
        // 查询视频
        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
        if (video == null) {
            log.warn("推送视频数据到governance 视频不存在，videoId = {}", videoId);
            return;
        }

        VideoPushRequestBo requestBo = new VideoPushRequestBo();
        requestBo.setTenantId(video.getTenantId());
        requestBo.setBatchNumber(video.getBatchNumber());
        requestBo.setVideoId(video.getVideoId());
        requestBo.setSecUid(video.getSecUid());
        requestBo.setStartTime(video.getStartTime());
        requestBo.setEndTime(video.getEndTime());
        requestBo.setPlatform(requestBo.transformedPlatform(video.getPlatformType()));

        // 查询视频的销售额、场观等数据
        VideoDataViewingConfuseInfoVo confuseInfo = videoDataViewingConfuseProducer.getByVideoId(videoId);
        boolean hasPerformance = confuseInfo != null && ObjectUtil.equals(confuseInfo.getDataSourceType(), 1);

        if (hasPerformance) {
            requestBo.setHasPerformance(1);
            requestBo.setCumulativeView(confuseInfo.getTotalWatchNum());
            requestBo.setSalesRevenue(confuseInfo.getVolumeStart() != null ? BigDecimal.valueOf(confuseInfo.getVolumeStart()) : null);

            // 查询paragraph的ossPath作为videoOssUrl
            VideoDataViewingConfuseInfoVo paragraphInfo = videoDataViewingParagraphRse.infoByVideoId(videoId);
            if (paragraphInfo != null && StrUtil.isNotBlank(paragraphInfo.getOssPath())) {
                requestBo.setVideoOssUrl(paragraphInfo.getOssPath());
            }
        } else {
            requestBo.setHasPerformance(0);
        }

        // 查询视频的商品数据
        List<ProductDetailsEntity> productList = productDetailsProducer.listByVideoId(videoId);
        if (productList != null && !productList.isEmpty()) {
            requestBo.setProducts(BeanUtil.copyToList(productList, VideoProductRequestBo.class));
        }

        // 组合数据，调用接口推给客户后台
        ResponseEntity<String> responseEntity = governanceHttpServer
                .post("/api/governance/performance/video/receive", requestBo, null)
                .retrieve()
                .toEntity(String.class);
        boolean isSuccess = responseEntity.getStatusCode().is2xxSuccessful();
        if (isSuccess) {
            String res = responseEntity.getBody();
            JSONObject jsonObject = JSONObject.parseObject(res);
            if (jsonObject == null || !ObjectUtil.equals(NumberUtil.parseInt(jsonObject.getString("code"), 0), 0)) {
                isSuccess = false;
            }
        }
        if (!isSuccess) {
            log.error("推送视频数据到governance失败, HTTP状态码: {}, msg = {}", responseEntity.getStatusCode(), responseEntity.getBody());
        }
    }

    /**
     * 根据用户+主播统计视频数量
     * @param secUid 主播唯一标识
     * @param userId 用户id
     * @param tenantId 租户id
     * @return 视频数量
     */
    public R<Integer> countBySecUid(String secUid, Long userId, Long tenantId) {
        int count = anchorVideoProducer.countBySecUid(secUid, userId, tenantId);
        return R.ok(count);
    }
}
