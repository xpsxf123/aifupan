package com.jiuyu.replay.api.logic.power.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.agent.bll.AgentBll;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.api.logic.power.UserDetailsLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bll.UserDetailsBll;
import com.jiuyu.replay.power.bo.UserDetailsBo;
import com.jiuyu.replay.power.vo.UserDetailsInfoVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;


/**
 * 用户详情表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@Service
public class UserDetailsLogicImpl implements UserDetailsLogic {

    @Resource
    private UserDetailsBll userDetailsBll;
    @Resource
    private UserBll userBll;
    @Resource
    private AgentBll agentBll;



    /**
     * 客户端保存或修改用户详情接口
     * @param userDetailsBo
     * @return
     */
    @Override
    public R<String> saveUserDetails(UserDetailsBo userDetailsBo) {
        if (ObjectUtil.isEmpty(userDetailsBo.getUserId())) {
            return R.error(30001, "用户id不能为空");
        }
        //校验视频会议地址-长度<=500
        if (StringUtil.isNotBlank(userDetailsBo.getVideoMeetPath())&& userDetailsBo.getVideoMeetPath().length() >= 500){
            return R.error(30001, "视频会议地址过长");
        }

        // 获取旧的用户详情记录
        R<UserDetailsInfoVo> oldUserDetailsInfoR = this.userDetailsBll.getByUserId(userDetailsBo.getUserId());
        if(oldUserDetailsInfoR.getData() != null) {
            UserDetailsInfoVo oldUserDetailsInfo = oldUserDetailsInfoR.getData();
            if(oldUserDetailsInfo.getChannelId() != null && !oldUserDetailsInfo.getChannelId().equals(userDetailsBo.getChannelId())) {
                // 用户换了渠道，判断旧渠道的状态，如果已经停用，允许修改为新的渠道
                R<AgentInfoVo> agentInfoVoR = this.agentBll.infoByChannelId(userDetailsBo.getChannelId());
                AgentInfoVo agentInfoVo = agentInfoVoR.getData();
                if (agentInfoVo != null) {
                    userDetailsBo.setAgentId(agentInfoVo.getId());
                } else {
                    userDetailsBo.setAgentId(0L);
                }
            }
        }

        return userDetailsBll.saveUserDetails(userDetailsBo);
    }
}

