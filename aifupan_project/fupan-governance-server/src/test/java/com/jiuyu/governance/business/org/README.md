# Org 模块单元测试架构说明

## 测试架构概述

本测试架构采用分层设计，按照业务模块的流程和测试类型（正常、异常、非法参数）进行组织，确保测试数据之间具有关联性。

## 目录结构

```
src/test/java/com/jiuyu/governance/business/org/
├── base/                          # 测试基类
│   └── BaseOrgTest.java           # Org 模块通用测试基类
└── service/                       # Service 层测试
    ├── SubCompanyServiceTest.java # 子公司服务测试
    ├── DeptServiceTest.java       # 部门服务测试
    ├── TeamServiceTest.java       # 团队服务测试
    └── PositionServiceTest.java   # 职位服务测试
```

## 核心组件

### 1. BaseOrgTest（测试基类）

**职责：**
- 提供 Spring Boot 测试环境支持
- 定义通用的测试常量（租户 ID、用户 ID）
- 提供测试生命周期方法（setUp、tearDown）

**使用方式：**
所有 Org 模块的测试类都继承自 `BaseOrgTest`，自动获得以下能力：
- `@SpringBootTest` 注解加载完整的 Spring 容器
- `@ActiveProfiles("local")` 使用本地测试配置
- 通用的测试数据构造方法

### 2. Service 层测试

每个 Service 测试类都遵循统一的测试模式：

#### 测试场景分类

**正常场景测试（编号 1-7）：**
- 新增操作 - 正常流程
- 修改操作 - 正常流程
- 删除操作 - 正常流程
- 分页查询 - 正常流程
- 存在性检查 - 正常流程
- 数据获取 - 正常流程
- 下拉选项 - 正常流程

**异常场景测试（编号 8-14）：**
- 数据不存在
- 名称重复
- 超出数量限制
- 关联数据检查（如部门下存在员工不可删除）

**非法参数测试（编号 15-19）：**
- null 值
- 空字符串
- 超长文本
- 必填字段缺失

## 测试数据关联设计

### 数据层级关系

```
租户 (Tenant)
  └── 子公司 (SubCompany)
      └── 部门 (Dept)
          └── 小组 (Team)
```

### 数据依赖关系

1. **子公司测试**：独立创建，仅依赖租户
2. **部门测试**：依赖子公司，必须先创建子公司
3. **团队测试**：依赖子公司和部门，必须先创建公司和部门
4. **职位测试**：独立创建，仅依赖租户

### 辅助方法设计

每个测试类都包含 `createTestXXX()` 辅助方法：

```java
// 示例：DeptServiceTest 中的辅助方法
private SubCompany createTestCompany(String name) {
    // 创建测试公司
}

private Dept createTestDept(Long companyId, String name) {
    // 创建测试部门，需要 companyId 参数
}
```

## 测试用例编号规则

测试用例采用统一命名格式：

```
@DisplayName("序号。测试功能 - 测试场景")
```

**示例：**
- `@DisplayName("1. 新增子公司 - 正常流程")`
- `@DisplayName("8. 新增子公司 - 公司名称重复")`
- `@DisplayName("12. 新增子公司 - 公司名称为空")`

## 测试清理机制

### BeforeEach + AfterEach 双重清理

```java
@BeforeEach
void setUp() {
    // 测试前清理脏数据
    ChainWrappers.lambdaDeleteChain(...)
        .eq(XX::getTenantId, DEFAULT_TENANT_ID)
        .like(XX::getName, "测试 XXX")
        .remove();
}

@AfterEach
void tearDown() {
    // 测试后清理所有创建的测试数据
    ChainWrappers.lambdaDeleteChain(...)
        .eq(XX::getTenantId, DEFAULT_TENANT_ID)
        .like(XX::getName, "测试 XXX")
        .remove();
}
```

**优势：**
- 防止测试间相互影响
- 即使测试失败也能保证数据清理
- 使用唯一前缀标识测试数据，避免误删

## Mock 对象使用

对于依赖外部服务的场景，使用 Mockito 创建 Mock 对象：

```java
// PositionServiceTest 中 Mock AccessUser
private AccessUser mockAccessUser;

@BeforeEach
void setUp() {
    mockAccessUser = Mockito.mock(AccessUser.class);
    Mockito.when(mockAccessUser.currentTenantId()).thenReturn(DEFAULT_TENANT_ID);
    Mockito.when(mockAccessUser.userId()).thenReturn(DEFAULT_USER_ID);
}
```

## 断言使用规范

使用 JUnit 5 的标准断言：

```java
// 验证响应码
assertEquals(200, response.getCode(), "新增成功应该返回 200");

// 验证存在性
assertTrue(exists, "应该返回 true");
assertFalse(notExists, "应该返回 false");

// 验证对象非空
assertNotNull(object, "对象不应为 null");

// 验证集合大小
assertEquals(2, list.size(), "应该返回 2 个结果");

// 验证包含关系
assertTrue(list.contains(item), "列表应该包含指定元素");

// 验证异常抛出
assertThrows(Exception.class, () -> {
    service.method(invalidParam);
}, "非法参数应该抛出异常");
```

## 测试覆盖的功能点

### SubCompanyService（子公司服务）
- ✅ 新增子公司（成功、名称重复、非法参数）
- ✅ 修改子公司（成功、不存在、名称重复）
- ✅ 删除子公司（成功、不存在）
- ✅ 分页查询子公司
- ✅ 存在性检查
- ✅ 获取名称 Map
- ✅ 下拉选项查询

### DeptService（部门服务）
- ✅ 新增部门（成功、公司不存在、名称重复、非法参数）
- ✅ 修改部门（成功、不存在、名称重复）
- ✅ 删除部门（成功、不存在）
- ⚠️ 删除部门 - 部门下存在员工（TODO: 需要 rbac 模块支持）
- ⚠️ 删除部门 - 部门下存在小组（TODO: 需要 room 模块支持）
- ✅ 分页查询部门
- ✅ 存在性检查
- ✅ 根据 ID 查询部门
- ✅ 获取名称 Map
- ✅ 下拉选项查询

### TeamService（团队服务）
- ✅ 新增小组（成功、部门不存在、名称重复、非法参数）
- ✅ 修改小组（成功、不存在）
- ✅ 删除小组（成功、不存在）
- ✅ 分页查询小组
- ✅ 获取名称 Map
- ✅ 下拉选项查询

### PositionService（职位服务）
- ✅ 新增岗位（成功、名称重复、非法参数）
- ✅ 修改岗位（成功、不存在、名称重复）
- ✅ 删除岗位（成功、不存在、默认岗位保护）
- ✅ 分页查询岗位
- ✅ 获取名称 Map
- ✅ 获取岗位选项
- ✅ 列出选项

## 运行测试

### 运行单个测试类
```bash
mvn test -Dtest=SubCompanyServiceTest
```

### 运行所有 Org 模块测试
```bash
mvn test -Dtest="com.jiuyu.governance.business.org.service.*Test"
```

### 运行特定测试方法
```bash
mvn test -Dtest=SubCompanyServiceTest#testAddSubCompany_Success
```

## 测试报告

测试执行后会生成 HTML 报告：
```
target/surefire-reports/index.html
```

## 后续扩展建议

1. **集成测试**：添加跨模块的集成测试
2. **性能测试**：为大数据量场景添加性能测试
3. **并发测试**：测试并发操作的数据一致性
4. **数据工厂**：创建专门的测试数据工厂类来管理测试数据生成
5. **测试覆盖率检查**：使用 JaCoCo 等工具监控测试覆盖率

## 注意事项

1. **测试数据隔离**：每个测试用例都应该独立，不依赖其他测试的结果
2. **清理机制**：必须确保测试数据能够被正确清理
3. **断言信息**：所有断言都应该提供清晰的描述信息
4. **测试顺序**：测试方法的执行顺序不固定，不要依赖方法间的执行顺序
5. **Mock 使用**：对于外部依赖（如 EmployeeService），使用 Mock 来隔离测试

---

**文档版本：** 1.0  
**创建时间：** 2026-03-27  
**维护者：** HeHui
