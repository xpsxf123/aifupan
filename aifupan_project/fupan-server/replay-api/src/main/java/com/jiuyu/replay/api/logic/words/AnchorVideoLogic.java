package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.ai.vo.DataDiagnosisConfigVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.generic.vo.words.LocalSourceVideoVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.video.*;
import com.jiuyu.replay.words.entity.AudioAnalysisEntity;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.anchor.HistoryBatchNumberVideoListVo;

import java.util.List;

/**
 * @author Admin
 */
public interface AnchorVideoLogic {

    /**
     * 根据主播sec_uid获取已录制该主播的视频
     * @param anchorVideoBo
     * @return
     */
    R<PageUtils<AnchorVideoVO>> selectVideoBySecUid(AnchorVideoBo anchorVideoBo);

    /**
     * 录制时上传视频信息
     * @param anchorVideoInfoBo
     * @return
     */
    R<String> save(AnchorVideoInfoBo anchorVideoInfoBo);

    /**
     * 跟查询用户Id分页查询录制视频信息
     */
    R<PageUtils<AnchorVideoVO>>pageLists(AnchorVideoBo anchorVideoBo);

    /**
     * 根据视频Id查询分析内容
     * @param id
     * @return
     */
     R<List<AudioAnalysisEntity>>selectByVideoId(AudioAnalysisBo id);

    /**
     * 新增直播视频分的内容
     * @param udioAnalysisVo
     * @return
     */
    R<String> saveAudioAnalysis(List<AudioAnalysisVo> udioAnalysisVo);

    /**
     * 更新视频信息
     * @param anchorVideoInfoBo
     * @return
     */
    R<String> updateVideo(AnchorVideoInfoBo anchorVideoInfoBo);

    /**
     * 客户端保存分析记录
     * @param anchorVideoRecodVo
     */
    void saveAnchorVideoRecod( List<AudioAnalysisVo> anchorVideoRecodVo);

    /**
     * 服务端分页查询分析记录
     * @param anchorVideoBo
     * @return
     */
    R<PageUtils<AnchorVideoVO>> selectAnchorVideoRecod(AnchorVideoBo anchorVideoBo);

    /**
     * 客户端分页查询录制视频信息
     * @param anchorVideoBo
     * @return
     */
    R<PageUtils<AnchorVideoVO>> pageByUserId(AnchorVideoBo anchorVideoBo);

    /**
     * 客户端查询视频列表
     * @return
     */
    R<List<AnchorVideoVO>> selectByuserId();

    /**
     * 根据客户端上传的List<voidId>删除
     * @param voidId
     * @return
     */
    R<String> removeByVoidId(List<String> voidId);

    /**
     * 客户端根据视频Id和行业ID查询该视频的分析内容
     * @param audioAnalysisBo
     * @return
     */
    R<List<AudioAnalysissVO>> selectByVideoIdOrTradeId(AudioAnalysisBo audioAnalysisBo);

    /**
     * 服务端查询录制记录
     * @param userId
     * @return
     */
    R<PageUtils<AnchorVideoVO>> listByUserId(AnchorVideoBo userId);

    /**
     * 服务端用户详情查询分析记录
     * @param userId
     * @return
     */
    R<PageUtils<AnchorVideoRecodListVo>> selectVideoRecod(AnchorVideoBo userId);

    /**
     * 清除视频的分析数据
     * @param videoId 视频唯一标识
     * @return
     */
    void clearAnalysis(String videoId);

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
     * 从云点播删掉视频文件， 加回云空间容量
     * @param videoId 视频唯一标识
     * @return
     */
    R<String> deleteOnlineVideo(String videoId);

    /**
     * 判断服务器是否有视频的分析数据
     * @param videoId 视频唯一标识
     * @param tradeId 行业id
     * @return
     */
    R<Boolean> checkVideoAnalysisExist(String videoId, Long tradeId);

    /**
     * 客户端同步分析数据到服务器
     * @param syncVideoAnalysisBo 分析数据
     * @return
     */
    R<String> syncVideoAnalysisToServer(SyncVideoAnalysisBo syncVideoAnalysisBo);

    /**
     *客户端根据租户id获取用户在服务器上的视频列表
     * @return
     */
    R<List<AnchorVideoInfoVo>> getVideoListByTenantId();

    /**
     * 根据视频唯一标识。获取视频信息
     * @param videoId 视频唯一标识
     * @return
     */
    R<AnchorVideoInfoVo> infoByVideoId(String videoId);

    /**
     * 获取云空间视频列表
     * @param cloudVideoListBo 请求参数
     * @return
     */
    R<PageUtils<AnchorVideoInfoVo>> listCloudVideo(CloudVideoListBo cloudVideoListBo);

    /**
     * 获取云空间对比列表
     * @param cloudContrastListBo 请求参数
     * @return
     */
    R<PageUtils<SyncContrastListVo>> listCloudContrast(CloudContrastListBo cloudContrastListBo);

    /**
     * PC后端获取分析数据
     * @param fileId 文件ID
     * @param videoId 视频ID
     * @return
     */
    R<OnlineAnalysisInfoVo> getAnalysisInfo(String fileId, String videoId);

    /**
     * 客户端获取视频列表
     * @param clientVideoListBo 查询参数
     * @return
     */
    R<PageUtils<AnchorVideoInfoVo>> clientVideoList(ClientVideoListBo clientVideoListBo);

    /**
     * 客户端删除视频
     * @param ids 视频uuid集合
     * @return
     */
    R<String> clientDeleteVideo(List<String> ids);

    /**
     * 客户端删除云空间视频
     * @param videoId 视频uuid
     * @return
     */
    R<String> clientDeleteCloudVideo(String videoId);

    /**
     * 客户端批量删除云空间视频。
     * 与 {@link #clientDeleteCloudVideo(String)} 的业务逻辑一致，按 ids 集合一次性处理；
     * 所有 DB 写均为常数次（与 N 无关），VOD 源文件按集合内单条调用（SDK 限制，非 N+1 数据库操作）。
     *
     * @param videoIds 视频uuid集合，单批最多 100 条
     * @return 删除结果文案，含 成功 / 跳过(无权限或非云端) / VOD 删除失败 三类计数
     */
    R<String> clientBatchDeleteCloudVideos(List<String> videoIds);

    /**
     * 客户端获取云空间视频列表
     * @param clientVideoListBo 查询参数
     * @return
     */
    R<PageUtils<AnchorVideoInfoVo>> clientListCloudVideo(ClientVideoListBo clientVideoListBo);

    /**
     * 获取用户视频列表
     * @param listUserVideoByConditionBo 查询参数
     * @return
     */
    R<List<AnchorVideoInfoVo>> listUserVideo(ListUserVideoByConditionBo listUserVideoByConditionBo);

    /**
     * 查询最近直播场次（主播下播后平台数据补采集专用）。
     *
     * <p>userId / tenantId 从 JWT 解析注入，不入参，保证租户隔离。</p>
     *
     * @param listRecentLiveSessionBo 查询参数（仅开播时间下限 startTimeGe）
     * @return 场次代表视频列表
     */
    R<List<AnchorVideoInfoVo>> listRecentLiveSessions(ListRecentLiveSessionBo listRecentLiveSessionBo);

    /**
     * 修改视频的分析状态
     * @param updateVideoAnalysisStatusBo 修改参数
     * @return
     */
    R<String> updateVideoAnalysisStatus(UpdateVideoAnalysisStatusBo updateVideoAnalysisStatusBo);

    /**
     * 修改视频的上传状态
     * @param updateVideoUploadStatusBo 修改参数
     * @return
     */
    R<String> updateVideoUploadStatus(UpdateVideoUploadStatusBo updateVideoUploadStatusBo);

    /**
     * 客户端根据视频唯一标识。获取视频信息
     * @param videoId 视频唯一标识
     * @return
     */
    R<AnchorVideoInfoVo> clientGetVideoByVideoId(String videoId);

    /**
     * 批量修改视频的大小时长
     * @param updateVideoUploadStatusBoList 视频大小时长集合
     * @return
     */
    R<String> updateVideoSizeDuration(List<UpdateVideoSizeDurationBo> updateVideoUploadStatusBoList);

    /**
     * 初始化视频表和文件，将分析中的改成分析失败
     * @return
     */
    R<String> initVideoAndFileAnalysisStatus();

    /**
     * 保存或修改视频信息
     * @param anchorVideoInfoBo 视频信息
     * @return
     */
    R<String> saveOrUpdateVideo(AnchorVideoInfoBo anchorVideoInfoBo);

    /**
     * 批量保存巨量拉取的视频记录
     * @param videoList 视频列表
     * @return
     */
    R<String> savePulledVideos(List<AnchorVideoInfoBo> videoList);

    /**
     * 客户端根据视频id集合获取视频列表
     * @param ids 视频uuid集合
     * @return
     */
    R<List<AnchorVideoInfoVo>> clientListVideoByVideoIds(List<String> ids);

    /**
     * 检查用户是否已经同步过本地数据到服务器
     * @return
     */
    R<Integer> checkUserSyncLocalData();

    /**
     * 同步本地视频、文件、对比到服务器
     * @param syncLocalDataToServerBo id参数集合
     * @return
     */
    R<String> syncLocalDataToServer(SyncLocalDataToServerBo syncLocalDataToServerBo);

    /**
     * 获取历史分段视频
     *
     * @param videoId
     * @param dataType
     * @param uploadStatus
     * @param limit
     * @return
     */
    R<List<HistoryBatchNumberVideoListVo>> historyBatchNumberVideoList(String videoId, Integer dataType, Integer uploadStatus, Integer limit);

    /**
     * 根据 batchNumber 查询整场直播的第一条记录（paragraph=0），返回该场次的原始 startTime
     *
     * @param batchNumber 直播场次 room_id
     * @return 场次第一条视频信息
     */
    R<AnchorVideoInfoVo> getLiveSessionByBatchNumber(String batchNumber);

    /**
     * 开始获取重要弹幕
     *
     * @param videoId 视频id
     * @return 时长成功
     */
    R<Boolean> startImportantBarrage(String videoId);

    /**
     * 获取数据诊断配置
     *
     * @param sourceId   源ID
     * @param sourceType 源类型
     * @return 数据诊断配置
     */
    DataDiagnosisConfigVo getDataDiagnosisConfig(String sourceId, Integer sourceType, String webVersion);

    /**
     * 更新数据诊断配置
     *
     * @param dataDiagnosisConfigVo 数据诊断配置
     */
    void updateDataDiagnosisConfig(DataDiagnosisConfigVo dataDiagnosisConfigVo);

    /**
     * 新版自然/优化原文生成（由 XXL-Job 定时器 {@code generateVideoContentNew} 调用）。
     *
     * <h3>核心改进（相比旧版）</h3>
     * <ul>
     *   <li>分段并行：原文按段落拆分，每段独立 AI 调用，批次内全部并行</li>
     *   <li>段级重试：每段最多重试 3 次（递增间隔 2s/4s/6s）</li>
     *   <li>格式纠正：每段生成后自动检查并纠正格式问题</li>
     *   <li>快速返回：只负责拾取 + 提交线程池，不等待 AI 结果</li>
     *   <li>多实例安全：乐观锁拾取，多实例不会重复处理同一条记录</li>
     * </ul>
     *
     * @param limit 每次最多处理的条数（视频和文件各查 limit 条），为 null 时默认 10
     */
    void generateVideoContentNew(Integer limit);

    /**
     * 根据secUid统计视频数量（当前用户+租户范围内）
     * @param secUid 主播唯一标识
     * @return 视频数量
     */
    R<Integer> countBySecUid(String secUid);

    /**
     * 客户端分页获取本地源视频列表（自动删除本地视频专用）。
     *
     * <p>userId / tenantId 从 JWT 解析，不入参，保证租户隔离与越权防护。</p>
     *
     * @param bo 查询参数（仅分页字段 page/limit）
     * @return 本地源视频精简分页
     */
    R<PageUtils<LocalSourceVideoVo>> listLocalSourceVideo(LocalSourceVideoBo bo);
}
