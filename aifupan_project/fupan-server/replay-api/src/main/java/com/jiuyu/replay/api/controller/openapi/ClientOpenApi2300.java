package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.annotation.TenantLock;
import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.third.AiRelatedLogic;
import com.jiuyu.replay.api.logic.third.ThirdLogic;
import com.jiuyu.replay.api.logic.words.AnchorUrlLogic;
import com.jiuyu.replay.api.logic.words.AnchorVideoLogic;
import com.jiuyu.replay.api.logic.words.DataScreenshotLogic;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.common.utils.CustomizeSseEmitter;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.words.bo.DataScreenshotBo;
import com.jiuyu.replay.words.bo.DataScreenshotUploadBo;
import com.jiuyu.replay.words.bo.ScreenshotAnalysisBo;
import com.jiuyu.replay.words.bo.SyncLocalDataToServerBo;
import com.jiuyu.replay.words.vo.AiOptionConfigVo;
import com.jiuyu.replay.words.vo.DataScreenshotInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @author ：lujie
 * @description：客户端openAPI-2.3.00
 * @date ：2025/3/18 上午11:45
 */
@RestController
@CrossOrigin
@RequestMapping("replay/openapi/v2300")
@Tag(name = "客户端openAPI-2.3.00")
@AllArgsConstructor
public class ClientOpenApi2300 {

    private final ImgOssUtils imgOssUtils;
    private final OssUtils ossUtils;
    private final DataScreenshotLogic dataScreenshotLogic;
    private final AnchorUrlLogic anchorUrlLogic;
    private final AiRelatedLogic aiRelatedLogic;
    private final ThirdLogic thirdLogic;
    private final AnchorVideoLogic anchorVideoLogic;

    /**
     * 检查用户是否已经同步过本地数据到服务器
     * @return
     */
    @Operation(summary = "检查用户是否已经同步过本地数据到服务器")
    @GetMapping("/checkUserSyncLocalData")
    public R<Integer> checkUserSyncLocalData() {

        return anchorVideoLogic.checkUserSyncLocalData();
    }

    /**
     * 同步本地视频、文件、对比到服务器
     * @param syncLocalDataToServerBo id参数集合
     * @return
     */
    @Operation(summary = "同步本地视频、文件、对比到服务器")
    @PostMapping("/syncLocalDataToServer")
    public R<String> syncLocalDataToServer(@RequestBody SyncLocalDataToServerBo syncLocalDataToServerBo) {

        return anchorVideoLogic.syncLocalDataToServer(syncLocalDataToServerBo);
    }

    /**
     * 更新主播最后开始录制时间
     * @param secUid 主播secuid
     * @param lastRecordTime 最后开始录制时间
     * @return
     */
    @Operation(summary = "更新主播最后开始录制时间")
    @GetMapping("/updateAnchorLastRecordTime")
    public R<String> updateAnchorLastRecordTime(@Parameter(description = "主播secuid", required = true)@RequestParam("secUid") String secUid,
                                     @Parameter(description = "最后开始录制时间", required = true)@RequestParam("lastRecordTime") String lastRecordTime) {

        return anchorUrlLogic.updateAnchorLastRecordTime(secUid, lastRecordTime);
    }

    /**
     * 更新主播置顶信息
     * @param secUid 主播secuid
     * @param action 动作 0：取消置顶 1：置顶
     * @param addTopTime 添加置顶的时间
     * @return
     */
    @Operation(summary = "更新主播置顶信息")
    @GetMapping("/updateAnchorTop")
    public R<String> updateAnchorTop(@Parameter(description = "主播secuid", required = true)@RequestParam("secUid") String secUid,
                                     @Parameter(description = "动作 0：取消置顶 1：置顶", required = true)@RequestParam("action") Integer action,
                                     @Parameter(description = "添加置顶的时间", required = true)@RequestParam("addTopTime") String addTopTime) {

        return anchorUrlLogic.updateAnchorTop(secUid, action, addTopTime);
    }


    @Operation(summary = "获取数据截图上传的预签名链接")
    @GetMapping("/getDataScreenshotPutUrl")
    public R<SignUploadUrlVo> getDataScreenshotPutUrl(String suffix){
        return R.ok(imgOssUtils.getDataScreenshotUploadUrl(suffix));
    }

    @Operation(summary = "获取OSS上传的预签名链接")
    @GetMapping("/getOssUploadSignUrl")
    public R<SignUploadUrlVo> getOssUploadSignUrl(@RequestParam String bucket, @RequestParam String ossKey) {
        return R.ok(ossUtils.getSignUploadUrl(bucket, ossKey));
    }

    @Operation(summary = "数据截图上传")
    @PostMapping("/screenshotUpload")
    @UserLock
    public R<DataScreenshotInfoVo> screenshotUpload(@RequestBody DataScreenshotUploadBo uploadBo){
        uploadBo.setSourceImagesType(0);
        return dataScreenshotLogic.screenshotUpload(uploadBo);
    }

    @Operation(summary = "数据截图分析")
    @PostMapping("/screenshotAnalysis")
    @TenantLock
    public SseEmitter screenshotAnalysis(@RequestBody ScreenshotAnalysisBo analysisBo) {
        CustomizeSseEmitter emitter = new CustomizeSseEmitter(240000L);
        dataScreenshotLogic.screenshotAnalysis(emitter, analysisBo);

        return emitter;
    }

    @Operation(summary = "根据视频查询数据截图列表")
    @GetMapping("/dataScreenshotList")
    public R<List<DataScreenshotListVo>> dataScreenshotList(@RequestParam Integer sourceType, @RequestParam String sourceId){
        return dataScreenshotLogic.dataScreenshotList(sourceType, sourceId);
    }

    @Operation(summary = "查询单个图片的数据")
    @GetMapping("/screenshotInfo")
    public R<DataScreenshotInfoVo> screenshotByCode(@RequestParam Long id){
        return dataScreenshotLogic.info(id);
    }

    @Operation(summary = "修改图片的数据")
    @PostMapping("/updateScreenshot")
    public R<String> updateScreenshot(@RequestBody DataScreenshotBo dataScreenshotBo){
        RRException.isNotEmpty(dataScreenshotBo.getId(), "id不能为空");
        RRException.isNotEmpty(dataScreenshotBo.getAiContent(), "ai识别的内容不能为空");
        DataScreenshotBo bo = new DataScreenshotBo();
        bo.setId(dataScreenshotBo.getId());
        bo.setAiContent(dataScreenshotBo.getAiContent());
        return dataScreenshotLogic.update(bo);
    }

    @Operation(summary = "数据截图删除")
    @PostMapping("/dataScreenshotDelete")
    public R<String> dataScreenshotDelete(@RequestBody DataScreenshotBo dataScreenshotBo){
        RRException.isNotEmpty(dataScreenshotBo.getId(), "id不能为空");
        return dataScreenshotLogic.delete(dataScreenshotBo.getId());
    }

    @Operation(summary = "查询视频中有效的数据截图")
    @GetMapping("/getExistDataScreenshotList")
    public R<List<DataScreenshotListVo>> getExistDataScreenshotList(@RequestParam Integer sourceType, @RequestParam String sourceId){
        return dataScreenshotLogic.getExistDataScreenshotList(sourceType, sourceId);
    }

    @Operation(summary = "获取视频对应的ai配置选项")
    @GetMapping("/aiOptionConfig")
    public R<AiOptionConfigVo> aiOptionConfig(@RequestParam Integer sourceType, @RequestParam String sourceId){
        return aiRelatedLogic.aiOptionConfig(sourceType, sourceId);
    }

    @Operation(summary = "图片内容安全检测")
    @GetMapping("/imageSecurity")
    public R<Boolean> imageSecurity(@RequestParam String key){
        return thirdLogic.imageSecurity(key, 0);
    }


}
