package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.PackageUserBll;
import com.jiuyu.replay.order.bo.PackageUserAddsBo;
import com.jiuyu.replay.order.bo.PackageUserListBo;
import com.jiuyu.replay.order.vo.PackageUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自定义版本用户关联
 */
@Slf4j
@RestController
@CrossOrigin
@RequestMapping("replay/packageUser")
@Tag(name = "自定义版本用户关联")
@AllArgsConstructor
public class PackageUserController {

    private final PackageUserBll packageUserBll;

    @PostMapping("/queryPage")
    @Operation(summary = "自定义版本用户分页查询")
    public R<PageUtils<PackageUserVo>> queryPage(@RequestBody @Validated PackageUserListBo listBo) {
        return packageUserBll.queryPage(listBo);
    }

    @PostMapping("/adds")
    @Operation(summary = "批量添加自定义版本用户")
    public R<String> adds(@RequestBody @Validated PackageUserAddsBo addsBo) {
        return R.ok(packageUserBll.adds(addsBo));
    }

    @PostMapping("/deleteByIds")
    @Operation(summary = "根据id集合删除自定义版本用户")
    public R<String> deleteByIds(@RequestBody @NotEmpty(message = "id集合不能为空") @Schema(description = "自定义版本用户id") List<Long> ids) {
        packageUserBll.deleteByIds(ids);
        return R.ok();
    }
}
