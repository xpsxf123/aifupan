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
import com.jiuyu.replay.order.bo.InvitationCodeBatchBo;
import com.jiuyu.replay.order.bo.InvitationCodeBatchListBo;
import com.jiuyu.replay.order.entity.InvitationCodeBatchEntity;
import com.jiuyu.replay.order.entity.InvitationCodeEntity;
import com.jiuyu.replay.order.producer.InvitationCodeBatchProducer;
import com.jiuyu.replay.order.repository.service.InvitationCodeBatchService;
import com.jiuyu.replay.order.repository.service.InvitationCodeService;
import com.jiuyu.replay.order.repository.service.PackageService;
import com.jiuyu.replay.order.vo.InvitationCodeBatchInfoVo;
import com.jiuyu.replay.order.vo.InvitationCodeBatchListVo;
import com.jiuyu.replay.order.vo.InvitationCodeListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


/**
 * 邀请码-批次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Service
public class InvitationCodeBatchProducerImpl implements InvitationCodeBatchProducer {

    @Resource
    private InvitationCodeBatchService invitationCodeBatchService;
    @Resource
    private InvitationCodeService invitationCodeService;
    @Resource
    private PackageService packageService;


    @Override
    public PageUtils<InvitationCodeBatchListVo> queryPage(InvitationCodeBatchListBo invitationCodeBatchListBo) {
        LambdaQueryWrapper<InvitationCodeBatchEntity> wrapper = new LambdaQueryWrapper<InvitationCodeBatchEntity>()
                .eq(ObjectUtil.isNotEmpty(invitationCodeBatchListBo.getStatus()), InvitationCodeBatchEntity::getStatus, invitationCodeBatchListBo.getStatus())
                .eq(ObjectUtil.isNotEmpty(invitationCodeBatchListBo.getType()), InvitationCodeBatchEntity::getType, invitationCodeBatchListBo.getType())
                .eq(ObjectUtil.isNotEmpty(invitationCodeBatchListBo.getChannelId()), InvitationCodeBatchEntity::getChannelId, invitationCodeBatchListBo.getChannelId())
                .like(ObjectUtil.isNotEmpty(invitationCodeBatchListBo.getKeyword()), InvitationCodeBatchEntity::getName, invitationCodeBatchListBo.getKeyword())
                .notIn(ObjectUtil.isNotEmpty(invitationCodeBatchListBo.getNotTypeList()), InvitationCodeBatchEntity::getType, invitationCodeBatchListBo.getNotTypeList())

        ;

        IPage<InvitationCodeBatchEntity> iPage = invitationCodeBatchService.page(new Query<InvitationCodeBatchEntity>().getPage(invitationCodeBatchListBo.getPage(), invitationCodeBatchListBo.getLimit()), wrapper);

        PageUtils<InvitationCodeBatchListVo> pageUtils = new PageUtils<>(invitationCodeBatchListBo.getPage(), invitationCodeBatchListBo.getLimit(), iPage);

        List<InvitationCodeBatchEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            pageUtils.setList(BeanUtil.copyToList(records, InvitationCodeBatchListVo.class));
        }

        return pageUtils;
    }

    @Override
    public InvitationCodeBatchInfoVo info(Long id) {

        InvitationCodeBatchEntity invitationCodeBatchEntity = invitationCodeBatchService.getById(id);
        if (invitationCodeBatchEntity != null) {
            InvitationCodeBatchInfoVo invitationCodeBatchInfoVo = new InvitationCodeBatchInfoVo();
            BeanUtils.copyProperties(invitationCodeBatchEntity, invitationCodeBatchInfoVo);
            return invitationCodeBatchInfoVo;
        }

        return null;
    }

    /**
     * 新增邀请码-批次
     *
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    public InvitationCodeBatchInfoVo save(InvitationCodeBatchBo invitationCodeBatchBo) {

        InvitationCodeBatchEntity invitationCodeBatchEntity = new InvitationCodeBatchEntity();
        BeanUtils.copyProperties(invitationCodeBatchBo, invitationCodeBatchEntity);
        invitationCodeBatchEntity.setId(SnowflakeManager.nextValue());
        invitationCodeBatchEntity.setCreateDate(new Date());
        invitationCodeBatchEntity.setUpdateDate(new Date());

        invitationCodeBatchService.save(invitationCodeBatchEntity);

        InvitationCodeBatchInfoVo invitationCodeBatchInfoVo = new InvitationCodeBatchInfoVo();
        BeanUtils.copyProperties(invitationCodeBatchEntity, invitationCodeBatchInfoVo);

        return invitationCodeBatchInfoVo;
    }

    /**
     * 修改邀请码-批次
     *
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    public void update(InvitationCodeBatchBo invitationCodeBatchBo) {

        InvitationCodeBatchEntity invitationCodeBatchEntity = new InvitationCodeBatchEntity();
        BeanUtils.copyProperties(invitationCodeBatchBo, invitationCodeBatchEntity);
        invitationCodeBatchEntity.setUpdateDate(new Date());

        invitationCodeBatchService.updateById(invitationCodeBatchEntity);

        invitationCodeService.update(new LambdaUpdateWrapper<InvitationCodeEntity>()
                .eq(InvitationCodeEntity::getBatchId, invitationCodeBatchBo.getId())
                .set(InvitationCodeEntity::getValidityStartDate, invitationCodeBatchBo.getValidityStartDate())
                .set(InvitationCodeEntity::getValidityEndDate, invitationCodeBatchBo.getValidityEndDate())
        );
    }

    /**
     * 删除邀请码-批次
     *
     * @param id 邀请码-批次id
     * @return
     */
    public void deleteById(Long id) {

        // 删除邀请批次
        invitationCodeBatchService.removeById(id);

        invitationCodeService.remove(new LambdaQueryWrapper<InvitationCodeEntity>()
                .eq(InvitationCodeEntity::getBatchId, id)
        );
    }

    @Override
    public List<InvitationCodeBatchInfoVo> listByIds(Collection<Long> codeBatchIds) {
        List<InvitationCodeBatchEntity> invitationCodeBatchEntities = this.invitationCodeBatchService.listByIds(codeBatchIds);
        if (invitationCodeBatchEntities != null && invitationCodeBatchEntities.size() > 0) {
            List<InvitationCodeBatchInfoVo> invitationCodeBatchInfoVos = invitationCodeBatchEntities.stream().map(item -> {
                InvitationCodeBatchInfoVo invitationCodeBatchInfoVo = new InvitationCodeBatchInfoVo();
                BeanUtils.copyProperties(item, invitationCodeBatchInfoVo);
                return invitationCodeBatchInfoVo;
            }).toList();

            return invitationCodeBatchInfoVos;
        }

        return null;
    }

    /**
     * 根据邀请码批次导出该批次的所有邀请码
     *
     * @param batchId
     * @return
     */
    @Override
    public R<List<InvitationCodeListVo>> invitationByBatchId(Long batchId) {
        QueryWrapper<InvitationCodeEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batchId);
        List<InvitationCodeEntity> list = this.invitationCodeService.list(wrapper);
        List<InvitationCodeListVo> voList = new ArrayList<>();
        InvitationCodeBatchEntity batchEntityById = this.invitationCodeBatchService.getById(batchId);
        if (list != null && list.size() > 0) {
            voList = list.stream().map(item -> {
                InvitationCodeListVo vo = new InvitationCodeListVo();
                BeanUtils.copyProperties(item, vo);
                vo.setPackageName(batchEntityById.getCommodityName());
                item.setIsLssued(1);
                this.invitationCodeService.updateById(item);
                return vo;
            }).toList();
        }
        batchEntityById.setIsLssued(1);
        this.invitationCodeBatchService.updateById(batchEntityById);

        return R.ok("", voList);
    }


}

