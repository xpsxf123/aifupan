---
name: 'scaffold-http'
description: 'Scaffolds the HTTP request layer (Axios config, interceptors) and generates API modules. Invoke when setting up networking or adding new APIs.'
---

# HTTP Layer Scaffolder

This skill helps scaffold the HTTP request layer and generate API modules following the project's standard architecture.

## Capabilities

1.  **Initialize HTTP Layer**: Generates the base configuration and directory structure.
2.  **Add API Module**: Creates a new API definition file.

## Rules

Reference: `.trae/rules/http-rules.md`

## 1. Initialize HTTP Layer

When the user asks to "setup http", "init networking", or similar, follow the guide below to create `src/http/`, `src/http/api/`, and `src/http/httpConfig/`.

IMPORTANT: You MUST follow `references/setup-guide.md` section "1. Initialize HTTP Layer" for the exact code implementation.

## 2. Add API Module

When the user asks to "add user api", "create order api", etc., follow the guide below to create `src/http/api/{name}.js` and update the index.

IMPORTANT: You MUST follow `references/setup-guide.md` section "2. Add API Module" for the code template and registration steps.
