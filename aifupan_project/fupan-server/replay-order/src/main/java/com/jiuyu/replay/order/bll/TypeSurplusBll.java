package com.jiuyu.replay.order.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.vo.*;
import com.jiuyu.replay.order.bo.TypeSurplusBo;
import com.jiuyu.replay.order.bo.TypeSurplusListBo;
import com.jiuyu.replay.order.producer.TypeSurplusProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 商品类型资产剩余表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Component
public class TypeSurplusBll {

    @Resource
    private TypeSurplusProducer typeSurplusProducer;


    /**
     * 商品类型资产剩余表列表
     * @param typeSurplusListBo 商品类型资产剩余表列表查询参数
     * @return
     */
    public R<PageUtils<TypeSurplusListVo>> queryPage(TypeSurplusListBo typeSurplusListBo) {

        return R.ok("获取成功", typeSurplusProducer.queryPage(typeSurplusListBo));
    }

    /**
    * 商品类型资产剩余表信息
    * @param id 商品类型资产剩余表id
    * @return
    */
    public R<TypeSurplusInfoVo> info(Long id) {

        TypeSurplusInfoVo typeSurplusInfoVo = typeSurplusProducer.info(id);
        return R.ok("获取成功", typeSurplusInfoVo);
    }

    /**
     * 新增商品类型资产剩余表
     * @param typeSurplusBo 商品类型资产剩余表对象
     * @return
     */
    public R<String> save(TypeSurplusBo typeSurplusBo) {

        TypeSurplusInfoVo typeSurplusInfoVo = typeSurplusProducer.save(typeSurplusBo);
        return R.ok("添加成功");
    }

    /**
     * 修改商品类型资产剩余表
     * @param typeSurplusBo 商品类型资产剩余表对象
     * @return
     */
    public R<String> update(TypeSurplusBo typeSurplusBo) {

        typeSurplusProducer.update(typeSurplusBo);
        return R.ok("修改成功");
    }

    /**
     * 删除商品类型资产剩余表
     * @param id 商品类型资产剩余表id
     * @return
     */
    public R<String> delete(Long id) {

        typeSurplusProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 获取当前商品剩余数量
     * @param propertyId
     * @param commodityTypeId
     * @return
     */
    public R<List<TypeSurplusInfoVo>> currentSurplus(Long propertyId, Long commodityTypeId) {
        return R.ok(typeSurplusProducer.currentSurplus(propertyId, commodityTypeId));
    }

    public void updateBatch(List<TypeSurplusBo> typeSurplusBos) {
        typeSurplusProducer.updateBatch(typeSurplusBos);
    }


    public void selectByDetailIds(List<ClintGetDataVo> details) {
        typeSurplusProducer.selectByDetailIds(details);
    }
}

