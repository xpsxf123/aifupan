package com.jiuyu.replay.generic.feign.power;

import com.jiuyu.replay.generic.bo.power.SalesListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesListVo;

import java.util.List;

/**
 * @author lyw
 */
public interface SalesFeign {

    /**
     * 用户跟进销售人员表列表
     *
     * @param salesListBo 用户跟进销售人员表列表查询参数
     * @return
     */
    PageUtils<SalesListVo> queryPage(SalesListBo salesListBo);

    R<SalesInfoVo> exCant(Long salesId);

    R<List<Long>> exCantIds(List<Long> salesIds);

    /**
     * 根据用户id获取销售
     *
     * @param userId 用户id
     * @return 销售
     */
    SalesInfoVo getByUserId(Long userId);

    /**
     * 根据用户id获取销售
     *
     * @param userId 用户id
     * @return 销售
     */
    SalesInfoVo getBySalesUserId(Long userId);

    /**
     * 根据用户ids获取销售列表
     *
     * @param userIds 用户id
     * @return 销售列表
     */
    List<SalesInfoVo> listByUserIds(List<Long> userIds);

    /**
     * 根据代理商id和销售类型查询销售
     *
     * @param agentId     代理商id
     * @param salesType   销售类型
     * @param userPolling 是否轮询
     * @return 销售列表
     */
    List<SalesInfoVo> listByAndAgentSalesTypeId(Long agentId, Integer salesType, Integer userPolling);

    /**
     * 根据销售id获取销售
     *
     * @param saleId 销售id
     * @return 销售
     */
    SalesInfoVo getById(Long saleId);

    /**
     * 更新员工状态
     *
     * @param agentId        代理商id
     * @param employeeStatus 员工状态
     */
    void updateEmployeeStatusByAgentId(Long agentId, Integer employeeStatus);
}
