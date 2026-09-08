package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserInfoExportVo;
import com.jiuyu.replay.generic.vo.power.UserVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlListVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlVo;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.generic.vo.words.anchor.AnchorUrlTradeVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.AnchorUrlProducer;
import com.jiuyu.replay.words.repository.dao.AnchorUrlUserDao;
import com.jiuyu.replay.words.repository.dao.AnchorUrlWhiteDao;
import com.jiuyu.replay.words.repository.dao.AnchorVideoDao;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.repository.service.AnchorVideoDetailService;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.repository.service.impl.AnchorUrlWhiteServiceImpl;
import com.jiuyu.replay.words.repository.service.impl.AnchorVideoServiceImpl;
import com.jiuyu.replay.words.vo.AnchorClientVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Service
public class AnchorUrlProducerImpl implements AnchorUrlProducer {

    @Resource
    private AnchorUrlService anchorUrlService;
    @Resource
    private AnchorUrlWhiteDao anchorUrlWhiteDao;
    @Resource
    private AnchorUrlUserDao anchorUrlUserDao;
    @Resource
    private AnchorVideoDao anchorVideoDao;
    @Resource
    private AnchorUrlUserService anchorUrlUserService;
    @Resource
    private TradeService tradeService;
    @Autowired
    private AnchorUrlWhiteServiceImpl anchorUrlWhiteService;
    @Autowired
    private AnchorVideoServiceImpl anchorVideoService;
    @Resource
    private AnchorVideoDetailService anchorVideoDetailService;

    @Override
    public PageUtils<AnchorUrlListVo> queryPage(AnchorUrlListBo anchorUrlListBo) {
        QueryWrapper<AnchorUrlEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(anchorUrlListBo.getKeyword())){
            wrapper.like("name", anchorUrlListBo.getKeyword());
        }

        IPage<AnchorUrlEntity> iPage = anchorUrlService.page(new Query<AnchorUrlEntity>().getPage(anchorUrlListBo.getPage(), anchorUrlListBo.getLimit()), wrapper);

        PageUtils<AnchorUrlListVo> pageUtils = new PageUtils<>(anchorUrlListBo.getPage(), anchorUrlListBo.getLimit(), iPage);

        List<AnchorUrlEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AnchorUrlListVo> vos = records.stream().map(item -> {
                AnchorUrlListVo anchorUrlVo = new AnchorUrlListVo();
                BeanUtils.copyProperties(item, anchorUrlVo);
                return anchorUrlVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AnchorUrlInfoVo info(Long id) {

        AnchorUrlEntity anchorUrlEntity = anchorUrlService.getById(id);
        if(anchorUrlEntity != null) {
            AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
            BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);
            return anchorUrlInfoVo;
        }

        return null;

    }

    /**
     * 新增主播url
     * @param anchorUrlBo 主播url对象
     * @return
     */
     public AnchorUrlInfoVo save(AnchorUrlBo anchorUrlBo) {

         AnchorUrlEntity anchorUrlEntity = new AnchorUrlEntity();
         BeanUtils.copyProperties(anchorUrlBo, anchorUrlEntity);
         anchorUrlEntity.setId(SnowflakeManager.nextValue());

         if(!StringUtils.isEmpty(anchorUrlBo.getHomeUrl())) {
             if(anchorUrlBo.getHomeUrl().contains("douyin")) {
                 anchorUrlEntity.setPlatform(0);
                 anchorUrlEntity.setPlatformResource("DouYinHomeLive");
             }else if(anchorUrlBo.getHomeUrl().contains("kuaishou")) {
                 anchorUrlEntity.setPlatform(1);
                 anchorUrlEntity.setPlatformResource("KuaiShouHomeLive");
             }else if(anchorUrlBo.getLiveUrl().contains("weixin")) {
                 anchorUrlEntity.setPlatform(2);
                 anchorUrlEntity.setPlatformResource("WeChatChannelsLive");
             }
         }

         if(!StringUtils.isEmpty(anchorUrlBo.getLiveUrl())) {
             if(anchorUrlBo.getLiveUrl().contains("douyin")) {
                 anchorUrlEntity.setPlatform(0);
                 anchorUrlEntity.setPlatformResource("DouYinLive");
             }else if(anchorUrlBo.getLiveUrl().contains("kuaishou")) {
                 anchorUrlEntity.setPlatform(1);
                 anchorUrlEntity.setPlatformResource("KuaiShouLive");
             }else if(anchorUrlBo.getLiveUrl().contains("weixin")) {
                 anchorUrlEntity.setPlatform(2);
                 anchorUrlEntity.setPlatformResource("WeChatChannelsLive");
             }
         }

         anchorUrlEntity.setCreateDate(new Date());
         anchorUrlEntity.setUpdateDate(new Date());
         anchorUrlService.save(anchorUrlEntity);

         AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
         BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);

         return anchorUrlInfoVo;
     }

    /**
     * 修改主播url
     * @param anchorUrlBo 主播url对象
     * @return
     */
    public void update(AnchorUrlBo anchorUrlBo) {

        AnchorUrlEntity anchorUrlEntity = new AnchorUrlEntity();
        BeanUtils.copyProperties(anchorUrlBo, anchorUrlEntity);

        anchorUrlService.updateById(anchorUrlEntity);
    }

    /**
     * 删除主播url
     * @param id 主播urlid
     * @return
     */
    public void deleteById(Long id) {

        anchorUrlService.removeById(id);
    }

    @Override
    public AnchorUrlInfoVo infoBySecUid(String secUid) {

        AnchorUrlEntity anchorUrlEntity = anchorUrlService.getOne(new QueryWrapper<AnchorUrlEntity>().eq("sec_uid", secUid));

        if(anchorUrlEntity != null) {
            AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
            BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);
            return anchorUrlInfoVo;
        }

        return null;
    }

    @Override
    public List<AnchorUrlInfoVo> listBySecUids(Collection<String> secUidList) {

        if(secUidList != null && secUidList.size() > 0) {

            QueryWrapper<AnchorUrlEntity> wrapper = new QueryWrapper<>();
            wrapper.in("sec_uid", secUidList);

            List<AnchorUrlEntity> anchorUrlEntities = anchorUrlService.list(wrapper);

            if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
                return anchorUrlEntities.stream().map(item -> {
                    AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                    BeanUtils.copyProperties(item, anchorUrlInfoVo);
                    return anchorUrlInfoVo;
                }).toList();
            }
        }

        return null;
    }

    @Override
    public List<AnchorUrlInfoVo> listAnchorByUserIdAndSecUids(Long userId, Long tenantId, Collection<String> secUids) {

        if(secUids != null && secUids.size() > 0) {

            QueryWrapper<AnchorUrlUserEntity> anchorUserWrapper = new QueryWrapper<>();
            anchorUserWrapper.eq("user_id", userId);
            anchorUserWrapper.eq("tenant_id", tenantId);
            anchorUserWrapper.in("anchor_url_sec_uid", secUids);
            List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(anchorUserWrapper);

            if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
                List<String> userSecUids = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();
                Map<String, AnchorUrlUserEntity> userAnchorMap = anchorUrlUserEntities.stream().collect(Collectors.toMap(AnchorUrlUserEntity::getAnchorUrlSecUid, item -> item, (o1, o2) -> o2));

                List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", userSecUids));
                if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
                    return anchorUrlEntities.stream().map(item -> {
                        AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                        BeanUtils.copyProperties(item, anchorUrlInfoVo);
                        // 设置主播名称为备注名称
                        AnchorUrlUserEntity anchorUrlUserEntity = userAnchorMap.get(anchorUrlInfoVo.getSecUid());
                        if(anchorUrlUserEntity != null) {
                            anchorUrlInfoVo.setAnchorName(anchorUrlUserEntity.getRemarksName());
                        }
                        return anchorUrlInfoVo;
                    }).toList();
                }
            }
        }

        return null;
    }

    @Override
    public List<AnchorUrlInfoVo> listByConditions(List<String> secUidList) {

        if(secUidList != null && secUidList.size() > 0) {

            QueryWrapper<AnchorUrlEntity> wrapper = new QueryWrapper<>();
            wrapper.and(w -> {
                w.in("sec_uid", secUidList).or().in("home_url", secUidList).or().in("live_url", secUidList);
            });

            List<AnchorUrlEntity> anchorUrlEntities = anchorUrlService.list(wrapper);

            if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
                return anchorUrlEntities.stream().map(item -> {
                    AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                    BeanUtils.copyProperties(item, anchorUrlInfoVo);
                    anchorUrlInfoVo.setId(0L);
                    return anchorUrlInfoVo;
                }).toList();
            }
        }

        return null;
    }

    @Override
    public void deleteBatchBySecUids(List<String> secUids) {

        this.anchorUrlService.remove(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));
    }

    @Override
    public void saveBatch(List<AnchorUrlBo> liveAnchorList) {

        if(liveAnchorList != null && liveAnchorList.size() > 0) {
            List<AnchorUrlEntity> anchorUrlEntities = liveAnchorList.stream().map(item -> {
                AnchorUrlEntity anchorUrlEntity = new AnchorUrlEntity();
                anchorUrlEntity.setId(SnowflakeManager.nextValue());
                BeanUtils.copyProperties(item, anchorUrlEntity);
                return anchorUrlEntity;
            }).toList();

            this.anchorUrlService.saveBatch(anchorUrlEntities);
        }
    }

    @Override
    public AnchorUrlInfoVo infoByCondition(String secUid, String liveUrl, String homeUrl) {
        if(!StringUtils.isEmpty(secUid) || !StringUtils.isEmpty(liveUrl) || !StringUtils.isEmpty(homeUrl)) {
            QueryWrapper<AnchorUrlEntity> wrapper = new QueryWrapper<>();
            if(!StringUtils.isEmpty(secUid)) {
                wrapper.eq("sec_uid", secUid);
            }else if(!StringUtils.isEmpty(liveUrl)) {
                wrapper.eq("live_url", liveUrl);
            }else if(!StringUtils.isEmpty(homeUrl)) {
                wrapper.eq("home_url", homeUrl);
            }

            AnchorUrlEntity anchorUrlEntity = this.anchorUrlService.getOne(wrapper);
            if(anchorUrlEntity != null) {
                AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);
                return anchorUrlInfoVo;
            }
        }

        return null;
    }

    @Override
    public List<AnchorUrlVo> lists(List<String> secUids) {
        List<AnchorUrlVo> listsVo =new ArrayList<>();
        if(secUids != null && secUids.size() > 0){
            List<AnchorUrlEntity> secUid = this.anchorUrlService.list(
                    new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));
            for (AnchorUrlEntity anchorUrlEntity : secUid) {
                AnchorUrlVo anchorUrlVo = new AnchorUrlVo();
                BeanUtils.copyProperties(anchorUrlEntity, anchorUrlVo);
                listsVo.add(anchorUrlVo);
            }
        }
        return listsVo;
    }

    /**
     * 服务端获取主播列表
     * @param anchorUrlPegBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorUrlVo>> seletAnchorUrl(AnchorUrlPegBo anchorUrlPegBo) {
        IPage<AnchorUrlEntity> iPage = anchorUrlService.pageList(new Query<AnchorUrlEntity>().getPage(anchorUrlPegBo.getPage(), anchorUrlPegBo.getLimit()), anchorUrlPegBo);

        PageUtils<AnchorUrlVo> pageUtils = new PageUtils<>(anchorUrlPegBo.getPage(), anchorUrlPegBo.getLimit(), iPage);

        List<AnchorUrlEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<String> secUids = records.stream().map(AnchorUrlEntity::getSecUid).distinct().toList();

            Map<String, Long> whiteMap = anchorUrlWhiteService.list(new QueryWrapper<AnchorUrlWhiteEntity>()
                            .select("sec_uid", "count(*) as user_id")
                            .lambda()
                            .in(AnchorUrlWhiteEntity::getSecUid, secUids)
                            .groupBy(AnchorUrlWhiteEntity::getSecUid))
                    .stream()
                    .collect(Collectors.toMap(AnchorUrlWhiteEntity::getSecUid, AnchorUrlWhiteEntity::getUserId));

            Map<String, Long> anchorUrlUserMap = anchorUrlUserService.list(new QueryWrapper<AnchorUrlUserEntity>()
                            .select("anchor_url_sec_uid", "count(*) as user_id")
                            .lambda()
                            .in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUids)
                            .groupBy(AnchorUrlUserEntity::getAnchorUrlSecUid))
                    .stream()
                    .collect(Collectors.toMap(AnchorUrlUserEntity::getAnchorUrlSecUid, AnchorUrlUserEntity::getUserId));

            Map<String, Long> videoMap = anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>()
                            .select("sec_uid", "count(*) as user_id")
                            .lambda()
                            .in(AnchorVideoEntity::getSecUid, secUids)
                            .groupBy(AnchorVideoEntity::getSecUid))
                    .stream()
                    .collect(Collectors.toMap(AnchorVideoEntity::getSecUid, AnchorVideoEntity::getUserId));

            // 获取主播行业
            List<AnchorUrlTradeVo> tempList = new ArrayList<>();
            List<AnchorUrlTradeVo> list1 = anchorUrlUserService.list(new LambdaQueryWrapper<AnchorUrlUserEntity>()
                            .select(AnchorUrlUserEntity::getAnchorUrlSecUid, AnchorUrlUserEntity::getTradeId, AnchorUrlUserEntity::getUserId, AnchorUrlUserEntity::getTenantId)
                            .in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUids))
                    .stream()
                    .map(item -> {
                        AnchorUrlTradeVo temp = BeanUtil.copyProperties(item, AnchorUrlTradeVo.class);
                        temp.setSecUid(item.getAnchorUrlSecUid());
                        return temp;
                    })
                    .toList();
            if (ObjectUtil.isNotEmpty(list1)) {
                tempList.addAll(list1);
            }
            // 获取视频行业
//            List<AnchorUrlTradeVo> list2 = anchorVideoDetailService.tradeListBySecUids(secUids);
//            if (ObjectUtil.isNotEmpty(list2)) {
//                tempList.addAll(list2);
//            }

            // 回显行业名称
            List<Long> tradeIds = tempList.stream().map(AnchorUrlTradeVo::getTradeId).distinct().toList();
            if (ObjectUtil.isNotEmpty(tradeIds)) {
                List<TradeEntity> list = tradeService.list(new LambdaQueryWrapper<TradeEntity>().in(TradeEntity::getId, tradeIds).select(TradeEntity::getName, TradeEntity::getId));
                DataUtils.setFieldNameById(tempList, "tradeId", "tradeName", list, "id", "name");
            }
            
            // 获取系统行业和AI纠正行业信息
            Set<Long> systemTradeIdSet = new HashSet<>();
            Set<Long> aiCorrectTradeIdSet = new HashSet<>();
            for (AnchorUrlEntity entity : records) {
                if (ObjectUtil.isNotEmpty(entity.getSystemTradeId())) {
                    systemTradeIdSet.add(entity.getSystemTradeId());
                }
                if (ObjectUtil.isNotEmpty(entity.getAiCorrectTradeId())) {
                    aiCorrectTradeIdSet.add(entity.getAiCorrectTradeId());
                }
            }
            
            // 获取系统行业名称
            Map<Long, String> systemTradeNameMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(systemTradeIdSet)) {
                List<TradeEntity> systemTradeList = tradeService.list(new LambdaQueryWrapper<TradeEntity>()
                        .in(TradeEntity::getId, systemTradeIdSet)
                        .select(TradeEntity::getName, TradeEntity::getId));
                systemTradeNameMap = systemTradeList.stream()
                        .collect(Collectors.toMap(TradeEntity::getId, TradeEntity::getName));
            }
            
            // 获取AI纠正行业名称
            Map<Long, String> aiCorrectTradeNameMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(aiCorrectTradeIdSet)) {
                List<TradeEntity> aiCorrectTradeList = tradeService.list(new LambdaQueryWrapper<TradeEntity>()
                        .in(TradeEntity::getId, aiCorrectTradeIdSet)
                        .select(TradeEntity::getName, TradeEntity::getId));
                aiCorrectTradeNameMap = aiCorrectTradeList.stream()
                        .collect(Collectors.toMap(TradeEntity::getId, TradeEntity::getName));
            }
            
            // 统计每个主播每个行业的用户数
            Map<String, List<AnchorUrlTradeVo>> tradeMap = tempList.stream()
                    .filter(item -> ObjectUtil.isNotEmpty(item.getSecUid()))
                    .collect(Collectors.groupingBy(
                            item -> item.getSecUid() + "_" + item.getTradeId(),
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    tradeList -> {
                                        if (!tradeList.isEmpty()) {
                                            AnchorUrlTradeVo first = tradeList.get(0);
                                            first.setUserCount(tradeList.size());
                                            return first;
                                        }
                                        return null;
                                    }
                            )
                    ))
                    .values()
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(AnchorUrlTradeVo::getSecUid));

            Map<Long, String> finalSystemTradeNameMap = systemTradeNameMap;
            Map<Long, String> finalAiCorrectTradeNameMap = aiCorrectTradeNameMap;
            List<AnchorUrlVo> vos = records.stream().map(item -> {
                AnchorUrlVo anchorUrlVo = new AnchorUrlVo();
                BeanUtils.copyProperties(item, anchorUrlVo);
                Long userCounts = whiteMap.get(item.getSecUid());
                anchorUrlVo.setUserCounts(userCounts == null ? 0 : userCounts.intValue());

                Long uCounts = anchorUrlUserMap.get(item.getSecUid());
                anchorUrlVo.setUCounts(uCounts == null ? 0 : uCounts.intValue());

                Long videoCounts = videoMap.get(item.getSecUid());
                anchorUrlVo.setVideCounts(videoCounts == null ? 0 : videoCounts.intValue());

                anchorUrlVo.setAnchorUrlTradeList(tradeMap.get(item.getSecUid()));
                
                // 设置系统行业信息
                if (ObjectUtil.isNotEmpty(item.getSystemTradeId())) {
                    anchorUrlVo.setSystemTradeId(item.getSystemTradeId());
                    anchorUrlVo.setSystemTradeName(finalSystemTradeNameMap.get(item.getSystemTradeId()));
                }
                
                // 设置AI纠正行业信息
                if (ObjectUtil.isNotEmpty(item.getAiCorrectTradeId())) {
                    anchorUrlVo.setAiCorrectTradeId(item.getAiCorrectTradeId());
                    anchorUrlVo.setAiCorrectTradeName(finalAiCorrectTradeNameMap.get(item.getAiCorrectTradeId()));
                }

                return anchorUrlVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return R.ok(pageUtils);
        // return null;
    }

    @Override
    public List<AnchorClientVo> listByUserId(Long userId) {

        List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(
                new QueryWrapper<AnchorUrlUserEntity>().eq("user_id", userId).orderByAsc("is_remove_record").orderByDesc("id"));

        if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
            List<String> secUids = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();

            List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));

            if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
                List<AnchorClientVo> anchorClientVos = anchorUrlEntities.stream().map(item -> {
                    AnchorClientVo anchorClientVo = new AnchorClientVo();
                    BeanUtils.copyProperties(item, anchorClientVo);
                    for (AnchorUrlUserEntity anchorUrlUserEntity : anchorUrlUserEntities) {
                        if (anchorUrlUserEntity.getAnchorUrlSecUid().equals(anchorClientVo.getSecUid())) {
                            anchorClientVo.setIsRemoveRecord(anchorUrlUserEntity.getIsRemoveRecord());
                            anchorClientVo.setIsAutoRecord(anchorUrlUserEntity.getIsAutoRecord());
                            anchorClientVo.setTradeId(anchorUrlUserEntity.getTradeId());
                            anchorClientVo.setIsBarrageMonitoring(anchorUrlUserEntity.getIsBarrageMonitoring());
                            anchorClientVo.setIsAutoUploadCloud(anchorUrlUserEntity.getIsAutoUploadCloud());
                            anchorClientVo.setIsTop(anchorUrlUserEntity.getIsTop());
                            anchorClientVo.setAddTopTime(anchorUrlUserEntity.getAddTopTime());
                            anchorClientVo.setLastRecordTime(anchorUrlUserEntity.getLastRecordTime());
                            break;
                        }
                    }
                    return anchorClientVo;
                }).toList();

                return anchorClientVos;
            }
        }

        return null;
    }


    /**
     * 只根据SecUid查询主播信息
     * @param secUid
     * @return
     */
    @Override
    public R<AnchorUrlInfoVo> infoBySecUidOne(String secUid) {
        AnchorUrlEntity urlEntity = this.anchorUrlService.getOne(new QueryWrapper<AnchorUrlEntity>().eq("sec_uid", secUid));

        if (urlEntity != null){
            Integer userCountBySecUid = this.anchorUrlUserDao.countBySecUid(secUid);
            Integer videoCountBySecUid = this.anchorVideoDao.countBySecUid(secUid);
            Integer userCounts = this.anchorUrlWhiteDao.getUserCounts(secUid);

            AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
            anchorUrlInfoVo.setUCounts(userCountBySecUid);
            anchorUrlInfoVo.setVideCounts(videoCountBySecUid);
            anchorUrlInfoVo.setUserCounts(userCounts);

            BeanUtils.copyProperties(urlEntity, anchorUrlInfoVo);
            return R.ok("",anchorUrlInfoVo);

        }

        return null;
    }

    /**
     * 根据user_id从主播用户关联表中查出用户关联的所有主播sec_uid,在拿sec_uid去查询主播信息
     * @param anchorUrlUserBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorUrlVo>> selectAnchorByUserId(AnchorUrlUserBo anchorUrlUserBo) {
        LambdaQueryWrapper<AnchorUrlUserEntity> UserWrapper = new LambdaQueryWrapper<AnchorUrlUserEntity>()
                .eq(ObjectUtil.isNotEmpty(anchorUrlUserBo.getUserId()), AnchorUrlUserEntity::getUserId, anchorUrlUserBo.getUserId())
                .eq(ObjectUtil.isNotEmpty(anchorUrlUserBo.getTenantId()), AnchorUrlUserEntity::getTenantId, anchorUrlUserBo.getTenantId())
                .select(AnchorUrlUserEntity::getAnchorUrlSecUid);
        List<AnchorUrlUserEntity> secUidList = this.anchorUrlUserService.list(UserWrapper);

        if (secUidList != null && secUidList.size() != 0){
        List<String> listSecUidS = secUidList.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();

        QueryWrapper<AnchorUrlEntity> anchorWrapper = new QueryWrapper<>();
        anchorWrapper.in("sec_uid", listSecUidS);

        IPage<AnchorUrlEntity> iPage = this.anchorUrlService.page(new Query<AnchorUrlEntity>().getPage(anchorUrlUserBo.getPage(), anchorUrlUserBo.getLimit()), anchorWrapper);
        PageUtils<AnchorUrlVo> pageUtils = new PageUtils<>(anchorUrlUserBo.getPage(), anchorUrlUserBo.getLimit(), iPage);

        List<AnchorUrlEntity> records = iPage.getRecords();

        if (records != null && records.size() > 0) {
            List<String> secUids = records.stream().map(AnchorUrlEntity::getSecUid).filter(StringUtil::isNotBlank).distinct().toList();
            List<AnchorUrlUserEntity> anchorUrlUserEntityList=anchorUrlUserService.listBySecUid(secUids, anchorUrlUserBo.getUserId(), anchorUrlUserBo.getTenantId());
            Map<String, AnchorUrlUserEntity> entityMap = anchorUrlUserEntityList.stream().collect(Collectors.toMap(AnchorUrlUserEntity::getAnchorUrlSecUid, Function.identity(), (o, n) -> n));
            List<AnchorUrlVo> voList = records.stream().map(item ->{
                AnchorUrlVo anchorUrlVo = new AnchorUrlVo();
                BeanUtils.copyProperties(item,anchorUrlVo);
                anchorUrlVo.setUserCounts(this.anchorUrlWhiteDao.getUserCounts(item.getSecUid()));
                anchorUrlVo.setUCounts(this.anchorUrlUserDao.countBySecUid(item.getSecUid()));
                anchorUrlVo.setVideCounts(this.anchorVideoDao.countBySecUid(item.getSecUid()));
                AnchorUrlUserEntity anchorUrlUserEntity = entityMap.get(item.getSecUid());
                if (anchorUrlUserEntity != null){
                    anchorUrlVo.setAccountType(anchorUrlUserEntity.getAccountType());
                    anchorUrlVo.setAnchorSituation(anchorUrlUserEntity.getAnchorSituation());
                }
                return anchorUrlVo;

            }).toList();
            pageUtils.setList(voList);
        }

        return R.ok("",pageUtils);
    }
        return R.ok("");
    }

    /**
     * 批量修改是否删除标记
     *
     * @param secUids
     * @param userId
     * @param isRemoveRecord
     */
    @Override
    public void updateIsRemoveRecord(List<String> secUids, Long userId, int isRemoveRecord) {
        if (ObjectUtil.isNotEmpty(secUids)){
            anchorUrlUserDao.update(new LambdaUpdateWrapper<AnchorUrlUserEntity>()
                    .set(AnchorUrlUserEntity::getIsRemoveRecord, isRemoveRecord)
                    .in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUids)
                    .eq(AnchorUrlUserEntity::getUserId, userId)
            );
        }
    }
    /**
     * 根据userId获取当前用户添加的主播信息
     * @param anchorUrlUserBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorUrlVo>> userAddAnchorRecord(AnchorUrlUserBo anchorUrlUserBo) {
        LambdaQueryWrapper<AnchorUrlUserEntity> UserWrapper = new LambdaQueryWrapper<AnchorUrlUserEntity>()
                .eq(ObjectUtil.isNotEmpty(anchorUrlUserBo.getUserId()), AnchorUrlUserEntity::getUserId, anchorUrlUserBo.getUserId())
                .eq(ObjectUtil.isNotEmpty(anchorUrlUserBo.getTenantId()), AnchorUrlUserEntity::getTenantId, anchorUrlUserBo.getTenantId());
        List<AnchorUrlUserEntity> secUidList = this.anchorUrlUserService.list(UserWrapper);

        if (secUidList != null && secUidList.size() != 0){
            List<String> listSecUidList = new ArrayList<>();
            for (AnchorUrlUserEntity anchorUrlUserEntity : secUidList) {
                if (anchorUrlUserEntity.getIsRemoveRecord() == 0) {
                    listSecUidList.add(anchorUrlUserEntity.getAnchorUrlSecUid());
                }
            }

            if (listSecUidList != null && listSecUidList.size() != 0){
                QueryWrapper<AnchorUrlEntity> anchorWrapper = new QueryWrapper<>();
                anchorWrapper.in("sec_uid", listSecUidList);
                IPage<AnchorUrlEntity> iPage = this.anchorUrlService.page(new Query<AnchorUrlEntity>().getPage(anchorUrlUserBo.getPage(), anchorUrlUserBo.getLimit()), anchorWrapper);
                PageUtils<AnchorUrlVo> pageUtils = new PageUtils<>(anchorUrlUserBo.getPage(), anchorUrlUserBo.getLimit(), iPage);

                List<AnchorUrlEntity> records = iPage.getRecords();
                if (records != null && records.size() > 0) {
                    List<String> secUids = records.stream().map(AnchorUrlEntity::getSecUid).filter(StringUtil::isNotBlank).distinct().toList();
                    List<AnchorUrlUserEntity> anchorUrlUserEntityList=anchorUrlUserService.listBySecUid(secUids, anchorUrlUserBo.getUserId(), anchorUrlUserBo.getTenantId());
                    Map<String, AnchorUrlUserEntity> entityMap = anchorUrlUserEntityList.stream().collect(Collectors.toMap(AnchorUrlUserEntity::getAnchorUrlSecUid, Function.identity(), (o, n) -> n));
                    List<AnchorUrlVo> voList = records.stream().map(item ->{
                        AnchorUrlVo anchorUrlVo = new AnchorUrlVo();
                        BeanUtils.copyProperties(item,anchorUrlVo);
                        anchorUrlVo.setUserCounts(this.anchorUrlWhiteDao.getUserCounts(item.getSecUid()));
                        anchorUrlVo.setUCounts(this.anchorUrlUserDao.countBySecUid(item.getSecUid()));
                        anchorUrlVo.setVideCounts(this.anchorVideoDao.countBySecUid(item.getSecUid()));
                        AnchorUrlUserEntity anchorUrlUserEntity = entityMap.get(item.getSecUid());
                        if (anchorUrlUserEntity!= null){
                            anchorUrlVo.setAccountType(anchorUrlUserEntity.getAccountType());
                            anchorUrlVo.setAnchorSituation(anchorUrlUserEntity.getAnchorSituation());
                        }
                        return anchorUrlVo;
                    }).toList();
                    pageUtils.setList(voList);
                }
                return R.ok("",pageUtils);
            }

        }
        return R.ok("");
    }

    @Override
    public List<UserInfoExportVo> exportUserInfoList(List<UserVo> userList) {

        if (userList == null || userList.size() == 0){
            return null;
        }

        List<AnchorUrlUserEntity> anchorUrlUserEntities = anchorUrlUserService.list(new QueryWrapper<AnchorUrlUserEntity>().in("user_id", userList.stream().map(UserVo::getId).toList()));

        List<AnchorUrlEntity> anchorUrlList = anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid",anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).distinct().toList()));
        Map<String, AnchorUrlEntity> anchorUrlCollect = anchorUrlList.stream().collect(Collectors.toMap(AnchorUrlEntity::getSecUid, Function.identity(), (a, b) -> b));

        List<TradeEntity> trade = tradeService.list();
        Map<Long, String> tradeCollect = trade.stream().collect(Collectors.toMap(TradeEntity::getId, TradeEntity::getName, (a, b) -> b));

        List<UserInfoExportVo> infoList = new ArrayList<>();

            for (int i = 0; i < userList.size(); i++) {
                UserVo item = userList.get(i);
                if (item.getUserType() == 0 || item.getUserType() == 2){
                    for (int j = 0; j < anchorUrlUserEntities.size(); j++) {

                        if (infoList.size() < 1000 ){
                            UserInfoExportVo exportVo = new UserInfoExportVo();
                            AnchorUrlUserEntity anchorUrlUserEntity = anchorUrlUserEntities.get(j);

                            if (anchorUrlUserEntity.getIsRemoveRecord() == 0){
                                if (item.getId().equals(anchorUrlUserEntity.getUserId())){
                                    exportVo.setAddAnchorDate(anchorUrlUserEntity.getCreateDate());
                                    exportVo.setAnchorTrade(tradeCollect.get(anchorUrlUserEntity.getTradeId()));

                                    AnchorUrlEntity anchorUrlEntity = anchorUrlCollect.get(anchorUrlUserEntity.getAnchorUrlSecUid());
                                    if (anchorUrlEntity != null){
                                        exportVo.setAnchorNickName(anchorUrlEntity.getAnchorName());
                                        String liveUrl = anchorUrlEntity.getLiveUrl();
                                        String[] parts = liveUrl.split("/");
                                        String url = parts[parts.length - 1];
                                        exportVo.setAnchorUrl(url);
                                    }else{
                                        continue;
                                    }
                                    exportVo.setNickName(item.getNickName());
                                    exportVo.setUserPhone(item.getPhone());
                                    exportVo.setUserCreateDate(item.getCreateDate());
                                    infoList.add(exportVo);
                                }
                            }
                        }else{
                            return infoList;
                        }
                    }
                }

            }

        return infoList;

//        List<List<UserInfoExportVo>> exportVoList = Userlist.stream().map(item ->{
//
//            List<AnchorUrlUserEntity> anchorUrlUserEntities = anchorUrlUserService.list(new QueryWrapper<AnchorUrlUserEntity>().eq("user_id", item.getId()));
//            List<UserInfoExportVo> infoList = new ArrayList<>();
//            for (int i = 0; i < anchorUrlUserEntities.size(); i++) {
//                UserInfoExportVo exportVo = new UserInfoExportVo();
//                AnchorUrlUserEntity anchorUrlUserEntity = anchorUrlUserEntities.get(i);
//                TradeEntity trade = tradeService.getById(anchorUrlUserEntity.getTradeId());
//                exportVo.setAnchorTrade(trade.getName());
//                exportVo.setAddAnchorDate(anchorUrlUserEntity.getCreateDate());
//
//                AnchorUrlEntity anchorUrlServiceById = anchorUrlService.getOne(new QueryWrapper<AnchorUrlEntity>().eq("sec_uid",anchorUrlUserEntity.getAnchorUrlSecUid()));
//                if (anchorUrlServiceById != null){
//                    exportVo.setAnchorNickName(anchorUrlServiceById.getAnchorName());
//                    String liveUrl = anchorUrlServiceById.getLiveUrl();
//                    String[] parts = liveUrl.split("/");
//                    String url = parts[parts.length - 1];
//
//                    exportVo.setAnchorUrl(url);
//                }else{
//                    exportVo.setAnchorNickName("当前暂无添加主播");
//                    exportVo.setAnchorUrl("");
//                }
//
//                exportVo.setNickName(item.getNickName());
//                exportVo.setUserPhone(item.getPhone());
//                exportVo.setUserCreateDate(item.getCreateDate());
//                infoList.add(exportVo);
//            }
//
//            return infoList;
//
//        }).toList();

//        return exportVoList;
    }

    @Override
    public boolean bindUserAnchor(UserAnchorBo userAnchorBo) {

        AnchorUrlEntity anchorUrlEntity = this.anchorUrlService.getOne(new QueryWrapper<AnchorUrlEntity>().eq("sec_uid", userAnchorBo.getSecUid()));
        if(anchorUrlEntity != null) {
            QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userAnchorBo.getUserId());
            wrapper.eq("anchor_url_sec_uid", userAnchorBo.getSecUid());
            wrapper.eq("tenant_id", userAnchorBo.getTenantId());
            AnchorUrlUserEntity anchorUrlUserEntity = this.anchorUrlUserService.getOne(wrapper);

            if(anchorUrlUserEntity != null) {
                anchorUrlUserEntity.setIsRemoveRecord(0);
                anchorUrlUserEntity.setIsAutoRecord(1);
                this.anchorUrlUserService.updateById(anchorUrlUserEntity);
            }else {
                anchorUrlUserEntity = new AnchorUrlUserEntity();
                anchorUrlUserEntity.setId(SnowflakeManager.nextValue());
                anchorUrlUserEntity.setAnchorUrlSecUid(userAnchorBo.getSecUid());
                anchorUrlUserEntity.setUserId(userAnchorBo.getUserId());
                anchorUrlUserEntity.setCreateDate(new Date());
                anchorUrlUserEntity.setUpdateDate(new Date());
                anchorUrlUserEntity.setTradeId(userAnchorBo.getTradeId());
                anchorUrlUserEntity.setIsRemoveRecord(0);
                anchorUrlUserEntity.setIsAutoRecord(1);
                anchorUrlUserEntity.setTenantId(userAnchorBo.getTenantId());
                anchorUrlUserEntity.setFolderName(WindowsFileUtils.sanitizeForFolderName(anchorUrlEntity.getAnchorName()));
                anchorUrlUserEntity.setRemarksName(anchorUrlEntity.getAnchorName());
                this.anchorUrlUserService.save(anchorUrlUserEntity);
            }

            return true;
        }

        return false;

    }



    @Override
    public List<AnchorUrlInfoVo> listByLikeName(String anchorName) {
        QueryWrapper<AnchorUrlEntity> wrapper = new QueryWrapper<>();
        wrapper.like("anchor_name", anchorName);
        List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(wrapper);
        if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
            List<AnchorUrlInfoVo> vos = anchorUrlEntities.stream().map(item -> {
                AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                BeanUtils.copyProperties(item, anchorUrlInfoVo);
                return anchorUrlInfoVo;
            }).collect(Collectors.toList());

            return vos;
        }
        return null;
    }

    @Override
    public List<AnchorUrlUserVo> listByUserIdAndSecUids(Long userId, Long tenantId, Collection<String> secUids) {

        if(secUids != null && secUids.size() > 0) {
            QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId);
            wrapper.eq("tenant_id", tenantId);
            wrapper.in("anchor_url_sec_uid", secUids);
            List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(wrapper);

            if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
                List<AnchorUrlUserVo> anchorUrlUserVos = anchorUrlUserEntities.stream().map(item -> {
                    AnchorUrlUserVo anchorUrlUserVo = new AnchorUrlUserVo();
                    BeanUtils.copyProperties(item, anchorUrlUserVo);
                    return anchorUrlUserVo;
                }).collect(Collectors.toList());

                return anchorUrlUserVos;
            }
        }

        return null;
    }

    @Override
    public List<AnchorUrlUserVo> listUserAnchorByUserIdsAndSecUids(Collection<Long> userIds, Long tenantId, Collection<String> secUids) {

        if(secUids != null && secUids.size() > 0) {
            // 获取用户主播列表
            QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
            wrapper.in("user_id", userIds);
            wrapper.eq("tenant_id", tenantId);
            wrapper.in("anchor_url_sec_uid", secUids);
            List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(wrapper);

            if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {

                // 获取主播列表
                List<String> secUidList = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();
                List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUidList));

                if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
                    Map<String, AnchorUrlEntity> anchorUrlMap = anchorUrlEntities.stream().collect(Collectors.toMap(AnchorUrlEntity::getSecUid, item -> item, (o1, o2) -> o1));

                    List<AnchorUrlUserVo> anchorUrlUserVos = anchorUrlUserEntities.stream().map(item -> {
                        AnchorUrlUserVo anchorUrlUserVo = new AnchorUrlUserVo();
                        BeanUtils.copyProperties(item, anchorUrlUserVo);
                        // 封装主播信息
                        AnchorUrlEntity anchorUrlEntity = anchorUrlMap.get(anchorUrlUserVo.getAnchorUrlSecUid());
                        if(anchorUrlEntity != null) {
                            AnchorUrlInfoVo anchorUrlInfoVo = BeanConvertUtils.convert(anchorUrlEntity, AnchorUrlInfoVo.class);
                            anchorUrlInfoVo.setAnchorName(anchorUrlUserVo.getRemarksName());
                            anchorUrlUserVo.setAnchorInfo(anchorUrlInfoVo);
                        }
                        return anchorUrlUserVo;
                    }).collect(Collectors.toList());

                    return anchorUrlUserVos;
                }
            }
        }

        return null;
    }


    @Override
    public List<Long> selectQuery(AnchorUrlBo anchorUrlBo) {
        return anchorUrlUserService.lambdaQuery().eq(AnchorUrlUserEntity::getAccountType, 0)
                .select(AnchorUrlUserEntity::getUserId)
                .groupBy(AnchorUrlUserEntity::getUserId)
                .having("COUNT(*) >= {0}",anchorUrlBo.getOwnCount()).list().stream().map(AnchorUrlUserEntity::getUserId).toList();
    }


    @Override
    public Map<Long, Long> countIHave(List<Long> longList) {
        return anchorUrlUserService.lambdaQuery()
                .select(AnchorUrlUserEntity::getUserId, AnchorUrlUserEntity::getAccountType)
                .in(AnchorUrlUserEntity::getUserId, longList)
                .eq(AnchorUrlUserEntity::getAccountType, 0)
                .list().stream().collect(Collectors.groupingBy(AnchorUrlUserEntity::getUserId, Collectors.counting()));
    }

    @Override
    public AnchorUrlInfoVo infoByAnchorNumber(String anchorNumber) {
        AnchorUrlEntity anchorUrlEntity = anchorUrlService.getOne(
                new QueryWrapper<AnchorUrlEntity>().eq("anchor_number", anchorNumber));

        if (anchorUrlEntity != null) {
            AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
            BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);
            return anchorUrlInfoVo;
        }

        return null;
    }

    @Override
    public boolean updateTrdeIdBySecUid(String secUid, Long aiTradeId, Long systemTradeId) {
        if (secUid == null || (aiTradeId == null && systemTradeId == null)) {
            return false;
        }
        Date now = new Date();
        return anchorUrlService.lambdaUpdate()
                .eq(AnchorUrlEntity::getSecUid, secUid)
                .set(ObjectUtil.isNotEmpty(aiTradeId), AnchorUrlEntity::getAiCorrectTradeId, aiTradeId)
                .set(ObjectUtil.isNotEmpty(aiTradeId), AnchorUrlEntity::getAddAiTradeDate, now)
                .set(ObjectUtil.isNotEmpty(systemTradeId), AnchorUrlEntity::getSystemTradeId, systemTradeId)
                .set(ObjectUtil.isNotEmpty(systemTradeId), AnchorUrlEntity::getUpdateSystemTradeDate, now)
                .update()
                ;
    }
}

