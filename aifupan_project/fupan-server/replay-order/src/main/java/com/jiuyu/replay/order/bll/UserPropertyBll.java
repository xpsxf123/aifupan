package com.jiuyu.replay.order.bll;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.constant.OrderProperties;
import com.jiuyu.replay.order.dto.PaidUserDto;
import com.jiuyu.replay.order.dto.UserPropertyTypeCacheDto;
import com.jiuyu.replay.order.entity.CommodityTypeEntity;
import com.jiuyu.replay.order.producer.*;
import com.jiuyu.replay.order.vo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 用户资产
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Component
@AllArgsConstructor
@Slf4j
public class UserPropertyBll {

    private final UserPropertyProducer userPropertyProducer;
    private final UserPropertyTypeProducer userPropertyTypeProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AnchorVideoFeign anchorVideoFeign;
    private final OrderProducer orderProducer;
    private final UserFeign userFeign;
    private final CommodityTypeProducer commodityTypeProducer;
    private final UserPropertyDetailsProducer userPropertyDetailsProducer;

    public static final int WITHHOLD_TIMEOUT = 30;


    /**
     * 用户资产列表
     * @param userPropertyListBo 用户资产列表查询参数
     * @return
     */
    public R<PageUtils<UserPropertyListVo>> queryPage(UserPropertyListBo userPropertyListBo) {

        PageUtils<UserPropertyListVo> data = userPropertyProducer.queryPage(userPropertyListBo);
        if (ObjectUtil.isNotEmpty(data) && ObjectUtil.isNotEmpty(data.getList())){
            List<UserPropertyListVo> list = data.getList();
            list.forEach(userPropertyListVo ->{
                List<UserPropertyTypeInfoVo> userProperty = getUserProperty(userPropertyListVo.getUserId());
                userPropertyListVo.setUserPropertyTypeList(userProperty);
            });
        }

        return R.ok("获取成功", data);
    }

    /**
    * 用户资产信息
    * @param id 用户资产id
    * @return
    */
    public R<UserPropertyInfoVo> info(Long id) {

        UserPropertyInfoVo userPropertyInfoVo = userPropertyProducer.info(id);
        return R.ok("获取成功", userPropertyInfoVo);
    }

    /**
     * 新增用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
    public R<String> save(UserPropertyBo userPropertyBo) {

        UserPropertyInfoVo userPropertyInfoVo = userPropertyProducer.save(userPropertyBo);
        return R.ok("添加成功");
    }

    /**
     * 修改用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
    public R<String> update(UserPropertyBo userPropertyBo) {

        userPropertyProducer.update(userPropertyBo);
        return R.ok("修改成功");
    }

    /**
     * 删除用户资产
     * @param userId
     * @return
     */
    @Transactional
    public R<String> deleteByUserId(Long userId) {

        userPropertyProducer.deleteByUserId(userId);

        // 停用用户的订单
        List<OrderInfoVo> orderInfoVoList = orderProducer.listExistByUserId(userId);
        if (ObjectUtil.isNotEmpty(orderInfoVoList)){
            // 只要订单的状态为未开始、生效中、冻结的
            orderInfoVoList = orderInfoVoList.stream()
                    .filter(item -> Arrays.asList(1, 2, 7).contains(item.getStatus()))
                    .toList();
            for (OrderInfoVo orderInfoVo : orderInfoVoList) {
                orderProducer.orderStop(orderInfoVo.getId(), null, true);
            }
        }
        return R.ok("删除成功");
    }


    /**
     * 检查用户资产
     * @param userIds
     */
    public void checkUserPropertyAndCreate(List<Long> userIds, Integer isUse) {
        userPropertyProducer.checkUserPropertyAndCreate(userIds, isUse);
    }

    /**
     * 添加全部用户资产缓存
     */
    public void addUserPropertyCache() {
        userPropertyProducer.addUserPropertyCache();
    }

    public R<String> assetsMinusOrPlus(AssetsMinusOrPlusBo assets) {
        userPropertyProducer.assetsMinusOrPlus(assets);
        return R.ok("处理成功");
    }

    /**
     * 查询用户资产信息
     * @param userId
     * @return
     */
    public List<UserPropertyTypeInfoVo> getUserProperty(Long userId) {
        List<UserPropertyTypeInfoVo> userProperty = userPropertyProducer.getUserProperty(userId);
        if (ObjectUtil.isNotEmpty(userProperty)) {
            userProperty.sort(Comparator.comparing(UserPropertyTypeVo::getCommodityTypeId));
        }
        return userProperty;
    }

    /**
     * 根据用户id和资产类型code查询用户总资产
     * @param userId
     * @param code
     * @return
     */
    public R<Long> getTotalPropertyByUserIdCode(Long userId, String code){
        // 获取用户可添加主播的数量
        long total = 0L;
        List<UserPropertyTypeInfoVo> userProperty = getUserProperty(userId);
        if (ObjectUtil.isNotEmpty(userProperty)) {
            UserPropertyTypeInfoVo typeInfoVo = userProperty.stream().filter(item -> item.getCommodityTypeCode().equals(code)).findFirst().orElse(null);
            if (ObjectUtil.isNotEmpty(typeInfoVo)) {
                total = typeInfoVo.getTotalQuantity();
            }
        }
        return R.ok(total);
    }

    public Map<String, Long> getTotalPropertyByUserIdCode(Long userId, List<String> codes) {
        List<UserPropertyTypeInfoVo> userProperty = getUserProperty(userId);
        if (ObjectUtil.isNotEmpty(userProperty)) {
            return userProperty.stream()
                    .filter(item -> codes.contains(item.getCommodityTypeCode()))
                    .collect(Collectors.toMap(UserPropertyTypeVo::getCommodityTypeCode, UserPropertyTypeVo::getTotalQuantity));
        }
        return new HashMap<>();
    }

    /**
     * 获取当前子账号的用户资产，如果没有就创建
     * @param parentId
     * @param id
     * @return
     */
    public R<String> setUpASubAccount(Long parentId, Long id) {
        // 冻结当前的版本，增量包
        orderProducer.freezeOrderByUserId(id, null);

        // 获取当前子账号的用户资产，如果没有就创建
        UserPropertyInfoVo infoVo = userPropertyProducer.getUserPropertyOrCreate(id, parentId);
        // 更新子账号资产，同步父账号的资产
        userPropertyProducer.useChildUserProperty(infoVo.getId());
        return R.ok("成功");
    }

    /**
     * 解绑子账号
     * @param currentUserId
     * @param subUserId
     */
    public void unbindingSubAccount(Long currentUserId, Long subUserId) {
        // 解冻订单
        orderProducer.unfreezeOrderByUserId(subUserId, false);
        // 更新资产
        UserPropertyInfoVo userPropertyInfoVo = userPropertyProducer.getOne(subUserId, 0, 0, 0L, 0L);
        userPropertyProducer.statisticsProperty(userPropertyInfoVo);
        userPropertyProducer.statisticsChildProperty(userPropertyInfoVo);

    }

    /**
     * 统计用户资产-根据用户id来统计，单前账号一定不能是子账号的
     * @param userId
     */
    public void statisticsUserProperty(Long userId) {
        log.info("统计用户资产userId：{}", userId);
        Long parentId = ResultUtil.getResult(userFeign.getUserParentId(userId));
        if (parentId != null) RRException.create("当前账号为子账号，操作失败");

        // 更新资产
        UserPropertyInfoVo userPropertyInfoVo = userPropertyProducer.getOne(userId, null, 0, 0L, 0L);
        userPropertyProducer.statisticsProperty(userPropertyInfoVo);
        userPropertyProducer.statisticsChildProperty(userPropertyInfoVo);
    }

    /**
     * 更新用户资产-不是公用的资产
     *
     * @param userId
     * @param code
     * @param quantity
     * @return
     */
    public R<String> updateByPropertyNum(Long userId, String code, Long quantity) {
        userPropertyProducer.updateUserProperty(userId, code, quantity);
        return R.ok();
    }

    /**
     * 清空用户资产-不是公用的资产
     * @param userIds 用户id
     * @param code 资产类型code
     */
    public void clearPrivateProperty(List<Long> userIds, String code) {
        userPropertyProducer.clearPrivateProperty(userIds, code);
    }

    /**
     * 查询用户资产信息
     * @param userId
     * @param code
     * @return
     */
    public R<Long> getTotalUserIdPropertyByCode(Long userId, String code){
        return R.ok(userPropertyProducer.getTotalUserIdPropertyByCode(userId, code));
    }

    /**
     * 查询用户资产信息
     * @param propertyId
     * @return
     */
    public R<List<UserPropertyTypeInfoVo>> getPropertyByPropertyId(Long propertyId) {
        return R.ok(userPropertyTypeProducer.getPropertyByPropertyId(propertyId));
    }

    /**
     * 转换为long, 如果为null，则返回0L
     *
     * @param value 值
     * @return 返回转后的long值
     */
    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return Convert.toLong(value, 0L);
    }

    /**
     * 查询用户资产信息
     * @param bo
     * @return
     */
    public R<IsPropertyHaveVo> isHave(IsPropertyHaveBo bo) {
        log.info("用户{}是否拥有{}", bo.getUserId(), bo.getCode());
        if (ObjectUtil.isEmpty(bo.getUserId())) return R.error(3001, "用户id为空");
        if (ObjectUtil.isEmpty(bo.getCode())) return R.error(3001, "资产code为空");
        if (ObjectUtil.isEmpty(bo.getThisUseNum())) return R.error(3001, "使用数量为空");
        IsPropertyHaveVo result = new IsPropertyHaveVo();
        Long parentUserId = userFeign.getUserIdOrParentId(bo.getUserId());

        // 当前用户使用的资产id
        long currentValue = toLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, parentUserId, bo.getCode())));
        long currentTotalValue = toLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, parentUserId, bo.getCode())));

        long tempTotalValue = currentValue + bo.getThisUseNum();
        // 查询已经预扣的资产
        List<RedisWithholdVo> tempUserProperty = getTempUserProperty(parentUserId);
        if (ObjectUtil.isNotEmpty(tempUserProperty)) {
            tempTotalValue += tempUserProperty.stream()
                    .filter(item -> bo.getCode().equals(item.getCommodityTypeCode()) && item.getNum() < 0)
                    .mapToLong(item -> Math.abs(item.getNum()))
                    .sum();
        }
        if (tempTotalValue > currentTotalValue) {
            result.setIsHave(false);
            return R.ok(result);
        } else {
            Long redisId = SnowflakeManager.nextValue();
            result.setRedisId(redisId);
            result.setIsHave(true);
            result.setWithholdId(StrUtil.format("{}_{}", parentUserId, redisId));

            RedisWithholdVo redisWithholdVo = new RedisWithholdVo();
            redisWithholdVo.setRedisId(redisId);
            redisWithholdVo.setWithholdId(result.getWithholdId());
            redisWithholdVo.setNum(-bo.getThisUseNum());
            redisWithholdVo.setPropertyId(parentUserId);
            redisWithholdVo.setCommodityTypeCode(bo.getCode());
            redisWithholdVo.setExpirationTime(System.currentTimeMillis() + (30 * 60 * 1000));
            addTempUserProperty(redisWithholdVo, parentUserId);
            return R.ok(result);
        }
    }

    public List<RedisWithholdVo> getTempUserProperty(Long userId) {
        List<RedisWithholdVo> result = new ArrayList<>();
        // 查询已经预扣的资产
        List<Object> redisList = redisTemplate.opsForHash().values(RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeHashCacheKey, userId));
        if (ObjectUtil.isNotEmpty(redisList)) {
            result = redisList.stream()
                    .filter(ObjectUtil::isNotEmpty)
                    .map(item -> {
                        RedisWithholdVo bean = null;
                        try {
                            bean = JSONUtil.toBean(item.toString(), RedisWithholdVo.class);
                        } catch (Exception e) {
                            log.error("redisWithholdVo转换错误", e);
                        }
                        return bean;
                    })
                    .filter(item -> item != null && item.getNum() != null && item.getNum() != 0 && System.currentTimeMillis() < item.getExpirationTime())
                    .toList();
        }
        return result;
    }

    /**
     * 添加预扣的缓存
     *
     * @param redisWithholdVo
     * @param userId
     */
    @Transactional
    public void addTempUserProperty(RedisWithholdVo redisWithholdVo, Long userId) {
        String redisKey = RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeHashCacheKey, userId);
        try {
            redisTemplate.opsForHash().put(redisKey, redisWithholdVo.getRedisId().toString(), JSONUtil.toJsonStr(redisWithholdVo));
            redisTemplate.expire(redisKey, WITHHOLD_TIMEOUT, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(
                    RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeAssociationCacheKey, redisWithholdVo.getRedisId()),
                    redisWithholdVo.getWithholdId(),
                    WITHHOLD_TIMEOUT,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.error("添加预扣的缓存 userId = {}, error: {}", userId, e.getMessage());
            throw new BusinessException(StatusCode.REDIS_OPERATE_ERROR);
        }
    }

    /**
     * 查询aiToken记录列表
     * @param propertyDetailsId
     * @return
     */
    public R<List<AiTokenUseRecordInfoVo>> aiTokenUseRecordByDetailId(Long propertyDetailsId) {
        return R.ok(userPropertyProducer.aiTokenUseRecordByDetailId(propertyDetailsId));
    }

    public R<IsPropertyHaveVo> isPropertyHaveAiToken(IsPropertyHaveBo bo) {
        log.info("用户{}是否拥有{}", bo.getUserId(), bo.getCode());
        if (ObjectUtil.isEmpty(bo.getUserId())) return R.error(3001, "用户id为空");
        if (ObjectUtil.isEmpty(bo.getCode())) return R.error(3001, "资产code为空");
        if (ObjectUtil.isEmpty(bo.getThisUseNum())) return R.error(3001, "使用数量为空");
        IsPropertyHaveVo result = new IsPropertyHaveVo();
        Long parentUserId = userFeign.getUserIdOrParentId(bo.getUserId());

        // 当前用户使用的资产id
        long currentValue = toLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, parentUserId, bo.getCode())));
        long currentTotalValue = toLong(redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, parentUserId, bo.getCode())));

        long tempTotalValue = currentValue;
        // 查询已经预扣的资产
        List<RedisWithholdVo> tempUserProperty = getTempUserProperty(parentUserId);
        if (ObjectUtil.isNotEmpty(tempUserProperty)) {
            tempTotalValue += tempUserProperty.stream()
                    .filter(item -> bo.getCode().equals(item.getCommodityTypeCode()) && item.getNum() < 0)
                    .mapToLong(item -> Math.abs(item.getNum()))
                    .sum();
        }
        // 比较资产是否足够
        if (tempTotalValue >= currentTotalValue) {
            result.setIsHave(false);
            return R.ok(result);
        } else {
            long tempValue = Math.abs(bo.getThisUseNum());
            // 如果最后一次扣除的数量大于剩余数量，则扣除剩余数量
            if (tempTotalValue + Math.abs(bo.getThisUseNum()) > currentTotalValue){
                tempValue = (currentTotalValue - tempTotalValue);
            }
            tempValue = -tempValue;
            Long redisId = SnowflakeManager.nextValue();
            result.setRedisId(redisId);
            result.setWithholdId(StrUtil.format("{}_{}", parentUserId, redisId));
            result.setIsHave(true);
            RedisWithholdVo redisWithholdVo = new RedisWithholdVo();
            redisWithholdVo.setRedisId(redisId);
            redisWithholdVo.setWithholdId(result.getWithholdId());
            redisWithholdVo.setNum(tempValue);
            redisWithholdVo.setPropertyId(parentUserId);
            redisWithholdVo.setCommodityTypeCode(bo.getCode());
            redisWithholdVo.setExpirationTime(System.currentTimeMillis() + (WITHHOLD_TIMEOUT * 60 * 1000));
            addTempUserProperty(redisWithholdVo, parentUserId);
            return R.ok(result);
        }
    }

    /**
     * 根据资产code获取用户资产
     * @param userId 用户id
     * @param code 资产code
     * @return
     */
    public UserPropertyTypeInfoVo getUserPropertyByCode(Long userId, String code) {

        List<UserPropertyTypeInfoVo> propertyTypeInfoVoList = userPropertyProducer.getUserProperty(userId);

        if(propertyTypeInfoVoList != null && !propertyTypeInfoVoList.isEmpty()) {
            for (UserPropertyTypeInfoVo userPropertyTypeInfoVo : propertyTypeInfoVoList) {
                if(userPropertyTypeInfoVo.getCommodityTypeCode().equals(code)) {
                    return userPropertyTypeInfoVo;
                }
            }
        }

        UserPropertyTypeInfoVo nullObj = new UserPropertyTypeInfoVo();
        nullObj.setTotalQuantity(0L);
        nullObj.setUseQuantity(0L);
        return nullObj;
    }

    /**
     * 按资产 code 快速读取用户资产（仅 2 次 Redis GET）。
     *
     * <p>区别于 {@link #getUserPropertyByCode(Long, String)} 全量拉取所有资产类型（按商品类型逐个 Redis GET）
     * 再内存过滤，本方法只读取单个 code 的总量/已用缓存，显著降低查询开销。owner 经
     * {@link com.jiuyu.replay.generic.feign.power.UserFeign#getUserIdOrParentId} 解析为父账号或自身，
     * 与 {@link #isPropertyHaveAiToken(IsPropertyHaveBo)} 预扣口径一致，仅适用于父账号共享型资产（如 aiTokenNum）。</p>
     *
     * @param userId 用户 id
     * @param code   资产 code
     * @return 资产信息（仅含 commodityTypeCode/totalQuantity/useQuantity，缓存缺省 0）
     */
    public UserPropertyTypeInfoVo getSharedPropertyByCode(Long userId, String code) {
        Long ownerId = userFeign.getUserIdOrParentId(userId);
        long used = toLong(redisTemplate.opsForValue()
                .get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, ownerId, code)));
        long total = toLong(redisTemplate.opsForValue()
                .get(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, ownerId, code)));
        UserPropertyTypeInfoVo vo = new UserPropertyTypeInfoVo();
        vo.setCommodityTypeCode(code);
        vo.setTotalQuantity(total);
        vo.setUseQuantity(used);
        return vo;
    }


    /**
     * 检查监控位授权量
     *
     * <p>根据资产 code 查询指定用户（或其主账号）的监控位总量、使用量及剩余量。
     * 当资产 sub_account_have=0 且传入子账号时，自动回退到主账号查询，无需调用方额外处理。</p>
     *
     * @param userId   用户 id（可为主账号或子账号）
     * @param baseCode 资产 code，如 scriptQualityNum / scriptFidelityNum / interactionPatrolNum
     * @return 监控位授权量信息（hasAuth / hasSurplus / totalQuantity / useQuantity / surplus）
     */
    public MonitorPositionAuthVo checkMonitorPosition(Long userId, String baseCode) {
        // 1. 查商品类型取 subAccountHave
        CommodityTypeInfoVo commodityType = commodityTypeProducer.getByCode(baseCode);

        // 2. 判断是否需要回退到主账号
        Long resolvedUserId = userId;
        if (commodityType != null && Integer.valueOf(0).equals(commodityType.getSubAccountHave())) {
            Long parentId = ResultUtil.getResult(userFeign.getUserParentId(userId));
            if (parentId != null && !parentId.equals(0L)) {
                resolvedUserId = parentId;
            }
        }

        // 3. 查询资产
        UserPropertyTypeInfoVo vo = getUserPropertyByCode(resolvedUserId, baseCode);

        // 4. 组装返回值
        long totalQuantity = vo.getTotalQuantity() == null ? 0L : vo.getTotalQuantity();
        long useQuantity = vo.getUseQuantity() == null ? 0L : vo.getUseQuantity();
        long surplus = Math.max(totalQuantity - useQuantity, 0L);

        MonitorPositionAuthVo result = new MonitorPositionAuthVo();
        result.setCode(baseCode);
        result.setTotalQuantity(totalQuantity);
        result.setUseQuantity(useQuantity);
        result.setRemainingQuantity(surplus);
        result.setHasAuth(totalQuantity > 0);
        result.setHasSurplus(surplus > 0);
        return result;
    }

    public Long clintGetData(Long id) {
        return userPropertyProducer.clintGetData(id);
    }

    public void removeTempUserProperty(String withholdId, Long redisId) {
        if (ObjectUtil.isNotEmpty(redisId)) {
            String redisKey = RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeAssociationCacheKey, redisId);
            String temp = (String) redisTemplate.opsForValue().get(redisKey);
            if (ObjectUtil.isNotEmpty(temp)) {
                withholdId = temp;
            }
            redisTemplate.delete(redisKey);
        }
        if (ObjectUtil.isNotEmpty(withholdId)) {
            String[] split = withholdId.split("_");
            if (split.length == 2) {
                redisTemplate.opsForHash().delete(RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeHashCacheKey, split[0]), split[1]);
            }
        }
    }

    public RedisWithholdVo getRedisCache(String withholdId, Long redisId) {
        if (ObjectUtil.isNotEmpty(redisId)) {
            String redisKey = RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeAssociationCacheKey, redisId);
            String temp = (String) redisTemplate.opsForValue().get(redisKey);
            if (ObjectUtil.isNotEmpty(withholdId)) {
                withholdId = temp;
            }
        }
        if (ObjectUtil.isNotEmpty(withholdId)) {
            String[] split = withholdId.split("_");
            if (split.length == 2) {
                String string = (String) redisTemplate.opsForHash().get(RedisCacheKey.getRedisKey(RedisCacheKey.tempUserPropertyTypeHashCacheKey, split[0]), split[1]);
                if (ObjectUtil.isNotEmpty(string)) {
                    return JSONObject.parseObject(string, RedisWithholdVo.class);
                }
            }
        }
        return null;
    }

    public boolean updateByPropertyNumRetBoolean(Long userId, String code, Long quantity) {
        return userPropertyProducer.updateUserProperty(userId, code, quantity);
    }

    /**
     * 检查资产是否能能用
     *
     * @param userId   用户id
     * @param code     资产code
     * @param quantity 要使用数量
     * @return 是否可以用
     */
    public boolean checkUseProperty(Long userId, String code, long quantity) {
        // 如果是订阅达人或者订阅爆款就返回false
        if (OrderEnums.commodityTypeCode.isUserShortVideoProperty(code)) {
            return false;
        }
        // 查询资产
        UserPropertyTypeInfoVo userPropertyByCode = getUserPropertyByCode(userId, code);
        if (ObjectUtil.isEmpty(userPropertyByCode)) {
            return false;
        }
        quantity = -quantity;
        // 获取剩余的量
        long useQuantity = userPropertyByCode.getUseQuantity() + quantity;

        return useQuantity >= 0 && useQuantity <= userPropertyByCode.getTotalQuantity();
    }

    /**
     * 检查资产是否能能用--短视频订阅的资产
     *
     * @param userId   用户id
     * @param code     资产code
     * @param quantity 要使用数量
     * @return 是否可以用
     */
    public boolean checkSubscribeUseProperty(Long userId, String code, long quantity) {
        if (code == null) {
            return false;
        }
        quantity = -quantity;
        // 如果不是订阅达人或者订阅爆款就返回false
        if (!OrderEnums.commodityTypeCode.isUserShortVideoProperty(code)) {
            return false;
        }

        // 返回本账号订阅的资产
        OrderEnums.commodityTypeCode userSubscribeEnum = OrderEnums.commodityTypeCode.getUserSubscribeEnum(code);

        if (userSubscribeEnum == null) {
            return false;
        }

        // 获取总资产
        List<UserPropertyTypeInfoVo> propertyTypeInfoVoList = userPropertyProducer.getUserProperty(userId);
        if (ObjectUtil.isEmpty(propertyTypeInfoVoList)) {
            return false;
        }

        // 获取共享的资产
        UserPropertyTypeInfoVo share = getUserPropertyTypeByCode(propertyTypeInfoVoList, code);
        ;

        // 获取用户对应的资产
        UserPropertyTypeInfoVo user = getUserPropertyTypeByCode(propertyTypeInfoVoList, userSubscribeEnum.getCode());
        ;

        // 判空
        if (share == null || user == null) {
            return false;
        }

        // 判断共享的订阅
        long shareQuantity = share.getUseQuantity() + quantity;
        boolean shareFlag = shareQuantity >= 0 && shareQuantity <= share.getTotalQuantity();

        // 判断本账号的订阅
        long userQuantity = user.getUseQuantity() + quantity;
        boolean userFlag = userQuantity >= 0 && userQuantity <= user.getTotalQuantity();

        // 返回
        return shareFlag && userFlag;
    }

    /**
     * 使用资产--短视频订阅
     *
     * @param userId   用户id
     * @param code     code
     * @param quantity 使用量
     * @return 是否成功
     */
    @CustomRedissonLock(key = "'replay:lock:order:useSubscribeProperty:' + #args[1]")
    public boolean useSubscribeProperty(Long userId, Long tenantId, String code, Long quantity) {

        // 检查是否可用
        if (quantity < 0) {
            boolean checked = checkSubscribeUseProperty(userId, code, quantity);
            if (!checked) {
                return false;
            }
        }

        OrderEnums.commodityTypeCode userSubscribeEnum = OrderEnums.commodityTypeCode.getUserSubscribeEnum(code);
        if (userSubscribeEnum == null) {
            return false;
        }

        // 获取总资产
        List<UserPropertyTypeInfoVo> propertyTypeInfoVoList = userPropertyProducer.getUserProperty(userId);
        if (ObjectUtil.isEmpty(propertyTypeInfoVoList)) {
            return false;
        }

        quantity = -quantity;

        // 获取共享的资产
        UserPropertyTypeInfoVo share = getUserPropertyTypeByCode(propertyTypeInfoVoList, code);

        // 获取用户对应的资产
        UserPropertyTypeInfoVo user = getUserPropertyTypeByCode(propertyTypeInfoVoList, userSubscribeEnum.getCode());

        // 判空
        if (share == null || user == null) {
            return false;
        }

        Long shareUserId = userPropertyProducer.getCurrentUserId(userId, code);

        // 减资产
        long shareQuantity = share.getUseQuantity() + quantity;
        long userQuantity = user.getUseQuantity() + quantity;
        shareQuantity = shareQuantity < 0 ? 0 : shareQuantity;
        userQuantity = userQuantity < 0 ? 0 : userQuantity;

        ArrayList<UpdateUserPropertyVo> list = new ArrayList<>();
        UpdateUserPropertyVo vo = new UpdateUserPropertyVo();
        vo.setUserId(shareUserId);
        vo.setCode(code);
        vo.setQuantity(shareQuantity);
        list.add(vo);

        UpdateUserPropertyVo e = new UpdateUserPropertyVo();
        e.setUserId(userId);
        e.setCode(userSubscribeEnum.getCode());
        e.setQuantity(userQuantity);
        list.add(e);
        userPropertyProducer.useSubscribeProperty(list);
        return true;
    }

    private void updateSubscribeProperty(Long userId, String code, Long quantity) {
        Long sharePropertyId = (Long) redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.userUsePropertyIdCacheKey, userId));
        if (ObjectUtil.isEmpty(sharePropertyId)) {
            RRException.create("获取用户资产失败");
        }
        boolean isShareUpdate = userPropertyProducer.updateUserPropertyType(userId, sharePropertyId, code, quantity);
        if (!isShareUpdate) {
            throw new BusinessException(StatusCode.DATA_UPDATE_ERROR.getCode(), "更新资产失败");
        }
    }


    /**
     * 根据code获取对应的资产
     *
     * @param propertyTypeInfoVoList 资产list
     * @param code                   code
     * @return 对应的资产
     */
    private UserPropertyTypeInfoVo getUserPropertyTypeByCode(List<UserPropertyTypeInfoVo> propertyTypeInfoVoList, String code) {
        for (UserPropertyTypeInfoVo item : propertyTypeInfoVoList) {
            if (item.getCommodityTypeCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 使用资产
     *
     * @param assets 参数
     */
    @Transactional
    public void useProperty(AssetsMinusOrPlusBo assets) {
        // 如果不是订阅达人或者订阅爆款就返回false
        if (OrderEnums.commodityTypeCode.isUserShortVideoProperty(assets.getCode())) {
            throw new BusinessException("当前code类型不支持");
        }
        UserPropertyImpl.use(assets);
    }

    /**
     * 删除用户资产缓存
     *
     * @param userIds
     */
    public void deleteUserPropertyCache(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return;
        }
        List<CommodityTypeListVo> typeList = commodityTypeProducer.list(new QueryWrapper<>());

        if (ObjectUtil.isEmpty(typeList)) {
            return;
        }

        List<String> keys = new ArrayList<>();
        for (Long userId : userIds) {
            typeList.forEach(item -> {
                keys.add(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeCacheKey, userId, item.getCode()));
                keys.add(RedisCacheKey.getRedisKey(RedisCacheKey.userPropertyTypeTotalCacheKey, userId, item.getCode()));
            });
        }

        try {
            redisTemplate.delete(keys);
        } catch (Exception ex) {
            log.error("删除用户资产缓存异常", ex);
        }
    }



    /**
     * 获取用户剩余资产
     *
     * @param userIds       用户ID
     * @param commodityCode 资产类型
     *
     * @return {@link Map }<{@link Long }, {@link Long }> key 用户ID value 剩余资产
     */
    public Map<Long, Long> getUserPropertyRemainingMap(Collection<Long> userIds, String commodityCode) {
        return userPropertyTypeProducer.getUserPropertyRemainingMap(userIds, commodityCode);
    }

    /**
     * 用户资产详情
     *
     * @param bo 参数
     * @return 列表
     */
    public PageUtils<SingleUserPropertyDetailsListVo> userPropertyDetails(UserPropertyDetailsList bo) {
        RRException.isNotEmpty(bo.getUserId(), "用户ID不能为空");
        UserDto userDto = userFeign.userById(bo.getUserId());
        if (userDto == null) {
            throw new BusinessException("用户不存在");
        }
        Long currentUserId = userDto.getId();
        if (userDto.getUserType() == UserEnums.userType.CLIENT_CHILD_USER.getCode()) {
            currentUserId = userDto.getParentId();
        }
        UserPropertyInfoVo property = userPropertyProducer.getUserPropertyOrCreate(currentUserId, 0L);


        PageUtils<SingleUserPropertyDetailsListVo> res = new PageUtils<>(List.of(), 0, bo.getPage(), bo.getLimit());
        if (property == null) {
            return res;
        }

        bo.setPropertyId(property.getId());

        if (userDto.getUserType() == UserEnums.userType.CLIENT_USER.getCode()) {
            bo.setUserId(null);
        }

        bo.setStartDate(DateUtil.offset(new Date(), DateField.MONTH, -3));

        List<String> allDates = getDatesForNext3MonthsAsUtilDate();
        res.setTotalCount(allDates.size());

        // 2. 使用 Hutool 分页：每页 10 条，取第 1 页（页码从 0 开始！）
        int page = bo.getPage();
        int pageSize = bo.getLimit();

        List<String> pageData = ListUtil.page(page - 1, pageSize, allDates);

        if (pageData.isEmpty()) {
            return res;
        }

        List<String> codes = List.of(OrderEnums.commodityTypeCode.AI_ANALYSIS_TIME.getCode(), OrderEnums.commodityTypeCode.AI_TOKEN_NUM.getCode());

        List<SingleUserPropertyDetailsListVo> resList = pageData.stream()
                .map(item -> SingleUserPropertyDetailsListVo.builder().createDate(item).build())
                .toList();
        Map<String, CommodityTypeEntity> commodityTypeMap = commodityTypeProducer.listAll()
                .stream()
                .collect(Collectors.toMap(CommodityTypeEntity::getCode, v -> v, (v1, v2) -> v2));

        for (String code : codes) {
            bo.setCommodityTypeCode(code);
            bo.setLimit(-1);
            bo.setStartDate(DateUtil.parse(pageData.get(pageData.size() - 1) + " 00:00:00"));
            bo.setEndDate(DateUtil.parse(pageData.get(0) + " 23:59:59"));
            PageUtils<UserPropertyDetailsInfoVo> pageList = userPropertyDetailsProducer.userPropertyDetails(bo);
            CommodityTypeEntity commodityType = commodityTypeMap.get(code);
            if (commodityType == null) {
                continue;
            }
            if (ObjectUtil.isNotEmpty(pageList.getList())) {
                Map<String, UserPropertyDetailsInfoVo> createMap = pageList.getList().stream()
                        .peek(item -> item.setCommodityTypeName(DateUtil.formatDate(item.getCreateDate())))
                        .collect(Collectors.toMap(UserPropertyDetailsVo::getCommodityTypeName, a -> a, (a, b) -> a));
                for (SingleUserPropertyDetailsListVo item : resList) {

                    Long quantity = 0L;
                    UserPropertyDetailsInfoVo userPropertyDetailsInfoVo = createMap.get(item.getCreateDate());
                    if (userPropertyDetailsInfoVo != null) {
                        quantity = userPropertyDetailsInfoVo.getQuantity();
                    }
                    if (code.equals(OrderEnums.commodityTypeCode.AI_ANALYSIS_TIME.getCode())) {
                        String num = transformationUnit(ObjectUtil.defaultIfNull(quantity, 0L).toString(), 60);
                        item.setAiAnalysisTime(StrUtil.format("{}{}", num, commodityType.getUnit()));
                    } else if (code.equals(OrderEnums.commodityTypeCode.AI_TOKEN_NUM.getCode())) {
                        String num = transformationUnit(ObjectUtil.defaultIfNull(quantity, 0L).toString(), 10000);
                        num = "0".equals(num) ? "0" : num + "万";
                        item.setAiTokenNum(StrUtil.format("{}{}", num, commodityType.getUnit()));
                    }
                }
            }
        }
        res.setList(resList);
        return res;
    }

    /**
     * 获取从今天开始往后3个月内的所有日期（不包含3个月后的那一天）
     * 返回 List<java.util.Date>
     */
    private List<String> getDatesForNext3MonthsAsUtilDate() {
        DateTime now = DateUtil.date();
        DateTime dateTime = DateUtil.offsetMonth(new Date(), -3);
        List<String> dates = new ArrayList<>();

        while (now.compareTo(dateTime) >= 0) {
            dates.add(DateUtil.formatDate(now));
            now = DateUtil.offsetDay(now, -1);
        }

        return dates;
    }

    /**
     * 单位转换
     *
     * @param quantity    数量
     * @param dividendNum 分数
     * @return 转换后的数量
     */
    private String transformationUnit(String quantity, int dividendNum) {
        if (ObjectUtil.isEmpty(quantity) || "0".equals(quantity)) {
            return quantity;
        }
        return BigDecimal.valueOf(NumberUtil.parseLong(quantity)).divide(BigDecimal.valueOf(dividendNum), 2, RoundingMode.HALF_UP).toString();
    }

    /**
     * 企业后台更新资产
     *
     * @param tenantUpdate
     */
    public void updateByPropertyNumByTenant(UpdateByPropertyNumByTenantBo tenantUpdate) {

        if (commodityTypeProducer.listAll().stream()
                .noneMatch(item -> item.getCode().equals(tenantUpdate.getCode()) && ObjectUtil.equals(item.getIsReset(), 0) && ObjectUtil.equals(item.getSubAccountHave(), 0))) {
            throw new BusinessException("code不符合条件");
        }

        Long userId = userFeign.getUserIdByTenantId(tenantUpdate.getTenantId());

        if (userId == null) {
            throw new BusinessException("租户id错误，没有找到对应的userId");
        }

        userPropertyProducer.updateUserProperty(userId, tenantUpdate.getCode(), tenantUpdate.getQuantity());
    }

    public UserPropertyTypeCacheDto getUserPropertyByTenant(Long tenantId) {

        Long userId = userFeign.getUserIdByTenantId(tenantId);

        if (userId == null) {
            throw new BusinessException("租户id错误，没有找到对应的userId");
        }
        // 获取资产
        List<UserPropertyTypeInfoVo> list = userPropertyProducer.getUserProperty(userId);
        UserPropertyInfoVo userProperty = new UserPropertyInfoVo();
        userProperty.setUserId(userId);
        userProperty.setUserPropertyTypeList(list);
        // 转化类型
        return UserPropertyTypeCacheDto.create(userProperty);
    }

    /**
     * 获取付费用户
     *
     */
    public PaidUserDto paidUser(long orderUserId, long userId, long tenantId) {
        boolean hasPaidUser = orderProducer.hasPaidUser(orderUserId);
        PaidUserDto paidUserDto = new PaidUserDto();
        paidUserDto.setPaid(hasPaidUser);
        paidUserDto.setRecord(anchorVideoFeign.hasRecord(userId, tenantId, 30));
        return paidUserDto;
    }
}

