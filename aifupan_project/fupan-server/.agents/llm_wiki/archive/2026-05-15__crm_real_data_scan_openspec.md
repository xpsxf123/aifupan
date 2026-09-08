# CRM 测试库真实数据提取（通过单元测试）

## Goal

不造数据、不 mock，直接从测试库筛选一批（≤30）可用于 SalesCoach 联调的真实客户数据，并输出关键字段：

- 客户手机号
- 销售手机号
- userId
- 订单 id + 状态

## Constraints

- 只读：不写入 tb_user / tb_sales / tb_order / tb_user_details 等任何表
- 可控执行：默认跳过，需要显式开启环境变量才会跑
- 数量上限：30

## Selection Rule (in test)

从 `tb_user_details` 取 `sale_id != null` 的用户，补齐：

- `tb_user`：phone 非空
- `tb_order`：存在 `commodity_type=1` 且 `status in (1,2)` 的订单

## Run

- 环境变量：CRM_REAL_DATA_SCAN=true
- 运行测试：CrmSalesCoachFixtureSeedTest#queryRealDataBatchForSalesCoachFixture

