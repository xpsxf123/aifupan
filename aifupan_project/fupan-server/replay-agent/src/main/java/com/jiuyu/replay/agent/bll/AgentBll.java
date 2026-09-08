package com.jiuyu.replay.agent.bll;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.agent.bo.AgentBo;
import com.jiuyu.replay.agent.bo.AgentListBo;
import com.jiuyu.replay.agent.constant.Constant;
import com.jiuyu.replay.agent.producer.AgentProducer;
import com.jiuyu.replay.agent.producer.ChannelProducer;
import com.jiuyu.replay.agent.producer.InviteUrlCodeProducer;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentListVo;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.power.AgentUserAddBo;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.SalesFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UpdateEmployeeStatusVo;
import com.jiuyu.replay.generic.vo.power.UserPass;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 代理商
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Component
public class AgentBll {

    @Resource
    private AgentProducer agentProducer;
    @Resource
    private InviteUrlCodeProducer inviteUrlCodeProducer;
    @Resource
    private ChannelProducer channelProducer;
    @Resource
    private UserFeign userFeign;
    @Resource
    private SystemKvService systemKvService;
    @Resource
    private SalesFeign salesFeign;


    /**
     * 代理商列表
     * @param agentListBo 代理商列表查询参数
     * @return
     */
    public R<PageUtils<AgentListVo>> queryPage(AgentListBo agentListBo) {

        if(agentListBo.getChannelId() != null) {
            List<Long> channelIds = this.channelProducer.getChannelAllChildId(agentListBo.getChannelId());
            agentListBo.setChannelIds(channelIds);
        }

        return R.ok("获取成功", agentProducer.queryPage(agentListBo));
    }

    /**
    * 代理商信息
    * @param id 代理商id
    * @return
    */
    public R<AgentInfoVo> info(Long id) {

        AgentInfoVo agentInfoVo = agentProducer.info(id, null);

        return R.ok("获取成功", agentInfoVo);
    }

    /**
     * 新增代理商
     * @param agentBo 代理商对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(AgentBo agentBo) {

        AgentInfoVo infoByChannelId = this.agentProducer.infoByChannelId(agentBo.getChannelId());
        if(infoByChannelId != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前渠道已关联代理商");
        }

        if(agentBo.getCommissionRate() < 0 || agentBo.getCommissionRate() > 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "佣金只能设置0-100%范围内");
        }
        if(agentBo.getRenewalCommissionRate() < 0 || agentBo.getRenewalCommissionRate() > 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "佣金只能设置0-100%范围内");
        }

        // 如果是普通代理商(agentType=0)，需要创建后台用户
        if (agentBo.getAgentType() != null && agentBo.getAgentType() == 0) {
            long count = agentProducer.countAgentByPhoneType(agentBo.getContactPhone(), 0, null);
            if (count > 0) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "代理商中已存在该手机号");
            }

            // 校验手机号是否已存在（查询userType为1的用户）
            UserDto existingUser = userFeign.getByPhoneAndType(agentBo.getContactPhone(), 1);
            if (existingUser != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该手机号已存在后台用户，无法创建");
            }
        }

        Long agentId = SnowflakeManager.nextValue();
        agentBo.setId(agentId);

        // 设置邀请链接code
        InviteUrlCodeBo inviteUrlCodeBo = new InviteUrlCodeBo();
        inviteUrlCodeBo.setAgentId(agentId);
        inviteUrlCodeBo.setCodeType(0);
        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeProducer.save(inviteUrlCodeBo);
        agentBo.setAgentUrlCode(inviteUrlCodeInfoVo.getUrlCode());

        // 获取渠道的名称
        ChannelInfoVo channelInfoVo = channelProducer.info(agentBo.getChannelId());
        if (channelInfoVo != null) {
            agentBo.setAgentName(channelInfoVo.getChannelName());
        }



        // 如果是普通代理商，创建对应的后台用户
        UserPass pass = null;
        if (agentBo.getAgentType() != null && agentBo.getAgentType() == 0) {
            pass = createBackendUserForAgent(agentBo);
            agentBo.setUserId(pass.getId());
        }

        // 保存代理商
        AgentInfoVo agentInfoVo = agentProducer.save(agentBo);

        return R.ok("添加成功", pass != null ? pass.getPass() : null);
    }

    /**
     * 修改代理商
     * @param agentBo 代理商对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(AgentBo agentBo) {
        if (agentBo.getCommissionRate() < 0 || agentBo.getCommissionRate() > 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "佣金只能设置0-100%范围内");
        }
        if (agentBo.getRenewalCommissionRate() < 0 || agentBo.getRenewalCommissionRate() > 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "佣金只能设置0-100%范围内");
        }
        AgentInfoVo agentInfoVo = this.agentProducer.info(agentBo.getId(), null);
        if(agentInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "代理商信息不存在");
        }

        if(!agentInfoVo.getChannelId().equals(agentBo.getChannelId())) {
            AgentInfoVo infoByChannelId = this.agentProducer.infoByChannelId(agentBo.getChannelId());
            if(infoByChannelId != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前渠道已关联代理商");
            }
        }

        if (!ObjectUtil.equals(agentBo.getAgentType(), agentInfoVo.getAgentType())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "不能修改代理商类型");
        }
        UserPass pass = null;
        // 如果是普通代理商(agentType=0)，需要处理后台用户
        if (agentBo.getAgentType() != null && agentBo.getAgentType() == 0) {
            long count = agentProducer.countAgentByPhoneType(agentBo.getContactPhone(), 0, agentInfoVo.getId());
            if (count > 0) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "代理商中已存在该手机号");
            }

            pass = updateBackendUserForAgent(agentBo, agentInfoVo);
            agentBo.setUserId(pass.getId());
        }

        // 获取渠道的名称
        ChannelInfoVo channelInfoVo = channelProducer.info(agentBo.getChannelId());
        if (channelInfoVo != null) {
            agentBo.setAgentName(channelInfoVo.getChannelName());
        }

        agentProducer.update(agentBo);
        return R.ok("修改成功", pass != null ? pass.getPass() : null);
    }

    /**
     * 删除代理商
     * @param id 代理商id
     * @return
     */
    public R<String> delete(Long id) {

        agentProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 根据邀请链接code获取代理商信息
     * @param inviteUrlCode 邀请链接code
     * @return
     */
    public R<AgentInfoVo> infoByUrlCode(String inviteUrlCode) {

        AgentInfoVo agentInfoVo = agentProducer.infoByUrlCode(inviteUrlCode);

        return R.ok("获取成功", agentInfoVo);
    }

    /**
     *
     * @param channelId
     * @return
     */
    public R<AgentInfoVo> infoByChannelId(Long channelId) {

        AgentInfoVo agentInfoVo = agentProducer.infoByChannelId(channelId);

        return R.ok("获取成功", agentInfoVo);
    }

    /**
     * 为普通代理商创建后台用户
     *
     * @param agentBo 代理商信息
     * @return 密码
     */
    private UserPass createBackendUserForAgent(AgentBo agentBo) {
        // 生成6位随机密码
        AgentUserAddBo userAddBo = new AgentUserAddBo();
        userAddBo.setUsername(String.valueOf(SnowflakeManager.nextValue()));
        userAddBo.setNickName(agentBo.getContactName());
        userAddBo.setPhone(agentBo.getContactPhone());
        userAddBo.setAdminUserType(UserEnums.adminUserType.AGENT.getCode());
        userAddBo.setRoleIdList(List.of(getAgentUserRoleId()));
        userAddBo.setTradeId(agentBo.getTradeId());
        userAddBo.setChannelId(agentBo.getChannelId());
        userAddBo.setAgentId(agentBo.getId());
        return userFeign.saveOrUpdateAgentUser(userAddBo);
    }

    private Long getAgentUserRoleId() {
        SystemKvEntity kv = systemKvService.getByKey("agent_user_role_id");
        if (ObjectUtil.isEmpty(kv)) {
            throw new BusinessException(StatusCode.SYSTEM_BUSY.getCode(), "没有配置代理商角色，请联系管理员");
        }
        Long agentRoleId = NumberUtil.parseLong(kv.getKvValue(), null);
        if (ObjectUtil.isEmpty(agentRoleId)) {
            throw new BusinessException(StatusCode.SYSTEM_BUSY.getCode(), "代理商角色配置错误，请联系管理员");
        }
        return agentRoleId;
    }

    /**
     * 为普通代理商更新后台用户
     *
     * @param agentBo      代理商信息
     * @param oldAgentInfo 原代理商信息
     */
    private UserPass updateBackendUserForAgent(AgentBo agentBo, AgentInfoVo oldAgentInfo) {
        // 根据手机号查询用户
        UserDto existingUser = userFeign.getByPhoneAndType(oldAgentInfo.getContactPhone(), 1);
        UserPass pass = null;
        if (existingUser != null) {
            AgentUserAddBo userAddBo = new AgentUserAddBo();
            userAddBo.setUserId(existingUser.getId());
            userAddBo.setNickName(agentBo.getContactName());
            userAddBo.setPhone(agentBo.getContactPhone());
            userAddBo.setAdminUserType(UserEnums.adminUserType.AGENT.getCode());
            userAddBo.setTradeId(agentBo.getTradeId());
            userAddBo.setChannelId(agentBo.getChannelId());
            userAddBo.setAgentId(oldAgentInfo.getId());
            pass = userFeign.saveOrUpdateAgentUser(userAddBo);
        } else {
            // 如果用户不存在，创建新用户
            agentBo.setId(oldAgentInfo.getId());
            pass = createBackendUserForAgent(agentBo);
        }
        return pass;
    }

    public AgentInfoVo infoByPhoneType(String phone, Integer type) {
        AgentInfoVo agentInfoVo = agentProducer.infoByPhoneType(phone, type);
        if (agentInfoVo != null) {
            return agentInfoVo;
        }
        return null;
    }

    /**
     * 修改代理商状态
     *
     * @param id          代理商id
     * @param agentStatus 代理商状态 0：未启用 1：启用中
     */
    public void updateAgentStatus(Long id, Integer agentStatus) {
        AgentInfoVo info = agentProducer.info(id, null);
        if (info == null) {
            throw new BusinessException(StatusCode.SYSTEM_BUSY.getCode(), "代理商不存在");
        }

        if (agentStatus == 1 && info.getEmployeeStatus() == 0) {
            throw new BusinessException(StatusCode.SYSTEM_BUSY.getCode(), "请先修改代理商的状态为在职");
        }
        AgentBo agentBo = new AgentBo();
        agentBo.setId(id);
        agentBo.setAgentStatus(agentStatus);
        agentProducer.update(agentBo);

        // 修改用户状态
        int status = agentStatus == 0 ? 1 : 0;
        userFeign.updateAdminUserStatusByPhone(info.getContactPhone(), status);
    }

    /**
     * 批量查询代理商信息
     *
     * @param agentIds 代理商ID列表
     * @return 代理商信息列表
     */
    public List<AgentInfoVo> listByIds(List<Long> agentIds) {
        return agentProducer.listByIds(agentIds);
    }

    /**
     * 根据用户id修改代理商姓名和手机号
     *
     * @param userId   用户id
     * @param nickName 销售姓名
     * @param phone    手机号
     */
    public void updateNameAndPhoneByUserId(Long userId, String nickName, String phone) {
        agentProducer.updateNameAndPhoneByUserId(userId, nickName, phone);
    }

    /**
     * 修改员工状态
     *
     * @param id             id
     * @param employeeStatus 员工状态
     */
    public void updateEmployeeStatus(Long id, Integer employeeStatus) {
        AgentInfoVo agent = agentProducer.info(id, null);
        if (agent == null) {
            return;
        }
        if (agent.getAgentType() == 0) {
            // 如果是普通代理商，就把对应的销售全部离职
            salesFeign.updateEmployeeStatusByAgentId(id, employeeStatus);
        }

        agentProducer.updateEmployeeStatus(id, employeeStatus);
    }

    /**
     * 获取修改员工状态的参数
     *
     * @param agentId 代理商id
     * @return 参数
     */
    public UpdateEmployeeStatusVo getUpdateEmployeeParams(Long agentId) {
        UpdateEmployeeStatusVo res = new UpdateEmployeeStatusVo();

        if (agentId == null) {
            return res;
        }
        AgentInfoVo info = agentProducer.info(agentId, null);
        if (info == null) {
            return res;
        }
        res.setAgentId(agentId);
        res.setUserId(info.getUserId());
        return res;
    }
}

