package com.jiuyu.replay.generic.feign.order;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.PackageInfoVo;
import com.jiuyu.replay.generic.vo.order.PackageVo;

import java.util.List;

public interface PackageFeign {

    /**
     * 获取所有版本套餐
     * @param packageType 套餐类型：1主要套餐，2次要套餐
     * @return
     */
    R<List<PackageInfoVo>> listAll(Integer packageType);

    /**
     * 获取单个版本套餐
     *
     * @param packageType 套餐类型：1主要套餐，2 次要套餐
     * @return 版本列表
     */
    List<PackageVo> listSingleAll(Integer packageType);
}
