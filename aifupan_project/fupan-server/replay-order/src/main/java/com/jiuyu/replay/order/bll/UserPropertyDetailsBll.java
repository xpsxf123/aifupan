package com.jiuyu.replay.order.bll;

import com.jiuyu.replay.generic.dto.order.UserResourceConsumptionDto;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.order.vo.UserPropertyDetailsListVo;
import com.jiuyu.replay.order.vo.UserPropertyDetailsInfoVo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsListBo;
import com.jiuyu.replay.order.producer.UserPropertyDetailsProducer;
import com.jiuyu.replay.order.rse.UserPropertyDetailsRse;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户资产消费记录表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Component
public class UserPropertyDetailsBll {

    @Resource
    private UserPropertyDetailsProducer userPropertyDetailsProducer;

    @Resource
    private UserPropertyDetailsRse userPropertyDetailsRse;

    /**
     * 用户资产消费记录表列表
     * 
     * @param userPropertyDetailsListBo 用户资产消费记录表列表查询参数
     * @return
     */
    public R<PageUtils<UserPropertyDetailsListVo>> queryPage(UserPropertyDetailsListBo userPropertyDetailsListBo) {

        return R.ok("获取成功", userPropertyDetailsProducer.queryPage(userPropertyDetailsListBo));
    }

    /**
     * 用户资产消费记录表信息
     * 
     * @param id 用户资产消费记录表id
     * @return
     */
    public R<UserPropertyDetailsInfoVo> info(Long id) {

        UserPropertyDetailsInfoVo userPropertyDetailsInfoVo = userPropertyDetailsProducer.info(id);
        return R.ok("获取成功", userPropertyDetailsInfoVo);
    }

    /**
     * 新增用户资产消费记录表
     * 
     * @param userPropertyDetailsBo 用户资产消费记录表对象
     * @return
     */
    public R<String> save(UserPropertyDetailsBo userPropertyDetailsBo) {

        UserPropertyDetailsInfoVo userPropertyDetailsInfoVo = userPropertyDetailsProducer.save(userPropertyDetailsBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户资产消费记录表
     * 
     * @param userPropertyDetailsBo 用户资产消费记录表对象
     * @return
     */
    public R<String> update(UserPropertyDetailsBo userPropertyDetailsBo) {

        userPropertyDetailsProducer.update(userPropertyDetailsBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户资产消费记录表
     *
     * @param id 用户资产消费记录表id
     * @return
     */
    public R<String> delete(Long id) {

        userPropertyDetailsProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取用户列表的昨日资源消耗统计
     *
     * @param userIds 用户ID列表
     * @param parentUserId 父id
     * @return 用户资源消耗统计列表
     */
    public List<UserResourceConsumptionDto> getYesterdayResourceConsumption(List<Long> userIds, Long parentUserId) {
        return userPropertyDetailsRse.getYesterdayResourceConsumption(userIds, parentUserId);
    }

    /**
     * 获取用户列表的本月资源消耗统计
     *
     * @param userIds 用户ID列表
     * @param parentUserId 父id
     * @return 用户资源消耗统计列表
     */
    public List<UserResourceConsumptionDto> getMonthlyResourceConsumption(List<Long> userIds, Long parentUserId) {
        return userPropertyDetailsRse.getMonthlyResourceConsumption(userIds, parentUserId);
    }

}
