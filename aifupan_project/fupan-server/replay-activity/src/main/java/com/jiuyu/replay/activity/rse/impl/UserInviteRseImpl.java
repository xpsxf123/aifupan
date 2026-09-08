package com.jiuyu.replay.activity.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.activity.entity.UserInviteEntity;
import com.jiuyu.replay.activity.repository.service.UserInviteService;
import com.jiuyu.replay.activity.rse.UserInviteRse;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.activity.UserInviteBo;
import com.jiuyu.replay.generic.bo.activity.UserInviteListBo;
import com.jiuyu.replay.generic.vo.activity.UserInviteInfoVo;
import com.jiuyu.replay.generic.vo.activity.UserInviteListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 用户-邀请关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-27 15:15:49
 */
@Service
public class UserInviteRseImpl implements UserInviteRse {

    @Resource
    private UserInviteService userInviteService;


    @Override
    public PageUtils<UserInviteListVo> queryPage(UserInviteListBo userInviteListBo) {
        QueryWrapper<UserInviteEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(userInviteListBo.getKeyword())) {
            wrapper.like("name", userInviteListBo.getKeyword());
        }

        IPage<UserInviteEntity> iPage = userInviteService.page(new Query<UserInviteEntity>().getPage(userInviteListBo.getPage(), userInviteListBo.getLimit()), wrapper);

        PageUtils<UserInviteListVo> pageUtils = new PageUtils<>(userInviteListBo.getPage(), userInviteListBo.getLimit(), iPage);

        List<UserInviteEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<UserInviteListVo> vos = records.stream().map(item -> {
                UserInviteListVo userInviteVo = new UserInviteListVo();
                BeanUtils.copyProperties(item, userInviteVo);
                return userInviteVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserInviteInfoVo info(Long id) {

        UserInviteEntity userInviteEntity = userInviteService.getById(id);
        if (userInviteEntity != null) {
            UserInviteInfoVo userInviteInfoVo = new UserInviteInfoVo();
            BeanUtils.copyProperties(userInviteEntity, userInviteInfoVo);
            return userInviteInfoVo;
        }

        return null;
    }

    @Override
    public UserInviteInfoVo save(UserInviteBo userInviteBo) {

        UserInviteEntity userInviteEntity = new UserInviteEntity();
        BeanUtils.copyProperties(userInviteBo, userInviteEntity);
        userInviteEntity.setId(SnowflakeManager.nextValue());
        userInviteEntity.setCreateDate(new Date());
        userInviteEntity.setUpdateDate(new Date());
        userInviteEntity.setInviteStatus(1);

        userInviteService.save(userInviteEntity);

        UserInviteInfoVo userInviteInfoVo = new UserInviteInfoVo();
        BeanUtils.copyProperties(userInviteEntity, userInviteInfoVo);

        return userInviteInfoVo;
    }

    @Override
    public void update(UserInviteBo userInviteBo) {

        UserInviteEntity userInviteEntity = new UserInviteEntity();
        BeanUtils.copyProperties(userInviteBo, userInviteEntity);
        userInviteEntity.setUpdateDate(new Date());

        userInviteService.updateById(userInviteEntity);
    }

    @Override
    public void deleteById(Long id) {

        userInviteService.removeById(id);
    }

    @Override
    public List<UserInviteInfoVo> listByCode(String urlCode) {

        QueryWrapper<UserInviteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("invite_code", urlCode);
        wrapper.eq("invite_status", 1);

        List<UserInviteEntity> userInviteEntities = this.userInviteService.list(wrapper);
        if (userInviteEntities != null && userInviteEntities.size() > 0) {
            List<UserInviteInfoVo> userInviteInfoVos = userInviteEntities.stream().map(item -> {
                UserInviteInfoVo userInviteInfoVo = new UserInviteInfoVo();
                BeanUtils.copyProperties(item, userInviteInfoVo);
                return userInviteInfoVo;
            }).collect(Collectors.toList());

            return userInviteInfoVos;
        }
        return null;
    }

    @Override
    public Long getInviteUserNumberByInviteUserId(Long inviteUserId) {
        return userInviteService.count(new LambdaQueryWrapper<>(UserInviteEntity.class).eq(UserInviteEntity::getInviteUserId, inviteUserId).eq(UserInviteEntity::getInviteType, 2).eq(UserInviteEntity::getInviteStatus, 1));
    }

    @Override
    public UserInviteBo judgeUserInviteEffective(Long acceptUserId) {
        UserInviteEntity userInvite = userInviteService.getOne(new LambdaQueryWrapper<>(UserInviteEntity.class).eq(UserInviteEntity::getPassiveUserId, acceptUserId).eq(UserInviteEntity::getInviteStatus, 1).eq(UserInviteEntity::getInviteType, 2));
        return BeanConvertUtils.convert(userInvite, UserInviteBo.class);
    }
}

