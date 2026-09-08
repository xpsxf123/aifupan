package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.AgentPromotionBo;
import com.jiuyu.replay.agent.bo.AgentPromotionListBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.producer.AgentPromotionProducer;
import com.jiuyu.replay.agent.producer.InviteUrlCodeProducer;
import com.jiuyu.replay.agent.vo.AgentPromotionInfoVo;
import com.jiuyu.replay.agent.vo.AgentPromotionListVo;
import com.jiuyu.replay.agent.vo.InviteUrlPromotionVo;
import com.jiuyu.replay.common.producer.FileProducer;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 代理商推广渠道
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Component
public class AgentPromotionBll {

    @Resource
    private AgentPromotionProducer agentPromotionProducer;
    @Resource
    private InviteUrlCodeProducer inviteUrlCodeProducer;
    @Resource
    private AgentProperties agentProperties;
    @Resource
    private FileProducer fileProducer;


    /**
     * 代理商推广渠道列表
     * @param agentPromotionListBo 代理商推广渠道列表查询参数
     * @return
     */
    public R<PageUtils<AgentPromotionListVo>> queryPage(AgentPromotionListBo agentPromotionListBo) {

        return R.ok("获取成功", agentPromotionProducer.queryPage(agentPromotionListBo));
    }

    /**
    * 代理商推广渠道信息
    * @param id 代理商推广渠道id
    * @return
    */
    public R<AgentPromotionInfoVo> info(Long id) {
        AgentPromotionInfoVo agentPromotionInfoVo = agentPromotionProducer.info(id);
        if(!StringUtil.isEmpty(agentPromotionInfoVo.getPosterImgIds())) {
            String[] imgIds = agentPromotionInfoVo.getPosterImgIds().split("_");
            if(imgIds.length > 0) {
                List<Long> imgIdList = Arrays.stream(imgIds).map(Long::valueOf).collect(Collectors.toList());
                agentPromotionInfoVo.setPosterImgList(this.fileProducer.listByFileIds(imgIdList));
            }
        }
        return R.ok("获取成功", agentPromotionInfoVo);
    }

    /**
     * 新增代理商推广渠道
     * @param agentPromotionBo 代理商推广渠道对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(AgentPromotionBo agentPromotionBo) {

        Long agentPromotionId = SnowflakeManager.nextValue();
        agentPromotionBo.setId(agentPromotionId);

        // 设置邀请链接code
        InviteUrlCodeBo inviteUrlCodeBo = new InviteUrlCodeBo();
        inviteUrlCodeBo.setAgentId(agentPromotionBo.getAgentId());
        inviteUrlCodeBo.setPromotionId(agentPromotionId);
        inviteUrlCodeBo.setCodeType(1);
        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeProducer.save(inviteUrlCodeBo);
        agentPromotionBo.setPromotionUrlCode(inviteUrlCodeInfoVo.getUrlCode());

        AgentPromotionInfoVo agentPromotionInfoVo = agentPromotionProducer.save(agentPromotionBo);
        return R.ok("添加成功");
    }

    /**
     * 修改代理商推广渠道
     * @param agentPromotionBo 代理商推广渠道对象
     * @return
     */
    public R<String> update(AgentPromotionBo agentPromotionBo) {

        agentPromotionProducer.update(agentPromotionBo);
        return R.ok("修改成功");
    }

    /**
     * 删除代理商推广渠道
     * @param id 代理商推广渠道id
     * @return
     */
    public R<String> delete(Long id) {

        agentPromotionProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取代理商的推广渠道列表
     * @param agentId 代理商id
     * @return
     */
    public R<List<AgentPromotionInfoVo>> listByAgentId(Long agentId) {

        List<AgentPromotionInfoVo> agentPromotionInfoVos = this.agentPromotionProducer.listByAgentId(agentId);

        return R.ok(agentPromotionInfoVos);
    }

    public List<InviteUrlPromotionVo> getNameById(List<InviteUrlPromotionVo> urlPromotionVos) {
        return agentPromotionProducer.getNameById(urlPromotionVos);
    }

    public List<Long> selectQuery(AgentPromotionBo agentPromotionBo) {
        return agentPromotionProducer.selectQuery(agentPromotionBo);
    }
}

