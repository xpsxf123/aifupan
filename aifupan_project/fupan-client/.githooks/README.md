# .githooks

本目录用于存放随仓库提交的 Git Hooks。

## 安装

执行：

```bash
npm run hooks:install
```

该命令会设置：

```bash
git config core.hooksPath .githooks
```

## 包含的 hooks

- `pre-commit`
  - 检测新增文件重名（按 basename）
  - 校验版本变更记录文件的版本号必须与当前分支名中的版本号一致
    - 支持：`docs/版本变更/<版本号>.md`
    - 兼容：`docs/<版本号>-修改记录.md`
    - 不一致会阻止提交，并清理本次新增/修改的版本文档
  - 通过后同步更新 `agent/wiki/preferences/file-registry.json`

- `commit-msg`
  - 若本次提交包含功能新增/修改（例如 `src/` 代码变更），自动将变更摘要追加到 `docs/版本变更/<版本号>.md` 并自动 `git add`

