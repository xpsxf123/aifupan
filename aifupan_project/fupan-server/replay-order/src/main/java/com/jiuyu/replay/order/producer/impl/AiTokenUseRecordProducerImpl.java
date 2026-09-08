package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordListVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.order.bo.AiTokenUseRecordListBo;
import com.jiuyu.replay.order.repository.service.AiTokenUseRecordService;
import com.jiuyu.replay.order.entity.AiTokenUseRecordEntity;
import com.jiuyu.replay.order.producer.AiTokenUseRecordProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * ai的token使用记录
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:00:33
 */
@Service
public class AiTokenUseRecordProducerImpl implements AiTokenUseRecordProducer {

    @Resource
    private AiTokenUseRecordService aiTokenUseRecordService;


    @Override
    public PageUtils<AiTokenUseRecordListVo> queryPage(AiTokenUseRecordListBo aiTokenUseRecordListBo) {
        QueryWrapper<AiTokenUseRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(aiTokenUseRecordListBo.getKeyword())){
            wrapper.like("name", aiTokenUseRecordListBo.getKeyword());
        }

        IPage<AiTokenUseRecordEntity> iPage = aiTokenUseRecordService.page(new Query<AiTokenUseRecordEntity>().getPage(aiTokenUseRecordListBo.getPage(), aiTokenUseRecordListBo.getLimit()), wrapper);

        PageUtils<AiTokenUseRecordListVo> pageUtils = new PageUtils<>(aiTokenUseRecordListBo.getPage(), aiTokenUseRecordListBo.getLimit(), iPage);

        List<AiTokenUseRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AiTokenUseRecordListVo> vos = records.stream().map(item -> {
                AiTokenUseRecordListVo aiTokenUseRecordVo = new AiTokenUseRecordListVo();
                BeanUtils.copyProperties(item, aiTokenUseRecordVo);
                return aiTokenUseRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AiTokenUseRecordInfoVo info(Long id) {

        AiTokenUseRecordEntity aiTokenUseRecordEntity = aiTokenUseRecordService.getById(id);
        if(aiTokenUseRecordEntity != null) {
            AiTokenUseRecordInfoVo aiTokenUseRecordInfoVo = new AiTokenUseRecordInfoVo();
            BeanUtils.copyProperties(aiTokenUseRecordEntity, aiTokenUseRecordInfoVo);
            return aiTokenUseRecordInfoVo;
        }

        return null;
    }

    /**
     * 新增ai的token使用记录
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
     public AiTokenUseRecordInfoVo save(AiTokenUseRecordBo aiTokenUseRecordBo) {

         Date now = new Date();
         AiTokenUseRecordEntity aiTokenUseRecordEntity = new AiTokenUseRecordEntity();
         BeanUtils.copyProperties(aiTokenUseRecordBo, aiTokenUseRecordEntity);
         aiTokenUseRecordEntity.setId(SnowflakeManager.nextValue());
         aiTokenUseRecordEntity.setCreateDate(now);
         aiTokenUseRecordEntity.setUpdateDate(now);

         aiTokenUseRecordService.save(aiTokenUseRecordEntity);

         AiTokenUseRecordInfoVo aiTokenUseRecordInfoVo = new AiTokenUseRecordInfoVo();
         BeanUtils.copyProperties(aiTokenUseRecordEntity, aiTokenUseRecordInfoVo);

         return aiTokenUseRecordInfoVo;
     }

    /**
     * 修改ai的token使用记录
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
    public void update(AiTokenUseRecordBo aiTokenUseRecordBo) {

        AiTokenUseRecordEntity aiTokenUseRecordEntity = new AiTokenUseRecordEntity();
        BeanUtils.copyProperties(aiTokenUseRecordBo, aiTokenUseRecordEntity);
        aiTokenUseRecordEntity.setUpdateDate(new Date());

        aiTokenUseRecordService.updateById(aiTokenUseRecordEntity);
    }

    /**
     * 删除ai的token使用记录
     * @param id ai的token使用记录id
     * @return
     */
    public void deleteById(Long id) {

        aiTokenUseRecordService.removeById(id);
    }

    @Override
    public AiTokenUseRecordInfoVo getByRequestId(String requestId) {
        if (ObjectUtil.isEmpty(requestId)) {
            return null;
        }
        AiTokenUseRecordEntity aiTokenUseRecordEntity = aiTokenUseRecordService.getOne(new LambdaQueryWrapper<AiTokenUseRecordEntity>()
                .eq(AiTokenUseRecordEntity::getRequestId, requestId)
                .last("limit 1")
        );

        if (ObjectUtil.isEmpty(aiTokenUseRecordEntity)) {
            return null;
        }

        return BeanUtil.copyProperties(aiTokenUseRecordEntity, AiTokenUseRecordInfoVo.class);
    }

    @Override
    public Long sumTotalTokensByTenantId(Long tenantId, Date sinceCreateDate) {
        if (ObjectUtil.isEmpty(tenantId)) {
            return 0L;
        }
        QueryWrapper<AiTokenUseRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.select("COALESCE(SUM(total_tokens), 0) AS total");
        wrapper.eq("tenant_id", tenantId);
        if (sinceCreateDate != null) {
            wrapper.ge("create_date", sinceCreateDate);
        }
        java.util.Map<String, Object> row = aiTokenUseRecordService.getMap(wrapper);
        if (row == null || row.get("total") == null) {
            return 0L;
        }
        Object total = row.get("total");
        if (total instanceof Number num) {
            return num.longValue();
        }
        return 0L;
    }

    @Override
    public Boolean existsByTenantIdAndAssistantType(Long tenantId, Integer assistantType) {
        if (ObjectUtil.isEmpty(tenantId) || ObjectUtil.isEmpty(assistantType)) {
            return Boolean.FALSE;
        }
        Long count = aiTokenUseRecordService.lambdaQuery()
                .eq(AiTokenUseRecordEntity::getTenantId, tenantId)
                .eq(AiTokenUseRecordEntity::getAssistantType, assistantType)
                .last("limit 1")
                .count();
        return count != null && count > 0;
    }
}

