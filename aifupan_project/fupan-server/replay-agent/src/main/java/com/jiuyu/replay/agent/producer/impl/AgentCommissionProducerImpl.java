package com.jiuyu.replay.agent.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.AgentCommissionBo;
import com.jiuyu.replay.agent.bo.AgentCommissionListBo;
import com.jiuyu.replay.agent.bo.CommissionAllocationBo;
import com.jiuyu.replay.agent.entity.AgentCommissionEntity;
import com.jiuyu.replay.agent.entity.AgentEntity;
import com.jiuyu.replay.agent.entity.AgentPromotionEntity;
import com.jiuyu.replay.agent.entity.InviteUrlCodeEntity;
import com.jiuyu.replay.agent.producer.AgentCommissionProducer;
import com.jiuyu.replay.agent.repository.service.AgentCommissionService;
import com.jiuyu.replay.agent.repository.service.AgentPromotionService;
import com.jiuyu.replay.agent.repository.service.AgentService;
import com.jiuyu.replay.agent.repository.service.InviteUrlCodeService;
import com.jiuyu.replay.agent.vo.AgentCommissionInfoVo;
import com.jiuyu.replay.agent.vo.AgentCommissionListVo;
import com.jiuyu.replay.agent.vo.CommissionRecordsVo;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 代理商佣金
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentCommissionProducerImpl implements AgentCommissionProducer {

    private final AgentCommissionService agentCommissionService;
    private final AgentService agentService;
    private final AgentPromotionService agentPromotionService;
    private final InviteUrlCodeService inviteUrlCodeService;
    private final SystemKvService systemKvService;


    @Override
    public PageUtils<AgentCommissionListVo> queryPage(AgentCommissionListBo agentCommissionListBo) {
        QueryWrapper<AgentCommissionEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(agentCommissionListBo.getKeyword())){
            wrapper.like("name", agentCommissionListBo.getKeyword());
        }

        IPage<AgentCommissionEntity> iPage = agentCommissionService.page(new Query<AgentCommissionEntity>().getPage(agentCommissionListBo.getPage(), agentCommissionListBo.getLimit()), wrapper);

        PageUtils<AgentCommissionListVo> pageUtils = new PageUtils<>(agentCommissionListBo.getPage(), agentCommissionListBo.getLimit(), iPage);

        List<AgentCommissionEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AgentCommissionListVo> vos = records.stream().map(item -> {
                AgentCommissionListVo agentCommissionVo = new AgentCommissionListVo();
                BeanUtils.copyProperties(item, agentCommissionVo);
                return agentCommissionVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AgentCommissionInfoVo info(Long id) {

        AgentCommissionEntity agentCommissionEntity = agentCommissionService.getById(id);
        if(agentCommissionEntity != null) {
            AgentCommissionInfoVo agentCommissionInfoVo = new AgentCommissionInfoVo();
            BeanUtils.copyProperties(agentCommissionEntity, agentCommissionInfoVo);
            return agentCommissionInfoVo;
        }

        return null;
    }

    @Override
    public AgentCommissionInfoVo save(AgentCommissionBo agentCommissionBo) {

         AgentCommissionEntity agentCommissionEntity = new AgentCommissionEntity();
         BeanUtils.copyProperties(agentCommissionBo, agentCommissionEntity);
         agentCommissionEntity.setId(SnowflakeManager.nextValue());
         agentCommissionEntity.setCreateDate(new Date());
         agentCommissionEntity.setUpdateDate(new Date());

         agentCommissionService.save(agentCommissionEntity);

         AgentCommissionInfoVo agentCommissionInfoVo = new AgentCommissionInfoVo();
         BeanUtils.copyProperties(agentCommissionEntity, agentCommissionInfoVo);

         return agentCommissionInfoVo;
     }

    @Override
    public void update(AgentCommissionBo agentCommissionBo) {

        AgentCommissionEntity agentCommissionEntity = new AgentCommissionEntity();
        BeanUtils.copyProperties(agentCommissionBo, agentCommissionEntity);
        agentCommissionEntity.setUpdateDate(new Date());

        agentCommissionService.updateById(agentCommissionEntity);
    }

    @Override
    public void deleteById(Long id) {

        agentCommissionService.removeById(id);
    }

    @Override
    public boolean commissionAllocation(CommissionAllocationBo allocationBo) {
        RRException.isNotEmpty(allocationBo.getOrderId(), "订单id不能为空");
        RRException.isNotEmpty(allocationBo.getUserId(), "用户id不能为空");
        RRException.isNotEmpty(allocationBo.getInviteUrlCode(), "邀请链接code不能为空");
        RRException.isNotEmpty(allocationBo.getCommissionType(), "分佣类型不能为空");

        if (allocationBo.getOrderTotalMoney() <= 0) {
            return false;
        }

        InviteUrlCodeEntity one = inviteUrlCodeService.getOne(new LambdaQueryWrapper<InviteUrlCodeEntity>()
                .eq(InviteUrlCodeEntity::getUrlCode, allocationBo.getInviteUrlCode())
        );

        if (one == null){
            log.info("邀请链接code查询失败, 不分佣，code={}", allocationBo.getInviteUrlCode());
            return false;
        }

        // 查询代理商
        RRException.isNotEmpty(one.getAgentId(), "数据错误，tb_invite_url_code表的代理商id为空");
        AgentEntity agent = agentService.getById(one.getAgentId());
        if (agent.getAgentStatus() == 0){
            log.info("查询代理商状态为0，不分佣，id={}", agent.getId());
            return false;
        }

        // 查询佣金比例
        Double commission = 0.0;

        // 现在都是代理商分佣
        one.setCodeType(0);
        if (one.getCodeType() == 0) {
            log.info("查询代理商失败，id={}", agent.getId());
            RRException.isNotEmpty(agent, "代理商查询失败");
            if (allocationBo.getCommissionType() == 0){
                commission = agent.getCommissionRate();
            }else if (allocationBo.getCommissionType() == 1){
                commission = agent.getRenewalCommissionRate();

                // 续费分佣判断：代理商是普通代理商，并且是第一次续费分佣
                if (agent.getAgentType() == 0 && agent.getAgentStatus() == 1){
                    // 断约用户天数，-1没有断约用户
                    int offsetNum = -1;

                    SystemKvEntity newRenewalIntervalR = systemKvService.getByKey("new_renewal_interval");
                    if (newRenewalIntervalR != null){
                        Integer breakAgreementNum1 = NumberUtil.parseInt(newRenewalIntervalR.getKvValue(), 90);
                        if (breakAgreementNum1 != -1){
                            offsetNum = 0;
                            offsetNum += breakAgreementNum1;
                        }
                    }
                    int breakAgreementNum = -1;
                    SystemKvEntity breakAgreementNumR = systemKvService.getByKey("break_agreement_num");
                    if (breakAgreementNumR != null){
                        breakAgreementNum = NumberUtil.parseInt(breakAgreementNumR.getKvValue(), 180);
                        if (breakAgreementNum != -1){
                            if (offsetNum == -1) offsetNum = 0;
                            offsetNum += breakAgreementNum;
                        }
                    }
                    DateTime offset = null;
                    if (offsetNum != -1){
                        offset = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, -offsetNum);
                    }

                    // 判断是否有过续费分佣
                    long count = agentCommissionService.count(new LambdaQueryWrapper<AgentCommissionEntity>()
                            .eq(AgentCommissionEntity::getAgentId, agent.getId())
                            .eq(AgentCommissionEntity::getUserId, allocationBo.getUserId())
                            .eq(AgentCommissionEntity::getCommissionType, 1)
                            .gt(breakAgreementNum != -1 && offset != null, AgentCommissionEntity::getCreateDate, offset)
                    );
                    if (count > 0){
                        log.info("续费不分佣，原因：代理商已经有过续费分佣，agentId={}", agent.getId());
                        return false;
                    }

                }else{
                    log.info("续费不分佣，原因：代理商不是普通代理商或者只有为未启用，agentId={}", agent.getId());
                    return false;
                }
            }else{
                RRException.create("分佣类型错误");
            }
        }else if (one.getCodeType() == 1 || one.getCodeType() == 3){
            RRException.isNotEmpty(one.getPromotionId(), "数据错误，tb_invite_url_code表的代理商id为空");
            AgentPromotionEntity agentPromotion = agentPromotionService.getById(one.getPromotionId());
            RRException.isNotEmpty(agentPromotion, "代理商推广渠道查询失败");
            if (agentPromotion.getPromotionStatus() == 0){
                log.info("代理商渠道状态为0，不分佣，id={}", agentPromotion.getId());
                return false;
            }
            if (allocationBo.getCommissionType() == 0){
                commission = agentPromotion.getCommissionRate();
            }else if (allocationBo.getCommissionType() == 1){
                commission = agentPromotion.getRenewalCommissionRate();
            }else{
                RRException.create("分佣类型错误");
            }
        }else  if (one.getCodeType() == 2){
            RRException.create("当前方法不适用");
        }
        int commissionMoney = new BigDecimal(allocationBo.getOrderTotalMoney()).multiply(BigDecimal.valueOf(commission > 1 ? 1 : commission)).intValue();
        if (commissionMoney <= 0){
            log.info("分佣金额为0，不分佣，obj={}", allocationBo);
            return false;
        }

        if (commissionMoney > allocationBo.getOrderTotalMoney()){
            log.info("分佣金额大于订单总金额，分全部金额，分佣金额={}，obj={}", commissionMoney, allocationBo);
            commissionMoney = allocationBo.getOrderTotalMoney();
        }

        AgentCommissionBo result = new AgentCommissionBo();
        result.setAgentId(one.getAgentId());
        result.setPromotionId(one.getPromotionId());
        result.setAgentSaleId(one.getAgentSaleId());
        result.setOrderId(allocationBo.getOrderId());
        result.setUserId(allocationBo.getUserId());
        result.setOrderTotalMoney(allocationBo.getOrderTotalMoney());
        result.setCommission(commission);
        result.setCommissionMoney(commissionMoney);
        result.setCommissionType(allocationBo.getCommissionType());
        result.setCommissionMode(one.getCodeType());
        result.setRemarks(allocationBo.getRemarks());
        Date now = new Date();
        result.setCommissionTime(now);
        save(result);
        return true;
    }

    @Override
    public List<CommissionRecordsVo> commissionRecords(Long agentId) {
        ArrayList<CommissionRecordsVo> result = new ArrayList<>();
        List<AgentCommissionEntity> list = agentCommissionService.list(new LambdaQueryWrapper<AgentCommissionEntity>()
                .eq(AgentCommissionEntity::getAgentId, agentId)
                .orderByDesc(AgentCommissionEntity::getCreateDate)
        );

        if (ObjectUtil.isNotEmpty(list)){

            Map<String, List<AgentCommissionEntity>> map = list.stream()
                    .filter(item -> ObjectUtil.isNotEmpty(item.getCreateDate()))
                    .collect(Collectors.groupingBy(
                            item -> DateUtil.format(item.getCreateDate(),  "yyyy-MM"),
                            LinkedHashMap::new, // 保留插入顺序
                            Collectors.toList()
                    ));
            Date now = new Date();
            String now2 = DateUtil.format(now, "yyyy-MM");
            map.forEach((key, value) -> {
                YearMonth yearMonth = YearMonth.parse(key);
                List<AgentCommissionEntity> commissionRateList = value.stream()
                        .filter(item -> item.getCommissionType() == 0)
                        .toList();
                // 新签佣金的平均值
                Double commissionRate = NumberUtil.round(commissionRateList.stream()
                        .mapToDouble(AgentCommissionEntity::getCommission)
                        .average().orElse(0.0), 4).doubleValue();
                double commissionRateAmount = commissionRateList.stream()
                        .mapToInt(AgentCommissionEntity::getOrderTotalMoney)
                        .sum();

                List<AgentCommissionEntity> renewalCommissionRateList = value.stream()
                        .filter(item -> item.getCommissionType() == 1)
                        .toList();
                // 新签佣金的平均值
                Double renewalCommissionRate = NumberUtil.round(renewalCommissionRateList.stream()
                        .mapToDouble(AgentCommissionEntity::getCommission)
                        .average().orElse(0.0), 4).doubleValue();
                double renewalCommissionRateAmount = renewalCommissionRateList.stream()
                        .mapToInt(AgentCommissionEntity::getOrderTotalMoney)
                        .sum();

                CommissionRecordsVo obj = new CommissionRecordsVo();
                obj.setStartDate(DateUtil.format(yearMonth.atDay(1).atStartOfDay(), "yyyy-MM-dd"));
                obj.setEndDate(DateUtil.format(yearMonth.atEndOfMonth().atStartOfDay(), "yyyy-MM-dd"));
                obj.setCommissionRate(commissionRate);
                obj.setCommissionRateAmount(NumberUtil.round(commissionRateAmount / 100, 2).doubleValue());
                obj.setRenewalCommissionRate(renewalCommissionRate);
                obj.setRenewalCommissionRateAmount(NumberUtil.round(renewalCommissionRateAmount / 100, 2).doubleValue());
                double settlementAmount = value.stream().mapToInt(AgentCommissionEntity::getCommissionMoney).sum();
                obj.setSettlementAmount(NumberUtil.round(settlementAmount / 100, 2).doubleValue());
                obj.setStatus(now2.equals(key) ? 0 : 1);
                result.add(obj);
            });

        }

        return result;
    }

    @Override
    public List<AgentCommissionInfoVo> listByOrderIds(List<Long> orderIds) {
        List<AgentCommissionEntity> list = agentCommissionService.list(new LambdaQueryWrapper<AgentCommissionEntity>()
                .in(AgentCommissionEntity::getOrderId, orderIds)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, AgentCommissionInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public AgentCommissionInfoVo getByOrderId(Long orderId) {
        AgentCommissionEntity one = agentCommissionService.getOne(new LambdaQueryWrapper<AgentCommissionEntity>()
                .eq(AgentCommissionEntity::getOrderId, orderId).last( " limit 1")
        );
        if (ObjectUtil.isNotEmpty(one)){
            return BeanUtil.copyProperties(one, AgentCommissionInfoVo.class);
        }
        return null;
    }
}

