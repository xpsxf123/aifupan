package com.jiuyu.replay.api.logic.order.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.order.UserPropertyLogic;
import com.jiuyu.replay.common.constant.packageunit.CommodityTypeConvert;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.generic.vo.order.*;
import com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.*;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.dto.UserPropertyTypeCacheDto;
import com.jiuyu.replay.order.vo.*;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.constant.Constant;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 用户资产
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Service
@AllArgsConstructor
public class UserPropertyLogicImpl implements UserPropertyLogic {

    private static final Logger log = LoggerFactory.getLogger(UserPropertyLogicImpl.class);
    private final UserPropertyBll userPropertyBll;
    private final UserBll userBll;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserPropertyDetailsBll userPropertyDetailsBll;
    private final OrderBll orderBll;
    private final OrderDetailBll orderDetailBll;
    private final AnchorUrlBll anchorUrlBll;
    private final UserPropertyTypeBll userPropertyTypeBll;
    private final TypeSurplusBll typeSurplusBll;
    private final OrderFeign orderFeign;
    private final UserFeign userFeign;
    private final CommodityTypeBll commodityTypeBll;
    private final AiModelBll aiModelBll;
    private final UserPropertyFeign userPropertyFeign;


    @Override
    public R<PageUtils<UserPropertyListVo>> queryPage(UserPropertyListBo userPropertyListBo) {
        if (ObjectUtil.isEmpty(userPropertyListBo.getUserIds())) userPropertyListBo.setUserIds(new ArrayList<>());
        List<Long> UIdS = new ArrayList<>();
        // 根据版本查询
        if (ObjectUtil.isNotEmpty(userPropertyListBo.getPackageId())) {
            List<Long> userIds = orderBll.getUserIdByPackageId(userPropertyListBo.getPackageId());
            if (userIds == null || userIds.isEmpty()) {
                PageUtils<UserPropertyListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
            R<List<Long>> userChild = userBll.getUserChild(userIds);
            List<Long> data = userChild.getData();
            if (data != null && !data.isEmpty()) {
                UIdS.addAll(data);
            }
            UIdS.addAll(userIds);
        }
        // 搜索用户
        if (ObjectUtil.isNotEmpty(userPropertyListBo.getUserName()) || ObjectUtil.isNotEmpty(userPropertyListBo.getNickName()) || ObjectUtil.isNotEmpty(userPropertyListBo.getPhone()) || ObjectUtil.isNotEmpty(userPropertyListBo.getKeywords()) || ObjectUtil.isNotEmpty(UIdS)) {
            UserListBo bo = new UserListBo();
            bo.setLimit(-1);
            bo.setUserName(userPropertyListBo.getUserName());
            bo.setNickName(userPropertyListBo.getNickName());
            bo.setPhone(userPropertyListBo.getPhone());
            bo.setKeyword(userPropertyListBo.getKeywords());
            bo.setUserIds(UIdS);
            R<PageUtils<UserListVo>> pageUtilsR = userBll.selectClientList(bo);
            if (ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())) {
                userPropertyListBo.getUserIds().addAll(pageUtilsR.getData().getList().stream().map(UserListVo::getId).toList());
            } else {
                return R.ok(new PageUtils<>(new ArrayList<>(), 0, userPropertyListBo.getLimit(), userPropertyListBo.getPage()));
            }
        }
        R<PageUtils<UserPropertyListVo>> pageUtilsR = userPropertyBll.queryPage(userPropertyListBo);
        if (ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())) {
            // 获取用户信息
            List<UserPropertyListVo> list = pageUtilsR.getData().getList();
            R<List<UserListVo>> userListR = userBll.listByIds(list.stream().map(UserPropertyVo::getUserId).toList());
            if (ObjectUtil.isNotEmpty(userListR.getData())) {
                list.forEach(userPropertyListVo -> {
                    UserListVo userListVo = userListR.getData().stream().filter(vo -> vo.getId().equals(userPropertyListVo.getUserId())).findFirst().orElse(null);
                    if (ObjectUtil.isNotEmpty(userListVo)) {
                        userPropertyListVo.setUserName(userListVo.getUsername());
                        userPropertyListVo.setNickName(userListVo.getNickName());
                        userPropertyListVo.setPhone(userListVo.getPhone());
                        userPropertyListVo.setUserId(userListVo.getId());
                        userPropertyListVo.setUserType(userListVo.getUserType());
                    }
                });
            }
            List<Long> userIds = new ArrayList<>();
            List<UserListVo> userDtos = ResultUtil.getResult(userListR);
            if (userDtos != null) {
                userIds = userDtos.stream()
                        .map(item -> ObjectUtil.isNotEmpty(item.getParentId()) && item.getParentId() != 0 ? item.getParentId() : item.getId())
                        .toList();
            }
            List<OrderInfoVo> orderList = !userIds.isEmpty() ? orderFeign.currentOrderByUserIds(userIds) : null;
            Map<Long, OrderInfoVo> orderMap;
            if (orderList != null) {
                orderMap = orderList.stream().collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (o, n) -> n));
            } else {
                orderMap = new HashMap<>();
            }
            // 获取版本信息
            list.forEach(item -> {
                Long id = item.getUserId();
                Long parentId = item.getParentUserId();
                if (parentId != null && parentId != 0) id = parentId;
                //去那生效订单信息
//                OrderInfoVo order = orderFeign.currentOrderByUserId(id);
//                if (order != null) {
//                    item.setPackageName(order.getCommodityName());
//                    item.setPackageId(order.getCommodityId());
//                }

                OrderInfoVo orderInfoVo = orderMap.get(id);
                if (orderInfoVo != null) {
                    item.setPackageLevel(orderInfoVo.getLevel());
                    item.setPackageName(orderInfoVo.getCommodityName());
                    item.setPackageId(orderInfoVo.getCommodityId());
                }
            });

        }
        return pageUtilsR;
    }

    /**
     * 查询用户资产信息
     *
     * @param id 用户资产id
     * @return
     */
    @Override
    public R<List<UserPropertyTypeInfoVo>> getPropertyByUserId(Long id) {
        return R.ok(userPropertyBll.getUserProperty(id));
    }

    @Override
    public R<List<UserPropertyTypeInfoVo>> getPropertyByPropertyId(Long propertyId) {
        return userPropertyBll.getPropertyByPropertyId(propertyId);
    }

    @Override
    public R<String> save(UserPropertyBo userPropertyBo) {

        return userPropertyBll.save(userPropertyBo);
    }

    @Override
    public R<String> update(UserPropertyBo userPropertyBo) {

        return userPropertyBll.update(userPropertyBo);
    }

    @Override
    public R<String> delete(Long id) {

        return userPropertyBll.deleteByUserId(id);
    }

    @Override
    public void checkUserPropertyAndCreate() {
        R<List<UserVo>> listR = userBll.listByClientAll();
        if (ObjectUtil.isNotEmpty(listR.getData())) {
            List<Long> userIds = listR.getData().stream().map(UserVo::getId).toList();
            userPropertyBll.checkUserPropertyAndCreate(userIds, 1);
        }
    }

    @Override
    public R<UserPropertyTypeCacheDto> getUserProperty(Long userId) {
        if (userId == null) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            if (localUser == null) return R.error(400, "请先登录");
            userId = localUser.getId();
        }
        List<UserPropertyTypeInfoVo> list = userPropertyBll.getUserProperty(userId);

        UserPropertyInfoVo userProperty = new UserPropertyInfoVo();
        userProperty.setUserId(userId);
        userProperty.setUserPropertyTypeList(list);
        return R.ok(UserPropertyTypeCacheDto.create(userProperty));
    }

    @Override
    @Transactional
    public R<String> assetsMinusOrPlus(AssetsMinusOrPlusBo assets) {
        if (ObjectUtil.isEmpty(assets.getCode()) && ObjectUtil.isNotEmpty(assets.getType())) {
            switch (assets.getType()) {
                case 1:
                    assets.setCode("monitorNum");
                    break;
                case 2:
                    assets.setCode("aiAnalysisTime");
                    break;
                case 3:
                    assets.setCode("anchorNum");
                    break;
                default:
                    break;
            }
        }
        if (assets.getCode().equals("anchorNum")) return R.ok();
        if (ObjectUtil.isEmpty(assets.getUserId())) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            assets.setUserId(localUser.getId());
            assets.setUserName(localUser.getNickName());
        } else {
            R<UserInfoVo> info = userBll.info(assets.getUserId(), false);
            if (info.getCode() == 0 && info.getData() != null) {
                assets.setUserId(info.getData().getId());
                assets.setUserName(info.getData().getNickName());
            }
        }

        // 判断是否有预扣id
        if (ObjectUtil.isNotEmpty(assets.getRedisId()) || ObjectUtil.isNotEmpty(assets.getWithholdId())) {
            RedisWithholdVo redisCache = userPropertyBll.getRedisCache(assets.getWithholdId(), assets.getRedisId());
            if (ObjectUtil.isNotEmpty(redisCache) && redisCache.getNum() != 0) {
                assets.setNum(redisCache.getNum());
                assets.setClearWithholdCache(userPropertyBll::removeTempUserProperty);
            }
        }

        UserPropertyImpl.use(assets);

        return R.ok("处理成功");
    }

    @Override
    @Transactional
    public R<String> assetsMinusOrPlusReal(AssetsMinusOrPlusBo assets) {
        if (ObjectUtil.isEmpty(assets.getUserId())) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            assets.setUserId(localUser.getId());
            assets.setUserName(localUser.getNickName());
        } else {
            R<UserInfoVo> info = userBll.info(assets.getUserId(), false);
            if (info.getCode() == 0 && info.getData() != null) {
                assets.setUserId(info.getData().getId());
                assets.setUserName(info.getData().getNickName());
            }
        }
        // 获取当前的资产
        List<UserPropertyTypeInfoVo> userProperty1 = userPropertyBll.getUserProperty(assets.getUserId());
        UserPropertyTypeInfoVo userPropertyTypeInfoVo = userProperty1.stream().filter(item -> "aiTokenNum".equals(item.getCommodityTypeCode())).findAny().orElse(null);
        if (ObjectUtil.isEmpty(userPropertyTypeInfoVo)) {
            assets.setNum(0L);
        } else {
            Long currentNum = userPropertyTypeInfoVo.getTotalQuantity() - userPropertyTypeInfoVo.getUseQuantity();
            currentNum = currentNum < 0 ? 0 : currentNum;
            if (Math.abs(assets.getNum()) > Math.abs(currentNum)) {
                assets.setNum(-currentNum);
            } else {
                assets.setNum(-(Math.abs(assets.getNum())));
            }
        }
        assets.setClearWithholdCache(userPropertyBll::removeTempUserProperty);
        UserPropertyImpl.use(assets);
        return R.ok("处理成功");
    }

    @Override
    public R<IsPropertyHaveVo> isPropertyHaveAiToken(IsPropertyHaveBo bo) {
        if (ObjectUtil.isEmpty(bo.getUserId())) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            bo.setUserId(localUser.getId());
        }
        return userPropertyBll.isPropertyHaveAiToken(bo);
    }

    @Override
    public R<IsPropertyHaveVo> isHave(IsPropertyHaveBo bo) {
        if (ObjectUtil.isEmpty(bo.getUserId())) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            bo.setUserId(localUser.getId());
        }
        return userPropertyBll.isHave(bo);
    }

    @Override
    public R<String> removeTempUserProperty(String withholdId, Long redisId) {
        userPropertyBll.removeTempUserProperty(withholdId, redisId);
        return R.ok("删除成功");
    }

    @Override
    public R<PageUtils<UserPropertyDetailsListVo>> pagePropertyDetails(UserPropertyDetailsListBo bo) {
        return userPropertyDetailsBll.queryPage(bo);
    }

    @Override
    public R<String> statisticsUserProperty(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return R.error(3001, "userId不能为空");
        }
        for (Long userId : userIds) {
            userPropertyBll.statisticsUserProperty(userId);
        }

        // 更新主播数，弹幕监控位。。。
        R<List<UserInfoVo>> userList = userBll.listParentUser(userIds);
        if (ObjectUtil.isNotEmpty(userList.getData())) {
            List<UserVo> userTempList = new ArrayList<>();
            List<UserInfoVo> userData = userList.getData();
            userData.forEach(item -> {
                userTempList.add(BeanUtil.copyProperties(item, UserVo.class));
                List<UserVo> childUserList = item.getChildUserList();
                if (ObjectUtil.isNotEmpty(childUserList)) {
                    userTempList.addAll(childUserList);
                }
            });

            for (UserVo user : userTempList) {
                R<List<AnchorUrlUserVo>> listR = anchorUrlBll.clientAnchorList(user.getId(), user.getActiveTenantId());
                List<AnchorUrlUserVo> urlVos = listR.getData();
                long num = 0L;
                long anchorBarrageNum = 0L;
                if (urlVos != null && !urlVos.isEmpty()) {
                    long total = userPropertyBll.getTotalPropertyByUserIdCode(user.getId(), "anchorNum").getData();
                    long size = urlVos.stream().filter(item -> item.getIsRemoveRecord() == 0).toList().size();
                    num = Math.min(size, total);

                    anchorBarrageNum = urlVos.stream().filter(item -> item.getIsRemoveRecord() == 0 && item.getIsBarrageMonitoring() == 1).toList().size();
                }
                userPropertyBll.updateByPropertyNum(user.getId(), "anchorNum", num);
                userPropertyBll.updateByPropertyNum(user.getId(), "anchorBarrageNum", anchorBarrageNum);
            }

            // 更新子账号数量
            for (UserInfoVo userDatum : userData) {
                userPropertyBll.updateByPropertyNum(userDatum.getId(), "subAccountCount", Long.valueOf(userDatum.getChildAccountCount()));
            }
        }

        // 获取用户当前使用的套餐
        R<List<OrderInfoVo>> validOrders = orderBll.getNotExpirationOrders(userIds);
        if (ObjectUtil.isNotEmpty(validOrders.getData())) {
            List<OrderInfoVo> data = validOrders.getData();
            orderBll.setUserPackageCache(data);
        }
        return R.ok("处理成功");
    }

    @Override
    public R<String> refreshProperty() {
        userPropertyBll.addUserPropertyCache();
        // 获取用户当前使用的套餐
        R<List<OrderInfoVo>> validOrders = orderBll.getNotExpirationOrders(null);
        if (ObjectUtil.isNotEmpty(validOrders.getData())) {
            List<OrderInfoVo> data = validOrders.getData();
            orderBll.setUserPackageCache(data);
        }
        return R.ok("完成刷新");
    }

    /**
     * 查询aiToken记录列表
     *
     * @param propertyDetailsId
     * @return
     */
    @Override
    public R<List<AiTokenUseRecordInfoVo>> aiTokenUseRecordByDetailId(Long propertyDetailsId) {
        return userPropertyBll.aiTokenUseRecordByDetailId(propertyDetailsId);
    }


    /**
     * 客户端获取资产信息
     *
     * @return
     */
    @Override
    public R<ClintPackageAssetsVo> clintGetData() {
        List<CommodityTypeInfoVo> commodityTypeList = ResultUtil.getResult(commodityTypeBll.listAll());
        ClintPackageAssetsVo clintPackageAssetsVo = new ClintPackageAssetsVo();
        if (ObjectUtil.isEmpty(commodityTypeList)) {
            clintPackageAssetsVo.setDataList(new ArrayList<>());
            return R.ok(clintPackageAssetsVo);
        }
        Map<String, CommodityTypeInfoVo> commodityTypeMap = commodityTypeList.stream()
                .collect(Collectors.toMap(CommodityTypeInfoVo::getCode, commodityTypeInfo -> commodityTypeInfo, (a, b) -> a));
        //从token中获取用户id
        Long userId = null;
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (ObjectUtil.isEmpty(localUser)||localUser.getId()==null){
            throw new BusinessException( Constant.CodeMsgEnum.NO_LOGIN.getCode(),"请先登录");
        }
        userId = localUser.getId();
        //取得用户正在使用的资产id
        Long propertyId = userPropertyBll.clintGetData(userId);
        //已经是所有父id=0的数据了
        List<UserPropertyTypeListVo> propertyTypeListVoList = userPropertyTypeBll.clintGetTypeData(userId, propertyId);
        //该用户没有资产类型总明细
        if (ObjectUtil.isEmpty(propertyTypeListVoList)) {
            throw new BusinessException( 5002,"该用户没有资产类型总明细");
        }

        //总类型数据
        List<ClintGetPackageDataVo> clintGetData = Convert.toList(ClintGetPackageDataVo.class, propertyTypeListVoList);
        //根据userid获取至父类parentId=0的userId
        userId = userBll.getUserIdNoPre(userId);
        //根据用户id获取生效中的订单id
        List<Long> orderIds = orderBll.getOrdersUserIdNoPre(userId);
        if (ObjectUtil.isEmpty(orderIds)) {
            //差不到，设置默认值返回 即0用量、0剩余、0总量
            handleDef(clintGetData);
            clintPackageAssetsVo.setDataList(clintGetData);
            return R.ok(clintPackageAssetsVo);
        }
        //根据订单id获取订单总详情（其类型为非版本资产的都归为增量包资产）
        List<ClintGetDataVo> details = orderDetailBll.getDetailByOrderIds(orderIds);
        if (details.isEmpty()) {
            handleDef(clintGetData);
            clintPackageAssetsVo.setDataList(clintGetData);
            return R.ok(clintPackageAssetsVo);
        }
        //根据详情结果，查需重置的（生效中的、在使用的、用完的）资产的用量和总理
        //并且根据订单详情id设置对应的资产用量，剩余，总量
        typeSurplusBll.selectByDetailIds(details);
        //数据类型转换（分钟转小时）
        for (ClintGetDataVo detail : details) {
            //内层用量（按类型，转单位）
            BigDecimal userNumber = CommodityTypeConvert.commodityBigDecimalOnlyHour(detail.getCommodityTypeCode(), detail.getUseNumber());
            detail.setUseNumber(userNumber);
            //内层总量（按类型，转单位）
            BigDecimal totalNumber =  CommodityTypeConvert.commodityBigDecimalOnlyHour(detail.getCommodityTypeCode(), detail.getTotalNumber());
            detail.setTotalNumber(totalNumber);
            //内层剩余用量（总量-用量） 上下文已做null 处理
            BigDecimal remUseNumber = detail.getTotalNumber().subtract(detail.getUseNumber());
            //如果差值为负数，取零
            remUseNumber = remUseNumber.compareTo(BigDecimal.ZERO)<0?BigDecimal.ZERO:remUseNumber;
            detail.setRemUseNumber(remUseNumber);
        }

        // 按商品类型ID分组
        Map<String, List<ClintGetDataVo>> commodityTypeIdMap = details.stream().collect(Collectors.groupingBy(ClintGetDataVo::getCommodityTypeCode));

        clintGetData.forEach(x -> {
            CommodityTypeInfoVo commodityType = commodityTypeMap.get(x.getCommodityTypeCode());
            List<ClintGetDataVo> clintGetDataVos = new ArrayList<>();
            if (ObjectUtil.equals(localUser.getUserType(), 2) && commodityType != null && commodityType.getSubAccountHave() == 1) {
                String code = "child_" + x.getCommodityTypeCode();
                CommodityTypeInfoVo commodityTypeTemp = commodityTypeMap.get(code);
                if (commodityTypeTemp != null) {
                    clintGetDataVos = commodityTypeIdMap.get(code);
                }
            } else {
                clintGetDataVos = commodityTypeIdMap.get(x.getCommodityTypeCode());
            }
            x.setAllInnerList(clintGetDataVos);
            if (ObjectUtil.isNotEmpty(clintGetDataVos)) {
                // 按 commodityType 分组 1为版本资产  0为增量包资产
                Map<Integer, List<ClintGetDataVo>> typeGroup = clintGetDataVos.stream().collect(Collectors.groupingBy(ClintGetDataVo::getCommodityType));
                List<ClintGetDataVo> type1List = typeGroup.getOrDefault(1, Collections.emptyList());
                List<ClintGetDataVo> type0List = typeGroup.getOrDefault(0, Collections.emptyList());
                //统计总数，版本用量总数，增量包用量总数
                toCount(x, type1List, type0List);
            }
            //设置默认值
            setDefaultData(x);
        });

        for (ClintGetPackageDataVo clintGetDatum : clintGetData) {
            //总用量 按类型转单位
            BigDecimal useQuantity = CommodityTypeConvert.commodityBigDecimalOnlyHour(clintGetDatum.getCommodityTypeCode(), clintGetDatum.getUseQuantity());
            clintGetDatum.setUseQuantity(useQuantity);
            //总量  按类型转单位
            BigDecimal taxQuantity =  CommodityTypeConvert.commodityBigDecimalOnlyHour(clintGetDatum.getCommodityTypeCode(), clintGetDatum.getTotalQuantity());
            clintGetDatum.setTotalQuantity(taxQuantity);
            //总剩余用量 总量-总用量  上下文已做null 处理
            BigDecimal remUseQuantity = clintGetDatum.getTotalQuantity().subtract(clintGetDatum.getUseQuantity());
            //如果差值为负数，取零
            remUseQuantity = remUseQuantity.compareTo(BigDecimal.ZERO)<0?BigDecimal.ZERO:remUseQuantity;
            clintGetDatum.setRemUseQuantity(remUseQuantity);
        }
        clintPackageAssetsVo.setDataList(clintGetData);
        return R.ok(clintPackageAssetsVo);
    }

    @Override
    public R<ClintPackageAssetsVo> clintGetDataFormat(R<ClintPackageAssetsVo> r) {
        UserCacheVo user = GlobalObject.getLocalUser();
        if (user == null || r == null || r.getCode() != 0 || ObjectUtil.isEmpty(r.getData())) {
            return r;
        }

        // dataList
        if (ObjectUtil.isNotEmpty(r.getData().getDataList())) {
            // 去掉带child_的
            List<ClintGetPackageDataVo> dataList = r.getData().getDataList();
            Integer userType = user.getUserType();
            dataList = dataList.stream()
                    .filter(item -> {
                        if (item.getCommodityTypeCode().startsWith("child_")) {
                            return false;
                        }
                        if (ObjectUtil.equals(userType, 2) && ObjectUtil.equals(item.getCommodityTypeCode(), "subAccountCount")) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // 把主账号同时录制主播数和主账号可添加主播数修改为同时录制主播数、可添加主播数
            dataList.forEach(item -> {

                // 把主账号这三个字去掉
                if (item.getCommodityTypeName().startsWith("主账号")) {
                    item.setCommodityTypeName(item.getCommodityTypeName().substring(3));
                }
            });

            r.getData().setDataList(dataList);
        }

        return r;
    }

    /**
     * 设置无资产情况的默认值
     * @param clintGetData
     */
    private void handleDef(List<ClintGetPackageDataVo> clintGetData) {
        clintGetData.forEach(this::setDefaultData);
    }

    /**
     * 为数据设置默认值
     * @param x
     */
    private void setDefaultData(ClintGetPackageDataVo x) {
        x.setSetMenuTotalQuantity(x.getSetMenuTotalQuantity()==null? BigDecimal.ZERO:x.getSetMenuTotalQuantity());
        x.setSetMenuRemUseQuantity(x.getSetMenuRemUseQuantity()==null?BigDecimal.ZERO:x.getSetMenuRemUseQuantity());
        x.setSetMenuUseQuantity(x.getSetMenuUseQuantity()==null?BigDecimal.ZERO:x.getSetMenuUseQuantity());
        x.setIncTotalQuantity(x.getIncTotalQuantity()==null?BigDecimal.ZERO:x.getIncTotalQuantity());
        x.setIncRemUseQuantity(x.getIncRemUseQuantity()==null?BigDecimal.ZERO:x.getIncRemUseQuantity());
        x.setIncUseQuantity(x.getIncUseQuantity()==null?BigDecimal.ZERO:x.getIncUseQuantity());
        BigDecimal remUseQuantity = x.getTotalQuantity().subtract(x.getUseQuantity());
        x.setRemUseQuantity(remUseQuantity.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remUseQuantity);
        BigDecimal incRemUseQuantity = x.getIncTotalQuantity().subtract(x.getIncUseQuantity());
        x.setIncRemUseQuantity(incRemUseQuantity.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : incRemUseQuantity);
        BigDecimal setMenuRemUseQuantity = x.getSetMenuTotalQuantity().subtract(x.getSetMenuUseQuantity());
        x.setSetMenuRemUseQuantity(setMenuRemUseQuantity.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : setMenuRemUseQuantity);
    }


    /**
     * 1、统计版本资产的用量总数，剩余用量总数，版本资产总和
     * 2、统计增量包的用量总数，剩余用量总数，增量包资产总和
     * @param x
     * @param type1List
     * @param type0List
     */
    private void toCount(ClintGetPackageDataVo x, List<ClintGetDataVo> type1List, List<ClintGetDataVo> type0List) {

        // 统计 commodityType=1 的 useNumber 和 totalNumber 总和
        BigDecimal  type1UseNumberSum = (type1List.stream().map(ClintGetDataVo::getUseNumber).map(num -> num == null ? BigDecimal.ZERO : num).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal type1TotalNumberSum = (type1List.stream().map(ClintGetDataVo::getTotalNumber).map(totalNumber -> totalNumber == null ? BigDecimal.ZERO : totalNumber).reduce(BigDecimal.ZERO, BigDecimal::add));

        // 统计 commodityType=0 的 useNumber 和 totalNumber 总和
        BigDecimal type0UseNumberSum = (type0List.stream().map(ClintGetDataVo::getUseNumber).map(useNumber -> useNumber == null ? BigDecimal.ZERO : useNumber).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal type0TotalNumberSum = (type0List.stream().map(ClintGetDataVo::getTotalNumber).map(totalNumber -> totalNumber == null ? BigDecimal.ZERO : totalNumber).reduce(BigDecimal.ZERO, BigDecimal::add));
        x.setIncPackageList(type0List);
        x.setIncUseQuantity(type0UseNumberSum);
        BigDecimal incRemUseQuantity = type0TotalNumberSum.subtract(type0UseNumberSum);
        x.setIncRemUseQuantity( incRemUseQuantity.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : incRemUseQuantity);
        x.setIncTotalQuantity(type0TotalNumberSum);
        x.setSetMenuList(type1List);
        x.setSetMenuUseQuantity(type1UseNumberSum);
        BigDecimal setMenuRemUseQuantity = type1TotalNumberSum.subtract(type1UseNumberSum);
        x.setSetMenuRemUseQuantity(setMenuRemUseQuantity.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : setMenuRemUseQuantity);
        x.setSetMenuTotalQuantity(type1TotalNumberSum);
    }

    /**
     * 三类 AI 监控能力监控位资产统计
     *
     * <p>按固定顺序（话术质检 → 话术还原度 → 互动巡检）查询当前用户的监控位授权量、使用量及剩余量。
     * 三类监控位 sub_account_have=0，子账号调用自动回退主账号资产，无需调用方额外处理。</p>
     *
     * @return 三类监控位授权量信息列表，顺序固定
     */
    @Override
    public R<List<MonitorPositionAuthVo>> monitorPositionStatistics() {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (localUser == null) {
            return R.error(400, "请先登录");
        }
        Long userId = localUser.getId();

        // 话术质检监控位
        MonitorPositionAuthVo quality = userPropertyFeign.checkMonitorPosition(userId,
                OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode());
        quality.setName(OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getMsg());

        // 话术还原度监控位
        MonitorPositionAuthVo fidelity = userPropertyFeign.checkMonitorPosition(userId,
                OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode());
        fidelity.setName(OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getMsg());

        // 互动巡检监控位
        MonitorPositionAuthVo patrol = userPropertyFeign.checkMonitorPosition(userId,
                OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode());
        patrol.setName(OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getMsg());

        return R.ok(Arrays.asList(quality, fidelity, patrol));
    }

}

