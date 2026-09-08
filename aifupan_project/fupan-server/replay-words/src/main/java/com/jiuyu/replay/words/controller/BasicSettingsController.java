package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.BasicSettingsInfoVo;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;
import com.jiuyu.replay.words.bll.BasicSettingsBll;
import com.jiuyu.replay.words.bo.BasicSettingsBo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/10 14:36
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("replay/words/basicSettings")
@Tag(name = "基础设置的控制器")
public class BasicSettingsController {

    private final BasicSettingsBll basicSettingsBll;
    private final UserFeign userFeign;

    @PostMapping("/updateAiPartialNew")
    @Operation(summary = "更新ai页面的部分主播字段")
    public R<BasicSettingsInfoVo> updateAiPartialNew(@RequestBody @Validated BasicSettingsBo basicSettingsBo) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        basicSettingsBo.setUserId(user.getId());
        basicSettingsBo.setTenantId(user.getActiveTenantId());
        return R.ok(basicSettingsBll.updateAiPartialNew(basicSettingsBo));
    }

    @GetMapping("/getAiPartial")
    @Operation(summary = "获取ai页面的部分主播字段")
    public R<BasicSettingsVo> getAiPartial(String sourceId, Integer sourceType) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        return R.ok(basicSettingsBll.getAiPartial(sourceId, sourceType, user.getId(), user.getActiveTenantId()));
    }

}
