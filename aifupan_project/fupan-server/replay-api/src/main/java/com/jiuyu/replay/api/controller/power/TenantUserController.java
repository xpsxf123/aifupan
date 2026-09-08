package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.TenantUserLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.TenantUserBo;
import com.jiuyu.replay.power.bo.TenantUserListBo;
import com.jiuyu.replay.power.vo.TenantUserInfoVo;
import com.jiuyu.replay.power.vo.TenantUserListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 租户-用户-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@RestController
@CrossOrigin
@RequestMapping("power/tenantuser")
@Tag(name = "租户-用户-关联表")
public class TenantUserController {

    @Resource
    private TenantUserLogic tenantUserLogic;

    /**
     * 租户-用户-关联表列表
     * @param tenantUserListBo 租户-用户-关联表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "租户-用户-关联表列表")
    public R<PageUtils<TenantUserListVo>> list(@Parameter(description = "租户-用户-关联表列表查询参数", required = true) @RequestBody TenantUserListBo tenantUserListBo){

        return tenantUserLogic.queryPage(tenantUserListBo);
    }


    /**
     * 租户-用户-关联表信息
     * @param id 租户-用户-关联表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "租户-用户-关联表信息")
    public R<TenantUserInfoVo> info(@Parameter(description = "租户-用户-关联表id", required = true) @RequestParam("id") Long id){

        return tenantUserLogic.info(id);
    }

    /**
     * 新增租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增租户-用户-关联表")
    public R<String> save(@Parameter(description = "租户-用户-关联表对象", required = true) @RequestBody TenantUserBo tenantUserBo){

        return tenantUserLogic.save(tenantUserBo);
    }

    /**
     * 修改租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改租户-用户-关联表")
    public R<String> update(@Parameter(description = "租户-用户-关联表对象", required = true) @RequestBody TenantUserBo tenantUserBo){

        return tenantUserLogic.update(tenantUserBo);
    }

    /**
     * 删除租户-用户-关联表
     * @param id 租户-用户-关联表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除租户-用户-关联表")
    public R<String> delete(@Parameter(description = "租户-用户-关联表id", required = true) @RequestParam("id") Long id){

        return tenantUserLogic.delete(id);
    }

}
