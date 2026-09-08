package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.agent.bo.CommissionAllocationBo;
import com.jiuyu.replay.agent.vo.CommissionRecordsVo;
import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.agent.vo.AgentCommissionListVo;
import com.jiuyu.replay.agent.vo.AgentCommissionInfoVo;
import com.jiuyu.replay.agent.bo.AgentCommissionBo;
import com.jiuyu.replay.agent.bo.AgentCommissionListBo;

import java.util.List;


/**
 * 代理商佣金
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface AgentCommissionProducer {


    /**
     * 代理商佣金列表
     * @param agentCommissionListBo 代理商佣金列表查询参数
     * @return
     */
    PageUtils<AgentCommissionListVo> queryPage(AgentCommissionListBo agentCommissionListBo);

    /**
    * 代理商佣金信息
    * @param id 代理商佣金id
    * @return
    */
    AgentCommissionInfoVo info(Long id);

    /**
     * 新增代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
     AgentCommissionInfoVo save(AgentCommissionBo agentCommissionBo);

    /**
     * 修改代理商佣金
     * @param agentCommissionBo 代理商佣金对象
     * @return
     */
    void update(AgentCommissionBo agentCommissionBo);

    /**
     * 删除代理商佣金
     * @param id 代理商佣金id
     * @return
     */
    void deleteById(Long id);


    /**
     * 分佣
     *
     * @param allocationBo
     * @return
     */
    boolean commissionAllocation(CommissionAllocationBo allocationBo);

    /**
     * 佣金结算记录
     * @param agentId
     * @return
     */
    List<CommissionRecordsVo> commissionRecords(Long agentId);

    /**
     * 根据订单id查询佣金信息
     * @param orderIds
     * @return
     */
    List<AgentCommissionInfoVo> listByOrderIds(List<Long> orderIds);

    /**
     * 根据订单id查询佣金信息
     * @param orderId
     * @return
     */
    AgentCommissionInfoVo getByOrderId(Long orderId);
}

