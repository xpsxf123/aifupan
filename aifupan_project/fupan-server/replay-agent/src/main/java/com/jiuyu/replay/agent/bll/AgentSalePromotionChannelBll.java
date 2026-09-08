package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelListVo;
import com.jiuyu.replay.agent.vo.AgentSalePromotionChannelInfoVo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelBo;
import com.jiuyu.replay.agent.bo.AgentSalePromotionChannelListBo;
import com.jiuyu.replay.agent.producer.AgentSalePromotionChannelProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 代理商销售-渠道关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Component
public class AgentSalePromotionChannelBll {

    @Resource
    private AgentSalePromotionChannelProducer agentSalePromotionChannelProducer;


    /**
     * 代理商销售-渠道关联表列表
     * @param agentSalePromotionChannelListBo 代理商销售-渠道关联表列表查询参数
     * @return
     */
    public R<PageUtils<AgentSalePromotionChannelListVo>> queryPage(AgentSalePromotionChannelListBo agentSalePromotionChannelListBo) {

        return R.ok("获取成功", agentSalePromotionChannelProducer.queryPage(agentSalePromotionChannelListBo));
    }

    /**
    * 代理商销售-渠道关联表信息
    * @param id 代理商销售-渠道关联表id
    * @return
    */
    public R<AgentSalePromotionChannelInfoVo> info(Long id) {

        AgentSalePromotionChannelInfoVo agentSalePromotionChannelInfoVo = agentSalePromotionChannelProducer.info(id);
        return R.ok("获取成功", agentSalePromotionChannelInfoVo);
    }

    /**
     * 新增代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
    public R<String> save(AgentSalePromotionChannelBo agentSalePromotionChannelBo) {

        AgentSalePromotionChannelInfoVo agentSalePromotionChannelInfoVo = agentSalePromotionChannelProducer.save(agentSalePromotionChannelBo);
        return R.ok("添加成功");
    }

    /**
     * 修改代理商销售-渠道关联表
     * @param agentSalePromotionChannelBo 代理商销售-渠道关联表对象
     * @return
     */
    public R<String> update(AgentSalePromotionChannelBo agentSalePromotionChannelBo) {

        agentSalePromotionChannelProducer.update(agentSalePromotionChannelBo);
        return R.ok("修改成功");
    }

    /**
     * 删除代理商销售-渠道关联表
     * @param id 代理商销售-渠道关联表id
     * @return
     */
    public R<String> delete(Long id) {

        agentSalePromotionChannelProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

