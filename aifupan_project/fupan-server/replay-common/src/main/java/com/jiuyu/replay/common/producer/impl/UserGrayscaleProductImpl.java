package com.jiuyu.replay.common.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleBo;
import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleListBo;
import com.jiuyu.replay.common.entity.UserGrayscaleEntity;
import com.jiuyu.replay.common.producer.UserGrayscaleProduct;
import com.jiuyu.replay.common.repository.service.UserGrayscaleService;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.userGrayscale.UserGrayscaleVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 17:02
 */
@Service
@Slf4j
@AllArgsConstructor
public class UserGrayscaleProductImpl implements UserGrayscaleProduct {

    private final UserGrayscaleService userGrayscaleService;

    @Override
    public PageUtils<UserGrayscaleVo> queryPage(UserGrayscaleListBo listBo) {

        LambdaQueryWrapper<UserGrayscaleEntity> wrapper = new LambdaQueryWrapper<UserGrayscaleEntity>()
                .eq(ObjectUtil.isNotEmpty(listBo.getUserId()), UserGrayscaleEntity::getUserId, listBo.getUserId())
                .eq(ObjectUtil.isNotEmpty(listBo.getVersionId()), UserGrayscaleEntity::getVersionId, listBo.getVersionId())
                .like(ObjectUtil.isNotEmpty(listBo.getPhone()), UserGrayscaleEntity::getPhone, listBo.getPhone())
                .like(ObjectUtil.isNotEmpty(listBo.getNickName()), UserGrayscaleEntity::getNickName, listBo.getNickName())
                .and(ObjectUtil.isNotEmpty(listBo.getKeyword()), w ->
                        w.like(UserGrayscaleEntity::getNickName, listBo.getKeyword()).or().like(UserGrayscaleEntity::getPhone, listBo.getKeyword())
                );

        IPage<UserGrayscaleEntity> iPage = userGrayscaleService.page(new Query<UserGrayscaleEntity>().getPageNoSort(listBo.getPage(), listBo.getLimit()), wrapper);

        PageUtils<UserGrayscaleVo> utils = new PageUtils<>(listBo.getPage(), listBo.getLimit(), iPage);
        if (ObjectUtil.isNotEmpty(iPage.getRecords())) {
            utils.setList(BeanUtil.copyToList(iPage.getRecords(), UserGrayscaleVo.class));
        }
        return utils;
    }

    @Override
    public void adds(List<UserGrayscaleBo> bos) {
        if (ObjectUtil.isEmpty(bos)) {
            log.warn("批量添加用户灰度记录，参数为空");
            return;
        }

        // 提取所有的用户ID和版本ID
        List<Long> userIds = bos.stream().map(UserGrayscaleBo::getUserId).distinct().collect(Collectors.toList());
        List<Long> versionIds = bos.stream().map(UserGrayscaleBo::getVersionId).distinct().collect(Collectors.toList());

        // 判断对应版本中是否已经添加过该用户
        LambdaQueryWrapper<UserGrayscaleEntity> wrapper = new LambdaQueryWrapper<UserGrayscaleEntity>()
                .select(UserGrayscaleEntity::getVersionId, UserGrayscaleEntity::getUserId)
                .in(UserGrayscaleEntity::getUserId, userIds)
                .in(UserGrayscaleEntity::getVersionId, versionIds);
        List<UserGrayscaleEntity> existList = userGrayscaleService.list(wrapper);

        // 构建已存在的用户版本映射 key: userId_versionId
        Map<String, UserGrayscaleEntity> existMap = existList.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getUserId() + "_" + entity.getVersionId(),
                        entity -> entity,
                        (existing, replacement) -> existing
                ));

        // 把已经添加的用户从bos中移除，只保留需要新增的
        List<UserGrayscaleBo> toAddBos = bos.stream()
                .filter(bo -> !existMap.containsKey(bo.getUserId() + "_" + bo.getVersionId()))
                .collect(Collectors.toList());

        if (ObjectUtil.isEmpty(toAddBos)) {
            log.info("所有用户灰度记录已存在，无需添加");
            return;
        }

        // 批量构建UserGrayscaleEntity
        List<UserGrayscaleEntity> entityList = new ArrayList<>();
        Date currentDate = new Date();

        for (UserGrayscaleBo bo : toAddBos) {
            UserGrayscaleEntity entity = BeanUtil.copyProperties(bo, UserGrayscaleEntity.class);
            entity.setId(SnowflakeManager.nextValue());
            entity.setCreateDate(currentDate);
            entity.setUpdateDate(currentDate);
            entityList.add(entity);
        }

        // 批量添加
        if (!entityList.isEmpty()) {
            userGrayscaleService.saveBatch(entityList);
            log.info("批量添加用户灰度记录成功，数量: {}", entityList.size());
        }
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            log.warn("批量删除用户灰度记录，参数为空");
            return;
        }

        boolean result = userGrayscaleService.removeByIds(ids);
        if (result) {
            log.info("批量删除用户灰度记录成功，数量: {}", ids.size());
        } else {
            log.warn("批量删除用户灰度记录失败，ids: {}", ids);
        }
    }
}
