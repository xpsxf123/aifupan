package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.order.vo.TypeConsumptionInfoVo;
import com.jiuyu.replay.order.vo.TypeConsumptionListVo;
import com.jiuyu.replay.order.bo.TypeConsumptionBo;
import com.jiuyu.replay.order.bo.TypeConsumptionListBo;
import com.jiuyu.replay.order.vo.TypeConsumptionVo;

import java.util.List;


/**
 * 商品类型用量关联表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
public interface TypeConsumptionProducer {


    /**
     * 商品类型用量关联表列表
     * @param typeConsumptionListBo 商品类型用量关联表列表查询参数
     * @return
     */
    PageUtils<TypeConsumptionListVo> queryPage(TypeConsumptionListBo typeConsumptionListBo);

    /**
    * 商品类型用量关联表信息
    * @param id 商品类型用量关联表id
    * @return
    */
    TypeConsumptionInfoVo info(Long id);

    /**
     * 新增商品类型用量关联表
     * @param typeConsumptionBo 商品类型用量关联表对象
     * @return
     */
     TypeConsumptionInfoVo save(TypeConsumptionBo typeConsumptionBo);

    /**
     * 修改商品类型用量关联表
     * @param typeConsumptionBo 商品类型用量关联表对象
     * @return
     */
    void update(TypeConsumptionBo typeConsumptionBo);

    /**
     * 删除商品类型用量关联表
     * @param id 商品类型用量关联表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据商品id和类型查询商品类型用量关联表列表
     * @param sourceId
     * @param type
     * @return
     */
    List<TypeConsumptionVo> listBySourceId(Long sourceId, Integer type);

    /**
     * 批量根据商品id和类型查询商品类型用量关联表列表
     * @param sourceId
     * @param type
     * @return
     */
    List<TypeConsumptionVo> listBySourceId(List<Long> sourceId, Integer type);

    /**
     * 根据商品id和类型删除商品类型用量关联表
     * @param commodityId
     * @param type
     */
    void deleteByIds(Long commodityId, int type);

    /**
     * 批量删除商品类型用量关联表
     * @param ids 商品类型用量关联表id集合
     */
    void deleteByIds(List<Long> ids);

    /**
     * 批量新增商品类型用量关联表
     * @param typeConsumptionList
     */
    List<TypeConsumptionVo> saveOrUpdateBatch(List<TypeConsumptionBo> typeConsumptionList);

    /**
     * 保存或更新商品类型用量关联表（自动获取商品类型信息）
     *
     * @param typeConsumptionBo 商品类型用量关联表对象
     * @return
     */
    TypeConsumptionInfoVo saveOrUpdateWithCommodityType(TypeConsumptionBo typeConsumptionBo);
}

