package com.jiuyu.governance.business.room.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.SubCompany;
import com.jiuyu.governance.business.org.pojo.entity.Team;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.room.base.BaseRoomTest;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.mapper.LiveRoomScheduleAttributeMapper;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoomScheduleAttribute;
import com.jiuyu.governance.business.room.pojo.request.*;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomScheduleAttributeResponse;
import com.jiuyu.governance.common.pojo.bo.IdName;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 直播间管理服务测试
 *
 * <p>测试范围：</p>
 * <ul>
 *     <li>直播间 CRUD 操作</li>
 *     <li>直播间状态管理</li>
 *     <li>分页查询与搜索</li>
 *     <li>排班配置管理</li>
 * </ul>
 *
 * @author HeHui
 * @date 2026-03-27
 */
@Slf4j
class LiveRoomServiceTest extends BaseRoomTest {

    @Autowired
    private LiveRoomService liveRoomService;

    @Autowired
    private LiveRoomMapper liveRoomMapper;

    @Autowired
    private LiveRoomScheduleAttributeMapper scheduleAttributeMapper;

    @Autowired
    private SubCompanyService companyService;

    @Autowired
    private DeptService deptService;

    @Autowired
    private TeamService teamService;

    private Long createdLiveRoomId;

    @BeforeEach
    public void setUp() {
        log.info("========== LiveRoomServiceTest 开始执行 ==========");
    }

    /**
     * 测试新增直播间 - 成功场景
     */
    @Test
    void testAddLiveRoom_Success() {
        log.info("【测试新增直播间】开始");

        // 1. 准备测试数据
        LiveRoomAddRequest request = buildLiveRoomAddRequest();

        // 2. 执行测试
        ApiResponse<Void> response = liveRoomService.addLiveRoom(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 3. 验证结果
        assertEquals(200, response.getCode(), "新增直播间应该成功");
        assertNotNull(createdLiveRoomId, "应该生成直播间 ID");

        // 4. 验证数据库记录
        LiveRoom savedRoom = liveRoomMapper.selectById(createdLiveRoomId);
        assertNotNull(savedRoom, "数据库中应该存在该直播间记录");
        assertEquals(request.getAnchorNumber(), savedRoom.getAnchorNumber(), "主播账号应该匹配");
        assertEquals(DEFAULT_TENANT_ID, savedRoom.getTenantId(), "租户 ID 应该匹配");
        assertEquals(AccountStatus.NORMAL, savedRoom.getAccountStatus(), "默认状态应该是正常");

        log.info("【测试新增直播间】通过，createdLiveRoomId: {}", createdLiveRoomId);
    }

    /**
     * 测试修改直播间 - 成功场景
     */
    @Test
    void testUpdateLiveRoom_Success() {
        log.info("【测试修改直播间】开始");

        // 1. 先创建一个直播间
        Long roomId = createTestLiveRoom();

        // 2. 构造修改请求
        LiveRoomUpdateRequest request = new LiveRoomUpdateRequest();
        request.setId(roomId);
        request.setCompanyId(DEFAULT_COMPANY_ID);
        request.setDeptId(DEFAULT_DEPT_ID);
        request.setTeamId(DEFAULT_TEAM_ID);
        List<Long> managerUserIds = new ArrayList<>();
        managerUserIds.add(DEFAULT_USER_ID);
        request.setManagerUserIds(managerUserIds);

        // 3. 执行测试
        ApiResponse<Void> response = liveRoomService.updateLiveRoom(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 4. 验证结果
        assertEquals(200, response.getCode(), "修改直播间应该成功");

        // 5. 验证数据库记录
        LiveRoom updatedRoom = liveRoomMapper.selectById(roomId);
        assertNotNull(updatedRoom, "更新后的直播间应该存在");
        assertEquals(DEFAULT_DEPT_ID, updatedRoom.getDeptId(), "部门 ID 应该更新");

        log.info("【测试修改直播间】通过");
    }

    /**
     * 测试删除直播间 - 成功场景
     */
    @Test
    void testDeleteLiveRoom_Success() {
        log.info("【测试删除直播间】开始");

        // 1. 先创建一个直播间
        Long roomId = createTestLiveRoom();

        // 2. 执行删除
        ApiResponse<Void> response = liveRoomService.deleteLiveRoom(roomId, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 3. 验证结果
        assertEquals(200, response.getCode(), "删除直播间应该成功");

        // 4. 验证数据库记录（应该被逻辑删除）
        LiveRoom deletedRoom = liveRoomMapper.selectById(roomId);
        assertNotNull(deletedRoom, "直播间记录应该存在（逻辑删除）");
        assertTrue(deletedRoom.getIsDeleted(), "isDeleted 应该为 true");

        log.info("【测试删除直播间】通过");
    }

    /**
     * 测试启用/停用直播间 - 成功场景
     */
    @Test
    void testEnableLiveRoom_Success() {
        log.info("【测试启用/停用直播间】开始");

        // 1. 先创建一个直播间
        Long roomId = createTestLiveRoom();

        // 2. 测试停用
        ApiResponse<Void> disableResponse = liveRoomService.enableLiveRoom(roomId, DEFAULT_TENANT_ID, DEFAULT_USER_ID, AccountStatus.DISABLED);
        assertEquals(200, disableResponse.getCode(), "停用直播间应该成功");

        LiveRoom disabledRoom = liveRoomMapper.selectById(roomId);
        assertEquals(AccountStatus.DISABLED, disabledRoom.getAccountStatus(), "状态应该为禁用");

        // 3. 测试启用
        ApiResponse<Void> enableResponse = liveRoomService.enableLiveRoom(roomId, DEFAULT_TENANT_ID, DEFAULT_USER_ID, AccountStatus.NORMAL);
        assertEquals(200, enableResponse.getCode(), "启用直播间应该成功");

        LiveRoom enabledRoom = liveRoomMapper.selectById(roomId);
        assertEquals(AccountStatus.NORMAL, enabledRoom.getAccountStatus(), "状态应该为正常");

        log.info("【测试启用/停用直播间】通过");
    }

    /**
     * 测试分页查询直播间列表 - 成功场景
     */
    @Test
    void testPageQueryLiveRoom_Success() {
        log.info("【测试分页查询直播间列表】开始");

        // 1. 先创建几个测试直播间
        createTestLiveRoom();
        createTestLiveRoom();

        // 2. 构造查询请求
        LiveRoomQueryRequest request = new LiveRoomQueryRequest();
        request.setPage(1);
        request.setLimit(10);
        request.setCompanyId(DEFAULT_COMPANY_ID);
        request.setDeptId(DEFAULT_DEPT_ID);
        request.setTeamId(DEFAULT_TEAM_ID);

        // 3. 执行查询
        PageData<LiveRoomResponse> pageData = liveRoomService.pageQueryLiveRoom(request, DEFAULT_TENANT_ID);

        // 4. 验证结果
        assertNotNull(pageData, "分页数据不应该为 null");
        assertTrue(pageData.getTotalCount() >= 2, "应该查询到至少 2 条记录");
        assertNotNull(pageData.getList(), "列表不应该为 null");

        log.info("【测试分页查询直播间列表】通过，totalCount: {}", pageData.getTotalCount());
    }

    /**
     * 测试获取直播间详情 - 成功场景
     */
    @Test
    void testGetLiveRoomDetail_Success() {
        log.info("【测试获取直播间详情】开始");

        // 1. 先创建一个直播间
        Long roomId = createTestLiveRoom();

        // 2. 执行查询
        LiveRoomResponse detail = liveRoomService.getLiveRoomDetail(roomId, DEFAULT_TENANT_ID);

        // 3. 验证结果
        assertNotNull(detail, "直播间详情不应该为 null");
        assertEquals(roomId, detail.getId(), "直播间 ID 应该匹配");
        assertNotNull(detail.getCompanyName(), "应该包含公司名称");
        assertNotNull(detail.getDeptName(), "应该包含部门名称");

        log.info("【测试获取直播间详情】通过");
    }

    /**
     * 测试直播间搜索查询 - 成功场景
     */
    @Test
    void testOptions_Success() {
        log.info("【测试直播间搜索查询】开始");

        // 1. 先创建一个测试直播间
        createTestLiveRoom();

        // 2. 构造搜索请求
        LiveRoomSearchQueryRequest request = new LiveRoomSearchQueryRequest();
        request.setTenantId(DEFAULT_TENANT_ID);
        request.setCompanyId(DEFAULT_COMPANY_ID);
        request.setDeptId(DEFAULT_DEPT_ID);
        request.setTeamId(DEFAULT_TEAM_ID);
        request.setKeyword("测试");
        request.setLimit(10);

        // 3. 执行搜索
        List<LabelOption> options = liveRoomService.options(request);

        // 4. 验证结果
        assertNotNull(options, "搜索结果不应该为 null");

        log.info("【测试直播间搜索查询】通过，result size: {}", options.size());
    }

    /**
     * 测试设置直播间排班配置 - 成功场景
     */
    @Test
    void testSetScheduleAttribute_Success() {
        log.info("【测试设置直播间排班配置】开始");

        // 1. 先创建一个直播间
        Long roomId = createTestLiveRoom();

        // 2. 构造排班配置请求
        LiveRoomScheduleAttributeSetRequest request = new LiveRoomScheduleAttributeSetRequest();
        request.setId(roomId);
        request.setStartPlan(LocalTime.of(9, 0));
        request.setEndPlan(LocalTime.of(18, 0));
        request.setShiftOptions(List.of(1, 2, 3));
        request.setRestOptions(List.of(30, 60));
        request.setPositionOptions(List.of(1L, 2L));

        // 3. 执行设置
        ApiResponse<Void> response = liveRoomService.setScheduleAttribute(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 4. 验证结果
        assertEquals(200, response.getCode(), "设置排班配置应该成功");

        // 5. 验证数据库记录
        LiveRoomScheduleAttribute attribute = scheduleAttributeMapper.selectById(roomId);
        assertNotNull(attribute, "排班配置记录应该存在");
        assertEquals(9 * 100, attribute.getStartPlan(), "开始时间应该为 09:00");

        log.info("【测试设置直播间排班配置】通过");
    }

    /**
     * 测试获取直播间排班配置 - 成功场景
     */
    @Test
    void testGetScheduleAttribute_Success() {
        log.info("【测试获取直播间排班配置】开始");

        // 1. 先创建一个直播间并设置排班配置
        Long roomId = createTestLiveRoom();

        LiveRoomScheduleAttributeSetRequest setRequest = new LiveRoomScheduleAttributeSetRequest();
        setRequest.setId(roomId);
        setRequest.setStartPlan(LocalTime.of(10, 0));
        setRequest.setEndPlan(LocalTime.of(19, 0));
        setRequest.setShiftOptions(List.of(1, 2));
        setRequest.setRestOptions(List.of(30));
        setRequest.setPositionOptions(List.of(1L));
        liveRoomService.setScheduleAttribute(setRequest, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 2. 执行查询
        LiveRoomScheduleAttributeResponse attribute = liveRoomService.getScheduleAttribute(roomId, DEFAULT_TENANT_ID);

        // 3. 验证结果
        assertNotNull(attribute, "排班配置不应该为 null");
        assertEquals(roomId, attribute.getId(), "直播间 ID 应该匹配");
        assertNotNull(attribute.getStartPlan(), "开始时间不应该为 null");

        log.info("【测试获取直播间排班配置】通过");
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建直播间新增请求
     */
    private LiveRoomAddRequest buildLiveRoomAddRequest() {
        LiveRoomAddRequest request = new LiveRoomAddRequest();
        request.setPlatform(0); // 抖音
        request.setAnchorNumber("test_anchor_" + RandomUtil.randomString(6));
        request.setCompanyId(DEFAULT_COMPANY_ID);
        request.setDeptId(DEFAULT_DEPT_ID);
        request.setTeamId(DEFAULT_TEAM_ID);
        request.setDebutDate(LocalDate.now());
        List<Long> managerUserIds = new ArrayList<>();
        managerUserIds.add(DEFAULT_USER_ID);
        request.setManagerUserIds(managerUserIds);
        return request;
    }

    /**
     * 创建测试直播间
     *
     * @return 创建的直播间 ID
     */
    private Long createTestLiveRoom() {
        if (createdLiveRoomId != null) {
            return createdLiveRoomId;
        }

        LiveRoomAddRequest request = buildLiveRoomAddRequest();
        ApiResponse<Void> response = liveRoomService.addLiveRoom(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        assertEquals(200, response.getCode(), "创建测试直播间应该成功");

        // 通过查询获取刚创建的直播间
        LiveRoomQueryRequest queryRequest = new LiveRoomQueryRequest();
        queryRequest.setPage(1);
        queryRequest.setLimit(1);
        queryRequest.setCompanyId(DEFAULT_COMPANY_ID);
        queryRequest.setDeptId(DEFAULT_DEPT_ID);
        queryRequest.setTeamId(DEFAULT_TEAM_ID);

        PageData<LiveRoomResponse> pageData = liveRoomService.pageQueryLiveRoom(queryRequest, DEFAULT_TENANT_ID);
        if (pageData.getTotalCount() > 0) {
            createdLiveRoomId = pageData.getList().get(0).getId();
        }

        return createdLiveRoomId;
    }
}
