package com.jiuyu.replay.api.controller.openapi;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.ai.impl.ViewingConfuseSentenceImpl;
import com.jiuyu.replay.api.logic.order.AiTokenUseRecordLogic;
import com.jiuyu.replay.api.logic.system.DictDataLogic;
import com.jiuyu.replay.api.logic.third.AiRelatedLogic;
import com.jiuyu.replay.api.logic.third.impl.AiRelatedLogicImpl;
import com.jiuyu.replay.api.logic.words.HistoryParagraphLogic;
import com.jiuyu.replay.api.logic.words.VideoDataViewingConfuseLogic;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.CustomizeSseEmitter;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.bo.ChatStreamProxyBo;
import com.jiuyu.replay.words.bo.HistoryParagraphBo;
import com.jiuyu.replay.words.bo.HistoryParagraphListBo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.vo.HistoryParagraphListVo;
import com.jiuyu.replay.words.vo.PromptAssemblyResultVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author ：lujie
 * @description：客户端ai相关的接口
 * @date ：2025/3/22 下午4:08
 */
@RestController
@CrossOrigin
@RequestMapping("replay/aiRelated")
@Tag(name = "客户端ai相关的接口")
@Slf4j
public class AiRelatedController {

    @Resource
    private HistoryParagraphLogic historyParagraphLogic;
    @Resource
    private AiRelatedLogic aiRelatedLogic;
    @Resource
    private AiTokenUseRecordLogic aiTokenUseRecordLogic;
    @Resource
    private VideoDataViewingConfuseLogic videoDataViewingConfuseLogic;
    @Resource
    private DictDataLogic dictDataLogic;

    @Operation(summary = "新增历史段落")
    @PostMapping("/addHistoryParagraph")
    @UserLock
    public R<HistoryParagraphInfoVo> addHistoryParagraph(@RequestBody HistoryParagraphBo paragraphBo){
        UserCacheVo localUser = GlobalObject.getLocalUser();
        paragraphBo.setUserId(localUser.getId());
        paragraphBo.setTenantId(localUser.getActiveTenantId());
        paragraphBo.setCode(UUID.randomUUID().toString().replace("-", ""));
        return historyParagraphLogic.save(paragraphBo);
    }

    @Operation(summary = "历史会话列表")
    @PostMapping("/historyParagraphList")
    @UserLock
    public R<List<HistoryParagraphListVo>> historyParagraphList(@RequestBody HistoryParagraphListBo paragraphBo){
        return historyParagraphLogic.historyParagraphList(paragraphBo);
    }

    @Operation(summary = "删除历史段落")
    @PostMapping("/deleteHistoryParagraph")
    @UserLock
    public R<String> deleteHistoryParagraph(@RequestBody HistoryParagraphBo paragraphBo){
        return historyParagraphLogic.deleteHistoryParagraph(paragraphBo);
    }

    @Operation(summary = "根据code查询历史段落内容")
    @PostMapping("/getHistoryContentByCode")
    @UserLock
    public R<HistoryParagraphInfoVo> getHistoryContentByCode(@RequestBody HistoryParagraphBo paragraphBo){
        return historyParagraphLogic.getByCode(paragraphBo.getType(), paragraphBo.getSourceId(), paragraphBo.getSourceType(), paragraphBo.getCode());
    }

    @Operation(summary = "问答接口")
    @PostMapping("/ask")
    public SseEmitter ask(@RequestBody AskRequestBo askRequestBo, @RequestHeader(value = "webVersion", required = false) String webVersion) {
        CustomizeSseEmitter emitter = new CustomizeSseEmitter(1000L * 60 * 20);
        try{
            aiRelatedLogic.ask(askRequestBo, emitter, webVersion);
        } catch (RRException e) {
            log.info("调用ai问答错误：RRException error: {}, 堆栈={}", e.getMsg(), CommonUtils.getExceptionStack(e));
            emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", e.getCode(), "msg", e.getMsg())), "error");
            emitter.sendStop2();
        } catch (Exception e) {
            log.info("调用ai问答错误：Exception error: {}，堆栈={}", e.getMessage(), CommonUtils.getExceptionStack(e));
            emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", 7005, "msg", AiRelatedLogicImpl.aiError)), "error");
            emitter.sendStop2();
        }
        return emitter;
    }

    @Operation(summary = "构建额外的上一次对话的输出字符串")
    @PostMapping("/extraBuildLastOutString")
    public R<String> extraBuildLastOutString(@RequestBody AskRequestBo askRequestBo) {
        return R.ok("获取成功", aiRelatedLogic.extraBuildLastOutString(askRequestBo));
    }

    @Operation(summary = "获取上下文缓存id")
    @PostMapping("/getContextId")
    @UserLock
    public R<String> getContextId(@RequestBody AskRequestBo askRequestBo){
        UserCacheVo localUser = GlobalObject.getLocalUser();
        askRequestBo.setUserId(localUser.getId());
        askRequestBo.setTenantId(localUser.getActiveTenantId());
        return aiRelatedLogic.getContextId(askRequestBo);
    }

    @Operation(summary = "更新上下文缓存id")
    @PostMapping("/updateContextId")
    public R<String> updateContextId(@RequestBody AskRequestBo askRequestBo){
        UserCacheVo localUser = GlobalObject.getLocalUser();
        askRequestBo.setUserId(localUser.getId());
        askRequestBo.setTenantId(localUser.getActiveTenantId());
        return aiRelatedLogic.updateContextId(askRequestBo);
    }

    @Operation(summary = "添加AiToken记录")
    @PostMapping("/saveAiTokenUseRecord")
    public R<AiTokenUseRecordInfoVo> saveAiTokenUseRecord(@RequestBody AiTokenUseRecordBo aiTokenUseRecordBo){
        return aiTokenUseRecordLogic.save(aiTokenUseRecordBo);
    }

    @PostMapping("/danMuCacheList")
    @Operation(summary = "查询视频的弹幕")
    public R<List<DanMuVo>> danMuCacheList(@RequestBody QueryDanMuBo queryDanMuBo){
        return aiRelatedLogic.danMuCacheList(queryDanMuBo);
    }

    @GetMapping("/videoDataViewingConfuseByVideoId")
    @Operation(summary = "查询视频的看板数据")
    public R<VideoDataViewingConfuseInfoVo> videoDataViewingConfuseByVideoId(String videoId){
        return videoDataViewingConfuseLogic.getByVideoId(videoId);
    }

    @GetMapping("/getVideoDataViewingStr")
    @Operation(summary = "查询视频的看板数据-字符串")
    public R<String> getVideoDataViewingStr(String videoId){
        String content = null;
        try{
            ViewingConfuseSentenceImpl impl = ApplicationContextUtil.getBean(ViewingConfuseSentenceImpl.class);
            impl.init(videoId, 3, null, null);
            content = impl.getBoard();
        }catch (Exception e){
            log.error("获取视频看板数据错误：{}", e.getMessage());
        }
        return R.ok("获取成功", content);
    }


    @Operation(summary = "问答中全局额外要求")
    @GetMapping("askRequireAdditional")
    public R<List<String>> askRequireAdditional(){
        List<String> result = new ArrayList<>();

        DictDataListBo dictDataListBo = new DictDataListBo();
        dictDataListBo.setLimit(-1);

        // 获取额外要求设置
        dictDataListBo.setTypeLogo("ask_require_additional");
        R<PageUtils<DictDataListVo>> pageUtilsR = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())  && !pageUtilsR.getData().getList().isEmpty()){
            result = pageUtilsR.getData().getList().stream().map(DictDataListVo::getValue).toList();
        }

        return R.ok(result);
    }

    @Operation(summary = "服务端AI流式对话代理（供C#客户端调用，服务端持有API Key转发到各厂商API）")
    @PostMapping("/chatStreamProxy")
    public SseEmitter chatStreamProxy(@RequestBody ChatStreamProxyBo bo) {
        CustomizeSseEmitter emitter = new CustomizeSseEmitter(1000L * 60 * 20);
        try {
            aiRelatedLogic.chatStreamProxy(bo, emitter);
        } catch (RRException e) {
            log.info("AI流式代理调用错误：RRException error: {}, 堆栈={}", e.getMsg(), CommonUtils.getExceptionStack(e));
            emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", e.getCode(), "msg", e.getMsg())), "error");
            emitter.sendStop2();
        } catch (Exception e) {
            log.info("AI流式代理调用错误：Exception error: {}，堆栈={}", e.getMessage(), CommonUtils.getExceptionStack(e));
            emitter.sendMessage(JSONUtil.toJsonStr(Map.of("code", 7005, "msg", AiRelatedLogicImpl.aiError)), "error");
            emitter.sendStop2();
        }
        return emitter;
    }

    @Operation(summary = "组装AI提示词（供C#桌面客户端调用，返回完整prompt+模型配置+contextId）")
    @PostMapping("/assemblePrompt")
    @UserLock
    public R<PromptAssemblyResultVo> assemblePrompt(@RequestBody AskRequestBo askRequestBo, @RequestHeader(value = "webVersion", required = false) String webVersion) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        askRequestBo.setUserId(localUser.getId());
        askRequestBo.setTenantId(localUser.getActiveTenantId());
        if (ObjectUtil.isEmpty(askRequestBo.getNikeName())) askRequestBo.setNikeName(localUser.getNickName());
        return R.ok(aiRelatedLogic.assemblePromptV2(askRequestBo, webVersion));
    }
}
