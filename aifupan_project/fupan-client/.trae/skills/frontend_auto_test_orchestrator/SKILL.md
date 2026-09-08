---
name: "frontend_auto_test_orchestrator"
description: "将接口定义自动编排为自动化测试参数，通过锚点触发页面操作、截取接口与HTML变化、三方诊断问题。当用户要做接口→页面交互→代码的自动化测试诊断闭环时调用。"
---

# 前端自动化测试编排器（接口 → 页面 → 代码 三方诊断）

## 概述

本技能提供一套**从接口定义到前端页面测试的自动化编排框架**，将以下三个维度串联为一个诊断闭环：

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  接口层       │ ──→ │  页面交互层   │ ──→ │  代码层       │
│  (API defs)  │     │  (Anchors)   │     │  (Source)    │
└──────────────┘     └──────────────┘     └──────────────┘
       │                     │                     │
       └─────────── 三方诊断报告 ←─────────────────┘
```

## 触发条件

- 用户提供接口列表，需要自动验证前端对接是否正确
- 用户想快速定位「接口返回 → 页面渲染 → 前端代码逻辑」链路中的问题
- 用户需要生成可一键执行的自动化测试任务包
- 用户提到"自动化测试、锚点测试、接口诊断、页面交互自动化"等关键词

## 核心流程

### 1. 接口 → 测试参数自动编排

- **输入**：接口定义（URL / method / 请求参数 / 响应字段）
- **产出**：自动化测试参数组合（正常值 / 边界值 / 异常值 / 必填缺失）
- **编排策略**：
  - 参数类型推断：从接口定义与已有代码中推断字段类型（string/number/boolean/enum）
  - 组合生成：自动生成正交测试用例（等价类 + 边界值 + 异常场景）
  - 优先级排序：按接口调用频率/业务重要性排列测试优先级

### 2. 页面代码与 HTML 内容收集

- **关联代码定位**：从接口 URL 反向定位调用方页面组件（通过 `src/api/` → `src/views/` 引用链）
- **页面信息采集**：
  - 页面组件文件路径与关键代码片段
  - 绑定的状态变量与 computed 依赖
  - 表单/表格/按钮等交互元素的 DOM 结构
  - 接口调用上下文（触发时机、loading 状态、错误处理）

### 3. 自动化锚点设计

- **锚点定义**：在页面 HTML 中标记关键交互节点，用于自动化操作触发
- **锚点类型**：
  | 类型 | 说明 | 示例 |
  |------|------|------|
  | 输入锚点 | 表单输入框/选择器 | `data-testid="anchor-input-{fieldName}"` |
  | 触发锚点 | 按钮/开关等操作元素 | `data-testid="anchor-trigger-{action}"` |
  | 展示锚点 | 表格/列表/文本渲染区域 | `data-testid="anchor-display-{section}"` |
  | 状态锚点 | loading/empty/error 状态标记 | `data-testid="anchor-state-{type}"` |
- **锚点定位策略**：优先使用已有 `data-testid`/`ref`/`id`，缺失时由技能辅助补标

### 4. 操作截取与变化分析

每次锚点操作后自动截取：

1. **接口快照**：请求 URL、参数、响应数据、耗时、状态码
2. **页面 HTML 快照**：目标区域前后 diff（文本变化 / DOM 结构变化 / class 变化）
3. **控制台日志**：错误、警告、未捕获异常

### 5. 三方诊断报告

将三个维度的数据交叉对比，输出诊断结论：

```
接口预期 ←→ 页面实际渲染 ←→ 代码逻辑
```

- **数据流差异**：接口返回字段 vs 页面实际展示字段
- **状态不一致**：loading/empty/error 态是否符合预期
- **边界异常**：空数据、超长文本、特殊字符的渲染表现
- **性能问题**：重复请求、未防抖、未缓存

### 6. 变更范围控制

- **影响范围分析**：修改前计算影响的组件/路由/接口范围
- **变更边界检查**：修改后确保代码变更不超出分析范围
- **回滚策略**：每个诊断修复提供独立的回滚步骤

## 工具调用关系

### 需要补齐的工具模块

| 模块 | 功能 | 依赖 |
|------|------|------|
| `anchor_designer` | 在页面中注入/管理自动化锚点 | 页面组件文件 |
| `api_param_generator` | 从接口定义生成测试参数组合 | 接口定义 |
| `page_snapshot_capturer` | 截取页面 HTML 与接口请求/响应 | 浏览器运行时 |
| `diff_analyzer` | 对比前后快照，输出差异报告 | 快照数据 |
| `code_linker` | 从接口反向追踪到页面代码引用链 | 源码项目 |
| `diagnosis_engine` | 三方交叉诊断，输出问题与修复建议 | 所有模块 |
| `scope_checker` | 变更影响范围分析 | 源码项目 |
| `task_packager` | 编排测试任务，一键打包执行 | 所有模块 |

### 调用链路

```
用户提供接口列表
  │
  ├──→ api_param_generator     (生成测试参数)
  ├──→ code_linker             (追踪关联页面代码)
  │
  ▼
anchor_designer                (注入操作锚点)
  │
  ▼
page_snapshot_capturer         (逐锚点操作 + 截取快照)
  │
  ▼
diff_analyzer                  (变化对比)
  │
  ▼
diagnosis_engine               (三方诊断)
  │
  ├──→ scope_checker           (变更范围控制)
  │
  ▼
task_packager                  (打包输出诊断报告)
```

## 前端操作台页面（待实现）

需提供一个前端页面用于：

1. **任务编排**：可视化拖拽/选择接口，排列测试顺序，配置锚点
2. **任务打包**：将编排好的任务序列保存为可执行任务包（JSON 格式）
3. **一键执行**：点击按钮触发全部自动化测试流程
4. **实时监控**：展示每个步骤的执行状态、接口耗时、页面截图
5. **报告导出**：导出诊断报告（HTML/JSON/Markdown），包含：
   - 每个接口的请求/响应数据
   - 页面渲染变化的 before/after 对比
   - 代码层问题定位（文件路径 + 行号）
   - 修复建议与影响范围

### 操作台页面建议路径

```
src/views/modules/devTools/AutoTestOrchestrator/
├── index.vue              # 主页面（任务列表 + 操作面板）
├── components/
│   ├── ApiSelector.vue    # 接口选择器
│   ├── AnchorDesigner.vue # 锚点可视化编辑器
│   ├── TaskRunner.vue     # 任务执行器（进度 + 日志）
│   ├── DiffViewer.vue     # 快照差异对比视图
│   └── ReportExporter.vue # 诊断报告导出
└── store/
    └── autoTestStore.js   # 测试状态管理
```

## 输出物规范

### 诊断报告结构

```json
{
  "timestamp": "ISO8601",
  "target_api": {
    "url": "/api/v1/xxx",
    "method": "GET"
  },
  "test_cases": [
    {
      "params": {},
      "anchor_operations": [],
      "snapshots": {
        "before": {},
        "after": {}
      },
      "diagnosis": {
        "issue_type": "data_mismatch | state_error | render_error | performance",
        "severity": "critical | major | minor",
        "affected_files": [],
        "fix_suggestion": ""
      }
    }
  ],
  "scope_analysis": {
    "changed_files": [],
    "affected_components": [],
    "rollback_steps": []
  }
}
```

## 当前状态

- **已完成**：无（本次为技能框架设计阶段）
- **待补齐**：
  - [ ] `api_param_generator` 参数生成逻辑（从另一个分支迁移）
  - [ ] `code_linker` 接口→代码反向追踪
  - [ ] `anchor_designer` 锚点注入与页面交互触发
  - [ ] `page_snapshot_capturer` 浏览器快照截取
  - [ ] `diff_analyzer` 前后快照对比
  - [ ] `diagnosis_engine` 三方交叉诊断
  - [ ] `scope_checker` 变更范围控制
  - [ ] `task_packager` 任务打包与一键执行
  - [ ] 前端操作台页面 `src/views/modules/devTools/AutoTestOrchestrator/`
  - [ ] 与 agentServe 智能体服务的集成（锚点触发 + 数据回传）

## 版本历史

- 2026-08-11：技能框架设计阶段，记录整体逻辑与模块调用关系
