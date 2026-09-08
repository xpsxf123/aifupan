package com.jiuyu.replay.words.controller;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.replay.generic.bo.words.anchor.SetAnchorTradeBo;
import com.jiuyu.replay.generic.bo.words.anchor.SubAnchorListBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorBaseInfoBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorUserBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorAuthStatusVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.BasicSettingsBll;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.bo.video.UpdateAiPartialBo;
import com.jiuyu.replay.words.vo.video.UpdateAiPartialVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/7/3 下午5:41
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("replay/words/anchorUrl")
@Tag(name = "主播相关的控制器")
public class AnchorUrlWordsController {

    private final AnchorUrlBll anchorUrlBll;
    private final UserFeign userFeign;
    private final BasicSettingsBll basicSettingsBll;
    private final AnchorUrlUserService anchorUrlUserService;


    @GetMapping("getCurrentMonitoringPosition")
    @Schema(description = "获取登录用户主播列表的全部监控位")
    public R<List<AnchorUrlUserVo>> getCurrentMonitoringPosition() {
        return R.ok(anchorUrlBll.getCurrentMonitoringPosition());
    }

    /**
     * 获取子账号主播列表
     *
     * @param subAnchorListBo 查询参数
     * @return
     */
    @PostMapping("/getSubUserAnchorList")
    @Operation(summary = "根据子用户id获取子用户的主播列表")
    public R<PageUtils<AnchorUrlUserVo>> getSubUserAnchorList(@RequestBody SubAnchorListBo subAnchorListBo) {
        return anchorUrlBll.getSubUserAnchorList(subAnchorListBo);
    }

    @PostMapping("/updateAiPartial")
    @Operation(summary = "更新ai页面的部分主播字段")
    public R<Boolean> updateAiPartial(@RequestBody @Validated({UpdateAiPartialBo.update.class}) UpdateAiPartialBo updateAiPartialBo) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        updateAiPartialBo.setUserId(user.getId());
        updateAiPartialBo.setTenantId(user.getActiveTenantId());
        return R.ok(anchorUrlBll.updateAiPartial(updateAiPartialBo));
    }

    @PostMapping("/updateAiPartialNew")
    @Operation(summary = "更新ai页面的部分主播字段")
    public R<UpdateAiPartialVo> updateAiPartialNew(@RequestBody @Validated({UpdateAiPartialBo.updateNew.class}) UpdateAiPartialBo updateAiPartialBo) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        updateAiPartialBo.setUserId(user.getId());
        updateAiPartialBo.setTenantId(user.getActiveTenantId());
        Boolean res = anchorUrlBll.updateAiPartialNew(updateAiPartialBo);
        UpdateAiPartialBo aiPartial = basicSettingsBll.getAiPartial(updateAiPartialBo.getVideoId());
        UpdateAiPartialVo result = BeanUtil.copyProperties(aiPartial, UpdateAiPartialVo.class);
        result.setIsUpdate(res);
        return R.ok(result);
    }

    @GetMapping("/getAiPartial")
    @Operation(summary = "获取ai页面的部分主播字段")
    public R<UpdateAiPartialBo> getAiPartial(String videoId) {
        return R.ok(basicSettingsBll.getAiPartial(videoId));
    }

    /**
     * 彻底删除主播接口
     *
     * @param secUid
     * @return
     */
    @DeleteMapping("/thoroughlyDelete")
    @Operation(summary = "删除主播")
    public R<Boolean> thoroughlyDeleteAnchor(@RequestParam("secUid") @NotBlank(message = "主播secUid不能为空") String secUid) {
        return R.ok(anchorUrlBll.thoroughlyDeleteAnchor(secUid));
    }

    /**
     * 重新添加主播
     *
     * @param secUid
     * @return
     */
    @PutMapping("/againAdd")
    @Operation(summary = "重新添加主播")
    public R<Boolean> againAddAnchor(@RequestParam("secUid") @NotBlank(message = "主播secUid不能为空") String secUid) {
        return R.ok(anchorUrlBll.againAddAnchor(secUid));
    }

    /**
     * 更新主播基础信息
     *
     * @param updateAnchorBaseInfoBo 主播基础信息
     * @return
     */
    @PostMapping("/updateAnchorBaseInfo")
    @Operation(summary = "更新主播基础信息")
    public R<String> updateAnchorBaseInfo(@RequestBody UpdateAnchorBaseInfoBo updateAnchorBaseInfoBo) {

        return anchorUrlBll.updateAnchorBaseInfo(updateAnchorBaseInfoBo);
    }

    /**
     * 更新用户主播信息
     *
     * @param updateAnchorUserBo 用户主播基础信息
     * @return
     */
    @PostMapping("/updateUserAnchorInfo")
    @Operation(summary = "更新用户主播信息")
    public R<String> updateUserAnchorInfo(@RequestBody @Validated UpdateAnchorUserBo updateAnchorUserBo) {

        return anchorUrlBll.updateUserAnchorInfo(updateAnchorUserBo);
    }

    /**
     * 根据主播抖音号获取主播信息
     *
     * @param anchorNumber 主播抖音号
     * @return 主播信息
     */
    @GetMapping("/getByAnchorNumber")
    @Operation(summary = "根据主播抖音号获取主播信息")
    public R<AnchorUrlInfoVo> getByAnchorNumber(@Parameter(description = "主播抖音号", required = true) @RequestParam @NotBlank(message = "主播抖音号不能为空") String anchorNumber) {
        return R.ok(anchorUrlBll.infoByAnchorNumber(anchorNumber));
    }

    /**
     * 获取主播授权状态
     *
     * @param secUid 主播secUid
     * @return 授权状态信息
     */
    @GetMapping("/getAuthStatus")
    @Operation(summary = "获取主播授权状态")
    public R<AnchorAuthStatusVo> getAuthStatus(@RequestParam @Parameter(description = "主播secUid") @NotBlank(message = "主播secUid不能为空") String secUid) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        AnchorAuthStatusVo vo = anchorUrlUserService.getAuthStatusBySecUid(secUid, user.getId(), user.getActiveTenantId());
        if (vo == null) {
            return R.error(30000, "记录不存在");
        }
        return R.ok(vo);
    }

    /**
     * 设置主播行业
     *
     * @param setAnchorTradeBo 设置主播行业参数
     * @return 结果
     */
    @PostMapping("/setAnchorTrade")
    @Operation(summary = "设置主播行业")
    public R<String> setAnchorTrade(@RequestBody @Validated SetAnchorTradeBo setAnchorTradeBo) {
        return anchorUrlBll.setAnchorTrade(setAnchorTradeBo);
    }

}
