package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.generic.vo.words.LocalSourceVideoVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.video.*;
import com.jiuyu.replay.words.vo.anchor.HistoryBatchNumberVideoListVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface AnchorVideoProducer {

    /**
     * 录制时上传视频信息
     */
    R<String> save(AnchorVideoInfoBo anchorVideoInfoBo);
    /**
     * 分页查询所有的分享视频
     */
    PageUtils<AnchorVideoVO> queryPage(AnchorVideoVO anchorVideoVO);

    /**
     * 跟查询用户Id分页查询录制视频信息
     * @param anchorVideoVO
     * @return
     */
    R<PageUtils<AnchorVideoVO>> pageLists(AnchorVideoBo anchorVideoVO);

    /**
     * 更新视频信息
     * @param anchorVideoInfoBo
     * @return
     */
    AnchorVideoInfoVo updateVideo(AnchorVideoInfoBo anchorVideoInfoBo);

    /**
     * 客户端查询视频列表
     * @param id
     * @return
     */
    R<List<AnchorVideoVO>> selectByuserId(Long id);

    /**
     * 据客户端上传的List<voidId>删除
     * @param voidId
     * @return
     */
    R<String> removeByVoidId(List<String> voidId);

    /**
     * 服务端用户详情查询录制记录
     * @param userId
     * @return
     */
    R<PageUtils<AnchorVideoVO>> listByUserId(AnchorVideoBo userId);

    /**
     * 批量保存分析记录
     * @param audioAnalysisSaveBos 分析记录集合
     */
    void saveBatch(List<AudioAnalysisSaveBo> audioAnalysisSaveBos);

    /**
     * 根据主播sec_uid获取已录制该主播的视频
     * @param anchorVideoBo
     * @return
     */
    R<PageUtils<AnchorVideoVO>> selectVideoBySecUid(AnchorVideoBo anchorVideoBo);

    /**
     * 根据视频唯一标识获取视频信息
     * @param videoId 视频唯一标识
     * @return
     */
    AnchorVideoInfoVo getByVideoId(String videoId);

    /**
     * 根据视频ID获取分析记录内容
     * @param videoId
     * @return
     */
    R<AnchorVideoVO> videoAnalysisByVideoId(String videoId);

    /**
     * 根据user_id获取录制分析
     * @param anchorVideoVO
     * @return
     */
    R<PageUtils<AnchorVideoVO>> videoAnalysisByUserId(AnchorVideoVO anchorVideoVO);

    /**
     * 根据租户id获取用户在服务器上的视频列表
     * @param activeTenantId 租户id
     * @param uploadStatus 视频上传状态 0：未上传 1：已上传
     * @return
     */
    List<AnchorVideoInfoVo> getVideoListByTenantId(Long activeTenantId, Integer uploadStatus);

    /**
     * 获取云空间视频列表
     * @param cloudVideoListBo 请求参数
     * @return
     */
    PageUtils<AnchorVideoInfoVo> listCloudVideoPage(CloudVideoListBo cloudVideoListBo);

    /**
     * 根据视频唯一标识集合获取视频列表
     * @param videoIds 视频唯一标识集合
     * @return
     */
    List<AnchorVideoInfoVo> listByVideoIds(Collection<String> videoIds);

    /**
     * 根据租户id统计存储量
     * @param activeTenantId    租户id
     * @return 单位 KB
     */
    Long statisticsStoreByTenantId(Long activeTenantId);

    /**
     * 条件获取ai收藏页视频数据
     * @param clientAiFavListBo 查询条件
     * @return
     */
    List<String> listByAiFav(ClientAiFavListBo clientAiFavListBo);

    /**
     * 客户端获取视频列表
     * @param clientVideoListBo 查询参数
     * @return
     */
    PageUtils<AnchorVideoInfoVo> clientVideoList(ClientVideoListBo clientVideoListBo);

    /**
     * 获取允许删除的视频id集合
     * @param ids 视频uuid集合
     * @param tenantId 租户id
     * @param userId 用户id
     * @return
     */
    List<String> getAllowDeleteVideo(List<String> ids, Long tenantId, Long userId);

    /**
     * 批量修改视频的删除状态
     * @param videoIds 视频id集合
     * @param deleteStatus 视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除
     */
    void batchUpdateVideoDelStatus(List<String> videoIds, Integer deleteStatus);

    /**
     * 批量修改视频的云空间分享状态
     * @param videoIds 视频id集合
     * @param uploadStatus 视频上传状态 0：未上传 1：已上传
     */
    void batchUpdateVideoUploadStatus(List<String> videoIds, Integer uploadStatus);

    /**
     * 获取ai复盘主播列表相关的视频列表信息
     * @param secUids 主播secUid集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @param isCloud 是否查云空间的 0:否 1:是
     * @param videoSliceType 视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频
     * @return
     */
    List<AnchorVideoInfoVo> listByAnchorRecordList(List<String> secUids, Long userId, Long tenantId, Integer isCloud, Integer videoSliceType);

    /**
     * 获取用户视频列表
     * @param listUserVideoByConditionBo 查询参数
     * @return
     */
    List<AnchorVideoInfoVo> listUserVideo(ListUserVideoByConditionBo listUserVideoByConditionBo);

    /**
     * 查询最近直播场次（主播下播后平台数据补采集专用）。
     *
     * <p>查询用户租户下、未删除、时长不小于 60 秒、开播时间不早于 startTimeGe 的原视频（videoSliceType=0），
     * 再按 batchNumber 分组，每组取 paragraph 最小（null 视为最小，再按 id 兜底）的一条作为该场次代表。
     * 返回一个 batchNumber 对应一条的代表列表。</p>
     *
     * @param userId      用户id
     * @param tenantId    租户id
     * @param startTimeGe 开播时间下限（格式 yyyy-MM-dd HH:mm:ss），为空时不加时间过滤
     * @return 场次代表视频列表，无数据返回空列表
     */
    List<AnchorVideoInfoVo> listRecentLiveSessions(Long userId, Long tenantId, String startTimeGe);

    /**
     * 修改视频的分析状态
     * @param updateVideoAnalysisStatusBo 修改参数
     * @return
     */
    void updateVideoAnalysisStatus(UpdateVideoAnalysisStatusBo updateVideoAnalysisStatusBo);

    /**
     * 修改视频的上传状态
     * @param updateVideoUploadStatusBo 修改参数
     * @return
     */
    void updateVideoUploadStatus(UpdateVideoUploadStatusBo updateVideoUploadStatusBo);

    /**
     * 客户端根据视频唯一标识。获取视频信息
     * @param videoId 视频唯一标识
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    AnchorVideoInfoVo clientGetVideoByVideoId(String videoId, Long userId, Long tenantId);

    /**
     * 批量修改视频的大小时长
     * @param updateVideoUploadStatusBoList 视频大小时长集合
     * @return
     */
    void updateVideoSizeDuration(List<UpdateVideoSizeDurationBo> updateVideoUploadStatusBoList);

    /**
     * 初始化视频，将分析中的视频改成分析失败
     * @return
     */
    void initVideoAnalysisStatus(Long userId, Long tenantId);

    /**
     * 保存或修改视频信息
     * @param anchorVideoInfoBo 视频信息
     * @return
     */
    void saveOrUpdate(AnchorVideoInfoBo anchorVideoInfoBo);

    /**
     * 批量保存巨量拉取的视频记录（带去重：同 batch_number + 同 userId 下 data_source=1 则跳过，否则软删除旧记录后插入）
     * @param videoList 视频列表
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 被软删除的旧记录列表（供 BLL 上传回收站和同步关联表）
     */
    List<AnchorVideoInfoVo> savePulledVideos(List<AnchorVideoInfoBo> videoList, Long userId, Long tenantId);

    /**
     * 客户端根据视频id集合获取视频列表
     * @param ids 视频uuid集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    List<AnchorVideoInfoVo> clientListVideoByVideoIds(List<String> ids, Long userId, Long tenantId);

    /**
     * 根据主播secuid集合获取昨天的视频列表
     * @param secUidList 主播secuid集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    List<AnchorVideoInfoVo> listYesterdayBySecUids(List<String> secUidList, Long userId, Long tenantId);

    /**
     * 根据主播secuid集合获取前天的视频列表
     * @param secUidList 主播secuid集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    List<AnchorVideoInfoVo> listDayBeforeYesterdayBySecUids(List<String> secUidList, Long userId, Long tenantId);

    /**
     * 同步本地视频、文件、对比到服务器
     * @param syncLocalDataToServerBo id参数集合
     * @return
     */
    void syncLocalDataToServer(SyncLocalDataToServerBo syncLocalDataToServerBo);

    /**
     * 获取历史直播批次-有视频
     *
     * @param videoId
     * @param dataType
     * @param uploadStatus
     * @param limit
     * @return
     */
    List<HistoryBatchNumberVideoListVo> historyBatchNumberList(String videoId, Integer dataType, Integer uploadStatus, Integer limit);

    /**
     * 查询场次中的第一条视频
     * @param batchNumberIds
     * @return
     */
    List<AnchorVideoInfoVo> listOneVideoByBatchNumber(List<Long> batchNumberIds);

    /**
     * 根据 batchNumber 和租户查询整场直播的第一条记录（paragraph=0）
     * @param batchNumber 直播场次 room_id
     * @param tenantId 租户ID
     * @return 场次第一条视频信息，不存在返回 null
     */
    AnchorVideoInfoVo getLiveSessionByBatchNumber(String batchNumber, Long tenantId);

    Integer getVideoStatus(String videoId, AnchorVideoFileAllVo videoContentVo);

    /**
     * 判断是否第一个视频
     * @param videoId
     * @return
     */
    boolean hasOneVideo(Long userId, Long tenantId);

    /**
     * 根据租户id 和 场次号 和 结束时间区间 获取视频videoId集合
     * @param tenantId 租户id
     * @param batchNumber 场次号
     * @param startTime 时间区间-开始
     * @param endTime 时间区间-结束
     * @return
     */
    List<String> listIdsByTenantAndBatchNumberAndEndTime(Long tenantId, Long batchNumber, String startTime, String endTime);

    /**
     * 根据secUid和duration获取对应的视频
     *
     * @param secUids  主播
     * @param duration 时长
     * @return 视频
     */
    AnchorVideoInfoVo getBySecUidAndGeDuration(String secUids, int duration);

    /**
     * 根据secUid查询上一场视频（startTime在本场之前且分析完成的最近一场）
     *
     * @param secUid          主播唯一标识
     * @param userId          用户id
     * @param tenantId        租户id
     * @param beforeStartTime 本场开始时间
     * @return 上一场视频信息，没有则返回null
     */
    AnchorVideoInfoVo getPrevBySecUid(String secUid, Long userId, Long tenantId, Date beforeStartTime);

    /**
     * 根据视频id获取父视频信息
     *
     * @param videoId 视频id
     * @return 父视频信息
     */
    AnchorVideoInfoVo getParentVideoByVideoId(String videoId);

    /**
     * 根据视频名称、用户id和租户id获取视频信息
     * @param videoName 视频名称
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    AnchorVideoInfoVo getByVideoName(String videoName, Long userId, Long tenantId);

    /**
     * 统计租户累计 AI 语音分析时长（秒）
     * 口径：租户名下未删除、analysis_status=2（分析完成）的视频 duration 总和（duration 单位：秒）
     *
     * @param tenantId 租户id
     *
     * @return AI 分析时长合计（秒），无记录返回 0
     */
    Long sumAnalyzedDurationByTenantId(Long tenantId);

    /**
     * 判断指定用户是否录制过视频(仅限已分析)
     *
     * @param userId      用户id
     * @param tenantId    租户id
     * @param minDuration 最短录制时长
     *
     * @return true 录制过视频；false 未录制过视频
     */
    boolean hasRecord(long userId, long tenantId, int minDuration);

    /**
     * 根据用户+主播统计视频数量
     * @param secUid 主播唯一标识
     * @param userId 用户id
     * @param tenantId 租户id
     * @return 视频数量
     */
    int countBySecUid(String secUid, Long userId, Long tenantId);

    /**
     * Data Hub：按 租户×业务账号 聚合成功分析记录
     *
     * @param tenantIds 租户id列表
     * @param startDate 窗口起（含，可空）
     * @param endDate   窗口止（不含，可空）
     *
     * @return {@link java.util.List }<{@link com.jiuyu.replay.words.vo.datahub.DataHubAnalysisStatsVo }>
     */
    java.util.List<com.jiuyu.replay.words.vo.datahub.DataHubAnalysisStatsVo> aggregateDataHubAnalysisStats(
            java.util.Collection<Long> tenantIds, java.util.Date startDate, java.util.Date endDate);

    /**
     * 分页获取本地源视频列表（自动删除本地视频专用精简查询）。
     *
     * <p>只查询删除逻辑所需的最小字段集，固定过滤条件全部下推 SQL，不做关联数据填充，
     * 与 {@link #clientVideoList(ClientVideoListBo)} 分离，避免返回 40+ 字段与关联数据。</p>
     *
     * @param userId   用户id
     * @param tenantId 租户id
     * @param page     当前页
     * @param limit    每页记录数
     * @return 本地源视频精简分页
     */
    PageUtils<LocalSourceVideoVo> listLocalSourceVideo(Long userId, Long tenantId, Integer page, Integer limit);
}
