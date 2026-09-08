package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.TenantLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.TenantBo;
import com.jiuyu.replay.power.bo.TenantListBo;
import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantOptionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@RestController
@CrossOrigin
@RequestMapping("power/tenant")
@Tag(name = "租户")
public class TenantController {

    @Resource
    private TenantLogic tenantLogic;

    /**
     * 租户列表
     * @param tenantListBo 租户列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "租户列表")
    public R<PageUtils<TenantListVo>> list(@Parameter(description = "租户列表查询参数", required = true) @RequestBody TenantListBo tenantListBo){

        return tenantLogic.queryPage(tenantListBo);
    }


    /**
     * 租户信息
     * @param id 租户id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "租户信息")
    public R<TenantInfoVo> info(@Parameter(description = "租户id", required = true) @RequestParam("id") Long id){

        return tenantLogic.info(id);
    }

    /**
     * 新增租户
     * @param tenantBo 租户对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增租户")
    public R<String> save(@Parameter(description = "租户对象", required = true) @RequestBody TenantBo tenantBo){

        return tenantLogic.save(tenantBo);
    }

    /**
     * 修改租户
     * @param tenantBo 租户对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改租户")
    public R<String> update(@Parameter(description = "租户对象", required = true) @RequestBody TenantBo tenantBo){

        return tenantLogic.update(tenantBo);
    }

    /**
     * 删除租户
     * @param id 租户id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除租户")
    public R<String> delete(@Parameter(description = "租户id", required = true) @RequestParam("id") Long id){
        return tenantLogic.delete(id);
    }

    /**
     * 租户下拉选项
     *
     * @param tenantSearchBO 租户搜索查询参数
     *
     * @return {@link R }<{@link List }<{@link TenantOptionVo }>>
     */
    @GetMapping("/select-options")
    public R<List<TenantOptionVo>> selectOptions(TenantSearchBO tenantSearchBO){
        return R.ok(tenantLogic.selectOptions(tenantSearchBO));
    }

    /**
     * 分页查询普通用户的租户列表
     *
     * @param bo 查询参数
     * @return {@link R }<{@link PageUtils }<{@link TenantListVo }>>
     */
    @PostMapping("/normalUserTenantList")
    @Operation(summary = "分页查询普通用户的租户列表")
    public R<PageUtils<TenantListVo>> normalUserTenantList(@Parameter(description = "查询参数", required = true) @RequestBody TenantNormalUserListBo bo) {
        return tenantLogic.pageNormalUserTenantList(bo);
    }

}
