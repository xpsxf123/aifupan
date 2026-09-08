package com.jiuyu.replay.video.project.controller;

import com.github.xiaoymin.knife4j.annotations.ApiSort;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.video.project.bo.VideoExtractBatchDeleteBo;
import com.jiuyu.replay.video.project.bo.VideoExtractBo;
import com.jiuyu.replay.video.project.bo.VideoExtractQueryBo;
import com.jiuyu.replay.video.project.bo.VideoExtractRemoteVideoBo;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.producer.VideoExtractProducer;
import com.jiuyu.replay.video.project.vo.OperationUserVo;
import com.jiuyu.replay.video.project.vo.VideoContentExtractVo;
import com.jiuyu.replay.video.project.vo.VideoExtractQueryVo;
import com.jiuyu.replay.video.project.vo.VideoExtractVo;
import com.jiuyu.replay.video.project.vo.video.ExtractPromptVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频提取文案控制器
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 短视频提取文案模块的控制器
 */
@Slf4j
@RestController
@RequestMapping("/replay/video/extract")
@RequiredArgsConstructor
@ApiSort(value = 1)
@Tag(name = "V2.5.3短视频/短视频提取文案模块", description = "短视频提取文案相关接口")
public class VideoExtractController {

    private final VideoExtractProducer videoExtractProducer;
    private final ImgOssUtils imgOssUtils;
    private final UserFeign userFeign;
    private final UserPropertyFeign userPropertyFeign;

    @Operation(
            summary = "客户端-视频文案提取（状态机接口）",
            description = """
                    **核心状态机接口**：支持URL链接和本地上传两种方式的视频文案提取，通过状态参数控制不同的处理阶段。
                    
                    ## 🔄 状态转换流程
                    
                    ### 1️⃣ PENDING (1) - 待处理
                    **首次提交**：创建提取任务
                    - 必填：videoTitle（视频标题）、videoUrl（视频URL）
                    - 创建视频基础信息和用户关联记录
                    - 状态：PENDING → 返回任务ID
                    
                    ### 2️⃣ PROCESSING (2) - 处理中
                    **上传完成**：提交视频文件信息
                    - 必填：id（任务ID）、videoHash（文件哈希）、coverUrl（封面URL）
                    - 检查是否已有相同文件的提取结果（基于hash）
                    - 如有现成结果：直接返回并标记COMPLETED
                    - 如无现成结果：标记PROCESSING，等待客户端处理
                    
                    ### 3️⃣ COMPLETED (3) - 已完成
                    **提取完成**：提交文案内容
                    - 必填：id（任务ID）、extractContent（文案内容）
                    - 保存文案到MongoDB
                    - 状态：PROCESSING → COMPLETED
                    - 扣减用户套餐资产
                    
                    ### 4️⃣ FAILED (4) - 失败
                    **处理失败**：标记失败原因
                    - 必填：id（任务ID）、extractErrorReason（失败原因）
                    - 状态：PROCESSING → FAILED
                    
                    ## 🔒 权限控制
                    - 租户隔离：只能操作自己租户下的数据
                    - 用户权限：子账号只能操作自己的记录
                    
                    ## ⚡ 性能优化
                    - 文件去重：相同hash的文件直接复用已有结果
                    - 事务保证：确保数据一致性
                    """
    )
    @PostMapping("/urlOrLocal")
    public R<VideoExtractVo> extractFromUrlAndLocal(
            @Parameter(
                    description = """
                            **视频文案提取请求参数**
                            
                            根据不同的extractStatus值，需要提供不同的参数：
                            
                            **PENDING(1) - 首次提交**：
                            - extractStatus: 1
                            - videoTitle: 视频标题（必填）
                            - videoUrl: 视频URL（必填）
                            - sourceType: 来源类型（1-URL链接，2-本地上传）
                            
                            **PROCESSING(2) - 文件上传完成**：
                            - extractStatus: 2
                            - id: 任务ID（必填）
                            - videoHash: 文件哈希值（必填）
                            - coverUrl: 封面URL（必填）
                            
                            **COMPLETED(3) - 文案提取完成**：
                            - extractStatus: 3
                            - id: 任务ID（必填）
                            - extractContent: 文案内容（必填）
                            
                            **FAILED(4) - 处理失败**：
                            - extractStatus: 4
                            - id: 任务ID（必填）
                            - extractErrorReason: 失败原因（必填）
                            """,
                    required = true,
                    schema = @Schema(implementation = VideoExtractBo.class)
            )
            @RequestBody @Validated VideoExtractBo videoExtractBo) {
        return R.ok(videoExtractProducer.extractFromUrlAndLocal(videoExtractBo));
    }

    @Operation(
            summary = "WEB端-查询历史提取记录",
            description = """
                    分页查询用户的视频文案提取历史记录，支持多条件筛选。
                    
                    ## 🔍 查询功能
                    - 按提取日期范围筛选
                    - 按视频标题模糊搜索
                    - 按操作人姓名模糊搜索
                    - 支持分页和排序
                    
                    ## 🔒 权限控制
                    - 主账号：查看租户下所有记录
                    - 子账号：只能查看自己的记录
                    
                    ## 📊 返回信息
                    - 提取状态和描述
                    - 视频基本信息
                    - 操作时间和操作人
                    - 文件存在状态
                    """
    )
    @PostMapping("/history")
    public R<PageUtils<VideoExtractQueryVo>> queryHistoryRecord(
            @Parameter(
                    description = """
                            **历史记录查询参数**
                            
                            支持的查询条件：
                            - extractDateStart/extractDateEnd: 提取日期范围
                            - videoTitle: 视频标题（模糊查询）
                            - operatorName: 操作人姓名（模糊查询）
                            - current: 当前页码（默认1）
                            - size: 每页大小（默认10）
                            """,
                    schema = @Schema(implementation = VideoExtractQueryBo.class)
            )
            @RequestBody @Validated VideoExtractQueryBo videoExtractQueryBo) {
        return R.ok(videoExtractProducer.queryHistoryRecord(videoExtractQueryBo));
    }

    @Operation(
            summary = "WEB端-批量删除视频提取记录",
            description = """
                    批量删除指定的视频文案提取记录，支持同时删除多条记录。
                    
                    ## 🗑️ 删除范围
                    - 用户视频关联表（tb_video_user_video）
                    - 视频基础信息表（tb_video_info）
                    - 采用逻辑删除，数据可恢复
                    
                    ## 🔒 权限控制
                    - 租户隔离：只能删除自己租户下的记录
                    - 主账号：可删除租户下所有记录
                    - 子账号：只能删除自己的记录
                    
                    ## ⚡ 性能特性
                    - 批量操作：支持一次删除多条记录
                    - 事务保证：确保数据一致性
                    - 权限过滤：自动过滤无权限的记录
                    
                    ## 📝 注意事项
                    - 建议单次删除不超过1000条记录
                    - 删除操作不可逆（逻辑删除）
                    - 会同时删除关联的视频基础信息
                    """
    )
    @DeleteMapping("/batch")
    public R<Boolean> batchDelete(
            @Parameter(
                    description = """
                            **批量删除请求参数**
                            
                            包含要删除的记录ID列表：
                            - ids: 记录ID数组（必填，不能为空）
                            - 支持删除1-1000条记录
                            - 自动过滤无权限的记录
                            - 不存在的记录会被忽略
                            
                            示例：
                            ```json
                            {
                              "ids": [1, 2, 3, 4, 5]
                            }
                            ```
                            """,
                    required = true,
                    schema = @Schema(implementation = VideoExtractBatchDeleteBo.class)
            )
            @RequestBody @Validated VideoExtractBatchDeleteBo batchDeleteBo) {
        return R.ok(videoExtractProducer.batchDelete(batchDeleteBo));
    }

    @Operation(
            summary = "WEB端-查询视频文案内容",
            description = """
                    通过用户视频关联表ID查询对应的文案内容。
                    
                    ## 🔍 查询流程
                    1. 根据关联表ID查询用户视频关联记录
                    2. 通过视频ID获取视频基础信息
                    3. 使用video_hash从MongoDB查询文案内容
                    4. 返回完整的文案数据
                    
                    ## 🔒 权限控制
                    - 租户隔离：只能查询自己租户下的记录
                    - 用户权限：子账号只能查询自己的记录
                    
                    ## 📄 返回内容
                    - 完整的音频转文字内容
                    - 字幕内容（如果有）
                    - OCR识别内容（如果有）
                    - 提取时间和方法信息
                    """
    )
    @GetMapping("/content/{id}")
    public R<VideoContentExtractVo> getContentById(
            @Parameter(
                    description = "用户视频关联表ID",
                    required = true,
                    example = "1001"
            )
            @PathVariable Long id) {
        return R.ok(videoExtractProducer.getContentById(id));
    }

    @Operation(
            summary = "客户端-查询待处理队列数据",
            description = """
                    查询当前用户所有待处理和处理中状态的视频记录，用于客户端队列处理。
                    
                    ## 🔍 查询范围
                    - 状态：PENDING(1) 和 PROCESSING(2)
                    - 范围：当前用户的所有记录
                    - 排序：按创建时间升序（先创建的先处理）
                    
                    ## 🎯 使用场景
                    - 客户端启动时加载待处理队列
                    - 定期同步服务端待处理任务
                    - 断线重连后恢复处理队列
                    
                    ## 📄 返回数据
                    - 用户视频关联表的基础信息
                    - 视频标题、状态、创建时间等
                    - 不包含文案内容（减少数据传输）
                    """
    )
    @GetMapping("/pendingQueue")
    public R<List<VideoExtractVo>> getPendingQueue() {
        return R.ok(videoExtractProducer.getPendingQueue());
    }

    @Operation(
            summary = "WEB端-查询操作用户列表",
            description = """
                    查询当前租户下所有进行过视频文案提取操作的用户列表，用于筛选和统计。
                    
                    ## 🔍 查询范围
                    - 范围：当前租户下的所有用户
                    - 条件：有过视频文案提取操作记录的用户
                    - 排序：按用户类型排序
                    
                    ## 🎯 使用场景
                    - 按操作人筛选历史记录
                    
                    ## 📄 返回数据
                    - 用户基本信息（ID、姓名、账号类型）
                    """
    )
    @GetMapping("/operationUsers")
    public R<List<OperationUserVo>> getOperationUsers() {
        return R.ok(videoExtractProducer.getOperationUsers());
    }

    @Operation(summary = "客户端-获取封面图片上传的预签名链接")
    @GetMapping("/getCoverImgPutUrl")
    public R<SignUploadUrlVo> getCoverImgPutUrl(String suffix) {
        return R.ok(imgOssUtils.getDateSignUploadUrl("img", suffix));
    }

    @Operation(
            summary = "客户端-批量创建提取文案任务",
            description = """
                    **批量创建提取文案任务**：专门用于第一批上传，批量创建用户视频关联记录。
                    
                    ## 🎯 功能说明
                    - **第一步**：批量创建提取任务（使用此接口）
                    - **后续步骤**：状态转换使用 extractFromUrlAndLocal 接口
                    - **智能处理**：如果视频已有文案（extractStatus=1），直接标记为COMPLETED状态
                    
                    ## 📝 请求参数
                    - **videoInfoVos**: 视频信息列表（必填）
                    - **sourceType**: 来源类型（必填，3-达人视频列表，4-爆款视频）
                    - **sourceId**: 来源ID（必填）
                    
                    ## 🔄 处理逻辑
                    1. 批量查询视频基础信息
                    2. 检查是否已存在提取记录（去重）
                    3. 智能判断初始状态：
                       - 如果视频已有文案 → 创建为COMPLETED状态
                       - 如果视频无文案 → 创建为PENDING状态
                    4. 返回所有相关的视频关联记录
                    
                    ## 📤 返回结果
                    返回视频关联记录集合，包含新建和已存在的记录，客户端可根据extractStatus判断哪些需要后续处理。
                    """
    )
    @PostMapping("/batchCreateExtract")
    public R<List<VideoExtractVo>> batchCreateExtract(
            @RequestBody @Validated VideoExtractRemoteVideoBo videoExtractRemoteVideoBo) {
        return R.ok(videoExtractProducer.batchCreateExtract(videoExtractRemoteVideoBo));
    }

    @Operation(summary = "客户端-重新提取文案", description = "提取失败！重新提取文案")
    @GetMapping("/reuse")
    public R<Boolean> reuseExtract(@RequestParam @NotNull(message = "用户视频ID不能为空") Long userVideoId) {
        return R.ok(videoExtractProducer.reuseExtract(userVideoId));
    }

    @Operation(summary = "客户端-获取视频详情")
    @GetMapping("/getVideoInfoByVideoId")
    public R<VideoInfoEntity> getVideoInfoByVideoId(@RequestParam @NotNull(message = "视频ID不能为空") Long videoId) {
        return R.ok(videoExtractProducer.getVideoInfoByVideoId(videoId));
    }

    @Operation(summary = "客户端-重新统计用户的短视频资产")
    @GetMapping("/syncUserShortVideoProperty")
    public R<Boolean> syncUserShortVideoProperty() {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        userPropertyFeign.syncSubAccountCount(user.getId());

        return R.ok(videoExtractProducer.syncUserShortVideoProperty(user.getId(), user.getActiveTenantId()));
    }

    @Operation(summary = "客户端-获取提取文案后优化文案的提示词")
    @GetMapping("/getExtractPrompt")
    public R<ExtractPromptVo> getExtractPrompt() {
        return R.ok(videoExtractProducer.getExtractPrompt());
    }

}
