package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.bo.UserDetailsBo;
import com.jiuyu.replay.power.bo.UserDetailsListBo;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.bo.crm.CrmUserDetailsSyncBo;
import com.jiuyu.replay.power.vo.UserDetailsInfoVo;
import com.jiuyu.replay.power.vo.UserDetailsListVo;
import com.jiuyu.replay.power.vo.UserDetailsVo;
import com.jiuyu.replay.power.vo.crm.CrmUserDetailsSyncVo;

import java.util.List;


/**
 * 用户详情表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
public interface UserDetailsProducer {


    /**
     * 用户详情表列表
     * @param userDetailsListBo 用户详情表列表查询参数
     * @return
     */
    PageUtils<UserDetailsListVo> queryPage(UserDetailsListBo userDetailsListBo);

    /**
    * 用户详情表信息
    * @param id 用户详情表id
    * @return
    */
    UserDetailsInfoVo info(Long id);

    /**
     * 新增用户详情表
     * @param userDetailsBo 用户详情表对象
     * @return
     */
     UserDetailsInfoVo save(UserDetailsBo userDetailsBo);

    /**
     * 修改用户详情表
     * @param userDetailsBo 用户详情表对象
     * @return
     */
    void update(UserDetailsBo userDetailsBo);

    /**
     * 删除用户详情表
     * @param id 用户详情表id
     * @return
     */
    void deleteById(Long id);


    /**
     * 客户端保存或修改用户详情接口
     *
     * @param userDetailsBo
     */
    void saveUserDetails(UserDetailsBo userDetailsBo);

    /**
     * 根据用户id查询用户详情
     * @param userId
     * @return
     */
    UserDetailsInfoVo userDetailByUserId(Long userId);

    /**
     * 根据销售人员ID查询用户
     * @param salesId 销售人员ID
     * @return
     */
    List<Long> getUserIdsBySalesId(Long salesId);

    /**
     * 查询当前渠道是否有绑定用户
     * @param id 渠道ID
     * @return
     */
    Long getByChannelId(Long id);

    /**
     * 查询当销售人员是否有绑定的用户
     * @param id 销售人员ID
     * @return
     */
    Long getBySaleId(Long id);

    /**
     * 根据微信名称查询用户
     * @param userWxName
     * @return
     */
    List<Long> getUserByWxName(String userWxName);

    /**
     * 根据用户id获取用户详情信息
     * @param userId 用户id
     * @return
     */
    UserDetailsInfoVo getByUserId(Long userId);

    /**
     * 根据查询条件查询用户详情
     * 支持：根据用户意向、客户类型、是否登录 查询用户详情
     * @param userListBo
     * @return
     */
    List<UserDetailsVo> selectByQuery(UserListBo userListBo);

    UserDetailsInfoVo getDetailByUserId(Long userId);

    List<UserDetailsVo> selectByUserIds(List<Long> longList);

    /**
     * 保存 CRM 结构化画像字段
     *
     * @param bo CRM 画像字段同步对象
     *
     * @return 同步结果
     */
    CrmUserDetailsSyncVo saveCrmProfileFields(CrmUserDetailsSyncBo bo);

    /**
     * 修改用户详情表登录状态
     * @param id
     * @return
     */
    boolean updateLoggedInStatus(Long id);

    /**
     * 根据销售人员id获取客户详情
     *
     * @param salesId 销售人员id
     * @return 数量
     */
    Long countClientDetailBySaleId(Long salesId);
}
