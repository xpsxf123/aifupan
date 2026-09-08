package com.jiuyu.replay.api.logic.common.impl;



import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.agent.bll.ChannelBll;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.api.logic.common.OperationLogLogic;
import com.jiuyu.replay.common.bll.OperationLogBll;
import com.jiuyu.replay.common.diff.Business;
import com.jiuyu.replay.common.vo.OperationLogListVo;
import com.jiuyu.replay.common.vo.OperationLogInfoVo;
import com.jiuyu.replay.common.bo.OperationLogBo;
import com.jiuyu.replay.common.bo.OperationLogListBo;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.power.bll.SalesBll;
import com.jiuyu.replay.words.bll.TradeBll;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 操作日志表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Service
public class OperationLogLogicImpl implements OperationLogLogic {

    @Resource
    private OperationLogBll operationLogBll;
    @Resource
    private ChannelBll channelBll;
    @Resource
    private SalesBll salesBll;
    @Resource
    private TradeBll tradeBll;

    @Override
    public R<PageUtils<OperationLogListVo>> queryPage(OperationLogListBo operationLogListBo) {
        R<PageUtils<OperationLogListVo>> pageUtilsR = operationLogBll.queryPage(operationLogListBo);
        if (ObjectUtil.isNotEmpty(pageUtilsR)&&ObjectUtil.isNotEmpty(pageUtilsR.getData())
                &&ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())){
            List<OperationLogListVo> list = pageUtilsR.getData().getList();
            if (Business.USER_DETAILS.getDesc().equals(operationLogListBo.getBusinessType())){
                toHandleUserDetailsOptimized(list);
            }
        }
        return pageUtilsR;
    }


    /**
     * 优化版：处理[用户详情]相关的操作记录
     * 逻辑与toHandleUserDetails一致，但结构更清晰、复用性更高
     */
    private void toHandleUserDetailsOptimized(List<OperationLogListVo> list) {
        // 1. 收集所有ID
        Set<Long> channelIds = new HashSet<>();
        Set<Long> salesIds = new HashSet<>();
        Set<Long> tradeIds = new HashSet<>();

        for (OperationLogListVo x : list) {
            if (Business.USER_DETAILS.getDesc().equals(x.getBusinessType())) {
                extractIdsFromData(x.getBeforeData(), channelIds, salesIds, tradeIds);
                extractIdsFromData(x.getAfterData(), channelIds, salesIds, tradeIds);
            }
        }

        // 2. 查询所有名称
        Map<Long, String> channelMap = channelBll.selectByChannelIds(new ArrayList<>(channelIds))
                .stream().collect(Collectors.toMap(ChannelInfoVo::getId, ChannelInfoVo::getChannelName, (oldV, newV) -> newV));
        Map<Long, String> salesMap = salesBll.selectBySalesIds(new ArrayList<>(salesIds))
                .stream().collect(Collectors.toMap(SalesInfoVo::getId, SalesInfoVo::getSalesName, (oldV, newV) -> newV));
        Map<Long, String> tradeMap = tradeBll.selectByTradeIds(new ArrayList<>(tradeIds))
                .stream().collect(Collectors.toMap(TradeListVo::getId, TradeListVo::getName, (oldV, newV) -> newV));

        // 3. 填充名称并设置回对象
        for (OperationLogListVo x : list) {
            if (Business.USER_DETAILS.getDesc().equals(x.getBusinessType())) {
                // 处理beforeData
                Map<String, Object> beforeMap = fillNamesToDataAndReturn(x.getBeforeData(), channelMap, salesMap, tradeMap);
                if (beforeMap != null) {
                    x.setBeforeObjData(beforeMap);
                }
                
                // 处理afterData
                Map<String, Object> afterMap = fillNamesToDataAndReturn(x.getAfterData(), channelMap, salesMap, tradeMap);
                if (afterMap != null) {
                    x.setAfterObjData(afterMap);
                }
            }
        }
    }

    /**
     * 从json字符串中提取ID，加入对应集合
     */
    private void extractIdsFromData(String data, Set<Long> channelIds, Set<Long> salesIds, Set<Long> tradeIds) {
        if (StringUtil.isNotBlank(data)) {
            Map<String, Object> map = JSONUtil.toBean(data, Map.class);
            addIfNotNull(channelIds, map.get("channelId"));
            addIfNotNull(salesIds, map.get("saleId"));
            addIfNotNull(tradeIds, map.get("tradeId"));
        }
    }

    /**
     * 如果value不为空且不为空字符串，则将其加入指定集合
     * @param set
     * @param value
     */
    private void addIfNotNull(Set<Long> set, Object value) {
        if (value != null) {
            String str = value.toString();
            // 这里过滤掉空字符串和全空格
            if (StringUtil.isNotBlank(str)) {
                try {
                    set.add(Long.valueOf(str));
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 给json字符串中的ID补充名称，并返回处理后的Map
     * @return 处理后的Map，如果data为空则返回null
     */
    private Map<String, Object> fillNamesToDataAndReturn(String data, Map<Long, String> channelMap, Map<Long, String> salesMap, Map<Long, String> tradeMap) {
        if (StringUtil.isNotBlank(data)) {
            Map<String, Object> map = JSONUtil.toBean(data, Map.class);
            putNameIfPresent(map, "channelId", "channelName", channelMap);
            putNameIfPresent(map, "saleId", "salesName", salesMap);
            putNameIfPresent(map, "tradeId", "tradeName", tradeMap);
            return map;
        }
        return null;
    }

    /**
     * 如果ID对应的名称存在，则将名称放入json字符串中
     * @param map
     * @param idKey
     * @param nameKey
     * @param nameMap
     */
    private void putNameIfPresent(Map<String, Object> map, String idKey, String nameKey, Map<Long, String> nameMap) {
        Object idObj = map.get(idKey);
        if (idObj != null && StringUtil.isNotBlank(idObj.toString())) {
            String name = nameMap.get(Long.valueOf(idObj.toString()));
            if (StringUtil.isNotBlank(name)) {
                map.put(nameKey, name);
            }
        }
    }

    @Override
    public R<OperationLogInfoVo> info(Long id) {

        return operationLogBll.info(id);
    }

    @Override
    public R<String> save(OperationLogBo operationLogBo) {

        return operationLogBll.save(operationLogBo);
    }

    @Override
    public R<String> update(OperationLogBo operationLogBo) {

        return operationLogBll.update(operationLogBo);
    }

    @Override
    public R<String> delete(Long id) {

        return operationLogBll.delete(id);
    }


}

