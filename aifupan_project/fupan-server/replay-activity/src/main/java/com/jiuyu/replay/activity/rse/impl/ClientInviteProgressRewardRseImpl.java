package com.jiuyu.replay.activity.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.activity.entity.ClientInviteProgressRewardEntity;
import com.jiuyu.replay.activity.repository.service.ClientInviteProgressRewardService;
import com.jiuyu.replay.activity.rse.ClientInviteProgressRewardRse;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressRewardBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressRewardListBo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 邀请进度奖励
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Service
public class ClientInviteProgressRewardRseImpl implements ClientInviteProgressRewardRse {

    @Resource
    private ClientInviteProgressRewardService clientInviteProgressRewardService;


    @Override
    public PageUtils<ClientInviteProgressRewardListVo> queryPage(ClientInviteProgressRewardListBo clientInviteProgressRewardListBo) {
        QueryWrapper<ClientInviteProgressRewardEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(clientInviteProgressRewardListBo.getKeyword())) {
            wrapper.like("name", clientInviteProgressRewardListBo.getKeyword());
        }

        IPage<ClientInviteProgressRewardEntity> iPage = clientInviteProgressRewardService.page(new Query<ClientInviteProgressRewardEntity>().getPage(clientInviteProgressRewardListBo.getPage(), clientInviteProgressRewardListBo.getLimit()), wrapper);

        PageUtils<ClientInviteProgressRewardListVo> pageUtils = new PageUtils<>(clientInviteProgressRewardListBo.getPage(), clientInviteProgressRewardListBo.getLimit(), iPage);

        List<ClientInviteProgressRewardEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<ClientInviteProgressRewardListVo> vos = records.stream().map(item -> {
                ClientInviteProgressRewardListVo clientInviteProgressRewardVo = new ClientInviteProgressRewardListVo();
                BeanUtils.copyProperties(item, clientInviteProgressRewardVo);
                return clientInviteProgressRewardVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ClientInviteProgressRewardInfoVo info(Long id) {

        ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = clientInviteProgressRewardService.getById(id);
        if (clientInviteProgressRewardEntity != null) {
            ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = new ClientInviteProgressRewardInfoVo();
            BeanUtils.copyProperties(clientInviteProgressRewardEntity, clientInviteProgressRewardInfoVo);
            return clientInviteProgressRewardInfoVo;
        }

        return null;
    }

    @Override
    public ClientInviteProgressRewardInfoVo save(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = new ClientInviteProgressRewardEntity();
        BeanUtils.copyProperties(clientInviteProgressRewardBo, clientInviteProgressRewardEntity);
        clientInviteProgressRewardEntity.setId(SnowflakeManager.nextValue());
        clientInviteProgressRewardEntity.setCreateDate(new Date());
        clientInviteProgressRewardEntity.setUpdateDate(new Date());

        clientInviteProgressRewardService.save(clientInviteProgressRewardEntity);

        ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = new ClientInviteProgressRewardInfoVo();
        BeanUtils.copyProperties(clientInviteProgressRewardEntity, clientInviteProgressRewardInfoVo);

        return clientInviteProgressRewardInfoVo;
    }

    @Override
    public void update(ClientInviteProgressRewardBo clientInviteProgressRewardBo) {

        ClientInviteProgressRewardEntity clientInviteProgressRewardEntity = new ClientInviteProgressRewardEntity();
        BeanUtils.copyProperties(clientInviteProgressRewardBo, clientInviteProgressRewardEntity);
        clientInviteProgressRewardEntity.setUpdateDate(new Date());

        clientInviteProgressRewardService.updateById(clientInviteProgressRewardEntity);
    }

    @Override
    public void deleteById(Long id) {

        clientInviteProgressRewardService.removeById(id);
    }

    @Override
    public List<ClientInviteProgressRewardInfoVo> listByProgressRewardIds(Collection<Long> progressRewardIds) {

        if (progressRewardIds != null && progressRewardIds.size() > 0) {
            List<ClientInviteProgressRewardEntity> clientInviteProgressRewardEntities = this.clientInviteProgressRewardService.listByIds(progressRewardIds);
            if (clientInviteProgressRewardEntities != null && clientInviteProgressRewardEntities.size() > 0) {
                List<ClientInviteProgressRewardInfoVo> clientInviteProgressRewardInfoVos = clientInviteProgressRewardEntities.stream().map(item -> {
                    ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = new ClientInviteProgressRewardInfoVo();
                    BeanUtils.copyProperties(item, clientInviteProgressRewardInfoVo);
                    return clientInviteProgressRewardInfoVo;
                }).collect(Collectors.toList());

                return clientInviteProgressRewardInfoVos;
            }
        }
        return null;
    }

    @Override
    public List<ClientInviteProgressRewardInfoVo> listByProgressIdsAndStatus(List<Long> progressIds) {

        QueryWrapper<ClientInviteProgressRewardEntity> wrapper = new QueryWrapper<>();
        wrapper.in("progress_id", progressIds);

        List<ClientInviteProgressRewardEntity> clientInviteProgressRewardEntities = this.clientInviteProgressRewardService.list(wrapper);
        if(clientInviteProgressRewardEntities != null && clientInviteProgressRewardEntities.size() > 0) {
            List<ClientInviteProgressRewardInfoVo> clientInviteProgressRewardInfoVos = clientInviteProgressRewardEntities.stream().map(item -> {
                ClientInviteProgressRewardInfoVo clientInviteProgressRewardInfoVo = new ClientInviteProgressRewardInfoVo();
                BeanUtils.copyProperties(item, clientInviteProgressRewardInfoVo);
                return clientInviteProgressRewardInfoVo;
            }).collect(Collectors.toList());

            return clientInviteProgressRewardInfoVos;
        }

        return null;
    }


    @Override
    public List<ClientInviteProgressRewardBo> listByProgressIds(List<Long> progressIds) {
        return BeanConvertUtils.convertList(clientInviteProgressRewardService.list(new LambdaQueryWrapper<>(ClientInviteProgressRewardEntity.class).in(ClientInviteProgressRewardEntity::getProgressId, progressIds)), ClientInviteProgressRewardBo.class);
    }
}

