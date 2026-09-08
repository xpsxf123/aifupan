package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.AgentPlatformSaleBo;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleListBo;
import com.jiuyu.replay.agent.constant.Constant;
import com.jiuyu.replay.agent.producer.AgentPlatformSaleProducer;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleListVo;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.power.SalesFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 代理商平台销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Component
public class AgentPlatformSaleBll {

    @Resource
    private AgentPlatformSaleProducer agentPlatformSaleProducer;
    @Resource
    private SalesFeign salesFeign;


    /**
     * 代理商平台销售列表
     * @param agentPlatformSaleListBo 代理商平台销售列表查询参数
     * @return
     */
    public R<PageUtils<AgentPlatformSaleListVo>> queryPage(AgentPlatformSaleListBo agentPlatformSaleListBo) {

        return R.ok("获取成功", agentPlatformSaleProducer.queryPage(agentPlatformSaleListBo));
    }

    /**
    * 代理商平台销售信息
    * @param id 代理商平台销售id
    * @return
    */
    public R<AgentPlatformSaleInfoVo> info(Long id) {

        AgentPlatformSaleInfoVo agentPlatformSaleInfoVo = agentPlatformSaleProducer.info(id);
        return R.ok("获取成功", agentPlatformSaleInfoVo);
    }

    /**
     * 新增代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    @CustomRedissonLock(key = "'replay:lock:agentPlatformSaleAdd'")
    public R<String> save(AgentPlatformSaleBo agentPlatformSaleBo) {
        RRException.isNotEmpty(agentPlatformSaleBo.getSaleId(), "销售不能为空");
        // 判断代理商销售是否是这个代理商的
        SalesInfoVo sales = salesFeign.getById(agentPlatformSaleBo.getSaleId());
        if (sales.getSalesType() == UserEnums.salesType.AGENT.getCode() && !sales.getAgentId().equals(agentPlatformSaleBo.getAgentId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该销售不是该代理商的");
        }

        // 判断平台销售是否已存在
        AgentPlatformSaleInfoVo oldPlatformSaleInfoVo = this.agentPlatformSaleProducer.infoByAgentIdAndSaleId(agentPlatformSaleBo.getAgentId(), agentPlatformSaleBo.getSaleId());
        if(oldPlatformSaleInfoVo != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该销售已存在");
        }

        AgentPlatformSaleInfoVo agentPlatformSaleInfoVo = agentPlatformSaleProducer.save(agentPlatformSaleBo);
        return R.ok("添加成功");
    }

    /**
     * 修改代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    public R<String> update(AgentPlatformSaleBo agentPlatformSaleBo) {

        AgentPlatformSaleInfoVo oldPlatformSaleInfoVo = this.agentPlatformSaleProducer.info(agentPlatformSaleBo.getId());
        if(oldPlatformSaleInfoVo != null) {

            if(!oldPlatformSaleInfoVo.getSaleId().equals(agentPlatformSaleBo.getSaleId())) {
                // 判断平台销售是否已存在
                AgentPlatformSaleInfoVo saleInfoVo = this.agentPlatformSaleProducer.infoByAgentIdAndSaleId(agentPlatformSaleBo.getAgentId(), agentPlatformSaleBo.getSaleId());
                if(saleInfoVo != null) {
                    return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该销售已存在");
                }
            }

            agentPlatformSaleProducer.update(agentPlatformSaleBo);
            return R.ok("修改成功");
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "销售信息不存在");
    }

    /**
     * 删除代理商平台销售
     * @param id 代理商平台销售id
     * @return
     */
    public R<String> delete(Long id) {

        agentPlatformSaleProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据代理商id获取平台销售列表
     *
     * @param agentId   代理商id
     * @param salesType
     * @return
     */
    public R<List<AgentPlatformSaleInfoVo>> listByAgentId(Long agentId, Integer salesType) {
        return R.ok(agentPlatformSaleProducer.listByAgentId(agentId, salesType));
    }

    /**
     * 根据代理商id和销售id获取平台销售信息
     * @param agentId 代理商id
     * @param saleId 销售id
     * @return
     */
    public R<AgentPlatformSaleInfoVo> infoByAgentIdAndSaleId(Long agentId, Long saleId) {

        AgentPlatformSaleInfoVo agentPlatformSaleInfoVo = agentPlatformSaleProducer.infoByAgentIdAndSaleId(agentId, saleId);

        return R.ok(agentPlatformSaleInfoVo);
    }

    /**
     * 轮询获取代理的平台销售
     * @param agentId 代理商id
     * @return
     */
    public Long getPollingSaleId(Long agentId) {
        return this.agentPlatformSaleProducer.getPollingSaleId(agentId);
    }
}

