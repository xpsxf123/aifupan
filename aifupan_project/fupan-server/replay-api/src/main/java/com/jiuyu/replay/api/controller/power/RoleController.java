package com.jiuyu.replay.api.controller.power;


import com.jiuyu.replay.api.logic.power.RoleLogic;
import com.jiuyu.replay.power.bo.RoleInfoBo;
import com.jiuyu.replay.power.bo.RoleListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.RoleInfoVo;
import com.jiuyu.replay.power.vo.RoleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;




/**
 * 角色
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@RestController
@CrossOrigin
@RequestMapping("replay/role")
@Tag(name = "角色")
public class RoleController {

    @Resource
    private RoleLogic roleLogic;

    /**
     * 角色列表
     * @param roleListBo 菜单列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "角色列表")
    public R<PageUtils<RoleVo>> list(@Parameter(description = "列表查询参数", required = true) @RequestBody RoleListBo roleListBo){

        return roleLogic.queryPage(roleListBo);
    }


    /**
     * 信息
     * @param id 角色id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "信息")
    public R<RoleInfoVo> info(@Parameter(description = "id", required = true) @RequestParam("id") Long id){

        return roleLogic.info(id);
    }

    /**
     * 保存
     * @param role 数据对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "保存")
    public R<String> save(@Parameter(description = "数据对象", required = true) @RequestBody RoleInfoBo role){

        return this.roleLogic.save(role);

    }

    /**
     * 修改
     * @param role 数据对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改")
    public R<String> update(@Parameter(description = "数据对象", required = true) @RequestBody RoleInfoBo role){

        return this.roleLogic.modify(role);

    }

    /**
     * 删除
     * @param id 角色id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除")
    public R<String> delete(@Parameter(description = "id", required = true) @RequestParam("id") Long id){

        return this.roleLogic.delete(id);

    }

}
