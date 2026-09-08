package com.jiuyu.replay.order.bll;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.producer.TypeConsumptionProducer;
import com.jiuyu.replay.order.vo.TypeConsumptionInfoVo;
import com.jiuyu.replay.order.vo.TypeConsumptionVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 套餐表(用户版本)
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Component
@AllArgsConstructor
public class TypeConsumptionBll {

    private final TypeConsumptionProducer typeConsumptionProducer;

    /**
     * 根据商品id和类型查询商品类型用量关联表列表
     * @param sourceId
     * @param type
     * @return
     */
    public R<List<TypeConsumptionInfoVo>> listBySourceId(Long sourceId, Integer type){
        List<TypeConsumptionVo> typeConsumptionVos = typeConsumptionProducer.listBySourceId(sourceId, type);
        return R.ok(BeanUtil.copyToList(typeConsumptionVos, TypeConsumptionInfoVo.class));
    }

}
