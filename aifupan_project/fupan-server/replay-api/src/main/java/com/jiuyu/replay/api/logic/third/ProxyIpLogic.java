package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.third.vo.ProxyIpListVo;
import com.jiuyu.replay.third.vo.ProxyIpInfoVo;
import com.jiuyu.replay.third.bo.ProxyIpBo;
import com.jiuyu.replay.third.bo.ProxyIpListBo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;


/**
 * 代理ip提取
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
public interface ProxyIpLogic {


    /**
     * 代理ip提取列表
     * @param proxyIpListBo 代理ip提取列表查询参数
     * @return
     */
    R<PageUtils<ProxyIpListVo>> queryPage(ProxyIpListBo proxyIpListBo);

    /**
    * 代理ip提取信息
    * @param id 代理ip提取id
    * @return
    */
    R<ProxyIpInfoVo> info(Long id);

    /**
     * 新增代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
    R<String> save(ProxyIpBo proxyIpBo);

    /**
     * 修改代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
    R<String> update(ProxyIpBo proxyIpBo);

    /**
     * 删除代理ip提取
     * @param id 代理ip提取id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取代理ip
     * @param forceUpdate 是否强制更换新的IP
     * @param validityType 时效类型 0：短效 1：长效
     * @return
     */
    R<ProxyIpRecordInfoVo> getProxyIp(Boolean forceUpdate, Integer validityType);
}

