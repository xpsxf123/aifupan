package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.ConversationBll;
import com.jiuyu.replay.ai.bo.UpdateCorrectStatusBo;
import com.jiuyu.replay.ai.bo.UpdateHtmlStatusBo;
import com.jiuyu.replay.ai.bo.UpdateLikesStatusBo;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.bo.ai.ConversationBo;
import com.jiuyu.replay.generic.bo.ai.ConversationListBo;
import com.jiuyu.replay.generic.vo.ai.ConversationPage;
import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/27 下午4:01
 */
@RestController
@CrossOrigin
@RequestMapping("replay/ai/conversation")
@Tag(name = "ai问答记录相关的控制器")
@AllArgsConstructor
public class ConversationController {

    private final ConversationBll conversationBll;
    private final ImgOssUtils imgOssUtils;

    @PostMapping("/updateLikesStatus")
    @Operation(summary = "更新点赞状态")
    public R<String> updateLikesStatus(@RequestBody UpdateLikesStatusBo updateLikesStatusBo){
        return conversationBll.updateLikesStatus(updateLikesStatusBo);
    }

    @PostMapping("/saveConversationData")
    @Operation(summary = "保存ai问答记录数据")
    public R<List<ConversationVo>> saveConversationData(@RequestBody List<ConversationBo> dataList){
        return R.ok(conversationBll.saveConversationData(dataList));
    }

    @PostMapping("/conversationPage")
    @Operation(summary = "分页查询ai问答记录数据")
    public R<ConversationPage<ConversationVo>> conversationPage(@RequestBody ConversationListBo listBo){
        return conversationBll.conversationPage(listBo);
    }

    @PostMapping("/isExist")
    @Operation(summary = "查询数据是否存在")
    public R<Boolean> isExist(@RequestBody ConversationBo conversationBo) {
        return R.ok("查询成功", conversationBll.isExist(conversationBo));
    }

    @GetMapping("/exportByQaCode")
    @Operation(summary = "根据qaCode获取对应的配置文件")
    public void exportByQaCode(HttpServletResponse response, @RequestParam("qaCodes") String qaCodes) throws IOException {
        if (qaCodes == null) {
            return;
        }
        conversationBll.exportByQaCode(response, List.of(qaCodes.split(",")));
    }

    @GetMapping("/getById")
    @Operation(summary = "根据问答记录id获取数据")
    public R<ConversationVo> getById(String id) {
        return R.ok(conversationBll.getById(id));
    }

    @PostMapping("/updateHtmlStatus")
    @Operation(summary = "更新HTML生成状态")
    public R<String> updateHtmlStatus(@RequestBody UpdateHtmlStatusBo updateHtmlStatusBo) {
        updateHtmlStatusBo.setHtmlType(AiEnums.htmlType.CLIENT.getCode());
        conversationBll.updateHtmlStatus(updateHtmlStatusBo);
        return R.ok("更新成功");
    }

    @GetMapping("/serviceGenerateHtml")
    @Operation(summary = "服务器生成html")
    public R<ConversationVo> serviceGenerateHtml(String id) {
        ConversationVo data = conversationBll.serviceGenerateHtml(id);
        data.setContent(null);
        return R.ok(data);
    }

    @GetMapping("/htmlProGenerateParams")
    @Operation(summary = "获取html生成前的参数(model-prompt)")
    public R<ConversationVo> htmlProGenerateParams(String id) {
        return R.ok(conversationBll.proGenerateHtml(id, AiEnums.htmlType.CLIENT.getCode()));
    }

    @GetMapping("/getHtmlTempToken")
    @Operation(summary = "获取html的模型配置")
    public R<AiTempTokenVo> getHtmlTempToken() {
        return R.ok(conversationBll.getHtmlTempToken());
    }

    @GetMapping("/getHtmlSignUploadUrl")
    @Operation(summary = "获取html上传的签名url")
    public R<SignUploadUrlVo> getHtmlSignUploadUrl() {
        return R.ok(imgOssUtils.getSignUploadUrl(ConversationBll.htmlPrefix, "html", MediaType.TEXT_HTML_VALUE));
    }

    @PostMapping("/getHtmlStatus")
    @Operation(summary = "获取html生成状态", description = "只有字段：id、htmlType、htmlStatus、htmlCreateDate、htmlSavePath、htmlCreateError")
    public R<List<ConversationVo>> getHtmlStatus(@RequestBody List<String> ids) {
        return R.ok(conversationBll.getHtmlStatus(ids));
    }

    // ==================== AI纠正内容相关接口 ====================

    @PostMapping("/updateCorrectStatus")
    @Operation(summary = "更新AI纠正状态")
    public R<String> updateCorrectStatus(@RequestBody UpdateCorrectStatusBo updateCorrectStatusBo) {
        updateCorrectStatusBo.setAiCorrectType(AiEnums.correctType.CLIENT.getCode());
        conversationBll.updateCorrectStatus(updateCorrectStatusBo);
        return R.ok("更新成功");
    }

    @GetMapping("/serviceCorrectAiContent")
    @Operation(summary = "服务器纠正AI内容")
    public R<ConversationVo> serviceCorrectAiContent(String id) {
        ConversationVo data = conversationBll.serviceCorrectAiContent(id);
        data.setContent(null);
        return R.ok(data);
    }

    @GetMapping("/correctProGenerateParams")
    @Operation(summary = "获取AI纠正前的参数(model-prompt)")
    public R<ConversationVo> correctProGenerateParams(String id) {
        return R.ok(conversationBll.proCorrectAiContent(id, AiEnums.correctType.CLIENT.getCode()));
    }

    @GetMapping("/getCorrectTempToken")
    @Operation(summary = "获取AI纠正的模型配置")
    public R<AiTempTokenVo> getCorrectTempToken() {
        return R.ok(conversationBll.getCorrectTempToken());
    }

    @PostMapping("/getCorrectStatus")
    @Operation(summary = "获取AI纠正状态", description = "只有字段：id、aiCorrectStatus、aiCorrectType、aiCorrectError")
    public R<List<ConversationVo>> getCorrectStatus(@RequestBody List<String> ids) {
        return R.ok(conversationBll.getCorrectStatus(ids));
    }

}
