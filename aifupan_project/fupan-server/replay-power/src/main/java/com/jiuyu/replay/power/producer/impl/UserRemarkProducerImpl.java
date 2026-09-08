package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.power.vo.UserRemarkListVo;
import com.jiuyu.replay.power.vo.UserRemarkInfoVo;
import com.jiuyu.replay.power.bo.UserRemarkBo;
import com.jiuyu.replay.power.bo.UserRemarkListBo;
import com.jiuyu.replay.power.repository.service.UserRemarkService;
import com.jiuyu.replay.power.repository.service.UserBusinessService;
import com.jiuyu.replay.power.repository.service.UserService;
import com.jiuyu.replay.power.entity.UserRemarkEntity;
import com.jiuyu.replay.power.entity.UserBusinessEntity;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.producer.UserRemarkProducer;
import com.jiuyu.replay.power.vo.LatestRemarkVo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 用户备注表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
@Service
public class UserRemarkProducerImpl implements UserRemarkProducer {

    @Resource
    private UserRemarkService userRemarkService;

    @Resource
    private UserBusinessService userBusinessService;

    @Resource
    private UserService userService;

    /**
     * 最大跟进记录内容长度
     */
    private static final int MAX_REMARK_LENGTH = 500;

    /**
     * IN 查询分批大小
     */
    private static final int IN_BATCH_SIZE = 500;

    @Override
    public PageUtils<UserRemarkListVo> queryPage(UserRemarkListBo userRemarkListBo) {
        QueryWrapper<UserRemarkEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(userRemarkListBo.getUserId())){
            wrapper.eq("user_id", userRemarkListBo.getUserId());
        }

        IPage<UserRemarkEntity> iPage = userRemarkService.page(new Query<UserRemarkEntity>().getPage(userRemarkListBo.getPage(), userRemarkListBo.getLimit()), wrapper);

        PageUtils<UserRemarkListVo> pageUtils = new PageUtils<>(userRemarkListBo.getPage(), userRemarkListBo.getLimit(), iPage);

        List<UserRemarkEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UserRemarkListVo> vos = records.stream().map(item -> {
                UserRemarkListVo userRemarkVo = new UserRemarkListVo();
                BeanUtils.copyProperties(item, userRemarkVo);
                return userRemarkVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }


    /**
     * 获取用户跟进记录详情
     * @param id 用户备注表id
     * @return
     */
    @Override
    public UserRemarkInfoVo info(Long id) {

        UserRemarkEntity userRemarkEntity = userRemarkService.getById(id);
        if(userRemarkEntity != null) {
            UserRemarkInfoVo userRemarkInfoVo = new UserRemarkInfoVo();
            BeanUtils.copyProperties(userRemarkEntity, userRemarkInfoVo);
            return userRemarkInfoVo;
        }

        return null;
    }

    /**
     * 保存用户跟进记录
     * @param userRemarkBo 用户备注表（跟进记录）对象
     * @return
     */
    @Override
    public void save(UserRemarkBo userRemarkBo) {
        //校验保存入参
        checkBo(userRemarkBo);

         UserRemarkEntity userRemarkEntity = new UserRemarkEntity();
         BeanUtils.copyProperties(userRemarkBo, userRemarkEntity);
         userRemarkEntity.setId(SnowflakeManager.nextValue());
         Date date = new Date();
         userRemarkEntity.setCreateDate(date);
         userRemarkEntity.setUpdateDate(date);
        //校验保存入参
        boolean save = userRemarkService.save(userRemarkEntity);
        if (!save){
            throw new BusinessException(StatusCode.SQL_EX.getCode(),"保存失败");
        }
        // 同步用户最新跟进记录到业务表
        syncLatestRemarkToBusiness(userRemarkBo.getUserId());
     }

     /**
     * 保存校验入参
     * @param userRemarkBo 用户备注表（跟进记录）对象
     */
    private void checkBo(UserRemarkBo userRemarkBo) {
        if(ObjectUtils.isEmpty(userRemarkBo)) {
            throw new BusinessException("请正确输入参数");
        }
        if(StringUtil.isNotBlank(userRemarkBo.getRemark())&& userRemarkBo.getRemark().length() > MAX_REMARK_LENGTH) {
            throw new BusinessException("跟进内容过长");
        }
        if (ObjectUtils.isEmpty(userRemarkBo.getFollowUpTime())) {
            throw new BusinessException("跟进时间不能为空");
        }

    }

    /**
     * 修改用户跟进记录
     * @param userRemarkBo 用户备注表（跟进记录）对象
     */
    @Override
    public void update(UserRemarkBo userRemarkBo) {
        checkBo(userRemarkBo);
        if (userRemarkBo.getId() == null) {
            throw new BusinessException("用户跟进记录ID不能为空");
        }

        UserRemarkEntity userRemarkEntity = new UserRemarkEntity();
        BeanUtils.copyProperties(userRemarkBo, userRemarkEntity);
        userRemarkEntity.setUpdateDate(new Date());
        //修改用户跟进记录
        boolean updated = userRemarkService.updateById(userRemarkEntity);
        if (!updated){
            throw new BusinessException(StatusCode.SQL_EX.getCode(),"更新失败");
        }
        // 同步用户最新跟进记录到业务表
        syncLatestRemarkToBusiness(userRemarkBo.getUserId());
    }

    @Override
    public void deleteById(Long id) {

        userRemarkService.removeById(id);
    }

    /**
     * 同步用户最新跟进记录到业务表
     *
     * @param userId 用户ID
     */
    @Override
    public void syncLatestRemarkToBusiness(Long userId) {
        if (userId == null) {
            return;
        }

        // 查询最大followUpTime的记录
        UserRemarkEntity latestRemark = userRemarkService.lambdaQuery()
                .eq(UserRemarkEntity::getUserId, userId)
                .orderByDesc(UserRemarkEntity::getFollowUpTime)
                .last("LIMIT 1")
                .one();

        if (latestRemark == null) {
            return;
        }

        // 冗余到UserBusiness表
        UserBusinessEntity business = userBusinessService.getById(userId);
        if (business == null) {
            business = new UserBusinessEntity();
            business.setCreateDate(new Date());
        }
        business.setUserId(userId);
        business.setAccordingStatus(latestRemark.getFollowStatus());
        business.setAccordingContent(latestRemark.getRemark());
        business.setAccordingDate(latestRemark.getFollowUpTime());
        business.setNextFolTime(latestRemark.getNextFolTime());
        business.setUpdateDate(new Date());

        userBusinessService.saveOrUpdate(business);
    }

    /**
     * 批量获取多个用户各自的最新一条跟进记录
     * 1 次批量 IN 读 tb_user_remark + 1 次批量 listByIds 解析跟进人姓名，不在循环内查库
     * tb_user_remark 物理无 tenant_id 列，故只按 user_id + is_deleted 过滤
     *
     * @param userIds 用户ID集合
     *
     * @return 用户ID → 最新跟进
     */
    @Override
    public Map<Long, LatestRemarkVo> batchLatestByUserIds(Collection<Long> userIds) {
        Map<Long, LatestRemarkVo> result = new HashMap<>();
        if (ObjectUtils.isEmpty(userIds)) {
            return result;
        }

        List<Long> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctUserIds.isEmpty()) {
            return result;
        }

        // 批量读跟进记录，按 create_date DESC, id DESC 排序
        List<UserRemarkEntity> remarkList = new ArrayList<>();
        for (List<Long> batchUserIds : CollUtil.split(distinctUserIds, IN_BATCH_SIZE)) {
            remarkList.addAll(userRemarkService.list(new LambdaQueryWrapper<UserRemarkEntity>()
                    .eq(UserRemarkEntity::getIsDeleted, 0)
                    .in(UserRemarkEntity::getUserId, batchUserIds)
                    .orderByDesc(UserRemarkEntity::getCreateDate)
                    .orderByDesc(UserRemarkEntity::getId)));
        }
        if (remarkList.isEmpty()) {
            return result;
        }

        // 每个用户取首条即最新一条
        Map<Long, UserRemarkEntity> latestMap = remarkList.stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(UserRemarkEntity::getUserId, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));

        // 跟进人姓名：create_id 去重后一次批量解析 nickName
        Set<Long> createIds = latestMap.values().stream()
                .map(UserRemarkEntity::getCreateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nickNameMap = new HashMap<>();
        if (!createIds.isEmpty()) {
            for (List<Long> batchCreateIds : CollUtil.split(new ArrayList<>(createIds), IN_BATCH_SIZE)) {
                List<UserEntity> users = userService.listByIds(batchCreateIds);
                if (users != null) {
                    for (UserEntity user : users) {
                        nickNameMap.put(user.getId(), user.getNickName());
                    }
                }
            }
        }

        for (Map.Entry<Long, UserRemarkEntity> entry : latestMap.entrySet()) {
            UserRemarkEntity entity = entry.getValue();
            LatestRemarkVo vo = new LatestRemarkVo();
            vo.setRemark(entity.getRemark());
            vo.setRemarkTime(entity.getCreateDate());
            vo.setCreateName(nickNameMap.get(entity.getCreateId()));
            result.put(entry.getKey(), vo);
        }

        return result;
    }


}

