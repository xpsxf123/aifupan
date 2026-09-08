package com.jiuyu.replay.words.controller;

import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson2.JSON;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.utils.excel.ExcelUtils;
import com.jiuyu.replay.common.vo.IdVo;
import com.jiuyu.replay.generic.bo.douyin.DouyinAnchorBo;
import com.jiuyu.replay.generic.bo.douyin.DouyinOpenResult;
import com.jiuyu.replay.generic.dto.third.ThirdSalesRankingResult;
import com.jiuyu.replay.generic.feign.douyin.DouyinOpenFeign;
import com.jiuyu.replay.generic.feign.third.ChanmamaFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.TradeRankAdminQueryBo;
import com.jiuyu.replay.words.bo.anchor.BatchCorrectTradeBo;
import com.jiuyu.replay.words.bo.anchor.ExcelUploadSimilarAnchor;
import com.jiuyu.replay.words.bo.anchor.ExcelUploadSimilarAnchorRequest;
import com.jiuyu.replay.words.bo.anchor.SwitchThirdCollect;
import com.jiuyu.replay.words.dto.SimilarAnchorCallbackDto;
import com.jiuyu.replay.words.dto.SimilarAnchorCallbackWrapperDto;
import com.jiuyu.replay.words.dto.TradeThirdRankingDto;
import com.jiuyu.replay.words.dto.UpdateSimilarAnchorDto;
import com.jiuyu.replay.words.producer.impl.TradeRankAnchorProducer;
import com.jiuyu.replay.words.vo.LeafTradeVo;
import com.jiuyu.replay.words.vo.TradeAnchorCountVo;
import com.jiuyu.replay.words.vo.TradeRankAdminVo;
import com.jiuyu.replay.words.vo.TradeRankVo;
import com.jiuyu.replay.words.vo.anchor.RankAnchorCheckResult;
import com.jiuyu.replay.words.vo.anchor.TradeRankInfo;
import com.jiuyu.replay.words.vo.anchor.TradeThirdRankingVO;
import com.jiuyu.replay.words.vo.anchor.TreeTradeThirdRank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RayChou
 * @date 2025/10/23 10:17
 */
@Slf4j
@RestController
@RequestMapping("/replay/words/tradeRank")
@RequiredArgsConstructor
@ApiSort(value = 1)
@Tag(name = "主播行业热榜", description = "行业热榜相关接口")
public class TradeRankController {

    private final TradeRankAnchorProducer tradeRankAnchorProducer;

    private final ChanmamaFeign chanmamaFeign;

    private final DouyinOpenFeign douyinOpenFeign;

    private final CommonProperties commonProperties;



    /**
     * 获取行业信息接口
     *
     * @param tradeId 行业ID
     *
     * @return 行业信息
     *
     */
    @GetMapping("/info")
    public R<TradeRankInfo> getInfo(@RequestParam Long tradeId) {
        return R.ok(tradeRankAnchorProducer.getInfo(tradeId));
    }

    /**
     * 获取行业热榜数据接口
     *
     * @param page    页码
     * @param limit   每页条数
     * @param tradeId 行业ID
     *
     * @return 行业热榜分页数据
     *
     * @author RayChou
     * @date 2025-10-28
     */
    @Operation(
        summary = "WEB端-获取行业热榜数据",
        description = "分页查询当前行业获取行业热榜数据，按账号热度倒序排列"
    )
    @GetMapping("/page")
    public R<PageUtils<TradeRankVo>> pageTradeRank(
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") Integer page,
        @RequestParam(defaultValue = "10") @Max(value = 300, message = "每页最大条数不能超过300条") Integer limit,
        @Parameter(description = "行业ID", required = true, example = "1001") @RequestParam Long tradeId,
        @RequestParam(required = false) String anchorName, @RequestParam(required = false) String liveKeyword) {
        PageUtils<TradeRankVo> pageUtils = tradeRankAnchorProducer.pageTradeRank(page, limit, tradeId, anchorName, liveKeyword);
        return R.ok(pageUtils);
    }

    /**
     * 后台管理系统-分页查询行业热榜数据（返回所有字段）
     *
     * @param queryBo 查询条件BO
     *
     * @return 行业热榜分页数据（包含tb_similar_anchor所有字段和tb_similar_collect主键ID）
     *
     * @author RayChou
     * @date 2025-10-28
     */
    @Operation(
        summary = "后台管理-分页查询行业热榜数据",
        description = "分页查询行业热榜数据，返回tb_similar_anchor表所有字段和tb_similar_collect主键ID。" +
            "行业ID可选：不传则查询全部行业数据，传入则查询指定行业的数据。" +
            "支持主播抖音账号精确筛选、主播名称模糊搜索、热度范围筛选、更新时间范围筛选。" +
            "按tb_similar_anchor表的update_date倒序排列。"
    )
    @PostMapping("/admin/page")
    public R<PageUtils<TradeRankAdminVo>> pageTradeRankAdmin(@RequestBody TradeRankAdminQueryBo queryBo) {
        PageUtils<TradeRankAdminVo> pageUtils = tradeRankAnchorProducer.pageTradeRankAdmin(queryBo);
        return R.ok(pageUtils);
    }

    /**
     * 后台管理-修正相似达人行业
     *
     * @param similarCollectId 相似主播收藏ID（tb_similar_collect主键）
     * @param tradeId          新的行业ID
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-05
     */
    @Operation(
        summary = "后台管理-修正相似达人行业",
        description = "修正相似达人的行业归属。直接修改收藏关系的行业ID。如果目标行业已存在该相似达人，则提示错误。"
    )
    @PostMapping("/admin/correctTrade")
    public R<Void> correctTrade(
        @Parameter(description = "相似主播榜单ID", required = true, example = "789012")
        @RequestParam Long similarCollectId,
        @Parameter(description = "新的行业ID", required = true, example = "1001")
        @RequestParam Long tradeId) {
        return tradeRankAnchorProducer.batchCorrectTrade(List.of(similarCollectId), tradeId);
    }

    /**
     * 后台管理-批量修正相似达人行业
     *
     * @param batchCorrectTradeBo 批量修正相似达人行业BO
     *
     * @return 操作结果
     *
     */
    @PostMapping("/admin/batch-correct-trade")
    public R<Void> batchCorrectTrade(@RequestBody @Validated BatchCorrectTradeBo batchCorrectTradeBo) {
        return tradeRankAnchorProducer.batchCorrectTrade(batchCorrectTradeBo.getSimilarCollectIds(), batchCorrectTradeBo.getTradeId());
    }

    /**
     * 后台管理-切换行业第三方采集状态
     *
     */
    @PostMapping("/admin/switch-third-collect")
    public R<Void> switchThirdCollect(@RequestBody @Validated SwitchThirdCollect thirdCollect) {
        return tradeRankAnchorProducer.switchThirdCollect(thirdCollect.getTradeId(), thirdCollect.getCollect());
    }

    /**
     * 后台管理-删除行业相似达人
     *
     * @param similarCollectId 相似主播收藏ID（tb_similar_collect主键）
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-05
     */
    @Operation(
        summary = "后台管理-删除行业相似达人",
        description = "删除tb_similar_collect关系表中的记录。"
    )
    @PostMapping("/admin/delete")
    public R<Boolean> deleteSimilarCollect(
        @Parameter(description = "相似主播榜单ID", required = true, example = "789012")
        @RequestParam Long similarCollectId) {
        return R.ok(tradeRankAnchorProducer.deleteSimilarCollect(similarCollectId));
    }

    /**
     * 后台管理-刷新历史相似达人行业关联数据
     *
     * @param cutoffTime 截止时间（格式：yyyy-MM-dd HH:mm:ss）
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-06
     */
    @Deprecated
    @Operation(
        summary = "后台管理-刷新历史相似达人行业关联数据",
        description = "将指定时间之前的相似达人数据与行业建立关联关系"
    )
    @PostMapping("/admin/refreshHistoryData")
    public R<String> refreshHistoryData(
        @Parameter(description = "截止时间（格式：yyyy-MM-dd HH:mm:ss）", required = true, example = "2025-11-05 09:15:50")
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime cutoffTime) {
        //return R.error("接口已废弃");
        return tradeRankAnchorProducer.refreshHistoryData(cutoffTime);
    }

    /**
     * 相似达人回调接口
     *
     * @param wrapperDto 回调包装数据（包含code、msg、data）
     *
     * @return 处理结果（成功：code=0, data=SUCCESS, msg=成功；失败：code=500, data=FAIL, msg=具体失败内容）
     *
     * @author RayChou
     * @date 2025-10-27
     */
    @PostMapping("/similarAnchorCallback")
    @Operation(summary = "相似达人回调接口", description = "接收第三方平台返回的相似达人数据")
    public R<String> similarAnchorCallback(@RequestBody SimilarAnchorCallbackWrapperDto wrapperDto) {
        log.info("========== [回调接口] 收到相似达人回调请求 ==========");
        log.info("[回调请求] requestId: {}, code: {}, msg: {} data: {}", wrapperDto.getData() != null ? wrapperDto.getData().getRequestId() : null, wrapperDto.getCode(), wrapperDto.getMsg(), JSON.toJSONString(wrapperDto.getData()));

        // 检查响应码
        if (wrapperDto.getCode() == null || wrapperDto.getCode() != 0) {
            String errorMsg = "第三方平台返回错误: " + wrapperDto.getMsg();
            log.warn("[回调请求] ❌ {}", errorMsg);
            return R.error(500, errorMsg, "FAIL");
        }

        // 检查data是否为空
        SimilarAnchorCallbackDto callbackDto = wrapperDto.getData();
        if (callbackDto == null) {
            String errorMsg = "回调数据为空";
            log.warn("[回调请求] ❌ {}", errorMsg);
            return R.error(500, errorMsg, "FAIL");
        }

        log.info("[回调请求] ✓ 回调数据验证通过，currentAnchorId: {}", callbackDto.getCurrentAnchorId());
        return tradeRankAnchorProducer.handleSimilarAnchorCallback(callbackDto);
    }


    /**
     * 第三方榜单回调接口
     *
     * @return 处理结果
     *
     */
    @PostMapping("/third-rank-callback")
    public R<Void> thirdRankCallback(@RequestBody ThirdSalesRankingResult callbackResult) {
        log.info("========== [回调接口] 收到第三方榜单回调请求 ==========");
        log.info("[第三方榜单] 回调请求 requestId: {}, categoryId: {}", callbackResult.getRequestId(), callbackResult.getCategoryId());
        if (EmptyUtil.isEmpty(callbackResult.getCategoryId())) {
            return R.error("参数错误缺少行业ID");
        }
        if  (EmptyUtil.isEmpty(callbackResult.getSalesRankingVos())) {
            log.info("[第三方榜单] 回调请求数据为空");
            return R.ok();
        }
        return tradeRankAnchorProducer.processThirdRankAnchor(null, callbackResult.getCategoryId(), callbackResult.getSalesRankingVos());
    }


    /**
     * 统计各行业30天内更新的相似达人数量
     *
     * @param tradeIds 行业ID列表（1到多个）
     *
     * @return 各行业的相似达人数量统计
     *
     * @author RayChou
     * @date 2025-10-30
     */
    @Operation(
        summary = "WEB端-统计各行业30天内更新的相似达人数量",
        description = "根据传入的行业ID列表，统计每个行业在30天内更新的相似达人数量。不存在数据的行业返回数量为0"
    )
    @GetMapping("/countSimilarAnchors")
    public R<List<TradeAnchorCountVo>> countSimilarAnchors(
        @Parameter(description = "行业ID列表（多个用逗号分隔）", required = true, example = "1001,1002,1003")
        @RequestParam List<Long> tradeIds) {
        List<Long> openRankTradeIds = tradeRankAnchorProducer.getOpenRankTradeIds(tradeIds);

//        // 获取所有有效的叶子节点行业ID
//        List<Long> leafTradeIds = tradeRankAnchorProducer.getLeafTradeIds();
//        Set<Long> leafTradeIdSet = new HashSet<>(leafTradeIds);
//
//        // 计算入参 tradeIds 与有效行业ID的交集（只查询有效的行业）
//        List<Long> validTradeIds = tradeIds.stream()
//            .filter(leafTradeIdSet::contains)
//            .toList();
//
//        // 如果交集数量为 0，直接返回所有行业的数量都为 0
//        if (validTradeIds.isEmpty()) {
//            List<TradeAnchorCountVo> result = tradeIds.stream()
//                .map(tradeId -> new TradeAnchorCountVo(tradeId, 0L))
//                .collect(Collectors.toList());
//            return R.ok(result);
//        }
        // 只查询有效行业的相似达人数量（时间范围：最近30天，SQL中使用 DATE_SUB(NOW(), INTERVAL 30 DAY)）
        // checkRankEnabled = true：只统计 rank_enabled = 1 的行业（WEB端用户展示）
        Map<Long, Long> countMap = tradeRankAnchorProducer.countTradeAnchor(true, LocalDateTime.now().minusDays(30), null, null, openRankTradeIds);


        // 返回所有入参行业的统计结果（有效行业返回实际数量，无效行业返回0）
        List<TradeAnchorCountVo> result = tradeIds.stream()
            .map(tradeId -> new TradeAnchorCountVo(
                tradeId,
                countMap.getOrDefault(tradeId, 0L)
            ))
            .collect(Collectors.toList());

        return R.ok(result);
    }

    /**
     * 后台管理-批量删除行业相似达人
     *
     * @param similarCollectIds 相似主播收藏ID列表（tb_similar_collect主键列表）
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-12
     */
    @Operation(
        summary = "后台管理-批量删除行业相似达人",
        description = "批量删除tb_similar_collect关系表中的记录。支持一次删除多个相似达人关系。"
    )
    @GetMapping("/admin/batchDelete")
    public R<Boolean> batchDeleteSimilarCollect(
        @RequestParam @Parameter(description = "相似主播收藏ID列表（tb_similar_collect主键列表）", required = true, example = "[789012, 789013, 789014]")
        List<Long> similarCollectIds) {
        return R.ok(tradeRankAnchorProducer.batchDeleteSimilarCollect(similarCollectIds));
    }

    /**
     * 后台管理-更改行业热榜生效状态
     *
     * @param tradeId     行业ID
     * @param rankEnabled 热榜生效状态（0:不生效, 1:生效）
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-12
     */
    @Operation(
        summary = "后台管理-更改行业热榜生效状态",
        description = "更改指定行业的热榜生效状态。0表示不生效（热榜不显示该行业数据），1表示生效（热榜显示该行业数据）。"
    )
    @GetMapping("/admin/updateRankEnabled")
    public R<Boolean> updateTradeRankEnabled(
        @Parameter(description = "行业ID", required = true, example = "1001")
        @RequestParam Long tradeId,
        @Parameter(description = "热榜生效状态（0:不生效, 1:生效）", required = true, example = "1")
        @RequestParam Integer rankEnabled) {
        return R.ok(tradeRankAnchorProducer.updateTradeRankEnabled(tradeId, rankEnabled));
    }

    /**
     * 测试接口-获取所有有效的叶子节点行业信息
     * 用于验证排除逻辑是否正确
     *
     * @return 有效的叶子节点行业信息列表（包含ID和名称）
     *
     * @author RayChou
     * @date 2025-11-13
     */
    @Operation(
        summary = "测试接口-获取所有有效的叶子节点行业信息",
        description = "返回所有有效的最底级行业（叶子节点）的ID和名称，已排除配置中指定的行业及其所有子行业。" +
            "用于测试验证排除逻辑是否正确。"
    )
    @GetMapping("/test/leafTrades")
    public R<List<LeafTradeVo>> getLeafTradeInfoList() {
        return R.ok(tradeRankAnchorProducer.getLeafTradeInfoList());
    }



    /**
     * 检测相似主播
     *
     * @param anchorNumber 主播抖音号
     * @param excludeSimilarAnchorId 排除的相似主播ID 列表中返回的ID
     */
    @GetMapping("/check-similar-anchor")
    public R<RankAnchorCheckResult> checkSimilarAnchor(@RequestParam String anchorNumber, @RequestParam(required = false) Long excludeSimilarAnchorId) {
        DouyinOpenResult<DouyinAnchorBo> openResult = douyinOpenFeign.searchAnchor(anchorNumber, true);
        if (openResult.failed()) {
            RankAnchorCheckResult checkResult = new RankAnchorCheckResult();
            checkResult.setPassed(false);
            checkResult.setReason(openResult.getMessage());
            return R.ok(checkResult);
        }
        RankAnchorCheckResult checkResult = new RankAnchorCheckResult();
        checkResult.setPassed(true);
        checkResult.setAnchorNumber(openResult.getData().getUnique_id());
        checkResult.setAnchorName(openResult.getData().getNickname());
        checkResult.setAnchorAvatar(openResult.getData().getAvatar());
        checkResult.setFollowCount(openResult.getData().getFollow_count());
        checkResult.setSecUid(openResult.getData().getSec_uid());
        return R.ok(checkResult);
    }

    /**
     * 同步第三方排行数据
     *
     * @param id 行业ID
     */
    @PostMapping("/sync-third-rank")
    public R<Void> syncThirdRank(@RequestBody @Validated IdVo id) {
        Long tradeId = id.getId();
        if (tradeId == null) {
            return R.error("请选择行业");
        }
        List<TradeThirdRankingVO> result = tradeRankAnchorProducer.getThirdRankingList(tradeId);
        if (EmptyUtil.isEmpty(result)) {
            return R.error("请先关联第三方榜单");
        }
        List<String> waitList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        // 构建回调地址
        String callBackUrl = commonProperties.getCurrentSystemAddress() + "replay/words/tradeRank/third-rank-callback";
        result.forEach(thirdTrade -> {
            if (!Boolean.TRUE.equals(thirdTrade.getEnableCollect())) {
                return;
            }
            R<ThirdSalesRankingResult> salesRankingResult = chanmamaFeign.getSalesRanking(thirdTrade.getThirdRankingId(), callBackUrl);
            if (salesRankingResult.fail() && !Objects.equals(salesRankingResult.getCode(),1)) {
                log.error("[第三方榜单] 榜单-{}，get sales ranking error, thirdId: {}, result: {}", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId(), salesRankingResult.getMsg());
                errorList.add(thirdTrade.getThirdRankingName());
                return;
            }
            if (EmptyUtil.isEmpty(salesRankingResult.getData()) || EmptyUtil.isEmpty(salesRankingResult.getData().getSalesRankingVos())) {
                log.info("[第三方榜单] 榜单-{}，get sales ranking success, thirdId: {} Waiting for third-party processing callback", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId());
                waitList.add(thirdTrade.getThirdRankingName());
                return;
            }
            log.info("[第三方榜单] 榜单-{}，get sales ranking success, thirdId: {}, resultSize: {}", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId(), salesRankingResult.getData().getSalesRankingVos().size());
            tradeRankAnchorProducer.processThirdRankAnchor(thirdTrade.getTradeId(), thirdTrade.getThirdRankingId(), salesRankingResult.getData().getSalesRankingVos());
        });
        if (EmptyUtil.isEmpty(errorList) && EmptyUtil.isEmpty(waitList)) {
            return R.ok();
        }
        StringBuilder sb = new StringBuilder();
        if (EmptyUtil.isNotEmpty(errorList)) {
            sb.append("同步失败榜单: \r\n");
            errorList.forEach(error -> sb.append(error).append(";"));
        }
        if (!sb.isEmpty()) {
            sb.append("\r\n");
        }
        if (EmptyUtil.isNotEmpty(waitList)) {
            sb.append("等待第三方处理回调榜单: \r\n");
            waitList.forEach(wait -> sb.append(wait).append(";"));
        }
        return R.error(sb.toString());
    }


    /**
     * 添加新的主播数据
     *
     * @param uploadSimilarAnchor 新的主播数据
     */
    @PostMapping("/add-anchor")
    public R<Void> addNewAnchor(@RequestBody @Validated ExcelUploadSimilarAnchor uploadSimilarAnchor) {
        return tradeRankAnchorProducer.addNewAnchor(uploadSimilarAnchor);
    }

    /**
     * 修改主播数据
     *
     * @param anchorDto 主播数据
     */
    @PutMapping("/update-anchor")
    public R<Void> updateAnchor(@RequestBody @Validated UpdateSimilarAnchorDto anchorDto) {
        return tradeRankAnchorProducer.updateAnchor(anchorDto);
    }


    /**
     * 下载模版
     */
    @GetMapping("/download-template")
    public ResponseEntity<byte[]> downloadTemplate() {
        return ExcelUtils.download(1, idx -> {
            if (idx != null) {
                return List.of();
            }
            ExcelUploadSimilarAnchor similarAnchor = new ExcelUploadSimilarAnchor();
            similarAnchor.setAnchorName("这里填主播名称");
            return List.of(similarAnchor);
        }, ExcelUploadSimilarAnchor::getAnchorName, "热榜主播导入模版" + ExcelTypeEnum.XLSX.getValue(), ExcelUploadSimilarAnchor.class);
    }

    /**
     * 导入热榜主播数据
     *
     * @param request 上传请求
     *
     * @return 上传结果
     */
    @PostMapping("/upload-anchor")
    public R<Void> uploadAnchor(ExcelUploadSimilarAnchorRequest request) throws IOException {
        if (EmptyUtil.isEmpty(request.getTradeId())) {
            return R.error("请选择行业");
        }
        if (EmptyUtil.isEmpty(request.getFile()) || request.getFile().isEmpty()) {
            return R.error("请选择文件");
        }
        MultipartFile file = request.getFile();
        String name = file.getName();
        if (EmptyUtil.isEmpty(name)) {
            name = file.getOriginalFilename();
        }
        if (EmptyUtil.isEmpty(name)) {
            return R.error("文件已损坏,无法导入");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return tradeRankAnchorProducer.importAnchor(inputStream, request.getTradeId());
        }
    }

    /**
     * 获取行业第三方榜单树形结构
     *
     * @param parentId 父级ID
     * @param keyword  关键词
     * @param limit    限制数量 最大1000
     *
     * @return 第三方榜单树形结构
     */
    @GetMapping("/third-tree-ranking")
    public R<List<TreeTradeThirdRank>> getTreeThirdRank(Long parentId, String keyword, Integer limit) {
        List<TreeTradeThirdRank> treeThirdRank = tradeRankAnchorProducer.getTreeThirdRank(parentId, keyword, limit);
        return R.ok(treeThirdRank);
    }

    /**
     * 获取行业关联第三方榜单列表
     *
     * @param tradeId 行业ID
     *
     * @return 第三方榜单列表
     */
    @GetMapping("/third-ranking")
    public R<List<TradeThirdRankingVO>> getThirdRankingList(@RequestParam Long tradeId) {
        return R.ok(tradeRankAnchorProducer.getThirdRankingList(tradeId));
    }


    /**
     * 添加行业关联第三方榜单
     *
     * @param dto 第三方榜单信息
     *
     * @return 操作结果
     */
    @PostMapping("/third-ranking")
    public R<Void> addTradeThirdRanking(@RequestBody @Validated TradeThirdRankingDto dto) {
        return tradeRankAnchorProducer.addTradeThirdRanking(dto);
    }


    /**
     * 修改行业关联第三方榜单
     *
     * @param dto 第三方榜单信息
     *
     * @return 操作结果
     */
    @PostMapping("/update-third")
    public R<Void> updateTradeThirdRanking(@RequestBody @Validated TradeThirdRankingDto dto) {
        return tradeRankAnchorProducer.updateTradeThirdRanking(dto);
    }

    /**
     * 删除行业关联第三方榜单
     *
     *
     * @return 操作结果
     */
    @PostMapping("/delete-third")
    public R<Void> deleteTradeThirdRanking(@RequestBody @Validated IdVo request) {
        return tradeRankAnchorProducer.deleteTradeThirdRanking(request.getId());
    }


    /**
     * 上榜
     *
     * @param request 列表记录中返回的 id
     *
     * @return 操作结果
     */
    @PostMapping("/up")
    public R<Void> up(@RequestBody @Validated IdVo request) {
        return tradeRankAnchorProducer.upRank(request.getId());
    }

    /**
     * 下榜
     *
     * @param request 列表记录中返回的 id
     *
     * @return 操作结果
     */
    @PostMapping("/down")
    public R<Void> down(@RequestBody @Validated IdVo request) {
        return tradeRankAnchorProducer.downRank(request.getId());
    }
}
