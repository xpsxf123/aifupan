# CRM 测试库真实数据导出到本地文档（通过单元测试）

## Goal

在“真实数据筛选单测”里补充本地落盘逻辑，把筛到的 ≤30 条客户数据写入本地文件，方便直接发给 SalesCoach 团队复用。

## Constraints

- 只读 DB：禁止插入/更新任何业务表
- 默认不执行：需要环境变量显式开启
- 输出到构建目录：写入 `target/crm-fixtures/`，避免误提交到仓库

## Output Files

- `target/crm-fixtures/crm_real_fixtures.json`
- `target/crm-fixtures/crm_real_fixtures.md`

## Run

- 环境变量：CRM_REAL_DATA_SCAN=true
- 运行测试：CrmSalesCoachFixtureSeedTest#queryRealDataBatchForSalesCoachFixture

