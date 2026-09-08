package com.jiuyu.replay.order.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.UserPropertyTypeBo;
import com.jiuyu.replay.order.bo.UserPropertyTypeListBo;
import com.jiuyu.replay.order.producer.UserPropertyTypeProducer;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeListVo;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 用户资产类型总明细
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Component
@AllArgsConstructor
public class UserPropertyTypeBll {

    private final UserPropertyTypeProducer userPropertyTypeProducer;
    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * 用户资产类型总明细列表
     * @param userPropertyTypeListBo 用户资产类型总明细列表查询参数
     * @return
     */
    public R<PageUtils<UserPropertyTypeListVo>> queryPage(UserPropertyTypeListBo userPropertyTypeListBo) {

        return R.ok("获取成功", userPropertyTypeProducer.queryPage(userPropertyTypeListBo));
    }

    /**
    * 用户资产类型总明细信息
    * @param id 用户资产类型总明细id
    * @return
    */
    public R<UserPropertyTypeInfoVo> info(Long id) {

        UserPropertyTypeInfoVo userPropertyTypeInfoVo = userPropertyTypeProducer.info(id);
        return R.ok("获取成功", userPropertyTypeInfoVo);
    }

    /**
     * 新增用户资产类型总明细
     * @param userPropertyTypeBo 用户资产类型总明细对象
     * @return
     */
    public R<String> save(UserPropertyTypeBo userPropertyTypeBo) {

        UserPropertyTypeInfoVo userPropertyTypeInfoVo = userPropertyTypeProducer.save(userPropertyTypeBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户资产类型总明细
     * @param userPropertyTypeBo 用户资产类型总明细对象
     * @return
     */
    public R<String> update(UserPropertyTypeBo userPropertyTypeBo) {

        userPropertyTypeProducer.update(userPropertyTypeBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户资产类型总明细
     * @param id 用户资产类型总明细id
     * @return
     */
    public R<String> delete(Long id) {

        userPropertyTypeProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 获取用户剩余资产-如果当前资产是子用户的，并且是公用的资源，就返回父用户的剩余资产
     * @param propertyId
     * @param commodityCode
     * @return
     */
    public R<UserPropertyTypeInfoVo> getUserSRemainingAssets(Long propertyId, String commodityCode) {
        return R.ok(userPropertyTypeProducer.getUserSRemainingAssets(propertyId, commodityCode));
    }


    public List<UserPropertyTypeListVo> clintGetTypeData(Long userId, Long propertyId) {
        return userPropertyTypeProducer.clintGetTypeData(userId,propertyId);
    }
}

