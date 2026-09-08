package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.bo.PackageSaveBo;
import com.jiuyu.replay.order.vo.CurrentFreeVersionVo;
import com.jiuyu.replay.order.vo.PackageListVo;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.bo.PackageListBo;

import java.util.List;


/**
 * 套餐表(用户版本)
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
public interface PackageLogic {


    /**
     * 套餐表(用户版本)列表
     * @param packageListBo 套餐表(用户版本)列表查询参数
     * @return
     */
    R<PageUtils<PackageListVo>> queryPage(PackageListBo packageListBo);

    /**
    * 套餐表(用户版本)信息
    * @param id 套餐表(用户版本)id
    * @return
    */
    R<PackageInfoVo> info(Long id);

    /**
     * 新增套餐表(用户版本)
     * @param packageBo 套餐表(用户版本)对象
     * @return
     */
    R<String> saveOrUpdate(PackageSaveBo packageBo);

    /**
     * 删除套餐表(用户版本)
     * @param id 套餐表(用户版本)id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 可以购买的套餐列表
     * @param userId
     * @return
     */
    R<List<PackageInfoVo>> canPurchasePackage(Long userId);

    /**
     * 用户续费套餐列表
     * @param userId
     * @return
     */
    R<List<PackageInfoVo>> userRenewal(Long userId);

    /**
     * 获取用户可以购买的增量包
     * @param userId
     * @return
     */
    R<PackageInfoVo> incrementByPackageId(Long userId);

    /**
     * 官网版本列表
     * @return
     */
    R<List<PackageListVo>> websiteList(Long userId);

    /**
     * 同步版本
     * @param id
     * @return
     */
    R<String> synchronousPackage(Long id);

    /**
     * 当前免费版本
     * @return
     */
    R<CurrentFreeVersionVo> currentFreeVersion();

    /**
     * 试用版本列表
     *
     * @return 试用列表
     */
    R<List<PackageListVo>> trialVersionList(Long userId);
}

