#!/usr/bin/env bash
# 一次性配置 git 使用 .claude/hooks/ 下的共享 hook
#
# 用法：bash .claude/scripts/tools/install_git_hooks.sh
#
# 执行：
#   1. git config core.hooksPath .claude/hooks
#   2. chmod +x .claude/hooks/*
#   3. 验证 hook 可执行

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

HOOKS_DIR=".claude/hooks"

if [ ! -d "${HOOKS_DIR}" ]; then
    echo "❌ ${HOOKS_DIR} 不存在，本仓库可能不需要 setup"
    exit 1
fi

echo "📌 配置 git core.hooksPath → ${HOOKS_DIR}"
git config core.hooksPath "${HOOKS_DIR}"

echo "📌 给 hook 文件加可执行权限"
chmod +x ${HOOKS_DIR}/* 2>/dev/null || true

echo "📌 当前已启用的 hook:"
for f in ${HOOKS_DIR}/*; do
    fname="$(basename "${f}")"
    # 跳过 README / 配置文件
    if [[ "${fname}" == "README"* ]] || [[ "${fname}" == *.md ]]; then
        continue
    fi
    if [ -x "${f}" ]; then
        echo "  ✅ ${fname}"
    else
        echo "  ⚠️  ${fname}（无可执行权限）"
    fi
done

echo ""
echo "✅ Git hooks 配置完成"
echo "   验证：git config --get core.hooksPath  应返回 ${HOOKS_DIR}"
echo "   绕过单次提交：git commit --no-verify"
