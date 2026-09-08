package com.jiuyu.replay.order.bean.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.common.properties.RocketMqProperties;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.constant.activity.InviteRewardRuleCodeEnum;
import com.jiuyu.replay.generic.dto.activity.UserInviteDto;
import com.jiuyu.replay.generic.dto.activity.UserInviteMqDto;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.activity.UserInviteFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.TypeSurplusBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsBo;
import com.jiuyu.replay.order.bo.UserPropertyTypeBo;
import com.jiuyu.replay.order.entity.PropertyDetailsTokenEntity;
import com.jiuyu.replay.order.producer.*;
import com.jiuyu.replay.order.producer.impl.*;
import com.jiuyu.replay.order.repository.service.PropertyDetailsTokenService;
import com.jiuyu.replay.order.repository.service.impl.PropertyDetailsTokenServiceImpl;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;
import com.jiuyu.replay.order.vo.TypeSurplusInfoVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

/**
 * @ClassName : UserPropertyBean
 * @Description : 用于做资产的加减
 */
public class UserPropertyImpl {

    public RedisTemplate redisTemplate;
    public UserPropertyProducer userPropertyProducer;
    public OrderProducer orderProducer;
    public OrderDetailProducer orderDetailProducer;
    public TypeSurplusProducer typeSurplusProducer;
    public UserPropertyTypeProducer userPropertyTypeProducer;
    private UserPropertyDetailsProducer userPropertyDetailsProducer;
    public CommodityTypeProducer commodityTypeProducer;
    private PropertyDetailsTokenService propertyDetailsTokenService;
    private RocketMqBll rocketMqBll;
    private RocketMqProperties rocketMqProperties;
    private UserFeign userFeign;
    private UserInviteFeign userInviteFeign;

    // redis中资源剩余信息
    private UserPropertyTypeInfoVo redisSurplus;
    // 当前用户要使用的资产对象
    private UserPropertyTypeInfoVo useUserPropertyType;
    private AssetsMinusOrPlusBo assets;
    // 用户id
    private Long userId;
    private String userName;
    private String commodityCode;
    // 本次使用的数量 正数为增加，负数为减少
    private Long thisUseNum;
    // 当前用户使用的资产id
    private Long propertyId;
    private UserDto user;

    // 构造方法注入
    private UserPropertyImpl() {
        this.create();
    }

    private void create() {
        redisTemplate = ApplicationContextUtil.getBean(StringRedisTemplate.class);
        userPropertyProducer = ApplicationContextUtil.getBean(UserPropertyProducerImpl.class);
        orderProducer = ApplicationContextUtil.getBean(OrderProducerImpl.class);
        orderDetailProducer = ApplicationContextUtil.getBean(OrderDetailProducerImpl.class);
        typeSurplusProducer = ApplicationContextUtil.getBean(TypeSurplusProducerImpl.class);
        userPropertyTypeProducer = ApplicationContextUtil.getBean(UserPropertyTypeProducerImpl.class);
        userPropertyDetailsProducer = ApplicationContextUtil.getBean(UserPropertyDetailsProducerImpl.class);
        commodityTypeProducer = ApplicationContextUtil.getBean(CommodityTypeProducerImpl.class);
        propertyDetailsTokenService = ApplicationContextUtil.getBean(PropertyDetailsTokenServiceImpl.class);
        rocketMqBll = ApplicationContextUtil.getBean(RocketMqBll.class);
        rocketMqProperties = ApplicationContextUtil.getBean(RocketMqProperties.class);
        userFeign = ApplicationContextUtil.getBean(UserFeign.class);
        userInviteFeign = ApplicationContextUtil.getBean(UserInviteFeign.class);
    }


    /**
     * 获取用户资产信息
     */
    private void setUserPropertyBll() {
        List<UserPropertyTypeInfoVo> userProperty = userPropertyProducer.getUserProperty(this.userId);
        // 获取当前用户要加减的资产
        userProperty.stream().filter(item -> this.commodityCode.equals(item.getCommodityTypeCode())).findFirst().ifPresent(typeInfoVo -> redisSurplus = typeInfoVo);
        if (redisSurplus == null) RRException.create(StrUtil.format("当前用户没有该类型的资源"));
    }

    /**
     * 判断资产是否足够
     *
     * @return
     */
    private boolean isHave() {
        this.setUserPropertyBll();
        Long useQuantity = redisSurplus.getUseQuantity();
        Long totalQuantity = redisSurplus.getTotalQuantity();
        long surplus = NumberUtil.sub(totalQuantity, useQuantity).longValue();
        return NumberUtil.compare(surplus, this.thisUseNum) >= 0;
    }

    public boolean isHave(AssetsMinusOrPlusBo assets) {
        this.assets = assets;
        this.userId = assets.getUserId();
        this.thisUseNum = Convert.toLong(assets.getNum());
        return this.isHave();
    }

    /**
     * 使用资产的入口
     *
     * @param assets 参数
     */
    public static void use(AssetsMinusOrPlusBo assets) {
        UserPropertyImpl userProperty = new UserPropertyImpl();
        userProperty.formalUse(assets);
    }


    /**
     * 正式使用资产
     *
     * @param assets 参数
     */
    private void formalUse(AssetsMinusOrPlusBo assets) {
        this.assets = assets;
        this.commodityCode = assets.getCode();
        this.userId = assets.getUserId();
        this.userName = assets.getUserName();

        user = userFeign.userById(userId);
        RRException.isNotEmpty(user, StrUtil.format("扣资产时用户查询失败"));


        this.thisUseNum = Convert.toLong(assets.getNum());

        // 判断是否拥有
        if (NumberUtil.compare(this.thisUseNum, 0L) < 0 && !this.isHave()) {
            RRException.create(StrUtil.format("当前账号资产[{}]不足。", getCommodityCodeName()));
        }
        // 查询当前用的资产类型，这个类型是用父用户的还使用自己的
        this.propertyId = Convert.toLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, userId)));
        UserPropertyTypeInfoVo userPropertyTypeInfoVoR = userPropertyTypeProducer.getUserSRemainingAssets(propertyId, this.commodityCode);
        if (ObjectUtil.isNotEmpty(userPropertyTypeInfoVoR)) {
            this.useUserPropertyType = userPropertyTypeInfoVoR;
        } else {
            RRException.create(StrUtil.format("当前账号没有该类型的资源"));
        }
        // 更新用户资产表剩余数量
        if (useUserPropertyType.getCommodityTypeReset() == 1) {
            // 更新订单剩余的资源列表
            this.updateTypeSurplus();

            Long currentSurplusCount = typeSurplusProducer.currentSurplusCount(this.useUserPropertyType.getPropertyId(), this.useUserPropertyType.getCommodityTypeId());
            // 更新当前用户使用资源-是可以重置的资产
            userPropertyTypeProducer.updateCurrent(this.useUserPropertyType.getId(), currentSurplusCount);
        } else {
            // 校验
            long s = this.useUserPropertyType.getTotalQuantity() - this.useUserPropertyType.getUseQuantity();
            if (this.thisUseNum < 0 && s < Math.abs(this.thisUseNum))
                RRException.create(StrUtil.format("当前账号[{}]资源不足。", getCommodityCodeName()));
            // 主用户使用资源-不可以重置资产
            UserPropertyTypeBo bo = new UserPropertyTypeBo();
            bo.setId(this.useUserPropertyType.getId());
            Long temp;
            if (NumberUtil.compare(this.thisUseNum, 0L) < 0) {
                temp = this.useUserPropertyType.getUseQuantity() + Math.abs(this.thisUseNum);
            } else {
                temp = this.useUserPropertyType.getUseQuantity() - Math.abs(this.thisUseNum);
            }
            if (temp < 0) temp = 0L;
            bo.setUseQuantity(this.useUserPropertyType.getTotalQuantity() < temp ? this.useUserPropertyType.getTotalQuantity() : temp);
            userPropertyTypeProducer.update(bo);
        }

        // 修改redis缓存和发送mq消息
        // 添加redis缓存
        this.setRedisCache();
        // 如果预扣，就把缓存删除
        if (ObjectUtil.isNotEmpty(assets.getWithholdId()) || ObjectUtil.isNotEmpty(assets.getRedisId()) && assets.getClearWithholdCache() != null) {
            assets.getClearWithholdCache().accept(assets.getWithholdId(), assets.getRedisId());
        }

        // 判断是否使用AI助手消耗
        if (StrUtil.isNotBlank(assets.getCode()) && assets.getCode().equals("aiTokenNum")) {
            if (Objects.nonNull(assets.getUserId())) {
                Long userId = assets.getUserId();
                UserInviteDto userInviteDto = userInviteFeign.judgeUserInviteEffective(userId);
                if (Objects.nonNull(userInviteDto)) {
                    // 发送用户邀请注册成功异步消息
                    UserInviteMqDto userInviteMqDto = new UserInviteMqDto(InviteRewardRuleCodeEnum.AI.getCode(), userId, RequestContext.getFingerprint());
                    rocketMqBll.syncSendNormalMessage(userId, rocketMqProperties.getTagUserInviteActivity(), IdUtil.simpleUUID(), JSON.toJSONString(userInviteMqDto));
                }
            }
        }
    }

    public void setRedisCache() {
        UserPropertyTypeInfoVo info = userPropertyTypeProducer.info(this.useUserPropertyType.getId());
        if (ObjectUtil.isNotEmpty(info)) {
            redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, info.getUserId(), info.getCommodityTypeCode()), info.getUseQuantity().toString());
        }
    }

    /**
     * 更新订单剩余的资源列表
     */
    public void updateTypeSurplus() {
        // 获取订单详情对应的商品类型资产剩余表
        List<TypeSurplusInfoVo> typeSurplusInfoVos = typeSurplusProducer.currentSurplus(this.useUserPropertyType.getPropertyId(), useUserPropertyType.getCommodityTypeId());
        if (ObjectUtil.isEmpty(typeSurplusInfoVos)) RRException.create("获取订单相关的资产出错");
        // 查出了在使用的和已经使用完的，因为有的资产是加的
        List<TypeSurplusInfoVo> list = typeSurplusInfoVos.stream().filter(item -> {
            if (NumberUtil.compare(this.thisUseNum, 0L) > 0) {
                return true;
            } else {
                return item.getUseStatus() == 0;
            }
        }).toList();
        // 如果是增加，则倒序
        if (NumberUtil.compare(this.thisUseNum, 0L) > 0) {
            // 是增加资产
//            List<Integer> commodityTypeCodes = Arrays.asList(0, 1, 2);
            list = list.stream()
//                    .peek(item -> {
//                        if (ObjectUtil.isEmpty(item.getCommodityTypeId())) RRException.create("订单类型查询失败");
//                        int i = commodityTypeCodes.indexOf(item.getCommodityType());
//                        if (i != -1) {
//                            item.setCommodityType(i);
//                        } else {
//                            RRException.create("订单类型错误");
//                        }
//                    })
                    // 顺序为 活动->邀请码->套餐->增量包
                    // 先按类型到序，再按结束时间升序
                    .sorted(Comparator.comparing(TypeSurplusInfoVo::getCommodityType).reversed().thenComparing(Comparator.comparing(TypeSurplusInfoVo::getEndTime).reversed())).toList();
        } else {
            // 是减少资产
//            List<Integer> commodityTypeCodes = Arrays.asList(2, 1, 0);
            list = list.stream()
//                    .peek(item -> {
//                        if (ObjectUtil.isEmpty(item.getCommodityTypeId())) RRException.create("订单类型查询失败");
//                        int i = commodityTypeCodes.indexOf(item.getCommodityType());
//                        if (i != -1) {
//                            item.setCommodityType(i);
//                        } else {
//                            RRException.create("订单类型错误");
//                        }
//                    })
                    // 顺序为 活动->邀请码->套餐->增量包
                    // 先按类型到序，再按结束时间升序
                    .sorted(Comparator.comparing(TypeSurplusInfoVo::getCommodityType).reversed().thenComparing(TypeSurplusInfoVo::getEndTime)).toList();
        }


        // 循环判断是否还有剩余
        List<TypeSurplusInfoVo> addTypeSurplusList = new ArrayList<>();
        Long totalSurplusNumber = null;
        List<UserPropertyDetailsBo> updateList = new ArrayList<>();
        Date now = new Date();
        // 循环剩余的订单资产
        for (TypeSurplusInfoVo item : list) {
            if (item.getTotalNumber() <= item.getUseNumber()) continue;
            UserPropertyDetailsBo e = new UserPropertyDetailsBo();
            e.setId(SnowflakeManager.nextValue());
            e.setUserId(this.userId);
            e.setUserName(this.userName);
            // 消费行租户id = 消费用户当时的 active_tenant_id（他在哪个租户里用）
            e.setTenantId(user.getActiveTenantId());
            e.setParentUserId(user.getParentId());
            e.setOrderDetailId(item.getOrderDetailId());
            e.setPropertyId(this.useUserPropertyType.getPropertyId());
            e.setTypeSurplusId(item.getId());
            e.setCommodityTypeId(item.getCommodityTypeId());
            e.setCommodityTypeCode(item.getCommodityTypeCode());
            CommodityTypeInfoVo commodityTypeInfoVo = commodityTypeProducer.info(item.getCommodityTypeId());
            if (ObjectUtil.isNotEmpty(commodityTypeInfoVo)) {
                e.setCommodityTypeName(commodityTypeInfoVo.getName());
                e.setCommodityTypeUnit(commodityTypeInfoVo.getUnit());
            }
            // 小于的就是减资源
            e.setSigns(this.thisUseNum < 0 ? 0 : 1);
            e.setRemarks(assets.getRemarks());
            e.setCreateDate(now);
            if (totalSurplusNumber == null) totalSurplusNumber = Math.abs(this.thisUseNum);
            addTypeSurplusList.add(item);
            if (NumberUtil.compare(this.thisUseNum, 0L) > 0) {
                item.setUseStatus(0);
                // 增加资产
                if (NumberUtil.compare(item.getUseNumber(), totalSurplusNumber) >= 0) {
                    item.setUseNumber(NumberUtil.sub(item.getUseNumber(), totalSurplusNumber).longValue());
                    e.setQuantity(totalSurplusNumber);
                    totalSurplusNumber = 0L;
                    updateList.add(e);
                    break;
                } else {
                    totalSurplusNumber = NumberUtil.sub(totalSurplusNumber, item.getUseNumber()).longValue();
                    e.setQuantity(item.getUseNumber());
                    item.setUseNumber(0L);
                    updateList.add(e);
                }
            } else {
                Long surplusNumber = NumberUtil.sub(item.getTotalNumber(), item.getUseNumber()).longValue();
                // 减少资产
                if (NumberUtil.compare(surplusNumber, totalSurplusNumber) >= 0) {
                    // 剩余数量大于等于本次使用数量，直接使用,就不需再循环了
                    item.setUseNumber(NumberUtil.add(item.getUseNumber(), totalSurplusNumber).longValue());
                    e.setQuantity(totalSurplusNumber);
                    totalSurplusNumber = 0L;
                    if (Objects.equals(item.getUseNumber(), item.getTotalNumber())) {
                        item.setUseStatus(1);
                    }
                    updateList.add(e);
                    break;
                } else {
                    totalSurplusNumber = NumberUtil.sub(totalSurplusNumber, surplusNumber).longValue();
                    e.setQuantity(item.getTotalNumber() - item.getUseNumber());
                    item.setUseNumber(item.getTotalNumber());
                    updateList.add(e);
                }
                addTypeSurplusList.add(item);
            }
        }
        // 判断是否还有剩余
        if (totalSurplusNumber == null || (NumberUtil.compare(this.thisUseNum, 0L) < 0 && totalSurplusNumber != 0L)) {
            RRException.create(StrUtil.format("当前账号资产[{}]不足", getCommodityCodeName()));
        }
        // 添加使用记录
        if (ObjectUtil.isNotEmpty(updateList)) {
            userPropertyDetailsProducer.saveBatch(updateList);

            // 关联aiToken记录
            ArrayList<PropertyDetailsTokenEntity> tokenList = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(assets.getAiTokenIds())) {
                updateList.forEach(update -> {
                    assets.getAiTokenIds().forEach(tokenId -> {
                        PropertyDetailsTokenEntity e = new PropertyDetailsTokenEntity();
                        e.setId(SnowflakeManager.nextValue());
                        e.setPropertyDetailsId(update.getId());
                        e.setAiTokenId(tokenId);
                        tokenList.add(e);
                    });
                });
            }
            if (ObjectUtil.isNotEmpty(tokenList)) propertyDetailsTokenService.saveBatch(tokenList);
        }
        // 更新
        if (ObjectUtil.isNotEmpty(addTypeSurplusList)) {
            typeSurplusProducer.updateBatch(BeanUtil.copyToList(addTypeSurplusList, TypeSurplusBo.class));
        }
    }

    public String getCommodityCodeName() {
        return useUserPropertyType.getCommodityTypeName();
    }
}
