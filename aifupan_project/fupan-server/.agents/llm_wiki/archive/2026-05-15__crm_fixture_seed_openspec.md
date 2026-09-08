# CRM 测试库 Fixture 数据生成（通过单元测试）

## Goal

用一个可重复执行的 JUnit 测试，在 CRM 测试库生成一套 SalesCoach 联调用的 fixture 数据，覆盖 CRM 入站 5 个接口的成功路径所需数据。

## Constraints

- 默认不执行：未显式开启时自动跳过，避免 CI/本地误写库
- 可重复执行：多次运行不报错（按手机号/销售手机号/订单归属做 upsert 或复用）
- 不改生产代码、不改 DDL

## Fixture Data

- 客户
  - phone: 13800000000
  - userId: 9007199254740993（> 2^53，用于验证 Long 序列化精度风险）
- 销售
  - phone: 13900000000
- 订单
  - commodityType=1
  - status=2（生效中）

## Verification (in test)

- 调用 CrmIntegrationService 的 5 个方法并断言 `code=0`，确保成功路径可被覆盖

## Run

- 设置环境变量：CRM_FIXTURE_SEED=true
- 运行测试类：CrmSalesCoachFixtureSeedTest

