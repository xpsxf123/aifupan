package com.jiuyu.replay.order.bll;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.TencentCosProperties;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.ExpirationUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.producer.*;
import com.jiuyu.replay.order.rse.PackageRse;
import com.jiuyu.replay.order.vo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 套餐表(用户版本)
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Component
@AllArgsConstructor
@Slf4j
public class PackageBll {

    private final PackageProducer packageProducer;

    private final CommodityPriceProducer commodityPriceProducer;

    private final TypeConsumptionProducer typeConsumptionProducer;

    private final CommodityProducer commodityProducer;

    private final IncrementProducer incrementProducer;

    private final CommodityTypeProducer commodityTypeProducer;

    private final RedisTemplate<String, Object> redisTemplate;

    private final OrderProducer orderProducer;

    private final CommonProperties commonProperties;

    private final TypeSurplusProducer typeSurplusProducer;

    private final UserPropertyProducer userPropertyProducer;

    private final TencentCosProperties tencentCosProperties;

    private final ImgOssUtils imgOssUtils;

    private final PackageRse packageRse;

    private final UserFeign userFeign;

    private final PackageUserProducer packageUserProducer;


    public R<List<PackageListVo>> listPackageByUser(Long userId) {
        List<PackageListVo> resList = new ArrayList<>();
        PackageListBo bo = new PackageListBo();
        bo.setLimit(-1);
        bo.setPage(-1);
        bo.setPackageType(OrderEnums.packageType.MAIN_PACKAGE.getCode());
        bo.setCustomizeType(OrderEnums.customizeType.SYSTEM.getCode());
        PageUtils<PackageListVo> data = packageProducer.queryPage(bo);
        if (ObjectUtil.isNotEmpty(data.getList())) {
            resList.addAll(data.getList());
        }

        UserDto userDto = userFeign.parentUserByUserId(userId);
        bo.setUserIds(Collections.singletonList(userDto.getId()));
        bo.setCustomizeType(OrderEnums.customizeType.CUSTOMIZE.getCode());
        PageUtils<PackageListVo> data1 = packageProducer.queryPage(bo);
        if (ObjectUtil.isNotEmpty(data1.getList())) {
            resList.addAll(data1.getList());
        }

        if (ObjectUtil.isNotEmpty(resList)) {
            setValue(resList);
            resList.sort(Comparator.comparing(PackageListVo::getLevel).reversed());
        }
        return R.ok(resList);
    }

    /**
     * 套餐表(用户版本)列表
     *
     * @param packageListBo 套餐表(用户版本)列表查询参数
     * @return
     */
    public R<PageUtils<PackageListVo>> queryPage(PackageListBo packageListBo) {
        PageUtils<PackageListVo> data = packageProducer.queryPage(packageListBo);
        if (ObjectUtil.isNotEmpty(data.getList())) {
            setValue(data.getList());
        }
        return R.ok("获取成功", data);
    }

    private void setValue(List<PackageListVo> list) {
        if (ObjectUtil.isNotEmpty(list)) {
            List<Long> ids = list.stream().map(PackageVo::getId).toList();

            // 获取商品类型
            List<TypeConsumptionVo> typeConsumptionVos = typeConsumptionProducer.listBySourceId(ids, 1);
            if (ObjectUtil.isNotEmpty(typeConsumptionVos)) {
                Map<Long, List<TypeConsumptionVo>> collect = typeConsumptionVos.stream().collect(Collectors.groupingBy(TypeConsumptionVo::getSourceId));
                DataUtils.setFieldMap(list, "id", "typeConsumptionList", collect);
            }

            // 获取商品价格
            List<CommodityPriceVo> commodityPriceVos = commodityPriceProducer.listByCommodityId(ids, 1);
            if (ObjectUtil.isNotEmpty(commodityPriceVos)) {
                Map<Long, List<CommodityPriceVo>> collect = commodityPriceVos.stream().collect(Collectors.groupingBy(CommodityPriceVo::getCommodityId));
                DataUtils.setFieldMap(list, "id", "commodityPriceList", collect);
            }

            // 获取套餐列表
            List<IncrementInfoVo> incrementInfoVoList = incrementProducer.listByPackageId(ids);
            if (ObjectUtil.isNotEmpty(incrementInfoVoList)) {
                // 查询商品
                List<Long> commodityIds = incrementInfoVoList.stream().map(IncrementVo::getCommodityId).toList();
                List<CommodityInfoVo> commodityInfoVos = commodityProducer.listCommodityByIds(commodityIds);
                if (ObjectUtil.isNotEmpty(commodityInfoVos)) {
                    DataUtils.setFieldObject(incrementInfoVoList, "commodityId", "commodity", commodityInfoVos, "id");
                }
                Map<Long, List<IncrementInfoVo>> collect = incrementInfoVoList.stream().collect(Collectors.groupingBy(IncrementInfoVo::getPackageId));
                DataUtils.setFieldMap(list, "id", "incrementList", collect);
            }

            // 获取自定义用户列表
            List<PackageUserVo> packageUserVos = packageUserProducer.listByPackageIds(ids);
            if (ObjectUtil.isNotEmpty(packageUserVos)) {
                Map<Long, List<PackageUserVo>> collect = packageUserVos.stream().collect(Collectors.groupingBy(PackageUserVo::getPackageId));
                DataUtils.setFieldMap(list, "id", "packageUserList", collect);
            }
            // 查询logo图
            list.forEach(item -> {
                if (ObjectUtil.isNotEmpty(item.getLogoImgs())) {
                    item.setLogoImgList(new ArrayList<>());
                    String[] s = item.getLogoImgs().split("_");
                    for (String name : s) {
                        FileShowVo file = new FileShowVo();
                        file.setUrl(imgOssUtils.getUrl(name));
                        item.getLogoImgList().add(file);
                    }
                }
                if (ObjectUtil.isNotEmpty(item.getWebsiteLogoImages())) {
                    item.setWebsiteLogoImagesList(new ArrayList<>());
                    String[] s = item.getWebsiteLogoImages().split("_");
                    for (String name : s) {
                        FileShowVo file = new FileShowVo();
                        file.setUrl(imgOssUtils.getUrl(name));
                        item.getWebsiteLogoImagesList().add(file);
                    }
                }
            });

        }
    }

    public PackageVo getOnt(PackageBo packageBo) {
        return packageProducer.getOnt(packageBo);
    }

    /**
     * 套餐表(用户版本)信息
     *
     * @param id
     * @return
     */
    public R<PackageInfoVo> getBtId(Long id) {
        PackageInfoVo info = packageProducer.info(id);
        if (!StringUtils.isEmpty(info)) {
            // 查询logo图
            if (ObjectUtil.isNotEmpty(info.getLogoImgs())) {
                info.setLogoImgList(new ArrayList<>());
                String[] s = info.getLogoImgs().split("_");
                if (s.length >= 1) {
                    info.setLogoImgs(imgOssUtils.getUrl(s[0]));
                }
            }
            return R.ok(info);
        }
        return R.ok();
    }

    /**
     * 套餐表(用户版本)信息-详情
     *
     * @param id 套餐表(用户版本)id
     * @return
     */
    public R<PackageInfoVo> info(Long id) {

        PackageInfoVo packageInfoVo = packageProducer.info(id);
        if (ObjectUtil.isEmpty(packageInfoVo)) return R.ok();

        // 获取商品类型
        packageInfoVo.setTypeConsumptionList(typeConsumptionProducer.listBySourceId(packageInfoVo.getId(), 1));

        // 获取商品价格
        packageInfoVo.setCommodityPriceList(commodityPriceProducer.listByCommodityId(packageInfoVo.getId(), 1));

        // 获取商品列表
        setObjIncrement(packageInfoVo);

        // 查询logo图
        setObjLogo(packageInfoVo);

        return R.ok("获取成功", packageInfoVo);
    }

    /**
     * 获取版本的增量包列表
     *
     * @param packageInfoVo 套餐信息
     */
    private void setObjIncrement(PackageInfoVo packageInfoVo) {
        List<IncrementInfoVo> incrementInfoVoList = incrementProducer.listByPackageId(packageInfoVo.getId());
        if (ObjectUtil.isNotEmpty(incrementInfoVoList)) {
            List<Long> ids = incrementInfoVoList.stream().map(IncrementVo::getCommodityId).toList();
            // 查询商品
            List<CommodityInfoVo> commodityInfoVos = commodityProducer.listCommodityByIds(ids);
            DataUtils.setFieldObject(incrementInfoVoList, "commodityId", "commodity", commodityInfoVos, "id");

            ids = incrementInfoVoList.stream().map(IncrementVo::getCommodityPriceId).toList();
            List<CommodityPriceVo> commodityPriceVos = commodityPriceProducer.listByIds(ids);
            DataUtils.setFieldObject(incrementInfoVoList, "commodityPriceId", "commodityPrice", commodityPriceVos, "id");

            packageInfoVo.setIncrementList(incrementInfoVoList);
        }
    }

    /**
     * 获取版本的logo图
     *
     * @param packageInfoVo 套餐信息
     */
    private void setObjLogo(PackageInfoVo packageInfoVo){
        // 查询logo图
        if (ObjectUtil.isNotEmpty(packageInfoVo.getLogoImgs())) {
            packageInfoVo.setLogoImgList(new ArrayList<>());
            String[] s = packageInfoVo.getLogoImgs().split("_");
            for (String name : s) {
                FileShowVo file = new FileShowVo();
                file.setUrl(imgOssUtils.getUrl(name));
                file.setName(name);
                packageInfoVo.getLogoImgList().add(file);
            }
        }

        // 查询官网logo图
        if (ObjectUtil.isNotEmpty(packageInfoVo.getWebsiteLogoImages())) {
            packageInfoVo.setWebsiteLogoImagesList(new ArrayList<>());
            String[] s = packageInfoVo.getWebsiteLogoImages().split("_");
            for (String name : s) {
                FileShowVo file = new FileShowVo();
                file.setUrl(imgOssUtils.getUrl(name));
                file.setName(name);
                packageInfoVo.getWebsiteLogoImagesList().add(file);
            }
        }
    }

    public R<PackageInfoVo> incrementByPackageId(OrderInfoVo order) {

        PackageInfoVo packageInfoVo = packageProducer.info(order.getCommodityId());
        if (ObjectUtil.isEmpty(packageInfoVo)) {
            packageInfoVo = packageProducer.infoByLevel(0);
            packageInfoVo.setCustomizeType(OrderEnums.customizeType.CUSTOMIZE.getCode());
            if (ObjectUtil.isEmpty(packageInfoVo)) {
                throw new BusinessException("版本不存在");
            }
        }

        // 获取商品类型
        packageInfoVo.setTypeConsumptionList(typeConsumptionProducer.listBySourceId(packageInfoVo.getId(), 1));

        // 获取商品价格
        packageInfoVo.setCommodityPriceList(commodityPriceProducer.listByCommodityId(packageInfoVo.getId(), 1));

        // 查询logo图
        setObjLogo(packageInfoVo);

        if (ObjectUtil.equals(packageInfoVo.getCustomizeType(), OrderEnums.customizeType.SYSTEM.getCode())) {
            // 获取商品列表
            setObjIncrement(packageInfoVo);
        } else if (ObjectUtil.equals(packageInfoVo.getCustomizeType(), OrderEnums.customizeType.CUSTOMIZE.getCode())) {
            setObjAllIncrement(packageInfoVo);
        }

        return R.ok(packageInfoVo);
    }

    private void setObjAllIncrement(PackageInfoVo packageInfoVo) {
        List<IncrementInfoVo> resList = new ArrayList<>();
        List<CommodityInfoVo> list = commodityProducer.listAll();
        List<CommodityPriceVo> commodityPriceVos = commodityPriceProducer.listAll();
        if (ObjectUtil.isEmpty(list) || ObjectUtil.isEmpty(commodityPriceVos)) {
            packageInfoVo.setIncrementList(resList);
            return;
        }

        commodityPriceVos.sort(Comparator.comparing(CommodityPriceVo::getCommodityId));
        Map<Long, CommodityInfoVo> commodityMap = list.stream()
                .filter(item -> item.getStatus() == OrderEnums.status.ENABLE.getCode())
                .collect(Collectors.toMap(CommodityVo::getId, item -> item));
        Map<Long, CommodityTypeEntity> typeMap = commodityTypeProducer.listAll()
                .stream()
                .collect(Collectors.toMap(CommodityTypeEntity::getId, Function.identity(), (a, b) -> a));
        for (CommodityPriceVo item : commodityPriceVos) {

            CommodityInfoVo commodity = commodityMap.get(item.getCommodityId());
            if (commodity == null) {
                continue;
            }
            CommodityTypeEntity commodityType = typeMap.get(commodity.getCommodityTypeId());
            if (commodityType != null) {
                commodity.setCommodityTypeCode(commodityType.getCode());
                commodity.setCommodityTypeName(commodityType.getName());
                commodity.setCommodityTypeUnit(commodityType.getUnit());
            }
            IncrementInfoVo increment = new IncrementInfoVo();
            increment.setCommodity(commodity);
            increment.setCommodityPrice(item);
            increment.setCommodityId(item.getCommodityId());
            increment.setCommodityPriceId(item.getId());
            increment.setPackageId(packageInfoVo.getId());
            increment.setDiscount(BigDecimal.valueOf(1));
            increment.setRealPrice(item.getOriginalPrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getOriginalPrice()));
            increment.setStatus(OrderEnums.status.ENABLE.getCode());
            resList.add(increment);
        }
        packageInfoVo.setIncrementList(resList);
    }

    /**
     * 新增套餐表(用户版本)
     *
     * @param packageBo 套餐表(用户版本)对象
     * @return
     */
    @Transactional
    public R<String> saveOrUpdate(PackageSaveBo packageBo) {
        // 修改套餐表(用户版本)
        if (ObjectUtil.isEmpty(packageBo.getLogoImgList()) && ObjectUtil.equal(packageBo.getPackageType(), 1))
            RRException.create("logo图片不能为空");
        if (ObjectUtil.isEmpty(packageBo.getWebsiteLogoImagesList()) && ObjectUtil.equal(packageBo.getPackageType(), 1))
            RRException.create("官网的logo图片不能为空");
        if (ObjectUtil.isEmpty(packageBo.getName())) RRException.create("版本名称不能为空");
        packageBo.setLogoImgs("");
        if (ObjectUtil.isNotEmpty(packageBo.getLogoImgList())) {
            packageBo.setLogoImgs(packageBo.getLogoImgList().stream().map(FileShowVo::getName).collect(Collectors.joining("_")));
        }
        packageBo.setWebsiteLogoImages("");
        if (ObjectUtil.isNotEmpty(packageBo.getWebsiteLogoImagesList())) {
            packageBo.setWebsiteLogoImages(packageBo.getWebsiteLogoImagesList().stream().map(FileShowVo::getName).collect(Collectors.joining("_")));
        }
        if (ObjectUtil.isEmpty(packageBo.getId())) {
            packageProducer.save(packageBo);
        } else {
            packageProducer.update(packageBo);
        }

        // 添加商品类型-先删除再添加
        typeConsumptionProducer.deleteByIds(packageBo.getId(), 1);
        if (ObjectUtil.isNotEmpty(packageBo.getTypeConsumptionList())) {
            List<CommodityTypeListVo> typeEntityList = commodityTypeProducer.list(new QueryWrapper<>());
            packageBo.getTypeConsumptionList().forEach(item -> {
                if (ObjectUtil.isEmpty(item.getNumber()) || item.getNumber() <= 0L) {
                    RRException.create(StrUtil.format("{}数量为必填，必须大于0", item.getCommodityTypeName()));
                }
                item.setSourceId(packageBo.getId());
                item.setType(1);
                CommodityTypeListVo temp = typeEntityList.stream()
                        .filter(val -> val.getId().equals(item.getCommodityTypeId()))
                        .findFirst()
                        .orElse(null);
                if (temp != null) {
                    item.setCommodityTypeCode(temp.getCode());
                    item.setCommodityTypeName(temp.getName());
                    item.setCommodityTypeUnit(temp.getUnit());
                    item.setCommodityTypeReset(temp.getIsReset());
                }
            });
            typeConsumptionProducer.saveOrUpdateBatch(packageBo.getTypeConsumptionList());
        }

        // 添加商品价格-先删除再添加
        commodityPriceProducer.deleteByCommodityId(packageBo.getId(), 1);
        if (ObjectUtil.isNotEmpty(packageBo.getCommodityPriceList())) {
            packageBo.getCommodityPriceList().forEach(item -> {
                if (ObjectUtil.isEmpty(item.getOriginalPrice())) item.setOriginalPrice(0);
                if (ObjectUtil.isEmpty(item.getRealPrice())) item.setRealPrice(0);
                if (item.getOriginalPrice() < 0L) {
                    RRException.create(StrUtil.format("原价为必填且不能小于0"));
                }
                if (ObjectUtil.isEmpty(item.getRealPrice()) || item.getRealPrice() < 0L) {
                    RRException.create(StrUtil.format("折扣价为必填且不能小于0"));
                }
                item.setCommodityId(packageBo.getId());
                item.setType(1);
                item.setId(null);
            });
            commodityPriceProducer.saveOrUpdateBatch(packageBo.getCommodityPriceList());
        }

        // 处理增量包
        // 删除套餐中的增量包
        incrementProducer.deleteByPackageId(packageBo.getId());
        List<IncrementBo> incrementList = packageBo.getIncrementList();
        if (ObjectUtil.isNotEmpty(incrementList)) {
            // 添加增量包
            incrementList.forEach(item -> {
                item.setPackageId(packageBo.getId());
            });
            incrementProducer.saveBatch(incrementList);
        }

        return R.ok("修改成功");
    }


    /**
     * 删除套餐表(用户版本)
     *
     * @param id 套餐表(用户版本)id
     * @return
     */
    public R<String> delete(Long id) {

        packageProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取可购买套餐列表
     *
     * @param bo
     * @return
     */
    public R<List<PackageInfoVo>> canPurchasePackage(CanPurchasePackageBo bo) {
        List<PackageInfoVo> list = packageProducer.canPurchasePackage(bo);
        return R.ok("获取成功", list);
    }

    /**
     * 获取免费套餐
     *
     * @return
     */
    public R<PackageInfoVo> getGratisPackage() {
        return R.ok(packageProducer.getGratisPackage());
    }

    /**
     * 获取初始化套餐
     *
     * @return
     */
    public R<PackageInfoVo> getPackageInit() {
        return R.ok(packageProducer.getPackageInit());
    }

    public R<PackageListAllByClientVo> packageListAllByClient(Long userId) {
        PackageListAllByClientVo result = new PackageListAllByClientVo();
        result.setPackageList(new ArrayList<>());
        // 查询当前套餐名称
        Long parentUserId = ResultUtil.getResult(userFeign.getUserParentId(userId));
        OrderInfoVo order = orderProducer.currentOrderByUserId(parentUserId == null ? userId : parentUserId);
        result.setPackageName(order == null ? "" : order.getCommodityName());

        // 当前套餐列表
        List<OrderInfoVo> orderInfoVoList = orderProducer.getOrderByUserId(parentUserId == null ? userId : parentUserId);
        result.setCurrentOrderList(orderInfoVoList);

        // 查询全部套餐
        PackageListBo bo = new PackageListBo();
        bo.setLimit(-1);
        R<PageUtils<PackageListVo>> packageListVoPageUtils = this.queryPage(bo);
        if (ObjectUtil.isNotEmpty(packageListVoPageUtils.getData()) && ObjectUtil.isNotEmpty(packageListVoPageUtils.getData().getList())) {
            List<PackageListVo> list = packageListVoPageUtils.getData().getList();
            list = list.stream().filter(item -> item.getStatus() == 1).toList();
            result.setPackageList(list);
        }
        return R.ok(result);
    }

    public R<List<PackageListVo>> websiteList(UserVersionOrderVo orderVo) {
        PackageListBo bo = new PackageListBo();
        bo.setStatus(1);
        bo.setPackageType(1);
        bo.setLimit(-1);
        R<PageUtils<PackageListVo>> packageListVosR = this.queryPage(bo);
        if (packageListVosR.getCode() == 0 && ObjectUtil.isNotEmpty(packageListVosR.getData()) && ObjectUtil.isNotEmpty(packageListVosR.getData().getList())) {
            List<PackageListVo> list = packageListVosR.getData().getList();
            List<PackageListVo> list1 = list.stream()
                    .filter(item -> item.getLevel() >= orderVo.getLevel())
                    .sorted(Comparator.comparing(PackageVo::getLevel)).toList();
            list1.forEach(item -> {
                // 在官网显示，且价格大于等于订单剩余价格
                if (ObjectUtil.isNotEmpty(item.getCommodityPriceList())) {
                    List<CommodityPriceVo> list2 = item.getCommodityPriceList().stream()
                            .filter(val -> val.getShowStatus() == 1 && (ObjectUtil.equals(item.getLevel(), orderVo.getLevel()) || val.getRealPrice() >= orderVo.getSurplusAmount()))
                            .peek(val -> {
                                if (ObjectUtil.isNotEmpty(val.getValidityNum()) && ObjectUtil.isNotEmpty(val.getValidityUnit())) {
                                    val.setValidityNumUnitName(ExpirationUtils.getExpirationUnitName(val.getValidityNum(), val.getValidityUnit()));
                                }
                            })
                            .toList();
                    item.setCommodityPriceList(list2);
                }
            })
            ;
            return R.ok(list1);

        }
        return R.ok(List.of());
    }

    /**
     * 同步版本的资源到用户
     *
     * @param id
     * @return
     */
    @Transactional
    public R<String> synchronousPackage(Long id) {
        log.info("开始同步版本 id = {}", id);
        PackageInfoVo info = packageProducer.detailById(id);
        RRException.isNotEmpty(info, "版本不存在");

        log.info("同步版本 开始获取要同步的订单数量");
        List<OrderDetailInfoVo> orderDetailsList = orderProducer.synchronousPackage(info);
        if (ObjectUtil.isNotEmpty(orderDetailsList)) {
            log.info("同步版本 要同步的订单数量 {}", orderDetailsList.size());

            // 获取用户的资产ids
            List<Long> userIds = orderDetailsList.stream()
                    .filter(item -> ObjectUtil.isNotEmpty(item.getPropertyId()) && item.getPropertyId() == -1L)
                    .map(OrderDetailInfoVo::getUserId).distinct().toList();
            if (ObjectUtil.isNotEmpty(userIds)){
                List<UserPropertyInfoVo> userPropertyList = userPropertyProducer.getUserPropertyByUserIds(userIds);
                if (ObjectUtil.isNotEmpty(userPropertyList)) {
                    // 去掉userid重复的对象
                    userPropertyList = userPropertyList.stream()
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toMap(UserPropertyInfoVo::getUserId, Function.identity(), (existing, replacement) -> existing, LinkedHashMap::new),
                                    map -> new ArrayList<>(map.values())
                            ));
                    DataUtils.setFieldNameById(orderDetailsList, "userId", "propertyId", userPropertyList, "userId", "id");
                }
            }

            log.info("同步tb_type_surplus表");
            typeSurplusProducer.synchronousTypeSurplus(orderDetailsList);
            log.info("同步tb_type_surplus表完成");

            orderDetailsList.stream()
                    .filter(item -> item.getStatus() != null && item.getStatus() == 1)
                    .map(OrderDetailInfoVo::getUserId).distinct()
                    .forEach(userId -> {
                        Long parentId = ResultUtil.getResult(userFeign.getUserParentId(userId));
                        if (parentId == null) {
                            // 更新资产
                            UserPropertyInfoVo userPropertyInfoVo = userPropertyProducer.getOne(userId, 1, 0, 0L, 0L);
                            if (ObjectUtil.isNotEmpty(userPropertyInfoVo)) {
                                redisTemplate.opsForSet().add("replay:sync-user-property", userPropertyInfoVo.getId());
                            }
                        }
                    });

        }
        return R.ok("同步成功");
    }

    @Transactional
    public void syncUserPropertyRedis(Long userPropertyId) {
        if (ObjectUtil.isNotEmpty(userPropertyId)) {
            UserPropertyInfoVo info = userPropertyProducer.info(userPropertyId);
            if (ObjectUtil.isNotEmpty(info) && ObjectUtil.equals(info.getIsUse(), 1)) {
                userPropertyProducer.statisticsProperty(info);
                userPropertyProducer.statisticsChildProperty(info);
            }
        }
    }

    public R<CurrentFreeVersionVo> currentFreeVersion() {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");

        Long userId = user.getId();
        // 判断是否子账号
        if (ObjectUtil.isNotEmpty(user.getActiveTenantId())) {
            UserDto userDto = userFeign.userById(user.getId());
            if (ObjectUtil.isNotEmpty(userDto) && ObjectUtil.isNotEmpty(userDto.getParentId()) && userDto.getParentId() != 0L) {
                userId = userDto.getParentId();
            }
        }


        OrderInfoVo orderInfoVo = orderProducer.currentUserOrderDetails(userId);

        CurrentFreeVersionVo result = new CurrentFreeVersionVo();

        if (ObjectUtil.isNotEmpty(orderInfoVo) && ObjectUtil.isNotEmpty(orderInfoVo.getOrderDetailList())) {

            List<OrderDetailInfoVo> orderDetailList = orderInfoVo.getOrderDetailList();

            orderDetailList.stream().filter(item -> item.getCommodityTypeCode().equals("aiAnalysisTime")).findFirst().ifPresent(item -> {
                result.setAiAnalysisTime(item.getTotalNumber());
            });

            orderDetailList.stream().filter(item -> item.getCommodityTypeCode().equals("aiTokenNum")).findFirst().ifPresent(item -> {
                result.setAiTokenNum(item.getTotalNumber());
            });

            orderDetailList.stream().filter(item -> item.getCommodityTypeCode().equals("textExtractionNum")).findFirst().ifPresent(item -> {
                result.setTextExtractionNum(item.getTotalNumber());
            });

        }

        return R.ok(result);
    }

    /**
     * 获取所有版本套餐
     * @param packageType 套餐类型：1主要套餐，2次要套餐
     * @return
     */
    public R<List<PackageInfoVo>> listAll(Integer packageType) {

        List<PackageInfoVo> packageInfoVos = this.packageProducer.listAllActivity(packageType);

        return R.ok(packageInfoVos);
    }

    /**
     * 获取所有版本套餐
     * @param packageType 套餐类型：1主要套餐，2次要套餐
     * @return
     */
    public com.jiuyu.replay.generic.vo.common.R<List<com.jiuyu.replay.generic.vo.order.PackageInfoVo>> listAllActivity(Integer packageType) {

        List<com.jiuyu.replay.generic.vo.order.PackageInfoVo> packageInfoVos = this.packageRse.listAllActivity(packageType);

        return com.jiuyu.replay.generic.vo.common.R.ok(packageInfoVos);
    }

    /**
     * 试用版本列表
     *
     * @return 试用列表
     */
    public List<PackageListVo> trialVersionList(Long userId) {
        List<PackageListVo> list = new ArrayList<>();
        PackageListBo bo = new PackageListBo();
        bo.setLimit(-1);
        bo.setPage(-1);
        bo.setStatus(1);
        bo.setPackageType(OrderEnums.packageType.MAIN_PACKAGE.getCode());
        bo.setCustomizeType(OrderEnums.customizeType.SYSTEM.getCode());
        PageUtils<PackageListVo> data = packageProducer.queryPage(bo);
        if (ObjectUtil.isNotEmpty(data.getList())) {
            list.addAll(data.getList());
        }
        if (ObjectUtil.isNotEmpty(userId)) {
            UserDto userDto = userFeign.parentUserByUserId(userId);
            bo.setUserIds(Collections.singletonList(userDto.getId()));
            bo.setCustomizeType(OrderEnums.customizeType.CUSTOMIZE.getCode());
            PageUtils<PackageListVo> data1 = packageProducer.queryPage(bo);
            if (ObjectUtil.isNotEmpty(data1.getList())) {
                list.addAll(data1.getList());
            }
        }

        if (ObjectUtil.isEmpty(list)) {
            return List.of();
        }

        List<Long> ids = list.stream().map(PackageVo::getId).toList();

        // 获取商品价格
        List<CommodityPriceVo> commodityPriceVos = commodityPriceProducer.listByCommodityIdAndTrial(ids, 1);
        if (ObjectUtil.isNotEmpty(commodityPriceVos)) {
            Map<Long, List<CommodityPriceVo>> collect = commodityPriceVos.stream().collect(Collectors.groupingBy(CommodityPriceVo::getCommodityId));
            DataUtils.setFieldMap(list, "id", "commodityPriceList", collect);
        }

        // 去掉无用的版本套餐
        list = list.stream().filter(item -> ObjectUtil.isNotEmpty(item.getCommodityPriceList())).toList();
        ids = list.stream().map(PackageVo::getId).toList();

        if (ObjectUtil.isEmpty(list)) {
            return List.of();
        }

        // 获取商品类型
        List<TypeConsumptionVo> typeConsumptionVos = typeConsumptionProducer.listBySourceId(ids, 1);
        if (ObjectUtil.isNotEmpty(typeConsumptionVos)) {
            Map<Long, List<TypeConsumptionVo>> collect = typeConsumptionVos.stream().collect(Collectors.groupingBy(TypeConsumptionVo::getSourceId));
            DataUtils.setFieldMap(list, "id", "typeConsumptionList", collect);
        }

        // 获取套餐列表
        List<IncrementInfoVo> incrementInfoVoList = incrementProducer.listByPackageId(ids);
        if (ObjectUtil.isNotEmpty(incrementInfoVoList)) {
            // 查询商品
            List<Long> commodityIds = incrementInfoVoList.stream().map(IncrementVo::getCommodityId).toList();
            List<CommodityInfoVo> commodityInfoVos = commodityProducer.listCommodityByIds(commodityIds);
            if (ObjectUtil.isNotEmpty(commodityInfoVos)) {
                DataUtils.setFieldObject(incrementInfoVoList, "commodityId", "commodity", commodityInfoVos, "id");
            }
            Map<Long, List<IncrementInfoVo>> collect = incrementInfoVoList.stream().collect(Collectors.groupingBy(IncrementInfoVo::getPackageId));
            DataUtils.setFieldMap(list, "id", "incrementList", collect);
        }

        // 查询logo图
        list.forEach(item -> {
            if (ObjectUtil.isNotEmpty(item.getLogoImgs())) {
                item.setLogoImgList(new ArrayList<>());
                String[] s = item.getLogoImgs().split("_");
                for (String name : s) {
                    FileShowVo file = new FileShowVo();
                    file.setUrl(imgOssUtils.getUrl(name));
                    item.getLogoImgList().add(file);
                }
            }
            if (ObjectUtil.isNotEmpty(item.getWebsiteLogoImages())) {
                item.setWebsiteLogoImagesList(new ArrayList<>());
                String[] s = item.getWebsiteLogoImages().split("_");
                for (String name : s) {
                    FileShowVo file = new FileShowVo();
                    file.setUrl(imgOssUtils.getUrl(name));
                    item.getWebsiteLogoImagesList().add(file);
                }
            }
        });
        return list;
    }

    public List<com.jiuyu.replay.generic.vo.order.PackageVo> listSingleAll(Integer packageType) {
        return packageProducer.listSingleAll(packageType);
    }

    /**
     * 获取版本中的子账号数量
     *
     * @param data 版本信息
     * @return 子账号数量
     */
    public int getSubAccountNum(PackageInfoVo data) {
        if (data == null || ObjectUtil.isEmpty(data.getTypeConsumptionList())) {
            return 0;
        }
        String code = OrderEnums.commodityTypeCode.SUB_ACCOUNT_COUNT.getCode();
        return data.getTypeConsumptionList().stream()
                .filter(item -> code.equals(item.getCommodityTypeCode()))
                .mapToInt(item -> ObjectUtil.defaultIfNull(item.getNumber(), 0L).intValue())
                .sum();
    }
}

