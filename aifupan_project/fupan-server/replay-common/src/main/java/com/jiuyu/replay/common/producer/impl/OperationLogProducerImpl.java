package com.jiuyu.replay.common.producer.impl;


import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.diff.Business;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.common.vo.OperationLogListVo;
import com.jiuyu.replay.common.vo.OperationLogInfoVo;
import com.jiuyu.replay.common.bo.OperationLogBo;
import com.jiuyu.replay.common.bo.OperationLogListBo;
import com.jiuyu.replay.common.repository.service.OperationLogService;
import com.jiuyu.replay.common.entity.OperationLogEntity;
import com.jiuyu.replay.common.producer.OperationLogProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 操作日志表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Service
public class OperationLogProducerImpl implements OperationLogProducer {

    @Resource
    private OperationLogService operationLogService;



    @Override
    public PageUtils<OperationLogListVo> queryPage(OperationLogListBo operationLogListBo) {
        QueryWrapper<OperationLogEntity> wrapper = new QueryWrapper<>();
        if(StringUtil.isNotEmpty(operationLogListBo.getKeyword())){
            wrapper.like("name", operationLogListBo.getKeyword());
        }
        wrapper.eq("business_user_id", operationLogListBo.getUserId());
        wrapper.orderBy(true,false,"operation_time");

        IPage<OperationLogEntity> iPage = operationLogService
                .page(new Query<OperationLogEntity>().getPage(operationLogListBo.getPage(), operationLogListBo.getLimit()), wrapper);

        PageUtils<OperationLogListVo> pageUtils = new PageUtils<>(operationLogListBo.getPage(), operationLogListBo.getLimit(), iPage);

        List<OperationLogEntity> records = iPage.getRecords();
        if(records != null && !records.isEmpty()) {
            List<OperationLogListVo> vos = records.stream().map(item -> {
                OperationLogListVo operationLogVo = new OperationLogListVo();
                BeanUtils.copyProperties(item, operationLogVo);
                return operationLogVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public OperationLogInfoVo info(Long id) {

        OperationLogEntity operationLogEntity = operationLogService.getById(id);
        if(operationLogEntity != null) {
            OperationLogInfoVo operationLogInfoVo = new OperationLogInfoVo();
            BeanUtils.copyProperties(operationLogEntity, operationLogInfoVo);
            return operationLogInfoVo;
        }

        return null;
    }

    @Override
    public OperationLogInfoVo save(OperationLogBo operationLogBo) {

         OperationLogEntity operationLogEntity = new OperationLogEntity();
         BeanUtils.copyProperties(operationLogBo, operationLogEntity);
         operationLogEntity.setId(SnowflakeManager.nextValue());
         operationLogEntity.setCreateDate(new Date());
         operationLogEntity.setUpdateDate(new Date());

         operationLogService.save(operationLogEntity);

         OperationLogInfoVo operationLogInfoVo = new OperationLogInfoVo();
         BeanUtils.copyProperties(operationLogEntity, operationLogInfoVo);

         return operationLogInfoVo;
     }

    @Override
    public void update(OperationLogBo operationLogBo) {

        OperationLogEntity operationLogEntity = new OperationLogEntity();
        BeanUtils.copyProperties(operationLogBo, operationLogEntity);
        operationLogEntity.setUpdateDate(new Date());

        operationLogService.updateById(operationLogEntity);
    }

    @Override
    public void deleteById(Long id) {

        operationLogService.removeById(id);
    }



    /**
     * 操作记录保存
     *
     * @param businessId
     * @param userId
     * @param allBeforeMap
     * @param allAfterMap
     * @param
     */
    @Override
    public void saveOptLog(String businessName, Long businessId, Long userId, Map<String
            , Object> allBeforeMap, Map<String, Object> allAfterMap,Long optUserId, String ip,String optName) {
        if (allBeforeMap.isEmpty()&&allAfterMap.isEmpty()){
            return;
        }
        OperationLogEntity operationLogEntity = new OperationLogEntity();
        operationLogEntity.setId(SnowflakeManager.nextValue());
        operationLogEntity.setBusinessId(businessId);
        operationLogEntity.setBusinessType(businessName);
        operationLogEntity.setBeforeData(JSON.toJSONString(allBeforeMap));
        operationLogEntity.setAfterData(JSON.toJSONString(allAfterMap));
        operationLogEntity.setOperatorId(optUserId);
        operationLogEntity.setBusinessUserId(userId);
        operationLogEntity.setOperationIp(ip);
        operationLogEntity.setOperationType("UPDATE");
        operationLogEntity.setOperatorName(optName);
        operationLogEntity.setOperationTime(new Date());
        operationLogService.save(operationLogEntity);
    }


}

