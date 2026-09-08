package com.jiuyu.replay.order.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.CommodityTypeBo;
import com.jiuyu.replay.order.bo.CommodityTypeListBo;
import com.jiuyu.replay.order.producer.CommodityTypeProducer;
import com.jiuyu.replay.order.rse.CommodityTypeRse;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;
import com.jiuyu.replay.order.vo.CommodityTypeListVo;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 商品类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
@Component
public class CommodityTypeBll {

    @Resource
    private CommodityTypeProducer commodityTypeProducer;
    @Resource
    private CommodityTypeRse commodityTypeRse;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 商品类型列表
     *
     * @param commodityTypeListBo 商品类型列表查询参数
     * @return
     */
    public R<PageUtils<CommodityTypeListVo>> queryPage(CommodityTypeListBo commodityTypeListBo) {

        return R.ok("获取成功", commodityTypeProducer.queryPage(commodityTypeListBo));
    }

    /**
     * 商品类型信息
     *
     * @param id 商品类型id
     * @return
     */
    public R<CommodityTypeInfoVo> info(Long id) {

        CommodityTypeInfoVo commodityTypeInfoVo = commodityTypeProducer.info(id);
        return R.ok("获取成功", commodityTypeInfoVo);
    }

    /**
     * 新增商品类型
     *
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    public R<String> save(CommodityTypeBo commodityTypeBo) {

        CommodityTypeInfoVo commodityTypeInfoVo = commodityTypeProducer.save(commodityTypeBo);
        return R.ok("添加成功");
    }

    /**
     * 修改商品类型
     *
     * @param commodityTypeBo 商品类型对象
     * @return
     */
    public R<String> update(CommodityTypeBo commodityTypeBo) {

        commodityTypeProducer.update(commodityTypeBo);
        return R.ok("修改成功");
    }

    /**
     * 删除商品类型
     *
     * @param id 商品类型id
     * @return
     */
    public R<String> delete(Long id) {

        commodityTypeProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 同步用户资产
     *
     * @param id
     * @return
     */
    public R<String> synchronousUserAssets(Long id) {
        commodityTypeProducer.synchronousUserAssets(id);
        return R.ok();
    }


    /**
     * 获取全部商品类型列表
     *
     * @return
     */
    public com.jiuyu.replay.generic.vo.common.R<List<com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo>> listAll() {

        List<com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo> commodityTypeInfoVos = commodityTypeRse.listAll();

        return com.jiuyu.replay.generic.vo.common.R.ok(commodityTypeInfoVos);
    }
}

