package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.bo.BindingAccountBo;
import com.jiuyu.replay.power.bo.BindingAccountListBo;
import com.jiuyu.replay.power.entity.BindingAccountEntity;
import com.jiuyu.replay.power.producer.BindingAccountProducer;
import com.jiuyu.replay.power.repository.service.BindingAccountService;
import com.jiuyu.replay.power.vo.BindingAccountInfoVo;
import com.jiuyu.replay.power.vo.BindingAccountListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 父子绑定记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Service
public class BindingAccountProducerImpl implements BindingAccountProducer {

    @Resource
    private BindingAccountService bindingAccountService;


    @Override
    public PageUtils<BindingAccountListVo> queryPage(BindingAccountListBo bindingAccountListBo) {
        LambdaQueryWrapper<BindingAccountEntity> wrapper = new LambdaQueryWrapper<BindingAccountEntity>()
                .eq(ObjectUtil.isNotEmpty(bindingAccountListBo.getParentUserId()), BindingAccountEntity::getParentUserId, bindingAccountListBo.getParentUserId())
                .eq(ObjectUtil.isNotEmpty(bindingAccountListBo.getChildUserId()), BindingAccountEntity::getChildUserId, bindingAccountListBo.getChildUserId())
                .eq(ObjectUtil.isNotEmpty(bindingAccountListBo.getBindingStatus()), BindingAccountEntity::getBindingStatus, bindingAccountListBo.getBindingStatus())
                .in(ObjectUtil.isNotEmpty(bindingAccountListBo.getChildUserIds()), BindingAccountEntity::getChildUserId, bindingAccountListBo.getChildUserIds())
                ;

        IPage<BindingAccountEntity> iPage = bindingAccountService.page(new Query<BindingAccountEntity>()
                .getPage(bindingAccountListBo.getPage(), bindingAccountListBo.getLimit(), "binding_date"),
                wrapper);

        PageUtils<BindingAccountListVo> pageUtils = new PageUtils<>(bindingAccountListBo.getPage(), bindingAccountListBo.getLimit(), iPage);

        List<BindingAccountEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<BindingAccountListVo> vos = records.stream().map(item -> {
                BindingAccountListVo bindingAccountVo = new BindingAccountListVo();
                BeanUtils.copyProperties(item, bindingAccountVo);
                return bindingAccountVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public BindingAccountInfoVo info(Long id) {

        BindingAccountEntity bindingAccountEntity = bindingAccountService.getById(id);
        if(bindingAccountEntity != null) {
            BindingAccountInfoVo bindingAccountInfoVo = new BindingAccountInfoVo();
            BeanUtils.copyProperties(bindingAccountEntity, bindingAccountInfoVo);
            return bindingAccountInfoVo;
        }

        return null;
    }

    /**
     * 新增父子绑定记录
     * @param bindingAccountBo 父子绑定记录对象
     * @return
     */
     public BindingAccountInfoVo save(BindingAccountBo bindingAccountBo) {

         BindingAccountEntity bindingAccountEntity = new BindingAccountEntity();
         BeanUtils.copyProperties(bindingAccountBo, bindingAccountEntity);
         bindingAccountEntity.setId(SnowflakeManager.nextValue());
         bindingAccountEntity.setCreateDate(new Date());

         bindingAccountService.save(bindingAccountEntity);

         BindingAccountInfoVo bindingAccountInfoVo = new BindingAccountInfoVo();
         BeanUtils.copyProperties(bindingAccountEntity, bindingAccountInfoVo);

         return bindingAccountInfoVo;
     }

    /**
     * 修改父子绑定记录
     * @param bindingAccountBo 父子绑定记录对象
     * @return
     */
    public void update(BindingAccountBo bindingAccountBo) {

        BindingAccountEntity bindingAccountEntity = new BindingAccountEntity();
        BeanUtils.copyProperties(bindingAccountBo, bindingAccountEntity);

        bindingAccountService.updateById(bindingAccountEntity);
    }

    /**
     * 删除父子绑定记录
     * @param id 父子绑定记录id
     * @return
     */
    public void deleteById(Long id) {

        bindingAccountService.removeById(id);
    }

    @Override
    public void unbindingSubAccount(Long currentUserId, Long subUserId, String unbindReason) {
        bindingAccountService.update(new LambdaUpdateWrapper<BindingAccountEntity>()
                .eq(BindingAccountEntity::getParentUserId, currentUserId)
                .eq(BindingAccountEntity::getChildUserId, subUserId)
                .set(BindingAccountEntity::getBindingStatus, 1)
                .set(BindingAccountEntity::getUnbindReason, unbindReason)
                .set(BindingAccountEntity::getUnbindDate, new Date())
        );
    }

    @Override
    public List<com.jiuyu.replay.power.vo.BindingAccountVo> listByParentUserIds(java.util.Collection<Long> parentUserIds) {
        if (ObjectUtil.isEmpty(parentUserIds)) {
            return List.of();
        }
        List<BindingAccountEntity> records = bindingAccountService.lambdaQuery()
                .in(BindingAccountEntity::getParentUserId, parentUserIds)
                .list();
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream().map(item -> {
            com.jiuyu.replay.power.vo.BindingAccountVo vo = new com.jiuyu.replay.power.vo.BindingAccountVo();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}

