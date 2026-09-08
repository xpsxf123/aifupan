package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.AgentCommissionBo;
import com.jiuyu.replay.agent.bo.AgentCommissionListBo;
import com.jiuyu.replay.agent.bo.CommissionAllocationBo;
import com.jiuyu.replay.agent.producer.AgentCommissionProducer;
import com.jiuyu.replay.agent.vo.AgentCommissionInfoVo;
import com.jiuyu.replay.agent.vo.AgentCommissionListVo;
import com.jiuyu.replay.agent.vo.CommissionRecordsVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 代理商佣金
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Component
public class AgentCommissionBll {

    @Resource
    private AgentCommissionProducer agentCommissionProducer;


    /**
     * 代理商佣金列表
     * @param agentCommissionListBo 代理商佣金列表查询参数
     * @return
     */
    public R<PageUtils<AgentCommissionListVo>> queryPage(AgentCommissionListBo agentCommissionListBo) {

        return R.ok("获取成功", agentCommissionProducer.queryPage(agentCommissionListBo));
    }

    /**
    * 代理商佣金信息
    * @param id 代理商佣金id
    * @return
    */
    public R<AgentCommissionInfoVo> info(Long id) {

        AgentCommissionInfoVo agentCommissionInfoVo = agentCommissionProducer.info(id);
        return R.ok("获取成功", agentCommissionInfoVo);
    }

    /**
     * 新增代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
    public R<String> save(AgentCommissionBo agentCommissionBo) {

        AgentCommissionInfoVo agentCommissionInfoVo = agentCommissionProducer.save(agentCommissionBo);
        return R.ok("添加成功");
    }

    /**
     * 修改代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
    public R<String> update(AgentCommissionBo agentCommissionBo) {

        agentCommissionProducer.update(agentCommissionBo);
        return R.ok("修改成功");
    }

    /**
     * 删除代理商佣金
     * @param id 代理商佣金id
     * @return
     */
    public R<String> delete(Long id) {

        agentCommissionProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 分佣
     * @param allocationBo
     * @return
     */
    public R<Boolean> commissionAllocation(CommissionAllocationBo allocationBo){
        return R.ok("分佣成功", agentCommissionProducer.commissionAllocation(allocationBo));
    }

    /**
     * 佣金结算记录
     * @param agentId
     * @return
     */
    public R<List<CommissionRecordsVo>> commissionRecords(Long agentId) {
        return R.ok("获取成功", agentCommissionProducer.commissionRecords(agentId));
    }

    /**
     * 根据订单id查询佣金
     * @param orderIds
     * @return
     */
    public R<List<AgentCommissionInfoVo>> listByOrderIds(List<Long> orderIds) {
        return R.ok("获取成功", agentCommissionProducer.listByOrderIds(orderIds));
    }

    /**
     * 根据订单id查询佣金
     * @param orderId
     * @return
     */
    public R<AgentCommissionInfoVo> getByOrderId(Long orderId) {
        return R.ok("获取成功", agentCommissionProducer.getByOrderId(orderId));
    }
}

