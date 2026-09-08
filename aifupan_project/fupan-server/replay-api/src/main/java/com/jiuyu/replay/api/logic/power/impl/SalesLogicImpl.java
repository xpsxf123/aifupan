package com.jiuyu.replay.api.logic.power.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.agent.bll.AgentBll;
import com.jiuyu.replay.agent.producer.AgentPlatformSaleProducer;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.api.logic.power.SalesLogic;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.power.EmployeeStatusBo;
import com.jiuyu.replay.generic.bo.power.SalesBo;
import com.jiuyu.replay.generic.bo.power.SalesListBo;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.*;
import com.jiuyu.replay.power.bll.SalesBll;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bll.UserDetailsBll;
import com.jiuyu.replay.power.constant.Constant;
import com.jiuyu.replay.power.entity.SalesEntity;
import com.jiuyu.replay.power.producer.SalesProducer;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserDetailsInfoVo;
import com.jiuyu.replay.power.vo.UserVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 用户跟进销售人员表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@Service
public class SalesLogicImpl implements SalesLogic {

    @Resource
    private SalesBll salesBll;
    @Resource
    private UserDetailsBll userDetailsBll;
    @Resource
    private UserBll userBll;
    @Resource
    private FileBll fileBll;
    @Resource
    private SystemKvBll systemKvBll;
    @Resource
    private UserFeign userFeign;
    @Resource
    private SalesProducer salesProducer;
    @Resource
    private AgentBll agentBll;
    @Resource
    private AgentPlatformSaleProducer agentPlatformSaleProducer;
    @Resource
    private UserProducer userProducer;
    @Resource
    private OrderFeign orderFeign;


    private final String DEFAULT_SALES_URL = "h5_default_qrcode_img_id";
    private final String CLIENT_DEFAULT_SALES_URL = "client_default_sale_qrcode_img_id";


    @Override
    public R<PageUtils<SalesListVo>> queryPage(SalesListBo salesListBo) {

        // 添加权限，如果是代理商，就只能看代理商自己的
        UserCacheVo user = GlobalObject.getLocalUser();
        if (ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT.getCode()) || ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT_SALE.getCode())) {
            if (!ObjectUtil.equals(salesListBo.getSalesType(), UserEnums.salesType.ADMIN.getCode())) {
                salesListBo.setAgentId(user.getAgentId());
            } else {
                salesListBo.setAgentId(null);
            }
        }

        R<PageUtils<SalesListVo>> pageUtilsR = salesBll.queryPage(salesListBo);
        PageUtils<SalesListVo> pageUtils = pageUtilsR.getData();
        if(pageUtils != null) {
            List<SalesListVo> salesListVoList = pageUtils.getList();
            if (ObjectUtil.isNotEmpty(salesListVoList)) {

                // 图片
                List<Long> imgIds = salesListVoList.stream().map(SalesVo::getQrcodeImgId).filter(ObjectUtil::isNotEmpty).distinct().toList();
                Map<Long, FileShowVo> fileMap = this.fileBll.listByFileIds(imgIds)
                        .stream()
                        .collect(Collectors.toMap(FileShowVo::getId, Function.identity(), (o, n) -> o));
                // 代理商名称
                Map<Long, AgentInfoVo> agentMap = agentBll.listByIds(salesListVoList.stream()
                                .map(SalesVo::getAgentId)
                                .filter(ObjectUtil::isNotEmpty)
                                .distinct().toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(AgentInfoVo::getId, Function.identity(), (o, n) -> o));

                for (SalesListVo salesVo : salesListVoList) {

                    // 图片
                    FileShowVo file = fileMap.get(salesVo.getQrcodeImgId());
                    if (ObjectUtil.isNotEmpty(file)) {
                        salesVo.setQrcodeImgInfo(file);
                    }

                    // 代理商名称
                    if (ObjectUtil.isNotEmpty(salesVo.getAgentId())) {
                        AgentInfoVo agent = agentMap.get(salesVo.getAgentId());
                        if (ObjectUtil.isNotEmpty(agent)) {
                            salesVo.setAgentName(agent.getAgentName());
                        }
                    }
                }
            }
        }

        return pageUtilsR;
    }

    @Override
    public R<SalesInfoVo> info(Long id) {

        R<SalesInfoVo> salesInfoVoR = salesBll.info(id);
        if(salesInfoVoR.getData() != null) {
            SalesInfoVo salesInfoVo = salesInfoVoR.getData();
            FileShowVo fileShowVo = this.fileBll.infoByFileId(salesInfoVo.getQrcodeImgId());
            salesInfoVo.setQrcodeImgInfo(fileShowVo);
        }

        return salesInfoVoR;
    }

    @Override
    public R<String> save(SalesBo salesBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        if (user != null && user.getUserType() == UserEnums.userType.MANAGE_ADMIN_USER.getCode()) {
            if (user.getAdminUserType() == UserEnums.adminUserType.AGENT.getCode() || user.getAdminUserType() == UserEnums.adminUserType.AGENT_SALE.getCode()) {
                salesBo.setAgentId(user.getAgentId());
                salesBo.setSalesType(UserEnums.salesType.AGENT.getCode());
            } else if (user.getAdminUserType() == UserEnums.adminUserType.ADMIN.getCode()) {
                salesBo.setSalesType(UserEnums.salesType.ADMIN.getCode());
            } else {
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "用户类型异常");
            }
        }

        return salesBll.save(salesBo);
    }

    @Override
    public R<String> update(SalesBo salesBo) {

        return salesBll.update(salesBo);
    }

    /**
     * 查询当销售人员是否有绑定的用户
     * @param id 销售人员ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {
        RRException.isNotEmpty(id, "销售不能为空");

        SalesInfoVo sales = salesProducer.info(id);
        RRException.isNotEmpty(sales, "销售不存在");

        // 查询是否有绑定代理商
        long countAgent = agentPlatformSaleProducer.countBySaleId(id);
        if (countAgent > 0) {
            return R.error(40001, "当前销售人员已经绑定代理商,请先取消绑定后再删除");
        }

        // 查询当销售人员是否有绑定的用户
        Long count = userDetailsBll.countClientDetailBySaleId(id);
        if (count > 0){
            return R.error(40001,"当前销售人员有绑定的用户,请先取消绑定后再删除");
        }
        salesBll.delete(id);

        // 删除用户
        if (sales.getUserId() != null) {
            // 修改关联用户的状态为禁用
            userProducer.updateStatusById(sales.getUserId(), 1);
        }

        return R.ok("删除成功");
    }

    @Override
    public R<SalesInfoVo> getCurrentUserSale() {

        UserCacheVo user = GlobalObject.getLocalUser();
        Long userId = user.getId();
        if(user.getUserType() == 2) {
            userId = user.getParentId();
        }

        SalesInfoVo salesInfoVo = null;

        // 获取用户详情信息
        R<UserDetailsInfoVo> userDetailsInfoVoR = this.userDetailsBll.getByUserId(userId);
        if(userDetailsInfoVoR.getData() != null) {
            UserDetailsInfoVo userDetailsInfoVo = userDetailsInfoVoR.getData();
            // 获取销售信息
            if(userDetailsInfoVo.getSaleId() != null && !userDetailsInfoVo.getSaleId().equals(0L)) {
                R<SalesInfoVo> salesInfoVoR = salesBll.info(userDetailsInfoVo.getSaleId());
                if(salesInfoVoR.getData() != null) {
                    salesInfoVo = salesInfoVoR.getData();
                    FileShowVo fileShowVo = this.fileBll.infoByFileId(salesInfoVo.getQrcodeImgId());
                    salesInfoVo.setQrcodeImgInfo(fileShowVo);
                }
            }
        }

        // 用户没有配置销售，使用默认的二维码
        if (salesInfoVo == null) {
            R<SystemKvInfoVo> kvInfoVoR = this.systemKvBll.getByKey(CLIENT_DEFAULT_SALES_URL);
            if (kvInfoVoR.getData() != null) {
                SystemKvInfoVo systemKvInfoVo = kvInfoVoR.getData();
                salesInfoVo = new SalesInfoVo();
                salesInfoVo.setSalesName("默认销售");
                FileShowVo fileShowVo = this.fileBll.infoByFileId(Long.valueOf(systemKvInfoVo.getKvValue()));
                salesInfoVo.setQrcodeImgInfo(fileShowVo);
            }
        }

        if (salesInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "未配置二维码");
        }

        // 这个功能，慢总说不用了，修改回原来的逻辑
        // 纯录制版本判断：如果用户是纯录制版本，替换为纯录制版本的销售二维码
//        try {
//            OrderInfoVo currentOrder = orderFeign.currentOrderByUserId(userId);
//            if (currentOrder != null && currentOrder.getLevel() != null) {
//                String pureRecordLevel = systemKvBll.getValueByKey("pure_recording_version_level", "");
//                if (StringUtil.isNotBlank(pureRecordLevel)
//                        && currentOrder.getLevel().toString().equals(pureRecordLevel)) {
//                    R<SystemKvInfoVo> pureRecordKv = this.systemKvBll.getByKey("pure_record_sale_qrcode_img_id");
//                    if (pureRecordKv.getData() != null) {
//                        FileShowVo pureRecordImg = this.fileBll.infoByFileId(Long.valueOf(pureRecordKv.getData().getKvValue()));
//                        salesInfoVo.setQrcodeImgInfo(pureRecordImg);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            // 获取订单失败不影响正常返回
//        }

        return R.ok(salesInfoVo);
    }



    /**
     * 获取当前用户的手机号获取销售人员信息
     *
     * @param phone
     * @return
     */
    @Override
    public R<SalesInfoVo> getCurrentUserSaleByPhone(String phone) {
        R<UserVo> byPhone = userBll.getByPhoneTenant(phone);
        if (byPhone.getData() == null){
            RRException.create("用户不存在");
        }
        UserVo user = byPhone.getData();
        Long userId = user.getId();
        if(user.getUserType() == 2) {
            if (ObjectUtil.isNotNull(user.getParentId()) && user.getParentId() != 0) {
                userId = user.getParentId();
            }
        }
        return userIdToSale(userId);
    }


    public R<SalesInfoVo> userIdToSale(Long userId){

        // 获取用户详情信息
        R<UserDetailsInfoVo> userDetailsInfoVoR = this.userDetailsBll.getByUserId(userId);
        if(userDetailsInfoVoR.getData() != null) {
            UserDetailsInfoVo userDetailsInfoVo = userDetailsInfoVoR.getData();
            // 获取销售信息
            if(userDetailsInfoVo.getSaleId() != null && !userDetailsInfoVo.getSaleId().equals(0L)) {
                R<SalesInfoVo> salesInfoVoR = salesBll.info(userDetailsInfoVo.getSaleId());
                if(salesInfoVoR.getData() != null) {
                    SalesInfoVo salesInfoVo = salesInfoVoR.getData();
                    if (ObjectUtil.isNotNull(salesInfoVo) && ObjectUtil.isNotNull(salesInfoVo.getQrcodeImgId()) && salesInfoVo.getQrcodeImgId() > 0) {
                        FileShowVo fileShowVo = this.fileBll.infoByFileId(salesInfoVo.getQrcodeImgId());
                        if (ObjectUtil.isNotNull(fileShowVo) && StringUtil.isNotBlank(fileShowVo.getUrl())) {
                            salesInfoVo.setQrcodeImgInfo(fileShowVo);
                            return salesInfoVoR;
                        }
                    }
                }
            }
        }

        // 用户没有配置销售，使用默认的二维码
        R<SystemKvInfoVo> kvInfoVoR = this.systemKvBll.getByKey(DEFAULT_SALES_URL);
        if (ObjectUtil.isNotEmpty(kvInfoVoR.getData())) {
            SystemKvInfoVo systemKvInfoVo = kvInfoVoR.getData();
            SalesInfoVo salesInfoVo = new SalesInfoVo();
            salesInfoVo.setSalesName("默认销售");
            FileShowVo fileShowVo = this.fileBll.infoByFileId(Long.valueOf(systemKvInfoVo.getKvValue()));
            salesInfoVo.setQrcodeImgInfo(fileShowVo);
            return R.ok(salesInfoVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "未配置二维码");
    }

    @Override
    public R<List<SalesCascaderVo>> userListSalesSearch(Integer employeeStatus) {
        UserCacheVo user = GlobalObject.getLocalUser();
        int adminType = user.getAdminUserType();

        // 1. 先查询代理商销售
        List<SalesEntity> agentSales = salesProducer.listAgentSales(
                adminType == UserEnums.adminUserType.AGENT.getCode() || adminType == UserEnums.adminUserType.AGENT_SALE.getCode() ? user.getAgentId() : null,
                adminType == UserEnums.adminUserType.AGENT_SALE.getCode() ? user.getPhone() : null,
                employeeStatus
        );

        // 2. 如果是代理商销售用户，直接返回自己的销售信息
        if (adminType == UserEnums.adminUserType.AGENT_SALE.getCode()) {
            return R.ok(agentSales.stream()
                    .map(sale -> SalesCascaderVo.builder()
                            .value(sale.getId())
                            .label(sale.getSalesName())
                            .build())
                    .collect(Collectors.toList()));
        }

        // 3. 查询平台销售
        List<SalesEntity> platformSales = salesProducer.listPlatformSales(employeeStatus);

        // 4. 查询所有代理商信息
        Set<Long> agentIds = agentSales.stream()
                .map(SalesEntity::getAgentId)
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toSet());
        Map<Long, String> agentNameMap = getAgentNameMap(agentIds, employeeStatus);

        // 5. 组装数据
        List<SalesCascaderVo> result = new ArrayList<>();

        // 添加平台销售
        List<SalesCascaderVo> platformLevel2 = convertToLevel2(platformSales);
        result.add(buildCascaderLevel1("0", "平台销售", platformLevel2));

        // 添加代理商销售
        List<SalesCascaderVo> agentLevel2 = agentSales.stream()
                .filter(item -> ObjectUtil.isNotEmpty(item.getAgentId()))
                .collect(Collectors.groupingBy(SalesEntity::getAgentId))
                .entrySet().stream()
                .map(entry -> buildAgentLevel2(entry.getKey(), entry.getValue(), agentNameMap))
                .collect(Collectors.toList());
        result.add(buildCascaderLevel1("1", "代理商销售", agentLevel2));

        return R.ok(result);
    }

    /**
     * 构建一级级联选择器节点
     *
     * @param value    节点值
     * @param label    节点标签
     * @param children 子节点列表
     * @return 级联选择器节点
     */
    private SalesCascaderVo buildCascaderLevel1(Object value, String label, List<SalesCascaderVo> children) {
        return SalesCascaderVo.builder()
                .value(value)
                .label(label)
                .children(children)
                .build();
    }

    /**
     * 将销售实体列表转换为二级级联选择器节点
     *
     * @param sales 销售实体列表
     * @return 级联选择器节点列表
     */
    private List<SalesCascaderVo> convertToLevel2(List<SalesEntity> sales) {
        return sales.stream()
                .map(sale -> SalesCascaderVo.builder()
                        .value(sale.getId())
                        .label(sale.getSalesName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建代理商二级级联选择器节点
     *
     * @param agentId      代理商ID
     * @param sales        销售实体列表
     * @param agentNameMap 代理商ID与名称的映射
     * @return 级联选择器节点
     */
    private SalesCascaderVo buildAgentLevel2(Long agentId, List<SalesEntity> sales, Map<Long, String> agentNameMap) {
        String agentName = agentNameMap.getOrDefault(agentId, "");
        return SalesCascaderVo.builder()
                .value(agentId)
                .label(agentName)
                .children(convertToLevel2(sales))
                .build();
    }

    @Override
    public R<List<SalesInfoVo>> userDetailsSalesSearch(Long userId) {
        UserCacheVo user = GlobalObject.getLocalUser();
        int adminType = user.getAdminUserType();

        List<SalesInfoVo> result = new ArrayList<>();
        // 平台用户：查询平台销售
        if (adminType == UserEnums.adminUserType.ADMIN.getCode()) {
            result = salesProducer.listBySalesTypeAndAgentId(0, null, null);
        }

        // 代理商用户：查询当前代理商下的销售
        if (adminType == UserEnums.adminUserType.AGENT.getCode() || adminType == UserEnums.adminUserType.AGENT_SALE.getCode()) {
            result = salesProducer.listBySalesTypeAndAgentId(1, user.getAgentId(), null);
        }

        if (ObjectUtil.isNotEmpty(userId)) {
            UserDetailsInfoVo userDetail = ResultUtil.getResult(userDetailsBll.getByUserId(userId));
            if (userDetail != null && ObjectUtil.isNotEmpty(userDetail.getSaleId()) && userDetail.getSaleId() != 0L) {
                if (result.stream().noneMatch(salesInfoVo -> ObjectUtil.equals(userDetail.getSaleId(), salesInfoVo.getId()))) {
                    SalesInfoVo sales = salesProducer.info(userDetail.getSaleId());
                    if (sales != null) {
                        result.add(sales);
                    }
                }
            }
        }

        return R.ok(result);
    }

    @Override
    public void updateChooseStatus(SalesBo salesBo) {
        SalesInfoVo sales = salesProducer.info(salesBo.getId());
        if (sales.getEmployeeStatus() == 0 && (ObjectUtil.equals(salesBo.getIsChoose(), 1) || ObjectUtil.equals(salesBo.getUserPolling(), 1))) {
            throw new BusinessException("该销售人员已离职，不能开启");
        }
        salesProducer.update(salesBo);
    }

    /**
     * 一次性查询所有代理商信息
     *
     * @param agentIds       代理商ID集合
     * @param employeeStatus 员工状态
     * @return 代理商ID与名称的映射
     */
    private Map<Long, String> getAgentNameMap(Set<Long> agentIds, Integer employeeStatus) {
        if (agentIds.isEmpty()) {
            return new HashMap<>();
        }

        List<AgentInfoVo> agents = agentBll.listByIds(new java.util.ArrayList<>(agentIds));
        return agents.stream()
                .filter(item -> employeeStatus == null || ObjectUtil.equals(item.getEmployeeStatus(), employeeStatus))
                .collect(Collectors.toMap(AgentInfoVo::getId, AgentInfoVo::getAgentName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateEmployeeStatus(EmployeeStatusBo employeeStatusBo) {
        // 根据sourceType分发到不同的模块处理
        Integer sourceType = employeeStatusBo.getSourceType();
        UpdateEmployeeStatusVo params = new UpdateEmployeeStatusVo();

        // 0-用户，1-代理商，2-销售
        // 获取对应的更新参数
        if (sourceType == 0) {
            params = userBll.getUpdateEmployeeParams(employeeStatusBo.getSourceId());
        } else if (sourceType == 1) {
            params = agentBll.getUpdateEmployeeParams(employeeStatusBo.getSourceId());
        } else if (sourceType == 2) {
            params = salesBll.getUpdateEmployeeParams(employeeStatusBo.getSourceId());
        }

        // 更新
        if (params.getUserId() != null) {
            userBll.updateEmployeeStatus(params.getUserId(), employeeStatusBo.getEmployeeStatus());
        }
        if (params.getAgentId() != null) {
            agentBll.updateEmployeeStatus(params.getAgentId(), employeeStatusBo.getEmployeeStatus());
        }
        if (params.getSalesId() != null) {
            salesBll.updateEmployeeStatus(params.getSalesId(), employeeStatusBo.getEmployeeStatus());
        }

        return R.ok();
    }
}

