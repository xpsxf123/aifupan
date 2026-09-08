# Implement Plan — Slice C

- [x] 1. 新建 FidelityReportDetailVo（mirror QualityReportDetailVo 13 字段 + patrol summaryJson schema）
- [ ] 2. ScriptMonitorBll 新增 fidelityReportDetail 方法（mirror qualityReportDetail）
- [ ] 3. ScriptMonitorBll.triggerReport 插入 sourceType=1 + monitorType=1 guard（AC-006）
- [ ] 4. ScriptMonitorController 新增 GET /fidelityReportDetail endpoint
- [ ] 5. mvn -pl replay-ai,replay-api compile -q
- [ ] 6. 验证编译通过
- [ ] 7. Yield to human for QA permission
- [ ] Archive openspec + changelog ← MUST be final item
