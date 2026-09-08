package com.jiuyu.replay.third.bll;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.utils.HttpUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.ali.DingDingUtils;
import com.jiuyu.replay.third.bo.ProxyIpBo;
import com.jiuyu.replay.third.bo.ProxyIpListBo;
import com.jiuyu.replay.third.constant.Constant;
import com.jiuyu.replay.third.constant.ShenlongProperties;
import com.jiuyu.replay.third.producer.ProxyIpProducer;
import com.jiuyu.replay.third.producer.ProxyIpRecordProducer;
import com.jiuyu.replay.third.vo.ProxyIpInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpListVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;


/**
 * 代理ip提取
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Component
@Slf4j
public class ProxyIpBll {

    @Resource
    private ProxyIpProducer proxyIpProducer;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ShenlongProperties shenlongProperties;
    @Resource
    private ProxyIpRecordProducer proxyIpRecordProducer;
    @Resource
    private HttpUtils httpUtils;
    @Resource
    private DingDingUtils dingDingUtils;


    /**
     * 代理ip提取列表
     * @param proxyIpListBo 代理ip提取列表查询参数
     * @return
     */
    public R<PageUtils<ProxyIpListVo>> queryPage(ProxyIpListBo proxyIpListBo) {

        return R.ok("获取成功", proxyIpProducer.queryPage(proxyIpListBo));
    }

    /**
    * 代理ip提取信息
    * @param id 代理ip提取id
    * @return
    */
    public R<ProxyIpInfoVo> info(Long id) {

        ProxyIpInfoVo proxyIpInfoVo = proxyIpProducer.info(id);
        return R.ok("获取成功", proxyIpInfoVo);
    }

    /**
     * 新增代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
    public R<String> save(ProxyIpBo proxyIpBo) {

        ProxyIpInfoVo proxyIpInfoVo = proxyIpProducer.save(proxyIpBo);
        return R.ok("添加成功");
    }

    /**
     * 修改代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
    public R<String> update(ProxyIpBo proxyIpBo) {

        proxyIpProducer.update(proxyIpBo);
        return R.ok("修改成功");
    }

    /**
     * 删除代理ip提取
     * @param id 代理ip提取id
     * @return
     */
    public R<String> delete(Long id) {

        proxyIpProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取代理ip
     * @param userId 用户id
     * @param tenantId 租户id
     * @param forceUpdate 是否强制更换新的IP
     * @param validityType 时效类型 0：短效 1：长效
     * @return
     */
    @CustomRedissonLock(key = "'get_proxy_ip_lock'")
    @Transactional(rollbackFor = Exception.class)
    public R<ProxyIpRecordInfoVo> getProxyIp(Long userId, Long tenantId, Boolean forceUpdate, Integer validityType) {

        if(forceUpdate) {
            redisTemplate.delete(shenlongProperties.getProxyIpUserRedisKeyPrefix() + validityType + userId);
        }

        Object obj = redisTemplate.opsForValue().get(shenlongProperties.getProxyIpUserRedisKeyPrefix() + validityType + userId);
        if(obj != null) {
            // 已存在，直接返回
            ProxyIpRecordInfoVo proxyIpRecordInfoVo = (ProxyIpRecordInfoVo) obj;
            return R.ok(proxyIpRecordInfoVo);
        }else {
            // 查找可用的ip链接
            ProxyIpInfoVo proxyIpInfoVo = this.proxyIpProducer.getUsableProxyIp(validityType);

            if(proxyIpInfoVo != null) {
                if (proxyIpInfoVo.getDayUseNum() >= 0){

                    if (proxyIpInfoVo.getRemainingNum() == 0) return R.ok();
                    // 获取当天的使用次数，判断是否可以在使用
                    Integer i = proxyIpRecordProducer.countDayUserNum(userId, tenantId, proxyIpInfoVo.getId());
                    if (i == null || i >= proxyIpInfoVo.getDayUseNum()) {
                        log.info("用户id：{}，{}代理ip已达上限", userId, DateUtil.today());
                        return R.ok();
                    }
                }

                // 通过链接获取IP
                String resultJsonStr = httpUtils.sendGet(proxyIpInfoVo.getExtractUrl(), null);
                log.info("通过链接获取IP=={}=={}", proxyIpInfoVo, resultJsonStr);
                //{"code":200,"data":[{"ip":"58.54.3.66","port":40017,"expire":"2025-03-08 16:32:10"}]}
                if(!StringUtils.isEmpty(resultJsonStr)) {
                    JSONObject jsonObject = JSON.parseObject(resultJsonStr);
                    if(jsonObject.getInteger("code") == 200 && !StringUtils.isEmpty(jsonObject.getString("data"))) {
                        JSONArray objArr = JSON.parseArray(jsonObject.getString("data"));
                        JSONObject dataJSONObject = objArr.getJSONObject(0);
                        String ip = dataJSONObject.getString("ip");
                        String port = dataJSONObject.getString("port");

                        // 修改剩余数量
                        this.proxyIpProducer.updateRemainingNum(proxyIpInfoVo.getId());

                        // 钉钉预警
                        dingDingWarn(proxyIpInfoVo);

                        // 保存用户的提取记录
                        if(!StringUtils.isEmpty(ip) && !StringUtils.isEmpty(port)) {

                            ProxyIpRecordInfoVo proxyIpRecordInfoVo = this.proxyIpRecordProducer.saveRecord(userId, tenantId, ip, port, proxyIpInfoVo.getIpEffectiveTime(), proxyIpInfoVo.getId());

                            proxyIpRecordInfoVo.setProxyUsername(proxyIpInfoVo.getProxyUsername());
                            proxyIpRecordInfoVo.setProxyPassword(proxyIpInfoVo.getProxyPassword());

                            // 存到redis，为了避免过期用不了，redis过期时间比真实有效期少一分钟
                            redisTemplate.opsForValue().set(
                                    shenlongProperties.getProxyIpUserRedisKeyPrefix() + validityType + userId,
                                    proxyIpRecordInfoVo,
                                    Duration.ofMinutes(proxyIpRecordInfoVo.getIpEffectiveTime() - 1));

                            return R.ok(proxyIpRecordInfoVo);
                        }
                    }


                }
            }
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有可用的代理IP");
    }

    /**
     * 检测是否需要发送钉钉预警信息
     * @param proxyIpInfoVo 代理IP信息
     */
    private void dingDingWarn(ProxyIpInfoVo proxyIpInfoVo) {
        try {
            if(proxyIpInfoVo.getValidityType() == 0) {
                // 短效
                if(proxyIpInfoVo.getRemainingNum() == 10000 || proxyIpInfoVo.getRemainingNum() == 5000 || proxyIpInfoVo.getRemainingNum() == 2000) {
                    dingDingUtils.sendTextMessage("短效代理IP数量预警，剩余：" + proxyIpInfoVo.getRemainingNum());
                }
            }else if(proxyIpInfoVo.getValidityType() == 1) {
                // 长效
                if(proxyIpInfoVo.getRemainingNum() == 5000 || proxyIpInfoVo.getRemainingNum() == 3000 || proxyIpInfoVo.getRemainingNum() == 1000) {
                    dingDingUtils.sendTextMessage("长效代理IP数量预警，剩余：" + proxyIpInfoVo.getRemainingNum());
                }
            }
        }catch (Exception e) {
            log.error("发送钉钉预警失败==dingDingWarn()", e);
        }

    }
}

