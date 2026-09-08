package com.jiuyu.replay.power.bll;

import cn.hutool.core.util.DesensitizedUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.TenantBo;
import com.jiuyu.replay.power.bo.TenantNormalUserListBo;
import com.jiuyu.replay.power.bo.TenantListBo;
import com.jiuyu.replay.power.bo.TenantSearchBO;
import com.jiuyu.replay.power.producer.TenantProducer;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.vo.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 租户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Component
public class TenantBll {

    @Resource
    private TenantProducer tenantProducer;
    @Resource
    private UserProducer userProducer;

    public R<TenantInfoVo> infoByUserId(Long userId) {

        TenantInfoVo tenantInfoVo = tenantProducer.infoByUserId(userId);
        return R.ok("获取成功", tenantInfoVo);
    }


    /**
     * 租户列表
     * @param tenantListBo 租户列表查询参数
     * @return
     */
    public R<PageUtils<TenantListVo>> queryPage(TenantListBo tenantListBo) {

        return R.ok("获取成功", tenantProducer.queryPage(tenantListBo));
    }

    /**
    * 租户信息
    * @param id 租户id
    * @return
    */
    public R<TenantInfoVo> info(Long id) {

        TenantInfoVo tenantInfoVo = tenantProducer.info(id);
        return R.ok("获取成功", tenantInfoVo);
    }

    /**
     * 新增租户
     * @param tenantBo 租户对象
     * @return
     */
    public R<String> save(TenantBo tenantBo) {

        TenantInfoVo tenantInfoVo = tenantProducer.save(tenantBo);
        return R.ok("添加成功");
    }

    /**
     * 修改租户
     * @param tenantBo 租户对象
     * @return
     */
    public R<String> update(TenantBo tenantBo) {

        tenantProducer.update(tenantBo);
        return R.ok("修改成功");
    }

    /**
     * 删除租户
     * @param id 租户id
     * @return
     */
    public R<String> delete(Long id) {

        tenantProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 解除租户绑定关系
     * @param parentUserId 解除者用户id
     * @param subUserId 被解除者用户id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> unbind(Long parentUserId, Long subUserId) {

        // 解除绑定关系
        Long tenantId = tenantProducer.unbind(parentUserId, subUserId);
        if(tenantId != null) {
            // 获取被解除者的用户信息
            UserInfoVo userInfoVo = this.userProducer.infoById(subUserId);
            if(tenantId.equals(userInfoVo.getActiveTenantId())) {
                // 当前激活的租户id是当前租户，修改激活的租户id
                TenantInfoVo tenantInfoVo = tenantProducer.infoByUserId(userInfoVo.getId());
                this.userProducer.updateActiveTenantId(userInfoVo.getId(), tenantInfoVo.getId());
                // 修改token
                this.userProducer.updateTenantIdRedisCacheById(userInfoVo.getId(), tenantInfoVo.getId());
            }
        }

        return R.ok("解除成功");
    }

    /**
     * 获取用户ids根据租户id
     *
     * @param tenantId 租户id
     * @return
     */
    public List<Long> listUserByTenantId(Long tenantId) {
        if (Objects.isNull(tenantId)) {
            return null;
        }
        return tenantProducer.listUserByTenantId(tenantId);
    }

    /**
     * 租户下拉选项
     *
     * @param tenantSearchBO 租户搜索
     *
     * @return {@link List }<{@link TenantOptionVo }>
     */
    public List<TenantOptionVo> selectOptions(TenantSearchBO tenantSearchBO) {
        return tenantProducer.selectOptions(tenantSearchBO);
    }

    public R<PageUtils<TenantListVo>> pageNormalUserTenants(TenantNormalUserListBo bo) {
        return R.ok("获取成功", tenantProducer.pageNormalUserTenants(bo));
    }


    /**
     * 获取租户的主账号信息
     *
     * @param tenantIds 租户id
     *
     * @return {@link Map }<{@link Long }, {@link TenantOptionVo }>
     */
    public Map<Long, TenantOptionVo> getTenantAccountMap(List<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return Map.of();
        }
        Map<Long, Long> tenantUserMap = tenantProducer.listTenantInfo(tenantIds).stream().collect(Collectors.toMap(TenantInfoVo::getId, TenantInfoVo::getUserId));
        if (EmptyUtil.isEmpty(tenantUserMap)) {
            return Map.of();
        }
        List<UserListVo> userList = userProducer.listByIds(tenantUserMap.values());
        if (EmptyUtil.isEmpty(userList)) {
            return Map.of();
        }
        Map<Long, UserListVo> userMap = userList.stream().collect(Collectors.toMap(UserListVo::getId, Function.identity()));
        userList = null;
        return tenantUserMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            TenantOptionVo tenantOptionVo = new TenantOptionVo();
            tenantOptionVo.setTenantId(entry.getKey());
            tenantOptionVo.setAccountName("");
            tenantOptionVo.setAccountMobile("");
            UserListVo tenantUser = userMap.get(entry.getValue());
            if (EmptyUtil.isNotEmpty(tenantUser)) {
                tenantOptionVo.setAccountName(tenantUser.getNickName());
                tenantOptionVo.setAccountMobile(DesensitizedUtil.mobilePhone(tenantUser.getPhone()));
            }
            return tenantOptionVo;
        }));
    }
}

