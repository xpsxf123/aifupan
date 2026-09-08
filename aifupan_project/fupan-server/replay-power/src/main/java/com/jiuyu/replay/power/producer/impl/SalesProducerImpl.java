package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.utils.UpdateWrapperUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesListVo;
import com.jiuyu.replay.generic.bo.power.SalesBo;
import com.jiuyu.replay.generic.bo.power.SalesListBo;
import com.jiuyu.replay.power.constant.RedisPowerKeyCache;
import com.jiuyu.replay.power.entity.SalesEntity;
import com.jiuyu.replay.power.entity.UserDetailsEntity;
import com.jiuyu.replay.power.producer.SalesProducer;
import com.jiuyu.replay.power.repository.service.SalesService;
import com.jiuyu.replay.power.repository.service.UserDetailsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 用户跟进销售人员表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@Slf4j
@Service
public class SalesProducerImpl implements SalesProducer {

    @Resource
    private SalesService salesService;
    @Resource
    private UserDetailsService userDetailsService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;




    @Override
    public PageUtils<SalesListVo> queryPage(SalesListBo salesListBo) {
        LambdaQueryWrapper<SalesEntity> wrapper = new QueryWrapper<SalesEntity>()
                .lambda()
                .eq(ObjectUtil.isNotEmpty(salesListBo.getUserId()), SalesEntity::getUserId, salesListBo.getUserId())
                .eq(ObjectUtil.isNotEmpty(salesListBo.getSalesType()), SalesEntity::getSalesType, salesListBo.getSalesType())
                .eq(ObjectUtil.isNotEmpty(salesListBo.getAgentId()), SalesEntity::getAgentId, salesListBo.getAgentId())
                .eq(ObjectUtil.isNotEmpty(salesListBo.getEmployeeStatus()), SalesEntity::getEmployeeStatus, salesListBo.getEmployeeStatus())
                .like(ObjectUtil.isNotEmpty(salesListBo.getKeyword()), SalesEntity::getSalesName, salesListBo.getKeyword())
                .like(ObjectUtil.isNotEmpty(salesListBo.getPhone()), SalesEntity::getPhone, salesListBo.getPhone());

        IPage<SalesEntity> iPage = salesService.page(new Query<SalesEntity>().getPage(salesListBo.getPage(), salesListBo.getLimit()), wrapper);

        PageUtils<SalesListVo> pageUtils = new PageUtils<>(salesListBo.getPage(), salesListBo.getLimit(), iPage);

        List<SalesEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<SalesListVo> vos = records.stream().map(item -> {
                SalesListVo salesVo = new SalesListVo();
                BeanUtils.copyProperties(item, salesVo);
                return salesVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public SalesInfoVo info(Long id) {

        SalesEntity salesEntity = salesService.getById(id);
        if(salesEntity != null) {
            SalesInfoVo salesInfoVo = new SalesInfoVo();
            BeanUtils.copyProperties(salesEntity, salesInfoVo);
            return salesInfoVo;
        }

        return null;
    }

    @Override
    public SalesInfoVo save(SalesBo salesBo) {


         SalesEntity salesEntity = new SalesEntity();
         BeanUtils.copyProperties(salesBo, salesEntity);
        if (salesBo.getId() == null) {
            salesEntity.setId(SnowflakeManager.nextValue());
        }
         salesEntity.setCreateDate(new Date());
         salesEntity.setUpdateDate(new Date());

         salesService.save(salesEntity);

         SalesInfoVo salesInfoVo = new SalesInfoVo();
         BeanUtils.copyProperties(salesEntity, salesInfoVo);

         return salesInfoVo;
     }

    @Override
    public void update(SalesBo salesBo) {


        SalesEntity salesEntity = new SalesEntity();
        BeanUtils.copyProperties(salesBo, salesEntity);
        salesEntity.setUpdateDate(new Date());

        salesService.updateById(salesEntity);
    }

    @Override
    public void updateAllField(SalesEntity salesEntity) {
        salesEntity.setUpdateDate(new Date());

        salesService.update(UpdateWrapperUtils.createFullNullUpdateWrapperSimple(salesEntity));
    }

    @Override
    public void deleteById(Long id) {
        salesService.removeById(id);
    }

    /**
     * 获取所有的销售人员
     * @return
     */
    @Override
    public List<SalesEntity> listAll() {
        List<SalesEntity> salesEntities = salesService.list();
        if (salesEntities != null && salesEntities.size() != 0) {
            return salesEntities;
        }
        return null;
    }

    @Override
    public List<SalesInfoVo> listByIds(Collection<Long> saleIds) {

        if(saleIds != null && saleIds.size() > 0) {
            List<SalesEntity> salesEntities = this.salesService.listByIds(saleIds);
            if(salesEntities != null && salesEntities.size() > 0) {
                List<SalesInfoVo> salesInfoVos = salesEntities.stream().map(item -> {
                    SalesInfoVo salesInfoVo = new SalesInfoVo();
                    BeanUtils.copyProperties(item, salesInfoVo);
                    return salesInfoVo;
                }).collect(Collectors.toList());

                return salesInfoVos;
            }
        }

        return null;
    }


    /**
     * 轮询销售人员
     * 1、获取缓存数据
     * 2、校验是否有 未开启分配的
     * 3、如果有 则重新获取 并存入缓存
     * 4、如果没有则直接从缓存取
     * @return
     */
    @Override
    public Long getPollingSaleId() {
        // 获取平台销售列表
        List<SalesEntity> saleList = getSaleList();
        if (saleList == null || saleList.isEmpty()) {
            log.info("[轮询销售-兜底] 获取销售，saleList.size = 0");
            return null;
        }
        log.info("[轮询销售-兜底] 获取销售，saleList.size = {}, saleList = {}", saleList.size(), JSONUtil.toJsonStr(saleList));

        // 获取索引
        long saleIndex = getSaleIndex();
        log.info("[轮询销售-兜底] 获取索引，index = {}", saleIndex);

        // 取模获取下标
        int index = (int) (saleIndex % saleList.size());
        log.info("[轮询销售-兜底] 取模获取下标，index = {}", index);
        if (index > saleList.size() - 1) {
            log.error("[轮询销售-兜底] 取模获取下标异常, index = {}, saleList = {}", index, saleList);
            return null;
        }

        SalesEntity salesEntity = saleList.get(index);
        log.info("[轮询销售-兜底] 根据下标获取销售，id = {}，名称 = {}", salesEntity.getId(), salesEntity.getSalesName());
        return salesEntity.getId();
    }

    /**
     * 获取代商平台销售索引
     *
     * @return 索引
     */
    private long getSaleIndex() {
        String saleIndexKey = RedisPowerKeyCache.REDIS_POWER_SALE_INDEX_KEY;
        Long index = 1L;
        int timeout = 30;
        try {
            index = this.stringRedisTemplate.opsForValue().increment(saleIndexKey, 1L);
            log.info("[轮询销售-兜底] 代商平台销售索引 获取redis的key，key=replay:sale:index: ，value={}", index);
            // 索引初始值为1, 则设置过期时间 30天
            if (index != null && index == 1L) {
                stringRedisTemplate.expire(saleIndexKey, timeout, TimeUnit.DAYS);
            }

            // index 超过 10000 重置为0
            if (index != null && index > 10000) {
                // 重置索引成1
                this.stringRedisTemplate.opsForValue().set(saleIndexKey, "1", timeout, TimeUnit.DAYS);
                index = 1L;
            }
        } catch (Exception e) {
            log.error("[轮询销售-兜底] 从redis获取全代商平台销售索引失败, error: {}", e.getMessage());
        }
        return index == null ? 0L : index - 1;
    }

    /**
     * 获取代商平台销售列表
     *
     * @return 平台销售列表
     */
    private List<SalesEntity> getSaleList() {
        return this.salesService.lambdaQuery()
                .eq(SalesEntity::getSalesType, UserEnums.salesType.ADMIN.getCode())
                .eq(SalesEntity::getIsChoose, 1)
                .eq(SalesEntity::getUserPolling, 1)
                .orderByAsc(SalesEntity::getId)
                .list();
//        String saleListKey = RedisPowerKeyCache.REDIS_POWER_SALE_LIST_KEY;
//        List<SalesEntity> result = new ArrayList<>();
//        Object obj = null;
//        try {
//            obj = this.redisTemplate.opsForValue().get(saleListKey);
//        } catch (Exception e) {
//            log.error("redis error:{}", e.getMessage());
//        }
//        if (obj != null) {
//            if (obj instanceof String) {
//                try {
//                    result = JSON.parseArray((String) obj, SalesEntity.class);
//                } catch (Exception e) {
//                    log.error("从redis获取代商平台销售列表失败 error: {}", e.getMessage());
//                    throw new BusinessException(StatusCode.PARAM_EX);
//                }
//                return result;
//            }
//        } else {
//            result = this.salesService.lambdaQuery()
//                    .eq(SalesEntity::getSalesType, UserEnums.salesType.ADMIN.getCode())
//                    .eq(SalesEntity::getIsChoose, 1)
//                    .list();
//
//            try {
//                // 缓存5分钟
//                this.redisTemplate.opsForValue().set(saleListKey, JSON.toJSONString(result), Duration.ofMinutes(5));
//            } catch (Exception e) {
//                log.error("redis set error : {}", e.getMessage());
//            }
//        }
//        return result;
    }


    /**
     * 通过销售id列表查询
     * @param list
     * @return
     */
    @Override
    public List<SalesInfoVo> selectBySalesIds(List<Long> list) {
        if (list != null && !list.isEmpty()){
            List<SalesEntity> salesEntities = salesService.listByIds(list);
            if (salesEntities != null && !salesEntities.isEmpty()){
                return BeanUtil.copyToList(salesEntities, SalesInfoVo.class);
            }
        }
        return List.of();
    }


    /**
     * 通过销售id列表查询过滤未启用的
     * @param salesIds
     * @return
     */
    @Override
    public List<SalesInfoVo> listByIdsFilterNoChoose(List<Long> salesIds) {
        if (salesIds != null && !salesIds.isEmpty()){
            List<SalesEntity> list = salesService.lambdaQuery().in(SalesEntity::getId, salesIds).eq(SalesEntity::getIsChoose, 0).list();
            if (list != null && !list.isEmpty()){
                return BeanUtil.copyToList(list, SalesInfoVo.class);
            }
        }
        return List.of();
    }

    @Override
    public SalesInfoVo getByUserId(Long userId) {
        UserDetailsEntity userDetails = userDetailsService.getOne(new LambdaQueryWrapper<UserDetailsEntity>()
                .eq(UserDetailsEntity::getUserId, userId)
                .last("limit 1")
        );
        if (userDetails == null || userDetails.getSaleId() == null) {
            return null;
        }

        SalesEntity entity = salesService.getById(userDetails.getSaleId());

        if (entity == null) {
            return null;
        }

        return BeanUtil.copyProperties(entity, SalesInfoVo.class);
    }

    @Override
    public List<SalesEntity> listAllBySalesType() {
        return salesService.list();
    }

    @Override
    public List<SalesEntity> listPlatformSales(Integer employeeStatus) {
        return salesService.lambdaQuery()
                .eq(SalesEntity::getSalesType, 0)
                .eq(ObjectUtil.isNotEmpty(employeeStatus), SalesEntity::getEmployeeStatus, employeeStatus)
                .list();
    }

    @Override
    public List<SalesEntity> listAgentSales(Long agentId, String phone, Integer employeeStatus) {
        return salesService.lambdaQuery()
                .eq(SalesEntity::getSalesType, 1)
                .eq(agentId != null, SalesEntity::getAgentId, agentId)
                .eq(ObjectUtil.isNotEmpty(phone), SalesEntity::getPhone, phone)
                .eq(ObjectUtil.isNotEmpty(employeeStatus), SalesEntity::getEmployeeStatus, employeeStatus)
                .list()
                ;
    }

    @Override
    public List<SalesInfoVo> listBySalesTypeAndAgentId(Integer salesType, Long agentId, Integer userPolling) {
        if (salesType == null && agentId == null) {
            log.error("salesType and agentId is null!");
            throw new BusinessException(StatusCode.PARAM_EX.getCode(), "销售类型和代理商ID不能同时为空");
        }
        return salesService.list(new LambdaQueryWrapper<SalesEntity>()
                        .eq(SalesEntity::getIsChoose, 1)
                        .eq(userPolling != null, SalesEntity::getUserPolling, 1)
                        .eq(salesType != null, SalesEntity::getSalesType, salesType)
                        .eq(agentId != null, SalesEntity::getAgentId, agentId)
                        .orderByAsc(SalesEntity::getId)
                )
                .stream()
                .map(entity -> BeanUtil.copyProperties(entity, SalesInfoVo.class))
                .filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toList());
    }

    @Override
    public void checkSaveOrUpdate(SalesBo salesBo) {
        if (ObjectUtil.isNotEmpty(salesBo.getId())) {
            Long count = salesService.lambdaQuery()
                    .eq(SalesEntity::getPhone, salesBo.getPhone())
                    .ne(SalesEntity::getId, salesBo.getId())
                    .count();
            if (count > 0) {
                throw new BusinessException(StatusCode.PARAM_EX.getCode(), "销售中手机号已存在");
            }
        } else {
            Long count = salesService.lambdaQuery()
                    .eq(SalesEntity::getPhone, salesBo.getPhone())
                    .count();
            if (count > 0) {
                throw new BusinessException(StatusCode.PARAM_EX.getCode(), "销售中手机号已存在");
            }
        }
    }

    @Override
    public SalesInfoVo getBySalesUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SalesEntity salesEntity = salesService.lambdaQuery()
                .eq(SalesEntity::getUserId, userId)
                .last("limit 1")
                .one();
        if (salesEntity == null) {
            return null;
        }

        return BeanUtil.copyProperties(salesEntity, SalesInfoVo.class);
    }

    @Override
    public List<SalesInfoVo> listByUserIds(List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return List.of();
        }
        List<SalesEntity> list = salesService.lambdaQuery()
                .in(SalesEntity::getUserId, userIds)
                .list();
        if (ObjectUtil.isEmpty(list)) {
            return List.of();
        }
        return BeanUtil.copyToList(list, SalesInfoVo.class);
    }

    @Override
    public void updateNameAndPhoneByUserId(Long userId, String nickName, String phone) {
        if (userId != null && (ObjectUtil.isNotEmpty(nickName) || ObjectUtil.isNotEmpty(phone))) {
            salesService.lambdaUpdate()
                    .eq(SalesEntity::getUserId, userId)
                    .set(SalesEntity::getSalesName, nickName)
                    .set(SalesEntity::getPhone, phone)
                    .update();
        }
    }

    @Override
    public void updateEmployeeStatus(Long id, Integer employeeStatus) {
        Integer isChoose = null, userPolling = null;
        if (employeeStatus == 0) {
            isChoose = 0;
            userPolling = 0;
        }
        salesService.lambdaUpdate()
                .eq(SalesEntity::getId, id)
                .set(SalesEntity::getEmployeeStatus, employeeStatus)
                .set(ObjectUtil.isNotEmpty(isChoose), SalesEntity::getIsChoose, isChoose)
                .set(ObjectUtil.isNotEmpty(userPolling), SalesEntity::getUserPolling, userPolling)
                .update();
    }

    @Override
    public void updateEmployeeStatusByAgentId(Long agentId, Integer employeeStatus) {
        Integer isChoose = null, userPolling = null;
        if (employeeStatus == 0) {
            isChoose = 0;
            userPolling = 0;
        }
        salesService.lambdaUpdate()
                .eq(SalesEntity::getAgentId, agentId)
                .eq(SalesEntity::getSalesType, UserEnums.salesType.AGENT.getCode())
                .set(SalesEntity::getEmployeeStatus, employeeStatus)
                .set(ObjectUtil.isNotEmpty(isChoose), SalesEntity::getIsChoose, isChoose)
                .set(ObjectUtil.isNotEmpty(userPolling), SalesEntity::getUserPolling, userPolling)
                .update();
    }
}

