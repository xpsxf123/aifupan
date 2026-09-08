package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.order.bo.CanPurchasePackageBo;
import com.jiuyu.replay.order.bo.PackageBo;
import com.jiuyu.replay.order.bo.PackageListBo;
import com.jiuyu.replay.order.entity.*;
import com.jiuyu.replay.order.producer.PackageProducer;
import com.jiuyu.replay.order.repository.service.*;
import com.jiuyu.replay.order.repository.service.impl.CommodityPriceServiceImpl;
import com.jiuyu.replay.order.vo.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 套餐表(用户版本)
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Service
@AllArgsConstructor
public class PackageProducerImpl implements PackageProducer {

    private final PackageService packageService;
    private final CommodityPriceServiceImpl commodityPriceService;
    private final TypeSurplusService typeSurplusService;
    private final TypeConsumptionService typeConsumptionService;
    private final IncrementService incrementService;
    private final ImgOssUtils imgOssUtils;
    private final PackageUserService packageUserService;


    @Override
    public PageUtils<PackageListVo> queryPage(PackageListBo packageListBo) {
        if (packageListBo == null) {
            packageListBo = new PackageListBo();
        }

        List<Long> ids = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(packageListBo.getPhone()) || ObjectUtil.isNotEmpty(packageListBo.getUserIds())) {
            ids.addAll(packageUserService.lambdaQuery()
                    .in(ObjectUtil.isNotEmpty(packageListBo.getUserIds()), PackageUserEntity::getUserId, packageListBo.getUserIds())
                    .select(PackageUserEntity::getPackageId)
                    .list()
                    .stream()
                    .map(PackageUserEntity::getPackageId)
                    .toList());
            if (ids.isEmpty()) {
                return new PageUtils<>(List.of(), 0, 0, 0);
            }
            packageListBo.setCustomizeType(OrderEnums.customizeType.CUSTOMIZE.getCode());
        }

        LambdaQueryWrapper<PackageEntity> wrapper = new LambdaQueryWrapper<PackageEntity>()
                .like(ObjectUtil.isNotEmpty(packageListBo.getName()), PackageEntity::getName, packageListBo.getName())
                .eq(ObjectUtil.isNotEmpty(packageListBo.getStatus()), PackageEntity::getStatus, packageListBo.getStatus())
                .eq(ObjectUtil.isNotEmpty(packageListBo.getPackageType()), PackageEntity::getPackageType, packageListBo.getPackageType())
                .eq(ObjectUtil.isNotEmpty(packageListBo.getCustomizeType()), PackageEntity::getCustomizeType, packageListBo.getCustomizeType())
                .in(ObjectUtil.isNotEmpty(ids), PackageEntity::getId, new HashSet<>(ids))
                .orderByDesc(PackageEntity::getLevel)
                ;

        IPage<PackageEntity> iPage = packageService.page(new Query<PackageEntity>().getPageNoSort(packageListBo.getPage(), packageListBo.getLimit()), wrapper);

        PageUtils<PackageListVo> pageUtils = new PageUtils<>(packageListBo.getPage(), packageListBo.getLimit(), iPage);

        List<PackageEntity> records = iPage.getRecords();
        if(ObjectUtil.isNotEmpty(records)) {
            List<PackageListVo> vos = records.stream().map(item -> {
                PackageListVo packageVo = new PackageListVo();
                BeanUtils.copyProperties(item, packageVo);
                return packageVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public PackageInfoVo info(Long id) {

        PackageEntity packageEntity = packageService.getById(id);
        if(packageEntity != null) {
            PackageInfoVo packageInfoVo = new PackageInfoVo();
            BeanUtils.copyProperties(packageEntity, packageInfoVo);
            return packageInfoVo;
        }

        return null;
    }

    @Override
    public PackageInfoVo infoDealsImg(Long id) {
        PackageInfoVo info = info(id);
        if (info != null) {
            // 查询logo图
            if (ObjectUtil.isNotEmpty(info.getLogoImgs())) {
                ArrayList<FileShowVo> fileShowVos = new ArrayList<>();
                FileShowVo e = new FileShowVo();
                e.setUrl(imgOssUtils.getUrl(info.getLogoImgs()));
                fileShowVos.add(e);
                info.setLogoImgList(fileShowVos);
            }

            // 查询官网logo图
            if (ObjectUtil.isNotEmpty(info.getWebsiteLogoImages())) {
                ArrayList<FileShowVo> fileShowVos = new ArrayList<>();
                FileShowVo e = new FileShowVo();
                e.setUrl(imgOssUtils.getUrl(info.getWebsiteLogoImages()));
                fileShowVos.add(e);
                info.setWebsiteLogoImagesList(fileShowVos);
            }
        }
        return info;
    }

    @Override
    public PackageInfoVo detailById(Long id) {

        PackageEntity packageEntity = packageService.getById(id);

        // 查询商品信息
        List<TypeConsumptionEntity> typeSurplusEntities = typeConsumptionService.list(new LambdaQueryWrapper<TypeConsumptionEntity>()
                .in(TypeConsumptionEntity::getSourceId, packageEntity.getId())
                .eq(TypeConsumptionEntity::getType, 1)
        );

        PackageInfoVo infoVo = BeanUtil.copyProperties(packageEntity, PackageInfoVo.class);

        infoVo.setTypeConsumptionList(BeanUtil.copyToList(typeSurplusEntities, TypeConsumptionVo.class));

        return infoVo;
    }

    /**
     * 新增套餐表(用户版本)
     * @param packageBo 套餐表(用户版本)对象
     * @return
     */
     public PackageInfoVo save(PackageBo packageBo) {

         PackageEntity packageEntity = new PackageEntity();
         BeanUtils.copyProperties(packageBo, packageEntity);
         packageEntity.setId(SnowflakeManager.nextValue());
         packageEntity.setCreateDate(new Date());
         packageEntity.setUpdateDate(new Date());

         packageService.save(packageEntity);
         packageBo.setId(packageEntity.getId());
         PackageInfoVo packageInfoVo = new PackageInfoVo();
         BeanUtils.copyProperties(packageEntity, packageInfoVo);

         return packageInfoVo;
     }

    /**
     * 修改套餐表(用户版本)
     * @param packageBo 套餐表(用户版本)对象
     * @return
     */
    public void update(PackageBo packageBo) {

        PackageEntity packageEntity = new PackageEntity();
        BeanUtils.copyProperties(packageBo, packageEntity);
        packageEntity.setUpdateDate(new Date());

        packageService.updateById(packageEntity);
    }

    /**
     * 删除套餐表(用户版本)
     * @param id 套餐表(用户版本)id
     * @return
     */
    public void deleteById(Long id) {

        // 删除套餐表(用户版本)
        packageService.removeById(id);

        // 删除套餐表(用户版本)价格
        commodityPriceService.remove(new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(CommodityPriceEntity::getCommodityId, id)
                .eq(CommodityPriceEntity::getType, 1)
        );

        // 删除套餐表(用户版本)商品
        typeConsumptionService.remove(new LambdaQueryWrapper<TypeConsumptionEntity>()
                .eq(TypeConsumptionEntity::getSourceId, id)
                .eq(TypeConsumptionEntity::getType, 1)
        );

        // 删除套餐表(用户版本)增量包
        incrementService.remove(new LambdaQueryWrapper<IncrementEntity>()
                .eq(IncrementEntity::getPackageId, id)
        );

    }

    @Override
    public PackageVo getOnt(PackageBo packageBo) {
        PackageEntity one = packageService.getOne(new LambdaQueryWrapper<PackageEntity>()
                .eq(ObjectUtil.isNotEmpty(packageBo.getId()), PackageEntity::getId, packageBo.getId())
                .like(ObjectUtil.isNotEmpty(packageBo.getName()), PackageEntity::getName, packageBo.getName())
                .eq(ObjectUtil.isNotEmpty(packageBo.getLevel()), PackageEntity::getLevel, packageBo.getLevel())
                .eq(ObjectUtil.isNotEmpty(packageBo.getStatus()), PackageEntity::getStatus, packageBo.getStatus())
                .eq(ObjectUtil.isNotEmpty(packageBo.getIsGive()), PackageEntity::getIsGive, packageBo.getIsGive())
        );
        if (one != null){
            return BeanUtil.copyProperties(one, PackageVo.class);
        }
        return null;
    }

    @Override
    public List<PackageInfoVo> canPurchasePackage(CanPurchasePackageBo bo) {
        LambdaQueryWrapper<PackageEntity> wrapper = new LambdaQueryWrapper<PackageEntity>()
                .eq(ObjectUtil.isNotEmpty(bo.getLevel()), PackageEntity::getLevel, bo.getLevel())
                .gt(ObjectUtil.isNotEmpty(bo.getMinLevel()), PackageEntity::getLevel, bo.getMinLevel())
                .eq(ObjectUtil.isNotEmpty(bo.getPackageType()), PackageEntity::getPackageType, bo.getPackageType())
                .eq(ObjectUtil.isNotEmpty(bo.getStatus()), PackageEntity::getStatus, bo.getStatus())
                .eq(ObjectUtil.isNotEmpty(bo.getPackageId()), PackageEntity::getId, bo.getPackageId())
                ;

        List<PackageEntity> list = packageService.list(wrapper);
        List<PackageInfoVo> result = new ArrayList<>();
        if(ObjectUtil.isNotEmpty(list)) {
            result = BeanUtil.copyToList(list, PackageInfoVo.class);
            List<Long> ids = list.stream().map(PackageEntity::getId).toList();
            // 查询商品价格
            List<CommodityPriceEntity> priceEntities = commodityPriceService.list(new LambdaQueryWrapper<CommodityPriceEntity>()
                    .in(CommodityPriceEntity::getCommodityId, ids)
                    .eq(CommodityPriceEntity::getType, 1)
            );
            if (ObjectUtil.isNotEmpty(priceEntities)){
                List<CommodityPriceVo> commodityPriceInfoVos = BeanUtil.copyToList(priceEntities, CommodityPriceVo.class);
                Map<Long, List<CommodityPriceVo>> listMap = commodityPriceInfoVos.stream().collect(Collectors.groupingBy(CommodityPriceVo::getCommodityId));
                DataUtils.setFieldMap(result, "id", "commodityPriceList", listMap);
            }

            // 查询商品信息
            List<TypeConsumptionEntity> typeSurplusEntities = typeConsumptionService.list(new LambdaQueryWrapper<TypeConsumptionEntity>()
                    .in(TypeConsumptionEntity::getSourceId, ids)
                    .eq(TypeConsumptionEntity::getType, 1)
            );
            if (ObjectUtil.isNotEmpty(typeSurplusEntities)){
                List<TypeConsumptionVo> typeConsumptionVos = BeanUtil.copyToList(typeSurplusEntities, TypeConsumptionVo.class);
                Map<Long, List<TypeConsumptionVo>> listMap = typeConsumptionVos.stream().collect(Collectors.groupingBy(TypeConsumptionVo::getSourceId));
                DataUtils.setFieldMap(result, "id", "typeConsumptionList", listMap);
            }
        }
        return result;
    }

    @Override
    public PackageInfoVo getGratisPackage() {
        PackageEntity one = packageService.getOne(new LambdaQueryWrapper<PackageEntity>()
                .eq(PackageEntity::getLevel, 0)
                .last("limit 1")
        );
        if (one == null) RRException.create("当前没有设置好免费的版本，请联系开发人员");
        CommodityPriceEntity price = commodityPriceService.getOne(new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(CommodityPriceEntity::getCommodityId, one.getId())
                .eq(CommodityPriceEntity::getType, 1)
                .last("limit 1")
        );
        if (price == null) RRException.create("当前没有设置好免费版本的商品价格，请联系开发人员");
        PackageInfoVo result = BeanUtil.copyProperties(one, PackageInfoVo.class);
        ArrayList<CommodityPriceVo> priceList = new ArrayList<>();
        priceList.add(BeanUtil.copyProperties(price, CommodityPriceVo.class));
        result.setCommodityPriceList(priceList);
        return result;
    }

    @Override
    public PackageInfoVo getPackageInit() {
        PackageEntity one = packageService.getOne(new LambdaQueryWrapper<PackageEntity>()
                .lt(PackageEntity::getLevel, 0)
                .last("limit 1")
        );
        if (one == null) RRException.create("当前系统没有设置好激活的版本，请联系开发人员");
        CommodityPriceEntity price = commodityPriceService.getOne(new LambdaQueryWrapper<CommodityPriceEntity>()
                .eq(CommodityPriceEntity::getCommodityId, one.getId())
                .eq(CommodityPriceEntity::getType, 1)
                .last("limit 1")
        );
        if (price == null) RRException.create("当前系统没有设置好激活版本的商品价格，请联系开发人员");
        PackageInfoVo result = BeanUtil.copyProperties(one, PackageInfoVo.class);
        ArrayList<CommodityPriceVo> priceList = new ArrayList<>();
        priceList.add(BeanUtil.copyProperties(price, CommodityPriceVo.class));
        result.setCommodityPriceList(priceList);
        return result;
    }

    @Override
    public CurrentFreeVersionVo currentFreeVersion() {
        CurrentFreeVersionVo result = new CurrentFreeVersionVo();
        result.setAiAnalysisTime(0L);
        result.setAiTokenNum(0L);
        PackageEntity one = packageService.getOne(new LambdaQueryWrapper<PackageEntity>()
                .eq(PackageEntity::getLevel, 0)
                .last("limit 1")
        );

        if (one != null) {
            List<TypeConsumptionEntity> typeConsumption = typeConsumptionService.list(new LambdaQueryWrapper<TypeConsumptionEntity>()
                    .eq(TypeConsumptionEntity::getSourceId, one.getId())
                    .eq(TypeConsumptionEntity::getType, 1)
            );
            if (ObjectUtil.isNotEmpty(typeConsumption)){

                TypeConsumptionEntity typeConsumptionEntity = typeConsumption.stream().filter(item -> "aiAnalysisTime".equals(item.getCommodityTypeCode())).findAny().orElse(null);
                if (ObjectUtil.isNotEmpty(typeConsumptionEntity)){
                    result.setAiAnalysisTime(typeConsumptionEntity.getNumber());
                }

                TypeConsumptionEntity typeConsumption1 = typeConsumption.stream().filter(item -> "aiTokenNum".equals(item.getCommodityTypeCode())).findAny().orElse(null);
                if (ObjectUtil.isNotEmpty(typeConsumption1)){
                    result.setAiTokenNum(typeConsumption1.getNumber());
                }

            }
        }

        return result;
    }

    @Override
    public List<PackageInfoVo> listAllActivity(Integer packageType) {

        QueryWrapper<PackageEntity> wrapper = new QueryWrapper<>();
        if(packageType != null) {
            wrapper.eq("package_type", packageType);
        }
        wrapper.eq("status", 1);
        List<PackageEntity> packageEntities = this.packageService.list(wrapper);

        if(packageEntities != null && packageEntities.size() > 0) {
            List<Long> packageIds = packageEntities.stream().map(PackageEntity::getId).toList();
            List<CommodityPriceEntity> commodityPriceEntities = this.commodityPriceService.list(new QueryWrapper<CommodityPriceEntity>().in("commodity_id", packageIds));

            List<PackageInfoVo> packageInfoVoList = packageEntities.stream().map(item -> {
                PackageInfoVo packageInfoVo = new PackageInfoVo();
                BeanUtils.copyProperties(item, packageInfoVo);
                // 封装套餐的价格列表
                List<CommodityPriceVo> commodityPriceList = new LinkedList<>();
                if (commodityPriceEntities != null && commodityPriceEntities.size() > 0) {
                    for (CommodityPriceEntity commodityPriceEntity : commodityPriceEntities) {
                        if (commodityPriceEntity.getCommodityId().equals(item.getId())) {
                            CommodityPriceVo commodityPriceVo = new CommodityPriceVo();
                            BeanUtils.copyProperties(commodityPriceEntity, commodityPriceVo);
                            commodityPriceList.add(commodityPriceVo);
                        }
                    }
                }
                packageInfoVo.setCommodityPriceList(commodityPriceList);

                return packageInfoVo;
            }).collect(Collectors.toList());

            return packageInfoVoList;
        }

        return null;
    }

    @Override
    public List<com.jiuyu.replay.generic.vo.order.PackageVo> listSingleAll(Integer packageType) {

        List<PackageEntity> packageEntities = this.packageService.list(new LambdaQueryWrapper<PackageEntity>()
                .eq(ObjectUtil.isNotEmpty(packageType), PackageEntity::getPackageType, packageType)
        );
        if (ObjectUtil.isNotEmpty(packageEntities)) {
            return BeanUtil.copyToList(packageEntities, com.jiuyu.replay.generic.vo.order.PackageVo.class);
        }
        return List.of();
    }

    @Override
    public PackageInfoVo infoByLevel(Integer level) {
        PackageEntity one = packageService.getOne(new LambdaQueryWrapper<PackageEntity>()
                .eq(PackageEntity::getLevel, level)
                .eq(PackageEntity::getPackageType, OrderEnums.packageType.MAIN_PACKAGE.getCode())
                .last("limit 1")
        );
        if (one != null) {
            return BeanUtil.copyProperties(one, PackageInfoVo.class);
        }
        return null;
    }

    @Override
    public void checkMainLevel(PackageBo packageBo) {
        if (packageBo.getLevel() == null) RRException.create("套餐等级不能为空");
        if (ObjectUtil.isNotNull(packageBo.getId())) {
            Long count = packageService.lambdaQuery()
                    .eq(PackageEntity::getPackageType, OrderEnums.packageType.MAIN_PACKAGE.getCode())
                    .eq(PackageEntity::getLevel, packageBo.getLevel())
                    .ne(PackageEntity::getId, packageBo.getId())
                    .count();
            if (count > 0) RRException.create("套餐等级已存在");
        } else {
            Long count = packageService.lambdaQuery()
                    .eq(PackageEntity::getPackageType, OrderEnums.packageType.MAIN_PACKAGE.getCode())
                    .eq(PackageEntity::getLevel, packageBo.getLevel())
                    .count();
            if (count > 0) RRException.create("套餐等级已存在");
        }
    }

    @Override
    public List<PackageInfoVo> listByIds(List<Long> packageIds) {

        List<PackageEntity> packageEntities = this.packageService.listByIds(packageIds);
        if(packageEntities != null && packageEntities.size() > 0) {
            List<PackageInfoVo> packageInfoVos = packageEntities.stream().map(item -> {
                PackageInfoVo packageInfoVo = new PackageInfoVo();
                BeanUtils.copyProperties(item, packageInfoVo);
                return packageInfoVo;
            }).toList();
            return packageInfoVos;
        }
        return null;
    }
}

