package com.jiuyu.replay.api.controller.openapi;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.api.annotation.TenantLock;
import com.jiuyu.replay.api.controller.openapi.governance.response.OpenGovernanceUserDetailsInfo;
import com.jiuyu.replay.api.logic.common.CommonLogic;
import com.jiuyu.replay.api.logic.order.InvitationCodeLogic;
import com.jiuyu.replay.api.logic.order.OrderLogic;
import com.jiuyu.replay.api.logic.order.UserPropertyLogic;
import com.jiuyu.replay.api.logic.power.UserLogic;
import com.jiuyu.replay.api.logic.third.AiModelLogic;
import com.jiuyu.replay.api.logic.words.AnchorVideoLogic;
import com.jiuyu.replay.api.logic.words.SensitiveWordsLogic;
import com.jiuyu.replay.api.logic.words.UserVideoAppealLogic;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.bll.ClientUpdateBll;
import com.jiuyu.replay.common.bll.ComputerConfigBll;
import com.jiuyu.replay.common.bo.ClientLogBo;
import com.jiuyu.replay.common.bo.ComputerConfigBo;
import com.jiuyu.replay.common.bo.ClientUpdateRecordBo;
import com.jiuyu.replay.common.bo.ClientVersionBo;
import com.jiuyu.replay.common.constant.TencentCosProperties;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.ClientUpdateFileInfoVo;
import com.jiuyu.replay.common.vo.ClientUpdateNewestInfoVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserAccountInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.order.bll.PackageBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusV2Bo;
import com.jiuyu.replay.order.bo.IsPropertyHaveBo;
import com.jiuyu.replay.order.bo.SubAccountListBo;
import com.jiuyu.replay.order.dto.UserPropertyTypeCacheDto;
import com.jiuyu.replay.order.vo.IsPropertyHaveVo;
import com.jiuyu.replay.order.vo.PackageListAllByClientVo;
import com.jiuyu.replay.power.bo.bindingSubAccountBo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.third.governance.GovernanceEmployeeService;
import com.jiuyu.replay.words.bo.CloudContrastListBo;
import com.jiuyu.replay.words.bo.CloudVideoListBo;
import com.jiuyu.replay.words.bo.UserVideoAppealClientBo;
import com.jiuyu.replay.words.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("replay/openapi")
@Tag(name = "客户端openAPI")
@Slf4j
public class ClientOpenApi {


    @Autowired
    private ClientUpdateBll clientUpdateBll;

    @Resource
    private UserPropertyLogic userPropertyLogic;

    @Autowired
    private PackageBll packageBll;

    @Autowired
    private UserLogic userLogic;

    @Autowired
    private OrderLogic orderLogic;

    @Resource
    private InvitationCodeLogic invitationCodeLogic;
    @Resource
    private SensitiveWordsLogic sensitiveWordsLogic;
    @Resource
    private UserVideoAppealLogic userVideoAppealLogic;
    @Resource
    private AnchorVideoLogic anchorVideoLogic;
    @Resource
    private TencentCosProperties tencentCosProperties;
    @Resource
    private CommonLogic commonLogic;
    @Autowired
    private AiModelLogic aiModelLogic;

    @Autowired
    private GovernanceEmployeeService employeeService;

    @Resource
    private ComputerConfigBll computerConfigBll;

    /**
     * 获取在线对比复盘分析信息，以zip形式返回
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线对比复盘分析信息，以zip形式返回")
    @GetMapping("/getOnlineContrastAnalysisZip")
    public byte[] getOnlineContrastAnalysisZip(@RequestParam(required = false) String contrastId) throws Exception {

        return sensitiveWordsLogic.getOnlineContrastAnalysisZip(contrastId);
    }

    /**
     * 获取云空间对比列表
     * @param cloudContrastListBo 请求参数
     * @return
     */
    @Operation(summary = "获取云空间对比列表")
    @PostMapping("/listCloudContrast")
    public R<PageUtils<SyncContrastListVoUpper>> listCloudContrast(@RequestBody CloudContrastListBo cloudContrastListBo) {

        R<PageUtils<SyncContrastListVo>> pageUtilsR = anchorVideoLogic.listCloudContrast(cloudContrastListBo);

        PageUtils<SyncContrastListVo> pageUtilsRData = pageUtilsR.getData();

        PageUtils<SyncContrastListVoUpper> pageUtils = new PageUtils<>();
        pageUtils.setTotalPage(pageUtilsRData.getTotalPage());
        pageUtils.setCurrPage(pageUtilsRData.getCurrPage());
        pageUtils.setPageSize(pageUtilsRData.getPageSize());
        pageUtils.setTotalCount(pageUtilsRData.getTotalCount());
        List<SyncContrastListVo> dataList = pageUtilsRData.getList();
        if(dataList != null && dataList.size() > 0) {
            String jsonStr = JSON.toJSONString(dataList);
            List<SyncContrastListVoUpper> anchorVideoInfoVoUppers = JSON.parseArray(jsonStr, SyncContrastListVoUpper.class);
            pageUtils.setList(anchorVideoInfoVoUppers);
        }

        return R.ok(pageUtils);
    }

    /**
     * 获取云空间视频列表
     * @param cloudVideoListBo 请求参数
     * @return
     */
    @Operation(summary = "获取云空间视频列表")
    @PostMapping("/listCloudVideo")
    public R<PageUtils<AnchorVideoInfoVoUpper>> listCloudVideo(@RequestBody CloudVideoListBo cloudVideoListBo) {

        R<PageUtils<AnchorVideoInfoVo>> pageUtilsR = anchorVideoLogic.listCloudVideo(cloudVideoListBo);
        PageUtils<AnchorVideoInfoVo> pageUtilsRData = pageUtilsR.getData();

        PageUtils<AnchorVideoInfoVoUpper> pageUtils = new PageUtils<>();
        pageUtils.setTotalPage(pageUtilsRData.getTotalPage());
        pageUtils.setCurrPage(pageUtilsRData.getCurrPage());
        pageUtils.setPageSize(pageUtilsRData.getPageSize());
        pageUtils.setTotalCount(pageUtilsRData.getTotalCount());
        List<AnchorVideoInfoVo> dataList = pageUtilsRData.getList();
        if(dataList != null && dataList.size() > 0) {
            String jsonStr = JSON.toJSONString(dataList);
            List<AnchorVideoInfoVoUpper> anchorVideoInfoVoUppers = JSON.parseArray(jsonStr, AnchorVideoInfoVoUpper.class);
            pageUtils.setList(anchorVideoInfoVoUppers);
        }

        return R.ok(pageUtils);
    }

    /**
     * 下载分析文件
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "下载分析文件")
    @GetMapping("/downloadAnalysisFile/{type}/{uuid}")
    public byte[] downloadAnalysisFile(@PathVariable Integer type, @PathVariable String uuid) {

        return sensitiveWordsLogic.downloadAnalysisFile(type, uuid);
    }

    /**
     * 获取在线复盘分析信息，以zip形式返回
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线复盘分析信息，以zip形式返回")
    @GetMapping("/getOnlineAnalysisZip/{type}/{uuid}")
    public byte[] getOnlineAnalysisZip(@PathVariable Integer type, @PathVariable String uuid) throws Exception {

        String fileId = "";
        String videoId = "";
        if(type == 0) {
            videoId = uuid;
        }else {
            fileId = uuid;
        }

        return sensitiveWordsLogic.getOnlineAnalysisZip(fileId, videoId);
    }

    /**
     * 获取在线复盘分析信息
     * @param fileId 文件唯一标识 uuid，传其中一个
     * @param videoId 视频唯一标识 uuid，传其中一个
     * @return
     */
    @Operation(summary = "获取在线复盘分析信息")
    @GetMapping("/getOnlineAnalysisInfo")
    public R<OnlineAnalysisInfoVo> getOnlineAnalysisInfo(@RequestParam(required = false) String fileId, @RequestParam(required = false) String videoId) throws Exception {

        return sensitiveWordsLogic.getOnlineAnalysisInfo(fileId, videoId);
    }

    /**
     * 获取在线对比复盘分析信息
     * @param contrastId 对比记录唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线对比复盘分析信息")
    @GetMapping("/getOnlineContrastAnalysisInfo")
    public R<OnlineContrastAnalysisInfoVo> getOnlineContrastAnalysisInfo(@RequestParam(required = false) String contrastId) throws Exception {

        return sensitiveWordsLogic.getOnlineContrastAnalysisInfo(contrastId);
    }


    /**
     * 新增客户端日志
     * @param clientLogBo
     * @return
     */
    @Operation(summary = "新增客户端日志")
    @PostMapping("/clientlog/save")
    public R<String> save(@RequestBody ClientLogBo clientLogBo) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        clientLogBo.setClientUser(localUser.getUsername());
        clientLogBo.setUserId(localUser.getId());
        return commonLogic.addClientLog(clientLogBo);
    }

    @Operation(summary = "获取用户的资产")
    @GetMapping("/userproperty/getUserProperty")
    public R<UserPropertyTypeCacheDto> getUserProperty(@RequestParam(required = false) Long userId){
        return userPropertyLogic.getUserProperty(userId);
    }

    @TenantLock(prefixKey = "tenant_lock:useProperty")
    @Operation(summary = "使用用户资产-有redisId，多退少补")
    @PostMapping("/userproperty/usePropertyReal")
    public R<String> minusAssetsReal(@RequestBody AssetsMinusOrPlusBo assets){
        RRException.isTrue(assets.getNum() > 0, "使用的量不能小于0");
        RRException.isNotEmpty(assets.getRedisId(), "redisId不能为空");
        assets.setNum(-Math.abs(assets.getNum()));
        return userPropertyLogic.assetsMinusOrPlusReal(assets);
    }

    @TenantLock(prefixKey = "tenant_lock:useProperty")
    @Operation(summary = "使用用户资产-有redisId，多退少补-aiToken")
    @PostMapping("/userproperty/minusAssetsRealAiToken")
    public R<String> minusAssetsRealAiToken(@RequestBody AssetsMinusOrPlusV2Bo assets) {
        RRException.isTrue(assets.getNum() > 0, "使用的量不能小于0");
        RRException.isNotEmpty(assets.getRedisId(), "redisId不能为空");
        assets = aiModelLogic.AiTokenConsumeMultiple(assets);
        assets.setNum(-Math.abs(assets.getNum()));
        return userPropertyLogic.assetsMinusOrPlusReal(assets);
    }

    @TenantLock(prefixKey = "tenant_lock:useProperty")
    @Operation(summary = "使用用户资产")
    @PostMapping("/userproperty/useProperty")
    public R<String> minusAssets(@RequestBody AssetsMinusOrPlusBo assets){
        if (assets.getNum() <= 0) return R.error(400, "使用的量不能小于0");
        assets.setNum(-assets.getNum());
        return userPropertyLogic.assetsMinusOrPlus(assets);
    }

    @TenantLock(prefixKey = "tenant_lock:useProperty")
    @Operation(summary = "加回用户的资产")
    @PostMapping("/userproperty/addProperty")
    public R<String> plusAssets(@RequestBody AssetsMinusOrPlusBo assets){
        if (assets.getNum() <= 0) return R.error(400, "使用的量不能小于0");
        assets.setNum(Math.abs(assets.getNum()));
        return userPropertyLogic.assetsMinusOrPlus(assets);
    }

    @Operation(summary = "检查用户是否可以使用资产")
    @PostMapping("/userproperty/isPropertyHave")
    @TenantLock(prefixKey = "tenant_lock:useProperty")
    public R<IsPropertyHaveVo> isPropertyHave(@RequestBody IsPropertyHaveBo bo) {
        return userPropertyLogic.isHave(bo);
    }

    @Operation(summary = "检查用户是否可以使用资产")
    @PostMapping("/userproperty/isPropertyHaveAiToken")
    @TenantLock(prefixKey = "tenant_lock:useProperty")
    public R<IsPropertyHaveVo> isPropertyHaveAiToken(@RequestBody IsPropertyHaveBo bo){
        bo.setCode("aiTokenNum");
        bo.setThisUseNum(-Math.abs(bo.getThisUseNum()));
        return userPropertyLogic.isPropertyHaveAiToken(bo);
    }

    @Operation(summary = "删除预扣资产")
    @GetMapping("/userproperty/removeTempUserProperty")
    @TenantLock(prefixKey = "tenant_lock:useProperty")
    public R<String> removeTempUserProperty(Long redisId, String WithholdId) {
        return userPropertyLogic.removeTempUserProperty(WithholdId, redisId);
    }

    @Operation(summary = "检测最新的客户端版本")
    @GetMapping("/clientupdate/checkNewest")
    public R<List<ClientUpdateNewestInfoVo>> checkNewest(@Parameter(description = "客户端版本号") @RequestParam("version") String version,
                                                         @Parameter(description = "客户端发布时间") @RequestParam("publishTime") String publishTime) {

        return clientUpdateBll.checkNewest(version, publishTime);

    }

    @Operation(summary = "获取套餐列表")
    @GetMapping("/package/packageListAll")
    public R<PackageListAllByClientVo> packageListAll(@Parameter(description = "用户id", required = true) @RequestParam(required = false)Long userId){
        if (ObjectUtil.isEmpty(userId)){
            UserCacheVo localUser = GlobalObject.getLocalUser();
            userId = localUser.getId();
        }
        return packageBll.packageListAllByClient(userId);
    }

    /**
     * 使用邀请码
     * @param code 邀请码
     * @return
     */
    @GetMapping("/invitationcode/exchange")
    @Operation(summary = "使用邀请码")
    @CustomRedissonLock(key = "'replay:check:invitationCode:' + #args[0]")
    public R<String> exchange(@Parameter(description = "邀请码", required = true) @RequestParam("code") String code){
        if (code == null) {
            return R.error(50001, "邀请码不能为空");
        }
        return invitationCodeLogic.exchange(code);
    }

    @PostMapping("/user/setUpASubAccount")
    @Operation(summary = "子账号绑定")
    public R<String> setUpASubAccount(@RequestBody @Validated(bindingSubAccountBo.binding.class) bindingSubAccountBo aSubAccountBo) {
        R<String> result = userLogic.setUpASubAccount(aSubAccountBo, true);
        if (result.success()) {
            Long subUserId = aSubAccountBo.getSubUserId();
            if (subUserId == null) {
                subUserId = userLogic.getPhoneUserId(aSubAccountBo.getSubPhone()).orElse(null);
            }
            if (subUserId == null) {
                return result;
            }
            R<OpenGovernanceUserDetailsInfo> info = userLogic.getMobileAccount(subUserId);
            if (info.getData() != null && info.getData().getPackageLevel() != null && info.getData().getPackageLevel() >= 20) {
                UserAccountInfoVo accountInfoVo = BeanUtil.copyProperties(info.getData(), UserAccountInfoVo.class);
                R<Void> bindResult = employeeService.bind(accountInfoVo);
                log.info("[绑定账户] user bind rest employee, user:{}, tenant: {}, {}, msg: {}", aSubAccountBo.getSubUserId(), info.getData().getTenantId(), bindResult.success() ? "成功" : "失败", bindResult.getMsg());
            }
        }
        return result;
    }

    @PostMapping("/user/setUpASubAccountBackstage")
    @Operation(summary = "后台子账号绑定")
    public R<String> setUpASubAccountBackstage(@RequestBody @Validated(bindingSubAccountBo.adminBinding.class) bindingSubAccountBo aSubAccountBo) {
        R<String> result = userLogic.setUpASubAccount(aSubAccountBo, !"666666".equals(aSubAccountBo.getCode()));
        if (result.success()) {
            Long subUserId = aSubAccountBo.getSubUserId();
            if (subUserId == null) {
                subUserId = userLogic.getPhoneUserId(aSubAccountBo.getSubPhone()).orElse(null);
            }
            if (subUserId == null) {
                return result;
            }
            R<OpenGovernanceUserDetailsInfo> info = userLogic.getMobileAccount(subUserId);
            if (info.getData() != null && info.getData().getPackageLevel() != null && info.getData().getPackageLevel() >= 20) {
                UserAccountInfoVo accountInfoVo = BeanUtil.copyProperties(info.getData(), UserAccountInfoVo.class);
                R<Void> bindResult = employeeService.bind(accountInfoVo);
                log.info("[绑定账户] user bind rest employee, user:{}, tenant: {}, {}, msg: {}", aSubAccountBo.getSubUserId(), info.getData().getTenantId(), bindResult.success() ? "成功" : "失败", bindResult.getMsg());
            }
        }
        return result;
    }

    @PostMapping("/user/unbindingSubAccount")
    @Operation(summary = "子账号解绑")
    public R<String> unbindingSubAccount(@RequestBody @Validated(bindingSubAccountBo.unbind.class) bindingSubAccountBo aSubAccountBo) {
        R<String> stringR = userLogic.unbindingSubAccount(aSubAccountBo);
        // 检查子账号解绑后的订单是否正常-主要检查是否有套餐
        orderLogic.checkUserOrder(aSubAccountBo.getSubUserId());
        if (stringR.success()) {
            employeeService.unbind(aSubAccountBo.getSubUserId());
        }
        return stringR;
    }

    /**
     * 用户列表
     * @return
     */
    @PostMapping("/user/subAccountList")
    @Operation(summary = "子用户列表")
    public R<PageUtils<UserListVo>> subAccountList(@RequestBody SubAccountListBo bo){
        return userLogic.subAccountList(bo);
    }

    @Operation(summary = "根据版本号查询版本信息")
    @GetMapping("/clientupdate/getVersionByVersion")
    public R<ClientUpdateNewestInfoVo> getVersionByVersion(@Parameter(description = "客户端版本号") @RequestParam("version") String version,
                                                           @Parameter(description = "类型 0：爱复盘软件补丁包 1：更新软件补丁包") @RequestParam("isFront") Integer isFront,
                                                           @Parameter(description = "客户端状态 0开发，1发布", required = false) @RequestParam(value = "status", required = false) Integer status
    ) {
        UserCacheVo user = GlobalObject.getLocalUser();
        com.jiuyu.replay.generic.vo.power.UserCacheVo newUser = user == null ? null : BeanUtil.copyProperties(user, com.jiuyu.replay.generic.vo.power.UserCacheVo.class);
        R<ClientUpdateNewestInfoVo> versionByVersion = clientUpdateBll.getVersionByVersion(newUser, version, isFront, status);
        // 设置下载地址
        if (versionByVersion.getCode() == 0 && ObjectUtil.isNotEmpty(versionByVersion.getData())){
            ClientUpdateNewestInfoVo data = versionByVersion.getData();
            ArrayList<String> fileDownLoadUrls = new ArrayList<>();
            fileDownLoadUrls.add(tencentCosProperties.getPublicBucket().getAccessUrl() + "/" + data.getCosKey());
            data.setFileDownLoadUrls(fileDownLoadUrls);
            // 如果clientFilesPath有值，把JSON中的cosKey加上域名后写回
            if (ObjectUtil.isNotEmpty(data.getClientFilesPath())) {
                List<ClientUpdateFileInfoVo> fileList = JSONUtil.toList(data.getClientFilesPath(), ClientUpdateFileInfoVo.class);
                String accessUrl = tencentCosProperties.getPublicBucket().getAccessUrl();
                for (ClientUpdateFileInfoVo file : fileList) {
                    file.setCosKey(accessUrl + "/" + file.getCosKey());
                }
                data.setClientFilesPath(JSONUtil.toJsonStr(fileList));
            }
        }
        return versionByVersion;
    }

    @Operation(summary = "根据版本号查询补丁包")
    @GetMapping("/clientupdate/getPackageVersion")
    public R<ClientUpdateNewestInfoVo> getPackageVersion(
            @Parameter(description = "客户端版本号") @RequestParam("version") String version,
            @Parameter(description = "现有的补丁包版本") @RequestParam("packageVersion") String packageVersion) {
        R<ClientUpdateNewestInfoVo> versionByVersion = clientUpdateBll.getPackageVersion(version, packageVersion);
        // 设置下载地址
        if (versionByVersion.getCode() == 0 && ObjectUtil.isNotEmpty(versionByVersion.getData())){
            ClientUpdateNewestInfoVo data = versionByVersion.getData();
            ArrayList<String> fileDownLoadUrls = new ArrayList<>();
            fileDownLoadUrls.add(tencentCosProperties.getPublicBucket().getAccessUrl() + "/img/" + data.getCosKey());
            data.setFileDownLoadUrls(fileDownLoadUrls);
        }
        return versionByVersion;
    }

    @Operation(summary = "保存客户端更新记录")
    @PostMapping("/clientupdate/saveRecord")
    public R<String> saveRecord(@RequestBody ClientUpdateRecordBo clientUpdateRecordBo) {
        clientUpdateRecordBo.setClientUser(GlobalObject.getLocalUser().getUsername());
        return clientUpdateBll.saveRecord(clientUpdateRecordBo);
    }

    /**
     * 下载文件
     * @return
     */
    @Operation(summary = "下载文件")
    @GetMapping("/clientupdate/downloadFile/{fileName}")
    public byte[] downloadFile(@Parameter(description = "版本号") @PathVariable String fileName) {

        return clientUpdateBll.downLoadFile(fileName);
    }

    @Operation(summary = "设置客户端版本")
    @PostMapping("/clientupdate/setClientVersion")
    public R<String> setClientVersion(@RequestBody ClientVersionBo bo){
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (ObjectUtil.isNotEmpty(localUser) && ObjectUtil.isEmpty(bo.getUserId())){
            bo.setUserId(localUser.getId());
        }
        return clientUpdateBll.setClientVersion(bo);
    }

    @Operation(summary = "用户上传视频申述")
    @PostMapping("/uservideoappeal/uploadVideoAppeal")
    public R<String> uploadVideoAppeal(@RequestBody UserVideoAppealClientBo bo){
        return userVideoAppealLogic.uploadVideoAppeal(bo);
    }

    @Operation(summary = "客户端登录后的接口")
    @GetMapping("/user/clientLogoPost")
    public R<String> clientLogoPost(@RequestParam(required = false, defaultValue = "replay") String clientVersion) {
        userLogic.clientLogoPost(clientVersion);
        return R.ok();
    }

    @Operation(summary = "客户端上报计算机配置")
    @PostMapping("/user/saveComputerConfig")
    public R<String> saveComputerConfig(@RequestBody ComputerConfigBo bo) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        RRException.isNotEmpty(bo.getCpuId(), "cpuId不能为空");
        computerConfigBll.saveOrUpdate(bo, localUser.getId());
        return R.ok();
    }
}
