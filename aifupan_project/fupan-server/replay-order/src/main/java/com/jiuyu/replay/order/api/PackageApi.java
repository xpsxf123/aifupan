package com.jiuyu.replay.order.api;

import com.jiuyu.replay.generic.feign.order.PackageFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.PackageInfoVo;
import com.jiuyu.replay.generic.vo.order.PackageVo;
import com.jiuyu.replay.order.bll.PackageBll;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PackageApi implements PackageFeign {

    @Resource
    private PackageBll packageBll;

    @Override
    public R<List<PackageInfoVo>> listAll(Integer packageType) {

        return this.packageBll.listAllActivity(packageType);

    }

    @Override
    public List<PackageVo> listSingleAll(Integer packageType) {
        return this.packageBll.listSingleAll(packageType);
    }
}
