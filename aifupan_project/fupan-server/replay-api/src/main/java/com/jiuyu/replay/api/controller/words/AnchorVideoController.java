package com.jiuyu.replay.api.controller.words;


import cn.hutool.core.collection.CollUtil;
import com.jiuyu.replay.ai.vo.DataDiagnosisConfigVo;
import com.jiuyu.replay.api.logic.words.AnchorVideoLogic;
import com.jiuyu.replay.api.logic.words.UploadFileAnalysisRecordLogic;
import com.jiuyu.replay.api.logic.words.UploadFileLogic;
import com.jiuyu.replay.api.logic.words.VideoAnalysisRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.generic.vo.words.LocalSourceVideoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.bo.AnchorVideoBo;
import com.jiuyu.replay.words.bo.AnchorVideoInfoBo;
import com.jiuyu.replay.words.bo.AudioAnalysisBo;
import com.jiuyu.replay.words.bo.SyncVideoAnalysisBo;
import com.jiuyu.replay.words.bo.video.*;
import com.jiuyu.replay.words.entity.AudioAnalysisEntity;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.producer.AnalysisMarkProducer;
import com.jiuyu.replay.words.repository.service.VideoTextNotesService;
import com.jiuyu.replay.words.vo.AnchorVideoRecodListVo;
import com.jiuyu.replay.words.vo.AudioAnalysisVo;
import com.jiuyu.replay.words.vo.AudioAnalysissVO;
import com.jiuyu.replay.words.vo.OnlineAnalysisInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @author Admin
 */
@RestController
@CrossOrigin
@RequestMapping("replay/AnchorVideo")
@Tag(name = "录制的视频")
public class AnchorVideoController {
    @Resource
    AnchorVideoLogic anchorVideoLogic;

    @Resource
    private VideoAnalysisRecordLogic videoAnalysisRecordLogic;
    @Resource
    private UploadFileLogic uploadFileLogic;
    @Resource
    private UploadFileAnalysisRecordLogic uploadFileAnalysisRecordLogic;

    @Resource
    private VideoTextNotesService textNotesService;

    @Resource
    private AnalysisMarkProducer analysisMarkProducer;


    /**
     * 客户端获取视频列表
     *
     * @param clientVideoListBo 查询参数
     *
     * @return
     */
    @PostMapping("/clientVideoList")
    @Operation(summary = "客户端获取视频列表")
    public R<PageUtils<AnchorVideoInfoVo>> clientVideoList(@RequestBody ClientVideoListBo clientVideoListBo) {
        R<PageUtils<AnchorVideoInfoVo>> result = anchorVideoLogic.clientVideoList(clientVideoListBo);
        if (result.success() && result.getData() != null && CollUtil.isNotEmpty(result.getData().getList())) {
            textNotesService.fullExistsNotes(result.getData().getList(), AnchorVideoInfoVo::getVideoId, VideoSourceType.LOCAL, AnchorVideoInfoVo::setNotesSummary, AnchorVideoInfoVo::setExistsNotes);
            analysisMarkProducer.fullExistsMark(result.getData().getList(), AnchorVideoInfoVo::getVideoId, VideoSourceType.LOCAL, AnchorVideoInfoVo::setExistsMark);
        }
        return result;
    }

    /**
     * 客户端分页获取本地源视频列表（自动删除本地视频专用精简接口）
     *
     * @param bo 查询参数（仅分页字段）
     * @return 本地源视频精简分页
     */
    @PostMapping("/listLocalSourceVideo")
    @Operation(summary = "客户端分页获取本地源视频列表")
    public R<PageUtils<LocalSourceVideoVo>> listLocalSourceVideo(@RequestBody LocalSourceVideoBo bo) {
        return anchorVideoLogic.listLocalSourceVideo(bo);
    }

    /**
     * 客户端根据视频id集合获取视频列表
     *
     * @param ids 视频uuid集合
     *
     * @return
     */
    @PostMapping("/clientListVideoByVideoIds")
    @Operation(summary = "客户端根据视频id集合获取视频列表")
    public R<List<AnchorVideoInfoVo>> clientListVideoByVideoIds(@Parameter(description = "视频uuid集合", required = true) @RequestBody List<String> ids) {

        return anchorVideoLogic.clientListVideoByVideoIds(ids);
    }

    /**
     * 客户端删除视频
     *
     * @param ids 视频uuid集合
     *
     * @return
     */
    @PostMapping("/clientDeleteVideo")
    @Operation(summary = "客户端删除视频")
    public R<String> clientDeleteVideo(@Parameter(description = "视频uuid集合", required = true) @RequestBody List<String> ids) {

        return anchorVideoLogic.clientDeleteVideo(ids);
    }

    /**
     * 客户端删除云空间视频
     *
     * @param videoId 视频uuid
     *
     * @return
     */
    @GetMapping("/clientDeleteCloudVideo")
    @Operation(summary = "客户端删除云空间视频")
    public R<String> clientDeleteCloudVideo(@Parameter(description = "视频uuid", required = true) @RequestParam String videoId) {

        return anchorVideoLogic.clientDeleteCloudVideo(videoId);
    }

    /**
     * 客户端批量删除云空间视频。
     * 业务逻辑与 {@link #clientDeleteCloudVideo(String)} 一致：按集合一次性处理；
     * 所有 DB 操作恒为常数次（无 N+1），单批最多 100 条（超出请前端分批调用）。
     *
     * @param videoIds 视频uuid集合
     * @return 删除结果文案，含 成功 / 跳过(无权限或非云端) / VOD 失败 / 不存在 计数
     */
    @PostMapping("/clientBatchDeleteCloudVideos")
    @Operation(summary = "客户端批量删除云空间视频")
    public R<String> clientBatchDeleteCloudVideos(@Parameter(description = "视频uuid集合（单批最多 100 条）", required = true) @RequestBody List<String> videoIds) {

        return anchorVideoLogic.clientBatchDeleteCloudVideos(videoIds);
    }

    /**
     * 客户端获取云空间视频列表
     *
     * @param clientVideoListBo 查询参数
     *
     * @return
     */
    @Operation(summary = "客户端获取云空间视频列表")
    @PostMapping("/clientListCloudVideo")
    public R<PageUtils<AnchorVideoInfoVo>> clientListCloudVideo(@RequestBody ClientVideoListBo clientVideoListBo) {
        R<PageUtils<AnchorVideoInfoVo>> result = anchorVideoLogic.clientListCloudVideo(clientVideoListBo);
        if (result.success() && result.getData() != null && CollUtil.isNotEmpty(result.getData().getList())) {
            textNotesService.fullExistsNotes(result.getData().getList(), AnchorVideoInfoVo::getVideoId, VideoSourceType.LOCAL, AnchorVideoInfoVo::setNotesSummary, AnchorVideoInfoVo::setExistsNotes);
            analysisMarkProducer.fullExistsMark(result.getData().getList(), AnchorVideoInfoVo::getVideoId, VideoSourceType.LOCAL, AnchorVideoInfoVo::setExistsMark);
        }
        return result;
    }

    /**
     * 客户端根据视频唯一标识。获取视频信息
     *
     * @param videoId 视频唯一标识
     *
     * @return
     */
    @GetMapping("/clientGetVideoByVideoId")
    @Operation(summary = "根据视频唯一标识。获取视频信息")
    public R<AnchorVideoInfoVo> clientGetVideoByVideoId(@Parameter(description = "视频唯一标识") @RequestParam String videoId) {

        return anchorVideoLogic.clientGetVideoByVideoId(videoId);
    }

    /**
     * 根据 batchNumber 查询整场直播的第一条记录（paragraph=0），用于计算分段偏移
     *
     * @param batchNumber 直播场次 room_id
     * @return 场次第一条视频信息（含 startTime 等）
     */
    @GetMapping("/clientGetLiveSessionByBatchNumber")
    @Operation(summary = "根据 batchNumber 查询整场直播的原始开始时间")
    public R<AnchorVideoInfoVo> clientGetLiveSessionByBatchNumber(@Parameter(description = "直播场次 room_id") @RequestParam String batchNumber) {
        return anchorVideoLogic.getLiveSessionByBatchNumber(batchNumber);
    }

    /**
     * 获取用户视频列表
     *
     * @param listUserVideoByConditionBo 查询参数
     *
     * @return
     */
    @PostMapping("/listUserVideo")
    @Operation(summary = "获取用户视频列表")
    public R<List<AnchorVideoInfoVo>> listUserVideo(@RequestBody ListUserVideoByConditionBo listUserVideoByConditionBo) {

        return anchorVideoLogic.listUserVideo(listUserVideoByConditionBo);
    }

    /**
     * 查询最近直播场次（主播下播后平台数据补采集专用）。
     *
     * @param listRecentLiveSessionBo 查询参数（仅开播时间下限 startTimeGe）
     * @return 场次代表视频列表
     */
    @PostMapping("/listRecentLiveSessions")
    @Operation(summary = "查询最近直播场次（主播下播后平台数据补采集）")
    public R<List<AnchorVideoInfoVo>> listRecentLiveSessions(@RequestBody ListRecentLiveSessionBo listRecentLiveSessionBo) {

        return anchorVideoLogic.listRecentLiveSessions(listRecentLiveSessionBo);
    }

    /**
     * 修改视频的分析状态
     *
     * @param updateVideoAnalysisStatusBo 修改参数
     *
     * @return
     */
    @Operation(summary = "修改视频的分析状态")
    @PostMapping("/updateVideoAnalysisStatus")
    public R<String> updateVideoAnalysisStatus(@RequestBody UpdateVideoAnalysisStatusBo updateVideoAnalysisStatusBo) {

        return anchorVideoLogic.updateVideoAnalysisStatus(updateVideoAnalysisStatusBo);
    }

    /**
     * 修改视频的上传状态
     *
     * @param updateVideoUploadStatusBo 修改参数
     *
     * @return
     */
    @Operation(summary = "修改视频的上传状态")
    @PostMapping("/updateVideoUploadStatus")
    public R<String> updateVideoUploadStatus(@RequestBody UpdateVideoUploadStatusBo updateVideoUploadStatusBo) {

        return anchorVideoLogic.updateVideoUploadStatus(updateVideoUploadStatusBo);
    }

    /**
     * 批量修改视频的大小时长
     *
     * @param updateVideoUploadStatusBoList 视频大小时长集合
     *
     * @return
     */
    @Operation(summary = "批量修改视频的大小时长")
    @PostMapping("/updateVideoSizeDuration")
    public R<String> updateVideoSizeDuration(@RequestBody List<UpdateVideoSizeDurationBo> updateVideoUploadStatusBoList) {

        return anchorVideoLogic.updateVideoSizeDuration(updateVideoUploadStatusBoList);
    }

    /**
     * 初始化视频表和文件，将分析中的改成分析失败
     *
     * @return
     */
    @Operation(summary = "初始化视频表和文件，将分析中的改成分析失败")
    @GetMapping("/initVideoAndFileAnalysisStatus")
    public R<String> initVideoAndFileAnalysisStatus() {

        return anchorVideoLogic.initVideoAndFileAnalysisStatus();
    }

    /**
     * 保存或修改视频信息
     *
     * @param anchorVideoInfoBo 视频信息
     *
     * @return
     */
    @PostMapping("/saveOrUpdateVideo")
    @Operation(summary = "保存或修改视频信息")
    public R<String> saveOrUpdateVideo(@RequestBody AnchorVideoInfoBo anchorVideoInfoBo) {

        return anchorVideoLogic.saveOrUpdateVideo(anchorVideoInfoBo);
    }

    /**
     * 批量保存巨量拉取的视频记录
     *
     * @param videoList 视频列表
     * @return
     */
    @PostMapping("/savePulledVideos")
    @Operation(summary = "批量保存巨量拉取的视频记录")
    public R<String> savePulledVideos(@RequestBody List<AnchorVideoInfoBo> videoList) {
        return anchorVideoLogic.savePulledVideos(videoList);
    }
    


    /**
     * PC后端获取分析数据
     *
     * @param fileId  文件ID
     * @param videoId 视频ID
     *
     * @return
     */
    @GetMapping("/getAnalysisInfo")
    @Operation(summary = "查看视频或文件分析内容")
    public R<OnlineAnalysisInfoVo> getAnalysisInfo(@RequestParam(required = false) String fileId, @RequestParam(required = false) String videoId) throws Exception {

        return anchorVideoLogic.getAnalysisInfo(fileId, videoId);
    }

    /**
     * 客户端根据租户id获取用户在服务器上的视频列表
     *
     * @return
     */
    @GetMapping("/getVideoListByTenantId")
    @Operation(summary = "客户端根据租户id获取用户在服务器上的视频列表")
    public R<List<AnchorVideoInfoVo>> getVideoListByTenantId() {
        return anchorVideoLogic.getVideoListByTenantId();
    }

    /**
     * 根据视频唯一标识。获取视频信息
     *
     * @param videoId 视频唯一标识
     *
     * @return
     */
    @GetMapping("/infoByVideoId")
    @Operation(summary = "根据视频唯一标识。获取视频信息")
    public R<AnchorVideoInfoVo> infoByVideoId(@Parameter(description = "视频唯一标识") @RequestParam String videoId) {

        return anchorVideoLogic.infoByVideoId(videoId);
    }

    /**
     * 客户端同步分析数据到服务器
     *
     * @param syncVideoAnalysisBo 分析数据
     *
     * @return
     */
    @PostMapping("/syncVideoAnalysisToServer")
    @Operation(summary = "客户端同步分析数据到服务器")
    public R<String> syncVideoAnalysisToServer(@RequestBody SyncVideoAnalysisBo syncVideoAnalysisBo) {

        return anchorVideoLogic.syncVideoAnalysisToServer(syncVideoAnalysisBo);
    }

    /**
     * 判断服务器是否有视频的分析数据
     *
     * @param videoId 视频唯一标识
     * @param tradeId 行业id
     *
     * @return
     */
    @GetMapping("/checkVideoAnalysisExist")
    @Operation(summary = "判断服务器是否有视频的分析数据")
    public R<Boolean> checkVideoAnalysisExist(@Parameter(description = "视频唯一标识") @RequestParam String videoId,
                                              @Parameter(description = "行业id") @RequestParam Long tradeId) {

        return anchorVideoLogic.checkVideoAnalysisExist(videoId, tradeId);
    }

    /**
     * 从云点播删掉视频文件， 加回云空间容量
     *
     * @param videoId 视频唯一标识
     *
     * @return
     */
    @GetMapping("/deleteOnlineVideo")
    @Operation(summary = "从云点播删掉视频文件，加回云空间容量")
    public R<String> deleteOnlineVideo(@Parameter(description = "视频唯一标识") @RequestParam String videoId) {

        return anchorVideoLogic.deleteOnlineVideo(videoId);
    }

    /**
     * 根据user_id获取录制分析
     *
     * @param anchorVideoVO
     *
     * @return
     */
    @PostMapping("/videoAnalysisByUserId")
    @Operation(summary = "根据user_id获取录制分析")
    public R<PageUtils<AnchorVideoVO>> videoAnalysisByUserId(@RequestBody AnchorVideoVO anchorVideoVO) {
        return anchorVideoLogic.videoAnalysisByUserId(anchorVideoVO);
    }

    /**
     * 根据视频ID获取分析记录内容
     *
     * @param videoId
     *
     * @return
     */
    @GetMapping("/videoAnalysisByVideoId")
    @Operation(summary = "根据video_id获取录制分析")
    public R<AnchorVideoVO> videoAnalysisByVideoId(@Parameter(description = "视频唯一标识") @RequestParam(value = "videoId", required = false) String videoId) {

        return anchorVideoLogic.videoAnalysisByVideoId(videoId);
    }

    /**
     * 根据主播sec_uid获取已录制该主播的视频
     *
     * @param anchorVideoBo
     *
     * @return
     */
    @PostMapping("/selectVideoBySecUid")
    @Operation(summary = "根据主播sec_uid获取已录制该主播的视频")
    public R<PageUtils<AnchorVideoVO>> selectVideoBySecUid(@RequestBody AnchorVideoBo anchorVideoBo) {

        return anchorVideoLogic.selectVideoBySecUid(anchorVideoBo);

    }

    /**
     * 客户端保存录制视频信息
     *
     * @return
     */
    @PostMapping("/saveVideoinfo")
    @Operation(summary = "客户端保存录制视频信息")
    public R<String> save(@RequestBody AnchorVideoInfoBo anchorVideoInfoBo) {
        anchorVideoLogic.save(anchorVideoInfoBo);
        return R.ok("保存成功");
    }

    // @PostMapping("/pageByUserId")
    // @Operation(summary = "客户端分页查询录制视频信息")
    // public R<PageUtils<AnchorVideoVO>>pageByUserId(@RequestBody AnchorVideoBo AnchorVideoBo){
    //     return anchorVideoLogic.pageByUserId(AnchorVideoBo);
    // }

    /**
     * 客户端查询录制视频信息
     *
     * @return
     */
    @GetMapping("/selectByuserId")
    @Operation(summary = "客户端查询录制视频信息")
    public R<List<AnchorVideoVO>> selectByuserId() {
        return anchorVideoLogic.selectByuserId();
    }


    /**
     * 服务端分页查询录制视频信息
     *
     * @param AnchorVideoBo
     *
     * @return
     */
    @PostMapping("/pageLists")
    @Operation(summary = "服务端分页查询录制视频信息")
    public R<PageUtils<AnchorVideoVO>> pageLists(@RequestBody AnchorVideoBo AnchorVideoBo) {
        return anchorVideoLogic.pageLists(AnchorVideoBo);
    }


    /**
     * 根据视频Id查询该视频的分析内容
     *
     * @param videoId
     *
     * @return
     */
    @PostMapping("/selectByVideoId")
    @Operation(summary = "服务端根据视频Id查询该视频的分析内容")
    public R<List<AudioAnalysisEntity>> selectByVideoId(@RequestBody AudioAnalysisBo videoId) {
        R<List<AudioAnalysisEntity>> listR = anchorVideoLogic.selectByVideoId(videoId);
        return listR;
    }

    /**
     * 根据视频id获取分析内容
     *
     * @param videoId
     * @param tradeId
     *
     * @return
     */
    @GetMapping("/selectAnalysisByVideoId")
    @Operation(summary = "根据视频ID查询视频的分析内容")
    public R<List<SentenceMarkVo>> selectAnalysisByVideoId(@RequestParam String videoId, @RequestParam Long tradeId) {
        return videoAnalysisRecordLogic.selectAnalysisByVideoId(videoId, tradeId);
    }

    /**
     * 根据文件id获取分析内容
     *
     * @param fileId
     * @param tradeId
     *
     * @return
     */
    @GetMapping("/selectAnalysisByFileId")
    @Operation(summary = "根据文件id获取分析内容")
    public R<List<SentenceMarkVo>> selectAnalysisByFileId(@RequestParam String fileId, @RequestParam Long tradeId) {
        return uploadFileAnalysisRecordLogic.selectAnalysisByFileId(fileId, tradeId);
    }

    /**
     * 根据文件id获取分析内容
     * @param fileId
     * @return
     */
//    @GetMapping("/selectAnalysisByFileId")
//    @Operation(summary = "根据视频ID查询视频的分析内容")
//    public R<List<OnlineAnalysisItemVo>> selectAnalysisByFileId(@RequestParam String fileId){
//        return videoAnalysisRecordLogic.selectAnalysisByFileId(fileId);
//    }


    /**
     * 新增直播视频分析的内容
     * @param udioAnalysisVo
     * @return
     */
    // @PostMapping("/saveAudioAnalysis")
    // @Operation(summary = "新增直播视频分析的内容")
    // public R<String> saveAudioAnalysis(@RequestBody List<AudioAnalysisVo> udioAnalysisVo){
    //     anchorVideoLogic.saveAudioAnalysis(udioAnalysisVo);
    //     return R.ok();
    //
    // }

    /**
     * 客户端更新视频信息
     *
     * @param anchorVideoInfoBo
     *
     * @return
     */
    @PostMapping("/UpdateVideo")
    @Operation(summary = "客户端更新视频信息")
    public R<String> updateVideo(@RequestBody AnchorVideoInfoBo anchorVideoInfoBo) {
        anchorVideoLogic.updateVideo(anchorVideoInfoBo);
        return R.ok();
    }

    /**
     * 客户端保存分析记录
     *
     * @param udioAnalysisVo
     *
     * @return
     */
    @PostMapping("/saveAnchorVideoRecod")
    @Operation(summary = "保存分析记录")
    public R<String> saveAnchorVideoRecod(@RequestBody List<AudioAnalysisVo> udioAnalysisVo) {
        anchorVideoLogic.saveAnchorVideoRecod(udioAnalysisVo);
        return R.ok();
    }

    /**
     * 服务端分析记录查询
     *
     * @param anchorVideoBo
     *
     * @return
     */
    @PostMapping("/selectAnchorVideoRecod")
    @Operation(summary = "服务端分析记录查询")
    public R<PageUtils<AnchorVideoVO>> selectAnchorVideoRecod(@RequestBody AnchorVideoBo anchorVideoBo) {
        return anchorVideoLogic.selectAnchorVideoRecod(anchorVideoBo);
    }

    /**
     * 根据客户端上传的List<voidId>删除
     *
     * @param voidId
     *
     * @return
     */
    @PostMapping("/removeByVoidId")
    @Operation(summary = "根据客户端上传的List<voidId>删除")
    public R<String> removeByVoidId(@RequestBody List<String> voidId) {
        return anchorVideoLogic.removeByVoidId(voidId);
    }

    /**
     * 客户端根据视频Id和行业ID查询该视频的分析内容
     *
     * @param audioAnalysisBo
     *
     * @return
     */
    @PostMapping("/selectByVideoIdOrTradeId")
    @Operation(summary = "Id查询该视频的分析内容")
    public R<List<AudioAnalysissVO>> selectByVideoIdOrTradeId(@RequestBody AudioAnalysisBo audioAnalysisBo) {
        R<List<AudioAnalysissVO>> listR = anchorVideoLogic.selectByVideoIdOrTradeId(audioAnalysisBo);
        return listR;
    }

    /**
     * 服务端用户详情查询录制记录
     */
    @PostMapping("/listByUserId")
    @Operation(summary = "服务端查询录制记录")
    public R<PageUtils<AnchorVideoVO>> listByUserId(@RequestBody AnchorVideoBo anchorVideoBo) {
        return anchorVideoLogic.listByUserId(anchorVideoBo);
    }

    /**
     * 服务端用户详情查询分析记录
     */
    @PostMapping("/selectVideoRecod")
    @Operation(summary = "服务端用户详情查询分析记录")
    public R<PageUtils<AnchorVideoRecodListVo>> selectVideoRecod(@RequestBody AnchorVideoBo anchorVideoBo) {
        return anchorVideoLogic.selectVideoRecod(anchorVideoBo);
    }


    /**
     * 清除视频的分析数据
     *
     * @param videoId 视频唯一标识
     *
     * @return
     */
    @GetMapping("/clearAnalysis")
    @Operation(summary = "清除视频的分析数据")
    public R<String> clearAnalysis(@RequestParam String videoId) {
        this.anchorVideoLogic.clearAnalysis(videoId);
        return R.ok();
    }

    @GetMapping("startImportantBarrage")
    @Schema(description = "开始获取重要弹幕")
    public R<Boolean> startImportantBarrage(@RequestParam @NotNull(message = "视频id不能为空") String videoId) {
        return anchorVideoLogic.startImportantBarrage(videoId);
    }

    @GetMapping("/getDataDiagnosisConfig")
    @Operation(summary = "获取数据诊断配置")
    public R<DataDiagnosisConfigVo> getDataDiagnosisConfig(String sourceId,
                                                           Integer sourceType,
                                                           @RequestHeader(value = "webVersion", required = false) String webVersion) {
        return R.ok(anchorVideoLogic.getDataDiagnosisConfig(sourceId, sourceType, webVersion));
    }

    @PostMapping("/updateDataDiagnosisConfig")
    @Operation(summary = "更新数据诊断配置")
    public R<DataDiagnosisConfigVo> updateDataDiagnosisConfig(@RequestBody @Validated DataDiagnosisConfigVo dataDiagnosisConfigVo,
                                                              @RequestHeader(value = "webVersion", required = false) String webVersion) {
        anchorVideoLogic.updateDataDiagnosisConfig(dataDiagnosisConfigVo);
        return getDataDiagnosisConfig(dataDiagnosisConfigVo.getSourceId(), dataDiagnosisConfigVo.getSourceType(), webVersion);
    }

    /**
     * 根据secUid统计视频数量
     *
     * @param secUid 主播唯一标识
     * @return 视频数量
     */
    @GetMapping("/countVideoBySecUid")
    @Operation(summary = "根据secUid统计视频数量")
    public R<Integer> countVideoBySecUid(@Parameter(description = "主播唯一标识") @RequestParam String secUid) {
        return anchorVideoLogic.countBySecUid(secUid);
    }
}
