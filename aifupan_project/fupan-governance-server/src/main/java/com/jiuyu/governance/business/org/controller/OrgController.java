package com.jiuyu.governance.business.org.controller;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.pojo.bo.DeptInfo;
import com.jiuyu.governance.business.org.pojo.bo.TeamInfo;
import com.jiuyu.governance.business.org.pojo.request.DeptSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanySelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.response.OrgTreeResponse;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSearchQueryRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomOrgResponse;
import com.jiuyu.governance.business.room.service.LiveRoomService;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.plugins.oauth.data.supports.PermissionContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 企业端 - 组织API
 *
 * @author HeHui
 * @date 2026-03-26 10:15
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/org")
@RequiredArgsConstructor
public class OrgController {

    private final SubCompanyService companyService;

    private final DeptService deptService;

    private final TeamService teamService;

    private final LiveRoomService liveRoomService;


    /**
     * 获取组织架构树
     * <p>
     * 构建公司 -> 部门 -> 小组的三级组织架构树形结构，用于前端展示完整的组织架构。
     *
     * <h3>注意事项：</h3>
     * <ul>
     *     <li>查询会自动应用数据权限过滤（通过 {@code AccessUser} 中的权限信息）</li>
     *     <li>部门和小组的查询使用了排除逻辑，避免同一层级数据重复</li>
     *     <li>所有层级都按 {@code sort} 字段进行升序排序</li>
     *     <li>如果某一层级没有数据，不会影响到上层节点的返回</li>
     *     <li>前端对接时需注意处理各级 {@code children} 可能为 null 的情况</li>
     * </ul>
     * @param level 层级，1-公司，2-部门及上，3-小组及上
     * @param accessUser 访问用户
     * @return 组织架构树形结构列表，包含公司、部门、小组的层级关系
     */
    @GetMapping("/tree")
    public ApiResponse<List<OrgTreeResponse>> tree(@RequestParam(required = false, defaultValue = "3") Integer level, AccessUser accessUser) {

        List<Long> roomTeamIds = new ArrayList<>();
        List<Long> roomDeptIds = new ArrayList<>();
        List<Long> roomCompanyIds = new ArrayList<>();

        if (level >= 4) {
            LiveRoomSearchQueryRequest queryRequest = new LiveRoomSearchQueryRequest();
            queryRequest.setTenantId(accessUser.currentTenantId());
            queryRequest.setLimit(300);
            queryRequest.setCurrentUserId(accessUser.userId());
            List<LiveRoomOrgResponse> liveRoomOrg = liveRoomService.getLiveRoomOrg(queryRequest);
            if (EmptyUtil.isNotEmpty(liveRoomOrg)) {
                liveRoomOrg.forEach(roomOrg -> {
                    if (roomOrg.getCompanyId() != null && !roomCompanyIds.contains(roomOrg.getCompanyId())) {
                        roomCompanyIds.add(roomOrg.getCompanyId());
                    }
                    if (roomOrg.getDeptId() != null && roomOrg.getDeptId() > 0L && !roomDeptIds.contains(roomOrg.getDeptId())) {
                        roomDeptIds.add(roomOrg.getDeptId());
                    }
                    if (roomOrg.getTeamId() != null && roomOrg.getTeamId() > 0L && !roomTeamIds.contains(roomOrg.getTeamId())) {
                        roomTeamIds.add(roomOrg.getTeamId());
                    }
                });
            }
        }

        Map<Long, List<TeamInfo>> deptTeamMap = Map.of();
        if (level >= 3) {
            // 注意必须清除权限上下文
            PermissionContextHolder.clearAll();
            TeamSelectQueryRequest teamSelectQueryRequest = new TeamSelectQueryRequest();
            teamSelectQueryRequest.setLimit(1000);
            teamSelectQueryRequest.setCurrentUserId(accessUser.userId());
            teamSelectQueryRequest.setTenantId(accessUser.currentTenantId());
            if (EmptyUtil.isEmpty(roomTeamIds)) {
                // 部门的小组
                deptTeamMap = teamService.getTeamList(teamSelectQueryRequest).stream().collect(Collectors.groupingBy(TeamInfo::getDeptId));
            } else {
                teamSelectQueryRequest.setExcludeIds(roomTeamIds);
                deptTeamMap = Stream.concat(teamService.getTeamList(teamSelectQueryRequest).stream(), teamService.getTeamInfoList(roomTeamIds).stream()).collect(Collectors.groupingBy(TeamInfo::getDeptId));
            }
        }
        // 公司的部门
        Map<Long, List<DeptInfo>> companyDeptMap = Map.of();
        if (level >= 2) {
            // 注意必须清除权限上下文
            PermissionContextHolder.clearAll();
            DeptSelectQueryRequest deptSelectQueryRequest = new DeptSelectQueryRequest();
            deptSelectQueryRequest.setLimit(500);
            deptSelectQueryRequest.setCurrentUserId(accessUser.userId());
            deptSelectQueryRequest.setTenantId(accessUser.currentTenantId());
            if (EmptyUtil.isEmpty(deptTeamMap)) {
                deptSelectQueryRequest.setExcludeIds(roomDeptIds);
                // 查询受我管理
                companyDeptMap = Stream.concat(deptService.getDeptInfoList(deptSelectQueryRequest).stream(), deptService.getDeptInfoList(roomDeptIds).stream()).collect(Collectors.groupingBy(DeptInfo::getCompanyId));
            } else {
                // 查询受我管理的部门 + 我管理小组的上级部门
                List<Long> excludeIds = new ArrayList<>();
                excludeIds.addAll(roomDeptIds);
                excludeIds.addAll(deptTeamMap.keySet());
                deptSelectQueryRequest.setExcludeIds(excludeIds);
                companyDeptMap = Stream.concat(deptService.getDeptInfoList(deptSelectQueryRequest).stream(), deptService.getDeptInfoList(excludeIds).stream()).collect(Collectors.groupingBy(DeptInfo::getCompanyId));
            }
        }
        PermissionContextHolder.clearAll();
        // 公司信息
        List<LabelOption> companyList = null;
        SubCompanySelectQueryRequest companySelectQueryRequest = new SubCompanySelectQueryRequest();
        companySelectQueryRequest.setLimit(300);
        companySelectQueryRequest.setTenantId(accessUser.currentTenantId());
        if (EmptyUtil.isEmpty(companyDeptMap)) {
            companySelectQueryRequest.setExcludeIds(roomCompanyIds);
            // 查询受我管理
            companyList = Stream.concat(companyService.options(companySelectQueryRequest).stream(), companyService.getCompanyList(roomCompanyIds).stream()).toList();
        } else {
            // 查询受我管理的公司 + 我管理部门的上级公司
            List<Long> excludeIds = new ArrayList<>();
            excludeIds.addAll(roomCompanyIds);
            excludeIds.addAll(companyDeptMap.keySet());
            companySelectQueryRequest.setExcludeIds(excludeIds);
            companyList = Stream.concat(companyService.options(companySelectQueryRequest).stream(), companyService.getCompanyList(excludeIds).stream()).toList();
        }

        // 构建组织架构树：公司 -> 部门 -> 小组
        List<OrgTreeResponse> orgTreeResponses = new ArrayList<>();
        for (LabelOption company : companyList) {
            OrgTreeResponse companyTree = new OrgTreeResponse();
            companyTree.setKey(company.getKey());
            companyTree.setLabel(company.getLabel());

            // 获取当前公司下的所有部门
            List<DeptInfo> deptInfos = companyDeptMap.get(company.getKey());
            if (EmptyUtil.isNotEmpty(deptInfos)) {
                List<OrgTreeResponse.DeptOption> deptOptions = new ArrayList<>();
                // 按排序字段对部门进行升序排序
                deptInfos.sort(Comparator.comparingInt(DeptInfo::getSort));
                for (DeptInfo deptInfo : deptInfos) {
                    OrgTreeResponse.DeptOption deptOption = new OrgTreeResponse.DeptOption();
                    deptOption.setKey(deptInfo.getId());
                    deptOption.setLabel(deptInfo.getName());
                    deptOptions.add(deptOption);

                    // 获取当前部门下的所有小组
                    List<TeamInfo> teamInfos = deptTeamMap.get(deptInfo.getId());
                    if (EmptyUtil.isNotEmpty(teamInfos)) {
                        // 按排序字段对小组进行升序排序
                        teamInfos.sort(Comparator.comparingInt(TeamInfo::getSort));
                        // 将小组转换为 LabelOption 并设置为部门的子节点
                        deptOption.setChildren(teamInfos.stream().map(teamInfo -> {
                            LabelOption teamOption = new LabelOption();
                            teamOption.setKey(teamInfo.getId());
                            teamOption.setLabel(teamInfo.getName());
                            return teamOption;
                        }).toList());
                    }
                }
                // 将部门列表设置为公司的子节点
                companyTree.setChildren(deptOptions);
            }
            orgTreeResponses.add(companyTree);
        }

        return ApiResponse.success(orgTreeResponses);

    }
}
