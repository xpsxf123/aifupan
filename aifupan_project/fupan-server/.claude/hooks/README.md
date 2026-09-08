# `.claude/hooks/` — 项目级共享 Git Hooks

## 为什么有这个目录

默认 git hook 在 `.git/hooks/` 下，**不入版本控制**，每个 clone 仓库的人都要自己配置。

为了让 hook 跨开发者共享，本仓库把 hook 放在 `.claude/hooks/`（入 commit），通过 `git config core.hooksPath` 让 git 从此目录读 hook。

## 一次性启用

任选一种：

### 方式 1：用 setup 脚本（推荐）

```bash
bash .claude/scripts/tools/install_git_hooks.sh
```

脚本会：
1. `git config core.hooksPath .claude/hooks`
2. `chmod +x .claude/hooks/*`（确保 hook 可执行）
3. 验证一次

### 方式 2：手工

```bash
git config core.hooksPath .claude/hooks
chmod +x .claude/hooks/*
```

## 当前 hooks

| Hook | 检查项 |
|---|---|
| `pre-commit` | 调 `filename_linter.py --staged` 验证 staged 文件名仅含 `[a-zA-Z0-9._-]`（Windows 兼容） |

## 添加新 hook

1. 写 hook 脚本到 `.claude/hooks/<hook-name>`（如 `pre-push`）
2. `chmod +x .claude/hooks/<hook-name>`
3. 测试：触发对应 git 动作看 hook 是否跑（如 `git push` 触发 pre-push）

## 添加新检查项到已有 hook

直接编辑 hook 脚本，在 `EXIT_CODE=0` 后追加：

```bash
echo "🔍 [pre-commit] 检查 XXX..."
if ! python3 .claude/scripts/gates/xxx_linter.py --staged; then
    EXIT_CODE=1
fi
```

## 绕过（不推荐）

```bash
git commit --no-verify
```

请仅在真的必要时使用（且 PR 描述明示原因）。

## 卸载

```bash
git config --unset core.hooksPath
# git hook 回退到 .git/hooks/
```
