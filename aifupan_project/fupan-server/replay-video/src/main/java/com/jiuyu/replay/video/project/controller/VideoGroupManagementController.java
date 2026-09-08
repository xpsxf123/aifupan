package com.jiuyu.replay.video.project.controller;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.video.project.bo.group.GroupAddBo;
import com.jiuyu.replay.video.project.bo.group.GroupEditBo;
import com.jiuyu.replay.video.project.bo.group.GroupQueryBo;
import com.jiuyu.replay.video.project.producer.VideoGroupManagementProducer;
import com.jiuyu.replay.video.project.vo.group.GroupVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分组管理控制器
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 通用分组管理接口控制器，支持达人分组和爆款订阅分组管理
 */
@Slf4j
@RestController
@RequestMapping("/replay/video/group")
@RequiredArgsConstructor
@Validated
@Tag(name = "V2.5.3短视频/分组管理模块", description = "通用分组管理相关接口")
public class VideoGroupManagementController {

    private final VideoGroupManagementProducer videoGroupManagementProducer;

    @PostMapping("/list")
    @Operation(summary = "WEB端-查询分组列表", description = "查询分组列表，支持分页和条件筛选")
    public R<PageUtils<GroupVo>> getGroupList(@RequestBody @Validated GroupQueryBo queryBo) {
        return R.ok(videoGroupManagementProducer.getGroupList(queryBo));
    }

    @GetMapping("/options")
    @Operation(
            summary = "WEB端-获取分组选项",
            description = """
                    **获取指定类型的分组选项列表，用于下拉选择**
                    
                    ## 📝 功能说明
                    - 返回简化的分组信息，适用于下拉选择框
                    - 默认分组始终排在第一位
                    - 按照sort_order和创建时间排序
                    
                    ## 🔒 权限控制
                    - 用户隔离：只能获取自己的分组选项
                    - 租户隔离：同租户下的分组选项
                    
                    ## 📤 返回字段
                    - groupId: 分组ID（默认分组为null）
                    - groupName: 分组名称
                    - groupType: 分组类型
                    - groupTypeName: 分组类型名称
                    """
    )
    public R<List<GroupVo>> getGroupOptions(
            @Parameter(
                    description = "分组类型：1-达人订阅分组，2-爆款订阅分组",
                    required = true,
                    example = "1"
            )
            @RequestParam Byte groupType) {
        return R.ok(videoGroupManagementProducer.getGroupOptions(groupType));
    }

    @PostMapping
    @Operation(
            summary = "WEB端-添加分组",
            description = """
                    **添加新的分组**
                    
                    ## 📝 功能说明
                    - 支持创建达人订阅分组和爆款订阅分组
                    - 自动检查分组名称重复
                    - 支持设置分组描述和排序
                    
                    ## 🔒 权限控制
                    - 用户隔离：只能创建自己的分组
                    - 租户隔离：同租户下的分组管理
                    """
    )
    public R<Boolean> addGroup(@Validated @RequestBody GroupAddBo addBo) {
        return R.ok(videoGroupManagementProducer.addGroup(addBo));
    }

    @PutMapping
    @Operation(
            summary = "WEB端-编辑分组",
            description = """
                    **编辑分组信息**
                    
                    ## 📝 功能说明
                    - 支持修改分组名称、描述和排序
                    - 自动检查分组名称重复（排除自己）
                    - 只能编辑自己创建的分组
                    
                    ## 🔒 权限控制
                    - 用户隔离：只能编辑自己的分组
                    - 租户隔离：同租户下的分组管理
                    
                    ## ⚠️ 注意事项
                    - 分组名称在同类型下不能重复
                    - 分组必须存在且有权限操作
                    """
    )
    public R<Boolean> editGroup(@Validated @RequestBody GroupEditBo editBo) {
        return R.ok(videoGroupManagementProducer.editGroup(editBo));
    }

    @DeleteMapping("/{groupId}")
    @Operation(
            summary = "WEB端-删除分组",
            description = """
                    **删除指定的分组**
                    
                    ## 📝 功能说明
                    - 逻辑删除分组（设置is_deleted=1）
                    - 检查分组下是否有成员
                    - 只能删除自己创建的分组
                    
                    ## 🔒 权限控制
                    - 用户隔离：只能删除自己的分组
                    - 租户隔离：同租户下的分组管理
                    
                    ## ⚠️ 注意事项
                    - 分组下有成员时无法删除
                    - 分组必须存在且有权限操作
                    - 删除后无法恢复
                    """
    )
    public R<Boolean> deleteGroup(@PathVariable Long groupId) {
        return R.ok(videoGroupManagementProducer.deleteGroup(groupId));
    }

    @GetMapping("/{groupId}")
    @Operation(
            summary = "WEB端-获取分组详情",
            description = """
                    **获取指定分组的详细信息**
                    
                    ## 📝 功能说明
                    - 获取分组的完整信息，包括成员数量
                    - 自动统计分组下的成员数量
                    - 只能查看自己创建的分组
                    
                    ## 🔒 权限控制
                    - 用户隔离：只能查看自己的分组
                    - 租户隔离：同租户下的分组管理
                    
                    ## 📤 返回信息
                    - 分组基本信息（ID、名称、类型、描述等）
                    - 成员数量统计
                    - 创建时间和创建者信息
                    
                    ## ⚠️ 注意事项
                    - 分组必须存在且有权限访问
                    - 成员数量实时统计
                    """
    )
    public R<GroupVo> getGroupDetail(
            @Parameter(
                    description = "分组ID",
                    required = true,
                    example = "1001"
            )
            @PathVariable Long groupId) {
        return R.ok(videoGroupManagementProducer.getGroupDetail(groupId));
    }
}
