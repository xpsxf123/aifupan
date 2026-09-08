package com.jiuyu.replay.api.logic.order.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.order.CommodityLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.vo.CommodityListVo;
import com.jiuyu.replay.order.vo.CommodityInfoVo;
import com.jiuyu.replay.order.bo.CommodityBo;
import com.jiuyu.replay.order.bo.CommodityListBo;
import com.jiuyu.replay.order.bll.CommodityBll;

import com.jiuyu.replay.order.vo.CommodityTypeDataVo;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 商品
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Service
public class CommodityLogicImpl implements CommodityLogic {

    @Resource
    private CommodityBll commodityBll;


    @Override
    public R<PageUtils<CommodityListVo>> queryPage(CommodityListBo commodityListBo) {

        return commodityBll.queryPage(commodityListBo);
    }

    @Override
    public R<List<CommodityInfoVo>> list(CommodityListBo commodityListBo) {
        return commodityBll.list(commodityListBo);
    }

    @Override
    public R<CommodityInfoVo> info(Long id) {

        return commodityBll.info(id);
    }

    @Override
    public R<String> save(CommodityBo commodityBo) {

        return commodityBll.save(commodityBo);
    }

    @Override
    public R<String> update(CommodityBo commodityBo) {

        return commodityBll.update(commodityBo);
    }

    @Override
    public R<String> delete(Long id) {

        return commodityBll.delete(id);
    }

    @Override
    public R<String> saveOrUpdate(CommodityBo commodityBo) {
        if (ObjectUtil.isNotEmpty(commodityBo.getId())){
            return commodityBll.update(commodityBo);
        }else{
            return commodityBll.save(commodityBo);
        }
    }

    @Override
    public R<Boolean> isDeletePriceId(Long priceId) {
        return commodityBll.isDeletePriceId(priceId);
    }

    /**
     * 商品列表（含类型）
     *
     * @param commodityListBo
     * @return
     */
    @Override
    public R<List<CommodityTypeDataVo>> listTypeAll(CommodityListBo commodityListBo) {
        R<List<CommodityInfoVo>> list = commodityBll.list(commodityListBo);

        //没有数据直接返回[]空列表
        if (ObjectUtil.isEmpty(list)||ObjectUtil.isEmpty(list.getData())){
            return R.ok(new ArrayList<>());
        }
        //所有商品数据
        List<CommodityInfoVo> dataList = list.getData();

        //按商品类型分组
        Map<Long, List<CommodityInfoVo>> groupList = dataList.stream()
                .filter(item -> item != null && item.getCommodityTypeId() != null)
                .collect(Collectors
                        .groupingBy(CommodityInfoVo::getCommodityTypeId
                                , Collectors.toList()));

        List<CommodityTypeDataVo> resultList = groupList.entrySet().stream()
                //过滤为null 或 [] 的数据集合
                .filter(entry ->
                        ObjectUtil.isNotEmpty(entry)&&ObjectUtil.isNotEmpty(entry.getValue()))
                //提取每个类型商品的 name、code等信息
                .map(entry -> {
                    //因为已经分组，所以无论取哪一个typeId都一样，所以取第一个
                    // 安全获取第一个元素
                    Optional<CommodityInfoVo> first = entry.getValue().stream()
                            // 过滤掉所有null元素
                            .filter(Objects::nonNull)
                            .findFirst();
                    return first.map(sample -> new CommodityTypeDataVo(
                            sample.getCommodityTypeId(),
                            sample.getCommodityTypeCode(),
                            sample.getCommodityTypeName(),
                            sample.getCommodityTypeUnit(),
                            entry.getValue()
                    )).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return R.ok(resultList);
    }
}

