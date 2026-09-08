package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.third.vo.ProxyIpRecordListVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import com.jiuyu.replay.third.bo.ProxyIpRecordBo;
import com.jiuyu.replay.third.bo.ProxyIpRecordListBo;


/**
 * 代理ip提取记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
public interface ProxyIpRecordLogic {


    /**
     * 代理ip提取记录列表
     * @param proxyIpRecordListBo 代理ip提取记录列表查询参数
     * @return
     */
    R<PageUtils<ProxyIpRecordListVo>> queryPage(ProxyIpRecordListBo proxyIpRecordListBo);

    /**
    * 代理ip提取记录信息
    * @param id 代理ip提取记录id
    * @return
    */
    R<ProxyIpRecordInfoVo> info(Long id);

    /**
     * 新增代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
    R<String> save(ProxyIpRecordBo proxyIpRecordBo);

    /**
     * 修改代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
    R<String> update(ProxyIpRecordBo proxyIpRecordBo);

    /**
     * 删除代理ip提取记录
     * @param id 代理ip提取记录id
     * @return
     */
    R<String> delete(Long id);


}

