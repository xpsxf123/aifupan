package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.bo.UserLoginLogBo;
import com.jiuyu.replay.power.bo.UserLoginLogListBo;
import com.jiuyu.replay.power.entity.UserLoginLogEntity;
import com.jiuyu.replay.power.producer.UserLoginLogProducer;
import com.jiuyu.replay.power.repository.service.UserLoginLogService;
import com.jiuyu.replay.power.vo.UserLoginLogInfoVo;
import com.jiuyu.replay.power.vo.UserLoginLogListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 用户登录日志
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Service
public class UserLoginLogProducerImpl implements UserLoginLogProducer {

    @Resource
    private UserLoginLogService userLoginLogService;
    @Resource
    private com.jiuyu.replay.power.repository.dao.UserLoginLogDao userLoginLogDao;


    @Override
    public PageUtils<UserLoginLogListVo> queryPage(UserLoginLogListBo userLoginLogListBo) {
        LambdaQueryWrapper<UserLoginLogEntity> wrapper = new LambdaQueryWrapper<UserLoginLogEntity>()
                .eq(ObjectUtil.isNotEmpty(userLoginLogListBo.getUserId()), UserLoginLogEntity::getUserId, userLoginLogListBo.getUserId())
                .like(ObjectUtil.isNotEmpty(userLoginLogListBo.getUserName()), UserLoginLogEntity::getUserName, userLoginLogListBo.getUserName())
                .eq(ObjectUtil.isNotEmpty(userLoginLogListBo.getUserType()), UserLoginLogEntity::getUserType, userLoginLogListBo.getUserType())
                .eq(ObjectUtil.isNotEmpty(userLoginLogListBo.getOperaType()), UserLoginLogEntity::getOperaType, userLoginLogListBo.getOperaType())
                .eq(ObjectUtil.isNotEmpty(userLoginLogListBo.getOperaStatus()), UserLoginLogEntity::getOperaStatus, userLoginLogListBo.getOperaStatus())
                .like(ObjectUtil.isNotEmpty(userLoginLogListBo.getIpAddress()), UserLoginLogEntity::getIpAddress, userLoginLogListBo.getIpAddress())
                ;

        IPage<UserLoginLogEntity> iPage = userLoginLogService.page(new Query<UserLoginLogEntity>().getPage(userLoginLogListBo.getPage(), userLoginLogListBo.getLimit()), wrapper);

        PageUtils<UserLoginLogListVo> pageUtils = new PageUtils<>(userLoginLogListBo.getPage(), userLoginLogListBo.getLimit(), iPage);

        List<UserLoginLogEntity> records = iPage.getRecords();
        if(records != null && !records.isEmpty()) {
            List<UserLoginLogListVo> vos = records.stream().map(item -> {
                UserLoginLogListVo userLoginLogVo = new UserLoginLogListVo();
                BeanUtils.copyProperties(item, userLoginLogVo);
                return userLoginLogVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserLoginLogInfoVo info(Long id) {

        UserLoginLogEntity userLoginLogEntity = userLoginLogService.getById(id);
        if(userLoginLogEntity != null) {
            UserLoginLogInfoVo userLoginLogInfoVo = new UserLoginLogInfoVo();
            BeanUtils.copyProperties(userLoginLogEntity, userLoginLogInfoVo);
            return userLoginLogInfoVo;
        }

        return null;
    }

    /**
     * 新增用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
     public UserLoginLogInfoVo save(UserLoginLogBo userLoginLogBo) {

         UserLoginLogEntity userLoginLogEntity = new UserLoginLogEntity();
         BeanUtils.copyProperties(userLoginLogBo, userLoginLogEntity);
         userLoginLogEntity.setId(SnowflakeManager.nextValue());
         userLoginLogEntity.setCreateDate(new Date());
         userLoginLogEntity.setUpdateDate(new Date());

         userLoginLogService.save(userLoginLogEntity);

         UserLoginLogInfoVo userLoginLogInfoVo = new UserLoginLogInfoVo();
         BeanUtils.copyProperties(userLoginLogEntity, userLoginLogInfoVo);

         return userLoginLogInfoVo;
     }

    /**
     * 修改用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    public void update(UserLoginLogBo userLoginLogBo) {

        UserLoginLogEntity userLoginLogEntity = new UserLoginLogEntity();
        BeanUtils.copyProperties(userLoginLogBo, userLoginLogEntity);
        userLoginLogEntity.setUpdateDate(new Date());

        userLoginLogService.updateById(userLoginLogEntity);
    }

    /**
     * 删除用户登录日志
     * @param id 用户登录日志id
     * @return
     */
    public void deleteById(Long id) {

        userLoginLogService.removeById(id);
    }

    @Override
    public List<com.jiuyu.replay.power.vo.datahub.DataHubLoginStatsVo> aggregateSuccessLogin(
            java.util.Collection<Long> userIds, Date startDate, Date endDate) {
        if (ObjectUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<com.jiuyu.replay.power.vo.datahub.DataHubLoginStatsVo> stats =
                userLoginLogDao.selectLoginStats(userIds, startDate, endDate);
        return stats == null ? List.of() : stats;
    }
}

