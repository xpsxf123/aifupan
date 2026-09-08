package com.jiuyu.governance.business.org.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.base.BaseOrgTest;
import com.jiuyu.governance.business.org.pojo.entity.Position;
import com.jiuyu.governance.business.org.pojo.request.PositionAddRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.PositionResponse;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 职位服务单元测试
 *
 * <p>测试场景覆盖：</p>
 * <ul>
 *     <li>正常场景：新增、修改、删除、查询</li>
 *     <li>异常场景：数据不存在、名称重复、默认岗位保护</li>
 *     <li>非法参数：null 值、空字符串、超长文本</li>
 * </ul>
 *
 * @author HeHui
 * @date 2026-03-27
 */
@DisplayName("职位服务测试")
public class PositionServiceTest extends BaseOrgTest {

    @Autowired
    private PositionService positionService;

    @Autowired
    private com.jiuyu.governance.business.org.mapper.PositionMapper positionMapper;

    /**
     * Mock AccessUser
     */
    private AccessUser mockAccessUser;

    /**
     * 测试前清理脏数据
     */
    @BeforeEach
    public void setUp() {
        // 清理测试数据
        ChainWrappers.lambdaUpdateChain(positionMapper)
            .eq(Position::getTenantId, DEFAULT_TENANT_ID)
            .like(Position::getName, "测试岗位")
            .remove();

        // 创建 Mock AccessUser
        mockAccessUser = Mockito.mock(AccessUser.class);
        Mockito.when(mockAccessUser.currentTenantId()).thenReturn(DEFAULT_TENANT_ID);
        Mockito.when(mockAccessUser.userId()).thenReturn(DEFAULT_USER_ID);
    }

    /**
     * 测试后清理
     */
    @AfterEach
    public void tearDown() {
        // 清理所有测试创建的职位
        ChainWrappers.lambdaUpdateChain(positionMapper)
            .eq(Position::getTenantId, DEFAULT_TENANT_ID)
            .like(Position::getName, "测试岗位")
            .remove();
    }

    // ==================== 正常场景测试 ====================

    @Test
    @DisplayName("1. 新增岗位 - 正常流程")
    public void testAddPosition_Success() {
        // 准备测试数据
        PositionAddRequest request = new PositionAddRequest();
        request.setName("测试岗位_" + System.currentTimeMillis());
        request.setSort(1);

        // 执行测试
        ApiResponse<Void> response = positionService.addPosition(request, mockAccessUser);

        // 验证结果
        assertEquals(200, response.getCode(), "新增成功应该返回 200");

        // 验证数据库是否存在
        List<Position> positions = ChainWrappers.lambdaQueryChain(positionMapper)
            .eq(Position::getTenantId, DEFAULT_TENANT_ID)
            .eq(Position::getName, request.getName())
            .list();
        assertEquals(1, positions.size(), "应该找到刚创建的岗位");
    }

    @Test
    @DisplayName("2. 修改岗位 - 正常流程")
    public void testUpdatePosition_Success() {
        // 先创建一个岗位
        Position position = createTestPosition("原名称_" + System.currentTimeMillis());

        // 准备更新请求
        PositionUpdateRequest request = new PositionUpdateRequest();
        request.setId(position.getId());
        request.setName("新名称_" + System.currentTimeMillis());
        request.setSort(2);

        // 执行更新
        ApiResponse<Void> response = positionService.updatePosition(request, mockAccessUser);

        // 验证结果
        assertEquals(200, response.getCode(), "更新成功应该返回 200");

        // 验证数据库是否已更新
        Position updated = positionMapper.selectById(position.getId());
        assertNotNull(updated, "更新后的岗位应该存在");
        assertEquals(request.getName(), updated.getName(), "岗位名称应该已更新");
        assertEquals(request.getSort(), updated.getSort(), "排序应该已更新");
    }

    @Test
    @DisplayName("3. 删除岗位 - 正常流程")
    public void testDeletePosition_Success() {
        // 先创建一个岗位（非默认岗位）
        Position position = createTestPosition("待删除岗位_" + System.currentTimeMillis());

        // 执行删除
        ApiResponse<Void> response = positionService.deletePosition(position.getId(), mockAccessUser);

        // 验证结果
        assertEquals(200, response.getCode(), "删除成功应该返回 200");

        // 验证数据库是否已删除（逻辑删除）
        Position deleted = positionMapper.selectById(position.getId());
        assertNotNull(deleted, "删除后的岗位记录仍存在（逻辑删除）");
        assertTrue(deleted.getIsDeleted(), "岗位应该被标记为已删除");
    }

    @Test
    @DisplayName("4. 分页查询岗位 - 正常流程")
    public void testPageQueryPosition_Success() {
        // 创建测试数据
        createTestPosition("查询测试岗位 1_" + System.currentTimeMillis());
        createTestPosition("查询测试岗位 2_" + System.currentTimeMillis());

        // 准备查询请求
        PositionPageQueryRequest request = new PositionPageQueryRequest();
        request.setPage(1);
        request.setLimit(10);

        // 执行查询
        PageData<PositionResponse> pageData = positionService.pageQueryPosition(request, mockAccessUser);

        // 验证结果
        assertNotNull(pageData, "分页数据不应为 null");
        assertTrue(pageData.getTotalCount() >= 2, "应该至少查询到 2 条数据");
    }

    @Test
    @DisplayName("5. 获取岗位名称 Map - 正常流程")
    public void testGetPositionNameMap_Success() {
        // 创建测试岗位
        Position position1 = createTestPosition("岗位 1_" + System.currentTimeMillis());
        Position position2 = createTestPosition("岗位 2_" + System.currentTimeMillis());

        // 执行查询
        Map<Long, String> nameMap = positionService.getPositionNameMap(List.of(position1.getId(), position2.getId()));

        // 验证结果
        assertNotNull(nameMap, "名称 Map 不应为 null");
        assertEquals(2, nameMap.size(), "应该返回 2 个岗位的名称");
        assertTrue(nameMap.containsKey(position1.getId()), "应该包含岗位 1 的 ID");
        assertTrue(nameMap.containsKey(position2.getId()), "应该包含岗位 2 的 ID");
    }

    @Test
    @DisplayName("6. 获取岗位选项 - 正常流程")
    public void testGetPositionOptions_Success() {
        // 创建测试岗位
        Position position1 = createTestPosition("岗位选项 1_" + System.currentTimeMillis());
        Position position2 = createTestPosition("岗位选项 2_" + System.currentTimeMillis());

        // 执行查询
        List<LabelOption> options = positionService.getPositionOptions(List.of(position1.getId(), position2.getId()));

        // 验证结果
        assertNotNull(options, "岗位选项列表不应为 null");
        assertEquals(2, options.size(), "应该返回 2 个岗位选项");
        assertTrue(options.stream().anyMatch(o -> o.getKey().equals(position1.getId())), "应该包含岗位 1 的选项");
        assertTrue(options.stream().anyMatch(o -> o.getKey().equals(position2.getId())), "应该包含岗位 2 的选项");
    }

    @Test
    @DisplayName("7. 列出选项 - 正常流程")
    public void testListOptions_Success() {
        // 创建测试岗位
        createTestPosition("列表选项岗位 1_" + System.currentTimeMillis());
        createTestPosition("列表选项岗位 2_" + System.currentTimeMillis());

        // 执行查询
        List<LabelOption> options = positionService.listOptions("列表选项", 10, DEFAULT_TENANT_ID);

        // 验证结果
        assertNotNull(options, "选项列表不应为 null");
        assertTrue(options.size() >= 2, "应该至少返回 2 个选项");
    }

    // ==================== 异常场景测试 ====================

    @Test
    @DisplayName("8. 新增岗位 - 岗位名称重复")
    public void testAddPosition_DuplicateName() {
        // 先创建一个岗位
        String positionName = "重名岗位_" + System.currentTimeMillis();
        createTestPosition(positionName);

        // 尝试创建同名岗位
        PositionAddRequest request = new PositionAddRequest();
        request.setName(positionName);
        request.setSort(2);

        // 执行测试
        ApiResponse<Void> response = positionService.addPosition(request, mockAccessUser);

        // 验证结果
        assertNotEquals(200, response.getCode(), "同名岗位应该失败");
        assertTrue(response.getMsg().contains("岗位名称已存在"), "错误信息应该提示岗位名称已存在");
    }

    @Test
    @DisplayName("9. 修改岗位 - 岗位不存在")
    public void testUpdatePosition_NotFound() {
        // 准备更新请求，使用不存在的 ID
        PositionUpdateRequest request = new PositionUpdateRequest();
        request.setId(999999L);
        request.setName("测试岗位");
        request.setSort(1);

        // 执行更新
        ApiResponse<Void> response = positionService.updatePosition(request, mockAccessUser);

        // 验证结果
        assertNotEquals(200, response.getCode(), "更新不存在的岗位应该失败");
        assertTrue(response.getMsg().contains("岗位不存在"), "错误信息应该提示岗位不存在");
    }

    @Test
    @DisplayName("10. 删除岗位 - 岗位不存在")
    public void testDeletePosition_NotFound() {
        // 执行删除，使用不存在的 ID
        ApiResponse<Void> response = positionService.deletePosition(999999L, mockAccessUser);

        // 验证结果
        assertNotEquals(200, response.getCode(), "删除不存在的岗位应该失败");
        assertTrue(response.getMsg().contains("岗位不存在"), "错误信息应该提示岗位不存在");
    }

    @Test
    @DisplayName("11. 删除岗位 - 默认岗位不可删除")
    public void testDeletePosition_DefaultPosition() {
        // 创建一个默认岗位
        Position position = createTestPosition("默认岗位_" + System.currentTimeMillis());
        position.setIsDefault(true);
        positionMapper.updateById(position);

        // 尝试删除默认岗位
        ApiResponse<Void> response = positionService.deletePosition(position.getId(), mockAccessUser);

        // 验证结果
        assertNotEquals(200, response.getCode(), "删除默认岗位应该失败");
        assertTrue(response.getMsg().contains("默认岗位不可删除"), "错误信息应该提示默认岗位不可删除");
    }

    @Test
    @DisplayName("12. 修改岗位 - 名称重复")
    public void testUpdatePosition_DuplicateName() {
        // 创建两个岗位
        Position position1 = createTestPosition("岗位 A_" + System.currentTimeMillis());
        Position position2 = createTestPosition("岗位 B_" + System.currentTimeMillis());

        // 尝试将 position2 的名称改为 position1 的名称
        PositionUpdateRequest request = new PositionUpdateRequest();
        request.setId(position2.getId());
        request.setName(position1.getName());
        request.setSort(2);

        // 执行更新
        ApiResponse<Void> response = positionService.updatePosition(request, mockAccessUser);

        // 验证结果
        assertNotEquals(200, response.getCode(), "更新为已存在的名称应该失败");
        assertTrue(response.getMsg().contains("岗位名称已存在"), "错误信息应该提示岗位名称已存在");
    }

    // ==================== 非法参数测试 ====================

    @Test
    @DisplayName("13. 新增岗位 - 岗位名称为空")
    public void testAddPosition_NullName() {
        // 准备请求，名称为 null
        PositionAddRequest request = new PositionAddRequest();
        request.setName(null);
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            positionService.addPosition(request, mockAccessUser);
        }, "岗位名称为 null 应该抛出异常");
    }

    @Test
    @DisplayName("14. 新增岗位 - 岗位名称为空字符串")
    public void testAddPosition_EmptyName() {
        // 准备请求，名称为空字符串
        PositionAddRequest request = new PositionAddRequest();
        request.setName("");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            positionService.addPosition(request, mockAccessUser);
        }, "岗位名称为空字符串应该抛出异常");
    }

    @Test
    @DisplayName("15. 新增岗位 - 岗位名称超长")
    public void testAddPosition_NameTooLong() {
        // 准备请求，名称超长（超过 100 字符）
        PositionAddRequest request = new PositionAddRequest();
        request.setName("这是一个非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的岗位名称超过了一百字符限制");
        request.setSort(1);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            positionService.addPosition(request, mockAccessUser);
        }, "岗位名称超长应该抛出异常");
    }

    @Test
    @DisplayName("16. 新增岗位 - 排序为 null")
    public void testAddPosition_NullSort() {
        // 准备请求，排序为 null
        PositionAddRequest request = new PositionAddRequest();
        request.setName("测试岗位_" + System.currentTimeMillis());
        request.setSort(null);

        // 执行测试（应该抛出验证异常）
        assertThrows(Exception.class, () -> {
            positionService.addPosition(request, mockAccessUser);
        }, "排序为 null 应该抛出异常");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试岗位
     *
     * @param name 岗位名称
     * @return 创建的岗位实体
     */
    private Position createTestPosition(String name) {
        Position position = new Position();
        position.setId(IdUtil.getSnowflakeNextId());
        position.setTenantId(DEFAULT_TENANT_ID);
        position.setName(name);
        position.setPositionCode("");
        position.setSort(1);
        position.setIsDefault(false);
        position.setCreateBy(DEFAULT_USER_ID);
        position.setUpdateBy(DEFAULT_USER_ID);
        position.setCreateDate(java.time.LocalDateTime.now());
        position.setUpdateDate(java.time.LocalDateTime.now());
        position.setIsDeleted(false);

        positionMapper.insert(position);
        return position;
    }
}
