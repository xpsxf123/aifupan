package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.HistoryParagraphBo;
import com.jiuyu.replay.words.bo.HistoryParagraphListBo;
import com.jiuyu.replay.words.entity.HistoryParagraphEntity;
import com.jiuyu.replay.words.producer.HistoryParagraphProducer;
import com.jiuyu.replay.words.repository.service.HistoryParagraphService;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.vo.HistoryParagraphListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * ai问答的历史分析段落记录
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Service
public class HistoryParagraphProducerImpl implements HistoryParagraphProducer {

    @Resource
    private HistoryParagraphService historyParagraphService;


    @Override
    public PageUtils<HistoryParagraphListVo> queryPage(HistoryParagraphListBo historyParagraphListBo) {
        LambdaQueryWrapper<HistoryParagraphEntity> wrapper = new LambdaQueryWrapper<HistoryParagraphEntity>()
                .eq(ObjectUtil.isNotEmpty(historyParagraphListBo.getSourceType()), HistoryParagraphEntity::getSourceType, historyParagraphListBo.getSourceType())
                .eq(ObjectUtil.isNotEmpty(historyParagraphListBo.getSourceId()), HistoryParagraphEntity::getSourceId, historyParagraphListBo.getSourceId())
                .eq(ObjectUtil.isNotEmpty(historyParagraphListBo.getType()), HistoryParagraphEntity::getType, historyParagraphListBo.getType())
                .orderByAsc(HistoryParagraphEntity::getId)
                ;

        IPage<HistoryParagraphEntity> iPage = historyParagraphService.page(new Query<HistoryParagraphEntity>().getPageNoSort(historyParagraphListBo.getPage(), historyParagraphListBo.getLimit()), wrapper);

        PageUtils<HistoryParagraphListVo> pageUtils = new PageUtils<>(historyParagraphListBo.getPage(), historyParagraphListBo.getLimit(), iPage);

        List<HistoryParagraphEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<HistoryParagraphListVo> vos = records.stream().map(item -> {
                HistoryParagraphListVo historyParagraphVo = new HistoryParagraphListVo();
                BeanUtils.copyProperties(item, historyParagraphVo);
                return historyParagraphVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public HistoryParagraphInfoVo info(Long id) {

        HistoryParagraphEntity historyParagraphEntity = historyParagraphService.getById(id);
        if(historyParagraphEntity != null) {
            HistoryParagraphInfoVo historyParagraphInfoVo = new HistoryParagraphInfoVo();
            BeanUtils.copyProperties(historyParagraphEntity, historyParagraphInfoVo);
            return historyParagraphInfoVo;
        }

        return null;
    }

    /**
     * 新增ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
     public HistoryParagraphInfoVo save(HistoryParagraphBo historyParagraphBo) {

         HistoryParagraphEntity historyParagraphEntity = new HistoryParagraphEntity();
         BeanUtils.copyProperties(historyParagraphBo, historyParagraphEntity);
         historyParagraphEntity.setId(SnowflakeManager.nextValue());
         historyParagraphEntity.setCreateDate(new Date());
         historyParagraphEntity.setUpdateDate(new Date());

         historyParagraphService.save(historyParagraphEntity);

         HistoryParagraphInfoVo historyParagraphInfoVo = new HistoryParagraphInfoVo();
         BeanUtils.copyProperties(historyParagraphEntity, historyParagraphInfoVo);

         return historyParagraphInfoVo;
     }

    /**
     * 修改ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
    public void update(HistoryParagraphBo historyParagraphBo) {

        HistoryParagraphEntity historyParagraphEntity = new HistoryParagraphEntity();
        BeanUtils.copyProperties(historyParagraphBo, historyParagraphEntity);
        historyParagraphEntity.setUpdateDate(new Date());

        historyParagraphService.updateById(historyParagraphEntity);
    }

    /**
     * 删除ai问答的历史分析段落记录
     * @param id ai问答的历史分析段落记录id
     * @return
     */
    public void deleteById(Long id) {

        historyParagraphService.removeById(id);
    }

    @Override
    public void deleteHistoryParagraph(HistoryParagraphBo paragraphBo) {
        RRException.isNotEmpty(paragraphBo.getSourceType(), "资产类型不能为空");
        RRException.isNotEmpty(paragraphBo.getSourceId(), "来源的id不能为空");
        RRException.isNotEmpty(paragraphBo.getCode(), "code不能为空");
        RRException.isNotEmpty(paragraphBo.getType(), "助手类型不能为空");
        historyParagraphService.remove(new LambdaQueryWrapper<HistoryParagraphEntity>()
                .eq(HistoryParagraphEntity::getSourceType, paragraphBo.getSourceType())
                .eq(HistoryParagraphEntity::getSourceId, paragraphBo.getSourceId())
                .eq(HistoryParagraphEntity::getCode, paragraphBo.getCode())
                .eq(HistoryParagraphEntity::getType, paragraphBo.getType())
        );
    }

    @Override
    public HistoryParagraphInfoVo getByCode(Integer type, String sourceId, Integer sourceType, String code) {
        HistoryParagraphEntity historyParagraphEntity = historyParagraphService.getOne(new LambdaQueryWrapper<HistoryParagraphEntity>()
                .eq(HistoryParagraphEntity::getType, type)
                .eq(HistoryParagraphEntity::getSourceId, sourceId)
                .eq(HistoryParagraphEntity::getSourceType, sourceType)
                .eq(HistoryParagraphEntity::getCode, code)
        );
        if(historyParagraphEntity != null) {
            HistoryParagraphInfoVo historyParagraphInfoVo = new HistoryParagraphInfoVo();
            BeanUtils.copyProperties(historyParagraphEntity, historyParagraphInfoVo);
            return historyParagraphInfoVo;
        }

        return null;
    }
}

