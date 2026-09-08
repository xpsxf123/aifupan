package com.jiuyu.governance.business.room.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.oauth.AuthenticationConstant;
import com.jiuyu.framework.oauth.UserInfo;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.shandard.Range;
import com.jiuyu.governance.business.room.base.BaseRoomTest;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.mapper.ScheduleEmployeeMapper;
import com.jiuyu.governance.business.room.mapper.WorkScheduleMapper;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import com.jiuyu.governance.business.room.pojo.request.schedule.*;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomSchedulePageResponse;
import com.jiuyu.governance.business.room.pojo.response.schedule.LiveRoomScheduleResponse;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 直播间排班管理服务测试
 *
 * <p>测试范围：</p>
 * <ul>
 *     <li>排班新增与批量生成</li>
 *     <li>排班查询（分页、范围）</li>
 *     <li>排班人员管理（添加、移除）</li>
 *     <li>排班修改与删除</li>
 *     <li>冲突检测验证</li>
 * </ul>
 *
 * @author HeHui
 * @date 2026-03-27
 */
@Slf4j
class LiveRoomScheduleManageServiceTest extends BaseRoomTest {

    @Autowired
    private LiveRoomScheduleManageService scheduleManageService;

    @Autowired
    private LiveRoomMapper liveRoomMapper;

    @Autowired
    private WorkScheduleMapper workScheduleMapper;

    @Autowired
    private ScheduleEmployeeMapper scheduleEmployeeMapper;

    private Long testLiveRoomId;
    private Long createdScheduleId;

    @BeforeEach
    public void setUp() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        UserInfo userInfo = new UserInfo();
        userInfo.setAccessToken("test-access-token");
        userInfo.setUserId(DEFAULT_USER_ID);
        userInfo.setUsername("test-username");
        userInfo.setCurrentTenantId(DEFAULT_TENANT_ID);
        userInfo.setUserType(OauthConstant.SYSTEM_USER);

        requestAttributes.setAttribute(AuthenticationConstant.ACCESS_USER_ATTRIBUTE_NAME, userInfo.toRecord(), 0);
        log.info("========== LiveRoomScheduleManageServiceTest 开始执行 ==========");
        // 准备测试用的直播间
        testLiveRoomId = getOrCreateTestLiveRoom();
    }

    /**
     * 测试新增排班 - 成功场景
     */
    @Test
    void testAddSchedule_Success() {
        log.info("【测试新增排班】开始");

        // 1. 准备测试数据
        LiveRoomScheduleAddRequest request = buildScheduleAddRequest(testLiveRoomId);

        // 2. 执行测试
        ApiResponse<Void> response = scheduleManageService.addSchedule(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 3. 验证结果
        assertEquals(0, response.getCode(), "新增排班应该成功");
        assertNotNull(createdScheduleId, "应该生成排班 ID");

        // 4. 验证数据库记录
        WorkSchedule savedSchedule = ChainWrappers.lambdaQueryChain(workScheduleMapper)
            .eq(WorkSchedule::getLiveRoomId, request.getLiveRoomId())
            .eq(WorkSchedule::getWorkDay, request.getWorkDay())
            .eq(WorkSchedule::getStartWork, Integer.valueOf(request.getSessions().get(0).getStartWork().getHour() + "" + request.getSessions().get(0).getStartWork().getMinute()))
            .eq(WorkSchedule::getIsDeleted, false)
            .one();
        assertNotNull(savedSchedule, "数据库中应该存在该排班记录");
        assertEquals(testLiveRoomId, savedSchedule.getLiveRoomId(), "直播间 ID 应该匹配");
        assertEquals(request.getWorkDay(), savedSchedule.getWorkDay(), "工作日期应该匹配");

        log.info("【测试新增排班】通过，createdScheduleId: {}", createdScheduleId);
    }

    /**
     * 测试时间范围查询排班 - 成功场景
     */
    @Test
    void testListRange_Success() {
        log.info("【测试时间范围查询排班】开始");

        // 1. 先创建几个测试排班
        createTestSchedule(testLiveRoomId);

        // 2. 构造查询请求
        LiveRoomScheduleQueryRequest request = new LiveRoomScheduleQueryRequest();
        request.setLiveRoomId(testLiveRoomId);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(7));

        // 3. 执行查询
        List<LiveRoomScheduleResponse> responses = scheduleManageService.listRange(request, DEFAULT_TENANT_ID);

        // 4. 验证结果
        assertNotNull(responses, "查询结果不应该为 null");
        assertFalse(responses.isEmpty(), "应该查询到至少 1 条排班记录");

        LiveRoomScheduleResponse firstSchedule = responses.get(0);
        assertNotNull(firstSchedule.getId(), "排班 ID 不应该为 null");
        assertNotNull(firstSchedule.getWorkDay(), "工作日期不应该为 null");

        log.info("【测试时间范围查询排班】通过，result size: {}", responses.size());
    }

    /**
     * 测试分页查询排班 - 成功场景
     */
    @Test
    void testPage_Success() {
        log.info("【测试分页查询排班】开始");

        // 1. 先创建几个测试排班
        createTestSchedule(testLiveRoomId);

        // 2. 构造分页请求
        LiveRoomSchedulePageRequest request = new LiveRoomSchedulePageRequest();
        request.setLiveRoomId(testLiveRoomId);
        request.setPage(1);
        request.setLimit(10);

        // 3. 执行分页查询
        PageData<LiveRoomSchedulePageResponse> pageData = scheduleManageService.page(request, DEFAULT_TENANT_ID);

        // 4. 验证结果
        assertNotNull(pageData, "分页数据不应该为 null");
        assertTrue(pageData.getTotalCount() >= 1, "应该查询到至少 1 条记录");
        assertNotNull(pageData.getList(), "列表不应该为 null");

        log.info("【测试分页查询排班】通过，totalCount: {}", pageData.getTotalCount());
    }

    /**
     * 测试添加排班人员 - 成功场景
     */
    @Test
    void testAddEmployee_Success() {
        log.info("【测试添加排班人员】开始");

        // 1. 先创建一个排班
        Long scheduleId = createTestSchedule(testLiveRoomId);

        // 2. 构造添加人员请求
        ScheduleAddEmployeeRequest request = new ScheduleAddEmployeeRequest();
        request.setScheduleId(scheduleId);
        request.setEmployeeId(DEFAULT_USER_ID);
        request.setPositionId(1L);

        // 3. 执行测试
        ApiResponse<Void> response = scheduleManageService.addEmployee(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 4. 验证结果
        assertEquals(0, response.getCode(), "添加排班人员应该成功");

        // 5. 验证数据库记录
        ScheduleEmployee savedEmp = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, scheduleId)
            .eq(ScheduleEmployee::getEmployeeId, DEFAULT_USER_ID)
            .eq(ScheduleEmployee::getIsDeleted, false)
            .one();
        assertNotNull(savedEmp, "数据库中应该存在该人员排班记录");
        assertEquals(scheduleId, savedEmp.getScheduleId(), "排班 ID 应该匹配");

        log.info("【测试添加排班人员】通过");
    }

    /**
     * 测试移除排班人员 - 成功场景
     */
    @Test
    void testRemoveEmployee_Success() {
        log.info("【测试移除排班人员】开始");

        // 1. 先创建排班和人员
        Long scheduleId = createTestScheduleWithEmployee(testLiveRoomId);

        // 2. 构造移除人员请求
        ScheduleRemoveEmployeeRequest request = new ScheduleRemoveEmployeeRequest();
        request.setScheduleId(scheduleId);
        request.setEmployeeId(DEFAULT_USER_ID);

        // 3. 执行测试
        ApiResponse<Void> response = scheduleManageService.removeEmployee(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 4. 验证结果
        assertEquals(0, response.getCode(), "移除排班人员应该成功");

        // 5. 验证数据库记录（应该被逻辑删除）
        ScheduleEmployee deletedEmp = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
            .eq(ScheduleEmployee::getScheduleId, scheduleId)
            .eq(ScheduleEmployee::getEmployeeId, DEFAULT_USER_ID)
            .one();
        assertNotNull(deletedEmp, "人员记录应该存在（逻辑删除）");
        assertTrue(deletedEmp.getIsDeleted(), "isDeleted 应该为 true");

        log.info("【测试移除排班人员】通过");
    }

    /**
     * 测试修改排班 - 成功场景
     */
    @Test
    void testUpdateSchedule_Success() {
        log.info("【测试修改排班】开始");

        // 1. 先创建一个排班
        Long scheduleId = createTestSchedule(testLiveRoomId);

        // 2. 构造修改请求
        ScheduleUpdateRequest request = new ScheduleUpdateRequest();
        request.setId(scheduleId);
        request.setStartWork(LocalTime.of(10, 0));
        request.setEndWork(LocalTime.of(18, 0));
        request.setScheduleDuration(480);
        request.setRestDuration(60);

        // 3. 执行测试
        ApiResponse<Void> response = scheduleManageService.updateSchedule(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 4. 验证结果
        assertEquals(0, response.getCode(), "修改排班应该成功");

        // 5. 验证数据库记录
        WorkSchedule updatedSchedule = workScheduleMapper.selectById(scheduleId);
        assertNotNull(updatedSchedule, "更新后的排班应该存在");
        assertEquals(10 * 100, updatedSchedule.getStartWork(), "开始时间应该更新为 10:00");
        assertEquals(18 * 100, updatedSchedule.getEndWork(), "结束时间应该更新为 18:00");

        log.info("【测试修改排班】通过");
    }

    /**
     * 测试删除排班 - 成功场景
     */
    @Test
    void testDeleteSchedule_Success() {
        log.info("【测试删除排班】开始");

        // 1. 先创建一个排班
        Long scheduleId = createTestSchedule(testLiveRoomId);

        // 2. 执行删除
        ApiResponse<Void> response = scheduleManageService.deleteSchedule(scheduleId, DEFAULT_TENANT_ID, DEFAULT_USER_ID);

        // 3. 验证结果
        assertEquals(0, response.getCode(), "删除排班应该成功");

        // 4. 验证数据库记录（应该被逻辑删除）
        WorkSchedule deletedSchedule = workScheduleMapper.selectById(scheduleId);
        assertNotNull(deletedSchedule, "排班记录应该存在（逻辑删除）");
        assertTrue(deletedSchedule.getIsDeleted(), "isDeleted 应该为 true");

        log.info("【测试删除排班】通过");
    }

    /**
     * 测试获取直播间排班 - 成功场景
     */
    @Test
    void testGetLiveSchedule_Success() {
        log.info("【测试获取直播间排班】开始");

        // 1. 先创建测试数据
        createTestScheduleWithEmployee(testLiveRoomId);

        // 2. 获取直播间的 secUid
        LiveRoom liveRoom = liveRoomMapper.selectById(testLiveRoomId);
        assertNotNull(liveRoom, "直播间应该存在");

        // 3. 构造查询参数
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(8);
        List<Range<LocalDateTime>> timeRanges = List.of(new Range<>(startTime, endTime));

        // 4. 执行查询
        List<com.jiuyu.governance.business.room.pojo.bo.RoomScheduleBo> schedules =
            ((LiveRoomScheduleService) scheduleManageService).getLiveSchedule(
                DEFAULT_TENANT_ID,
                LivePlatformType.DOU_YIN,
                List.of(liveRoom.getSecUid()),
                timeRanges
            );

        // 5. 验证结果
        assertNotNull(schedules, "查询结果不应该为 null");

        log.info("【测试获取直播间排班】通过，result size: {}", schedules.size());
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建排班新增请求
     */
    private LiveRoomScheduleAddRequest buildScheduleAddRequest(Long liveRoomId) {
        LiveRoomScheduleAddRequest request = new LiveRoomScheduleAddRequest();
        request.setLiveRoomId(liveRoomId);
        request.setWorkDay(LocalDate.now());
        ScheduleBatchRequest batchConfig = new ScheduleBatchRequest();
        batchConfig.setCycleDays(List.of( 2, 3, 4, 5,  7));
        batchConfig.setEndDate(LocalDate.now().plusDays(60));

        request.setBatchConfig(batchConfig);

        // 设置班次信息
        List<ScheduleSessionRequest> sessions = new ArrayList<>();
        ScheduleSessionRequest session = new ScheduleSessionRequest();
        session.setStartWork(LocalTime.of(17, 0));
        session.setEndWork(LocalTime.of(21, 0));
        session.setScheduleDuration(480);
        session.setRestDuration(60);
        //session.setRemark("测试班次_" + RandomUtil.randomString(4));

        // 设置排班人员
        List<ScheduleEmployeeRequest> employees = new ArrayList<>();
        ScheduleEmployeeRequest employee = new ScheduleEmployeeRequest();
        employee.setEmployeeId(DEFAULT_USER_ID);
        employee.setPositionId(1L);
        employees.add(employee);
        session.setEmployees(employees);

        sessions.add(session);
        request.setSessions(sessions);

        return request;
    }

    /**
     * 获取或创建测试直播间
     */
    private Long getOrCreateTestLiveRoom() {
        if (testLiveRoomId != null) {
            return testLiveRoomId;
        }

        // 查询是否已有测试直播间
        List<LiveRoom> existingRooms = ChainWrappers.lambdaQueryChain(liveRoomMapper)
            .eq(LiveRoom::getTenantId, DEFAULT_TENANT_ID)
            .eq(LiveRoom::getIsDeleted, false)
            .last("LIMIT 1")
            .list();

        if (!existingRooms.isEmpty()) {
            testLiveRoomId = existingRooms.get(0).getId();
            return testLiveRoomId;
        }

        // 创建新的测试直播间
        LiveRoom room = new LiveRoom();
        room.setId(System.currentTimeMillis());
        room.setTenantId(DEFAULT_TENANT_ID);
        room.setCompanyId(DEFAULT_COMPANY_ID);
        room.setDeptId(DEFAULT_DEPT_ID);
        room.setTeamId(DEFAULT_TEAM_ID);
        room.setSecUid("test_sec_uid_" + RandomUtil.randomString(8));
        room.setPlatform(0);
        room.setAnchorNumber("test_anchor_" + RandomUtil.randomString(6));
        room.setAnchorName("测试主播");
        room.setHomeUrl("https://test.com");
        room.setLiveUrl("");
        room.setAnchorAvatar("");
        room.setAccountStatus(com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus.NORMAL);
        room.setCreateBy(DEFAULT_USER_ID);
        room.setUpdateBy(DEFAULT_USER_ID);
        room.setCreateDate(LocalDateTime.now());
        room.setUpdateDate(LocalDateTime.now());
        room.setIsDeleted(false);

        liveRoomMapper.insert(room);
        testLiveRoomId = room.getId();

        return testLiveRoomId;
    }

    /**
     * 创建测试排班
     */
    private Long createTestSchedule(Long liveRoomId) {
        if (createdScheduleId != null) {
            return createdScheduleId;
        }

        LiveRoomScheduleAddRequest request = buildScheduleAddRequest(liveRoomId);
        ApiResponse<Void> response = scheduleManageService.addSchedule(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        assertEquals(0, response.getCode(), "创建测试排班应该成功");

        // 查询刚创建的排班
        List<WorkSchedule> schedules = ChainWrappers.lambdaQueryChain(workScheduleMapper)
            .eq(WorkSchedule::getLiveRoomId, liveRoomId)
            .eq(WorkSchedule::getTenantId, DEFAULT_TENANT_ID)
            .eq(WorkSchedule::getIsDeleted, false)
            .orderByDesc(WorkSchedule::getCreateDate)
            .last("LIMIT 1")
            .list();

        if (!schedules.isEmpty()) {
            createdScheduleId = schedules.get(0).getId();
        }

        return createdScheduleId;
    }

    /**
     * 创建带人员的测试排班
     */
    private Long createTestScheduleWithEmployee(Long liveRoomId) {
        Long scheduleId = createTestSchedule(liveRoomId);

        // 添加人员
        ScheduleAddEmployeeRequest addRequest = new ScheduleAddEmployeeRequest();
        addRequest.setScheduleId(scheduleId);
        addRequest.setEmployeeId(DEFAULT_USER_ID);
        addRequest.setPositionId(1L);

        ApiResponse<Void> response = scheduleManageService.addEmployee(addRequest, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
        assertEquals(0, response.getCode(), "添加人员应该成功");

        return scheduleId;
    }
}
