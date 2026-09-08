package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.power.SalesListVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesCascaderVo;
import com.jiuyu.replay.generic.bo.power.EmployeeStatusBo;
import com.jiuyu.replay.generic.bo.power.SalesBo;
import com.jiuyu.replay.generic.bo.power.SalesListBo;

import java.util.List;


/**
 * 用户跟进销售人员表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
public interface SalesLogic {


    /**
     * 用户跟进销售人员表列表
     * @param salesListBo 用户跟进销售人员表列表查询参数
     * @return
     */
    R<PageUtils<SalesListVo>> queryPage(SalesListBo salesListBo);

    /**
    * 用户跟进销售人员表信息
    * @param id 用户跟进销售人员表id
    * @return
    */
    R<SalesInfoVo> info(Long id);

    /**
     * 新增用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
    R<String> save(SalesBo salesBo);

    /**
     * 修改用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
    R<String> update(SalesBo salesBo);

    /**
     * 删除用户跟进销售人员表
     * @param id 用户跟进销售人员表id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取当前用户的销售人员信息
     * @return
     */
    R<SalesInfoVo> getCurrentUserSale();

    /**
     * 获取当前用户的手机号获取销售人员信息
     * @param phone
     * @return
     */
    R<SalesInfoVo> getCurrentUserSaleByPhone(String phone);

    /**
     * 用户列表销售搜索 - Cascader 级联选择器
     *
     * @return 销售列表
     */
    R<List<SalesCascaderVo>> userListSalesSearch(Integer employeeStatus);

    /**
     * 用户详情销售搜索 - 根据用户类型查询对应的销售
     *
     * @return 销售列表
     */
    R<List<SalesInfoVo>> userDetailsSalesSearch(Long userId);

    /**
     * 修改是否开启轮询
     *
     * @param salesBo 用户跟进销售人员表对象
     */
    void updateChooseStatus(SalesBo salesBo);

    /**
     * 修改员工状态
     *
     * @param employeeStatusBo 员工状态参数
     * @return
     */
    R<String> updateEmployeeStatus(EmployeeStatusBo employeeStatusBo);
}