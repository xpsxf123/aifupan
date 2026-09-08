package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.IndustryKnowledgeBo;
import com.jiuyu.replay.words.bo.IndustryKnowledgeListBo;
import com.jiuyu.replay.words.entity.IndustryKnowledgeEntity;
import com.jiuyu.replay.words.entity.TradeEntity;
import com.jiuyu.replay.words.producer.IndustryKnowledgeProducer;
import com.jiuyu.replay.words.repository.service.IndustryKnowledgeService;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.vo.IndustryKnowledgeListVo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统行业知识库
 *
 * @author jxy
 * @date 2024-06-24
 */
@Service
public class IndustryKnowledgeProducerImpl implements IndustryKnowledgeProducer {

    @Resource
    private IndustryKnowledgeService industryKnowledgeService;
    @Resource
    private TradeService tradeService;

    @Override
    public PageUtils<IndustryKnowledgeListVo> queryPage(IndustryKnowledgeListBo listBo) {
        QueryWrapper<IndustryKnowledgeEntity> wrapper = new QueryWrapper<>();
        if (listBo.getTradeId() != null) {
            wrapper.eq("trade_id", listBo.getTradeId());
        }
        if (listBo.getKnowledgeType() != null) {
            wrapper.eq("knowledge_type", listBo.getKnowledgeType());
        }
        wrapper.orderByDesc("update_date");

        IPage<IndustryKnowledgeEntity> iPage = industryKnowledgeService.page(
                new Query<IndustryKnowledgeEntity>().getPage(listBo.getPage(), listBo.getLimit()), wrapper);

        PageUtils<IndustryKnowledgeListVo> pageUtils = new PageUtils<>(listBo.getPage(), listBo.getLimit(), iPage);

        List<IndustryKnowledgeEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> tradeIds = records.stream().map(IndustryKnowledgeEntity::getTradeId).distinct().toList();
            Map<Long, String> tradeNameMap = tradeService.getTradeNameMap(tradeIds);

            List<IndustryKnowledgeListVo> vos = records.stream().map(item -> {
                IndustryKnowledgeListVo vo = new IndustryKnowledgeListVo();
                BeanUtils.copyProperties(item, vo);
                vo.setTradeName(tradeNameMap.getOrDefault(item.getTradeId(), ""));
                return vo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public IndustryKnowledgeVo info(Long id) {
        IndustryKnowledgeEntity entity = industryKnowledgeService.getById(id);
        if (entity != null) {
            IndustryKnowledgeVo vo = new IndustryKnowledgeVo();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }
        return null;
    }

    @Override
    public IndustryKnowledgeVo save(IndustryKnowledgeBo bo) {
        IndustryKnowledgeEntity entity = new IndustryKnowledgeEntity();
        BeanUtils.copyProperties(bo, entity);
        entity.setId(SnowflakeManager.nextValue());
        entity.setCreateDate(new Date());
        entity.setUpdateDate(new Date());

        industryKnowledgeService.save(entity);

        IndustryKnowledgeVo vo = new IndustryKnowledgeVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(IndustryKnowledgeBo bo) {
        IndustryKnowledgeEntity entity = new IndustryKnowledgeEntity();
        BeanUtils.copyProperties(bo, entity);
        entity.setUpdateDate(new Date());

        industryKnowledgeService.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long id) {
        IndustryKnowledgeEntity entity = new IndustryKnowledgeEntity();
        entity.setId(id);
        entity.setIsDeleted(1);
        entity.setUpdateDate(new Date());
        industryKnowledgeService.updateById(entity);
    }

    @Override
    public IndustryKnowledgeVo getByTradeIdAndType(Long tradeId, Integer knowledgeType) {
        QueryWrapper<IndustryKnowledgeEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("trade_id", tradeId);
        wrapper.eq("knowledge_type", knowledgeType);
        wrapper.eq("is_deleted", 0);
        IndustryKnowledgeEntity entity = industryKnowledgeService.getOne(wrapper);
        if (entity != null) {
            IndustryKnowledgeVo vo = new IndustryKnowledgeVo();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }
        return null;
    }

}
