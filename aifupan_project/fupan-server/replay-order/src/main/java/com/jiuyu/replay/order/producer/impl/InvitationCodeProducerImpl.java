package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.InvitationCodeBo;
import com.jiuyu.replay.order.bo.InvitationCodeListBo;
import com.jiuyu.replay.order.entity.InvitationCodeBatchEntity;
import com.jiuyu.replay.order.entity.InvitationCodeEntity;
import com.jiuyu.replay.order.producer.InvitationCodeProducer;
import com.jiuyu.replay.order.repository.service.InvitationCodeBatchService;
import com.jiuyu.replay.order.repository.service.InvitationCodeService;
import com.jiuyu.replay.order.repository.service.PackageService;
import com.jiuyu.replay.order.vo.InvitationCodeBatchListVo;
import com.jiuyu.replay.order.vo.InvitationCodeInfoVo;
import com.jiuyu.replay.order.vo.InvitationCodeListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 邀请码
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Service
public class InvitationCodeProducerImpl implements InvitationCodeProducer {

    @Resource
    private InvitationCodeService invitationCodeService;
    @Resource
    private InvitationCodeBatchService invitationCodeBatchService;
    @Resource
    private PackageService packageService;


    @Override
    public PageUtils<InvitationCodeListVo> queryPage(InvitationCodeListBo invitationCodeListBo) {
        // 不查询激活版的
        List<InvitationCodeBatchEntity> activeVersions = invitationCodeBatchService.list(new LambdaQueryWrapper<InvitationCodeBatchEntity>()
                .eq(InvitationCodeBatchEntity::getType, 2)
        );
        if (invitationCodeListBo.getNotCodeBatchIds() == null) {
            invitationCodeListBo.setNotCodeBatchIds(new ArrayList<>());
        }
        if (ObjectUtil.isNotEmpty(activeVersions)) {
            invitationCodeListBo.getNotCodeBatchIds().addAll(activeVersions.stream().map(InvitationCodeBatchEntity::getId).toList());
        }

        LambdaQueryWrapper<InvitationCodeEntity> wrapper = new LambdaQueryWrapper<InvitationCodeEntity>()
                .like(ObjectUtil.isNotEmpty(invitationCodeListBo.getCode()), InvitationCodeEntity::getCode, invitationCodeListBo.getCode())
                .eq(ObjectUtil.isNotEmpty(invitationCodeListBo.getCodeBatchId()), InvitationCodeEntity::getBatchId, invitationCodeListBo.getCodeBatchId())
                .eq(ObjectUtil.isNotEmpty(invitationCodeListBo.getUseStatus()), InvitationCodeEntity::getUseStatus, invitationCodeListBo.getUseStatus())
                .eq(ObjectUtil.isNotEmpty(invitationCodeListBo.getStatus()), InvitationCodeEntity::getStatus, invitationCodeListBo.getStatus())
                .notIn(ObjectUtil.isNotEmpty(invitationCodeListBo.getNotCodeBatchIds()), InvitationCodeEntity::getBatchId, invitationCodeListBo.getNotCodeBatchIds())
                ;

        IPage<InvitationCodeEntity> iPage = invitationCodeService.page(new Query<InvitationCodeEntity>().getPage(invitationCodeListBo.getPage(), invitationCodeListBo.getLimit()), wrapper);

        PageUtils<InvitationCodeListVo> pageUtils = new PageUtils<>(invitationCodeListBo.getPage(), invitationCodeListBo.getLimit(), iPage);

        List<InvitationCodeEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<InvitationCodeListVo> vos = records.stream().map(item -> {
                InvitationCodeListVo invitationCodeVo = new InvitationCodeListVo();
                BeanUtils.copyProperties(item, invitationCodeVo);
                return invitationCodeVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public InvitationCodeInfoVo info(Long id) {

        InvitationCodeEntity invitationCodeEntity = invitationCodeService.getById(id);
        if(invitationCodeEntity != null) {
            InvitationCodeInfoVo invitationCodeInfoVo = new InvitationCodeInfoVo();
            BeanUtils.copyProperties(invitationCodeEntity, invitationCodeInfoVo);
            return invitationCodeInfoVo;
        }

        return null;
    }

    /**
     * 新增邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
     public InvitationCodeInfoVo save(InvitationCodeBo invitationCodeBo) {

         InvitationCodeEntity invitationCodeEntity = new InvitationCodeEntity();
         BeanUtils.copyProperties(invitationCodeBo, invitationCodeEntity);
         invitationCodeEntity.setId(SnowflakeManager.nextValue());
         invitationCodeEntity.setCreateDate(new Date());
         invitationCodeEntity.setUpdateDate(new Date());

         invitationCodeService.save(invitationCodeEntity);

         InvitationCodeInfoVo invitationCodeInfoVo = new InvitationCodeInfoVo();
         BeanUtils.copyProperties(invitationCodeEntity, invitationCodeInfoVo);

         return invitationCodeInfoVo;
     }

    /**
     * 修改邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    public void update(InvitationCodeBo invitationCodeBo) {

        InvitationCodeEntity invitationCodeEntity = new InvitationCodeEntity();
        BeanUtils.copyProperties(invitationCodeBo, invitationCodeEntity);
        invitationCodeEntity.setUpdateDate(new Date());

        invitationCodeService.updateById(invitationCodeEntity);
    }

    /**
     * 删除邀请码
     * @param id 邀请码id
     * @return
     */
    public void deleteById(Long id) {

        invitationCodeService.removeById(id);
    }

    @Override
    public void saveBatch(List<InvitationCodeBo> invitationCodeBos) {

        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";

        if(invitationCodeBos != null && invitationCodeBos.size() > 0) {
            List<InvitationCodeEntity> invitationCodeEntities = invitationCodeBos.stream().map(item -> {
                InvitationCodeEntity invitationCodeEntity = new InvitationCodeEntity();
                BeanUtils.copyProperties(item, invitationCodeEntity);
                invitationCodeEntity.setId(SnowflakeManager.nextValue());
                StringBuilder sb = new StringBuilder(8);
                Random random = new Random();
                for (int i = 0; i < 8; i++) {
                    int index = random.nextInt(chars.length());
                    sb.append(chars.charAt(index));
                }
                invitationCodeEntity.setCode(sb.toString());
                invitationCodeEntity.setUseStatus(0);
                invitationCodeEntity.setCreateDate(new Date());
                invitationCodeEntity.setUpdateDate(new Date());

                return invitationCodeEntity;
            }).toList();

            this.invitationCodeService.saveBatch(invitationCodeEntities);
        }
    }

    @Override
    public List<InvitationCodeInfoVo> listByCodeBatchId(Long codeBatchId) {
        List<InvitationCodeEntity> invitationCodeEntities = this.invitationCodeService.list(new QueryWrapper<InvitationCodeEntity>().eq("batch_id", codeBatchId));
        if(invitationCodeEntities != null && invitationCodeEntities.size() > 0) {
            List<InvitationCodeInfoVo> invitationCodeInfoVos = invitationCodeEntities.stream().map(item -> {
                InvitationCodeInfoVo invitationCodeInfoVo = new InvitationCodeInfoVo();
                BeanUtils.copyProperties(item, invitationCodeInfoVo);
                return invitationCodeInfoVo;
            }).toList();

            return invitationCodeInfoVos;
        }
        return null;
    }

    @Override
    public List<InvitationCodeInfoVo> listByCodeBatchIds(List<Long> codeBatchIds) {

        List<InvitationCodeEntity> invitationCodeEntities = this.invitationCodeService.list(new QueryWrapper<InvitationCodeEntity>()
                .in("batch_id", codeBatchIds));
        if (invitationCodeEntities != null && !invitationCodeEntities.isEmpty()) {
            return BeanUtil.copyToList(invitationCodeEntities, InvitationCodeInfoVo.class);
        }

        return null;
    }

    @Override
    public InvitationCodeInfoVo infoByCode(String invitationCode) {
        InvitationCodeEntity invitationCodeEntity = this.invitationCodeService.getOne(new QueryWrapper<InvitationCodeEntity>().eq("code", invitationCode));
        if(invitationCodeEntity != null) {
            InvitationCodeInfoVo invitationCodeInfoVo = new InvitationCodeInfoVo();
            BeanUtils.copyProperties(invitationCodeEntity, invitationCodeInfoVo);
            return invitationCodeInfoVo;
        }
        return null;
    }

    @Override
    public List<InvitationCodeInfoVo> listByUserId(Long userId) {
        List<InvitationCodeEntity> invitationCodeEntities = this.invitationCodeService.list(new QueryWrapper<InvitationCodeEntity>().eq("user_id", userId));
        if(invitationCodeEntities != null && invitationCodeEntities.size() > 0) {
            List<InvitationCodeInfoVo> invitationCodeInfoVos = invitationCodeEntities.stream().map(item -> {
                InvitationCodeInfoVo invitationCodeInfoVo = new InvitationCodeInfoVo();
                BeanUtils.copyProperties(item, invitationCodeInfoVo);
                return invitationCodeInfoVo;
            }).toList();

            return invitationCodeInfoVos;
        }
        return null;
    }

    @Override
    public void useInvitation(Long userId, Long orderId, Long codeId) {
        InvitationCodeEntity invitationCodeEntity = this.invitationCodeService.getById(codeId);
        if(invitationCodeEntity != null) {
            invitationCodeEntity.setUserId(userId);
            invitationCodeEntity.setUseDate(new Date());
            invitationCodeEntity.setOrderId(orderId);
            invitationCodeEntity.setUseStatus(1);
            this.invitationCodeService.updateById(invitationCodeEntity);
        }
    }


    /**
     * 邀请码导出
     * @param invitationCodeBo
     * @return
     */
    @Override
    public R<List<InvitationCodeListVo>> exportInvitation(List<InvitationCodeBo> invitationCodeBo) {

        List<InvitationCodeListVo> voList;

        voList = invitationCodeBo.stream().map(item ->{
            InvitationCodeListVo vo = new InvitationCodeListVo();
            BeanUtils.copyProperties(item, vo);
            InvitationCodeEntity entity = new InvitationCodeEntity();
            BeanUtils.copyProperties(item,entity);
            entity.setIsLssued(1);
            this.invitationCodeService.updateById(entity);
            InvitationCodeBatchEntity batchById = this.invitationCodeBatchService.getById(item.getBatchId());
            vo.setPackageName(batchById == null ? "" :batchById.getCommodityName());
            return vo;
        }).toList();

        return R.ok("导出成功",voList);
    }

    @Override
    public void updateIsLssued(List<Long> ids) {
        if (ObjectUtil.isNotEmpty(ids)){
            this.invitationCodeService.update(new LambdaUpdateWrapper<InvitationCodeEntity>()
                    .in(InvitationCodeEntity::getId, ids)
                    .set(InvitationCodeEntity::getIsLssued, 1)
            );
        }
    }

    @Override
    public List<InvitationCodeBatchListVo> codeCountByBatchId(List<Long> batchIds) {
        List<Map<String, Object>> list = this.invitationCodeService.listMaps(new QueryWrapper<InvitationCodeEntity>()
                .select("batch_id as id",
                        "count(1) as code_count",
                        "count(if(use_status = 1, 1, null)) as use_code_count",
                        "count(if(use_status is null or use_status = 0, 1, null)) as not_use_code_count")
                .lambda()
                .in(InvitationCodeEntity::getBatchId, batchIds)
                .groupBy(InvitationCodeEntity::getBatchId)
        );
        if (ObjectUtil.isNotEmpty(list)) {
            return BeanUtil.copyToList(list, InvitationCodeBatchListVo.class);
        }
        return List.of();
    }
}

