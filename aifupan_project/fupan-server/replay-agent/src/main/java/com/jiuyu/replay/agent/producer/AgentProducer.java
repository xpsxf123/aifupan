package com.jiuyu.replay.agent.producer;

import com.jiuyu.replay.agent.bo.AgentBo;
import com.jiuyu.replay.agent.bo.AgentListBo;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentListVo;
import com.jiuyu.replay.generic.utils.PageUtils;

import java.util.List;


/**
 * 代理商
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
public interface AgentProducer {


    /**
     * 代理商列表
     * @param agentListBo 代理商列表查询参数
     * @return
     */
    PageUtils<AgentListVo> queryPage(AgentListBo agentListBo);

    /**
    * 代理商信息
    * @param id 代理商id
    * @param agentStatus 代理商状态 0：未启用 1：启用中
    * @return
    */
    AgentInfoVo info(Long id, Integer agentStatus);

    /**
     * 新增代理商
     * @param agentBo 代理商对象
     * @return
     */
     AgentInfoVo save(AgentBo agentBo);

    /**
     * 修改代理商
     * @param agentBo 代理商对象
     * @return
     */
    void update(AgentBo agentBo);

    /**
     * 删除代理商
     * @param id 代理商id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据渠道id获取代理商
     * @param channelId 渠道id
     * @return
     */
    AgentInfoVo infoByChannelId(Long channelId);

    /**
     * 根据邀请链接code获取代理商信息
     * @param inviteUrlCode 邀请链接code
     * @return
     */
    AgentInfoVo infoByUrlCode(String inviteUrlCode);

    /**
     * 获取客户端用户邀请链接的默认代理商
     * @return
     */
    AgentInfoVo getClientDefaultAgent();

    /**
     * 根据渠道id获取代理商信息
     *
     * @param channelId 渠道id
     * @return
     */
    AgentInfoVo getByChannelId(String channelId);

    /**
     * 根据手机号和类型统计代理商数量
     *
     * @param contactPhone 手机号
     * @param type         类型
     * @param noAgentId    不查的代理商id
     * @return 数量
     */
    long countAgentByPhoneType(String contactPhone, Integer type, Long noAgentId);

    /**
     * 根据手机号和类型获取代理商信息
     *
     * @param phone 手机号
     * @param type  类型
     * @return 代理商信息
     */
    AgentInfoVo infoByPhoneType(String phone, Integer type);

    /**
     * 批量查询代理商信息
     *
     * @param agentIds 代理商ID集合
     * @return 代理商信息列表
     */
    List<AgentInfoVo> listByIds(List<Long> agentIds);

    /**
     * 根据渠道id修改代理商名称
     *
     * @param channelId   渠道id
     * @param channelName 渠道名称
     */
    void updateAgentNameByChannelId(Long channelId, String channelName);

    /**
     * 根据用户id修改代理商姓名和手机号
     *
     * @param userId   用户id
     * @param nickName 销售姓名
     * @param phone    手机号
     */
    void updateNameAndPhoneByUserId(Long userId, String nickName, String phone);

    /**
     * 根据销售用户id获取代理商信息
     *
     * @param userId 销售用户id
     * @return 代理商信息
     */
    AgentInfoVo getBySalesUserId(Long userId);

    /**
     * 修改员工状态
     *
     * @param Id             id
     * @param employeeStatus 状态
     */
    void updateEmployeeStatus(Long Id, Integer employeeStatus);
}

