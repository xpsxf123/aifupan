package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.vo.TotalOnlineNumListVo;
import com.jiuyu.replay.words.vo.TotalOnlineNumInfoVo;
import com.jiuyu.replay.words.bo.TotalOnlineNumBo;
import com.jiuyu.replay.words.bo.TotalOnlineNumListBo;
import com.jiuyu.replay.words.repository.service.TotalOnlineNumService;
import com.jiuyu.replay.words.entity.TotalOnlineNumEntity;
import com.jiuyu.replay.words.producer.TotalOnlineNumProducer;

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
 * 直播总观看人次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Service
public class TotalOnlineNumProducerImpl implements TotalOnlineNumProducer {

    @Resource
    private TotalOnlineNumService totalOnlineNumService;


    @Override
    public PageUtils<TotalOnlineNumListVo> queryPage(TotalOnlineNumListBo totalOnlineNumListBo) {
        QueryWrapper<TotalOnlineNumEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(totalOnlineNumListBo.getKeyword())){
            wrapper.like("name", totalOnlineNumListBo.getKeyword());
        }

        IPage<TotalOnlineNumEntity> iPage = totalOnlineNumService.page(new Query<TotalOnlineNumEntity>().getPage(totalOnlineNumListBo.getPage(), totalOnlineNumListBo.getLimit()), wrapper);

        PageUtils<TotalOnlineNumListVo> pageUtils = new PageUtils<>(totalOnlineNumListBo.getPage(), totalOnlineNumListBo.getLimit(), iPage);

        List<TotalOnlineNumEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<TotalOnlineNumListVo> vos = records.stream().map(item -> {
                TotalOnlineNumListVo totalOnlineNumVo = new TotalOnlineNumListVo();
                BeanUtils.copyProperties(item, totalOnlineNumVo);
                return totalOnlineNumVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public TotalOnlineNumInfoVo info(Long id) {

        TotalOnlineNumEntity totalOnlineNumEntity = totalOnlineNumService.getById(id);
        if(totalOnlineNumEntity != null) {
            TotalOnlineNumInfoVo totalOnlineNumInfoVo = new TotalOnlineNumInfoVo();
            BeanUtils.copyProperties(totalOnlineNumEntity, totalOnlineNumInfoVo);
            return totalOnlineNumInfoVo;
        }

        return null;
    }

    /**
     * 新增直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
     public TotalOnlineNumInfoVo save(TotalOnlineNumBo totalOnlineNumBo) {

         TotalOnlineNumEntity totalOnlineNumEntity = new TotalOnlineNumEntity();
         BeanUtils.copyProperties(totalOnlineNumBo, totalOnlineNumEntity);
         totalOnlineNumEntity.setId(SnowflakeManager.nextValue());
         totalOnlineNumEntity.setCreateDate(new Date());
         totalOnlineNumEntity.setUpdateDate(new Date());

         totalOnlineNumService.save(totalOnlineNumEntity);

         TotalOnlineNumInfoVo totalOnlineNumInfoVo = new TotalOnlineNumInfoVo();
         BeanUtils.copyProperties(totalOnlineNumEntity, totalOnlineNumInfoVo);

         return totalOnlineNumInfoVo;
     }

    /**
     * 修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    public void update(TotalOnlineNumBo totalOnlineNumBo) {

        TotalOnlineNumEntity totalOnlineNumEntity = new TotalOnlineNumEntity();
        BeanUtils.copyProperties(totalOnlineNumBo, totalOnlineNumEntity);
        totalOnlineNumEntity.setUpdateDate(new Date());

        totalOnlineNumService.updateById(totalOnlineNumEntity);
    }

    /**
     * 删除直播总观看人次
     * @param id 直播总观看人次id
     * @return
     */
    public void deleteById(Long id) {

        totalOnlineNumService.removeById(id);
    }

    @Override
    public TotalOnlineNumInfoVo infoByBatchNumber(String batchNumber) {

        TotalOnlineNumEntity totalOnlineNumEntity = this.totalOnlineNumService.getOne(new QueryWrapper<TotalOnlineNumEntity>().eq("batch_number", batchNumber));
        if(totalOnlineNumEntity != null) {
            TotalOnlineNumInfoVo totalOnlineNumInfoVo = new TotalOnlineNumInfoVo();
            BeanUtils.copyProperties(totalOnlineNumEntity, totalOnlineNumInfoVo);
            return totalOnlineNumInfoVo;
        }

        return null;
    }

    @Override
    public TotalOnlineNumInfoVo infoByUserIdAndBatchNumber(Long userId, String batchNumber) {

        TotalOnlineNumEntity totalOnlineNumEntity = this.totalOnlineNumService.getOne(new QueryWrapper<TotalOnlineNumEntity>().eq("user_id", userId).eq("batch_number", batchNumber));

        if(totalOnlineNumEntity != null) {
            TotalOnlineNumInfoVo totalOnlineNumInfoVo = new TotalOnlineNumInfoVo();
            BeanUtils.copyProperties(totalOnlineNumEntity, totalOnlineNumInfoVo);
            return totalOnlineNumInfoVo;
        }

        return null;
    }

    @Override
    public List<TotalOnlineNumEntity> listAllNum() {
        List<TotalOnlineNumEntity> onlineNumList = this.totalOnlineNumService.list();
        if(onlineNumList != null && onlineNumList.size() > 0) {
            return onlineNumList;
        }
        return null;
    }

}

