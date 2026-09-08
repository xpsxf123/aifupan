package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.vo.AiCueButtonListVo;
import com.jiuyu.replay.words.vo.AiCueButtonInfoVo;
import com.jiuyu.replay.words.bo.AiCueButtonBo;
import com.jiuyu.replay.words.bo.AiCueButtonListBo;
import com.jiuyu.replay.words.repository.service.AiCueButtonService;
import com.jiuyu.replay.words.entity.AiCueButtonEntity;
import com.jiuyu.replay.words.producer.AiCueButtonProducer;

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
 * 固定提示按钮

 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
@Service
public class AiCueButtonProducerImpl implements AiCueButtonProducer {

    @Resource
    private AiCueButtonService aiCueButtonService;


    @Override
    public PageUtils<AiCueButtonListVo> queryPage(AiCueButtonListBo aiCueButtonListBo) {

        QueryWrapper<AiCueButtonEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(aiCueButtonListBo.getKeyword())){
            wrapper.like("button_name", aiCueButtonListBo.getKeyword());
        }

//        //行业信息筛选
//        if(!StringUtils.isEmpty(aiCueButtonListBo.getTradeId())){
//            wrapper.eq("trade_id", aiCueButtonListBo.getTradeId());
//        }

        IPage<AiCueButtonEntity> iPage = aiCueButtonService.page(new Query<AiCueButtonEntity>().getPage(aiCueButtonListBo.getPage(), aiCueButtonListBo.getLimit()), wrapper);

        PageUtils<AiCueButtonListVo> pageUtils = new PageUtils<>(aiCueButtonListBo.getPage(), aiCueButtonListBo.getLimit(), iPage);

        List<AiCueButtonEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AiCueButtonListVo> vos = records.stream().map(item -> {
                AiCueButtonListVo aiCueButtonVo = new AiCueButtonListVo();
                BeanUtils.copyProperties(item, aiCueButtonVo);
                return aiCueButtonVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AiCueButtonInfoVo info(Long id) {

        AiCueButtonEntity aiCueButtonEntity = aiCueButtonService.getById(id);
        if(aiCueButtonEntity != null) {
            AiCueButtonInfoVo aiCueButtonInfoVo = new AiCueButtonInfoVo();
            BeanUtils.copyProperties(aiCueButtonEntity, aiCueButtonInfoVo);
            return aiCueButtonInfoVo;
        }

        return null;
    }

    /**
     * 新增固定提示按钮
     * @param aiCueButtonBo 固定提示按钮对象
     * @return
     */
     public AiCueButtonInfoVo save(AiCueButtonBo aiCueButtonBo) {

         AiCueButtonEntity aiCueButtonEntity = new AiCueButtonEntity();
         BeanUtils.copyProperties(aiCueButtonBo, aiCueButtonEntity);
         aiCueButtonEntity.setId(SnowflakeManager.nextValue());
         aiCueButtonEntity.setCreateDate(new Date());
         aiCueButtonEntity.setUpdateDate(new Date());

         aiCueButtonService.save(aiCueButtonEntity);

         AiCueButtonInfoVo aiCueButtonInfoVo = new AiCueButtonInfoVo();
         BeanUtils.copyProperties(aiCueButtonEntity, aiCueButtonInfoVo);

         return aiCueButtonInfoVo;
     }

    /**
     * 修改固定提示按钮
     * @param aiCueButtonBo 固定提示按钮对象
     * @return
     */
    public void update(AiCueButtonBo aiCueButtonBo) {

        AiCueButtonEntity aiCueButtonEntity = new AiCueButtonEntity();
        BeanUtils.copyProperties(aiCueButtonBo, aiCueButtonEntity);
        aiCueButtonEntity.setUpdateDate(new Date());

        aiCueButtonService.updateById(aiCueButtonEntity);
    }

    /**
     * 删除固定提示按钮
     * @param id 固定提示按钮id
     * @return
     */
    public void deleteById(Long id) {

        aiCueButtonService.removeById(id);
    }


}

