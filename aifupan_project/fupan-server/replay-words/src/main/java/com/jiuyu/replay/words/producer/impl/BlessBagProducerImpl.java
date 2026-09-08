package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.BlessBagBo;
import com.jiuyu.replay.words.bo.BlessBagListBo;
import com.jiuyu.replay.words.entity.BlessBagEntity;
import com.jiuyu.replay.words.producer.BlessBagProducer;
import com.jiuyu.replay.words.repository.service.BlessBagService;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.generic.vo.words.BlessBagListVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 福袋信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
@Service
@Slf4j
public class BlessBagProducerImpl implements BlessBagProducer {

    @Resource
    private BlessBagService blessBagService;
    @Resource
    private RedissonClient redissonClient;


    @Override
    public PageUtils<BlessBagListVo> queryPage(BlessBagListBo blessBagListBo) {
        QueryWrapper<BlessBagEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(blessBagListBo.getKeyword())){
            wrapper.like("name", blessBagListBo.getKeyword());
        }

        IPage<BlessBagEntity> iPage = blessBagService.page(new Query<BlessBagEntity>().getPage(blessBagListBo.getPage(), blessBagListBo.getLimit()), wrapper);

        PageUtils<BlessBagListVo> pageUtils = new PageUtils<>(blessBagListBo.getPage(), blessBagListBo.getLimit(), iPage);

        List<BlessBagEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<BlessBagListVo> vos = records.stream().map(item -> {
                BlessBagListVo blessBagVo = new BlessBagListVo();
                BeanUtils.copyProperties(item, blessBagVo);
                return blessBagVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public BlessBagInfoVo info(Long id) {

        BlessBagEntity blessBagEntity = blessBagService.getById(id);
        if(blessBagEntity != null) {
            BlessBagInfoVo blessBagInfoVo = new BlessBagInfoVo();
            BeanUtils.copyProperties(blessBagEntity, blessBagInfoVo);
            return blessBagInfoVo;
        }

        return null;
    }


    @Override
    public List<BlessBagInfoVo> infoByVideo(String videoId) {
        List<BlessBagEntity> list = blessBagService.lambdaQuery()
                .eq(BlessBagEntity::getVideoId, videoId).list();
        if(list != null) {
            List<BlessBagInfoVo> result = Convert.toList(BlessBagInfoVo.class, list);
            for (BlessBagInfoVo blessBagInfoVo : result) {
                if (Objects.nonNull(blessBagInfoVo.getLotteryInfo())){
                    JSONObject map =JSONUtil.parseObj(blessBagInfoVo.getLotteryInfo());
                    blessBagInfoVo.setBlessBagReward("总"+blessBagInfoVo.getPrizeCount()+map.getJSONObject("prize_info").get("name"));
                }
                if (blessBagInfoVo.getStartTime()!=null&&blessBagInfoVo.getDrawTime()!=null){
                    Date date1 = new Date(blessBagInfoVo.getStartTime()*1000);
                    Date date2 = new Date(blessBagInfoVo.getDrawTime()*1000);
                    // 定义输出格式（MM-dd HH:mm）
                    String pattern = "MM-dd HH:mm";
                    String pattern2 = "HH:mm";
                    // 格式化时间
                    String time1 = DateUtil.format(date1, pattern); // "04-24 08:36"
                    String time2 = DateUtil.format(date2, pattern2); // "09:15"
                    blessBagInfoVo.setBlessBagTime(time1 + "—" + time2);
                }
                if (Objects.nonNull(blessBagInfoVo.getConditions())){
                    List<JSONObject> mapList = JSONUtil.toList(blessBagInfoVo.getConditions(), JSONObject.class);
                    String string = mapList.stream()
                            .map(json -> json.getStr("description"))
                            .filter(StrUtil::isNotBlank)
                            .collect(Collectors.joining("/"));
                    blessBagInfoVo.setGetCondition(string);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }


    @Override
    public BlessBagInfoVo save(BlessBagBo blessBagBo) {
        BlessBagEntity blessBag = null;

        String lockKey = "blessBag_save_" + blessBagBo.getVideoId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 3. 尝试获取锁
            boolean isLocked = lock.tryLock(
                    10, // 等待时间
                    10, // 锁自动释放时间
                    TimeUnit.SECONDS
            );
            if (isLocked) {
                blessBag = blessBagService.lambdaQuery()
                        .eq(BlessBagEntity::getBatchNumber, blessBagBo.getBatchNumber())
                        .eq(BlessBagEntity::getVideoId, blessBagBo.getVideoId())
                        .eq(BlessBagEntity::getUserId, blessBagBo.getUserId())
                        .eq(BlessBagEntity::getTenantId, blessBagBo.getTenantId())
                        .eq(BlessBagEntity::getStartTime, blessBagBo.getStartTime())
                        .eq(BlessBagEntity::getDrawTime, blessBagBo.getDrawTime())
                        .last("limit 1")
                        .one();


                if (blessBag == null){
                    blessBag = new BlessBagEntity();
                    BeanUtils.copyProperties(blessBagBo, blessBag);
                    blessBag.setId(SnowflakeManager.nextValue());
                    blessBag.setCreateDate(new Date());
                    blessBag.setUpdateDate(new Date());
                    blessBagService.save(blessBag);
                }else{
                    BeanUtils.copyProperties(blessBagBo, blessBag, "id", "createDate");
                    blessBag.setUpdateDate(new Date());
                    blessBagService.updateById(blessBag);
                }
            }

        } catch (InterruptedException e) {
            log.error("获取分布式锁失败，Key: {}", lockKey);
        } finally {
            // 5. 释放锁（确保当前线程持有锁）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return blessBag == null ? null :info(blessBag.getId());
     }

    @Override
    public void update(BlessBagBo blessBagBo) {

        BlessBagEntity blessBagEntity = new BlessBagEntity();
        BeanUtils.copyProperties(blessBagBo, blessBagEntity);
        blessBagEntity.setUpdateDate(new Date());

        blessBagService.updateById(blessBagEntity);
    }

    @Override
    public void deleteById(Long id) {

        blessBagService.removeById(id);
    }

    @Override
    public List<BlessBagInfoVo> listByVideoId(String videoId) {

        List<BlessBagEntity> list = blessBagService.list(new LambdaQueryWrapper<BlessBagEntity>()
                .eq(BlessBagEntity::getVideoId, videoId)
        );

        if(list != null) {
            return BeanUtil.copyToList(list, BlessBagInfoVo.class);
        }
        return new ArrayList<>();
    }
}

