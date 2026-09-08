package com.jiuyu.replay.third.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.third.vo.chanmama.ChanmamaAccountListVo;
import com.jiuyu.replay.third.vo.chanmama.ChanmamaAccountInfoVo;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaAccountBo;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaAccountListBo;
import com.jiuyu.replay.third.repository.service.ChanmamaAccountService;
import com.jiuyu.replay.third.entity.ChanmamaAccountEntity;
import com.jiuyu.replay.third.producer.ChanmamaAccountProducer;

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
 * 第三方数据平台账号
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-14 15:59:03
 */
@Service
public class ChanmamaAccountProducerImpl implements ChanmamaAccountProducer {

    @Resource
    private ChanmamaAccountService chanmamaAccountService;


    @Override
    public PageUtils<ChanmamaAccountListVo> queryPage(ChanmamaAccountListBo chanmamaAccountListBo) {
        QueryWrapper<ChanmamaAccountEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(chanmamaAccountListBo.getKeyword())){
            wrapper.like("name", chanmamaAccountListBo.getKeyword());
        }

        IPage<ChanmamaAccountEntity> iPage = chanmamaAccountService.page(new Query<ChanmamaAccountEntity>().getPage(chanmamaAccountListBo.getPage(), chanmamaAccountListBo.getLimit()), wrapper);

        PageUtils<ChanmamaAccountListVo> pageUtils = new PageUtils<>(chanmamaAccountListBo.getPage(), chanmamaAccountListBo.getLimit(), iPage);

        List<ChanmamaAccountEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ChanmamaAccountListVo> vos = records.stream().map(item -> {
                ChanmamaAccountListVo chanmamaAccountVo = new ChanmamaAccountListVo();
                BeanUtils.copyProperties(item, chanmamaAccountVo);
                return chanmamaAccountVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ChanmamaAccountInfoVo info(Long id) {

        ChanmamaAccountEntity chanmamaAccountEntity = chanmamaAccountService.getById(id);
        if(chanmamaAccountEntity != null) {
            ChanmamaAccountInfoVo chanmamaAccountInfoVo = new ChanmamaAccountInfoVo();
            BeanUtils.copyProperties(chanmamaAccountEntity, chanmamaAccountInfoVo);
            return chanmamaAccountInfoVo;
        }

        return null;
    }

    @Override
    public ChanmamaAccountInfoVo save(ChanmamaAccountBo chanmamaAccountBo) {

         ChanmamaAccountEntity chanmamaAccountEntity = new ChanmamaAccountEntity();
         BeanUtils.copyProperties(chanmamaAccountBo, chanmamaAccountEntity);
         chanmamaAccountEntity.setId(SnowflakeManager.nextValue());
         chanmamaAccountEntity.setCreateDate(new Date());
         chanmamaAccountEntity.setUpdateDate(new Date());

         chanmamaAccountService.save(chanmamaAccountEntity);

         ChanmamaAccountInfoVo chanmamaAccountInfoVo = new ChanmamaAccountInfoVo();
         BeanUtils.copyProperties(chanmamaAccountEntity, chanmamaAccountInfoVo);

         return chanmamaAccountInfoVo;
     }

    @Override
    public void update(ChanmamaAccountBo chanmamaAccountBo) {

        ChanmamaAccountEntity chanmamaAccountEntity = new ChanmamaAccountEntity();
        BeanUtils.copyProperties(chanmamaAccountBo, chanmamaAccountEntity);
        chanmamaAccountEntity.setUpdateDate(new Date());

        chanmamaAccountService.updateById(chanmamaAccountEntity);
    }

    @Override
    public void deleteById(Long id) {

        chanmamaAccountService.removeById(id);
    }


}

