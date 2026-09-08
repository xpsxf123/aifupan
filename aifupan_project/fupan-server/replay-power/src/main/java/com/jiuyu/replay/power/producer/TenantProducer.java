package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.vo.TenantListVo;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.bo.TenantBo;
import com.jiuyu.replay.power.bo.TenantListBo;
import com.jiuyu.replay.power.vo.TenantOptionVo;

import java.util.List;


/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
public interface TenantProducer {


    /**
     * 租户列表
     * @param tenantListBo 租户列表查询参数
     * @return
     */
    PageUtils<TenantListVo> queryPage(TenantListBo tenantListBo);

    /**
    * 租户信息
    * @param id 租户id
    * @return
    */
    TenantInfoVo info(Long id);

    /**
     * 新增租户
     * @param tenantBo 租户对象
     * @return
     */
     TenantInfoVo save(TenantBo tenantBo);

    /**
     * 修改租户
     * @param tenantBo 租户对象
     * @return
     */
    void update(TenantBo tenantBo);

    /**
     * 删除租户
     * @param id 租户id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据用户id获取租户
     * @param userId
     * @return
     */
    TenantInfoVo infoByUserId(Long userId);

    /**
     * 解除租户绑定关系
     * @param parentUserId 解除者用户id
     * @param subUserId 被解除者用户id
     * @return
     */
    Long unbind(Long parentUserId, Long subUserId);

    /**
     * 创建自己的租户
     *
     * @param userId
     * @param phone
     */
    void createMyTenant(Long userId, String phone);

    /**
     * 获取用户ids根据租户id
     *
     * @param tenantId 租户id
     * @return
     */
    List<Long> listUserByTenantId(Long tenantId);

    /**
     * 租户下拉选项
     *
     * @param tenantSearchBO 租户搜索
     *
     * @return {@link List }<{@link TenantOptionVo }>
     */
    List<TenantOptionVo> selectOptions(TenantSearchBO tenantSearchBO);

    /**
     * 获取租户信息
     *
     * @param tenantIds 租户id
     *
     * @return {@link List }<{@link TenantInfoVo }>
     */
    List<TenantInfoVo> listTenantInfo(List<Long> tenantIds);

    PageUtils<TenantListVo> pageNormalUserTenants(TenantNormalUserListBo bo);
}

