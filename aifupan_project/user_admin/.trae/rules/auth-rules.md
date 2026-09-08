# 权限规则 (Auth Rules)

本规则仅定义权限系统的核心约束。实现细节见 Skill。

## 1. 权限码 (Code)

- **格式**: `平台:页面:操作` (冒号分隔)。
  - `admin:user:add` (正确)
- **通配符 (`*`)**:
  - `*`: 超级权限。
  - `admin:*`: 平台全权。
  - `admin:user:*`: 模块全权。
  - `admin:*:MENU`: 所有菜单。

## 2. 关键字 (Keywords)

- **MENU**: 仅控制菜单/路由。
- **WRITE**: 写操作集合。
- **READ**: 只读权限。

## 3. 验证 (Verification)

- **禁止**: 字符串硬编码比对。
- **必须**: 使用 Validator/Directives/Hooks。
- **原则**: 默认拒绝，最小权限，后端为准。

## 4. 配置 (Config)

- **位置**: `src/auth/`。
- **隔离**: 不同平台权限配置需逻辑隔离。
