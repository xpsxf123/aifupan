# Room 模块单元测试说明

## 测试模块结构

```
src/test/java/com/jiuyu/governance/business/room/
├── base/                          # 测试基类
│   └── BaseRoomTest.java          # Room 模块测试基类，提供通用测试能力
├── service/                       # Service 层测试
│   ├── LiveRoomServiceTest.java           # 直播间管理服务测试
│   └── LiveRoomScheduleManageServiceTest.java  # 排班管理服务测试
└── utils/                         # 测试工具类
    └── RoomTestDataBuilder.java   # 测试数据构建工具
```

## 测试覆盖范围

### 1. LiveRoomServiceTest - 直播间管理服务测试

**测试场景：**
- ✅ 新增直播间（成功场景）
- ✅ 修改直播间（成功场景）
- ✅ 删除直播间（成功场景）
- ✅ 启用/停用直播间（成功场景）
- ✅ 分页查询直播间列表（成功场景）
- ✅ 获取直播间详情（成功场景）
- ✅ 直播间搜索查询（成功场景）
- ✅ 设置直播间排班配置（成功场景）
- ✅ 获取直播间排班配置（成功场景）

**验证点：**
- Service 方法调用结果验证
- 数据库记录验证（使用 MyBatis-Plus + Business Mapper）
- 参数传递验证
- 业务规则验证（如状态、关联关系等）

### 2. LiveRoomScheduleManageServiceTest - 排班管理服务测试

**测试场景：**
- ✅ 新增排班（成功场景）
- ✅ 时间范围查询排班（成功场景）
- ✅ 分页查询排班（成功场景）
- ✅ 添加排班人员（成功场景）
- ✅ 移除排班人员（成功场景）
- ✅ 修改排班（成功场景）
- ✅ 删除排班（成功场景）
- ✅ 获取直播间排班（成功场景）

**验证点：**
- 排班 CRUD 操作验证
- 排班人员管理验证
- 时间范围查询验证
- 分页查询验证
- 冲突检测机制验证

## 测试设计原则

### 1. 不允许改动 Service 代码
所有测试用例严格遵循现有 Service 接口定义，不修改任何 Service 实现。

### 2. 入参和出参格式规范
- **入参**：完全符合 Service 方法定义的参数类型和格式
- **出参**：严格按照 Service 返回类型进行验证
- **请求对象**：使用 `RoomTestDataBuilder` 工具类统一构建

### 3. 使用 MyBatis-Plus + 业务 Mapper 验证
```java
// 示例：验证数据库记录
WorkSchedule savedSchedule = workScheduleMapper.selectById(createdScheduleId);
assertNotNull(savedSchedule, "数据库中应该存在该排班记录");
assertEquals(testLiveRoomId, savedSchedule.getLiveRoomId(), "直播间 ID 应该匹配");

// 示例：使用 ChainWrappers 查询验证
ScheduleEmployee savedEmp = ChainWrappers.lambdaQueryChain(scheduleEmployeeMapper)
    .eq(ScheduleEmployee::getScheduleId, scheduleId)
    .eq(ScheduleEmployee::getEmployeeId, DEFAULT_USER_ID)
    .eq(ScheduleEmployee::getIsDeleted, false)
    .one();
```

## 测试数据管理

### 1. 测试基类常量
```java
protected static final Long DEFAULT_TENANT_ID = 1L;   // 默认租户 ID
protected static final Long DEFAULT_USER_ID = 1L;     // 默认用户 ID
protected static final Long DEFAULT_COMPANY_ID = 1L;  // 默认公司 ID
protected static final Long DEFAULT_DEPT_ID = 1L;     // 默认部门 ID
protected static final Long DEFAULT_TEAM_ID = 1L;     // 默认小组 ID
```

### 2. 测试数据构建工具
使用 `RoomTestDataBuilder` 统一构建测试数据：
```java
// 构建直播间新增请求
LiveRoomAddRequest request = RoomTestDataBuilder.buildLiveRoomAddRequest(
    DEFAULT_COMPANY_ID, 
    DEFAULT_DEPT_ID, 
    DEFAULT_TEAM_ID
);

// 构建排班新增请求
LiveRoomScheduleAddRequest request = RoomTestDataBuilder.buildScheduleAddRequest(
    liveRoomId, 
    LocalDate.now()
);
```

## 运行测试

### Maven 命令
```bash
# 运行所有 Room 模块测试
mvn test -Dtest="**/business/room/**/*Test"

# 运行单个测试类
mvn test -Dtest=LiveRoomServiceTest
mvn test -Dtest=LiveRoomScheduleManageServiceTest

# 运行特定测试方法
mvn test -Dtest=LiveRoomServiceTest#testAddLiveRoom_Success
```

### IDEA 运行
- 右键点击测试类或测试方法
- 选择 "Run 'XXXTest'" 或 "Debug 'XXXTest'"

## 测试执行流程

每个测试类的执行流程：
```
@BeforeEach setUp()
    ↓
准备测试数据（创建直播间等）
    ↓
执行具体测试方法
    ↓
验证结果（断言）
    ↓
@AfterEach (自动清理)
```

## 验证策略

### 1. Service 层验证
```java
ApiResponse<Void> response = liveRoomService.addLiveRoom(request, DEFAULT_TENANT_ID, DEFAULT_USER_ID);
assertEquals(200, response.getCode(), "新增直播间应该成功");
```

### 2. 数据库层验证（使用 Mapper）
```java
LiveRoom savedRoom = liveRoomMapper.selectById(createdLiveRoomId);
assertNotNull(savedRoom, "数据库中应该存在该直播间记录");
assertEquals(request.getAnchorNumber(), savedRoom.getAnchorNumber(), "主播账号应该匹配");
```

### 3. 业务规则验证
```java
assertEquals(AccountStatus.NORMAL, savedRoom.getAccountStatus(), "默认状态应该是正常");
assertTrue(deletedRoom.getIsDeleted(), "isDeleted 应该为 true");
```

## 注意事项

1. **测试隔离**：每个测试用例应独立，避免相互依赖
2. **数据清理**：测试完成后应清理测试数据（逻辑删除）
3. **事务回滚**：建议在测试中使用 `@Transactional` 注解自动回滚
4. **日志输出**：使用 `log.info()` 记录关键测试步骤
5. **异常处理**：对于失败场景的测试，应验证异常信息

## 扩展建议

后续可以补充以下测试场景：

1. **失败场景测试**
   - 重复创建直播间
   - 修改不存在的直播间
   - 删除已删除的直播间
   - 排班冲突检测

2. **边界条件测试**
   - 分页边界值
   - 时间范围边界
   - 参数为空/null

3. **集成测试**
   - 完整的业务流程测试
   - 多模块联动测试

## 参考文档

- [单元测试架构与验证规范](../../../docs/unit-test-specification.md)
- [Org 模块测试示例](../org/service/)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
