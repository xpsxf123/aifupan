package com.jiuyu.replay.third.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bo.ProxyIpRecordBo;
import com.jiuyu.replay.third.bo.ProxyIpRecordListBo;
import com.jiuyu.replay.third.producer.ProxyIpRecordProducer;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 代理ip提取记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Component
public class ProxyIpRecordBll {

    @Resource
    private ProxyIpRecordProducer proxyIpRecordProducer;


    /**
     * 代理ip提取记录列表
     * @param proxyIpRecordListBo 代理ip提取记录列表查询参数
     * @return
     */
    public R<PageUtils<ProxyIpRecordListVo>> queryPage(ProxyIpRecordListBo proxyIpRecordListBo) {

        return R.ok("获取成功", proxyIpRecordProducer.queryPage(proxyIpRecordListBo));
    }

    /**
    * 代理ip提取记录信息
    * @param id 代理ip提取记录id
    * @return
    */
    public R<ProxyIpRecordInfoVo> info(Long id) {

        ProxyIpRecordInfoVo proxyIpRecordInfoVo = proxyIpRecordProducer.info(id);
        return R.ok("获取成功", proxyIpRecordInfoVo);
    }

    /**
     * 新增代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
    public R<String> save(ProxyIpRecordBo proxyIpRecordBo) {

        ProxyIpRecordInfoVo proxyIpRecordInfoVo = proxyIpRecordProducer.save(proxyIpRecordBo);
        return R.ok("添加成功");
    }

    /**
     * 修改代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
    public R<String> update(ProxyIpRecordBo proxyIpRecordBo) {

        proxyIpRecordProducer.update(proxyIpRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除代理ip提取记录
     * @param id 代理ip提取记录id
     * @return
     */
    public R<String> delete(Long id) {

        proxyIpRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

