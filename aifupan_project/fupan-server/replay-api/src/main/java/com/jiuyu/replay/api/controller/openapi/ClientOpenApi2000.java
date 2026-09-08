package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.logic.words.DataModelLogic;
import com.jiuyu.replay.api.logic.words.SensitiveWordsLogic;
import com.jiuyu.replay.api.task.UserRollupScheduledTasks;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.WordsMarkBo;
import com.jiuyu.replay.words.bo.WordsMarkReAnalysisBo;
import com.jiuyu.replay.words.vo.AnalysisContractResultCloudVo;
import com.jiuyu.replay.words.vo.AnalysisResultCloudVo;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.words.vo.DataModelInfoVo;
import com.jiuyu.replay.words.vo.VideoRoiVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("replay/openapi/v2000")
@Tag(name = "客户端openAPI-2.0.00")
public class ClientOpenApi2000 {

    @Resource
    private DataModelLogic dataModelLogic;
    @Resource
    private SensitiveWordsLogic sensitiveWordsLogic;
    @Resource
    private UserRollupScheduledTasks userRollupScheduledTasks;

    /**
     * 根据视频id或对比id获取模型列表
     * @param uuid 视频id或对比id
     * @param type 类型 0：视频 1：对比 2：文件
     * @return
     */
    @Operation(summary = "根据视频id或对比id获取模型列表")
    @GetMapping("/listTradeModel")
    public R<List<DataModelInfoVo>> listTradeModel(@Parameter(description = "视频id或对比id", required = true)@RequestParam String uuid,
                                                   @Parameter(description = "类型 0：视频 1：对比 2：文件", required = true)@RequestParam Integer type) {

        return dataModelLogic.listTradeModel(uuid, type);
    }

    /**
     * 文字关键词/敏感词标识2_0
     * @return
     */
    @Operation(summary = "文字关键词/敏感词标识2_0")
    @PostMapping("/wordsMark")
    public R<AnalysisResultVo> wordsMark2_0(@Parameter(description = "文字内容对象", required = true) @RequestBody List<WordsMarkBo> wordsMarkBoList) throws Exception {

        return sensitiveWordsLogic.wordsMark2_0(wordsMarkBoList);

    }

    /**
     * 文本内容关键词/敏感词标识2_0
     * @return
     */
    @Operation(summary = "文本关键词/敏感词标识2_0")
    @PostMapping("/wordsMarkByText")
    public R<AnalysisResultVo> wordsMarkByText2_0(@Parameter(description = "文字内容对象", required = true) @RequestBody WordsMarkBo wordsMarkBo) {

        return sensitiveWordsLogic.wordsMarkByText2_0(wordsMarkBo);
    }

    /**
     * 二次分析2_0
     * @return
     */
    @Operation(summary = "二次分析2_0")
    @PostMapping("/wordsMarkReAnalysis")
    public R<AnalysisResultVo> wordsMarkReAnalysis2_0(@Parameter(description = "二次分析对象", required = true) @RequestBody WordsMarkReAnalysisBo wordsMarkReAnalysisBo) {

        return sensitiveWordsLogic.wordsMarkReAnalysis2_0(wordsMarkReAnalysisBo);
    }

    /**
     * 下载分析文件2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "下载分析文件2_0")
    @GetMapping("/downloadAnalysisFile/{type}/{uuid}")
    public byte[] downloadAnalysisFile2_0(@PathVariable Integer type, @PathVariable String uuid) {

        return sensitiveWordsLogic.downloadAnalysisFile2_0(type, uuid);
    }

    /**
     * 获取在线复盘分析信息，以zip形式返回2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线复盘分析信息，以zip形式返回2_0")
    @GetMapping("/getOnlineAnalysisZip/{type}/{uuid}")
    public byte[] getOnlineAnalysisZip2_0(@PathVariable Integer type, @PathVariable String uuid) throws Exception {

        return sensitiveWordsLogic.getOnlineAnalysisZip2_0(type, uuid);
    }

    /**
     * 获取在线对比复盘分析信息，以zip形式返回2_0
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线对比复盘分析信息，以zip形式返回2_0")
    @GetMapping("/getOnlineContrastAnalysisZip")
    public byte[] getOnlineContrastAnalysisZip2_0(@RequestParam(required = false) String contrastId) throws Exception {

        return sensitiveWordsLogic.getOnlineContrastAnalysisZip2_0(contrastId);
    }

    /**
     * 获取在线复盘分析信息2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线复盘分析信息2_0")
    @GetMapping("/getOnlineAnalysis")
    public R<AnalysisResultCloudVo> getOnlineAnalysis(
            @Parameter(description = "类型 0：视频 1：文件", required = true)@RequestParam Integer type,
            @Parameter(description = "视频或文件的唯一标识 uuid", required = true)@RequestParam String uuid) throws Exception {

        return sensitiveWordsLogic.getOnlineAnalysis2_0(type, uuid);
    }

    /**
     * 获取在线复盘分析信息2_0
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取分析信息")
    @GetMapping("/getAnalysis")
    public R<AnalysisResultCloudVo> getAnalysis( @Parameter(description = "类型 0：视频 1：文件", required = true)@RequestParam Integer type,
                                                 @Parameter(description = "视频或文件的唯一标识 uuid", required = true)@RequestParam String uuid){
        return sensitiveWordsLogic.getAnalysis(type, uuid);
    }

    /**
     * 获取在线对比复盘分析信息2_0
     * @param contrastId 对比id
     * @return
     */
    @Operation(summary = "获取在线对比复盘分析信息2_0")
    @GetMapping("/getOnlineContrastAnalysis")
    public R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis(@Parameter(description = "对比id", required = true)@RequestParam(required = false) String contrastId) throws Exception {

        return sensitiveWordsLogic.getOnlineContrastAnalysis2_0(contrastId);
    }

    /**
     * 获取在线对比复盘分析信息2_0
     * @param contrastId 对比id
     * @return
     */
    @Operation(summary = "获取对比复盘分析信息")
    @GetMapping("/getContrastAnalysis")
    public R<AnalysisContractResultCloudVo> getContrastAnalysis(@Parameter(description = "对比id", required = true)@RequestParam(required = false) String contrastId){

        return sensitiveWordsLogic.getContrastAnalysis(contrastId);
    }

    /**
     * 调用更新用户分析汇总数据接口
     */
    @GetMapping("/invokeTimingUpdateData")
    @Operation(summary = "调用更新用户分析汇总数据接口")
    public R<String> invokeTimingUpdateData() throws ParseException {
        userRollupScheduledTasks.timingUpdateData();
        return R.ok("");
    }

    /**
     * 查询视频ROI信息
     * @param videoId 视频唯一标识
     * @return
     */
    @Operation(summary = "查询视频ROI信息")
    @GetMapping("/getVideoRoi")
    public R<VideoRoiVo> getVideoRoi(@Parameter(description = "视频唯一标识", required = true) @RequestParam String videoId) {
        return sensitiveWordsLogic.getVideoRoi(videoId);
    }

}