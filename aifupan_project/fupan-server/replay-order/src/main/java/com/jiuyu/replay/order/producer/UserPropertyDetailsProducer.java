package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.order.bo.UserPropertyDetailsList;
import com.jiuyu.replay.order.vo.UserPropertyDetailsListVo;
import com.jiuyu.replay.order.vo.UserPropertyDetailsInfoVo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsListBo;

import java.util.Date;
import java.util.List;


/**
 * 用户资产消费记录表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface UserPropertyDetailsProducer {


    /**
     * 用户资产消费记录表列表
     * @param userPropertyDetailsListBo 用户资产消费记录表列表查询参数
     * @return
     */
    PageUtils<UserPropertyDetailsListVo> queryPage(UserPropertyDetailsListBo userPropertyDetailsListBo);

    /**
    * 用户资产消费记录表信息
    * @param id 用户资产消费记录表id
    * @return
    */
    UserPropertyDetailsInfoVo info(Long id);

    /**
     * 新增用户资产消费记录表
     * @param userPropertyDetailsBo 用户资产消费记录表对象
     * @return
     */
     UserPropertyDetailsInfoVo save(UserPropertyDetailsBo userPropertyDetailsBo);

    /**
     * 修改用户资产消费记录表
     * @param userPropertyDetailsBo 用户资产消费记录表对象
     * @return
     */
    void update(UserPropertyDetailsBo userPropertyDetailsBo);

    /**
     * 删除用户资产消费记录表
     * @param id 用户资产消费记录表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 批量更新用户资产消费记录表
     * @param updateList
     */
    void saveBatch(List<UserPropertyDetailsBo> updateList);

    /**
     * 查询用户资产消费记录表
     *
     * @param bo 查询参数
     * @return 列表
     */
    PageUtils<UserPropertyDetailsInfoVo> userPropertyDetails(UserPropertyDetailsList bo);


    /**
     * 统计租户的消耗总token数
     *
     * @param mainUserId      主用户ID
     * @param sinceCreateDate 创建时间
     */
    Long sumTotalTokensByTenant(long mainUserId, Date sinceCreateDate);
}

