# Implement Plan — Change__2026-06-11_18-57-48 (Slice B)

- [ ] 1. 新建 StandardScriptInfoVo (replay-generic)
- [ ] 2. 新建 StandardScriptFeign SPI (replay-generic)
- [ ] 3. 新建 StandardScriptApi (replay-words)
- [ ] 4. 新建 ScriptMonitorFidelityGenerateBll (replay-ai)
- [ ] 5. 修改 ScriptMonitorMqHandler — fidelity tag 路由到 FidelityGenerateBll
- [ ] 6. 修改 ScriptMonitorBll — 解 3 处限闸 + 注入 StandardScriptFeign
- [ ] 7. 修改 ScriptMonitorApi — 加 monitorType=1 autoTrigger 路径
- [ ] 8. mvn -pl replay-generic,replay-ai,replay-words install -DskipTests
- [ ] 9. Yield to human for QA permission
- [ ] 10. Archive openspec + changelog
