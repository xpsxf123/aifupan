package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.AnchorUrlLogic;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AuthUsageVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.anchor.AddOrUpdateAnchorBo;
import com.jiuyu.replay.words.bo.anchor.ClientAnchorListBo;
import com.jiuyu.replay.words.bo.anchor.TopAnchorBo;
import com.jiuyu.replay.words.vo.AnchorClientVo;
import com.jiuyu.replay.words.vo.anchor.AnchorRecordListVo;
import com.jiuyu.replay.words.vo.anchor.AnchorYesterdayRecordVo;
import com.jiuyu.replay.words.vo.anchor.OpenMonitoringPositionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@RestController
@CrossOrigin
@RequestMapping("replay/anchorurl")
@Tag(name = "主播")
public class AnchorUrlController {

    @Resource
    private AnchorUrlLogic anchorUrlLogic;

    @GetMapping("/getAiTrade")
    @Operation(summary = "获取Ai推荐的行业")
    public R<String> getAiTrade(String secUid) {
        this.anchorUrlLogic.getAiTrade(secUid);
        return R.ok();
    }

    /**
     * 发送上下播主播消息
     * @param anchorUrlName 主播名称
     * @param type 类型：0上播，1下播
     */
    @GetMapping("/sendSwitchAnchorMsg")
    @Operation(summary = "发送上下播主播消息")
    public R<String> sendSwitchAnchorMsg(@Parameter(description = "主播名称", required = true) @RequestParam String anchorUrlName,
                                         @Parameter(description = "类型：0上播，1下播", required = true) @RequestParam Integer type) {

        return this.anchorUrlLogic.sendSwitchAnchorMsg(anchorUrlName, type);
    }

    /**
     * 客户端获取AI复盘主播列表
     * @param clientAnchorListBo 查询参数
     * @return
     */
    @PostMapping("/clientAnchorRecordList")
    @Operation(summary = "客户端获取AI复盘主播列表")
    public R<PageUtils<AnchorRecordListVo>> clientAnchorRecordList(@RequestBody ClientAnchorListBo clientAnchorListBo) {

        return anchorUrlLogic.clientAnchorRecordList(clientAnchorListBo);
    }

    /**
     * 客户端获取云空间主播列表
     * @return
     */
    @PostMapping("/clientTenantAnchorList")
    @Operation(summary = "客户端获取云空间主播列表")
    public R<List<AnchorUrlUserVo>> clientTenantAnchorList() {

        return anchorUrlLogic.clientTenantAnchorList();
    }

    /**
     * 置顶主播
     * @param topAnchorBo 置顶主播参数
     * @return
     */
    @PostMapping("/topAnchor")
    @Operation(summary = "置顶主播")
    public R<String> topAnchor(@RequestBody TopAnchorBo topAnchorBo) {

        return anchorUrlLogic.topAnchor(topAnchorBo);
    }

    /**
     * 根据secUid获取用户主播信息（可获取同租户下的）
     * @param secUid 主播SecUid
     * @return
     */
    @GetMapping("/getUserAnchorBySecUid")
    @Operation(summary = "根据secUid获取用户主播信息（可获取同租户下的）")
    public R<AnchorUrlUserVo> getUserAnchorBySecUid(@Parameter(description = "主播SecUid", required = true) @RequestParam String secUid) {

        return anchorUrlLogic.getUserAnchorBySecUid(secUid);
    }

    /**
     * 根据secUid获取用户主播信息
     * @param secUid 主播SecUid
     * @return
     */
    @GetMapping("/getCurrUserAnchorBySecUid")
    @Operation(summary = "根据secUid获取用户主播信息")
    public R<AnchorUrlUserVo> getCurrUserAnchorBySecUid(@Parameter(description = "主播SecUid", required = true) @RequestParam String secUid) {

        return anchorUrlLogic.getCurrUserAnchorBySecUid(secUid);
    }

    /**
     * 客户端获取主播列表
     * @return
     */
    @GetMapping("/clientAnchorList")
    @Operation(summary = "客户端获取主播列表")
    public R<List<AnchorUrlUserVo>> clientAnchorList() {

        return anchorUrlLogic.clientAnchorList();
    }

    /**
     * 根据主播secuid集合获取昨日录制场次列表
     * @param secUidList 主播secuid集合
     * @return
     */
    @PostMapping("/listAnchorYesterdayRecord")
    @Operation(summary = "根据主播secuid集合获取昨日录制场次列表")
    public R<List<AnchorYesterdayRecordVo>> listAnchorYesterdayRecord(@Parameter(description = "主播secuid集合", required = true) @RequestBody List<String> secUidList) {

        return anchorUrlLogic.listAnchorYesterdayRecord(secUidList);
    }

    /**
     * 根据主播唯一标识(secUid、homeUrl、liveUrl)集合获取主播
     * @param uniquesList 主播唯一标识集合
     * @return
     */
    @PostMapping("/listByUniques")
    @Operation(summary = "根据主播唯一标识(secUid、homeUrl、liveUrl)集合获取主播")
    public R<List<AnchorUrlInfoVo>> listByUniques(@Parameter(description = "主播唯一标识集合", required = true) @RequestBody List<String> uniquesList){

        return anchorUrlLogic.listByUniques(uniquesList);
    }

    /**
     * 绑定主播和用户的关系
     * @param userAnchorBo 绑定信息
     * @return
     */
    @PostMapping("/bindUserAnchor")
    @Operation(summary = "绑定主播和用户的关系")
    public R<String> bindUserAnchor(@RequestBody UserAnchorBo userAnchorBo){
        return anchorUrlLogic.bindUserAnchor(userAnchorBo);
    }

    /**
     * 修改用户绑定的主播信息
     * @param anchorUrlUserBo
     * @return
     */
    @PostMapping("/updateUserAnchor")
    @Operation(summary = "修改用户绑定的主播信息")
    public R<String> updateUserAnchor(@Parameter(description = "用户与主播绑定", required = true) @RequestBody AnchorUrlUserBo anchorUrlUserBo){

        return anchorUrlLogic.updateUserAnchor(anchorUrlUserBo);
    }

    /**
     * 添加或修改用户的主播信息
     * @param addOrUpdateAnchorBo 添加或修改用户的主播信息参数
     * @return
     */
    @PostMapping("/addOrUpdateAnchor")
    @Operation(summary = "添加或修改用户的主播信息")
    public R<String> addOrUpdateAnchor(@Parameter(description = "用户与主播绑定", required = true) @RequestBody AddOrUpdateAnchorBo addOrUpdateAnchorBo){

        return anchorUrlLogic.addOrUpdateAnchor(addOrUpdateAnchorBo);
    }






















    /**
     * 添加或修改主播信息
     * @param anchorUrlBo 主播信息
     * @return
     */
    @PostMapping("/saveOrUpdateAnchor")
    @Operation(summary = "添加或修改主播信息")
    public R<String> saveOrUpdateAnchor(@RequestBody AnchorUrlBo anchorUrlBo){
        return anchorUrlLogic.saveOrUpdateAnchor(anchorUrlBo);
    }

    /**
     * 根据userId获取当前用户添加的主播信息
     * @param anchorUrlUserBo
     * @return
     */
    @PostMapping("/userAddAnchorRecord")
    @Operation(summary = "根据userId获取当前用户添加的主播信息")
    public R<PageUtils<AnchorUrlVo>> userAddAnchorRecord(@RequestBody AnchorUrlUserBo anchorUrlUserBo){
        return anchorUrlLogic.userAddAnchorRecord(anchorUrlUserBo);
    }

    /**
     * 根据user_id从主播用户关联表中查出用户关联的所有主播sec_uid,在拿sec_uid去查询主播信息
     * @param anchorUrlUserBo
     * @return
     */
    @PostMapping("/selectAnchorByUserId")
    @Operation(summary = "根据user_id从主播用户关联表中查出用户关联的所有主播sec_uid,在拿sec_uid去查询主播信息")
    public R<PageUtils<AnchorUrlVo>> selectAnchorByUserId(@RequestBody AnchorUrlUserBo anchorUrlUserBo){

        return anchorUrlLogic.selectAnchorByUserId(anchorUrlUserBo);

    }


    /**
     * 删除主播白名单
     * @param anchorUrlWhiteBos
     * @return
     */
    @PostMapping("/removeAnchorWhite")
    @Operation(summary = "删除主播白名单")
    public R<String> removeAnchorWhite(@Parameter(description = "白名单的用户和主播id") @RequestBody List<AnchorUrlWhiteBo> anchorUrlWhiteBos){
        return anchorUrlLogic.removeAnchorWhite(anchorUrlWhiteBos);
    }

    /**
     * 查询该主播的所属用户信息（白名单）
     * @param secUid
     * @return
     */
    @GetMapping("/selectUserByAnchorWhite")
    @Operation(summary = "查询该主播的所属用户信息（白名单）")
    public R<List<UserVo>> selectUserByAnchorWhite(@Parameter(description = "主播唯一标识") @RequestParam(value = "secUid",required = false) String secUid){

        return anchorUrlLogic.selectUserByAnchorWhite(secUid);

    }

    /**
     * 添加主播至用户白名单
     * @param userId
     * @param secUid
     * @return
     */
    @GetMapping("/saveAnchorInUserWhite")
    @Operation(summary = "添加主播至用户白名单")
    public R<String> saveAnchorInUserWhite(@Parameter(description = "主播唯一标识") @RequestParam(value = "userId",required = false) Long userId,
                                           @Parameter(description = "主播唯一标识") @RequestParam(value = "secUid",required = false) String secUid) {

        return anchorUrlLogic.saveAnchorInUserWhite(userId,secUid);

    }


    /**
     * 获取主播url信息
     * @param secUid 主播唯一标识
     * @param liveUrl 主播直播地址
     * @param homeUrl 主播主页地址
     * @return
     */
    @GetMapping("/infoBySecUid")
    @Operation(summary = "主播url信息")
    public R<AnchorUrlInfoVo> infoBySecUid(@Parameter(description = "主播唯一标识") @RequestParam(value = "secUid", required = false) String secUid,
                                           @Parameter(description = "主播直播地址") @RequestParam(value = "liveUrl", required = false) String liveUrl,
                                           @Parameter(description = "主播主页地址") @RequestParam(value = "homeUrl", required = false) String homeUrl){

        return anchorUrlLogic.infoByCondition(secUid, liveUrl, homeUrl);

    }

    /**
     * 只根据SecUid查询主播信息
     * @param secUid
     * @return
     */
    @GetMapping("/infoBySecUidOne")
    @Operation(summary = "主播url信息")
    public R<AnchorUrlInfoVo> infoBySecUidOne(@Parameter(description = "主播唯一标识") @RequestParam(value = "secUid",required = false) String secUid){
        return anchorUrlLogic.infoBySecUidOne(secUid);
    }

    /**
     * 新增主播url
     * @param anchorUrlBo 主播url对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增主播url")
    public R<String> save(@Parameter(description = "主播url对象", required = true) @RequestBody AnchorUrlBo anchorUrlBo){

        return anchorUrlLogic.save(anchorUrlBo);

    }

    /**
     * 批量保存
     * @param anchorUrlBos 主播集合
     * @return
     */
    @PostMapping("/saveBatch")
    @Operation(summary = "批量保存")
    public R<String> saveBatch(@Parameter(description = "主播集合", required = true) @RequestBody List<AnchorUrlBo> anchorUrlBos){

        return anchorUrlLogic.saveBatch(anchorUrlBos);

    }

    /**
     * 根据主播唯一标识集合获取主播
     * @param secUidList 主播唯一标识集合
     * @return
     */
    @PostMapping("/listBySecUids")
    @Operation(summary = "根据主播唯一标识集合获取主播")
    public R<List<AnchorUrlInfoVo>> listBySecUids(@Parameter(description = "主播唯一标识集合", required = true) @RequestBody List<String> secUidList){

        return anchorUrlLogic.listBySecUids(secUidList);
    }

    /**
     * 根据主播唯一标识集合获取主播(只返回有live地址的主播)
     * @param secUidList 主播唯一标识集合
     * @return
     */
    @PostMapping("/listLiveBySecUids")
    @Operation(summary = "根据主播唯一标识集合获取主播")
    public R<List<AnchorUrlInfoVo>> listLiveBySecUids(@Parameter(description = "主播唯一标识集合", required = true) @RequestBody List<String> secUidList){

        return anchorUrlLogic.listLiveBySecUids(secUidList);
    }

    /**
     * 根据userToken获取主播列表
     */
    @GetMapping("/listByUserToken")
    @Operation(summary = "根据userToken获取主播列表")
    public R<List<AnchorClientVo>>listByUserToken(){

        return anchorUrlLogic.listByUserToken();
    }



    /**
     * 新增用户绑定主播
     * @param list
     * @return
     */
    @PostMapping("/saveBatchs")
    @Operation(summary = "新增用户与主播绑定")
    public R<String> saveBatchs(@Parameter(description = "用户与主播绑定", required = true) @RequestBody List<AnchorUrlUserBo> list){
        return anchorUrlLogic.savs(list);
    }




    @GetMapping("/deletBysecuid")
    @Operation(summary = "删除用户与主播绑定关系")
    public void deletBySecUid(@Parameter(description = "删除用户与主播绑定关系", required = true) @RequestParam String secUid){
         anchorUrlLogic.deletBySecUid(secUid);
    }

    /**
     * 服务端获取所以用户所绑定主播列表
     * @param anchorUrlPegBo
     * @return
     */
    @PostMapping("/seletByUserId")
    @Operation(summary = "服务端获取所以用户所绑定主播列表")
    public  R<PageUtils<AnchorUrlVo>> seletByUserId(@Parameter(description = "服务端获取主播列表", required = true) @RequestBody AnchorUrlPegBo anchorUrlPegBo){
        return anchorUrlLogic.seletByUserId(anchorUrlPegBo);

    }

    /**
     * 服务端获取主播列表
     * @param anchorUrlPegBo
     * @return
     */
    @PostMapping("/seletAnchorUrl")
    @Operation(summary = "服务端获取主播列表")
    public  R<PageUtils<AnchorUrlVo>> seletAnchorUrl(@Parameter(description = "服务端获取主播列表", required = true) @RequestBody AnchorUrlPegBo anchorUrlPegBo){
        return anchorUrlLogic.seletAnchorUrl(anchorUrlPegBo);

    }

    /**
     * 客户查询用户是否有录制该主播
     * @param secUid
     * @return
     */
    @PostMapping("/seletBysecUidAnchorUrlWhite")
    @Operation(summary = "客户查询用户是否有录制该主播")
    public R<List<String>> seletBysecUidAnchorUrlWhite(@RequestBody List<String> secUidS){
       return anchorUrlLogic.seletBysecUidAnchorUrlWhite(secUidS);
    }


    /**
     * 客户根据ssecUid查询用户是否有录制该主播
     * @param
     * @return
     */
    @GetMapping("/seletBySerId")
    @Operation(summary = "客户根据secUid查询用户是否有录制该主播")
    public R<Boolean> seletBySerId(@RequestParam String secUidS){
        return anchorUrlLogic.seletBySerId(secUidS);
    }

    /**
     * 服务端主播保存用户的白名单
     */
    @PostMapping("/saveAnchorUrlWhite")
    @Operation(summary = "主播保存白名单")
    public R<String> saveAnchorUrlWhite(@RequestBody AnchorUrlWhiteBo anchorUrlWhiteBo){
       return anchorUrlLogic.saveAnchorUrlWhite(anchorUrlWhiteBo);
    }

    /**
     * 服务端主播列表删除用户白名单
     */
    @PostMapping("/removeAnchorUrlWhite")
    @Operation(summary = "服务端主播列表删除用户白名单")
    public R<String> removeAnchorUrlWhite(@RequestBody AnchorUrlWhiteBo anchorUrlWhiteBo){
        return anchorUrlLogic.removeAnchorUrlWhite(anchorUrlWhiteBo);
    }


    /**
     * 服务端查询查询主播的白名单
     * @param anchorUrlWhiteListBo
     * @return
     */
    @PostMapping("/seletUidAnchorUrlWhite")
    @Operation(summary = "服务端查询查询主播的白名单")
    public R<PageUtils<UserListVo>> seletUidAnchorUrlWhite(@RequestBody AnchorUrlWhiteListBo anchorUrlWhiteListBo){
        return anchorUrlLogic.seletUidAnchorUrlWhite(anchorUrlWhiteListBo);
    }


    // /**
    //  * 服务端用户详情获取所绑定的主播
    //  * @param anchorUrlPegBo
    //  * @return
    //  */
    // @PostMapping("/selet")
    // @Operation(summary = "服务端用户详情获取所绑定的主播")
    // public  R<PageUtils<AnchorUrlVo>> selet(@Parameter(description = "服务端获取主播列表", required = true) @RequestBody AnchorUrlPegBo anchorUrlPegBo){
    //     return anchorUrlLogic.seletByUserId(anchorUrlPegBo);
    //
    // }

    @GetMapping("/openMonitoringPosition")
    @Operation(summary = "新添加的主播后-自动打开监控位")
    public R<OpenMonitoringPositionVo> openMonitoringPosition(String secUid) {
        return anchorUrlLogic.openMonitoringPosition(secUid);
    }

    @GetMapping("/generatedAnchorKeywords")
    @Operation(summary = "生成关键词")
    public R<String> generatedAnchorKeywords(String secUid) {
        anchorUrlLogic.generatedAnchorKeywords(secUid);
        return R.ok();
    }

    /**
     * 获取授权用量信息
     *
     * @param authType 授权类型 1=巨量 / 2=千川 / 3=来客
     * @return 授权用量信息
     */
    @GetMapping("/getAuthUsage")
    @Operation(summary = "获取授权用量信息")
    public R<AuthUsageVo> getAuthUsage(
            @Parameter(description = "授权类型 1=巨量 / 2=千川 / 3=来客", required = true)
            @RequestParam Integer authType) {
        return anchorUrlLogic.getAuthUsage(authType);
    }
}
