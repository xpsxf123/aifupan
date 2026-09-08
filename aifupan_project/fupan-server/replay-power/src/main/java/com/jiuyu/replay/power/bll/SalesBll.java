package com.jiuyu.replay.power.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.power.AgentUserAddBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.*;
import com.jiuyu.replay.generic.bo.power.SalesBo;
import com.jiuyu.replay.generic.bo.power.SalesListBo;
import com.jiuyu.replay.power.entity.SalesEntity;
import com.jiuyu.replay.power.producer.SalesProducer;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * 用户跟进销售人员表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@Component
public class SalesBll {

    @Resource
    private SalesProducer salesProducer;
    @Resource
    private SystemKvService systemKvService;
    @Resource
    private UserFeign userFeign;

    /**
     * 获取轮询到的销售id
     * @return
     */
    public Long getPollingSaleId() {
        return this.salesProducer.getPollingSaleId();
    }

    /**
     * 用户跟进销售人员表列表
     * @param salesListBo 用户跟进销售人员表列表查询参数
     * @return
     */
    public R<PageUtils<SalesListVo>> queryPage(SalesListBo salesListBo) {

        return R.ok("获取成功", salesProducer.queryPage(salesListBo));
    }

    /**
    * 用户跟进销售人员表信息
    * @param id 用户跟进销售人员表id
    * @return
    */
    public R<SalesInfoVo> info(Long id) {

        SalesInfoVo salesInfoVo = salesProducer.info(id);
        return R.ok("获取成功", salesInfoVo);
    }

    /**
     * 新增用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(SalesBo salesBo) {

        // 校验
        salesProducer.checkSaveOrUpdate(salesBo);

        // 创建用户或修改用户
        AgentUserAddBo userAddBo = getAgentUserBo(salesBo);
        userAddBo.setNickName(salesBo.getSalesName());
        userAddBo.setPhone(salesBo.getPhone());
        userAddBo.setStatus(0);
        long salesId = SnowflakeManager.nextValue();
        salesBo.setId(salesId);
        userAddBo.setSaleId(salesId);
        UserPass userPass = userFeign.saveOrUpdateAgentUser(userAddBo);
        salesBo.setUserId(userPass.getId());

        SalesInfoVo sales = salesProducer.save(salesBo);
        return R.ok("添加成功", userPass.getPass());
    }

    /**
     * 获取代理商销售添加用户的bo
     *
     * @param salesBo 销售信息
     * @return
     */
    @NotNull
    private AgentUserAddBo getAgentUserBo(SalesBo salesBo) {
        AgentUserAddBo userAddBo = new AgentUserAddBo();
        // 设置类型
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        if (user.getAdminUserType() == UserEnums.adminUserType.AGENT.getCode() || user.getAdminUserType() == UserEnums.adminUserType.AGENT_SALE.getCode()) {
            userAddBo.setAdminUserType(UserEnums.adminUserType.AGENT_SALE.getCode());
            userAddBo.setAgentId(user.getAgentId());
        } else {
            if (ObjectUtil.equals(salesBo.getSalesType(), UserEnums.salesType.AGENT.getCode())) {
                userAddBo.setAdminUserType(UserEnums.adminUserType.AGENT_SALE.getCode());
                if (ObjectUtil.isEmpty(salesBo.getAgentId())) {
                    throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "代理商不能为空");
                }
                userAddBo.setAgentId(salesBo.getAgentId());
            } else {
                userAddBo.setAdminUserType(UserEnums.adminUserType.ADMIN.getCode());
            }
        }
        return userAddBo;
    }

    /**
     * 修改用户跟进销售人员表
     * @param salesBo 用户跟进销售人员表对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(@NotNull SalesBo salesBo) {

        SalesInfoVo sales = salesProducer.info(salesBo.getId());
        if (ObjectUtil.isEmpty(sales)) {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "销售信息不存在");
        }

        if (!Objects.equals(sales.getSalesType(), salesBo.getSalesType())) {
            throw new BusinessException(StatusCode.OPERATION_EX.getCode(), "销售类型不能修改");
        }

        AgentUserAddBo userAddBo = getAgentUserBo(salesBo);
        userAddBo.setNickName(salesBo.getSalesName());
        userAddBo.setPhone(salesBo.getPhone());
        userAddBo.setUserId(sales.getUserId());
        userAddBo.setSaleId(salesBo.getId());
        UserPass userPass = userFeign.saveOrUpdateAgentUser(userAddBo);
        salesBo.setUserId(userPass.getId());
        if (ObjectUtil.equals(sales.getSalesType(), UserEnums.salesType.ADMIN.getCode())) {
            salesBo.setAgentId(null);
        }
        BeanUtil.copyProperties(salesBo, sales);
        salesProducer.updateAllField(BeanUtil.copyProperties(sales, SalesEntity.class));
        return R.ok("修改成功", userPass.getPass());
    }

    /**
     * 删除用户跟进销售人员表
     * @param id 用户跟进销售人员表id
     * @return
     */
    public R<String> delete(Long id) {

        salesProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 获取所有的销售人员
     * @return
     */
    public List<SalesEntity> listAll() {
        return salesProducer.listAll();
    }

    /**
     * 根据销售人员id集合获取销售人员列表
     * @param saleIds 销售人员id集合
     * @return
     */
    public R<List<SalesInfoVo>> listByIds(Collection<Long> saleIds) {

        List<SalesInfoVo> salesInfoVos = this.salesProducer.listByIds(saleIds);

        return R.ok(salesInfoVos);
    }


    /**
     * 根据销售人员id集合获取销售人员列表
     * @param list
     * @return
     */
    public List<SalesInfoVo> selectBySalesIds(List<Long> list) {
        return this.salesProducer.selectBySalesIds(list);
    }


    /**
     * 获取没有开启轮询的
     * @param salesIds
     * @return
     */
    public List<SalesInfoVo> listByIdsFilterNoChoose(List<Long> salesIds) {
        return this.salesProducer.listByIdsFilterNoChoose(salesIds);
    }

    /**
     * 根据用户id修改销售姓名和手机号
     *
     * @param userId   用户id
     * @param nickName 销售姓名
     * @param phone    手机号
     */
    public void updateNameAndPhoneByUserId(Long userId, String nickName, String phone) {
        salesProducer.updateNameAndPhoneByUserId(userId, nickName, phone);
    }

    /**
     * 修改员工状态
     *
     * @param id             id
     * @param employeeStatus 员工状态
     */
    public void updateEmployeeStatus(Long id, Integer employeeStatus) {
        salesProducer.updateEmployeeStatus(id, employeeStatus);
    }

    /**
     * 获取修改员工状态的参数
     *
     * @param salesId 销售id
     * @return 参数
     */
    public UpdateEmployeeStatusVo getUpdateEmployeeParams(Long salesId) {
        UpdateEmployeeStatusVo res = new UpdateEmployeeStatusVo();

        if (salesId == null) {
            return res;
        }
        SalesInfoVo info = salesProducer.info(salesId);
        if (info == null) {
            return res;
        }
        res.setSalesId(salesId);
        res.setUserId(info.getUserId());
        return res;
    }
}

