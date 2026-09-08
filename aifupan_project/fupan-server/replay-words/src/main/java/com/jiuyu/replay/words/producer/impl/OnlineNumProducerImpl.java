package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.vo.OnlineNumListVo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.bo.OnlineNumBo;
import com.jiuyu.replay.words.bo.OnlineNumListBo;
import com.jiuyu.replay.words.repository.service.OnlineNumService;
import com.jiuyu.replay.words.entity.OnlineNumEntity;
import com.jiuyu.replay.words.producer.OnlineNumProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 直播实时在线人数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Service
public class OnlineNumProducerImpl implements OnlineNumProducer {

    @Resource
    private OnlineNumService onlineNumService;


    @Override
    public PageUtils<OnlineNumListVo> queryPage(OnlineNumListBo onlineNumListBo) {
        QueryWrapper<OnlineNumEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(onlineNumListBo.getKeyword())){
            wrapper.like("name", onlineNumListBo.getKeyword());
        }

        IPage<OnlineNumEntity> iPage = onlineNumService.page(new Query<OnlineNumEntity>().getPage(onlineNumListBo.getPage(), onlineNumListBo.getLimit()), wrapper);

        PageUtils<OnlineNumListVo> pageUtils = new PageUtils<>(onlineNumListBo.getPage(), onlineNumListBo.getLimit(), iPage);

        List<OnlineNumEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<OnlineNumListVo> vos = records.stream().map(item -> {
                OnlineNumListVo onlineNumVo = new OnlineNumListVo();
                BeanUtils.copyProperties(item, onlineNumVo);
                return onlineNumVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public OnlineNumInfoVo info(Long id) {

        OnlineNumEntity onlineNumEntity = onlineNumService.getById(id);
        if(onlineNumEntity != null) {
            OnlineNumInfoVo onlineNumInfoVo = new OnlineNumInfoVo();
            BeanUtils.copyProperties(onlineNumEntity, onlineNumInfoVo);
            return onlineNumInfoVo;
        }

        return null;
    }

    /**
     * 新增直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
     public OnlineNumInfoVo save(OnlineNumBo onlineNumBo) {

         OnlineNumEntity onlineNumEntity = new OnlineNumEntity();
         BeanUtils.copyProperties(onlineNumBo, onlineNumEntity);
         onlineNumEntity.setId(SnowflakeManager.nextValue());
         onlineNumEntity.setCreateDate(new Date());
         onlineNumEntity.setUpdateDate(new Date());

         onlineNumService.save(onlineNumEntity);

         OnlineNumInfoVo onlineNumInfoVo = new OnlineNumInfoVo();
         BeanUtils.copyProperties(onlineNumEntity, onlineNumInfoVo);

         return onlineNumInfoVo;
     }

    /**
     * 修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    public void update(OnlineNumBo onlineNumBo) {

        OnlineNumEntity onlineNumEntity = new OnlineNumEntity();
        BeanUtils.copyProperties(onlineNumBo, onlineNumEntity);
        onlineNumEntity.setUpdateDate(new Date());

        onlineNumService.updateById(onlineNumEntity);
    }

    /**
     * 删除直播实时在线人数
     * @param id 直播实时在线人数id
     * @return
     */
    public void deleteById(Long id) {

        onlineNumService.removeById(id);
    }

    @Override
    public List<OnlineNumInfoVo> listByVideoId(String videoId) {

        List<OnlineNumEntity> onlineNumEntities = this.onlineNumService.list(new QueryWrapper<OnlineNumEntity>().eq("video_id", videoId));
        if(onlineNumEntities != null && onlineNumEntities.size() > 0) {
            List<OnlineNumInfoVo> onlineNumInfoVoList = onlineNumEntities.stream().map(item -> {
                OnlineNumInfoVo onlineNumInfoVo = new OnlineNumInfoVo();
                BeanUtils.copyProperties(item, onlineNumInfoVo);
                return onlineNumInfoVo;
            }).toList();

            return onlineNumInfoVoList;
        }

        return null;
    }

    @Override
    public OnlineNumInfoVo infoByUserIdAndBatchNumber(Long userId, String batchNumber) {

        QueryWrapper<OnlineNumEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("batch_number", batchNumber);

        List<OnlineNumEntity> onlineNumEntities = this.onlineNumService.list(wrapper);
        if(onlineNumEntities != null && onlineNumEntities.size() > 0) {
            for (OnlineNumEntity onlineNumEntity : onlineNumEntities) {
                if(!StringUtils.isEmpty(onlineNumEntity.getPeopleNumData())) {
                    OnlineNumInfoVo onlineNumInfoVo = new OnlineNumInfoVo();
                    BeanUtils.copyProperties(onlineNumEntity, onlineNumInfoVo);
                    return onlineNumInfoVo;
                }
            }
        }

        return null;
    }

    @Override
    public List<OnlineNumInfoVo> listByUserIdAndBatchNumber(Long userId, String batchNumber) {

        QueryWrapper<OnlineNumEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("batch_number", batchNumber);

        List<OnlineNumEntity> onlineNumEntities = this.onlineNumService.list(wrapper);

        if(onlineNumEntities != null && onlineNumEntities.size() > 0) {
            List<OnlineNumInfoVo> onlineNumInfoVoList = new LinkedList<>();
            for (OnlineNumEntity onlineNumEntity : onlineNumEntities) {
                if(!StringUtils.isEmpty(onlineNumEntity.getPeopleNumData())) {

                    String[] itemList = onlineNumEntity.getPeopleNumData().split("_");
                    for (String item : itemList) {
                        String[] split = item.split("@");
                        if(split.length > 1) {
                            OnlineNumInfoVo onlineNumInfoVo = new OnlineNumInfoVo();
                            if(!StringUtils.isEmpty(split[0]) && !StringUtils.isEmpty(split[1])) {
                                onlineNumInfoVo.setRecordDate(item.split("@")[0]);
                                onlineNumInfoVo.setPeopleNum(item.split("@")[1]);
                                onlineNumInfoVoList.add(onlineNumInfoVo);
                            }
                        }
                    }

                }else {
                    OnlineNumInfoVo onlineNumInfoVo = new OnlineNumInfoVo();
                    onlineNumInfoVo.setPeopleNum(onlineNumEntity.getPeopleNum());
                    onlineNumInfoVo.setRecordDate(onlineNumEntity.getRecordDate());
                    onlineNumInfoVoList.add(onlineNumInfoVo);
                }
            }

            return onlineNumInfoVoList;
        }

        return null;
    }


}

