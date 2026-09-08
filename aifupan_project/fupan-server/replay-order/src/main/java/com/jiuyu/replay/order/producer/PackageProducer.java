package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.order.bo.CanPurchasePackageBo;
import com.jiuyu.replay.order.vo.CurrentFreeVersionVo;
import com.jiuyu.replay.order.vo.PackageListVo;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.bo.PackageBo;
import com.jiuyu.replay.order.bo.PackageListBo;
import com.jiuyu.replay.order.vo.PackageVo;

import java.util.List;


/**
 * 套餐表(用户版本)
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
public interface PackageProducer {


    /**
     * 套餐表(用户版本)列表
     * @param packageListBo 套餐表(用户版本)列表查询参数
     * @return
     */
    PageUtils<PackageListVo> queryPage(PackageListBo packageListBo);

    /**
    * 套餐表(用户版本)信息
    * @param id 套餐表(用户版本)id
    * @return
    */
    PackageInfoVo info(Long id);

    /**
     * 获取版本信息，处理过图片
     *
     * @param id 版本id
     * @return 版本
     */
    PackageInfoVo infoDealsImg(Long id);

    /**
     * 套餐表(用户版本)信息
     * @param id
     * @return
     */
    PackageInfoVo detailById(Long id);

    /**
     * 新增套餐表(用户版本)
     * @param packageBo 套餐表(用户版本)对象
     * @return
     */
     PackageInfoVo save(PackageBo packageBo);

    /**
     * 修改套餐表(用户版本)
     * @param packageBo 套餐表(用户版本)对象
     * @return
     */
    void update(PackageBo packageBo);

    /**
     * 删除套餐表(用户版本)
     * @param id 套餐表(用户版本)id
     * @return
     */
    void deleteById(Long id);

    /**
     * 获取套餐表(用户版本)
     * @param packageBo
     * @return
     */
    PackageVo getOnt(PackageBo packageBo);

    /**
     * 可以购买的套餐列表
     * @param bo
     * @return
     */
    List<PackageInfoVo> canPurchasePackage(CanPurchasePackageBo bo);

    /**
     * 根据套餐id集合获取列表
     * @param packageIds 套餐id集合
     * @return
     */
    List<PackageInfoVo> listByIds(List<Long> packageIds);

    /**
     * 获取免费套餐
     * @return
     */
    PackageInfoVo getGratisPackage();

    /**
     * 获取初始化套餐
     * @return
     */
    PackageInfoVo getPackageInit();

    /**
     * 获取当前免费套餐资源
     * @return
     */
    CurrentFreeVersionVo currentFreeVersion();

    /**
     * 获取所有版本套餐
     * @param packageType 套餐类型：1主要套餐，2次要套餐
     * @return
     */
    List<PackageInfoVo> listAllActivity(Integer packageType);

    /**
     * 获取所有单版本套餐
     *
     * @param packageType 套餐类型：1主要套餐，2 次要套餐
     * @return 版本列表
     */
    List<com.jiuyu.replay.generic.vo.order.PackageVo> listSingleAll(Integer packageType);

    /**
     * 根据级别获取套餐
     *
     * @param level 级别
     * @return 套餐
     */
    PackageInfoVo infoByLevel(Integer level);

    /**
     * 校验级别
     *
     * @param packageBo 套餐对象
     */
    void checkMainLevel(PackageBo packageBo);
}

