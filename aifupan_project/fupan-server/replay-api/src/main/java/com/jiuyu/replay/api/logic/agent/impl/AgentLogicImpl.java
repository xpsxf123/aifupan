package com.jiuyu.replay.api.logic.agent.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.agent.bll.AgentBll;
import com.jiuyu.replay.agent.bo.AgentBo;
import com.jiuyu.replay.agent.bo.AgentListBo;
import com.jiuyu.replay.agent.constant.AgentProperties;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentListVo;
import com.jiuyu.replay.agent.vo.AgentVo;
import com.jiuyu.replay.api.logic.agent.AgentLogic;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.words.bll.TradeBll;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 代理商
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
@Slf4j
public class AgentLogicImpl implements AgentLogic {

    @Resource
    private AgentBll agentBll;
    @Resource
    private UserBll userBll;
    @Resource
    private TradeBll tradeBll;
    @Resource
    private FileBll fileBll;
    @Resource
    private AgentProperties agentProperties;


    @Override
    public R<PageUtils<AgentListVo>> queryPage(AgentListBo agentListBo) {

        R<PageUtils<AgentListVo>> pageUtilsR = agentBll.queryPage(agentListBo);
        if(pageUtilsR.getCode() == 0 && pageUtilsR.getData() != null) {
            PageUtils<AgentListVo> pageUtils = pageUtilsR.getData();
            List<AgentListVo> agentList = pageUtils.getList();
            if(agentList != null && agentList.size() > 0) {

                // 获取运营人员列表
                Set<Long> operationUserIds = agentList.stream().map(AgentVo::getOperationUserId).collect(Collectors.toSet());
                R<List<UserListVo>> userListR = this.userBll.listByIds(operationUserIds);
                List<UserListVo> userList = userListR.getData();
                // 获取行业列表
                Set<Long> tradeIds = agentList.stream().map(AgentVo::getTradeId).collect(Collectors.toSet());
                R<List<TradeListVo>> tradeListR = this.tradeBll.listByIds(tradeIds);
                List<TradeListVo> tradeList = tradeListR.getData();
                // 获取创建人列表
                Set<Long> createUserIds = agentList.stream().map(AgentVo::getCreateUserId).collect(Collectors.toSet());
                R<List<UserListVo>> createUserListR = this.userBll.listByIds(createUserIds);
                List<UserListVo> createUserList = createUserListR.getData();

                for (AgentListVo agentListVo : agentList) {
                    // 封装运营人员信息
                    if(userList != null && userList.size() > 0) {
                        for (UserListVo userListVo : userList) {
                            if(userListVo.getId().equals(agentListVo.getOperationUserId())) {
                                agentListVo.setOperationUserName(userListVo.getNickName());
                                break;
                            }
                        }
                    }
                    // 封装行业信息
                    if(tradeList != null && tradeList.size() > 0) {
                        for (TradeListVo tradeListVo : tradeList) {
                            if(tradeListVo.getId().equals(agentListVo.getTradeId())) {
                                agentListVo.setTradeName(tradeListVo.getName());
                                break;
                            }
                        }
                    }
                    // 封装创建人信息
                    if(createUserList != null && createUserList.size() > 0) {
                        for (UserListVo userListVo : createUserList) {
                            if(userListVo.getId().equals(agentListVo.getCreateUserId())) {
                                agentListVo.setCreateUserName(userListVo.getNickName());
                                break;
                            }
                        }
                    }
                }
            }
        }

        return pageUtilsR;
    }

    @Override
    public R<AgentInfoVo> currentUserInfo() {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        UserInfoVo user = ResultUtil.getResult(userBll.info(localUser.getId(), false));
        if (user == null || user.getUserType() != UserEnums.userType.MANAGE_ADMIN_USER.getCode() || user.getAdminUserType() != UserEnums.adminUserType.AGENT.getCode()) {
            throw new BusinessException("用户类型不符，不能操作");
        }
        AgentInfoVo agent = agentBll.infoByPhoneType(user.getPhone(), 0);

        if (agent == null) {
            return R.error(3001, "代理商不存在");
        }

        if (!ObjectUtil.equals(agent.getId(), localUser.getAgentId())) {
            log.error("[代理商主页] 代理商id和登录信息中的代理商id不匹配，不能操作, agentId={}, localUserAgentId={}", agent.getId(), localUser.getAgentId());
            throw new BusinessException("用户类型不符，不能操作");
        }
        return info(agent.getId());
    }

    @Override
    public R<AgentInfoVo> info(Long id) {

        R<AgentInfoVo> agentInfoVoR = agentBll.info(id);

        AgentInfoVo agentInfoVo = agentInfoVoR.getData();
        if(agentInfoVo != null) {
            // 封装海报图片
            if(!StringUtils.isEmpty(agentInfoVo.getPosterImgIds())) {
                String[] imgIds = agentInfoVo.getPosterImgIds().split("_");
                if(imgIds.length > 0) {
                    List<Long> imgIdList = Arrays.stream(imgIds).map(Long::valueOf).collect(Collectors.toList());
                    agentInfoVo.setPosterImgList(this.fileBll.listByFileIds(imgIdList));
                }
            }
            // 封装链接地址
            agentInfoVo.setUrl(agentProperties.getUrl() + agentInfoVo.getAgentUrlCode());
            // 封装行业
            R<TradeInfoVo> tradeInfoVoR = this.tradeBll.info(agentInfoVo.getTradeId());
            if(tradeInfoVoR.getData() != null) {
                TradeInfoVo tradeInfoVo = tradeInfoVoR.getData();
                agentInfoVo.setTradeName(tradeInfoVo.getName());
            }
            // 封装平台运营人员
            R<UserInfoVo> operationUserR = this.userBll.info(agentInfoVo.getOperationUserId(), false);
            if(operationUserR.getData() != null) {
                UserInfoVo userInfoVo = operationUserR.getData();
                agentInfoVo.setOperationUserName(userInfoVo.getNickName());
            }
            // 封装创建人
            R<UserInfoVo> createUserR = this.userBll.info(agentInfoVo.getCreateUserId(), false);
            if(createUserR.getData() != null) {
                UserInfoVo userInfoVo = createUserR.getData();
                agentInfoVo.setCreateUserName(userInfoVo.getNickName());
            }

        }

        return agentInfoVoR;
    }

    @Override
    public R<String> save(AgentBo agentBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        agentBo.setCreateUserId(user.getId());

        return agentBll.save(agentBo);
    }

    @Override
    public R<String> update(AgentBo agentBo) {

        return agentBll.update(agentBo);
    }

    @Override
    public R<String> delete(Long id) {

        return agentBll.delete(id);
    }

    @Override
    public R<String> updateAgentStatus(Long id, Integer agentStatus) {
        agentBll.updateAgentStatus(id, agentStatus);
        return R.ok();
    }
}

