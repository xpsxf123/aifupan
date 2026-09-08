package com.jiuyu.replay.third.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.third.bo.ProxyIpRecordBo;
import com.jiuyu.replay.third.bo.ProxyIpRecordListBo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordListVo;


/**
 * 代理ip提取记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
public interface ProxyIpRecordProducer {


    /**
     * 代理ip提取记录列表
     * @param proxyIpRecordListBo 代理ip提取记录列表查询参数
     * @return
     */
    PageUtils<ProxyIpRecordListVo> queryPage(ProxyIpRecordListBo proxyIpRecordListBo);

    /**
    * 代理ip提取记录信息
    * @param id 代理ip提取记录id
    * @return
    */
    ProxyIpRecordInfoVo info(Long id);

    /**
     * 新增代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
     ProxyIpRecordInfoVo save(ProxyIpRecordBo proxyIpRecordBo);

    /**
     * 修改代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
    void update(ProxyIpRecordBo proxyIpRecordBo);

    /**
     * 删除代理ip提取记录
     * @param id 代理ip提取记录id
     * @return
     */
    void deleteById(Long id);


    /**
     * 保存用户的提取记录
     *
     * @param userId          用户id
     * @param tenantId        租户id
     * @param ip              代理IP
     * @param port            代理端口
     * @param ipEffectiveTime 有效时长
     * @param proxyId
     */
    ProxyIpRecordInfoVo saveRecord(Long userId, Long tenantId, String ip, String port, Integer ipEffectiveTime, Long proxyId);

    /**
     * 查询用户当天的提取次数
     *
     * @param userId
     * @param tenantId
     * @param proxyId
     * @return
     */
    Integer countDayUserNum(Long userId, Long tenantId, Long proxyId);
}

