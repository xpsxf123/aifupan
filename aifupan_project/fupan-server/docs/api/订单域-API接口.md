# API接口文档 - 订单域

> 基础路径: /replay

## 1. OrderController - 订单管理
**路径前缀**: `replay/order`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 订单列表 | OrderListBo | PageUtils\<OrderListVo\> |
| GET | /info | 订单信息 | id(Long) | OrderInfoVo |
| POST | /save | 新增订单 | OrderBo | String |
| POST | /update | 修改订单 | OrderBo | String |
| GET | /delete | 删除订单 | id(Long) | String |
| GET | /getOrderByUserId | 查询用户在用订单 | userId(Long) | List\<OrderInfoVo\> |
| GET | /userVersionOrder | 获取用户当前版本订单(多合一) | userId(Long,可选) | UserVersionOrderVo |
| GET | /newPcCreateOrder | 新下的订单(之前没有的) | userId(Long) | String |
| GET | /updateFreeVersion | 从激活版升级到免费版 @UserLock | userId(Long) | String |
| GET | /invitationCodeCreateOrder | 邀请码创建订单 | userId,packageId,commodityPriceId | String |
| POST | /pcUpgradeOrder | 版本升级 | CreateClientOrder | String |
| POST | /pcRenewalOrder | 版本续费 | CreateClientOrder | String |
| POST | /pcIncrementsOrder | 购买增量包 | CreateClientOrder | String |
| POST | /createOrder | 创建订单 | CreateOrderBo | String |
| GET | /orderStop | 取消订单 | orderId(Long) | String |
| POST | /orderEdit | 订单编辑-订单修改 | CreateClientOrder | String |
| POST | /createOnlineOrder | 创建在线支付订单 @UserLock | OnlinePayOrderBo | CreateOrderVo |
| GET | /currentUserStayOrder | 获取用户待支付订单 | userId(Long,可选) | OrderInfoVo |
| GET | /closeOrder | 订单关闭 | orderId(Long) | String |

## 2. OrderPayController - 订单支付信息
**路径前缀**: `replay/orderpay`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 支付信息列表 | OrderPayListBo | PageUtils\<OrderPayListVo\> |
| GET | /info | 支付信息详情 | id(Long) | OrderPayInfoVo |
| POST | /save | 新增支付信息 | OrderPayBo | String |
| POST | /update | 修改支付信息 | OrderPayBo | String |
| GET | /delete | 删除支付信息 | id(Long) | String |

## 3. CommodityController - 商品管理
**路径前缀**: `replay/commodity`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 商品列表(分页) | CommodityListBo | PageUtils\<CommodityListVo\> |
| POST | /listAll | 商品列表(全部) | CommodityListBo | List\<CommodityInfoVo\> |
| POST | /listTypeAll | 商品列表(含类型) | CommodityListBo | List\<CommodityTypeDataVo\> |
| GET | /info | 商品信息 | id(Long) | CommodityInfoVo |
| POST | /save | 新增商品 | CommodityBo | String |
| POST | /update | 修改商品 | CommodityBo | String |
| POST | /saveOrUpdate | 修改或保存商品 | CommodityBo | String |
| GET | /delete | 删除商品 | id(Long) | String |
| GET | /isDeletePriceId | 是否能删除增量包价格 | priceId(Long) | Boolean |

## 4. PackageController - 套餐管理
**路径前缀**: `replay/package`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 套餐列表(分页) | PackageListBo | PageUtils\<PackageListVo\> |
| GET | /trialVersionList | 试用版本列表 | - | List\<PackageListVo\> |
| POST | /websiteList | 官网版本列表 | userId(Long,可选) | List\<PackageListVo\> |
| GET | /info | 套餐信息 | id(Long) | PackageInfoVo |
| POST | /saveOrUpdate | 新增/修改套餐 | PackageSaveBo | String |
| GET | /delete | 删除套餐 | id(Long) | String |
| GET | /canPurchasePackage | 可购买的套餐列表 | userId(Long) | List\<PackageInfoVo\> |
| GET | /userRenewal | 用户续费套餐列表 | userId(Long) | List\<PackageInfoVo\> |
| GET | /incrementByPackageId | 用户可购买的增量包 | userId(Long) | PackageInfoVo |
| GET | /synchronousPackage | 同步版本 | id(Long) | String |

## 5. UserPropertyController - 用户资产
**路径前缀**: `replay/userproperty`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /list | 用户资产列表 | UserPropertyListBo | PageUtils\<UserPropertyListVo\> |
| POST | /pagePropertyDetails | 资产使用详情列表 | UserPropertyDetailsListBo | PageUtils\<UserPropertyDetailsListVo\> |
| GET | /getPropertyByUserId | 查询用户资产(缓存) | userId(Long) | List\<UserPropertyTypeInfoVo\> |
| GET | /getPropertyByPropertyId | 查询用户资产(数据库) | propertyId(Long) | List\<UserPropertyTypeInfoVo\> |
| POST | /save | 新增用户资产 | UserPropertyBo | String |
| POST | /update | 修改用户资产 | UserPropertyBo | String |
| GET | /delete | 删除用户资产 | id(Long) | String |
| GET | /checkUserPropertyAndCreate | 检查用户资产(无则创建) | - | String |
| GET | /seleTokenInfo | 查询用户资产 | userId(Long) | UserPropertyTypeCacheDto |
| POST | /saveUserPropertyDetails | 使用用户资产(扣减) | AssetsMinusOrPlusBo | String |
| POST | /updateUserPropertyDetails | 加回用户资产 | AssetsMinusOrPlusBo | String |
| POST | /statisticsUserProperty | 重新统计用户资产 | List\<Long\> userIds | String |
| POST | /refreshProperty | 刷新用户资产缓存 | - | String |
| GET | /aiTokenUseRecordByDetailId | 查询aiToken记录 | propertyDetailsId(Long) | List\<AiTokenUseRecordInfoVo\> |
| GET | /clintGetData | 客户端获取用户资产详情 | - | ClintPackageAssetsVo |

## 6. payController - 支付
**路径前缀**: `replay/pay`

| HTTP | 路径 | 说明 | 参数 | 返回 |
|------|------|------|------|------|
| POST | /alipay | 支付宝支付 | orderId(Long) | String |
| POST | /payment | 支付宝支付(测试) | - | String |
| POST | /notify | 支付宝回调通知 | HttpServletRequest | String |

## 7. OrderNotifyController - 订单通知回调
**路径前缀**: `replay/order`

> 微信支付回调等通知接口
