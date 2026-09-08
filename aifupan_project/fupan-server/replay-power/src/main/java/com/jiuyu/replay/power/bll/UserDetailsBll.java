package com.jiuyu.replay.power.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.UserDetailsBo;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.bo.UserUpdateBo;
import com.jiuyu.replay.power.producer.CompanyProducer;
import com.jiuyu.replay.power.producer.UserDetailsProducer;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.vo.CompanyVo;
import com.jiuyu.replay.power.vo.UserDetailsInfoVo;
import com.jiuyu.replay.power.vo.UserDetailsVo;
import com.jiuyu.replay.power.vo.UserVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 用户详情表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@Component
@AllArgsConstructor
public class UserDetailsBll {

    private final UserDetailsProducer userDetailsProducer;
    private final UserProducer userProducer;
    private final CompanyProducer companyProducer;


    /**
     * 客户端保存或修改用户详情接口
     * @param userDetailsBo
     */
    @Transactional
    public R<String> saveUserDetails(UserDetailsBo userDetailsBo) {
        UserUpdateBo updateBo = new UserUpdateBo();
        updateBo.setId(userDetailsBo.getUserId());
        updateBo.setNickName(userDetailsBo.getNickName());
        userProducer.update(updateBo);
        userDetailsProducer.saveUserDetails(userDetailsBo);
       return R.ok("修改成功");
    }
    /**
     * 服务端根据用户id查询用户详情
     * @param userId
     * @return
     */
    public R<UserDetailsInfoVo> userDetailByUserId(Long userId) {
        UserDetailsInfoVo info = userDetailsProducer.userDetailByUserId(userId);
        if (info == null){
            info = new UserDetailsInfoVo();
            info.setUserId(userId);
            info.setId(null);
        }else{
            info.setId(info.getId());
        }
        UserVo user = userProducer.getById(userId);
        if (user == null) RRException.create("用户信息查询失败");
        info.setParentId(user.getParentId());
        info.setUserType(user.getUserType());
        info.setUsername(user.getUsername());
        info.setNickName(user.getNickName());
        info.setPhone(user.getPhone());
        info.setRegisterDate(user.getCreateDate());
        info.setUserStatus(user.getStatus());
        info.setTenantId(user.getActiveTenantId());

        // 查询公司的信息
        if (ObjectUtil.isNotEmpty(info.getCompanyId())){
            CompanyVo info1 = companyProducer.info(info.getCompanyId());
            info.setCompany(info1);
        }
        return R.ok(info);
    }

    /**
     * 根据销售人员ID查询用户
     * @param salesId 销售人员ID
     * @return
     */
    public List<Long> getUserIdsBySalesId(Long salesId) {
        return userDetailsProducer.getUserIdsBySalesId(salesId);
    }

    /**
     * 查询当前渠道是否有绑定用户
     * @param id 渠道ID
     * @return
     */
    public Long getByChannelId(Long id) {
        return userDetailsProducer.getByChannelId(id);
    }

    /**
     * 查询当销售人员是否有绑定的用户
     * @param id 销售人员ID
     * @return
     */
    public Long countClientDetailBySaleId(Long id) {
        return userDetailsProducer.countClientDetailBySaleId(id);
    }

    /**
     * 根据微信名称查询用户
     * @param userWxName
     * @return
     */
    public List<Long> getUserByWxName(String userWxName) {
        return userDetailsProducer.getUserByWxName(userWxName);
    }

    /**
     * 根据用户id查询用户详情
     *
     * @param userIds
     * @return
     */
    public List<UserDetailsVo> listByUserIds(List<Long> userIds) {
        return userDetailsProducer.selectByUserIds(userIds);
    }

    /**
     * 根据用户id获取用户详情信息
     * @param userId 用户id
     * @return
     */
    public R<UserDetailsInfoVo> getByUserId(Long userId) {

        UserDetailsInfoVo userDetailsInfoVo = this.userDetailsProducer.getByUserId(userId);

        return R.ok(userDetailsInfoVo);
    }

    /**
     * 根据查询条件查询用户详情
     * 支持：根据用户意向、客户类型、是否登录 查询用户详情
     * @param userListBo
     * @return
     */
    public List<UserDetailsVo> selectByQuery(UserListBo userListBo) {
        return userDetailsProducer.selectByQuery(userListBo);
    }

    public List<UserDetailsVo> selectByUserIds(List<Long> longList) {
        return userDetailsProducer.selectByUserIds(longList);
    }
}
