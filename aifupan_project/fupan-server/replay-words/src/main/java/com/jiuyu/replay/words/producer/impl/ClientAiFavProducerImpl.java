package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.words.vo.ClientAiFavListVo;
import com.jiuyu.replay.words.vo.ClientAiFavInfoVo;
import com.jiuyu.replay.words.bo.ClientAiFavBo;
import com.jiuyu.replay.words.bo.ClientAiFavListBo;
import com.jiuyu.replay.words.repository.service.ClientAiFavService;
import com.jiuyu.replay.words.entity.ClientAiFavEntity;
import com.jiuyu.replay.words.producer.ClientAiFavProducer;

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
 * 运营/违规收藏列表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Service
public class ClientAiFavProducerImpl implements ClientAiFavProducer {

    @Resource
    private ClientAiFavService clientAiFavService;


    @Override
    public PageUtils<ClientAiFavListVo> queryPage(ClientAiFavListBo clientAiFavListBo) {
        QueryWrapper<ClientAiFavEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", clientAiFavListBo.getUserId());
        wrapper.eq("tenant_id", clientAiFavListBo.getTenantId());
        if(!StringUtils.isEmpty(clientAiFavListBo.getFavType())) {
            wrapper.eq("fav_type", clientAiFavListBo.getFavType());
        }
        if(!StringUtils.isEmpty(clientAiFavListBo.getDataResourceType())) {
            wrapper.eq("data_resource_type", clientAiFavListBo.getDataResourceType());
        }
        if(clientAiFavListBo.getResourceIds() != null && clientAiFavListBo.getResourceIds().size() > 0) {
            wrapper.in("data_resource_uuid", clientAiFavListBo.getResourceIds());
        }
        wrapper.orderByDesc("update_date");

        IPage<ClientAiFavEntity> iPage = clientAiFavService.page(new Query<ClientAiFavEntity>().getPageNoSort(clientAiFavListBo.getPage(), clientAiFavListBo.getLimit()), wrapper);

        PageUtils<ClientAiFavListVo> pageUtils = new PageUtils<>(clientAiFavListBo.getPage(), clientAiFavListBo.getLimit(), iPage);

        List<ClientAiFavEntity> records = iPage.getRecords();
        if(records != null && !records.isEmpty()) {
            List<ClientAiFavListVo> vos = records.stream().map(item -> {
                ClientAiFavListVo clientAiFavVo = new ClientAiFavListVo();
                BeanUtils.copyProperties(item, clientAiFavVo);
                return clientAiFavVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ClientAiFavInfoVo info(Long id) {

        ClientAiFavEntity clientAiFavEntity = clientAiFavService.getById(id);
        if(clientAiFavEntity != null) {
            ClientAiFavInfoVo clientAiFavInfoVo = new ClientAiFavInfoVo();
            BeanUtils.copyProperties(clientAiFavEntity, clientAiFavInfoVo);
            return clientAiFavInfoVo;
        }

        return null;
    }

    @Override
    public ClientAiFavInfoVo save(ClientAiFavBo clientAiFavBo) {

         ClientAiFavEntity clientAiFavEntity = new ClientAiFavEntity();
         BeanUtils.copyProperties(clientAiFavBo, clientAiFavEntity);
         clientAiFavEntity.setId(SnowflakeManager.nextValue());
         clientAiFavEntity.setCreateDate(new Date());
         clientAiFavEntity.setUpdateDate(new Date());

         clientAiFavService.save(clientAiFavEntity);

         ClientAiFavInfoVo clientAiFavInfoVo = new ClientAiFavInfoVo();
         BeanUtils.copyProperties(clientAiFavEntity, clientAiFavInfoVo);

         return clientAiFavInfoVo;
     }

    @Override
    public void update(ClientAiFavBo clientAiFavBo) {

        ClientAiFavEntity clientAiFavEntity = new ClientAiFavEntity();
        BeanUtils.copyProperties(clientAiFavBo, clientAiFavEntity);
        clientAiFavEntity.setUpdateDate(new Date());

        clientAiFavService.updateById(clientAiFavEntity);
    }

    @Override
    public void deleteById(Long id) {

        clientAiFavService.removeById(id);
    }

    @Override
    public void batchDelete(List<String> ids, Long userId, Long tenantId) {
        if(ids != null && ids.size() > 0) {
            QueryWrapper<ClientAiFavEntity> wrapper = new QueryWrapper<>();
            wrapper.in("id", ids);
            wrapper.eq("user_id", userId);
            wrapper.eq("tenant_id", tenantId);
            this.clientAiFavService.remove(wrapper);
        }
    }

    @Override
    public ClientAiFavInfoVo infoByFavTypeAndResourceId(Integer favType, String dataResourceUuid) {
        QueryWrapper<ClientAiFavEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("fav_type", favType);
        wrapper.eq("data_resource_uuid", dataResourceUuid);
        ClientAiFavEntity clientAiFavEntity = this.clientAiFavService.getOne(wrapper);
        if(clientAiFavEntity != null) {
            ClientAiFavInfoVo clientAiFavInfoVo = new ClientAiFavInfoVo();
            BeanUtils.copyProperties(clientAiFavEntity, clientAiFavInfoVo);
            return clientAiFavInfoVo;
        }
        return null;
    }

    @Override
    public void removeByResourceIds(List<String> resourceIds) {

        if(resourceIds != null && resourceIds.size() > 0) {
            QueryWrapper<ClientAiFavEntity> wrapper = new QueryWrapper<>();
            wrapper.in("data_resource_uuid", resourceIds);
            this.clientAiFavService.remove(wrapper);
        }
    }

    @Override
    public boolean hasDelete(ClientAiFavBo clientAiFavBo) {
        RRException.isNotEmpty(clientAiFavBo, "参数不能为空");
        RRException.isNotEmpty(clientAiFavBo.getTenantId(), "租户id不能为空");
        RRException.isNotEmpty(clientAiFavBo.getUserId(), "用户id不能为空");
        RRException.isNotEmpty(clientAiFavBo.getFavType(), "收藏类型不能为空");
        RRException.isNotEmpty(clientAiFavBo.getDataResourceType(), "数据来源类型不能为空");
        RRException.isNotEmpty(clientAiFavBo.getDataResourceUuid(), "数据来源id不能为空");

        ClientAiFavEntity one = clientAiFavService.getOne(new LambdaUpdateWrapper<ClientAiFavEntity>()
                .eq(ClientAiFavEntity::getTenantId, clientAiFavBo.getTenantId())
                .eq(ClientAiFavEntity::getUserId, clientAiFavBo.getUserId())
                .eq(ClientAiFavEntity::getFavType, clientAiFavBo.getFavType())
                .eq(ClientAiFavEntity::getDataResourceType, clientAiFavBo.getDataResourceType())
                .eq(ClientAiFavEntity::getDataResourceUuid, clientAiFavBo.getDataResourceUuid())
                .last("limit 1")
        );
        if (one != null){
            return  false;
        }

        int num = clientAiFavService.hasDelete(clientAiFavBo);

        return num == 1;
    }
}

