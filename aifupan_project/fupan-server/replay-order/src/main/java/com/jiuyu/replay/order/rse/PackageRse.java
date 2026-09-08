package com.jiuyu.replay.order.rse;

import com.jiuyu.replay.generic.vo.order.PackageInfoVo;

import java.util.List;

public interface PackageRse {

    /**
     * 获取所有版本套餐
     * @param packageType 套餐类型：1主要套餐，2次要套餐
     * @return
     */
    List<PackageInfoVo> listAllActivity(Integer packageType);
}
