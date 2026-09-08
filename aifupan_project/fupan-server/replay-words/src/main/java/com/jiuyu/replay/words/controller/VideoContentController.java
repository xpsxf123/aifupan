package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import jodd.util.StringUtil;
import com.jiuyu.replay.words.bll.AnchorVideoDetailBll;
import com.jiuyu.replay.words.bll.CueWordsBll;
import com.jiuyu.replay.words.bll.SensitiveWordsBll;
import com.jiuyu.replay.words.bo.video.VideoTextNotesBO;
import com.jiuyu.replay.words.enums.NotesType;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.repository.service.VideoTextNotesService;
import com.jiuyu.replay.words.vo.AnalysisResultAllVo;
import com.jiuyu.replay.words.vo.NotesContentVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;
import com.jiuyu.replay.words.vo.video.ToGeneratedVo;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午3:35
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("replay/words/videoContent")
@Tag(name = "自然、优化原文的控制器")
public class VideoContentController {

    @Resource
    private AnchorVideoDetailBll anchorVideoDetailBll;
    @Resource
    private SensitiveWordsBll sensitiveWordsBll;
    @Resource
    private CueWordsBll cueWordsBll;

    @Resource
    private VideoTextNotesService textNotesService;
    @Resource
    private UserFeign userFeign;
    @Autowired
    private AnchorVideoBll anchorVideoBll;


    @GetMapping("getVideoContent")
    @Schema(description = "获取视频内容")
    public R<AnchorVideoFileAllVo> getVideoContent(@Parameter(description = "视频id") String sourceId,
                                                   @Parameter(description = "类型0视频、1文件、2对比分析") Integer sourceType,
                                                   @Parameter(description = "内容类型 1自然原文，2优化原文") Integer type) {
        return anchorVideoDetailBll.getVideoContent(sourceId, type, sourceType);
    }

    @PostMapping("generateVideoContent")
    @Schema(description = "生成视频内容-服务器")
    public R<String> generateVideoContent(@RequestBody GenerateVideoContentBo bo) {
        //获取原文
        return anchorVideoDetailBll.generateVideoContentStatus(bo);
    }


    @GetMapping("exportVideoContent")
    @Schema(description = "导出视频或文档的内容文本")
    public void exportVideoContent(@Parameter(description = "视频id") String sourceId,
                                   @Parameter(description = "类型0视频、1文件、2对比分析") Integer sourceType,
                                   @Parameter(description = "内容类型 0分钟段落,1自然原文，2优化原文") Integer type,
                                   HttpServletResponse response) {
        if (sourceId == null) {
            RRException.create("视频id不能为空");
        }
        if (sourceType == null) {
            sourceType = WordsEnum.sourceType.VIDEO.getCode();
        }
        if (type == null) {
            type = WordsEnum.contentType.MINUTE.getCode();
        }
        if (sourceType == 2) {
            RRException.create("暂时不支持其他类型");
        }
        if (type == 0) {
            R<AnalysisResultAllVo> resultAllVoR = sensitiveWordsBll.getParagraphContent(sourceType, sourceId, type);
            anchorVideoDetailBll.exportContent(resultAllVoR.getData(), response, sourceType);
        } else {
            R<AnchorVideoFileAllVo> videoContent = anchorVideoDetailBll.getVideoContent(sourceId, type, sourceType);
            anchorVideoDetailBll.exportVideoContent(videoContent.getData(), response, sourceType);
        }
    }


    /**
     * 保存视频笔记
     *
     * @param notesBO 笔记内容
     *
     * @return {@link R }<{@link Long }>
     */
    @PostMapping("/notes")
    public R<Long> notes(@RequestBody @Validated VideoTextNotesBO notesBO) {
        Optional<VideoSourceType> videoSourceType = VideoSourceType.codeOf(notesBO.getSourceType());
        if (videoSourceType.isEmpty()) {
            return R.error(500, "错误的视频类型");
        }
        Optional<NotesType> notesType = NotesType.codeOf(notesBO.getNotesType());
        if (notesType.isEmpty()) {
            return R.error(500, "错误的笔记类型");
        }
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        return textNotesService.saveNotes(notesBO.getSourceId(), videoSourceType.get(), notesType.get(), notesBO.getContent(), user);
    }

    /**
     * 同步笔记小结
     * 获取最新的数据看板和数据截图
     *
     * @param sourceId   源ID
     * @param sourceType 源类型
     *
     * @return {@link R }<{@link String }>
     */
    @GetMapping("/last-review")
    public R<String> synchronousReview(@RequestParam String sourceId, @RequestParam Integer sourceType) {
        Optional<VideoSourceType> videoSourceType = VideoSourceType.codeOf(sourceType);
        if (videoSourceType.isEmpty()) {
            return R.error(500, "错误的视频类型");
        }
        String content = textNotesService.getSourceDataSummary(sourceId, videoSourceType.get()).orElse("");
        return R.ok("",content);
    }


    /**
     * 初始化租户笔记
     *
     * @return {@link R }<{@link String }>
     */
    @GetMapping("/init-tenant-notes")
    public R<String> initTenantNotes(@RequestParam String password) {
        if (!Objects.equals(password,"ChtZAYxW2DNJ38s37Q05mo6eE9P0dzru")) {
            return R.error(401, "非法访问");
        }
        textNotesService.initTenantNotes();
        return R.ok();
    }

    /**
     * 获取视频笔记
     *
     * @param sourceId   视频ID
     * @param sourceType 视频类型
     * @param notesType  笔记类型
     *
     * @return {@link R }<{@link String }>
     */
    @GetMapping("/notes")
    public R<NotesContentVo> notes(@RequestParam String sourceId, @RequestParam Integer sourceType, @RequestParam Integer notesType) {
        Optional<VideoSourceType> videoSourceType = VideoSourceType.codeOf(sourceType);
        if (videoSourceType.isEmpty()) {
            return R.error(500, "错误的视频类型");
        }
        Optional<NotesType> notesTypeOptional = NotesType.codeOf(notesType);
        if (notesTypeOptional.isEmpty()) {
            return R.error(500, "错误的笔记类型");
        }
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        NotesContentVo content = textNotesService.getNotesContent(sourceId, videoSourceType.get(), notesTypeOptional.get(), user).orElseGet(() -> {
            NotesContentVo vo = new NotesContentVo();
            vo.setLastVersion(0);
            vo.setCreateTime(LocalDateTime.now());
            vo.setUpdateTime(vo.getCreateTime());
            vo.setContent("");
            return vo;
        });
        return R.ok(content);
    }

    /**
     * 获取指定视频笔记的历史版本内容
     *
     * <p>该接口用于获取某个视频笔记的指定历史版本内容。用户需提供视频 ID、类型、笔记类型和版本号。
     * 系统会校验用户权限，并返回对应的历史笔记内容。</p>
     *
     * @param sourceId   视频的唯一标识符，表示要查询的视频资源
     * @param sourceType 视频来源类型，支持以下取值：
     *                   <ul>
     *                     <li>1: 本地录制</li>
     *                     <li>2: 上传文件</li>
     *                   </ul>
     * @param notesType  笔记类型，支持以下取值：
     *                   <ul>
     *                     <li>1: 原文笔记</li>
     *                     <li>2: 复盘小结</li>
     *                     <li>3: 分段笔记</li>
     *                   </ul>
     * @param version    请求的历史版本号，用于定位特定版本的笔记内容
     *
     * @return 返回封装在 {@link R} 中的笔记内容字符串，
     *     如果找到内容则返回对应字符串，否则返回空字符串
     */
    @GetMapping("/notes-history")
    public R<String> getHistoryContent(
        @RequestParam String sourceId,
        @RequestParam Integer sourceType,
        @RequestParam Integer notesType,
        @RequestParam Integer version) {
        Optional<VideoSourceType> videoSourceType = VideoSourceType.codeOf(sourceType);
        if (videoSourceType.isEmpty()) {
            return R.error(500, "错误的视频类型");
        }
        Optional<NotesType> notesTypeOptional = NotesType.codeOf(notesType);
        if (notesTypeOptional.isEmpty()) {
            return R.error(500, "错误的笔记类型");
        }
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        Optional<String> historyContent = textNotesService.getHistoryContent(sourceId, videoSourceType.get(), notesTypeOptional.get(), version, user);
        return R.ok(historyContent.orElse(""));
    }


    @GetMapping("contentByToGenerated")
    @Schema(description = "获取当前用户待生产的自然、优化原文")
    public R<List<ToGeneratedVo>> contentByToGenerated() {
        return R.ok(anchorVideoDetailBll.contentByToGenerated((type, uuid) -> ResultUtil.getResult(sensitiveWordsBll.getAnalysisData(type, uuid))));
    }

    @PostMapping("generateOntQAContent")
    @Schema(description = "生成视频内容的提示词-客户端")
    public R<ToGeneratedVo> generateOntQAContent(@RequestBody GenerateVideoContentBo bo) {
        return R.ok(anchorVideoDetailBll.generateOntQAContent(bo, (type, uuid) -> ResultUtil.getResult(sensitiveWordsBll.getAnalysisData(type, uuid))));
    }

    @PostMapping("completeVideoContentGenerate")
    @Schema(description = "检测自然、优化原文是否生成成功-不返回")
    public R<String> checkVideoContent(@RequestBody ToGeneratedVo generatedVo) {
        anchorVideoDetailBll.checkVideoContent(generatedVo);
        return R.ok();
    }

    @PostMapping("videoContentUpdateFailStatus")
    @Schema(description = "修改自然、优化原文的状态为失败")
    public R<String> videoContentUpdateFailStatus(@RequestBody ToGeneratedVo generatedVo) {
        generatedVo.setStatus(3);
        anchorVideoDetailBll.videoContentUpdateStatus(generatedVo);
        return R.ok();
    }


    @PostMapping("saveVideoContent")
    @Schema(description = "保存自然、优化原文")
    public R<String> saveVideoContent(@RequestBody VideoContentVo contentVo) {
        if (contentVo == null || StringUtil.isBlank(contentVo.getId())) {
            return R.error("参数不能为空");
        }
        anchorVideoDetailBll.saveVideoContent(contentVo);
        return R.ok();
    }

    @GetMapping("hasOneVideo")
    @Schema(description = "是否要自动生成自然/优化原文")
    public R<Boolean> hasOneVideo() {
        return R.ok(anchorVideoBll.hasOneVideo());
    }
}