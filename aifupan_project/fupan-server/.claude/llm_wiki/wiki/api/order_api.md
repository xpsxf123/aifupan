<!-- module: order -->
<!-- area: api -->
<!-- generated-by: reverse-scan + field-level enhancement -->
<!-- last-scan: 2026-05-20 -->
<!-- source-paths: replay-order/, replay-api/.../controller/order/, replay-generic/.../vo/order/ -->

# Order API -- 接口契约

> `R<T>` envelope: `wiki/frontend-api/_response_envelope.md`; 错误码: `wiki/frontend-api/_error_codes.md`。

---

## 一、鉴权与公共约定

- 所有 Controller 带 `@CrossOrigin`；登录态由全局拦截器解析。`@UserLock` 防同一用户并发。
- 分页 ListBo 继承 `PageBo`(`page/pageSize`)。
- **价格单位**: `originalPrice/realPrice/payMoney/totalPrice/discountRate/price` 均为**分**。
- 错误码: `BusinessException(StatusCode.OPERATION_EX.getCode(), msg)` / `R.error(StatusCode.BASE_VALID_PARAM.getCode(), msg)`。

### 通用枚举

| 枚举 | 值 | 含义 |
|---|---|---|
| orderType | 0/1/2/3/4/5/6/7 | 免费/升级/免费换收费/续费/增量包/版本活动/编辑/商品活动 |
| orderStatus | 0/1/2/3/4/5/6/7/8 | 未支付/未开始/生效中/已过期/已退款/已结束(升级)/超时关闭/冻结/手动取消 |
| commodityType | 0/1/2/3 | 增量包/正常版本(月底重置)/活动版本(不重置)/邀请活动 |
| validityUnit | 0/1/2/3/4/5 | 小时/天/月/季度/半年/年 |
| payStatus | 0/1/2/3 | 未支付/已支付/超时关闭/已退款 |
| payType | 0/1 | 微信/支付宝 |
| invCodeType | 0/1/2 | 机构码/个人码/激活码 |
| packageType | 1/2 | 主要套餐/次要套餐 |
| commodityTypeCode(资产) | vipLevel, analysisTime, monitorNum, anchorNum, wordMarkTime, storageNum, aiAnalysisTime, videoTaggingTime, textTaggingWordCount, subAccountCount, child_monitorNum, child_anchorNum | |

---

## 二、核心数据模型速查

### 2.1 订单 OrderVo

> id, userId, userName, commodityId, commodityName, title, status, orderType, commodityType, level, quantity, originalPrice(分), discount(BigDecimal), discountRate(分), realPrice(分), totalPrice(分), expiration, expirationUnit, payDate, startDate, endDate, realEndDate, isCommission(0/1), beforeUpgrading, afterUpgrading, source(0:正常下单/1:手工/2:邀请/3:邀请码), trialOrder(0/1), createId/updateId, createName/updateName

- **OrderListVo**: 追加 `commission`(Double), `commissionMoney`(Integer/分), `commissionType`(Integer 0:新签 1:续费)
- **OrderInfoVo**: 追加 `orderDetailList`(List\<OrderDetailInfoVo\>), `orderPay`(OrderPayInfoVo), `planEndDate`, `orderExtend`(OrderExtendInfoVo), +佣金字段

### 2.2 订单明细 OrderDetailVo

> id, orderId, commodityId, commodityName, commodityTypeId, commodityTypeCode, commodityTypeName, commodityTypeUnit, commodityTypeReset, totalNumber, startDate, resetNum/Unit, resetDate, nextReset, expirationDate, status(0:未开始/1:生效中/2:已过期/3:已失效)

### 2.3 支付 OrderPayVo

> id, orderId, payStatus, payType, payMoney(分), thirdOrderNum, payDate, payCode, refundMoney, refundStatus(0:未申请/1:中/2:成功/3:失败), refundErrorReason

OrderPayInfoVo 追加: `urlCode`, `title`

### 2.4 订单扩展 OrderExtendVo

> id, orderId, payPictures(逗号分隔,最多25张), payPictureList, fileList(List\<FileShowVo\>), remarks, createId/updateId

### 2.5 商品 CommodityVo

> id, name, status(0:未上架/1:已上架), isGive(0/1), commodityTypeId, number

- **CommodityListVo**: 追加 `commodityTypeCode`, `commodityTypeName`, `commodityTypeUnit`
- **CommodityInfoVo**: 追加上3项 + `commodityType`(CommodityTypeVo) + `commodityPriceList`(List\<CommodityPriceVo\>)

### 2.6 商品价格 CommodityPriceVo

> id, type(0:商品/1:套餐), commodityId, originalPrice(分), discount(BigDecimal 0-1), realPrice(分), validityNum, validityUnit, showStatus(0:不显示/1:显示), trialVersion(0/1)

### 2.7 商品类型 CommodityTypeVo

> id, name, code(资产key), unit, isReset, resetNum, resetUnit, subAccountHave(0/1)

### 2.8 套餐 PackageVo

> id, name, level, isCompress, packageType(1:主要/2:次要), resetUse, resetNum/Unit, status(0:未上架/1:已上架), isGive, description, logoImgs, websiteLogoImages, customizeType(0:系统/1:自定义)

PackageListVo/InfoVo 追加: `typeConsumptionList`(List\<TypeConsumptionVo\>), `commodityPriceList`, `incrementList`, `logoImgList`/`websiteLogoImagesList`(List\<FileShowVo\>)

### 2.9 套餐资源 TypeConsumptionVo

> id, commodityTypeId, commodityTypeCode/Name/Unit, commodityTypeReset, sourceId, type(0:商品/1:订单详情), number

### 2.10 邀请码批次 InvitationCodeBatchVo

> id, name, commodityId/Name, commodityLevel, commodityPriceId, commodityRealPrice(分), commodityValidityNum/Unit, price(分), commodityType(0:包月/1:到期失效), isInfinite, type(0:机构/1:个人/2:激活), quantity, validityStartDate/EndDate, status(0:正常/1:禁用), isLssued, isGratis, remarks, channelId

- **BatchListVo**: 追加 `packageName`, `codeCount`/`useCodeCount`/`notUseCodeCount`, `channelName`
- **BatchInfoVo**: 追加 `codeList`(List\<InvitationCodeInfoVo\>)

### 2.11 邀请码 InvitationCodeVo

> id, batchId, code, useStatus(0:未用/1:已用), useDate, userId, validityStartDate/EndDate, status(0:正常/1:禁用), orderId

- **ListVo**: 追加 `batchName`, `packageName`, `userName`, `channelName`
- **InfoVo**: 追加 `userName`, `userPhone`, `invitationCodeBatchVo`

### 2.12 用户资产类型 UserPropertyTypeVo

> id, userId, parentId, propertyId, commodityTypeId/Code/Name/Unit, commodityTypeReset, useQuantity, totalQuantity, totalUseQuantity

### 2.13 资产明细 UserPropertyDetailsVo

> id, userId, userName, parentUserId, orderDetailId, propertyId, commodityTypeId/Code/Name/Unit, quantity, signs(0:减/1:加), remarks, assetCreationType(0:用户/1:系统)

### 2.14 PackageUserVo

> id, packageId, userId, tenantId, phone, nickName

### 2.15 客户端资产 ClintPackageAssetsVo

> packageVersion, expirationDate, nextUpdateTime, dataList(List\<ClintGetPackageDataVo\>: useQuantity/remUseQuantity/totalQuantity + 套餐/增量包分组)

---

## 三、接口详解

### 3.1 Order (订单)

#### POST `/replay/order/createOnlineOrder` -- 在线支付
- Auth: token + `@UserLock`
- Body `OnlinePayOrderBo`: `commodityId`(Long), `commodityPriceId`(Long), `payType`(Integer), `level`(Integer)
- Response `CreateOrderVo`: `title`, `orderId`, `orderPayId`, `urlCode`(支付二维码), `payMoney`(分), `payType`

#### POST `/replay/order/pcUpgradeOrder` -- 版本升级
- Body `CreateClientOrder`: `userId`, `commodityId`(目标套餐), `commodityPriceId`, `commodityType`, `discountRate`(分), `payType`, `beforeUpgrading`, `trialOrder`, `payPictures`(逗号分隔)
- 校验: 目标 `level` > 当前版本 `level`
- `pcRenewalOrder`/`pcIncrementsOrder` 使用相同的 `CreateClientOrder` Body

#### POST `/replay/order/createOrder` -- 通用创建
- Body `CreateOrderBo`(完整): `commodityId`, `title`, `commodityPriceId`, `priceVo`(CommodityPriceInfoVo,无priceId时传), `userId`, `userName`, `orderType`, `commodityType`, `discountRate`, `source`, `beforeUpgrading`, `payType`, `trialOrder`, `payPictures`

#### GET `/replay/order/userVersionOrder` -- 用户版本订单
- Query: `userId: Long`(可选)
- Response `UserVersionOrderVo`: `commodityName`, `startDate/endDate`, `totalDay/surplusDay`, `surplusAmount`(可抵扣/分), `level`, `originalPrice/discountRate/realPrice`, `packageLogoUrl`

#### 其他 Order API 一览
| API | In | Out | 说明 |
|---|---|---|---|
| GET `.../info` | `id: Long` | `R<OrderInfoVo>` | 订单详情 |
| GET `.../getOrderByUserId` | `userId: Long` | `R<List<OrderInfoVo>>` | 用户在用订单 |
| GET `.../currentUserStayOrder` | `userId: Long`(可选) | `R<OrderInfoVo>` | 待支付订单 |
| GET `.../orderStop` | `orderId: Long` | `R<String>` | 取消(status=8) |
| GET `.../closeOrder` | `orderId: Long` | `R<String>` | 关闭 |
| GET `.../delete` | `id: Long` | `R<String>` | 软删 |
| GET `.../newPcCreateOrder` | `userId: Long` | `R<String>` | 首次下单初始化 |
| GET `.../updateFreeVersion` | `userId: Long` | `R<String>` | @UserLock,激活->免费 |
| GET `.../invitationCodeCreateOrder` | `userId, packageId, commodityPriceId: Long` | `R<String>` | 邀请码下单 |
| POST `.../orderEdit` | `CreateClientOrder` | `R<String>` | 后台改单 |
| POST `.../save` | `OrderBo`(同OrderVo字段) | `R<String>` | 后台新增 |
| POST `.../update` | `OrderBo` | `R<String>` | 后台修改 |
| POST `.../list` | `OrderListBo` | `PageUtils<OrderListVo>` | 分页 |

`OrderListBo` 查询参数: `title`, `userId`, `userName`, `commodityId`, `id`, `status`, `statusList`, `orderType`, `source`, `startDate`, `endDate`, `createId`

---

### 3.2 Commodity (商品)

| API | In | Out | 说明 |
|---|---|---|---|
| POST `.../list` | `CommodityListBo`(name,commodityTypeId,status) | `PageUtils<CommodityListVo>` | 分页 |
| POST `.../listAll` | 同上 | `List<CommodityInfoVo>` | 全量 |
| POST `.../listTypeAll` | 同上 | `List<CommodityTypeDataVo>` | 含类型分组 |
| GET `.../info` | `id: Long` | `CommodityInfoVo` | 详情 |
| POST `.../save` `.../update` `.../saveOrUpdate` | `CommodityBo` | `R<String>` | CRUD |
| GET `.../delete` | `id: Long` | `R<String>` | 软删 |
| GET `.../isDeletePriceId` | `priceId: Long` | `R<Boolean>` | 价格是否可删 |

`CommodityBo`: `id`, `name`(@NotNull), `status`, `isGive`, `commodityTypeId`, `number`, `commodityPriceList`(List\<CommodityPriceBo\>)

**CommodityTypeDataVo** (listTypeAll 响应): `commodityTypeId/code/name/unit` + `commodityListVo`(List\<CommodityInfoVo\>)

---

### 3.3 CommodityType (商品类型)

| API | In | Out |
|---|---|---|
| POST `.../list` | `CommodityTypeListBo`(keyword) | `PageUtils<CommodityTypeListVo>` |
| GET `.../info` | `id: Long` | `CommodityTypeInfoVo` |
| POST `.../save` `.../update` | `CommodityTypeBo`(同2.7) | `R<String>` |
| GET `.../delete` | `id: Long` | `R<String>` |
| GET `.../synchronousUserAssets` | `id: Long`(可选) | `R<String>` |

---

### 3.4 Package (套餐/版本)

| API | In | Out | 说明 |
|---|---|---|---|
| POST `.../list` | `PackageListBo`(name,status,packageType,customizeType,phone,userIds) | `PageUtils<PackageListVo>` | 默认customizeType=SYSTEM |
| GET `.../trialVersionList` | `userId: Long` | `List<PackageListVo>` | 试用版本 |
| POST `.../websiteList` | `userId: Long`(可选) | `List<PackageListVo>` | 官网版本 |
| GET `.../info` | `id: Long` | `PackageInfoVo` | 详情 |
| POST `.../saveOrUpdate` | `PackageSaveBo` | `R<String>` | 新增/修改 |
| GET `.../delete` | `id: Long` | `R<String>` | -- |
| GET `.../canPurchasePackage` | `userId: Long`(必填) | `List<PackageInfoVo>` | -- |
| GET `.../userRenewal` | `userId: Long`(必填) | `List<PackageInfoVo>` | -- |
| GET `.../incrementByPackageId` | `userId: Long` | `PackageInfoVo` | -- |
| GET `.../synchronousPackage` | `id: Long` | `R<String>` | -- |

**PackageSaveBo**(extends PackageBo): `name`(@NotNull), `packageType/level/isCompress/resetUse/resetNum/Unit/status/isGive`, `description`, `logoImgs/websiteLogoImages`, `customizeType`, `typeConsumptionList`, `commodityPriceList`, `logoImgList/websiteLogoImagesList`(List\<FileShowVo\>), `incrementList`

---

### 3.5 PackageUser (自定义版本用户)

| API | In | Out |
|---|---|---|
| POST `.../queryPage` | `PackageUserListBo`(packageId @NotNull, keyword) | `PageUtils<PackageUserVo>` |
| POST `.../adds` | `PackageUserAddsBo`(packageId @NotNull, phones @NotBlank 逗号分隔) | `R<String>` |
| POST `.../deleteByIds` | `List<Long>` ids @NotEmpty | `R<String>` |

---

### 3.6 InvitationCodeBatch (邀请码批次)

| API | In | Out |
|---|---|---|
| POST `.../list` | `InvitationCodeBatchListBo`(keyword,status,notTypeList,type,channelId) | `PageUtils<InvitationCodeBatchListVo>` |
| GET `.../info` | `id: Long` | `InvitationCodeBatchInfoVo` |
| GET `.../invitationByBatchId` | `batchId: Long` | `List<InvitationCodeListVo>` |
| GET `.../checkOnlyActivationCode` | -- | `InvitationCodeBatchInfoVo` |
| POST `.../save` `.../update` | `InvitationCodeBatchBo`(同2.10) | `R<String>` |
| GET `.../delete` | `id: Long` | `R<String>` |
| GET `.../getTypeConsumptionById` | `id: Long` | `List<TypeConsumptionInfoVo>` |

批次 save 副作用: 写 `tb_invitation_code_batch`，按 `quantity` 生成 `tb_invitation_code` 行并快照价格/等级/有效期。

---

### 3.7 InvitationCode (邀请码)

| API | In | Out |
|---|---|---|
| POST `.../list` | `InvitationCodeListBo`(code,batchId,useStatus,status,notCodeBatchIds) | `PageUtils<InvitationCodeListVo>` |
| GET `.../info` | `id: Long` | `InvitationCodeInfoVo` |
| POST `.../save` `.../update` | `InvitationCodeBo`(同2.11) | `R<String>` |
| GET `.../delete` | `id: Long` | `R<String>` |
| POST `.../exportInvitation` | `List<InvitationCodeBo>` | `List<InvitationCodeListVo>` |
| POST `.../updateIsLssued` | `List<Long>` ids | `R<String>` |

---

### 3.8 UserProperty (用户资产)

| API | In | Out |
|---|---|---|
| POST `.../list` | `UserPropertyListBo`(keywords,userId,userIds,userName,nickName,phone,commodityTypeCode,packageId) | `PageUtils<UserPropertyListVo>` |
| POST `.../pagePropertyDetails` | `UserPropertyDetailsListBo`(userId,userName,parentUserId,propertyId,commodityTypeId/code,start/endCreateDate) | `PageUtils<UserPropertyDetailsListVo>` |
| GET `.../getPropertyByUserId` | `userId: Long` | `List<UserPropertyTypeInfoVo>`(缓存) |
| GET `.../getPropertyByPropertyId` | `propertyId: Long` | `List<UserPropertyTypeInfoVo>`(DB) |
| POST `.../save` `.../update` | `UserPropertyBo`(userId,isUse,parentId,parentUserId,type) | `R<String>` |
| GET `.../delete` | `id: Long` | `R<String>` |
| GET `.../checkUserPropertyAndCreate` | -- | `R<String>` |
| GET `.../seleTokenInfo` | `userId: Long` | `UserPropertyTypeCacheDto` |
| POST `.../statisticsUserProperty` | `List<Long>` userIds | `R<String>` 重算 |
| POST `.../refreshProperty` | -- | `R<String>` 刷新缓存 |
| GET `.../aiTokenUseRecordByDetailId` | `propertyDetailsId: Long` | `List<AiTokenUseRecordInfoVo>` |
| GET `.../clintGetData` | -- | `ClintPackageAssetsVo` |

**UserPropertyListVo** (extends UserPropertyVo): 追加 `userName, nickName, phone, userType, packageName, packageId, packageLevel, expirationTime, userPropertyTypeList`(List\<UserPropertyTypeInfoVo\>)

#### POST `.../saveUserPropertyDetails` -- 扣减资产
- Body `AssetsMinusOrPlusBo`:
  | Field | Type | Meaning |
  |---|---|---|
  | userId | Long | 用户ID |
  | code | String | 资产code(见枚举) |
  | num | Long | 扣减量(>0,内部取负) |
  | redisId | Long | Redis缓存ID |
  | withholdId | String | 预扣ID |
  | deductionType | Integer | 0:正常 1:多退少扣完 |
  | aiTokenIds | List\<Long\> | aiToken记录ID |
  | remarks | String | 备注 |
- 校验: `num <= 0` -> `R.error(3001, "使用的量不能小于0")`

#### POST `.../updateUserPropertyDetails` -- 加回资产
- Body 同上；`num` 自动取正。

---

### 3.9 OrderPay (支付信息)

CRUD: POST list(GET info) + POST save/update + GET delete。参数 `OrderPayBo`(同2.3), 列表 `OrderPayListBo`(payStatus)。

---

### 3.10 OrderExtend (订单扩展)

CRUD: POST list(GET info) + POST save/update + GET delete。Body `OrderExtendBo`(orderId, payPictures 逗号分隔最多25张, remarks)。

---

### 3.11 Payment (支付/回调)

| API | In | 说明 |
|---|---|---|
| POST `/replay/pay/alipay` | `orderId: Long` | 内部调用,返回支付宝支付链接 |
| POST `/replay/pay/notify` | HttpServletRequest | 支付宝异步通知,验签后更新订单 |

---

## 四、出站契约（CRM 订单事件）

| 触发 | eventType |
|---|---|
| `successOrder` + orderType=1 | `UPGRADE` |
| `successOrder` + orderType=3 | `RENEWAL` |
| `successOrder` + (orderType=4 或 commodityType=0) | `INCREMENT` |
| `successOrder` + 其他 | `PAY_SUCCESS` |

---

## 五、Hard Constraint 检查

- Controller 返回 `R<T>`: 全模块满足
- 构造器注入: OrderController/PackageUserController 用 `@AllArgsConstructor`(正确)；其余 Controller 用 `@Resource`(历史遗留,新代码禁止)
- `@Transactional(rollbackFor = Exception.class)`: Bll/Producer 层落实
- `@TableLogic` 禁用: `InvitationUsageRecordEntity` 历史遗留
- ID: Producer 层 `SnowflakeManager.nextValue()`
