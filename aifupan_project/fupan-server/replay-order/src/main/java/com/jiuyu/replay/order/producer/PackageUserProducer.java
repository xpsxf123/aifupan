package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.order.bo.PackageUserListBo;
import com.jiuyu.replay.order.entity.PackageUserEntity;
import com.jiuyu.replay.order.vo.PackageUserVo;

import java.util.List;

/**
 * 自定义版本用户关联
 */
public interface PackageUserProducer {

    /**
     * 分页查询
     *
     * @param listBo 查询参数
     * @return 分页数据
     */
    PageUtils<PackageUserVo> queryPage(PackageUserListBo listBo);

    /**
     * 批量添加
     *
     * @param entities 实体列表
     */
    void saveBatch(List<PackageUserEntity> entities);

    /**
     * 根据id集合删除
     *
     * @param ids id集合
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据packageId和userId列表查询已存在的记录
     *
     * @param packageId 版本id
     * @param userIds   用户id列表
     * @return 已存在的记录
     */
    List<PackageUserVo> listByPackageIdAndUserIds(Long packageId, List<Long> userIds);

    /**
     * 根据packageId列表查询
     *
     * @param ids packageId列表
     * @return 包含packageId列表的记录
     */
    List<PackageUserVo> listByPackageIds(List<Long> ids);
}
