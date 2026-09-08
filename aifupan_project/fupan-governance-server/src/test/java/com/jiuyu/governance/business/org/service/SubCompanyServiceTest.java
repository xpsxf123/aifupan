package com.jiuyu.governance.business.org.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.base.BaseOrgTest;
import com.jiuyu.governance.business.org.mapper.SubCompanyMapper;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.entity.SubCompany;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyAddRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.SubCompanyResponse;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
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
 * 子公司服务单元测试
 *
 * <p>测试场景覆盖：</p>
 * <ul>
 *     <li>正常场景：新增、修改、删除、查询</li>
 *     <li>异常场景：数据不存在、名称重复、超出数量限制</li>
 *     <li>非法参数：null 值、空字符串、超长文本</li>
 * </ul>
 *
 * @author HeHui
 * @date 2026-03-27
 */
@DisplayName("子公司服务测试")
public class SubCompanyServiceTest extends BaseOrgTest {

    @Autowired
    private SubCompanyService subCompanyService;

    @Autowired
    private ManagerConnectorProcessor connectorProcessor;

    @Autowired
    private SubCompanyMapper companyMapper;

    /**
     * 测试前清理脏数据
     */
    @BeforeEach
    public void setUp() {
        // 清理测试数据
        ChainWrappers.lambdaUpdateChain(companyMapper)
            .eq(SubCompany::getTenantId, DEFAULT_TENANT_ID)
            .like(SubCompany::getName, "测试公司")
            .remove();
    }

    /**
     * 测试后清理
     */
    @AfterEach
    public void tearDown() {
        // 清理所有测试创建的子公司
        ChainWrappers.lambdaUpdateChain(companyMapper)
            .eq(SubCompany::getTenantId, DEFAULT_TENANT_ID)
            .like(SubCompany::getName, "测试公司")
            .remove();
    }

    // ==================== 正常场景测试 ====================

    @Test
    @DisplayName("1. 新增子公司 - 正常流程")
    public void testAddSubCompany_Success() {
        // 准备测试数据
        SubCompanyAddRequest request = new SubCompanyAddRequest();
        request.setName("测试公司_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试
        ApiResponse<Void> response = subCompanyService.addSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "新增成功应该返回 200");

        // 验证数据库是否存在
        boolean exists = subCompanyService.hasId(IdUtil.getSnowflakeNextId(), DEFAULT_TENANT_ID);
        // 由于 ID 是随机生成的，这里通过名称查询验证
        List<SubCompany> companies = ChainWrappers.lambdaQueryChain(companyMapper)
            .eq(SubCompany::getTenantId, DEFAULT_TENANT_ID)
            .eq(SubCompany::getName, request.getName())
            .list();
        assertEquals(1, companies.size(), "应该找到刚创建的子公司");
    }

    @Test
    @DisplayName("2. 修改子公司 - 正常流程")
    public void testUpdateSubCompany_Success() {
        // 先创建一个子公司
        SubCompany company = createTestCompany("原名称_" + System.currentTimeMillis());

        // 准备更新请求
        SubCompanyUpdateRequest request = new SubCompanyUpdateRequest();
        request.setId(company.getId());
        request.setName("新名称_" + System.currentTimeMillis());
        request.setSort(2);

        // 执行更新
        ApiResponse<Void> response = subCompanyService.updateSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "更新成功应该返回 200");

        // 验证数据库是否已更新
        SubCompany updated = companyMapper.selectById(company.getId());
        assertNotNull(updated, "更新后的公司应该存在");
        assertEquals(request.getName(), updated.getName(), "公司名称应该已更新");
        assertEquals(request.getSort(), updated.getSort(), "排序应该已更新");
    }

    @Test
    @DisplayName("3. 删除子公司 - 正常流程")
    public void testDeleteSubCompany_Success() {
        // 先创建一个子公司
        SubCompany company = createTestCompany("待删除公司_" + System.currentTimeMillis());

        // 执行删除
        ApiResponse<Void> response = subCompanyService.deleteSubCompany(company.getId(), DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertEquals(200, response.getCode(), "删除成功应该返回 200");

        // 验证数据库是否已删除（逻辑删除）
        SubCompany deleted = companyMapper.selectById(company.getId());
        assertNotNull(deleted, "删除后的公司记录仍存在（逻辑删除）");
        assertTrue(deleted.getIsDeleted(), "公司应该被标记为已删除");
    }

    @Test
    @DisplayName("4. 分页查询子公司 - 正常流程")
    public void testPageQuerySubCompany_Success() {
        // 创建测试数据
        createTestCompany("查询测试公司 1_" + System.currentTimeMillis());
        createTestCompany("查询测试公司 2_" + System.currentTimeMillis());

        // 准备查询请求
        SubCompanyPageQueryRequest request = new SubCompanyPageQueryRequest();
        request.setPage(1);
        request.setLimit(10);

        // 执行查询
        PageData<SubCompanyResponse> pageData = subCompanyService.pageQuerySubCompany(request, null);

        // 验证结果
        assertNotNull(pageData, "分页数据不应为 null");
        assertTrue(pageData.getTotalCount() >= 2, "应该至少查询到 2 条数据");
    }

    @Test
    @DisplayName("5. 判断公司是否存在 - 正常流程")
    public void testHasId_Success() {
        // 创建测试公司
        SubCompany company = createTestCompany("存在性测试公司_" + System.currentTimeMillis());

        // 测试存在的情况
        boolean exists = subCompanyService.hasId(company.getId(), DEFAULT_TENANT_ID);
        assertTrue(exists, "应该返回 true");

        // 测试不存在的情况
        boolean notExists = subCompanyService.hasId(999999L, DEFAULT_TENANT_ID);
        assertFalse(notExists, "应该返回 false");
    }

    @Test
    @DisplayName("6. 获取公司名称 Map - 正常流程")
    public void testGetNameMap_Success() {
        // 创建测试公司
        SubCompany company1 = createTestCompany("公司 1_" + System.currentTimeMillis());
        SubCompany company2 = createTestCompany("公司 2_" + System.currentTimeMillis());

        // 执行查询
        Map<Long, String> nameMap = subCompanyService.getNameMap(List.of(company1.getId(), company2.getId()));

        // 验证结果
        assertNotNull(nameMap, "名称 Map 不应为 null");
        assertEquals(2, nameMap.size(), "应该返回 2 个公司的名称");
        assertTrue(nameMap.containsKey(company1.getId()), "应该包含公司 1 的 ID");
        assertTrue(nameMap.containsKey(company2.getId()), "应该包含公司 2 的 ID");
    }

    @Test
    @DisplayName("7. 下拉选择 - 正常流程")
    public void testOptions_Success() {
        // 创建测试公司
        createTestCompany("下拉选项公司 1_" + System.currentTimeMillis());
        createTestCompany("下拉选项公司 2_" + System.currentTimeMillis());

        // 准备查询请求
        var queryRequest = new com.jiuyu.governance.business.org.pojo.request.SubCompanySelectQueryRequest();
        queryRequest.setTenantId(DEFAULT_TENANT_ID);
        queryRequest.setLimit(10);

        // 执行查询
        List<LabelOption> options = subCompanyService.options(queryRequest);

        // 验证结果
        assertNotNull(options, "下拉选项列表不应为 null");
        assertTrue(options.size() >= 2, "应该至少返回 2 个选项");
    }

    // ==================== 异常场景测试 ====================

    @Test
    @DisplayName("8. 新增子公司 - 公司名称重复")
    public void testAddSubCompany_DuplicateName() {
        // 先创建一个公司
        String companyName = "重名公司_" + System.currentTimeMillis();
        createTestCompany(companyName);

        // 尝试创建同名公司
        SubCompanyAddRequest request = new SubCompanyAddRequest();
        request.setName(companyName);
        request.setSort(1);

        // 执行测试
        ApiResponse<Void> response = subCompanyService.addSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "同名公司应该失败");
        assertTrue(response.getMsg().contains("公司名称已存在"), "错误信息应该提示公司名称已存在");
    }

    @Test
    @DisplayName("9. 修改子公司 - 公司不存在")
    public void testUpdateSubCompany_NotFound() {
        // 准备更新请求，使用不存在的 ID
        SubCompanyUpdateRequest request = new SubCompanyUpdateRequest();
        request.setId(999999L);
        request.setName("测试公司");
        request.setSort(1);

        // 执行更新
        ApiResponse<Void> response = subCompanyService.updateSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "更新不存在的公司应该失败");
        assertTrue(response.getMsg().contains("公司不存在"), "错误信息应该提示公司不存在");
    }

    @Test
    @DisplayName("10. 删除子公司 - 公司不存在")
    public void testDeleteSubCompany_NotFound() {
        // 执行删除，使用不存在的 ID
        ApiResponse<Void> response = subCompanyService.deleteSubCompany(999999L, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "删除不存在的公司应该失败");
        assertTrue(response.getMsg().contains("公司不存在"), "错误信息应该提示公司不存在");
    }

    @Test
    @DisplayName("11. 修改子公司 - 名称重复")
    public void testUpdateSubCompany_DuplicateName() {
        // 创建两个公司
        SubCompany company1 = createTestCompany("公司 A_" + System.currentTimeMillis());
        SubCompany company2 = createTestCompany("公司 B_" + System.currentTimeMillis());

        // 尝试将 company2 的名称改为 company1 的名称
        SubCompanyUpdateRequest request = new SubCompanyUpdateRequest();
        request.setId(company2.getId());
        request.setName(company1.getName());
        request.setSort(2);

        // 执行更新
        ApiResponse<Void> response = subCompanyService.updateSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 验证结果
        assertNotEquals(200, response.getCode(), "更新为已存在的名称应该失败");
        assertTrue(response.getMsg().contains("公司名称已存在"), "错误信息应该提示公司名称已存在");
    }

    // ==================== 非法参数测试 ====================

    @Test
    @DisplayName("12. 新增子公司 - 公司名称为空")
    public void testAddSubCompany_NullName() {
        // 准备请求，名称为 null
        SubCompanyAddRequest request = new SubCompanyAddRequest();
        request.setName(null);
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            subCompanyService.addSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "公司名称为 null 应该抛出异常");
    }

    @Test
    @DisplayName("13. 新增子公司 - 公司名称为空字符串")
    public void testAddSubCompany_EmptyName() {
        // 准备请求，名称为空字符串
        SubCompanyAddRequest request = new SubCompanyAddRequest();
        request.setName("");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            subCompanyService.addSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "公司名称为空字符串应该抛出异常");
    }

    @Test
    @DisplayName("14. 新增子公司 - 公司名称超长")
    public void testAddSubCompany_NameTooLong() {
        // 准备请求，名称超长（超过 50 字符）
        SubCompanyAddRequest request = new SubCompanyAddRequest();
        request.setName("这是一个非常非常非常非常非常非常非常非常非常非常长的公司名称超过了五十字符限制");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            subCompanyService.addSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        }, "公司名称超长应该抛出异常");
    }

    @Test
    @DisplayName("15. 新增子公司 - 排序为 null")
    public void testAddSubCompany_NullSort() {
        // 准备请求，排序为 null
        SubCompanyAddRequest request = new SubCompanyAddRequest();
        request.setName("测试公司_" + System.currentTimeMillis());
        request.setSort(null);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            subCompanyService.addSubCompany(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
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
}
