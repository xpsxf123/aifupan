package com.jiuyu.replay.power.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesListVo;
import com.jiuyu.replay.generic.bo.power.SalesBo;
import com.jiuyu.replay.generic.bo.power.SalesListBo;
import com.jiuyu.replay.power.entity.SalesEntity;

import java.util.Collection;
import java.util.List;


/**
 * 用户跟进销售人员表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
public interface SalesProducer {


    /**
     * 用户跟进销售人员表列表
     * @param salesListBo 用户跟进销售人员表列表查询参数
     * @return
     */
    PageUtils<SalesListVo> queryPage(SalesListBo salesListBo);

    /**
    * 用户跟进销售人员表信息
    * @param id 用户跟进销售人员表id
    * @return
    */
    SalesInfoVo info(Long id);

    /**
     * 新增用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
     SalesInfoVo save(SalesBo salesBo);

    /**
     * 修改用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
    void update(SalesBo salesBo);

    /**
     * 修改用户跟进销售人员表
     *
     * @param salesBo 用户跟进销售人员表对象
     */
    void updateAllField(SalesEntity salesBo);

    /**
     * 删除用户跟进销售人员表
     * @param id 用户跟进销售人员表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 获取所有的销售人员
     * @return
     */
    List<SalesEntity> listAll();

    /**
     * 根据销售人员id集合获取销售人员列表
     * @param saleIds 销售人员id集合
     * @return
     */
    List<SalesInfoVo> listByIds(Collection<Long> saleIds);

    /**
     * 获取轮询到的销售id
     * @return
     */
    Long getPollingSaleId();

    List<SalesInfoVo> selectBySalesIds(List<Long> list);

    List<SalesInfoVo> listByIdsFilterNoChoose(List<Long> salesIds);

    /**
     * 更具用户id获取销售
     *
     * @param userId 用户id
     * @return 销售
     */
    SalesInfoVo getByUserId(Long userId);

    /**
     * 获取所有销售数据（按销售类型分组）
     *
     * @return
     */
    List<SalesEntity> listAllBySalesType();

    /**
     * 查询全部平台销售
     *
     * @return 平台销售列表
     */
    List<SalesEntity> listPlatformSales(Integer employeeStatus);

    /**
     * 根据权限查询代理商销售
     *
     * @param agentId        代理商ID，为null时查询所有代理商销售
     * @param employeeStatus 员工状态
     * @return 代理商销售列表
     */
    List<SalesEntity> listAgentSales(Long agentId, String phone, Integer employeeStatus);

    /**
     * 根据销售类型和代理商ID查询销售
     *
     * @param salesType 销售类型（0=平台销售，1=代理商销售）
     * @param agentId   代理商ID，为null时不过滤
     * @param userPolling 是否用户轮询 0=否，1=是
     * @return 销售列表
     */
    List<SalesInfoVo> listBySalesTypeAndAgentId(Integer salesType, Long agentId, Integer userPolling);

    /**
     * 校验并保存或更新销售信息
     *
     * @param salesBo 销售信息
     */
    void checkSaveOrUpdate(SalesBo salesBo);

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
     * 根据用户id修改销售姓名和手机号
     *
     * @param userId   用户id
     * @param nickName 销售姓名
     * @param phone    手机号
     */
    void updateNameAndPhoneByUserId(Long userId, String nickName, String phone);

    /**
     * 修改员工状态
     *
     * @param Id             id
     * @param employeeStatus 员工状态
     */
    void updateEmployeeStatus(Long Id, Integer employeeStatus);

    /**
     * 批量修改员工状态
     *
     * @param agentId        代理商id
     * @param employeeStatus 员工状态
     */
    void updateEmployeeStatusByAgentId(Long agentId, Integer employeeStatus);
}

