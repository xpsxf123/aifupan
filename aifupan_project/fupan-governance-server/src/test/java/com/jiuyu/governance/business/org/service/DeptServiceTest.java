package com.jiuyu.governance.business.org.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.base.BaseOrgTest;
import com.jiuyu.governance.business.org.mapper.DeptMapper;
import com.jiuyu.governance.business.org.mapper.SubCompanyMapper;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.SubCompany;
import com.jiuyu.governance.business.org.pojo.request.DeptAddRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.DeptResponse;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 部门服务单元测试
 *
 * <p>测试场景覆盖：</p>
 * <ul>
 *     <li>正常场景：新增、修改、删除、查询</li>
 *     <li>异常场景：数据不存在、名称重复、超出数量限制、关联数据检查</li>
 *     <li>非法参数：null 值、空字符串、超长文本</li>
 * </ul>
 *
 * @author HeHui
 * @date 2026-03-27
 */
@DisplayName("部门服务测试")
public class DeptServiceTest extends BaseOrgTest {

    @Autowired
    private DeptService deptService;

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private SubCompanyService subCompanyService;

    @Autowired
    private SubCompanyMapper companyMapper;

    /**
     * 测试前清理脏数据
     */
    @BeforeEach
    public void setUp() {
        // 清理测试数据
        ChainWrappers.lambdaUpdateChain(deptMapper)
            .eq(Dept::getTenantId, DEFAULT_TENANT_ID)
            .like(Dept::getName, "测试部门")
            .remove();
    }

    /**
     * 测试后清理
     */
    @AfterEach
    public void tearDown() {
        // 清理所有测试创建的部门
        ChainWrappers.lambdaUpdateChain(deptMapper)
            .eq(Dept::getTenantId, DEFAULT_TENANT_ID)
            .like(Dept::getName, "测试部门")
            .remove();
    }

    // ==================== 正常场景测试 ====================

    @Test
    @DisplayName("1. 新增部门 - 正常流程")
    public void testAddDept_Success() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 准备测试数据
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(company.getId());
        request.setName("测试部门_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试
        ApiResponse<Void> response = deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "新增成功应该返回 200");

        // 验证数据库是否存在
        List<Dept> depts = ChainWrappers.lambdaQueryChain(deptMapper)
            .eq(Dept::getTenantId, DEFAULT_TENANT_ID)
            .eq(Dept::getName, request.getName())
            .list();
        assertEquals(1, depts.size(), "应该找到刚创建的部门");
    }

    @Test
    @DisplayName("2. 修改部门 - 正常流程")
    void testUpdateDept_Success() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "原名称_" + System.currentTimeMillis());

        // 准备更新请求
        DeptUpdateRequest request = new DeptUpdateRequest();
        request.setId(dept.getId());
        request.setCompanyId(company.getId());
        request.setName("新名称_" + System.currentTimeMillis());
        request.setSort(2);

        // 执行更新
        ApiResponse<Void> response = deptService.updateDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "更新成功应该返回 200");

        // 验证数据库是否已更新
        Optional<Dept> updated = deptService.findDept(dept.getId(), DEFAULT_TENANT_ID);
        assertTrue(updated.isPresent(), "更新后的部门应该存在");
        assertEquals(request.getName(), updated.get().getName(), "部门名称应该已更新");
        assertEquals(request.getSort(), updated.get().getSort(), "排序应该已更新");
    }

    @Test
    @DisplayName("3. 删除部门 - 正常流程")
    void testDeleteDept_Success() {
        // 先创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "待删除部门_" + System.currentTimeMillis());

        // 执行删除
        ApiResponse<Void> response = deptService.deleteDept(dept.getId(), DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "删除成功应该返回 200");

        // 验证数据库是否已删除（逻辑删除）
        Optional<Dept> deleted = deptService.findDept(dept.getId(), DEFAULT_TENANT_ID);
        assertFalse(deleted.isPresent(), "部门应该被删除或标记为已删除");
    }

    @Test
    @DisplayName("4. 分页查询部门 - 正常流程")
    void testPageQueryDept_Success() {
        // 创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 创建测试数据
        createTestDept(company.getId(), "查询测试部门 1_" + System.currentTimeMillis());
        createTestDept(company.getId(), "查询测试部门 2_" + System.currentTimeMillis());

        // 准备查询请求
        DeptPageQueryRequest request = new DeptPageQueryRequest();
        request.setTenantId(DEFAULT_TENANT_ID);
        request.setCompanyId(company.getId());
        request.setPage(1);
        request.setLimit(10);

        // 执行查询
        PageData<DeptResponse> pageData = deptService.pageQueryDept(request, null);

        // 验证结果
        assertNotNull(pageData, "分页数据不应为 null");
        assertTrue(pageData.getTotalCount() >= 2, "应该至少查询到 2 条数据");
    }

    @Test
    @DisplayName("5. 判断部门是否存在 - 正常流程")
    void testHasId_Success() {
        // 创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "存在性测试部门_" + System.currentTimeMillis());

        // 测试存在的情况
        boolean exists = deptService.hasId(dept.getId(), DEFAULT_TENANT_ID);
        assertTrue(exists, "应该返回 true");

        // 测试不存在的情况
        boolean notExists = deptService.hasId(999999L, DEFAULT_TENANT_ID);
        assertFalse(notExists, "应该返回 false");
    }

    @Test
    @DisplayName("6. 根据 ID 查询部门 - 正常流程")
    void testFindDept_Success() {
        // 创建公司和部门
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());
        Dept dept = createTestDept(company.getId(), "查询测试部门_" + System.currentTimeMillis());

        // 执行查询
        Optional<Dept> result = deptService.findDept(dept.getId(), DEFAULT_TENANT_ID);

        // 验证结果
        assertTrue(result.isPresent(), "应该查询到部门");
        assertEquals(dept.getId(), result.get().getId(), "部门 ID 应该匹配");
        assertEquals(dept.getName(), result.get().getName(), "部门名称应该匹配");
    }

    @Test
    @DisplayName("7. 获取部门名称 Map - 正常流程")
    void testGetDeptNameMap_Success() {
        // 创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 创建测试部门
        Dept dept1 = createTestDept(company.getId(), "部门 1_" + System.currentTimeMillis());
        Dept dept2 = createTestDept(company.getId(), "部门 2_" + System.currentTimeMillis());

        // 执行查询
        Map<Long, String> nameMap = deptService.getDeptNameMap(List.of(dept1.getId(), dept2.getId()));

        // 验证结果
        assertNotNull(nameMap, "名称 Map 不应为 null");
        assertEquals(2, nameMap.size(), "应该返回 2 个部门的名称");
        assertTrue(nameMap.containsKey(dept1.getId()), "应该包含部门 1 的 ID");
        assertTrue(nameMap.containsKey(dept2.getId()), "应该包含部门 2 的 ID");
    }

    @Test
    @DisplayName("8. 部门下拉选择 - 正常流程")
    void testOptions_Success() {
        // 创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 创建测试部门
        createTestDept(company.getId(), "下拉选项部门 1_" + System.currentTimeMillis());
        createTestDept(company.getId(), "下拉选项部门 2_" + System.currentTimeMillis());

        // 准备查询请求
        DeptSelectQueryRequest queryRequest = new DeptSelectQueryRequest();
        queryRequest.setTenantId(DEFAULT_TENANT_ID);
        queryRequest.setCompanyId(company.getId());
        queryRequest.setLimit(10);

        // 执行查询
        List<LabelOption> options = deptService.options(queryRequest);

        // 验证结果
        assertNotNull(options, "下拉选项列表不应为 null");
        assertTrue(options.size() >= 2, "应该至少返回 2 个选项");
    }

    // ==================== 异常场景测试 ====================

    @Test
    @DisplayName("9. 新增部门 - 公司不存在")
    void testAddDept_CompanyNotFound() {
        // 准备请求，使用不存在的公司 ID
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(999999L);
        request.setName("测试部门_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试
        ApiResponse<Void> response = deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "公司不存在应该失败");
        assertTrue(response.getMsg().contains("所属公司不存在"), "错误信息应该提示公司不存在");
    }

    @Test
    @DisplayName("10. 新增部门 - 部门名称重复")
    void testAddDept_DuplicateName() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 创建一个部门
        String deptName = "重名部门_" + System.currentTimeMillis();
        createTestDept(company.getId(), deptName);

        // 尝试创建同名部门
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(company.getId());
        request.setName(deptName);
        request.setSort(2);

        // 执行测试
        ApiResponse<Void> response = deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "同名部门应该失败");
        assertTrue(response.getMsg().contains("同公司下部门名称已存在"), "错误信息应该提示名称已存在");
    }

    @Test
    @DisplayName("11. 修改部门 - 部门不存在")
    void testUpdateDept_NotFound() {
        // 准备更新请求，使用不存在的 ID
        DeptUpdateRequest request = new DeptUpdateRequest();
        request.setId(999999L);
        request.setCompanyId(1L);
        request.setName("测试部门");
        request.setSort(1);

        // 执行更新
        ApiResponse<Void> response = deptService.updateDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "更新不存在的部门应该失败");
        assertTrue(response.getMsg().contains("部门不存在"), "错误信息应该提示部门不存在");
    }

    @Test
    @DisplayName("12. 删除部门 - 部门不存在")
    void testDeleteDept_NotFound() {
        // 执行删除，使用不存在的 ID
        ApiResponse<Void> response = deptService.deleteDept(999999L, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "删除不存在的部门应该失败");
        assertTrue(response.getMsg().contains("部门不存在"), "错误信息应该提示部门不存在");
    }

    @Test
    @DisplayName("13. 删除部门 - 部门下存在员工")
    void testDeleteDept_HasEmployees() {
        // TODO: 需要先创建员工并关联到部门
        // 这个测试需要 rbac 模块的支持，暂时跳过
        // 实际使用时应该先创建员工数据
    }

    @Test
    @DisplayName("14. 删除部门 - 部门下存在小组")
    void testDeleteDept_HasTeams() {
        // TODO: 需要先创建小组并关联到部门
        // 这个测试需要 room 模块的支持，暂时跳过
        // 实际使用时应该先创建小组数据
    }

    // ==================== 非法参数测试 ====================

    @Test
    @DisplayName("15. 新增部门 - 公司名称为空")
    void testAddDept_NullName() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 准备请求，名称为 null
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(company.getId());
        request.setName(null);
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "部门名称为 null 应该抛出异常");
    }

    @Test
    @DisplayName("16. 新增部门 - 公司名称为空字符串")
    void testAddDept_EmptyName() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 准备请求，名称为空字符串
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(company.getId());
        request.setName("");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "部门名称为空字符串应该抛出异常");
    }

    @Test
    @DisplayName("17. 新增部门 - 公司名称超长")
    void testAddDept_NameTooLong() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 准备请求，名称超长（超过 40 字符）
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(company.getId());
        request.setName("这是一个非常非常非常非常非常非常非常非常长的部门名称超过了四十字符限制");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "部门名称超长应该抛出异常");
    }

    @Test
    @DisplayName("18. 新增部门 - 公司为 null")
    void testAddDept_NullCompanyId() {
        // 准备请求，公司为 null
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(null);
        request.setName("测试部门_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "公司为 null 应该抛出异常");
    }

    @Test
    @DisplayName("19. 新增部门 - 排序为 null")
    void testAddDept_NullSort() {
        // 先创建公司
        SubCompany company = createTestCompany("测试公司_" + System.currentTimeMillis());

        // 准备请求，排序为 null
        DeptAddRequest request = new DeptAddRequest();
        request.setCompanyId(company.getId());
        request.setName("测试部门_" + System.currentTimeMillis());
        request.setSort(null);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            deptService.addDept(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
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
}
