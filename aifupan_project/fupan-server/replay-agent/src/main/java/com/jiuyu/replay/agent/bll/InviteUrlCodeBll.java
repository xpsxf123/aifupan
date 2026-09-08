package com.jiuyu.replay.agent.bll;

import com.jiuyu.replay.agent.bo.InviteUrlCodeInfoBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.constant.Constant;
import com.jiuyu.replay.agent.producer.AgentPlatformSaleProducer;
import com.jiuyu.replay.agent.producer.AgentProducer;
import com.jiuyu.replay.agent.producer.AgentPromotionProducer;
import com.jiuyu.replay.agent.producer.InviteUrlCodeProducer;
import com.jiuyu.replay.agent.rse.AgentRse;
import com.jiuyu.replay.agent.rse.InviteUrlCodeRse;
import com.jiuyu.replay.agent.vo.AgentPromotionInfoVo;
import com.jiuyu.replay.agent.vo.InviteUrlPromotionVo;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.common.producer.FileProducer;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeInfoConditionBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeListBo;
import com.jiuyu.replay.generic.feign.activity.ActivityFeign;
import com.jiuyu.replay.generic.feign.common.FileFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityInfoVo;
import com.jiuyu.replay.generic.vo.agent.AgentInfoVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeListVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Component
public class InviteUrlCodeBll {

    @Resource
    private InviteUrlCodeProducer inviteUrlCodeProducer;
    @Resource
    private AgentPlatformSaleProducer agentPlatformSaleProducer;
    @Resource
    private AgentPromotionProducer agentPromotionProducer;
    @Resource
    private AgentProducer agentProducer;
    @Resource
    private FileProducer fileProducer;
    @Resource
    private AgentProperties agentProperties;
    @Resource
    private UserFeign userFeign;
    @Resource
    private ActivityFeign activityFeign;
    @Resource
    private InviteUrlCodeRse inviteUrlCodeRse;
    @Resource
    private AgentRse agentRse;
    @Resource
    private FileFeign fileFeign;
    @Resource
    RedissonClient redissonClient;


    /**
     * 邀请链接的code列表
     *
     * @param inviteUrlCodeListBo 邀请链接的code列表查询参数
     * @return
     */
    public R<PageUtils<InviteUrlCodeListVo>> queryPage(InviteUrlCodeListBo inviteUrlCodeListBo) {

        return R.ok("获取成功", inviteUrlCodeProducer.queryPage(inviteUrlCodeListBo));
    }

    /**
     * 邀请链接的code信息
     *
     * @param id 邀请链接的codeid
     * @return
     */
    public R<InviteUrlCodeInfoVo> info(Long id) {

        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeProducer.info(id);
        return R.ok("获取成功", inviteUrlCodeInfoVo);
    }

    /**
     * 新增邀请链接的code
     *
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    public R<String> save(InviteUrlCodeBo inviteUrlCodeBo) {

        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeProducer.save(inviteUrlCodeBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请链接的code
     *
     * @param inviteUrlCodeBo 邀请链接的code对象
     * @return
     */
    public R<String> update(InviteUrlCodeBo inviteUrlCodeBo) {

        inviteUrlCodeProducer.update(inviteUrlCodeBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请链接的code
     *
     * @param id 邀请链接的codeid
     * @return
     */
    public R<String> delete(Long id) {

        inviteUrlCodeProducer.deleteById(id);
        return R.ok("删除成功");
    }


    public R<InviteUrlCodeInfoVo> getByInviteUrlCode(String inviteUrlCode) {

        return R.ok("获取成功", inviteUrlCodeProducer.getByInviteUrlCode(inviteUrlCode));
    }

    /**
     * 根据邀请链接的code获取信息
     *
     * @param code 邀请链接的code
     * @return
     */
    public R<InviteUrlCodeInfoVo> infoByCode(String code) {

        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeProducer.infoByCode(code);
        if (inviteUrlCodeInfoVo != null) {

//            // 轮询分配一个平台销售
//            AgentPlatformSaleInfoVo platformSaleInfoVo = this.agentPlatformSaleProducer.getPollingSaleId(inviteUrlCodeInfoVo.getAgentId());
//            if(platformSaleInfoVo != null) {
//                inviteUrlCodeInfoVo.setSaleId(platformSaleInfoVo.getSaleId());
//                // 封装二维码图片
//                if(platformSaleInfoVo.getChannelQrcodeImgId() != null) {
//                    inviteUrlCodeInfoVo.setSaleQrcodeImg(this.fileProducer.infoByFileId(platformSaleInfoVo.getChannelQrcodeImgId()));
//                }
//            }
            //查询代理商推广渠道
            if (inviteUrlCodeInfoVo.getCodeType() ==1){
                AgentPromotionInfoVo promotionInfoVo = agentPromotionProducer.info(inviteUrlCodeInfoVo.getPromotionId());
                if (promotionInfoVo != null){
                    inviteUrlCodeInfoVo.setBtnBgColor(promotionInfoVo.getBtnBgColor());
                    inviteUrlCodeInfoVo.setBtContent(promotionInfoVo.getBtContent());
                    // 封装海报图片
                    if(!StringUtil.isEmpty(promotionInfoVo.getPosterImgIds())) {
                        String[] imgIds = promotionInfoVo.getPosterImgIds().split("_");
                        if(imgIds.length > 0) {
                            List<Long> imgIdList = Arrays.stream(imgIds).map(Long::valueOf).collect(Collectors.toList());
                            inviteUrlCodeInfoVo.setPosterImgList(this.fileProducer.listByFileIds(imgIdList));
                        }
                    }
                }
            }



            // 封装代理商信息
            AgentInfoVo agentInfoVo = this.agentRse.info(inviteUrlCodeInfoVo.getAgentId(), 1);
            if (agentInfoVo != null) {
                inviteUrlCodeInfoVo.setAgentId(agentInfoVo.getId());
                inviteUrlCodeInfoVo.setBtnBgColor(StringUtil.isBlank(inviteUrlCodeInfoVo.getBtnBgColor())?agentInfoVo.getBtnBgColor():inviteUrlCodeInfoVo.getBtnBgColor());
                inviteUrlCodeInfoVo.setBtContent(StringUtil.isBlank(inviteUrlCodeInfoVo.getBtContent())?agentInfoVo.getBtContent():inviteUrlCodeInfoVo.getBtContent());
                // 封装海报图片
                if(!StringUtils.isEmpty(agentInfoVo.getPosterImgIds())) {
                    if (inviteUrlCodeInfoVo.getPosterImgList()  == null ||inviteUrlCodeInfoVo.getPosterImgList().isEmpty()){
                        String[] imgIds = agentInfoVo.getPosterImgIds().split("_");
                        if(imgIds.length > 0) {
                            List<Long> imgIdList = Arrays.stream(imgIds).map(Long::valueOf).collect(Collectors.toList());
                            inviteUrlCodeInfoVo.setPosterImgList(this.fileProducer.listByFileIds(imgIdList));
                        }
                    }
                }
            }

        }
        return R.ok("获取成功", inviteUrlCodeInfoVo);
    }

    /**
     * 获取当前用户的邀请链接
     *
     * @return
     */
    public R<String> getUserInviteUrl() {

        R<InviteUrlCodeInfoVo> inviteUrlCodeInfoVoR = this.getUserInviteUrlCodeInfo();
        if (inviteUrlCodeInfoVoR.getData() != null) {
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeInfoVoR.getData();
            return R.ok("获取成功", agentProperties.getUrl() + inviteUrlCodeInfoVo.getUrlCode());
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "生成邀请链接失败");

    }

    /**
     * 获取当前用户的邀请链接
     *
     * @return
     */
    public R<InviteUrlCodeInfoVo> getUserInviteUrlCodeInfo() {
        // 获取用户信息
        R<UserCacheVo> userR = userFeign.getLocalUser();
        if (userR == null || userR.getData() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户信息不存在");
        }
        UserCacheVo user = userR.getData();
        Long tenantId = user.getActiveTenantId();
        Long userId = user.getId();
        if (user.getUserType() == 2) {
            // 子账号，用父账号的用户id
            userId = user.getParentId();
        }

        // 获取活动信息
        R<ClientInviteActivityInfoVo> activityInfoVoR = this.activityFeign.infoActivateById(null);
        ClientInviteActivityInfoVo activityInfoVo = activityInfoVoR.getData();
        if (activityInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "活动已结束");
        }
        RLock lock = redissonClient.getLock(LockKeyPrefix.USER.getLockKey("inviteUrlCode:" + userId));
        try {
            // 获取链接code
            InviteUrlCodeInfoConditionBo inviteUrlCodeInfoConditionBo = new InviteUrlCodeInfoConditionBo();
            inviteUrlCodeInfoConditionBo.setUserId(userId);
            inviteUrlCodeInfoConditionBo.setTenantId(tenantId);
            inviteUrlCodeInfoConditionBo.setActivityId(activityInfoVo.getId());
            inviteUrlCodeInfoConditionBo.setCodeType(2);
            InviteUrlCodeInfoVo inviteUrlCodeInfoVo = this.inviteUrlCodeRse.infoByCondition(inviteUrlCodeInfoConditionBo);
            if (inviteUrlCodeInfoVo != null) {
                return R.ok("获取成功", inviteUrlCodeInfoVo);
            }
            // 当前用户没有邀请链接，生成邀请链接
            AgentInfoVo agentInfoVo = this.agentRse.info(activityInfoVo.getAgentId(), 1);
            if (agentInfoVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前没有配置代理商信息");
            }
            // 保存链接信息
            InviteUrlCodeBo inviteUrlCodeBo = new InviteUrlCodeBo();
            inviteUrlCodeBo.setAgentId(agentInfoVo.getId());
            inviteUrlCodeBo.setUserId(userId);
            inviteUrlCodeBo.setSubUserId(user.getUserType() == 2 ? user.getId() : null);
            inviteUrlCodeBo.setActivityId(activityInfoVo.getId());
            inviteUrlCodeBo.setTenantId(tenantId);
            inviteUrlCodeBo.setCodeType(2);
            InviteUrlCodeInfoVo urlCodeInfoVo = inviteUrlCodeRse.save(inviteUrlCodeBo);
            return R.ok("获取成功", urlCodeInfoVo);
        } catch (Exception e) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), e.getMessage());
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取用户邀请码信息
     *
     * @param activityId 活动id
     * @param userId     邀请用户id
     * @return
     */
    public InviteUrlCodeInfoBo getUserInviteUrlCodeByActivityIdUserId(Long activityId, Long userId) {
        return inviteUrlCodeRse.getUserInviteUrlCodeByActivityIdUserId(activityId, userId);
    }


    public List<InviteUrlPromotionVo> getByCodes(List<String> inUrlCodes) {
        return inviteUrlCodeProducer.getByCodes(inUrlCodes);
    }

    public List<String> selectQuery(List<Long> promotionIds) {
        return inviteUrlCodeProducer.selectQuery(promotionIds);
    }

    public List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(List<String> inviteUrlCodes) {
        return inviteUrlCodeRse.inviteCodeAndPromotionName(inviteUrlCodes);
    }
}

