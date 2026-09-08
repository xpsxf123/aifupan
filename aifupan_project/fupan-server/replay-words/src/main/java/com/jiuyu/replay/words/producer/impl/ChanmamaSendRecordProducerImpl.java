package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordBo;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordListBo;
import com.jiuyu.replay.words.entity.ChanmamaSendRecordEntity;
import com.jiuyu.replay.words.producer.ChanmamaSendRecordProducer;
import com.jiuyu.replay.words.repository.service.ChanmamaSendRecordService;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordInfoVo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 第三方数据平台发送记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-22 14:21:09
 */
@Service
public class ChanmamaSendRecordProducerImpl implements ChanmamaSendRecordProducer {

    @Resource
    private ChanmamaSendRecordService chanmamaSendRecordService;


    @Override
    public PageUtils<ChanmamaSendRecordListVo> queryPage(ChanmamaSendRecordListBo chanmamaSendRecordListBo) {
        QueryWrapper<ChanmamaSendRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(chanmamaSendRecordListBo.getKeyword())){
            wrapper.like("name", chanmamaSendRecordListBo.getKeyword());
        }

        IPage<ChanmamaSendRecordEntity> iPage = chanmamaSendRecordService.page(new Query<ChanmamaSendRecordEntity>().getPage(chanmamaSendRecordListBo.getPage(), chanmamaSendRecordListBo.getLimit()), wrapper);

        PageUtils<ChanmamaSendRecordListVo> pageUtils = new PageUtils<>(chanmamaSendRecordListBo.getPage(), chanmamaSendRecordListBo.getLimit(), iPage);

        List<ChanmamaSendRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ChanmamaSendRecordListVo> vos = records.stream().map(item -> {
                ChanmamaSendRecordListVo chanmamaSendRecordVo = new ChanmamaSendRecordListVo();
                BeanUtils.copyProperties(item, chanmamaSendRecordVo);
                return chanmamaSendRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ChanmamaSendRecordInfoVo info(Long id) {

        ChanmamaSendRecordEntity chanmamaSendRecordEntity = chanmamaSendRecordService.getById(id);
        if(chanmamaSendRecordEntity != null) {
            ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = new ChanmamaSendRecordInfoVo();
            BeanUtils.copyProperties(chanmamaSendRecordEntity, chanmamaSendRecordInfoVo);
            return chanmamaSendRecordInfoVo;
        }

        return null;
    }

    @Override
    public ChanmamaSendRecordInfoVo save(ChanmamaSendRecordBo chanmamaSendRecordBo) {

         ChanmamaSendRecordEntity chanmamaSendRecordEntity = new ChanmamaSendRecordEntity();
         BeanUtils.copyProperties(chanmamaSendRecordBo, chanmamaSendRecordEntity);
         chanmamaSendRecordEntity.setId(SnowflakeManager.nextValue());
         chanmamaSendRecordEntity.setCreateDate(new Date());
         chanmamaSendRecordEntity.setUpdateDate(new Date());

         chanmamaSendRecordService.save(chanmamaSendRecordEntity);

         ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = new ChanmamaSendRecordInfoVo();
         BeanUtils.copyProperties(chanmamaSendRecordEntity, chanmamaSendRecordInfoVo);

         return chanmamaSendRecordInfoVo;
     }

    @Override
    public void update(ChanmamaSendRecordBo chanmamaSendRecordBo) {

        ChanmamaSendRecordEntity chanmamaSendRecordEntity = new ChanmamaSendRecordEntity();
        BeanUtils.copyProperties(chanmamaSendRecordBo, chanmamaSendRecordEntity);
        chanmamaSendRecordEntity.setUpdateDate(new Date());

        chanmamaSendRecordService.updateById(chanmamaSendRecordEntity);
    }

    @Override
    public void deleteById(Long id) {

        chanmamaSendRecordService.removeById(id);
    }

    @Override
    public ChanmamaSendRecordInfoVo infoByRequestId(String requestId) {

        ChanmamaSendRecordEntity chanmamaSendRecordEntity = this.chanmamaSendRecordService.getOne(new QueryWrapper<ChanmamaSendRecordEntity>().eq("request_id", requestId));
        if(chanmamaSendRecordEntity != null) {
            ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = new ChanmamaSendRecordInfoVo();
            BeanUtils.copyProperties(chanmamaSendRecordEntity, chanmamaSendRecordInfoVo);
            return chanmamaSendRecordInfoVo;
        }

        return null;
    }

    @Override
    public void updateCallbackBody(ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo, Integer code, String body, Integer dataStatus, Integer accountType, String roomId) {

        QueryWrapper<ChanmamaSendRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("request_id", chanmamaSendRecordInfoVo.getRequestId());
        ChanmamaSendRecordEntity chanmamaSendRecordEntity = this.chanmamaSendRecordService.getOne(wrapper);

        if(chanmamaSendRecordEntity != null) {
            chanmamaSendRecordEntity.setAccountType(accountType);
            chanmamaSendRecordEntity.setDataStatus(dataStatus);
            chanmamaSendRecordEntity.setCallbackBody(body);
            chanmamaSendRecordEntity.setCallbackStatus(code.toString());
            chanmamaSendRecordEntity.setRoomId(roomId);
            chanmamaSendRecordEntity.setUpdateDate(new Date());
            this.chanmamaSendRecordService.updateById(chanmamaSendRecordEntity);
        }

    }

    @Override
    public void saveRevisionData(Long sendRecodeId, Integer status, String revisionRequestBody, String revisionResponseBody) {

        ChanmamaSendRecordEntity chanmamaSendRecordEntity = this.chanmamaSendRecordService.getById(sendRecodeId);
        if(chanmamaSendRecordEntity != null) {
            chanmamaSendRecordEntity.setDataStatus(status);
            chanmamaSendRecordEntity.setUpdateDate(new Date());
            chanmamaSendRecordEntity.setRevisionRequestBody(revisionRequestBody);
            chanmamaSendRecordEntity.setRevisionResponseBody(revisionResponseBody);
            this.chanmamaSendRecordService.updateById(chanmamaSendRecordEntity);
        }
    }


}

