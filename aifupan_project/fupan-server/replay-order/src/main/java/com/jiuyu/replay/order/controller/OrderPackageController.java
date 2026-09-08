package com.jiuyu.replay.order.controller;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/3/23 14:57
 */

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.PackageBll;
import com.jiuyu.replay.order.vo.PackageListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/8/6 下午3:55
 */
@Slf4j
@RestController
@RequestMapping("replay/order/package")
@Tag(name = "版本相关的控制器")
@AllArgsConstructor
public class OrderPackageController {

    private final PackageBll packageBll;

    /**
     * 用户版本列表-用户可以购买的版本
     *
     * @param userId 用户id
     * @return 用户版本列表-用户可以购买的版本
     */
    @GetMapping("/listPackageByUser")
    @Operation(summary = "用户版本列表-用户可以购买的版本")
    public R<List<PackageListVo>> listPackageByUser(Long userId) {
        return packageBll.listPackageByUser(userId);
    }
}
