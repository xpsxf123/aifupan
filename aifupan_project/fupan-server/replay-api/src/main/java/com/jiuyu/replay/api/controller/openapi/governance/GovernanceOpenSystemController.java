package com.jiuyu.replay.api.controller.openapi.governance;

import com.jiuyu.replay.api.interceptor.FeatureSignature;
import com.jiuyu.replay.api.logic.common.SystemKvLogic;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：lujie
 * @date ：2026/5/11 19:35
 */
@FeatureSignature(client = "governance")
@RestController
@RequestMapping("/replay/openapi/governance/system")
public class GovernanceOpenSystemController {

    @Resource
    private SystemKvLogic systemKvLogic;

    @GetMapping("/getByKey")
    @Operation(summary = "根据key获取系统配置的键值对信息")
    public R<SystemKvInfoVo> getByKey(@Parameter(description = "系统配置的键值对key", required = true) @RequestParam("key") String key) {
        return systemKvLogic.getByKey(key);
    }
}
