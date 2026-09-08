package com.jiuyu.governance.business.org.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.base.BaseOrgTest;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.SubCompany;
import com.jiuyu.governance.business.org.pojo.entity.Team;
import com.jiuyu.governance.business.org.pojo.request.TeamAddRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.TeamResponse;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 团队服务单元测试
 *
 * <p>测试场景覆盖：</p>
 * <ul>
 *     <li>正常场景：新增、修改、删除、查询</li>
 *     <li>异常场景：数据不存在、名称重复、部门不存在</li>
 *     <li>非法参数：null 值、空字符串、超长文本</li>
 * </ul>
 *
 * @author HeHui
 * @date 2026-03-27
 */
@DisplayName("团队服务测试")
public class TeamServiceTest extends BaseOrgTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private SubCompanyService subCompanyService;

    @Autowired
    private DeptService deptService;

    @Autowired
    private com.jiuyu.governance.business.org.mapper.TeamMapper teamMapper;

    @Autowired
    private com.jiuyu.governance.business.org.mapper.DeptMapper deptMapper;

    @Autowired
    private com.jiuyu.governance.business.org.mapper.SubCompanyMapper companyMapper;

    /**
     * 测试前清理脏数据
     */
    @BeforeEach
    public void setUp() {
        // 清理测试数据
        ChainWrappers.lambdaUpdateChain(teamMapper)
            .eq(Team::getTenantId, DEFAULT_TENANT_ID)
            .like(Team::getName, "测试小组")
            .remove();
    }

    /**
     * 测试后清理
     */
    @AfterEach
    public void tearDown() {
        // 清理所有测试创建的团队
        ChainWrappers.lambdaUpdateChain(teamMapper)
            .eq(Team::getTenantId, DEFAULT_TENANT_ID)
            .like(Team::getName, "测试小组")
            .remove();
    }

    // ==================== 正常场景测试 ====================

    @Test
    @DisplayName("1. 新增小组 - 正常流程")
    public void testAddTeam_Success() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 准备测试数据
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(dept.getId());
        request.setName("测试小组_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试
        ApiResponse<Void> response = teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "新增成功应该返回 200");

        // 验证数据库是否存在
        List<Team> teams = ChainWrappers.lambdaQueryChain(teamMapper)
            .eq(Team::getTenantId, DEFAULT_TENANT_ID)
            .eq(Team::getName, request.getName())
            .list();
        assertEquals(1, teams.size(), "应该找到刚创建的小组");
    }

    @Test
    @DisplayName("2. 修改小组 - 正常流程")
    public void testUpdateTeam_Success() {
        // 先创建公司、部门和小组
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());
        Team team = createTestTeam(company.getId(), dept.getId(), "原名称_" + System.currentTimeMillis());

        // 准备更新请求
        TeamUpdateRequest request = new TeamUpdateRequest();
        request.setId(team.getId());
        request.setDeptId(dept.getId());
        request.setName("新名称_" + System.currentTimeMillis());
        request.setSort(2);

        // 执行更新
        ApiResponse<Void> response = teamService.updateTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "更新成功应该返回 200");

        // 验证数据库是否已更新
        Team updated = teamMapper.selectById(team.getId());
        assertNotNull(updated, "更新后的小组应该存在");
        assertEquals(request.getName(), updated.getName(), "小组名称应该已更新");
        assertEquals(request.getSort(), updated.getSort(), "排序应该已更新");
    }

    @Test
    @DisplayName("3. 删除小组 - 正常流程")
    public void testDeleteTeam_Success() {
        // 先创建公司、部门和小组
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());
        Team team = createTestTeam(company.getId(), dept.getId(), "待删除小组_" + System.currentTimeMillis());

        // 执行删除
        ApiResponse<Void> response = teamService.deleteTeam(team.getId(), DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "删除成功应该返回 200");

        // 验证数据库是否已删除（逻辑删除）
        Team deleted = teamMapper.selectById(team.getId());
        assertNotNull(deleted, "删除后的小组记录仍存在（逻辑删除）");
        assertTrue(deleted.getIsDeleted(), "小组应该被标记为已删除");
    }

    @Test
    @DisplayName("4. 分页查询小组 - 正常流程")
    public void testPageQueryTeam_Success() {
        // 创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 创建测试数据
        createTestTeam(company.getId(), dept.getId(), "查询测试小组 1_" + System.currentTimeMillis());
        createTestTeam(company.getId(), dept.getId(), "查询测试小组 2_" + System.currentTimeMillis());

        // 准备查询请求
        TeamPageQueryRequest request = new TeamPageQueryRequest();
        request.setTenantId(DEFAULT_TENANT_ID);
        request.setCompanyId(company.getId());
        request.setDeptId(dept.getId());
        request.setDeptIds(List.of(dept.getId()));
        request.setPage(1);
        request.setLimit(10);

        // 执行查询
        PageData<TeamResponse> pageData = teamService.pageQueryTeam(request, null);

        // 验证结果
        assertNotNull(pageData, "分页数据不应为 null");
        assertTrue(pageData.getTotalCount() >= 2, "应该至少查询到 2 条数据");
    }

    @Test
    @DisplayName("5. 获取小组名称 Map - 正常流程")
    public void testGetTeamNameMap_Success() {
        // 创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 创建测试小组
        Team team1 = createTestTeam(company.getId(), dept.getId(), "小组 1_" + System.currentTimeMillis());
        Team team2 = createTestTeam(company.getId(), dept.getId(), "小组 2_" + System.currentTimeMillis());

        // 执行查询
        Map<Long, String> nameMap = teamService.getTeamNameMap(List.of(team1.getId(), team2.getId()));

        // 验证结果
        assertNotNull(nameMap, "名称 Map 不应为 null");
        assertEquals(2, nameMap.size(), "应该返回 2 个小组的名称");
        assertTrue(nameMap.containsKey(team1.getId()), "应该包含小组 1 的 ID");
        assertTrue(nameMap.containsKey(team2.getId()), "应该包含小组 2 的 ID");
    }

    @Test
    @DisplayName("6. 小组下拉选择 - 正常流程")
    public void testOptions_Success() {
        // 创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 创建测试小组
        createTestTeam(company.getId(), dept.getId(), "下拉选项小组 1_" + System.currentTimeMillis());
        createTestTeam(company.getId(), dept.getId(), "下拉选项小组 2_" + System.currentTimeMillis());

        // 准备查询请求
        TeamSelectQueryRequest queryRequest = new TeamSelectQueryRequest();
        queryRequest.setTenantId(DEFAULT_TENANT_ID);
        queryRequest.setDeptId(dept.getId());
        queryRequest.setLimit(10);

        // 执行查询
        List<LabelOption> options = teamService.options(queryRequest);

        // 验证结果
        assertNotNull(options, "下拉选项列表不应为 null");
        assertTrue(options.size() >= 2, "应该至少返回 2 个选项");
    }

    // ==================== 异常场景测试 ====================

    @Test
    @DisplayName("7. 新增小组 - 部门不存在")
    public void testAddTeam_DeptNotFound() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 准备请求，使用不存在的部门 ID
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(999999L);
        request.setName("测试小组_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试
        ApiResponse<Void> response = teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "部门不存在应该失败");
        assertTrue(response.getMsg().contains("部门不存在"), "错误信息应该提示部门不存在");
    }

    @Test
    @DisplayName("8. 新增小组 - 小组名称重复")
    public void testAddTeam_DuplicateName() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 创建一个小组
        String teamName = "重名小组_" + System.currentTimeMillis();
        createTestTeam(company.getId(), dept.getId(), teamName);

        // 尝试创建同名小组
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(dept.getId());
        request.setName(teamName);
        request.setSort(2);

        // 执行测试
        ApiResponse<Void> response = teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "同名小组应该失败");
        assertTrue(response.getMsg().contains("同部门下小组名称已存在"), "错误信息应该提示名称已存在");
    }

    @Test
    @DisplayName("9. 修改小组 - 小组不存在")
    public void testUpdateTeam_NotFound() {
        // 准备更新请求，使用不存在的 ID
        TeamUpdateRequest request = new TeamUpdateRequest();
        request.setId(999999L);
        request.setDeptId(1L);
        request.setName("测试小组");
        request.setSort(1);

        // 执行更新
        ApiResponse<Void> response = teamService.updateTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "更新不存在的小组应该失败");
        assertTrue(response.getMsg().contains("小组不存在"), "错误信息应该提示小组不存在");
    }

    @Test
    @DisplayName("10. 删除小组 - 小组不存在")
    public void testDeleteTeam_NotFound() {
        // 执行删除，使用不存在的 ID
        ApiResponse<Void> response = teamService.deleteTeam(999999L, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "删除不存在的小组应该失败");
        assertTrue(response.getMsg().contains("小组不存在"), "错误信息应该提示小组不存在");
    }

    // ==================== 非法参数测试 ====================

    @Test
    @DisplayName("11. 新增小组 - 小组名称为空")
    public void testAddTeam_NullName() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 准备请求，名称为 null
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(dept.getId());
        request.setName(null);
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "小组名称为 null 应该抛出异常");
    }

    @Test
    @DisplayName("12. 新增小组 - 小组名称为空字符串")
    public void testAddTeam_EmptyName() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 准备请求，名称为空字符串
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(dept.getId());
        request.setName("");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "小组名称为空字符串应该抛出异常");
    }

    @Test
    @DisplayName("13. 新增小组 - 公司名称超长")
    public void testAddTeam_NameTooLong() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 准备请求，名称超长（超过 40 字符）
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(dept.getId());
        request.setName("这是一个非常非常非常非常非常非常非常非常长的小组名称超过了四十字符限制");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "小组名称超长应该抛出异常");
    }

    @Test
    @DisplayName("14. 新增小组 - 公司为 null")
    public void testAddTeam_NullCompanyId() {
        // 先创建部门
        Dept dept = createTestDept(1L, "测试部门_" + System.currentTimeMillis());

        // 准备请求，公司为 null
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(dept.getId());
        request.setName("测试小组_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "公司为 null 应该抛出异常");
    }

    @Test
    @DisplayName("15. 新增小组 - 部门为 null")
    public void testAddTeam_NullDeptId() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 准备请求，部门为 null
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(null);
        request.setName("测试小组_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "部门为 null 应该抛出异常");
    }

    @Test
    @DisplayName("16. 新增小组 - 排序为 null")
    public void testAddTeam_NullSort() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "测试部门_" + System.currentTimeMillis());

        // 准备请求，排序为 null
        TeamAddRequest request = new TeamAddRequest();
        request.setDeptId(dept.getId());
        request.setName("测试小组_" + System.currentTimeMillis());
        request.setSort(null);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            teamService.addTeam(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "排序为 null 应该抛出异常");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试公司
     *
     * @param name 公司名称
     * @return 创建的公司实体
     */
    private SubCompany createTestCompany(String name) {
        SubCompany company = new SubCompany();
        company.setId(IdUtil.getSnowflakeNextId());
        company.setTenantId(DEFAULT_TENANT_ID);
        company.setName(name);
        company.setSort(1);
        company.setCreateBy(DEFAULT_USER_ID);
        company.setUpdateBy(DEFAULT_USER_ID);
        company.setCreateDate(LocalDateTime.now());
        company.setUpdateDate(LocalDateTime.now());
        company.setIsDeleted(false);

        companyMapper.insert(company);
        return company;
    }

    /**
     * 创建测试部门
     *
     * @param companyId 公司 ID
     * @param name 部门名称
     * @return 创建的部门实体
     */
    private Dept createTestDept(Long companyId, String name) {
        Dept dept = new Dept();
        dept.setId(IdUtil.getSnowflakeNextId());
        dept.setTenantId(DEFAULT_TENANT_ID);
        dept.setCompanyId(companyId);
        dept.setName(name);
        dept.setSort(1);
        dept.setCreateBy(DEFAULT_USER_ID);
        dept.setUpdateBy(DEFAULT_USER_ID);
        dept.setCreateDate(LocalDateTime.now());
        dept.setUpdateDate(LocalDateTime.now());
        dept.setIsDeleted(false);

        deptMapper.insert(dept);
        return dept;
    }

    /**
     * 创建测试小组
     *
     * @param companyId 公司 ID
     * @param deptId 部门 ID
     * @param name 小组名称
     * @return 创建的小组实体
     */
    private Team createTestTeam(Long companyId, Long deptId, String name) {
        Team team = new Team();
        team.setId(IdUtil.getSnowflakeNextId());
        team.setTenantId(DEFAULT_TENANT_ID);
        team.setCompanyId(companyId);
        team.setDeptId(deptId);
        team.setName(name);
        team.setSort(1);
        team.setCreateBy(DEFAULT_USER_ID);
        team.setUpdateBy(DEFAULT_USER_ID);
        team.setCreateDate(LocalDateTime.now());
        team.setUpdateDate(LocalDateTime.now());
        team.setIsDeleted(false);

        teamMapper.insert(team);
        return team;
    }
}
