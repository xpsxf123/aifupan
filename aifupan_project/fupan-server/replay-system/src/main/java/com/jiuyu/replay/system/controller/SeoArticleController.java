package com.jiuyu.replay.system.controller;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoArticleBo;
import com.jiuyu.replay.system.bo.SeoArticleListBo;
import com.jiuyu.replay.system.bo.SeoIdsBo;
import com.jiuyu.replay.system.bo.SeoImportFileBo;
import com.jiuyu.replay.system.bo.SeoArticleStatusBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.repository.service.SeoArticleService;
import com.jiuyu.replay.system.repository.service.SeoImportTaskService;
import com.jiuyu.replay.system.vo.SeoArticleInfoVo;
import com.jiuyu.replay.system.vo.SeoArticleListVo;
import com.jiuyu.replay.system.vo.SeoImportProgressVo;
import com.jiuyu.replay.system.vo.SeoImportTaskVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SEO 文章管理。
 *
 * @author claude
 * @date 2026-08-13
 */
@RestController
@CrossOrigin
@RequestMapping("replay/seoArticle")
@Tag(name = "SEO 内容管理-文章")
public class SeoArticleController {

    private final SeoArticleService seoArticleService;

    private final SeoImportTaskService seoImportTaskService;

    public SeoArticleController(SeoArticleService seoArticleService,
                                SeoImportTaskService seoImportTaskService) {
        this.seoArticleService = seoArticleService;
        this.seoImportTaskService = seoImportTaskService;
    }

    /**
     * 文章列表。出参含分类名与标签（带状态），不含正文。
     *
     * @param bo 查询参数
     * @return 分页结果
     */
    @PostMapping("/list")
    @Operation(summary = "文章列表")
    public R<PageUtils<SeoArticleListVo>> list(
            @Parameter(description = "查询参数", required = true) @RequestBody SeoArticleListBo bo) {
        return R.ok("获取成功", seoArticleService.queryPage(bo));
    }

    /**
     * 文章详情，含正文。
     *
     * @param id 文章 id
     * @return 详情
     */
    @GetMapping("/info")
    @Operation(summary = "文章详情")
    public R<SeoArticleInfoVo> info(@Parameter(description = "文章id", required = true) @RequestParam("id") Long id) {
        return R.ok("获取成功", seoArticleService.info(id));
    }

    /**
     * 新增文章。响应回传实际入库的 slug——它可能因冲突被追加随机数，运营需知情。
     *
     * @param bo 文章对象
     * @return 含实际 slug 与 slugAppended 的结果
     */
    @PostMapping("/save")
    @Operation(summary = "新增文章")
    public R<SeoSaveResultVo> save(@Parameter(description = "文章对象", required = true) @RequestBody SeoArticleBo bo) {
        return R.ok("添加成功", seoArticleService.saveArticle(bo));
    }

    /**
     * 修改文章。
     *
     * @param bo 文章对象
     * @return 含实际 slug 与 slugAppended 的结果
     */
    @PostMapping("/update")
    @Operation(summary = "修改文章")
    public R<SeoSaveResultVo> update(
            @Parameter(description = "文章对象", required = true) @RequestBody SeoArticleBo bo) {
        return R.ok("修改成功", seoArticleService.updateArticle(bo));
    }

    /**
     * 删除文章。
     *
     * @param id 文章 id
     * @return 删除结果
     */
    @GetMapping("/delete")
    @Operation(summary = "删除文章")
    public R<String> delete(@Parameter(description = "文章id", required = true) @RequestParam("id") Long id) {
        seoArticleService.deleteArticles(List.of(id));
        return R.ok("删除成功");
    }

    /**
     * 批量删除文章。
     *
     * @param bo 主键集合
     * @return 删除结果
     */
    @PostMapping("/deleteByIds")
    @Operation(summary = "批量删除文章")
    public R<String> deleteByIds(@Parameter(description = "主键集合", required = true) @RequestBody SeoIdsBo bo) {
        seoArticleService.deleteArticles(bo.getIds());
        return R.ok("删除成功");
    }

    /**
     * 发布 / 下架文章。首次发布时写入发布时间，重新发布不覆盖。
     *
     * @param bo 状态变更参数
     * @return 变更结果
     */
    @PostMapping("/changeStatus")
    @Operation(summary = "发布/下架文章")
    public R<String> changeStatus(
            @Parameter(description = "状态变更参数", required = true) @RequestBody SeoArticleStatusBo bo) {
        seoArticleService.changeStatus(bo.getId(), bo.getArticleStatus());
        return R.ok("操作成功");
    }

    /**
     * 批量导入 Markdown 文章。只接收 .md，立即返回 taskId，导入在后台进行。
     *
     * <p>文件内容必须在本方法（HTTP 线程）内读完：MultipartFile 背后是请求的临时文件，
     * 请求一结束容器就清理，异步线程再读会拿到空流。
     *
     * @param files              多个 .md 文件
     * @param autoCreateCategory 分类不存在时是否自动创建，1 是
     * @return 任务 ID
     */
    @PostMapping("/import")
    @Operation(summary = "批量导入文章")
    public R<SeoImportTaskVo> importArticles(
            @Parameter(description = "md文件", required = true) @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "分类不存在时自动创建") @RequestParam(value = "autoCreateCategory", required = false) Integer autoCreateCategory) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要导入的 .md 文件");
        }
        if (files.length > SeoConstant.MAX_IMPORT_FILES) {
            throw new BusinessException("单次最多导入 " + SeoConstant.MAX_IMPORT_FILES
                    + " 篇，当前 " + files.length + " 篇");
        }
        // 总字节先于逐文件校验：单文件 2MB × 50 = 100MB 会常驻堆直到导入结束，
        // 而 multipart 全局配的是 1024MB，请求侧没有任何闸门。按总量卡才拦得住
        long totalBytes = 0L;
        for (MultipartFile file : files) {
            totalBytes += file.getSize();
        }
        if (totalBytes > SeoConstant.MAX_IMPORT_TOTAL_SIZE) {
            throw new BusinessException("单次导入总大小不能超过 "
                    + (SeoConstant.MAX_IMPORT_TOTAL_SIZE / 1024 / 1024) + "MB，请分批导入");
        }

        List<SeoImportFileBo> payload = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            // 单个文件的问题一律降级为「这一篇失败」，不否决整批——
            // 一个拼错扩展名的文件不该让另外 49 篇正常文件一起作废（openspec §5.4）
            if (fileName == null || fileName.isBlank()) {
                payload.add(SeoImportFileBo.rejected("(未命名文件)", "文件名为空，无法识别"));
                continue;
            }
            if (!fileName.toLowerCase().endsWith(".md")) {
                payload.add(SeoImportFileBo.rejected(fileName, "仅支持 .md 文件，不支持压缩包等其他格式"));
                continue;
            }
            if (file.getSize() > SeoConstant.MAX_IMPORT_FILE_SIZE) {
                payload.add(SeoImportFileBo.rejected(fileName, "文件超过 "
                        + (SeoConstant.MAX_IMPORT_FILE_SIZE / 1024 / 1024) + "MB 上限"));
                continue;
            }
            try {
                payload.add(SeoImportFileBo.of(fileName, decodeUtf8(file.getBytes())));
            } catch (CharacterCodingException e) {
                // 必须排在 IOException 之前——它是 IOException 的子类，顺序反了就永远走不到。
                // GBK 另存的 md 解码后通常仍能解析出 front matter，会「成功导入一篇乱码文章」，
                // 比直接报错更难发现。这里严格解码，让编码问题当场暴露
                payload.add(SeoImportFileBo.rejected(fileName, "文件不是 UTF-8 编码，请另存为 UTF-8 后重试"));
            } catch (IOException e) {
                payload.add(SeoImportFileBo.rejected(fileName, "文件读取失败，请重新上传"));
            }
        }
        return R.ok("已提交导入", seoImportTaskService.submit(
                payload, Objects.equals(autoCreateCategory, 1)));
    }

    /**
     * 查询导入进度与逐篇结果，供前端轮询。
     *
     * @param taskId 任务 ID
     * @return 进度
     */
    @GetMapping("/importProgress")
    @Operation(summary = "导入进度")
    public R<SeoImportProgressVo> importProgress(
            @Parameter(description = "任务id", required = true) @RequestParam("taskId") String taskId) {
        return R.ok("获取成功", seoImportTaskService.progress(taskId));
    }

    /**
     * 严格 UTF-8 解码。
     *
     * <p>不用 {@code new String(bytes, UTF_8)}——它遇到非法字节会静默替换成 U+FFFD，
     * 于是 GBK 编码的文件会「解码成功」，front matter 里的 ASCII 键名照样能解析，
     * 结果是一篇标题正文全是乱码的文章被成功导入。CodingErrorAction.REPORT 让它当场失败。
     */
    private static String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }
}
