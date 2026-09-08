package com.jiuyu.replay.agent.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleBo;
import com.jiuyu.replay.agent.bo.AgentPlatformSaleListBo;
import com.jiuyu.replay.agent.constant.RedisAgentKeyCache;
import com.jiuyu.replay.agent.entity.AgentPlatformSaleEntity;
import com.jiuyu.replay.agent.producer.AgentPlatformSaleProducer;
import com.jiuyu.replay.agent.repository.service.AgentPlatformSaleService;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleListVo;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleVo;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.power.SalesFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 代理商平台销售
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Service
@Slf4j
public class AgentPlatformSaleProducerImpl implements AgentPlatformSaleProducer {

    @Resource
    private AgentPlatformSaleService agentPlatformSaleService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SalesFeign salesFeign;




    @Override
    public PageUtils<AgentPlatformSaleListVo> queryPage(AgentPlatformSaleListBo agentPlatformSaleListBo) {
        QueryWrapper<AgentPlatformSaleEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(agentPlatformSaleListBo.getKeyword())){
            wrapper.like("name", agentPlatformSaleListBo.getKeyword());
        }

        IPage<AgentPlatformSaleEntity> iPage = agentPlatformSaleService.page(new Query<AgentPlatformSaleEntity>().getPage(agentPlatformSaleListBo.getPage(), agentPlatformSaleListBo.getLimit()), wrapper);

        PageUtils<AgentPlatformSaleListVo> pageUtils = new PageUtils<>(agentPlatformSaleListBo.getPage(), agentPlatformSaleListBo.getLimit(), iPage);

        List<AgentPlatformSaleEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AgentPlatformSaleListVo> vos = records.stream().map(item -> {
                AgentPlatformSaleListVo agentPlatformSaleVo = new AgentPlatformSaleListVo();
                BeanUtils.copyProperties(item, agentPlatformSaleVo);
                return agentPlatformSaleVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AgentPlatformSaleInfoVo info(Long id) {

        AgentPlatformSaleEntity agentPlatformSaleEntity = agentPlatformSaleService.getById(id);
        if(agentPlatformSaleEntity != null) {
            AgentPlatformSaleInfoVo agentPlatformSaleInfoVo = new AgentPlatformSaleInfoVo();
            BeanUtils.copyProperties(agentPlatformSaleEntity, agentPlatformSaleInfoVo);
            return agentPlatformSaleInfoVo;
        }

        return null;
    }


    /**
     * 添加代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     * @return
     */
    @Override
    public AgentPlatformSaleInfoVo save(AgentPlatformSaleBo agentPlatformSaleBo) {

        //校验 销售人员
        exCant(agentPlatformSaleBo.getSaleId());

         AgentPlatformSaleEntity agentPlatformSaleEntity = new AgentPlatformSaleEntity();
         BeanUtils.copyProperties(agentPlatformSaleBo, agentPlatformSaleEntity);
         agentPlatformSaleEntity.setId(SnowflakeManager.nextValue());
         agentPlatformSaleEntity.setCreateDate(new Date());
         agentPlatformSaleEntity.setUpdateDate(new Date());

         agentPlatformSaleService.save(agentPlatformSaleEntity);

         AgentPlatformSaleInfoVo agentPlatformSaleInfoVo = new AgentPlatformSaleInfoVo();
         BeanUtils.copyProperties(agentPlatformSaleEntity, agentPlatformSaleInfoVo);

         return agentPlatformSaleInfoVo;
     }


    /**
     * 修改代理商平台销售
     * @param agentPlatformSaleBo 代理商平台销售对象
     */
    @Override
    public void update(AgentPlatformSaleBo agentPlatformSaleBo) {

        //校验 销售人员
        exCant(agentPlatformSaleBo.getSaleId());

        AgentPlatformSaleEntity agentPlatformSaleEntity = new AgentPlatformSaleEntity();
        BeanUtils.copyProperties(agentPlatformSaleBo, agentPlatformSaleEntity);
        agentPlatformSaleEntity.setUpdateDate(new Date());

        agentPlatformSaleService.updateById(agentPlatformSaleEntity);
    }

    @Override
    public void deleteById(Long id) {

        agentPlatformSaleService.removeById(id);
    }

    @Override
    public List<AgentPlatformSaleInfoVo> listByAgentId(Long agentId, Integer salesType) {
        return this.agentPlatformSaleService.listAgentSales(agentId, salesType);
    }

    @Override
    public AgentPlatformSaleInfoVo infoByAgentIdAndSaleId(Long agentId, Long saleId) {

        QueryWrapper<AgentPlatformSaleEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId);
        wrapper.eq("sale_id", saleId);
        AgentPlatformSaleEntity platformSaleEntity = this.agentPlatformSaleService.getOne(wrapper);
        if(platformSaleEntity != null) {
            AgentPlatformSaleInfoVo agentPlatformSaleInfoVo = new AgentPlatformSaleInfoVo();
            BeanUtils.copyProperties(platformSaleEntity, agentPlatformSaleInfoVo);
            return agentPlatformSaleInfoVo;
        }

        return null;
    }


    /**
     * 轮询获取代理平台销售
     * @param agentId 代理商id
     * @return
     */
    @Override
    public Long getPollingSaleId(Long agentId) {

        // 获取平台销售列表
        List<AgentPlatformSaleInfoVo> saleList = getSaleList(agentId);
        if (saleList == null || saleList.isEmpty()) {
            log.info("[轮询销售] 获取平台销售列表 size = 0");
            return null;
        }
        log.info("[轮询销售] 获取平台销售列表 size = {}, saleList = {}", saleList.size(), JSONUtil.toJsonStr(saleList));

        // 获取索引
        long saleIndex = getSaleIndex(agentId);
        log.info("[轮询销售] 获取索引 index = {}", saleIndex);

        // 取模获取下标
        int index = (int) (saleIndex % saleList.size());
        if (index > saleList.size() - 1) {
            log.error("[轮询销售] 取模获取下标异常, agentId = {}", agentId);
            return null;
        }

        AgentPlatformSaleInfoVo sale = saleList.get(index);
        log.info("[轮询销售] 根据下标获取销售，id = {}，名称 = {}", sale.getId(), sale.getSalesName());
        return sale.getSaleId();
    }

    @Override
    public long countBySaleId(Long salesId) {
        if (salesId == null) {
            return 0;
        }
        return agentPlatformSaleService.lambdaQuery()
                .eq(AgentPlatformSaleEntity::getSaleId, salesId)
                .count()
                ;
    }

    /**
     * 获取代商平台销售索引
     *
     * @param agentId 代理商id
     * @return 索引
     */
    private long getSaleIndex(Long agentId) {
        String saleIndexKey = RedisAgentKeyCache.REDIS_AGENT_SALE_INDEX_KEY + agentId;
        Long index = 1L;
        try {
            index = this.stringRedisTemplate.opsForValue().increment(saleIndexKey, 1L);

            // 索引初始值为1, 则设置过期时间 30天
            if (index != null && index == 1L) {
                stringRedisTemplate.expire(saleIndexKey, 30, TimeUnit.DAYS);
            }

            // index 超过 10000 重置为0
            if (index != null && index > 10000) {
                // 重置索引成1
                this.stringRedisTemplate.opsForValue().set(saleIndexKey, "1", 30, TimeUnit.DAYS);
                index = 1L;
            }
        } catch (Exception e) {
            log.error("从redis获取代商平台销售索引失败, error: {}", e.getMessage());
        }
        return index == null ? 0L : index - 1;
    }

    /**
     *
     * @param sales
     * @return
     */
    private List<AgentPlatformSaleInfoVo> salesToAgentPlatformSaleInfoVo(List<SalesInfoVo> sales) {
        if (sales != null && !sales.isEmpty()) {
            return sales.stream().map(item -> {
                AgentPlatformSaleInfoVo e = new AgentPlatformSaleInfoVo();
                e.setSalesName(item.getSalesName());
                e.setPhone(item.getPhone());
                e.setSaleId(item.getId());
                e.setSalesType(item.getSalesType());
                return e;
            }).collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * 获取代商平台销售列表
     * @param agentId  代商id
     * @return 平台销售列表
     */
    private List<AgentPlatformSaleInfoVo> getSaleList(Long agentId) {
        Map<Integer, List<AgentPlatformSaleInfoVo>> salesMap = this.agentPlatformSaleService.getValidAgentSaleList(agentId)
                .stream()
                .filter(item -> ObjectUtil.isNotEmpty(item.getSalesType()))
                .collect(Collectors.groupingBy(AgentPlatformSaleInfoVo::getSalesType));

        // 判断代理商是否有设置代理商销售
        List<AgentPlatformSaleInfoVo> agentSalesList = salesMap.get(UserEnums.salesType.AGENT.getCode());
        if (ObjectUtil.isNotEmpty(agentSalesList)) {
            return agentSalesList;
        }

        // 判断代理商是否创建了代理商销售
        List<SalesInfoVo> agentSales = salesFeign.listByAndAgentSalesTypeId(agentId, UserEnums.salesType.AGENT.getCode(), 1);
        if (ObjectUtil.isNotEmpty(agentSales)) {
            return salesToAgentPlatformSaleInfoVo(agentSales);
        }

        // 判断代理商是否有设置平台销售
        List<AgentPlatformSaleInfoVo> platformSalesList = salesMap.get(UserEnums.salesType.ADMIN.getCode());
        if (ObjectUtil.isNotEmpty(platformSalesList)) {
            return platformSalesList;
        }

        // 判断系统是否创建了平台销售
        List<SalesInfoVo> platformSales = salesFeign.listByAndAgentSalesTypeId(null, UserEnums.salesType.ADMIN.getCode(), 1);
        if (ObjectUtil.isNotEmpty(platformSales)) {
            return salesToAgentPlatformSaleInfoVo(platformSales);
        }
        return List.of();
//
//
//        String saleListKey = RedisAgentKeyCache.REDIS_AGENT_SALE_LIST_KEY + agentId;
//        List<AgentPlatformSaleInfoVo> result = new ArrayList<>();
//        Object obj = null;
//        try {
//            obj = this.redisTemplate.opsForValue().get(saleListKey);
//        } catch (Exception e) {
//            log.error("redis error:{}", e.getMessage());
//        }
//        if (obj != null) {
//            if (obj instanceof String) {
//                try {
//                    result = JSON.parseArray((String) obj, AgentPlatformSaleInfoVo.class);
//                } catch (Exception e) {
//                    log.error("从redis获取代商平台销售列表失败 agentId = {}, error: {}", agentId, e.getMessage());
//                    throw new BusinessException(StatusCode.PARAM_EX);
//                }
//                return result;
//            }
//        } else {
//            result = this.agentPlatformSaleService.getValidAgentSaleList(agentId);
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
     * 代理商 添加/修改  销售
     * 限制方法，如果你想绑定的销售是  不参与轮询的就不允许添加到代理商销售
     * 校验 销售人员 是否开启分配轮询
     */
    private void exCant(Long saleId){
        if (saleId==null||saleId == 0) {
            return;
        }
        R<SalesInfoVo> salesInfoVoR = salesFeign.exCant(saleId);
        if (salesInfoVoR == null || salesInfoVoR.getData() == null){
            throw new BusinessException(StatusCode.BASE_VALID_PARAM.getCode(), "该销售查询失败，请重新选择销售");
        }
//        SalesInfoVo sales = salesInfoVoR.getData();
//        if (sales.getIsChoose()==0){
//            throw new BusinessException(StatusCode.BASE_VALID_PARAM.getCode(), "该销售人员不参与分配，请更换");
//        }
    }

    /**
     * 获取 不参与分配销售的  销售
     * @param agentPlatformSaleInfoVos
     * @return
     */
    private List<Long> exCantIds(List<AgentPlatformSaleInfoVo> agentPlatformSaleInfoVos){
        List<Long> saleIds = agentPlatformSaleInfoVos.stream()
                .map(AgentPlatformSaleVo::getSaleId)
                .filter(item -> item!=null&&item != 0L).toList();
        // 调用接口获取排除的销售ID列表
        R<List<Long>> response = salesFeign.exCantIds(saleIds);
        // 判断调用是否成功
        if (response != null && ObjectUtil.isNotEmpty(response.getData())) {
            // 可根据实际项目选择抛出异常、记录日志或返回空列表
            return response.getData();
        }
        return Collections.emptyList();
    }


}

