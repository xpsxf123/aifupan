package com.jiuyu.replay.order.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.bo.CommodityListBo;
import com.jiuyu.replay.order.producer.*;
import com.jiuyu.replay.order.repository.service.impl.CommodityPriceServiceImpl;
import com.jiuyu.replay.order.rse.CommodityRse;
import com.jiuyu.replay.order.vo.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 商品
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Component
@AllArgsConstructor
public class CommodityBll {

    private final CommodityProducer commodityProducer;

    private final TypeConsumptionProducer typeConsumptionProducer;

    private final CommodityPriceProducer commodityPriceProducer;

    private final PackageProducer packageProducer;

    private final CommodityTypeProducer commodityTypeProducer;
    private final CommodityPriceServiceImpl commodityPriceService;
    private final CommodityRse commodityRse;


    /**
     * 商品列表
     *
     * @param commodityListBo 商品列表查询参数
     * @return
     */
    public R<PageUtils<CommodityListVo>> queryPage(CommodityListBo commodityListBo) {

        PageUtils<CommodityListVo> data = commodityProducer.queryPage(commodityListBo);
        if (ObjectUtil.isNotEmpty(data.getList())) {
            List<CommodityListVo> list = data.getList();
            List<Long> ids = list.stream().map(CommodityVo::getCommodityTypeId).distinct().toList();
            List<CommodityTypeInfoVo> commodityTypeInfoVos = commodityTypeProducer.listByIds(ids);
            DataUtils.setFieldNameById(list, "commodityTypeId", "commodityTypeCode", commodityTypeInfoVos, "id", "code");
            DataUtils.setFieldNameById(list, "commodityTypeId", "commodityTypeName", commodityTypeInfoVos, "id", "name");
            DataUtils.setFieldNameById(list, "commodityTypeId", "commodityTypeUnit", commodityTypeInfoVos, "id", "unit");
        }
        return R.ok("获取成功", data);
    }

    public R<List<CommodityInfoVo>> list(CommodityListBo commodityListBo) {
        List<CommodityInfoVo> list = commodityProducer.list(commodityListBo);
        if (ObjectUtil.isNotEmpty(list)) {
            List<Long> ids = list.stream().map(CommodityVo::getCommodityTypeId).distinct().toList();
            // 获取商品类型
            List<CommodityTypeInfoVo> commodityTypeInfoVos = commodityTypeProducer.listByIds(ids);
            DataUtils.setFieldNameById(list, "commodityTypeId", "commodityTypeCode", commodityTypeInfoVos, "id", "code");
            DataUtils.setFieldNameById(list, "commodityTypeId", "commodityTypeName", commodityTypeInfoVos, "id", "name");
            DataUtils.setFieldNameById(list, "commodityTypeId", "commodityTypeUnit", commodityTypeInfoVos, "id", "unit");

            // 查询对应的商品价格
            List<Long> idss = list.stream().map(CommodityVo::getId).distinct().toList();
            List<CommodityPriceVo> commodityPriceVos = commodityPriceProducer.listByCommodityId(idss, 0);
            if (ObjectUtil.isNotEmpty(commodityPriceVos)) {
                Map<Long, List<CommodityPriceVo>> listMap = commodityPriceVos.stream().collect(Collectors.groupingBy(CommodityPriceVo::getCommodityId));
                DataUtils.setFieldMap(list, "id", "commodityPriceList", listMap);
            }
        }
        return R.ok("获取成功", list);
    }

    /**
     * 商品信息
     *
     * @param id 商品id
     * @return
     */
    public R<CommodityInfoVo> info(Long id) {

        CommodityInfoVo commodityInfoVo = commodityProducer.info(id);
        if (ObjectUtil.isEmpty(commodityInfoVo)) RRException.create("增量包获取失败");
//         获取商品类型
//        commodityInfoVo.setTypeConsumptionList(typeConsumptionProducer.listByCommodityId(commodityInfoVo.getId(), 1));
        commodityInfoVo.setCommodityType(commodityTypeProducer.info(commodityInfoVo.getCommodityTypeId()));
        if (ObjectUtil.isNotEmpty(commodityInfoVo.getCommodityType())) {
            commodityInfoVo.setCommodityTypeCode(commodityInfoVo.getCommodityType().getCode());
            commodityInfoVo.setCommodityTypeName(commodityInfoVo.getCommodityType().getName());
            commodityInfoVo.setCommodityTypeUnit(commodityInfoVo.getCommodityType().getUnit());
        }

        // 获取商品价格
        commodityInfoVo.setCommodityPriceList(commodityPriceProducer.listByCommodityId(commodityInfoVo.getId(), 0));

        return R.ok("获取成功", commodityInfoVo);
    }


    /**
     * 新增商品
     *
     * @param commodityBo 商品对象
     * @return
     */
    public R<String> save(CommodityBo commodityBo) {

        CommodityInfoVo save = commodityProducer.save(commodityBo);
        if (ObjectUtil.isNotEmpty(save.getId())) {
//            // 添加商品类型
//            if (ObjectUtil.isNotEmpty(save.getTypeConsumptionList())){
//                typeConsumptionProducer.saveOrUpdateBatch(commodityBo.getTypeConsumptionList());
//            }

            // 添加商品价格
            if (ObjectUtil.isNotEmpty(commodityBo.getCommodityPriceList())) {
                commodityBo.getCommodityPriceList().forEach(item -> {
                    item.setId(null);
                    item.setCommodityId(save.getId());
                });
                commodityPriceProducer.saveOrUpdateBatch(commodityBo.getCommodityPriceList());
            }
        } else {
            return R.error(400, "添加失败");
        }
        return R.ok("添加成功");
    }

    /**
     * 修改商品
     *
     * @param commodityBo 商品对象
     * @return
     */
    public R<String> update(CommodityBo commodityBo) {

        commodityProducer.update(commodityBo);

//        // 添加商品类型-先删除再添加
//        typeConsumptionProducer.deleteByCommodityId(commodityBo.getId(), 1);
//        if (ObjectUtil.isNotEmpty(commodityBo.getTypeConsumptionList())){
//            typeConsumptionProducer.saveOrUpdateBatch(commodityBo.getTypeConsumptionList());
//        }

        // 添加商品价格-先删除再添加
//        commodityPriceProducer.deleteByCommodityId(commodityBo.getId(), 0);
        if (ObjectUtil.isNotEmpty(commodityBo.getCommodityPriceList())) {
            commodityBo.getCommodityPriceList().forEach(item -> {
                item.setCommodityId(commodityBo.getId());
//                item.setId(null);
            });
            commodityPriceProducer.saveOrUpdateBatch(commodityBo.getCommodityPriceList());
        }

        return R.ok("修改成功");
    }

    /**
     * 删除商品
     *
     * @param id 商品id
     * @return
     */
    @Transactional
    public R<String> delete(Long id) {

        commodityProducer.deleteById(id);
        return R.ok("删除成功");
    }


    public R<Boolean> isDeletePriceId(Long priceId) {
        return R.ok(commodityProducer.isDeletePriceId(priceId));
    }

    /**
     * 获取所有有效的商品信息
     *
     * @return
     */
    public List<CommodityBo> listAll() {
        return commodityRse.listAll();
    }
}

