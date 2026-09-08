package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.DiagnosisCueBll;
import com.jiuyu.replay.ai.bll.DiagnosisModelBll;
import com.jiuyu.replay.ai.bo.DiagnosisSignUploadUrlBo;
import com.jiuyu.replay.ai.bo.SaveDiagnosisBo;
import com.jiuyu.replay.ai.bo.UpdateReadStatusBo;
import com.jiuyu.replay.ai.bo.conversationByCueWordsIdsBo;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.SaveDiagnosisCueBo;
import com.jiuyu.replay.generic.vo.ai.AiDiagnosisCueVo;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.ai.DataDiagnosisStatusVo;
import com.jiuyu.replay.generic.vo.ai.UnreadDiagnosisReportVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/24 下午7:11
 */
@RestController
@CrossOrigin
@RequestMapping("replay/ai/diagnosis")
@Tag(name = "ai诊断相关的控制器")
@AllArgsConstructor
public class DiagnosisController {

    private final DiagnosisCueBll diagnosisBll;
    private final DiagnosisModelBll diagnosisModelBll;


    @GetMapping("/getHandelSuccessDiagnosis")
    @Operation(summary = "获取视频处理完成的诊断报告问题")
    public R<List<DiagnosisCueInfoVo>> getHandelSuccessDiagnosis(@RequestParam(value = "videoId", required = false) String videoId,
                                                                 @RequestParam(value = "sourceId", required = false) String sourceId,
                                                                 @RequestParam(value = "diagnosisType", required = false) Integer diagnosisType) {
        if (sourceId == null) sourceId = videoId;
        if (diagnosisType == null) diagnosisType = AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode();
        if (sourceId == null) {
            return R.error("缺少来源id");
        }
        return R.ok(diagnosisBll.getHandelSuccessDiagnosis(sourceId, diagnosisType));
    }

    @GetMapping("/handleDiagnosisByUser")
    @Operation(summary = "获取用户待分析和分析中的诊断报告")
    public R<List<DiagnosisCueInfoVo>> handleDiagnosisByUser(){
        return R.ok(diagnosisBll.handleDiagnosisByUser());
    }

    @PostMapping("/saveDiagnosis")
    @Operation(summary = "诊断分析-提示词添加分析")
    public R<List<DiagnosisCueInfoVo>> saveDiagnosis(@RequestBody SaveDiagnosisBo saveDiagnosisBo){
        return R.ok(diagnosisBll.saveDiagnosis(saveDiagnosisBo));
    }

    @GetMapping("/isGenerateDiagnosisFile")
    @Operation(summary = "查询视频是否需要生成诊断报告")
    public R<String> isGenerateDiagnosisFile(String sourceId, @RequestParam(value = "diagnosisType", required = false) Integer diagnosisType) {
        if (diagnosisType == null) diagnosisType = AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode();
        return diagnosisBll.isGenerateDiagnosisFile(sourceId, diagnosisType);
    }

    @GetMapping("/getAutoDiagnosisQuestions")
    @Schema(description = "获取要自动提问的诊断问题")
    public R<List<DiagnosisCueInfoVo>> getAutoDiagnosisQuestions(String videoId){
        return diagnosisBll.getAutoDiagnosisQuestions(videoId);
    }

    @PostMapping("/conversationByCueWordsIds")
    @Operation(summary = "根据来源id和类型查询ai模型")
    public R<List<ConversationVo>> conversationByCueWordsIds(@RequestBody conversationByCueWordsIdsBo conversationByCueWordsIdsBo) {
        return R.ok(diagnosisBll.conversationByCueWordsIds(conversationByCueWordsIdsBo));
    }

    @PostMapping("/getDiagnosisSignUploadUrl")
    @Operation(summary = "获取诊断报告上传的预签名链接")
    public R<SignUploadUrlVo> getDiagnosisSignUploadUrl(@RequestBody @Validated DiagnosisSignUploadUrlBo urlBo) {
        if (urlBo.getUploadType() == null) {
            urlBo.setUploadType(0);
        }
        return diagnosisBll.getDiagnosisSignUploadUrl(urlBo);
    }

    @GetMapping("/getDiagnosisDownloadUrl")
    @Operation(summary = "获取诊断报告下载的预签名链接")
    public R<String> getDiagnosisDownloadUrl(@RequestParam("videoId") String videoId,
                                             @RequestParam(value = "sourceType", required = false) Integer sourceType,
                                             @RequestParam(value = "uploadType", required = false) Integer uploadType) {
        if (uploadType == null) uploadType = 1;
        if (sourceType == null) sourceType = 0;
        return diagnosisBll.getDiagnosisDownloadUrl(videoId, sourceType, uploadType);
    }

    @PostMapping("/updateDiagnosisCueStatus")
    @Operation(summary = "更新ai诊断中提示词状态")
    public R<String> updateDiagnosisCueStatus(@RequestBody DiagnosisCueBo diagnosisCueBo){
        return diagnosisBll.updateDiagnosisCueStatus(diagnosisCueBo);
    }

    @GetMapping("/aiModelBySourceIdAndType")
    @Operation(summary = "根据来源id和类型查询ai模型")
    public R<AiModelInfoVo> aiModelBySourceIdAndType(@RequestParam("sourceId")  @Parameter(description = "来源id", required = true) String sourceId,
                                                     @RequestParam("sourceType") @Parameter(description = "来源类型 0主播 1视频", required = true) Integer sourceType,
                                                     @RequestParam(value = "diagnosisType", required = false) @Parameter(description = "诊断类型 0内容诊断 1数据诊断") Integer diagnosisType,
                                                     @RequestHeader(value = "webVersion", required = false) String webVersion
    ) {
        if (diagnosisType == null) diagnosisType = 0;
        return diagnosisBll.aiModelBySourceIdAndType(sourceId, sourceType, diagnosisType, webVersion);
    }

    @PostMapping("/saveDiagnosisCue")
    @Operation(summary = "保存ai诊断中的模型设置-主播和视频")
    public R<String> saveDiagnosisCue(@RequestBody SaveDiagnosisCueBo saveDiagnosisCueBo){
        return diagnosisBll.saveDiagnosisCue(saveDiagnosisCueBo);
    }

    @GetMapping("/listDiagnosis")
    @Operation(summary = "用户对应ai诊断提示词配置列表")
    public R<AiDiagnosisCueVo> listDiagnosis(@RequestParam(value = "sourceId", required = false)  @Parameter(description = "来源id") String sourceId,
                                             @RequestParam(value = "sourceType",required = false)  @Parameter(description = "来源类型 0主播 1视频")Integer sourceType,
                                             @RequestParam(value = "tradeId", required = false) @Parameter(description = "行业id") Long tradeId,
                                             @RequestHeader(value = "webVersion", required = false) String webVersion
                                             ){

        return diagnosisBll.listDiagnosis(sourceId, sourceType, tradeId, webVersion);
    }

    @GetMapping("/listDiagnosisModel")
    @Operation(summary = "ai诊断模型列表-主播和视频列表")
    public R<List<AiModelInfoVo>> listDiagnosisModel(@RequestHeader(value = "webVersion", required = false) String webVersion) {
        return diagnosisBll.listDiagnosisModel(webVersion);
    }

    @PostMapping("/updateDiagnosisModel")
    @Operation(summary = "更新主播获视频使用的诊断报告模型")
    public R<String> updateDiagnosisModel(@RequestBody DiagnosisModelBo diagnosisModelBo){
        diagnosisModelBll.updateDiagnosisModel(diagnosisModelBo);
        return R.ok();
    }

    @PostMapping("/updateReadStatus")
    @Operation(summary = "批量更新诊断报告已读状态")
    public R<String> updateReadStatus(@RequestBody @Validated UpdateReadStatusBo bo) {
        diagnosisBll.updateReadStatus(bo.getIds(), bo.getIsRead());
        return R.ok("更新成功");
    }

    @GetMapping("/listUnreadDataDiagnosis")
    @Operation(summary = "获取数据诊断未读报告列表")
    public R<List<UnreadDiagnosisReportVo>> listUnreadDataDiagnosis(@RequestParam(value = "videoSliceType", required = false) Integer videoSliceType) {
        if (videoSliceType == null) videoSliceType = 0;
        return diagnosisBll.listUnreadDataDiagnosis(videoSliceType);
    }

    @GetMapping("/getDataDiagnosisStatus")
    @Operation(summary = "获取视频数据诊断状态与内容")
    public R<DataDiagnosisStatusVo> getDataDiagnosisStatus(@RequestParam("videoId") @Parameter(description = "视频id", required = true) String videoId) {
        return diagnosisBll.getDataDiagnosisStatus(videoId);
    }
}
