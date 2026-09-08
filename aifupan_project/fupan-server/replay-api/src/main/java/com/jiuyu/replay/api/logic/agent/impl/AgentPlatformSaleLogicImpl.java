package com.jiuyu.replay.api.logic.agent.impl;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.agent.bll.AgentPlatformSaleBll;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleBo;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleListBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.producer.AgentProducer;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleListVo;
import com.jiuyu.replay.api.logic.agent.AgentPlatformSaleLogic;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.power.bll.SalesBll;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 代理商平台销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Service
public class AgentPlatformSaleLogicImpl implements AgentPlatformSaleLogic {

    @Resource
    private AgentPlatformSaleBll agentPlatformSaleBll;
    @Resource
    private FileBll fileBll;
    @Resource
    private SalesBll salesBll;
    @Resource
    private AgentProducer agentProducer;
    @Resource
    private AgentProperties agentProperties;

    @Override
    public R<PageUtils<AgentPlatformSaleListVo>> queryPage(AgentPlatformSaleListBo agentPlatformSaleListBo) {

        return agentPlatformSaleBll.queryPage(agentPlatformSaleListBo);
    }

    @Override
    public R<AgentPlatformSaleInfoVo> info(Long id) {

        R<AgentPlatformSaleInfoVo> platformSaleInfoVoR = agentPlatformSaleBll.info(id);
        AgentPlatformSaleInfoVo platformSaleInfoVo = platformSaleInfoVoR.getData();
        if(platformSaleInfoVo != null) {
            R<SalesInfoVo> salesInfoVoR = this.salesBll.info(platformSaleInfoVo.getSaleId());
            if(salesInfoVoR.getData() != null) {
                SalesInfoVo salesInfoVo = salesInfoVoR.getData();
                FileShowVo fileShowVo = fileBll.infoByFileId(salesInfoVo.getQrcodeImgId());
                platformSaleInfoVo.setChannelQrcodeImg(fileShowVo);
            }

        }

        return platformSaleInfoVoR;
    }

    @Override
    public R<String> save(AgentPlatformSaleBo agentPlatformSaleBo) {

        return agentPlatformSaleBll.save(agentPlatformSaleBo);
    }

    @Override
    public R<String> update(AgentPlatformSaleBo agentPlatformSaleBo) {

        return agentPlatformSaleBll.update(agentPlatformSaleBo);
    }

    @Override
    public R<String> delete(Long id) {

        return agentPlatformSaleBll.delete(id);
    }

    @Override
    public R<List<AgentPlatformSaleInfoVo>> listByAgentId(Long agentId, Integer salesType) {

        R<List<AgentPlatformSaleInfoVo>> listR = agentPlatformSaleBll.listByAgentId(agentId, salesType);
        List<AgentPlatformSaleInfoVo> agentPlatformSaleInfoVos = listR.getData();
        if (agentPlatformSaleInfoVos != null && !agentPlatformSaleInfoVos.isEmpty()) {

            // 封装销售人员推广码
            AgentInfoVo agent = agentProducer.info(agentId, null);
            if (agent != null) {
                for (AgentPlatformSaleInfoVo agentPlatformSale : agentPlatformSaleInfoVos) {
                    agentPlatformSale.setUrl(StrUtil.format("{}{}&salesId={}", agentProperties.getUrl(), agent.getAgentUrlCode(), agentPlatformSale.getSaleId()));
                }
            }

        }

        return listR;
    }


}

