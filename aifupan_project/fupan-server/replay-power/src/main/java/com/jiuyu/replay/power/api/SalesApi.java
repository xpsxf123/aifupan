package com.jiuyu.replay.power.api;


import com.jiuyu.replay.generic.bo.power.SalesListBo;
import com.jiuyu.replay.generic.feign.power.SalesFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesListVo;
import com.jiuyu.replay.power.producer.SalesProducer;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class SalesApi implements SalesFeign {

    @Resource
    private SalesProducer salesProducer;

    @Override
    public PageUtils<SalesListVo> queryPage(SalesListBo salesListBo) {
        return salesProducer.queryPage(salesListBo);
    }

    /**
     * 获取可轮询的 销售id
     * @param salesId
     * @return
     */
    public R<SalesInfoVo> exCant(Long salesId) {
        if (salesId == null){
            return R.ok();
        }
        return R.ok(salesProducer.info(salesId));
    }


    /**
     * 获取可轮询的 销售id
     * @param salesIds
     * @return
     */
    @Override
    public R<List<Long>> exCantIds(List<Long> salesIds) {
        if (salesIds==null || salesIds.isEmpty()){
            return R.ok(Collections.emptyList());
        }
        //通过销售ids 以及 不开启轮询的筛选  查询销售
        List<SalesInfoVo> voList = this.salesProducer.listByIdsFilterNoChoose(salesIds);
        if (voList!=null&&!voList.isEmpty()){
            List<Long> ids = voList.stream()
                    .map(SalesInfoVo::getId).toList();
            return R.ok(ids);
        }
        return R.ok(Collections.emptyList());
    }

    @Override
    public SalesInfoVo getByUserId(Long userId) {
        return salesProducer.getByUserId(userId);
    }

    @Override
    public SalesInfoVo getBySalesUserId(Long userId) {
        return salesProducer.getBySalesUserId(userId);
    }

    @Override
    public List<SalesInfoVo> listByUserIds(List<Long> userIds) {
        return salesProducer.listByUserIds(userIds);
    }

    @Override
    public List<SalesInfoVo> listByAndAgentSalesTypeId(Long agentId, Integer salesType, Integer userPolling) {
        return salesProducer.listBySalesTypeAndAgentId(salesType, agentId, userPolling);
    }

    @Override
    public SalesInfoVo getById(Long saleId) {
        return salesProducer.info(saleId);
    }

    @Override
    public void updateEmployeeStatusByAgentId(Long agentId, Integer employeeStatus) {
        salesProducer.updateEmployeeStatusByAgentId(agentId, employeeStatus);
    }
}
