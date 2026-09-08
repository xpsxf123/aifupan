package com.jiuyu.replay.third.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.third.vo.ProxyIpListVo;
import com.jiuyu.replay.third.vo.ProxyIpInfoVo;
import com.jiuyu.replay.third.bo.ProxyIpBo;
import com.jiuyu.replay.third.bo.ProxyIpListBo;


/**
 * 代理ip提取
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
public interface ProxyIpProducer {


    /**
     * 代理ip提取列表
     * @param proxyIpListBo 代理ip提取列表查询参数
     * @return
     */
    PageUtils<ProxyIpListVo> queryPage(ProxyIpListBo proxyIpListBo);

    /**
    * 代理ip提取信息
    * @param id 代理ip提取id
    * @return
    */
    ProxyIpInfoVo info(Long id);

    /**
     * 新增代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
     ProxyIpInfoVo save(ProxyIpBo proxyIpBo);

    /**
     * 修改代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
    void update(ProxyIpBo proxyIpBo);

    /**
     * 删除代理ip提取
     * @param id 代理ip提取id
     * @return
     */
    void deleteById(Long id);


    /**
     * 查找可用的ip链接
     * @param validityType 时效类型 0：短效 1：长效
     * @return
     */
    ProxyIpInfoVo getUsableProxyIp(Integer validityType);

    /**
     * 减剩余次数
     * @param id
     */
    void updateRemainingNum(Long id);
}

