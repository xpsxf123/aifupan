#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import argparse
import os
from datetime import datetime


def build_dir_name(intent: str, timestamp: str) -> str:
    return f"{intent}__{timestamp}"


def ensure_unique_dir(base_dir: str, dir_name: str) -> str:
    path = os.path.join(base_dir, dir_name)
    if not os.path.exists(path):
        return path
    index = 1
    while True:
        indexed = f"{dir_name}__{index:02d}"
        path = os.path.join(base_dir, indexed)
        if not os.path.exists(path):
            return path
        index += 1


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--intent", required=True, choices=["Learn", "Change", "DocQA", "Audit"])
    # 时间戳使用 '-' 分隔 H/M/S 而非 ':'，保证 Windows 文件系统兼容
    # (Windows 禁用文件名字符: < > : " / \ | ? *)
    parser.add_argument("--timestamp", default=datetime.now().strftime("%Y-%m-%d_%H-%M-%S"))
    parser.add_argument("--base-dir", default=".claude/runs")
    args = parser.parse_args()

    base_dir = args.base_dir
    os.makedirs(base_dir, exist_ok=True)

    dir_name = build_dir_name(args.intent, args.timestamp)
    run_dir = ensure_unique_dir(base_dir, dir_name)
    os.makedirs(run_dir, exist_ok=True)
    print(run_dir)


if __name__ == "__main__":
    main()

