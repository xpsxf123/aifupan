package com.jiuyu.replay.reward.bll;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.constant.packageunit.CommodityTypeConvert;
import com.jiuyu.replay.common.constant.packageunit.ValidityPeriod;
import com.jiuyu.replay.common.constant.packageunit.ValidityPeriodCalculator;
import com.jiuyu.replay.common.constant.packageunit.ValidityUnitEnum;
import com.jiuyu.replay.common.lock.DistributedLock;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.reward.ClientInviteRewardRecordListBo;
import com.jiuyu.replay.generic.bo.reward.UserRewardBo;
import com.jiuyu.replay.generic.constant.activity.InviteRewardRuleCodeEnum;
import com.jiuyu.replay.generic.dto.activity.*;
import com.jiuyu.replay.generic.dto.power.UserDeviceFingerprintDto;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.activity.ActivityFeign;
import com.jiuyu.replay.generic.feign.activity.ClientInviteProgressFeign;
import com.jiuyu.replay.generic.feign.activity.ClientInviteProgressRewardFeign;
import com.jiuyu.replay.generic.feign.activity.UserInviteFeign;
import com.jiuyu.replay.generic.feign.agent.InviteUrlCodeFeign;
import com.jiuyu.replay.generic.feign.order.CommodityTypeFeign;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.order.PackageFeign;
import com.jiuyu.replay.generic.feign.power.UserDeviceFingerprintFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.activity.ClientInviteActivityInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardInfoVo;
import com.jiuyu.replay.generic.vo.activity.ClientInviteProgressRewardVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.CommodityPriceVo;
import com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo;
import com.jiuyu.replay.generic.vo.order.CommodityTypeVo;
import com.jiuyu.replay.generic.vo.order.PackageInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.reward.ClientInviteRewardRecordInfoVo;
import com.jiuyu.replay.generic.vo.reward.ClientUserRewardRecordDetailVo;
import com.jiuyu.replay.generic.vo.reward.ClientUserRewardRecordVo;
import com.jiuyu.replay.generic.vo.reward.RewardSummaryVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.system.DictDataVo;
import com.jiuyu.replay.reward.bo.ClientInviteRewardRecordInfoBo;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordEntity;
import com.jiuyu.replay.reward.entity.ClientInviteRewardRecordGroupEntity;
import com.jiuyu.replay.reward.rse.ClientInviteRewardRecordRse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 进度奖励记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 17:03:13
 */
@Slf4j
@Component
public class ClientInviteRewardRecordBll {

    @Resource
    private UserFeign userFeign;
    @Resource
    private ActivityFeign activityFeign;
    @Resource
    private InviteUrlCodeFeign inviteUrlCodeFeign;
    @Resource
    private UserInviteFeign userInviteFeign;
    @Resource
    private ClientInviteRewardRecordRse clientInviteRewardRecordRse;
    @Resource
    private ClientInviteProgressRewardFeign clientInviteProgressRewardFeign;
    @Resource
    private CommodityTypeFeign commodityTypeFeign;
    @Resource
    private ClientInviteProgressFeign clientInviteProgressFeign;
    @Resource
    PackageFeign packageFeign;
    @Resource
    OrderFeign orderFeign;
    @Resource
    UserDeviceFingerprintFeign userDeviceFingerprintFeign;
    @Resource
    private DictDataFeign dictDataFeign;
    @Resource
    private DistributedLock distributedLock;

    /**
     * 客户端获取奖励汇总
     *
     * @return
     */
    public List<RewardSummaryVo> clientGetRewardSummary() {
        // 获取用户信息
        R<UserCacheVo> userR = userFeign.getLocalUser();
        if (userR == null || userR.getData() == null) {
            throw new RRException("用户信息不存在");
        }
        UserCacheVo user = userR.getData();
        Long userId = user.getId();
        Long tenantId = user.getActiveTenantId();

        // 如果当前登录用户是子账号直接赋值主账号id给当前用户id
        if (user.getUserType() == 2) {
            userId = user.getParentId();
        }

        // 获取活动信息
        R<ClientInviteActivityInfoVo> activityInfoVoR = this.activityFeign.infoActivateById(null);
        ClientInviteActivityInfoVo activityInfoVo = activityInfoVoR.getData();
        if (Objects.isNull(activityInfoVo)) {
            // 活动已过期或不存在直接返回空集合
            return List.of();
        }

        // 获取当前用户邀请码根据活动id和主账号用户id
        InviteUrlCodeInfoDto inviteUrlCodeInfoDto = inviteUrlCodeFeign.getUserInviteUrlCodeByActivityIdUserId(activityInfoVo.getId(), userId);
        if (Objects.isNull(inviteUrlCodeInfoDto)) {
            // 用户未生成邀请码直接空集合
            return List.of();
        }
        // 获取总邀请人数
        Long inviteNumber = userInviteFeign.getInviteUserNumberByInviteUserId(userId);
        if (inviteNumber == null || inviteNumber <= 0) {
            // 用户未邀请有效用户直接返回空集合
            return List.of();
        }

        // 获取邀请用户所有的奖励
        List<ClientInviteRewardRecordInfoBo> clientInviteRewardRecordInfoBoList = clientInviteRewardRecordRse.listByRewardUserIdAndRewardTargetTypeAndRewardStatus(userId, 0, 1);
        if (CollectionUtil.isEmpty(clientInviteRewardRecordInfoBoList)) {
            return List.of();
        }
        // 获取用户所有的进度奖励明细
        List<Long> progressRewardIds = clientInviteRewardRecordInfoBoList.stream().map(ClientInviteRewardRecordInfoBo::getProgressRewardId).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(progressRewardIds)) {
            return List.of();
        }
        R<List<ClientInviteProgressRewardInfoVo>> progressRewardList = clientInviteProgressRewardFeign.listByProgressRewardIds(progressRewardIds);
        if (CollectionUtil.isEmpty(progressRewardList.getData())) {
            return List.of();
        }
        Map<Long, ClientInviteProgressRewardInfoVo> progressRewardInfoVoMap = progressRewardList.getData().stream().collect(Collectors.toMap(ClientInviteProgressRewardInfoVo::getId, progressRewardInfoVo -> progressRewardInfoVo));

        // 查询所有增量包商品集合
        R<List<CommodityTypeInfoVo>> commodityTypeList = commodityTypeFeign.listAll();
        if (CollectionUtil.isEmpty(commodityTypeList.getData())) {
            return List.of();
        }
        // 获取所有版本套餐
        R<List<PackageInfoVo>> packageListR = packageFeign.listAll(1);
        if (CollectionUtil.isEmpty(packageListR.getData())) {
            return List.of();
        }
        Map<Long, CommodityTypeInfoVo> commodityMap = commodityTypeList.getData().stream().collect(Collectors.toMap(CommodityTypeInfoVo::getId, s -> s));
        Map<Long, PackageInfoVo> packageAllMap = packageListR.getData().stream().collect(Collectors.toMap(PackageInfoVo::getId, s -> s));

        // 设置商品的id和赠送的数量到用户奖励记录列表中
        clientInviteRewardRecordInfoBoList.forEach(item -> {
            if (Objects.nonNull(item.getProgressRewardId()) && progressRewardInfoVoMap.containsKey(item.getProgressRewardId())) {
                Long progressRewardId = item.getProgressRewardId();
                ClientInviteProgressRewardInfoVo progressRewardInfoVo = progressRewardInfoVoMap.get(progressRewardId);
                if (Objects.nonNull(progressRewardInfoVo) && Objects.nonNull(progressRewardInfoVo.getRewardType()) && progressRewardInfoVo.getRewardType() == 0) {
                    item.setPackageId(progressRewardInfoVo.getPackageId());
                    item.setPackagePriceId(progressRewardInfoVo.getPackagePriceId());
                } else {
                    item.setCommodityTypeId(progressRewardInfoVo.getCommodityTypeId());
                    item.setCommodityNumber(progressRewardInfoVo.getCommodityNumber());
                }
            }
        });
        // 分组获取到用户所有的增量包奖励
        List<RewardSummaryVo> rewardSummaryVoList = new ArrayList<>();
        RewardSummaryVo inviteNumberRewardSummary = new RewardSummaryVo("总邀请人数", inviteNumber, "人");
        rewardSummaryVoList.add(inviteNumberRewardSummary);

        Map<Long, List<ClientInviteRewardRecordInfoBo>> packageMap = clientInviteRewardRecordInfoBoList.stream().filter(s -> Objects.nonNull(s) && Objects.nonNull(s.getPackageId())).collect(Collectors.groupingBy(ClientInviteRewardRecordInfoBo::getPackageId));
        Map<Long, List<ClientInviteRewardRecordInfoBo>> commodityTypeMap = clientInviteRewardRecordInfoBoList.stream().filter(s -> Objects.nonNull(s) && Objects.nonNull(s.getCommodityTypeId())).collect(Collectors.groupingBy(ClientInviteRewardRecordInfoBo::getCommodityTypeId));
        if (!packageMap.isEmpty()) {
            // 如果是版本包相加
            packageMap.forEach((k, v) -> {
                if (packageAllMap.containsKey(k)) {
                    PackageInfoVo packageInfoVo = packageAllMap.get(k);
                    Map<Long, CommodityPriceVo> commodityPriceVoMap = packageInfoVo.getCommodityPriceList().stream().collect(Collectors.toMap(CommodityPriceVo::getId, Function.identity(), (ex, replace) -> ex));
                    RewardSummaryVo rewardSummaryVo = new RewardSummaryVo();
                    // 设置label
                    rewardSummaryVo.setRewardLabel(packageInfoVo.getName());
                    if (CollectionUtil.isNotEmpty(v) && v.size() > 1) {
                        List<ValidityPeriod> validityPeriods = v.stream().map(s -> {
                            Long packagePriceId = s.getPackagePriceId();
                            if (!commodityPriceVoMap.containsKey(packagePriceId)) {
                                return null;
                            }
                            CommodityPriceVo commodityPriceVo = commodityPriceVoMap.get(packagePriceId);
                            return new ValidityPeriod(commodityPriceVo.getValidityNum(), commodityPriceVo.getValidityUnit());
                        }).filter(Objects::nonNull).collect(Collectors.toList());

                        ValidityPeriod validityPeriod = ValidityPeriodCalculator.addWithOptimalUnit(validityPeriods);
                        rewardSummaryVo.setRewardNum(Long.valueOf(validityPeriod.getValidityNum()));
                        rewardSummaryVo.setRewardUnit(validityPeriod.getValidityName());
                    } else {
                        ClientInviteRewardRecordInfoBo rewardRecordInfoBo = v.get(0);
                        CommodityPriceVo commodityPriceVo = commodityPriceVoMap.get(rewardRecordInfoBo.getPackagePriceId());
                        List<ValidityPeriod> validityPeriods = Arrays.asList(new ValidityPeriod(commodityPriceVo.getValidityNum(), commodityPriceVo.getValidityUnit()));
                        ValidityPeriod validityPeriod = ValidityPeriodCalculator.addWithOptimalUnit(validityPeriods);
                        rewardSummaryVo.setRewardNum(Long.valueOf(validityPeriod.getValidityNum()));
                        rewardSummaryVo.setRewardUnit(validityPeriod.getValidityName());
                    }
                    rewardSummaryVoList.add(rewardSummaryVo);
                }
            });
        }
        if (!commodityTypeMap.isEmpty()) {
            // 如果是增加包相加
            commodityTypeMap.forEach((k, v) -> {
                if (commodityMap.containsKey(k)) {
                    CommodityTypeInfoVo commodityTypeInfoVo = commodityMap.get(k);
                    RewardSummaryVo rewardSummaryVo = new RewardSummaryVo();
                    // 设置label
                    rewardSummaryVo.setRewardLabel(commodityTypeInfoVo.getName());
                    // 设置单位
                    rewardSummaryVo.setRewardUnit(commodityTypeInfoVo.getUnit());
                    // 设置数量
                    rewardSummaryVo.setRewardNum(v.stream().map(s -> {
                        s.setCommodityNumber(CommodityTypeConvert.commodityNumberConvert(commodityTypeInfoVo.getCode(), s.getCommodityNumber()));
                        return s;
                    }).mapToLong(ClientInviteRewardRecordInfoBo::getCommodityNumber).sum());
                    rewardSummaryVoList.add(rewardSummaryVo);
                }
            });
        }
        return rewardSummaryVoList;
    }

    /**
     * 发放用户邀请奖励
     *
     * @param userInviteMqDto 用户邀请MQ数据传输对象
     * @return 处理结果：true-处理完成(成功或无需处理)，false-需要重试
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean giveUserInviteAward(UserInviteMqDto userInviteMqDto) {
        // 奖励规则code
        String rewardRuleCode = userInviteMqDto.getRewardRuleCode();
        // 验证用户指纹
        boolean validUserFingerprint = validUserFingerprint(userInviteMqDto);
        if (!validUserFingerprint) {
            return true;
        }
        // 查询用户邀请信息 如果是用户邀请用户才能进行奖励发放
        UserInviteDto userInviteDto = userInviteFeign.judgeUserInviteEffective(userInviteMqDto.getAcceptUserId());
        if (Objects.isNull(userInviteDto)) {
            return true;
        }
        // 获取邀请用户id
        Long inviteUserId = userInviteDto.getInviteUserId();
        // 获取邀请用户的租户id
        Long inviteUserTenantId = userFeign.getUserTenantId(inviteUserId).getData();
        if (Objects.isNull(inviteUserTenantId) || inviteUserTenantId == 0) {
            return true;
        }
        // 获取被邀请用户id
        Long passiveUserId = userInviteDto.getPassiveUserId();
        // 获取被邀请用户的租户id
        Long passiveUserTenantId = userFeign.getUserTenantId(passiveUserId).getData();
        if (Objects.isNull(passiveUserTenantId) || passiveUserTenantId == 0) {
            return true;
        }
        // 如果邀请人和被邀请人是同一人 不进行处理
        if (inviteUserId.longValue() == passiveUserId) {
            return true;
        }
        // 获取活动信息
        R<ClientInviteActivityInfoVo> activityInfoVoR = this.activityFeign.infoActivateById(null);
        ClientInviteActivityInfoVo activityInfoVo = activityInfoVoR.getData();
        if (Objects.isNull(activityInfoVo)) {
            // 活动已过期或不存在 直接不处理
            return true;
        }
        // 查询邀请人是否已经创建奖励记录
        List<ClientInviteRewardRecordInfoBo> rewardRecordInfoBos = clientInviteRewardRecordRse.listByActivityIdAndRewardTargetTypeAndRewardUserIdAndRewardSourceUserId(activityInfoVo.getId(), 0, inviteUserId, passiveUserId);
        // 邀请人最终奖励记录列表信息
        List<ClientInviteRewardRecordInfoBo> inviteRewardRecordList = new ArrayList<>();
        // 被邀请人最终奖励记录列表信息
        List<ClientInviteRewardRecordInfoBo> passiveRewardRecordList = new ArrayList<>();
        // 没有创建邀请人奖励记录 第一次进行邀请人奖励记录和被邀请人奖励记录创建
        if (CollectionUtil.isEmpty(rewardRecordInfoBos)) {
            // 创建邀请人和被邀请人奖励记录
            createRewardRecord(activityInfoVo.getId(), inviteUserId, passiveUserId, inviteUserTenantId, inviteRewardRecordList, passiveUserTenantId, passiveRewardRecordList);
        } else {
            // 添加邀请人奖励记录在最终处理列表中
            inviteRewardRecordList.addAll(rewardRecordInfoBos);
            // 查询被邀请人奖励记录
            List<ClientInviteRewardRecordInfoBo> passiveRewardRecordInfoBos = clientInviteRewardRecordRse.listByActivityIdAndRewardTargetTypeAndRewardUserIdAndRewardSourceUserId(activityInfoVo.getId(), 1, passiveUserId, inviteUserId);
            if (CollectionUtil.isNotEmpty(passiveRewardRecordInfoBos)) {
                // 添加被邀请人奖励记录在最终处理列表中
                passiveRewardRecordList.addAll(passiveRewardRecordInfoBos);
            }
        }
        // 组装最终调用业务方发送奖励接口所需要的集合
        List<InviteUserRewardDetailDto> inviteUserRewardDetailDtos = new ArrayList<>();
        // 调用邀请用户奖励记录进行组装数据
        buildSendRewardRecordDetail(activityInfoVo.getActivityName(), rewardRuleCode, inviteUserTenantId, inviteRewardRecordList, inviteUserRewardDetailDtos);
        // 调用被邀请用户奖励记录进行组装数据
        buildSendRewardRecordDetail(activityInfoVo.getActivityName(), rewardRuleCode, passiveUserTenantId, passiveRewardRecordList, inviteUserRewardDetailDtos);
        // 如果筛选后存在奖励记录发放 进行调用奖励发放接口并修改发放状态为已发放
        if (CollectionUtil.isNotEmpty(inviteUserRewardDetailDtos)) {
            // 调用业务端发送具体奖励
            orderFeign.addActivityOrder(inviteUserRewardDetailDtos);
            //修改发放状态
            clientInviteRewardRecordRse.updateRewardStatusByIds(inviteUserRewardDetailDtos.stream().map(InviteUserRewardDetailDto::getId).collect(Collectors.toList()));
        }
        return true;
    }

    /**
     * 组装发送奖励记录数据
     *
     * @param activityName         活动名称
     * @param rewardRuleCode       进度code
     * @param rewardTenantId       邀请人或被邀请人租户id
     * @param rewardRecordList     邀请人或被邀请人奖励记录列表
     * @param userRewardDetailDtos 最终筛选后的奖励记录数据Dto
     */
    private void buildSendRewardRecordDetail(String activityName, String rewardRuleCode, Long rewardTenantId, List<ClientInviteRewardRecordInfoBo> rewardRecordList, List<InviteUserRewardDetailDto> userRewardDetailDtos) {
        if (CollectionUtil.isNotEmpty(rewardRecordList)) {
            // 判断邀请用户或被邀请用户是否已经发放过奖励
            List<ClientInviteRewardRecordInfoBo> inviteCollect = rewardRecordList.stream().filter(item -> item.getProgressCode().equals(rewardRuleCode) && item.getRewardStatus() == 0 && item.getRewardTenantId().longValue() == rewardTenantId).collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(inviteCollect)) {
                // 根据奖励记录中的奖励规则id查询具体奖励信息
                R<List<ClientInviteProgressRewardInfoVo>> listByProgressRewardIds = clientInviteProgressRewardFeign.listByProgressRewardIds(inviteCollect.stream().map(ClientInviteRewardRecordInfoBo::getProgressRewardId).collect(Collectors.toList()));
                if (CollectionUtil.isNotEmpty(listByProgressRewardIds.getData())) {
                    Map<Long, ClientInviteProgressRewardInfoVo> progressRewardInfoVoMap = listByProgressRewardIds.getData().stream().collect(Collectors.toMap(ClientInviteProgressRewardInfoVo::getId, item -> item));
                    inviteCollect.forEach(item -> {
                        ClientInviteProgressRewardInfoVo progressRewardInfoVo = progressRewardInfoVoMap.get(item.getProgressRewardId());
                        InviteUserRewardDetailDto inviteUserRewardDetailDto = new InviteUserRewardDetailDto();
                        inviteUserRewardDetailDto.setId(item.getId());
                        inviteUserRewardDetailDto.setUserId(item.getRewardUserId());
                        inviteUserRewardDetailDto.setTenantId(item.getRewardTenantId());
                        inviteUserRewardDetailDto.setRewardType(progressRewardInfoVo.getRewardType());
                        inviteUserRewardDetailDto.setPackageId(progressRewardInfoVo.getPackageId());
                        inviteUserRewardDetailDto.setPackagePriceId(progressRewardInfoVo.getPackagePriceId());
                        inviteUserRewardDetailDto.setCommodityTypeId(progressRewardInfoVo.getCommodityTypeId());
                        inviteUserRewardDetailDto.setCommodityNumber(progressRewardInfoVo.getCommodityNumber());
                        inviteUserRewardDetailDto.setValidityNum(progressRewardInfoVo.getValidityNum());
                        inviteUserRewardDetailDto.setValidityUnit(progressRewardInfoVo.getValidityUnit());
                        inviteUserRewardDetailDto.setActivityName(activityName);
                        userRewardDetailDtos.add(inviteUserRewardDetailDto);
                    });
                }
            }
        }
    }


    /**
     * 创建邀请人和被邀请人奖励记录
     *
     * @param activityId              活动id
     * @param inviteUserId            邀请人
     * @param passiveUserId           被邀请人
     * @param inviteUserTenantId      邀请人租户id
     * @param inviteRewardRecordList  邀请人返回奖励记录列表
     * @param passiveUserTenantId     被邀请人租户id
     * @param passiveRewardRecordList 被邀请人返回奖励记录列表
     */
    private void createRewardRecord(Long activityId, Long inviteUserId, Long passiveUserId, Long inviteUserTenantId, List<ClientInviteRewardRecordInfoBo> inviteRewardRecordList, Long passiveUserTenantId, List<ClientInviteRewardRecordInfoBo> passiveRewardRecordList) {
        // 查询邀请人活动进度
        List<ClientInviteProgressDto> inviteProgressList = clientInviteProgressFeign.listClientInviteProgressByActivityIdAndInviteProgressType(activityId, 1);
        if (CollectionUtil.isNotEmpty(inviteProgressList)) {
            // 查询邀请人活动进度下配置的奖励规则
            List<Long> progressIds = inviteProgressList.stream().map(ClientInviteProgressDto::getId).collect(Collectors.toList());
            List<ClientInviteProgressRewardDto> inviteProgressRewardDtos = clientInviteProgressRewardFeign.listByProgressIds(progressIds);
            if (CollectionUtil.isNotEmpty(inviteProgressRewardDtos)) {
                Map<Long, ClientInviteProgressDto> progressDtoMap = inviteProgressList.stream().collect(Collectors.toMap(ClientInviteProgressDto::getId, inviteProgress -> inviteProgress));
                inviteProgressRewardDtos.forEach(item -> {
                    ClientInviteRewardRecordInfoBo inviteRewardRecordInfoBo = new ClientInviteRewardRecordInfoBo();
                    inviteRewardRecordInfoBo.setId(SnowflakeManager.nextValue());
                    inviteRewardRecordInfoBo.setActivityId(activityId);
                    inviteRewardRecordInfoBo.setProgressId(item.getProgressId());
                    inviteRewardRecordInfoBo.setProgressRewardId(item.getId());
                    inviteRewardRecordInfoBo.setRewardTargetType(0);
                    inviteRewardRecordInfoBo.setRewardUserId(inviteUserId);
                    inviteRewardRecordInfoBo.setRewardSourceUserId(passiveUserId);
                    inviteRewardRecordInfoBo.setRewardTenantId(inviteUserTenantId);
                    inviteRewardRecordInfoBo.setProgressCode(progressDtoMap.get(item.getProgressId()).getInviteProgressCode());
                    inviteRewardRecordInfoBo.setRewardStatus(0L);
                    inviteRewardRecordInfoBo.setUpdateDate(new Date());
                    inviteRewardRecordInfoBo.setCreateDate(new Date());
                    inviteRewardRecordList.add(inviteRewardRecordInfoBo);
                });
            }
        }
        // 查询被邀请人活动进度
        List<ClientInviteProgressDto> passiveInviteProgressList = clientInviteProgressFeign.listClientInviteProgressByActivityIdAndInviteProgressType(activityId, 0);
        if (CollectionUtil.isNotEmpty(passiveInviteProgressList)) {
            // 查询被邀请人活动进度下配置的奖励规则
            List<Long> passiveProgressIds = passiveInviteProgressList.stream().map(ClientInviteProgressDto::getId).collect(Collectors.toList());
            List<ClientInviteProgressRewardDto> passiveInviteProgressRewardDtos = clientInviteProgressRewardFeign.listByProgressIds(passiveProgressIds);
            if (CollectionUtil.isNotEmpty(passiveInviteProgressRewardDtos)) {
                Map<Long, ClientInviteProgressDto> passiveProgressDtoMap = passiveInviteProgressList.stream().collect(Collectors.toMap(ClientInviteProgressDto::getId, inviteProgress -> inviteProgress));
                passiveInviteProgressRewardDtos.forEach(item -> {
                    ClientInviteRewardRecordInfoBo passiveInviteRewardRecordInfoBo = new ClientInviteRewardRecordInfoBo();
                    passiveInviteRewardRecordInfoBo.setId(SnowflakeManager.nextValue());
                    passiveInviteRewardRecordInfoBo.setActivityId(activityId);
                    passiveInviteRewardRecordInfoBo.setProgressId(item.getProgressId());
                    passiveInviteRewardRecordInfoBo.setProgressRewardId(item.getId());
                    passiveInviteRewardRecordInfoBo.setRewardTargetType(1);
                    passiveInviteRewardRecordInfoBo.setRewardUserId(passiveUserId);
                    passiveInviteRewardRecordInfoBo.setRewardSourceUserId(inviteUserId);
                    passiveInviteRewardRecordInfoBo.setRewardTenantId(passiveUserTenantId);
                    passiveInviteRewardRecordInfoBo.setProgressCode(passiveProgressDtoMap.get(item.getProgressId()).getInviteProgressCode());
                    passiveInviteRewardRecordInfoBo.setRewardStatus(0L);
                    passiveInviteRewardRecordInfoBo.setCreateDate(new Date());
                    passiveInviteRewardRecordInfoBo.setUpdateDate(new Date());
                    passiveRewardRecordList.add(passiveInviteRewardRecordInfoBo);
                });
            }
        }
        if (CollectionUtil.isNotEmpty(inviteRewardRecordList)) {
            clientInviteRewardRecordRse.batchSave(inviteRewardRecordList);
        }
        if (CollectionUtil.isNotEmpty(passiveRewardRecordList)) {
            clientInviteRewardRecordRse.batchSave(passiveRewardRecordList);
        }
    }

    /**
     * 后台获取奖励列表
     *
     * @param clientInviteRewardRecordListBo 查询参数
     * @return
     */
    public R<PageUtils<ClientInviteRewardRecordInfoVo>> listByBack(ClientInviteRewardRecordListBo clientInviteRewardRecordListBo) {

        if (!StringUtils.isEmpty(clientInviteRewardRecordListBo.getInviterName())) {
            R<List<Long>> userIdsR = userFeign.listByNameOrPhone(clientInviteRewardRecordListBo.getInviterName());
            if (userIdsR.getData() == null || userIdsR.getData().size() < 1) {
                List<Long> userIds = new LinkedList<>();
                userIds.add(0L);
                clientInviteRewardRecordListBo.setInviterUserIds(userIds);
            } else {
                clientInviteRewardRecordListBo.setInviterUserIds(userIdsR.getData());
            }
        }
        if (!StringUtils.isEmpty(clientInviteRewardRecordListBo.getInviteeName())) {
            R<List<Long>> userIdsR = userFeign.listByNameOrPhone(clientInviteRewardRecordListBo.getInviteeName());
            if (userIdsR.getData() == null || userIdsR.getData().size() < 1) {
                List<Long> userIds = new LinkedList<>();
                userIds.add(0L);
                clientInviteRewardRecordListBo.setInviteeUserIds(userIds);
            } else {
                clientInviteRewardRecordListBo.setInviteeUserIds(userIdsR.getData());
            }
        }

        PageUtils<ClientInviteRewardRecordInfoVo> pageUtils = this.clientInviteRewardRecordRse.listByBack(clientInviteRewardRecordListBo);

        List<ClientInviteRewardRecordInfoVo> rewardRecordInfoVos = pageUtils.getList();
        if (rewardRecordInfoVos != null && rewardRecordInfoVos.size() > 0) {
            // 填充用户的名称
            Set<Long> userIds = rewardRecordInfoVos.stream().flatMap(item -> Stream.of(item.getRewardUserId(), item.getRewardSourceUserId())).collect(Collectors.toSet());
            List<UserDto> userList = this.userFeign.listByIds(userIds);
            if (userList != null && userList.size() > 0) {
                Map<Long, UserDto> userMap = userList.stream().collect(Collectors.toMap(UserDto::getId, Function.identity(), (a, b) -> a));

                for (ClientInviteRewardRecordInfoVo rewardRecordInfoVo : rewardRecordInfoVos) {

                    UserDto rewardUser = userMap.get(rewardRecordInfoVo.getRewardUserId());
                    if (rewardUser != null) {
                        rewardRecordInfoVo.setInviterName(rewardUser.getNickName());
                    }
                    UserDto rewardSourceUser = userMap.get(rewardRecordInfoVo.getRewardSourceUserId());
                    if (rewardSourceUser != null) {
                        rewardRecordInfoVo.setInviteeName(rewardSourceUser.getNickName());
                    }
                }
            }

            // 填充奖励明细
            Set<Long> progressRewardIds = new HashSet<>();
            for (ClientInviteRewardRecordInfoVo rewardRecordInfoVo : rewardRecordInfoVos) {
                progressRewardIds.addAll(rewardRecordInfoVo.getProgressRewardIds());
            }
            // 获取进度奖励列表
            R<List<ClientInviteProgressRewardInfoVo>> progressRewardListR = this.clientInviteProgressRewardFeign.listByProgressRewardIds(progressRewardIds);
            List<ClientInviteProgressRewardInfoVo> progressRewardList = progressRewardListR.getData();
            if (progressRewardList != null && progressRewardList.size() > 0) {
                // 获取商品类型列表
                R<List<CommodityTypeInfoVo>> commodityTypeListR = this.commodityTypeFeign.listAll();
                List<CommodityTypeInfoVo> commodityTypeList = commodityTypeListR.getData();
                if (commodityTypeList != null && commodityTypeList.size() > 0) {
                    // 商品map
                    Map<Long, CommodityTypeInfoVo> commodityTypeMap = commodityTypeList.stream().collect(Collectors.toMap(CommodityTypeVo::getId, Function.identity()));
                    // 进度奖励map
                    Map<Long, ClientInviteProgressRewardInfoVo> progressRewardMap = progressRewardList.stream().collect(Collectors.toMap(ClientInviteProgressRewardVo::getId, Function.identity()));
                    // 进度code字典
                    Map<String, String> rewardCodeMap = new HashMap<>();
                    List<DictDataListVo> dictDataListVos = dictDataFeign.dictDataListByCode("invite_user_reward_code");
                    if (dictDataListVos != null && dictDataListVos.size() > 0) {
                        rewardCodeMap = dictDataListVos.stream().collect(Collectors.toMap(DictDataVo::getValue, DictDataVo::getLabel));
                    }

                    for (ClientInviteRewardRecordInfoVo rewardRecordInfoVo : rewardRecordInfoVos) {
                        // 设置进度的所有奖励
                        List<String> rewardList = new LinkedList<>();
                        for (Long progressRewardId : rewardRecordInfoVo.getProgressRewardIds()) {
                            ClientInviteProgressRewardInfoVo progressRewardInfoVo = progressRewardMap.get(progressRewardId);
                            if (progressRewardInfoVo != null) {
                                CommodityTypeInfoVo commodityTypeInfoVo = commodityTypeMap.get(progressRewardInfoVo.getCommodityTypeId());
                                if (commodityTypeInfoVo != null) {
                                    long commodityNumber = CommodityTypeConvert.commodityNumberConvert(commodityTypeInfoVo.getCode(), progressRewardInfoVo.getCommodityNumber());
                                    rewardList.add(commodityTypeInfoVo.getName() + commodityNumber + commodityTypeInfoVo.getUnit());
                                }
                            }
                        }
                        rewardRecordInfoVo.setRewardList(rewardList);

                        // 设置进度code对应的label
                        rewardRecordInfoVo.setProgressCodeStr(rewardCodeMap.get(rewardRecordInfoVo.getProgressCode()));
                    }
                }

            }

        }

        return R.ok(pageUtils);
    }

    /**
     * 获取用户邀请奖励记录列表
     *
     * @param userRewardBo
     * @return
     */
    public PageUtils<ClientUserRewardRecordVo> clientGetUserRewardList(UserRewardBo userRewardBo) {
        Long userId = userFeign.getCurrentUserParentId();
        if (Objects.isNull(userId)) {
            throw new RRException("用户信息不存在");
        }
        // 1.分页获取邀请人已发放的奖励记录
        IPage<ClientInviteRewardRecordGroupEntity> rewardRecordEntityIPage = clientInviteRewardRecordRse.pageUserRewardListByRewardUserIdAndRewardTargetTypeGroupByProgressId(userId, 0, userRewardBo.getPage(), userRewardBo.getLimit());
        List<ClientInviteRewardRecordGroupEntity> records = rewardRecordEntityIPage.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return new PageUtils<>(rewardRecordEntityIPage, List.of());
        }
        Set<Long> rewardSourceUserIds = records.stream().map(ClientInviteRewardRecordEntity::getRewardSourceUserId).collect(Collectors.toSet());
        if (CollectionUtil.isEmpty(rewardSourceUserIds)) {
            return new PageUtils<>(rewardRecordEntityIPage, List.of());
        }

        List<Long> progressRewardIds = records.stream().map(ClientInviteRewardRecordGroupEntity::getProgressRewardIdStr).filter(str -> str != null && !str.trim().isEmpty()).flatMap(str -> Arrays.stream(str.split("_"))).map(String::trim).filter(s -> !s.isEmpty()).map(s -> {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }).filter(id -> id != null).distinct().collect(Collectors.toList());

        if (CollectionUtil.isEmpty(progressRewardIds)) {
            return new PageUtils<>(rewardRecordEntityIPage, List.of());
        }
        List<UserDto> userList = userFeign.listByIds(rewardSourceUserIds);
        if (CollectionUtil.isEmpty(userList)) {
            return new PageUtils<>(rewardRecordEntityIPage, List.of());
        }
        R<List<ClientInviteProgressRewardInfoVo>> progressRewardList = clientInviteProgressRewardFeign.listByProgressRewardIds(progressRewardIds);
        if (CollectionUtil.isEmpty(progressRewardList.getData())) {
            return new PageUtils<>(rewardRecordEntityIPage, List.of());
        }
        List<ClientUserRewardRecordVo> clientUserRewardRecordVos = new ArrayList<>();
        Map<Long, UserDto> userDtoMap = userList.stream().collect(Collectors.toMap(UserDto::getId, Function.identity(), (existing, replacement) -> existing));
        Map<Long, ClientInviteProgressRewardInfoVo> progressRewardInfoVoMap = progressRewardList.getData().stream().collect(Collectors.toMap(ClientInviteProgressRewardInfoVo::getId, Function.identity(), (existing, replacement) -> existing));

        // 获取所有商品类型
        R<List<CommodityTypeInfoVo>> commodityTypeListR = commodityTypeFeign.listAll();
        List<CommodityTypeInfoVo> commodityTypeList = commodityTypeListR.getData();
        // 获取所有版本套餐
        R<List<PackageInfoVo>> packageListR = packageFeign.listAll(1);
        List<PackageInfoVo> packageList = packageListR.getData();
        Map<Long, PackageInfoVo> packageInfoVoMap = packageList.stream().collect(Collectors.toMap(PackageInfoVo::getId, Function.identity(), (ex, replace) -> ex));
        Map<Long, CommodityTypeInfoVo> commodityTypeInfoVoMap = commodityTypeList.stream().collect(Collectors.toMap(CommodityTypeInfoVo::getId, Function.identity(), (ex, replace) -> ex));

        records.forEach(item -> {
            UserDto userDto = userDtoMap.get(item.getRewardSourceUserId());
            ClientUserRewardRecordVo clientUserRewardRecordVo = new ClientUserRewardRecordVo();
            clientUserRewardRecordVos.add(clientUserRewardRecordVo);
            clientUserRewardRecordVo.setUserNickName(Objects.nonNull(userDto) ? userDto.getNickName() : Constant.UnKnownDataEnum.UN_KNOWN_DATA_SHOW.getTitle());
            clientUserRewardRecordVo.setPhone(Objects.nonNull(userDto) ? DesensitizedUtil.mobilePhone(userDto.getPhone()) : Constant.UnKnownDataEnum.UN_KNOWN_DATA_SHOW.getTitle());
            clientUserRewardRecordVo.setProgress(InviteRewardRuleCodeEnum.getNameByCode(item.getProgressCode()));
            clientUserRewardRecordVo.setRewardDate(item.getSendDate());
            if (StrUtil.isBlank(item.getProgressRewardIdStr())) {
                clientUserRewardRecordVo.setRewardDetailList(List.of());
                return;
            }
            List<ClientUserRewardRecordDetailVo> rewardDetailList = new ArrayList<>();
            clientUserRewardRecordVo.setRewardDetailList(rewardDetailList);
            List<Long> itemProgressRewardIds = Arrays.stream(item.getProgressRewardIdStr().split("_")).map(Long::parseLong).collect(Collectors.toList());
            itemProgressRewardIds.forEach(itemProgressRewardId -> {
                ClientInviteProgressRewardInfoVo progressRewardInfoVo = progressRewardInfoVoMap.get(itemProgressRewardId);
                if (Objects.isNull(progressRewardInfoVo)) {
                    return;
                }
                // if-奖励类型为版本 else-奖励类型为增量包
                if (progressRewardInfoVo.getRewardType() == 0) {
                    if (!packageInfoVoMap.containsKey(progressRewardInfoVo.getPackageId())) {
                        return;
                    }
                    PackageInfoVo packageInfoVo = packageInfoVoMap.get(progressRewardInfoVo.getPackageId());
                    Map<Long, CommodityPriceVo> commodityPriceVoMap = packageInfoVo.getCommodityPriceList().stream().collect(Collectors.toMap(CommodityPriceVo::getId, Function.identity(), (ex, replace) -> ex));
                    if (!commodityPriceVoMap.containsKey(progressRewardInfoVo.getPackagePriceId())) {
                        return;
                    }
                    CommodityPriceVo commodityPriceVo = commodityPriceVoMap.get(progressRewardInfoVo.getPackagePriceId());
                    ClientUserRewardRecordDetailVo rewardDetailVo = new ClientUserRewardRecordDetailVo();
                    rewardDetailVo.setRewardType(packageInfoVo.getName());
                    rewardDetailVo.setRewardNumber(Long.valueOf(commodityPriceVo.getValidityNum()));
                    rewardDetailVo.setRewardUnit(ValidityUnitEnum.getNameByCode(commodityPriceVo.getValidityUnit()));
                    rewardDetailList.add(rewardDetailVo);

                } else {
                    if (!commodityTypeInfoVoMap.containsKey(progressRewardInfoVo.getCommodityTypeId())) {
                        return;
                    }
                    CommodityTypeInfoVo commodityTypeInfoVo = commodityTypeInfoVoMap.get(progressRewardInfoVo.getCommodityTypeId());
                    ClientUserRewardRecordDetailVo rewardDetailVo = new ClientUserRewardRecordDetailVo();
                    rewardDetailVo.setRewardType(commodityTypeInfoVo.getName());
                    rewardDetailVo.setRewardUnit(commodityTypeInfoVo.getUnit());
                    rewardDetailVo.setRewardNumber(CommodityTypeConvert.commodityNumberConvert(commodityTypeInfoVo.getCode(), progressRewardInfoVo.getCommodityNumber()));
                    rewardDetailList.add(rewardDetailVo);
                }
            });
        });
        return new PageUtils<>(rewardRecordEntityIPage, clientUserRewardRecordVos);
    }

    /**
     * 验证用户指纹
     *
     * @param userInviteMqDto MQ传输dto数据
     * @return true-有效，false-无效
     */
    private boolean validUserFingerprint(UserInviteMqDto userInviteMqDto) {
        // 获取奖励规则代码
        String rewardRuleCode = userInviteMqDto.getRewardRuleCode();
        // 获取用户ID
        Long userId = userInviteMqDto.getAcceptUserId();
        // 获取设备指纹
        String fingerprint = userInviteMqDto.getFingerprint();
        log.info("验证用户指纹: userId={}, fingerprint={}, rewardRuleCode={}", userId, fingerprint, rewardRuleCode);
        if (Objects.isNull(userId)) {
            log.warn("用户id为空，无法验证。");
            return false;
        }
        // 奖励规则代码为空，不能发放奖励
        if (StrUtil.isBlank(rewardRuleCode)) {
            log.warn("用户奖励规则代码为空，无法验证。");
            return false;
        }
        // 用户id为空，不能发放奖励
        if (StrUtil.isBlank(fingerprint)) {
            log.warn("用户指纹为空，无法验证，userId={}", userId);
            return false;
        }
        // 验证指纹合法性
        if (!fingerprint.startsWith("desktop_") && !fingerprint.startsWith("web_")) {
            log.warn("用户指纹不合法，fingerprint={}", fingerprint);
            return false;
        }
        // 获取指纹失败
        if (fingerprint.toLowerCase().contains("error")) {
            log.warn("用户指纹包含error错误代码，不处理！fingerprint={}", fingerprint);
            return false;
        }

        // 查询设备指纹信息
        Integer deviceType = fingerprint.startsWith("desktop_") ? 0 : 1;
        try {
            UserDeviceFingerprintDto deviceFingerprintDto = userDeviceFingerprintFeign.getUserDeviceFingerprintByFingerprintAndDeviceType(fingerprint, deviceType);
            // 1. 注册场景
            if (InviteRewardRuleCodeEnum.REGISTER.getCode().equals(rewardRuleCode)) {
                // 注册场景：来源于浏览器没有基础数据所以满足邀请条件即可领取
                // 注册时会在tb_device_fingerprint保存设备当前用户id和设备指纹信息绑定
                // 如果指纹已存在（之前已有用户用过该设备），则不发放奖励
                if (deviceFingerprintDto != null) {
                    if (!userId.equals(deviceFingerprintDto.getUserId())) {
                        log.debug("注册场景：当前指纹已存在，指纹绑定的用户ID与当前用户不匹配，不发放奖励, userId={}, fingerprint={}, existingUserId={}", userId, fingerprint, deviceFingerprintDto.getUserId());
                        return false;
                    }
                    return true;
                }

                // 保存设备指纹信息
                UserDeviceFingerprintDto newDeviceDto = new UserDeviceFingerprintDto();
                newDeviceDto.setId(SnowflakeManager.nextValue());
                newDeviceDto.setUserId(userId);
                newDeviceDto.setFingerprint(fingerprint);
                newDeviceDto.setDeviceType(deviceType); // 0-desktop , 1-web
                userDeviceFingerprintFeign.saveUserDeviceFingerprint(newDeviceDto);

                log.debug("注册场景：新设备指纹，发放奖励, userId={}, fingerprint={}", userId, fingerprint);
                return true;
            }

            // 2. 登录场景
            else if (InviteRewardRuleCodeEnum.DOWNLOAD.getCode().equals(rewardRuleCode)) {
                // 2.1 web登录直接不发放奖励
                if (fingerprint.startsWith("web_")) {
                    // 检查指纹是否已存在
                    if (deviceFingerprintDto == null) {
                        // 指纹不存在，记录新指纹
                        UserDeviceFingerprintDto newDeviceDto = new UserDeviceFingerprintDto();
                        newDeviceDto.setId(SnowflakeManager.nextValue());
                        newDeviceDto.setUserId(userId);
                        newDeviceDto.setFingerprint(fingerprint);
                        newDeviceDto.setDeviceType(deviceType);
                        userDeviceFingerprintFeign.saveUserDeviceFingerprint(newDeviceDto);
                        log.debug("登录场景：Web登录新指纹已记录，不发放奖励, userId={}, fingerprint={}", userId, fingerprint);
                    } else {
                        log.debug("登录场景：Web登录指纹已存在，不发放奖励, userId={}, fingerprint={}", userId, fingerprint);
                    }
                    return false;
                }

                // 2.2 客户端登录，指纹已存在
                if (deviceFingerprintDto != null) {
                    // 判断当前用户id是否和指纹信息的用户id不匹配，不匹配不发放
                    if (!userId.equals(deviceFingerprintDto.getUserId())) {
                        log.debug("登录场景：指纹绑定的用户ID与当前用户不匹配，不发放奖励, userId={}, fingerprint={}, existingUserId={}", userId, fingerprint, deviceFingerprintDto.getUserId());
                        return false;
                    }
                    return true;
                }

                // 2.3 客户端登录，指纹不存在
                // 存储当前用户id和指纹绑定，进行新设备邀请登录奖励发放
                UserDeviceFingerprintDto newDeviceDto = new UserDeviceFingerprintDto();
                newDeviceDto.setId(SnowflakeManager.nextValue());
                newDeviceDto.setUserId(userId);
                newDeviceDto.setFingerprint(fingerprint);
                newDeviceDto.setDeviceType(deviceType);
                userDeviceFingerprintFeign.saveUserDeviceFingerprint(newDeviceDto);

                log.debug("登录场景：用户在新设备上登录，发放奖励, userId={}, fingerprint={}", userId, fingerprint);
                return true;
            }

            // 3.录制分析直播2场奖励 和 4.使用AI助手消耗 - 逻辑相同
            else if (InviteRewardRuleCodeEnum.USE.getCode().equals(rewardRuleCode) || InviteRewardRuleCodeEnum.AI.getCode().equals(rewardRuleCode)) {

                // 3.1/4.1 指纹不存在：存储当前用户id和指纹绑定，进行新设备邀请奖励发放
                if (deviceFingerprintDto != null) {
                    // 3.2/4.2 指纹存在：判断当前用户id是否和指纹信息的用户id相匹配，不匹配不发放
                    if (!userId.equals(deviceFingerprintDto.getUserId())) {
                        log.debug("功能使用场景：指纹绑定的用户ID与当前用户不匹配，不发放奖励, userId={}, fingerprint={}, existingUserId={}, rewardRuleCode={}", userId, fingerprint, deviceFingerprintDto.getUserId(), rewardRuleCode);
                        return false;
                    }
                    log.debug("功能使用场景：用户在已绑定设备上使用功能，发放奖励, userId={}, fingerprint={}, rewardRuleCode={}", userId, fingerprint, rewardRuleCode);
                    return true;
                }

                UserDeviceFingerprintDto newDeviceDto = new UserDeviceFingerprintDto();
                newDeviceDto.setId(SnowflakeManager.nextValue());
                newDeviceDto.setUserId(userId);
                newDeviceDto.setFingerprint(fingerprint);
                newDeviceDto.setDeviceType(deviceType);  // 0-desktop , 1-web
                userDeviceFingerprintFeign.saveUserDeviceFingerprint(newDeviceDto);

                log.debug("功能使用场景：新设备，发放奖励, userId={}, fingerprint={}, rewardRuleCode={}", userId, fingerprint, rewardRuleCode);
                return true;
            }

            // 其他场景默认返回false
            log.debug("未知奖励规则代码，不发放奖励, rewardRuleCode={}, userId={}, fingerprint={}", rewardRuleCode, userId, fingerprint);
            return false;
        }catch (Exception e) {
            log.warn("[验证指纹] 用户指纹已存在，主从架构查询同步手动进行重试！ rewardRuleCode={}, userId={}, fingerprint={}", rewardRuleCode, userId, fingerprint);
            throw new BusinessException(StatusCode.NEED_RETRY);
        }
    }
}

