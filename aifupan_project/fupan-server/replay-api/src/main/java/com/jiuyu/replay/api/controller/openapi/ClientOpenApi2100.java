package com.jiuyu.replay.api.controller.openapi;

import cn.hutool.core.util.ObjectUtil;
import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.sql.SQLResultSet;
import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.system.DictDataLogic;
import com.jiuyu.replay.api.logic.third.TableStoreLogic;
import com.jiuyu.replay.api.logic.words.AnchorUrlLogic;
import com.jiuyu.replay.api.logic.words.SensitiveWordsLogic;
import com.jiuyu.replay.api.logic.words.SocketCollectMessageLogic;
import com.jiuyu.replay.common.vo.ClientConfigVo;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.words.video.VideoDanMuExportDetailVo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.third.bo.*;
import com.jiuyu.replay.third.tablestore.TableStoreServiceUtils;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import com.jiuyu.replay.third.vo.QueryOtherDanMuVo;
import com.jiuyu.replay.words.vo.AnalysisContractResultCloudVo;
import com.jiuyu.replay.words.vo.AnalysisResultCloudVo;
import com.jiuyu.replay.words.vo.OnlineChartVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author ：lujie
 * @description：客户端openAPI-2.1.00
 * @date ：2025/1/14 上午11:45
 */
@RestController
@CrossOrigin
@RequestMapping("replay/openapi/v2100")
@Tag(name = "客户端openAPI-2.1.00")
@AllArgsConstructor
public class ClientOpenApi2100 {

    private final TableStoreLogic tableStoreLogic;
    private final SyncClient syncClient;
    private final AnchorUrlLogic anchorUrlLogic;
    private final SocketCollectMessageLogic socketCollectMessageLogic;
    private final DictDataLogic dictDataLogic;
    private final SensitiveWordsLogic sensitiveWordsLogic;

    /**
     * 获取在线复盘分析信息2_1
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线复盘分析信息2_1")
    @GetMapping("/getOnlineAnalysis")
    public R<AnalysisResultCloudVo> getOnlineAnalysis2_1(
            @Parameter(description = "类型 0：视频 1：文件", required = true)@RequestParam Integer type,
            @Parameter(description = "视频或文件的唯一标识 uuid", required = true)@RequestParam String uuid) throws Exception {

        return sensitiveWordsLogic.getOnlineAnalysis2_1(type, uuid);
    }

    /**
     * 获取在线对比复盘分析信息2_1
     * @param contrastId 对比id
     * @return
     */
    @Operation(summary = "获取在线对比复盘分析信息2_1")
    @GetMapping("/getOnlineContrastAnalysis")
    public R<AnalysisContractResultCloudVo> getOnlineContrastAnalysis2_1(@Parameter(description = "对比id", required = true)@RequestParam(required = false) String contrastId) throws Exception {

        return sensitiveWordsLogic.getOnlineContrastAnalysis2_1(contrastId);
    }

    /**
     * 获取在线复盘分析信息，以zip形式返回2_1
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线复盘分析信息，以zip形式返回2_0")
    @GetMapping("/getOnlineAnalysisZip/{type}/{uuid}")
    public byte[] getOnlineAnalysisZip2_1(@PathVariable Integer type, @PathVariable String uuid) throws Exception {

        return sensitiveWordsLogic.getOnlineAnalysisZip2_1(type, uuid);
    }

    /**
     * 获取在线对比复盘分析信息，以zip形式返回2_1
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线对比复盘分析信息，以zip形式返回2_0")
    @GetMapping("/getOnlineContrastAnalysisZip")
    public byte[] getOnlineContrastAnalysisZip2_1(@RequestParam(required = false) String contrastId) throws Exception {

        return sensitiveWordsLogic.getOnlineContrastAnalysisZip2_1(contrastId);
    }

    /**
     * 获取分析文件的下载链接地址
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取分析文件的下载链接地址")
    @GetMapping("/getAnalysisDownloadUrl")
    public R<String> getAnalysisDownloadUrl(@Parameter(description = "类型 0：视频 1：文件", required = true) @RequestParam Integer type,
                                            @Parameter(description = "视频或文件的唯一标识 uuid", required = true) @RequestParam String uuid) {

        return sensitiveWordsLogic.getAnalysisDownloadUrl(type, uuid);
    }

    /**
     * 修改用户-主播是否自动上传云空间的状态
     * @param secUid 主播secuid
     * @param isAutoUploadCloud 自动上传云空间 0否 1是
     * @return
     */
    @Operation(summary = "修改用户-主播是否自动上传云空间的状态")
    @GetMapping("/updateAutoUploadCloud")
    public R<String> updateAutoUploadCloud(@Parameter(description = "主播secuid", required = true)@RequestParam("secUid") String secUid,
                                           @Parameter(description = "自动上传云空间 0否 1是", required = true)@RequestParam("isAutoUploadCloud") Integer isAutoUploadCloud) {
        return anchorUrlLogic.updateAutoUploadCloud(secUid, isAutoUploadCloud);
    }


    @Operation(summary = "修改主播的弹幕监控位状态")
    @GetMapping("/updateBarrageMonitoring")
    @UserLock
    public R<String> updateBarrageMonitoring(@RequestParam("secUid") String secUid,
                                             @RequestParam("isBarrageMonitoring") Integer isBarrageMonitoring) {
        return anchorUrlLogic.updateBarrageMonitoring(secUid, isBarrageMonitoring);
    }

    @Operation(summary = "上传弹幕数据")
    @PostMapping("/uploadDanMuData")
    public R<String> uploadDanMuData(@RequestPart("danMuData") UploadDanMuDataBo danMuData, @RequestPart("file") MultipartFile file) throws Exception {
        return tableStoreLogic.uploadDanMuData(danMuData, file);
    }

    @Operation(summary = "搜索弹幕数据")
    @PostMapping("/queryDanMuData")
    public R<QueryDanMuVo> queryDanMuData(@RequestBody QueryDanMuBo queryDanMuBo){
        return tableStoreLogic.queryDanMuData(queryDanMuBo);
    }

    @Operation(summary = "搜索弹幕数据的总条数")
    @PostMapping("/queryDanMuCount")
    public R<VideoDanMuExportDetailVo> queryDanMuCount(@RequestBody QueryDanMuBo queryDanMuBo) {
        return tableStoreLogic.queryDanMuCount(queryDanMuBo);
    }

    @Operation(summary = "弹幕数据导出")
    @PostMapping("/queryDanMuExport")
    public void queryDanMuExport(HttpServletResponse response, @RequestBody QueryDanMuExport queryDanMuExport) throws IOException {
        tableStoreLogic.queryDanMuExport(response, queryDanMuExport);
    }

    @Operation(summary = "弹幕数据导出")
    @GetMapping("/queryDanMuExportGet")
    public void queryDanMuExportGet(HttpServletResponse response, QueryDanMuExport queryDanMuExport) throws IOException {
        tableStoreLogic.queryDanMuExport(response, queryDanMuExport);
    }

    @Operation(summary = "搜索其他场次弹幕数据")
    @PostMapping("/queryOtherDanMuData")
    public R<List<QueryOtherDanMuVo>> queryOtherDanMuData(@RequestBody QueryOtherDanMuBo danMu) throws Exception {
        return tableStoreLogic.queryOtherDanMuData(danMu);
    }

    @Operation(summary = "查询弹幕数据是否存在")
    @PostMapping("/existsBarrage")
    public R<Boolean> existsBarrage(@RequestBody UploadLocalDanMuDataBo bo){
        return tableStoreLogic.existsBarrage(bo);
    }

    @Operation(summary = "分析详情页的数据曲线")
    @GetMapping("/onlineChartData")
    public R<OnlineChartVo> onlineChartData(String videoId, Integer step) {
        if (step == null) {
            step = 1;
        }
        return socketCollectMessageLogic.onlineChartData(videoId, step);
    }

    @Operation(summary = "获取客户端配置")
    @GetMapping("/getClientConfig")
    public R<ClientConfigVo> getClientConfig() {
        ClientConfigVo result = new ClientConfigVo();

        DictDataListBo dictDataListBo = new DictDataListBo();
        dictDataListBo.setLimit(-1);

        // websocket方式 获取方式 0:js计算，1浏览器获取
        dictDataListBo.setTypeLogo("websocket_address_type");
        R<PageUtils<DictDataListVo>> pageUtilsR = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())  && !pageUtilsR.getData().getList().isEmpty()){
            result.setWebsocketWay(Integer.parseInt(pageUtilsR.getData().getList().get(0).getValue()));
        }

        // 弹幕保存最大数量
        dictDataListBo.setTypeLogo("max_barrage_num");
        R<PageUtils<DictDataListVo>> pageUtilsR1 = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR1.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR1.getData()) && ObjectUtil.isNotEmpty(pageUtilsR1.getData().getList())  && !pageUtilsR1.getData().getList().isEmpty()){
            result.setMaxBarrageNum(Integer.parseInt(pageUtilsR1.getData().getList().get(0).getValue()));
        }

        // 是否显示由腾讯云AI技术支持图标
        dictDataListBo.setTypeLogo("show_tencent_ai_icon");
        R<PageUtils<DictDataListVo>> pageUtilsR2 = dictDataLogic.queryPage(dictDataListBo);
        if (pageUtilsR2.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR2.getData()) && ObjectUtil.isNotEmpty(pageUtilsR2.getData().getList())  && !pageUtilsR2.getData().getList().isEmpty()){
            result.setShowTencentAiIcon(Integer.parseInt(pageUtilsR2.getData().getList().get(0).getValue()));
        }

        return R.ok(result);
    }

    @Operation(summary = "test111")
    @PostMapping("/test111")
    public R<String> test111(){

        SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql("select count(*) as count from review_barrage_detail", syncClient);

        if (sqlResultSet.hasNext()) {
            return R.ok(sqlResultSet.next().get(0).toString());
        }

        return R.ok("不知道");
    }

    @Operation(summary = "testTableSql")
    @PostMapping("/testTableSql")
    public R<String> testTableSql(String sql){
        SQLResultSet sqlResultSet = TableStoreServiceUtils.querySearchSql(sql, syncClient);

        if (sqlResultSet.hasNext()) {

            return R.ok(sqlResultSet.next().get(0).toString());
        }

        return R.ok("不知道");
    }
}