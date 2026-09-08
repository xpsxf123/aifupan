# Implement Plan — restoreDegree-slice-a

- [ ] 1. AiEnums.java — 重命名 18/19 + 新增 25/26/27 枚举值（AC-6）
- [ ] 2. StandardScriptEntity.java — 去 anchorUrlUserId + 加 secUid（AC-9）
- [ ] 3. BO 新建：GenerateStandardScriptBo / ConfirmStandardScriptBo / TimeAxisItemBo（AC-13）
- [ ] 4. VO 新建：StandardScriptVo / TimeAxisItemVo / StandardScriptDetailVo / StandardScriptConfirmVo（AC-13）
- [ ] 5. StandardScriptService 接口 + StandardScriptServiceImpl 实现（AC-11）
- [ ] 6. ScriptMonitorStandardScriptBll 新建（T21/T23/T24 + parseMarkdownTable）（AC-1/AC-2/AC-3）
- [ ] 7. AnchorUrlBll 修改 — 解除 B3:1116-1125 暂禁 + 解除 B8:2317-2324 暂禁 + 注入 StandardScriptService（AC-4/AC-5）
- [ ] 8. StandardScriptController 新建（AC-1/AC-2/AC-3）
- [ ] 9. sql/replay-34.sql — 5 条 tb_cue_words INSERT（AC-7）
- [ ] 10. sql/replay-35.sql — DDL 变更（AC-8）
- [ ] 11. mvn -pl replay-common,replay-words,replay-api compile -q
- [ ] 12. scope_guard.py 检查
- [ ] Archive openspec + changelog ← MUST be final item
