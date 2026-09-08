<!-- module: order -->
<!-- area: data -->
<!-- generated-by: reverse-scan -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-order/src/main/java/com/jiuyu/replay/order/entity/ -->
<!-- parent: ./order_data.md -->

# Order Data Detail — 完整 DDL 与字段说明

> 本文为 `order_data.md` 的 DDL 补充文件，包含 20 张表的完整建表语句和索引细节。先读 `order_data.md` 了解全域结构，再按需查阅本文。

---

## 一、商品配置域（3 张）

### `tb_commodity` — 商品

```sql
CREATE TABLE tb_commodity (
  id BIGINT NOT NULL,                       -- 雪花 ID
  name VARCHAR(255) COMMENT '商品名',
  status TINYINT COMMENT '0未上架 1已上架',
  is_give TINYINT COMMENT '0否 1是 是否赠送商品',
  commodity_type_id BIGINT COMMENT '商品类型id',
  number BIGINT COMMENT '商品数量',
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_commodity_type` — 商品类型

```sql
CREATE TABLE tb_commodity_type (
  id BIGINT NOT NULL,
  name VARCHAR(255),
  code VARCHAR(64) COMMENT '资源 code, 见 OrderEnums.commodityTypeCode',
  unit VARCHAR(32),
  is_reset TINYINT COMMENT '0否 1是 是否周期重置',
  reset_num INT,
  reset_unit TINYINT COMMENT '0小时 1天 2月 3季度 4半年 5年',
  sub_account_have TINYINT COMMENT '子账号是否拥有 0否 1有',
  is_deleted TINYINT,
  create_date DATETIME,
  update_date DATETIME,
  PRIMARY KEY (id)
);
```

### `tb_commodity_price` — 商品/套餐价格档位

```sql
CREATE TABLE tb_commodity_price (
  id BIGINT NOT NULL,
  type TINYINT COMMENT '0商品 1套餐',
  commodity_id BIGINT,
  original_price INT COMMENT '原价 单位:分',
  discount DECIMAL(5,4) COMMENT '0-1',
  real_price INT COMMENT '真实价 单位:分',
  validity_num INT,
  validity_unit TINYINT COMMENT '0小时 1天 2月 3季度 4半年 5年',
  show_status TINYINT COMMENT '0官网隐藏 1官网显示',
  trial_version TINYINT COMMENT '0正式 1试用',
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

---

## 二、套餐域（4 张）

### `tb_package` — 套餐（用户版本）

```sql
CREATE TABLE tb_package (
  id BIGINT NOT NULL,
  name VARCHAR(255),
  level INT COMMENT '升级等级,只能从低到高',
  is_compress TINYINT COMMENT '0否 1是',
  package_type TINYINT COMMENT '1主要 2次要',
  reset_use TINYINT COMMENT '是否循环重置用量',
  reset_num INT,
  reset_unit TINYINT,
  status TINYINT COMMENT '0未上架 1已上架',
  is_give TINYINT,
  description TEXT,
  logo_imgs TEXT COMMENT 'logo 图片列表',
  website_logo_images TEXT,
  customize_type TINYINT COMMENT '0系统 1自定义',
  is_deleted TINYINT,
  create_date DATETIME,
  update_date DATETIME,
  PRIMARY KEY (id)
);
```

### `tb_package_user` — 自定义版本-用户关联

```sql
CREATE TABLE tb_package_user (
  id BIGINT NOT NULL,
  package_id BIGINT,
  user_id BIGINT,
  tenant_id BIGINT,
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_increment` — 增量包

```sql
CREATE TABLE tb_increment (
  id BIGINT NOT NULL,
  commodity_id BIGINT,
  package_id BIGINT,
  commodity_price_id BIGINT,
  discount DECIMAL(5,4),
  real_price DECIMAL(10,2),                -- 注意：tb_increment 用 DECIMAL，不是 INT 分
  status TINYINT,
  create_date DATETIME,
  -- 无 update_date / is_deleted
  PRIMARY KEY (id)
);
```

> ⚠️ `tb_increment.real_price` 是 `DECIMAL`，与 `tb_commodity_price.real_price` (`INT 分`) **类型不一致**——可能是历史遗留。读写时校验单位。

### `tb_type_consumption` — 商品类型用量配置（配方表）

```sql
CREATE TABLE tb_type_consumption (
  id BIGINT NOT NULL,
  commodity_type_id BIGINT,
  commodity_type_code VARCHAR(64),
  commodity_type_name VARCHAR(255),
  commodity_type_unit VARCHAR(32),
  commodity_type_reset TINYINT,
  source_id BIGINT COMMENT 'type=0:commodity_id, type=1:order_detail_id',
  type TINYINT COMMENT '0商品 1套餐 2邀请码',
  number BIGINT,
  PRIMARY KEY (id)
);
```

---

## 三、订单域（4 张）

### `tb_order` — 订单主表

```sql
CREATE TABLE tb_order (
  id BIGINT NOT NULL,
  user_id BIGINT,
  user_name VARCHAR(255),
  commodity_id BIGINT,
  commodity_name VARCHAR(255),
  title VARCHAR(255),
  status TINYINT COMMENT '0未支付 1未开始 2生效 3过期 4已退款 5升级失效 6超时关闭 7冻结 8手动取消',
  order_type TINYINT COMMENT '0免费 1升级 2免费转付 3续费 4增量 5版本活动 6编辑 7商品活动',
  commodity_type TINYINT COMMENT '0增量 1正常版本 2活动版本 3邀请活动',
  level INT,
  quantity INT,
  original_price INT COMMENT '单位:分',
  discount DECIMAL(5,4),
  discount_rate INT COMMENT '套餐升级差价 单位:分',
  real_price INT,
  total_price INT,
  expiration INT,
  expiration_unit TINYINT,
  pay_date DATETIME,
  start_date DATETIME,
  end_date DATETIME,
  real_end_date DATETIME COMMENT '真实过期时间(升级后回算)',
  is_commission TINYINT,
  before_upgrading BIGINT COMMENT '升级前订单 id',
  after_upgrading BIGINT COMMENT '升级后订单 id',
  source TINYINT COMMENT '0正常 1手动 2邀请成功 3邀请码',
  trial_order TINYINT,
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_order_detail` — 订单资源行

```sql
CREATE TABLE tb_order_detail (
  id BIGINT NOT NULL,
  order_id BIGINT,
  commodity_id BIGINT,
  commodity_name VARCHAR(255),
  commodity_type_id BIGINT,
  commodity_type_code VARCHAR(64),
  commodity_type_name VARCHAR(255),
  commodity_type_unit VARCHAR(32),
  commodity_type_reset TINYINT,
  total_number BIGINT,
  start_date DATETIME,
  reset_num INT,
  reset_unit TINYINT,
  reset_date DATETIME,
  next_reset DATETIME,
  expiration_date DATETIME,
  status TINYINT COMMENT '0未开始 1生效 3已失效 4冻结',
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_order_pay` — 订单支付

```sql
CREATE TABLE tb_order_pay (
  id BIGINT NOT NULL,
  order_id BIGINT,
  pay_status TINYINT COMMENT '0未支付 1已支付 2失败 3已退款 4取消',
  pay_type TINYINT COMMENT '0微信 1支付宝',
  pay_money INT COMMENT '单位:分',
  third_order_num VARCHAR(128),
  pay_date DATETIME,
  pay_code TEXT COMMENT '支付二维码',
  third_callback_content LONGTEXT,
  refund_order_num VARCHAR(128),
  refund_third_order_num VARCHAR(128),
  refund_money INT,
  refund_status TINYINT COMMENT '0未申请 1申请中 2成功 3失败',
  refund_error_reason VARCHAR(500),
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_order_extend` — 订单扩展（含 `BaseEntity`）

```sql
CREATE TABLE tb_order_extend (
  id BIGINT NOT NULL,
  order_id BIGINT,
  pay_pictures TEXT COMMENT '付费截图 id 列表,逗号分隔,最多25张',
  remarks VARCHAR(500),
  create_id BIGINT,
  update_id BIGINT,
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

> `OrderExtendEntity extends BaseEntity` —— `BaseEntity` 提供 `create_id / update_id`。这是 order 模块**唯一** extends `BaseEntity` 的业务实体。

---

## 四、邀请码域（3 张）

### `tb_invitation_code_batch`

```sql
CREATE TABLE tb_invitation_code_batch (
  id BIGINT NOT NULL,
  name VARCHAR(255),
  commodity_id BIGINT,
  commodity_name VARCHAR(255),
  commodity_level INT,
  commodity_price_id BIGINT,
  commodity_real_price INT COMMENT '单位:分',
  commodity_validity_num INT,
  commodity_validity_unit TINYINT,
  price INT COMMENT '邀请码单价 单位:分',
  commodity_type TINYINT COMMENT '0正常包月 1到期失效',
  is_infinite TINYINT,
  resource_type TINYINT COMMENT '0后台创建',
  user_id BIGINT COMMENT '创建人',
  type TINYINT COMMENT '0机构 1个人 2激活',
  quantity INT,
  validity_start_date DATETIME,
  validity_end_date DATETIME,
  status TINYINT COMMENT '0正常 1禁用',
  is_lssued TINYINT COMMENT '是否已下发',
  is_gratis TINYINT COMMENT '0否 1是 免费',
  remarks VARCHAR(500),
  channel_id BIGINT,
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_invitation_code`

```sql
CREATE TABLE tb_invitation_code (
  id BIGINT NOT NULL,
  batch_id BIGINT,
  code VARCHAR(64),
  use_status TINYINT COMMENT '0未使用 1已使用',
  use_date DATETIME,
  user_id BIGINT COMMENT '未使用时为0',
  validity_start_date DATETIME,
  validity_end_date DATETIME,
  status TINYINT COMMENT '0正常 1禁用',
  order_id BIGINT COMMENT '使用后关联订单 id',
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  is_lssued TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_invitation_usage_record` — ⚠️ 唯一使用 `@TableLogic` 的实体

```sql
CREATE TABLE tb_invitation_usage_record (
  id BIGINT NOT NULL,
  invitation_code_batch_id BIGINT NOT NULL,
  invitation_code_id BIGINT NOT NULL,
  invitation_code VARCHAR(64) NOT NULL,
  tenant_id BIGINT DEFAULT 0,
  user_id BIGINT,
  order_id BIGINT,
  create_id BIGINT NOT NULL,
  create_date DATETIME NOT NULL,            -- @TableField(fill = INSERT)
  update_id BIGINT,                         -- @TableField(fill = INSERT_UPDATE)
  update_date DATETIME,                     -- @TableField(fill = INSERT_UPDATE)
  is_deleted TINYINT DEFAULT 0,             -- @TableLogic (与项目约定冲突,历史遗留)
  PRIMARY KEY (id)
);
```

---

## 五、用户资产域（4 张）

### `tb_user_property` — 资产容器

```sql
CREATE TABLE tb_user_property (
  id BIGINT NOT NULL,
  user_id BIGINT,
  is_use TINYINT COMMENT '0未使用 1使用',
  parent_id BIGINT COMMENT '资产 id',
  parent_user_id BIGINT COMMENT '父用户 id(子账号场景)',
  type TINYINT COMMENT '0自有 1子用户共享',
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_user_property_type` — 用户-资产类型总账

```sql
CREATE TABLE tb_user_property_type (
  id BIGINT NOT NULL,
  user_id BIGINT,
  parent_id BIGINT,
  property_id BIGINT,
  commodity_type_id BIGINT,
  commodity_type_code VARCHAR(64),
  commodity_type_name VARCHAR(255),
  commodity_type_unit VARCHAR(32),
  commodity_type_reset TINYINT COMMENT '0否 1是 月底清零',
  use_quantity BIGINT COMMENT '当前周期已用',
  total_quantity BIGINT COMMENT '当前周期总数',
  total_use_quantity BIGINT COMMENT '历史累计使用',
  create_date DATETIME,
  update_date DATETIME,
  is_deleted TINYINT,
  PRIMARY KEY (id)
);
```

### `tb_user_property_details` — 资产消费流水（无 isDeleted）

```sql
CREATE TABLE tb_user_property_details (
  id BIGINT NOT NULL,
  user_id BIGINT,
  user_name VARCHAR(255),
  parent_user_id BIGINT,
  order_detail_id BIGINT,
  property_id BIGINT,
  type_surplus_id BIGINT,
  commodity_type_id BIGINT,
  commodity_type_code VARCHAR(64),
  commodity_type_name VARCHAR(255),
  commodity_type_unit VARCHAR(32),
  quantity BIGINT,
  signs TINYINT COMMENT '0减 1加',
  remarks VARCHAR(500),
  asset_creation_type TINYINT COMMENT '0用户 1系统',
  create_date DATETIME,
  PRIMARY KEY (id)
);
```

### `tb_type_surplus` — 资产批次（运行时）

```sql
CREATE TABLE tb_type_surplus (
  id BIGINT NOT NULL,
  commodity_type_id BIGINT,
  commodity_type_code VARCHAR(64),
  property_id BIGINT,
  user_id BIGINT,
  order_detail_id BIGINT,
  use_number BIGINT,
  total_number BIGINT,
  start_time DATETIME,
  end_time DATETIME,
  use_status TINYINT COMMENT '0使用中 1已用完 2弃用 3过期 4冻结',
  time_status TINYINT COMMENT '0生效 1已过期 2停用 3冻结',
  PRIMARY KEY (id)
);
```

---

## 六、AI 计费域（2 张）

### `tb_ai_token_use_record` — AI 模型调用流水

```sql
CREATE TABLE tb_ai_token_use_record (
  id BIGINT NOT NULL,
  tenant_id BIGINT,
  user_id BIGINT,
  request_source_type TINYINT COMMENT '0客户端 1运营端',
  use_source_type TINYINT COMMENT '0视频 1文件 2对比 3截图 4文案视频 5主播关键词',
  use_source_id VARCHAR(64),
  assistant_type TINYINT COMMENT '0运营 1违规 2/4弹幕 3截图',
  model_name VARCHAR(64),
  request_id VARCHAR(128),
  finish_reason VARCHAR(32) COMMENT 'stop/length/content_filter/tool_calls',
  prompt_tokens INT,
  completion_tokens INT,
  image_tokens INT,
  audio_tokens INT,
  video_tokens INT,
  cached_tokens INT,
  reasoning_tokens INT,
  total_tokens INT,
  update_date DATETIME,
  create_date DATETIME,
  remarks VARCHAR(500),
  PRIMARY KEY (id)
);
```

### `tb_property_details_token` — 资产明细-Token 多对多关联

```sql
CREATE TABLE tb_property_details_token (
  id BIGINT NOT NULL,
  ai_token_id BIGINT,
  property_details_id BIGINT,
  PRIMARY KEY (id)
);
```

---

## 七、索引建议（按表）

> 大部分查询走 `LambdaQueryWrapper`，未观察到显式索引声明，索引在数据库侧维护，下方为基于查询模式的建议。

| 表 | 推荐索引 | 查询场景 |
|---|---|---|
| `tb_order` | `idx_user_status (user_id, status)` | 查用户在用订单 |
| `tb_order` | `idx_status_end (status, end_date)` | 定时器扫过期订单 |
| `tb_order_detail` | `idx_order (order_id)` | 订单展开 |
| `tb_order_detail` | `idx_next_reset (next_reset, status)` | 月底重置批扫 |
| `tb_order_pay` | `uk_order (order_id)` | 1:1 |
| `tb_order_pay` | `idx_third_order (third_order_num)` | 支付回调匹配 |
| `tb_user_property_type` | `idx_user_code (user_id, commodity_type_code)` | 总账查询 |
| `tb_type_surplus` | `idx_user_code_status (user_id, commodity_type_code, use_status, time_status)` | 扣资产批次定位 |
| `tb_user_property_details` | `idx_user_create (user_id, create_date)` | 流水分页 |
| `tb_invitation_code` | `uk_code (code)` | 兑换查码 |
| `tb_ai_token_use_record` | `uk_request_id (request_id)` | 幂等回写 |

`<待补充>`：实际索引以 DBA 维护为准。
