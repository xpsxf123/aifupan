package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.PackageUserListBo;
import com.jiuyu.replay.order.entity.PackageUserEntity;
import com.jiuyu.replay.order.producer.PackageUserProducer;
import com.jiuyu.replay.order.repository.dao.PackageUserDao;
import com.jiuyu.replay.order.repository.service.PackageUserService;
import com.jiuyu.replay.order.vo.PackageUserVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义版本用户关联
 */
@Service
@Slf4j
@AllArgsConstructor
public class PackageUserProducerImpl implements PackageUserProducer {

    private final PackageUserService packageUserService;
    private final PackageUserDao packageUserDao;

    @Override
    public PageUtils<PackageUserVo> queryPage(PackageUserListBo listBo) {
        Page<PackageUserVo> page = new Page<>(listBo.getPage(), listBo.getLimit());
        IPage<PackageUserVo> iPage = packageUserDao.queryPage(page, listBo.getPackageId(), listBo.getKeyword());

        PageUtils<PackageUserVo> utils = new PageUtils<>(listBo.getPage(), listBo.getLimit(), iPage);
        if (ObjectUtil.isNotEmpty(iPage.getRecords())) {
            utils.setList(iPage.getRecords());
        }
        return utils;
    }

    @Override
    public void saveBatch(List<PackageUserEntity> entities) {
        if (ObjectUtil.isEmpty(entities)) {
            return;
        }
        packageUserService.saveBatch(entities);
        log.info("批量添加自定义版本用户成功，数量: {}", entities.size());
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        packageUserService.removeByIds(ids);
        log.info("批量删除自定义版本用户成功，数量: {}", ids.size());
    }

    @Override
    public List<PackageUserVo> listByPackageIdAndUserIds(Long packageId, List<Long> userIds) {
        List<PackageUserEntity> list = packageUserService.list(new LambdaQueryWrapper<PackageUserEntity>()
                .eq(PackageUserEntity::getIsDeleted, 0)
                .eq(PackageUserEntity::getPackageId, packageId)
                .in(PackageUserEntity::getUserId, userIds)
        );
        return BeanUtil.copyToList(list, PackageUserVo.class);
    }

    @Override
    public List<PackageUserVo> listByPackageIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return List.of();
        }
        List<PackageUserEntity> list = packageUserService.lambdaQuery()
                .in(PackageUserEntity::getPackageId, ids)
                .list();
        return BeanUtil.copyToList(list, PackageUserVo.class);
    }
}
