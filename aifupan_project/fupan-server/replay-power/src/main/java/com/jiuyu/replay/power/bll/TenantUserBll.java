package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.TenantUserListVo;
import com.jiuyu.replay.power.vo.TenantUserInfoVo;
import com.jiuyu.replay.power.bo.TenantUserBo;
import com.jiuyu.replay.power.bo.TenantUserListBo;
import com.jiuyu.replay.power.producer.TenantUserProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 租户-用户-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Component
public class TenantUserBll {

    @Resource
    private TenantUserProducer tenantUserProducer;


    /**
     * 租户-用户-关联表列表
     * @param tenantUserListBo 租户-用户-关联表列表查询参数
     * @return
     */
    public R<PageUtils<TenantUserListVo>> queryPage(TenantUserListBo tenantUserListBo) {

        return R.ok("获取成功", tenantUserProducer.queryPage(tenantUserListBo));
    }

    /**
    * 租户-用户-关联表信息
    * @param id 租户-用户-关联表id
    * @return
    */
    public R<TenantUserInfoVo> info(Long id) {

        TenantUserInfoVo tenantUserInfoVo = tenantUserProducer.info(id);
        return R.ok("获取成功", tenantUserInfoVo);
    }

    /**
     * 新增租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
    public R<String> save(TenantUserBo tenantUserBo) {

        TenantUserInfoVo tenantUserInfoVo = tenantUserProducer.save(tenantUserBo);
        return R.ok("添加成功");
    }

    /**
     * 修改租户-用户-关联表
     * @param tenantUserBo 租户-用户-关联表对象
     * @return
     */
    public R<String> update(TenantUserBo tenantUserBo) {

        tenantUserProducer.update(tenantUserBo);
        return R.ok("修改成功");
    }

    /**
     * 删除租户-用户-关联表
     * @param id 租户-用户-关联表id
     * @return
     */
    public R<String> delete(Long id) {

        tenantUserProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

