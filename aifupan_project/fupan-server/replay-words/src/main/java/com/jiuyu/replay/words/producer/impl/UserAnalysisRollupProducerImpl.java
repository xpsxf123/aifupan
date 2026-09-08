package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.power.UserVo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupBo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupListBo;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.UserAnalysisRollupProducer;
import com.jiuyu.replay.words.repository.service.UserAnalysisRollupService;
import com.jiuyu.replay.words.vo.UserAnalysisRollupInfoVo;
import com.jiuyu.replay.words.vo.UserAnalysisRollupListVo;
import com.jiuyu.replay.words.vo.analysisRollup.AnalysisRollupVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Service
@Slf4j
public class UserAnalysisRollupProducerImpl implements UserAnalysisRollupProducer {

    @Resource
    private UserAnalysisRollupService userAnalysisRollupService;


    @Override
    public PageUtils<UserAnalysisRollupListVo> queryPage(UserAnalysisRollupListBo userAnalysisRollupListBo) {
        QueryWrapper<UserAnalysisRollupEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(userAnalysisRollupListBo.getKeyword())){
            wrapper.like("name", userAnalysisRollupListBo.getKeyword());
        }

        IPage<UserAnalysisRollupEntity> iPage = userAnalysisRollupService.page(new Query<UserAnalysisRollupEntity>().getPage(userAnalysisRollupListBo.getPage(), userAnalysisRollupListBo.getLimit()), wrapper);

        PageUtils<UserAnalysisRollupListVo> pageUtils = new PageUtils<>(userAnalysisRollupListBo.getPage(), userAnalysisRollupListBo.getLimit(), iPage);

        List<UserAnalysisRollupEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UserAnalysisRollupListVo> vos = records.stream().map(item -> {
                UserAnalysisRollupListVo userAnalysisRollupVo = new UserAnalysisRollupListVo();
                BeanUtils.copyProperties(item, userAnalysisRollupVo);
                return userAnalysisRollupVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserAnalysisRollupInfoVo info(Long id) {

        UserAnalysisRollupEntity userAnalysisRollupEntity = userAnalysisRollupService.getById(id);
        if(userAnalysisRollupEntity != null) {
            UserAnalysisRollupInfoVo userAnalysisRollupInfoVo = new UserAnalysisRollupInfoVo();
            BeanUtils.copyProperties(userAnalysisRollupEntity, userAnalysisRollupInfoVo);
            return userAnalysisRollupInfoVo;
        }

        return null;
    }

    @Override
    public UserAnalysisRollupInfoVo save(UserAnalysisRollupBo userAnalysisRollupBo) {

         UserAnalysisRollupEntity userAnalysisRollupEntity = new UserAnalysisRollupEntity();
         BeanUtils.copyProperties(userAnalysisRollupBo, userAnalysisRollupEntity);
         userAnalysisRollupEntity.setId(SnowflakeManager.nextValue());
         userAnalysisRollupEntity.setCreateDate(new Date());
         userAnalysisRollupEntity.setUpdateDate(new Date());

         userAnalysisRollupService.save(userAnalysisRollupEntity);

         UserAnalysisRollupInfoVo userAnalysisRollupInfoVo = new UserAnalysisRollupInfoVo();
         BeanUtils.copyProperties(userAnalysisRollupEntity, userAnalysisRollupInfoVo);

         return userAnalysisRollupInfoVo;
     }

    @Override
    public void update(UserAnalysisRollupBo userAnalysisRollupBo) {

        UserAnalysisRollupEntity userAnalysisRollupEntity = new UserAnalysisRollupEntity();
        BeanUtils.copyProperties(userAnalysisRollupBo, userAnalysisRollupEntity);
        userAnalysisRollupEntity.setUpdateDate(new Date());

        userAnalysisRollupService.updateById(userAnalysisRollupEntity);
    }

    @Override
    public void deleteById(Long id) {

        userAnalysisRollupService.removeById(id);
    }
    // 日期格式化工具类
    class DateParser {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public static Date parseStringToDate(String dateString) throws ParseException {
            return DATE_FORMAT.parse(dateString);
        }
    }

    // 将 String 转换为 Date
    private static Date parseStringToDate(String dateString) throws ParseException {
        return DateParser.parseStringToDate(dateString);
    }
        /**
     * 定时更新用户分析汇总数据
     *
     * @param listUserIds          所有用户ID
     * @param videoAnalysisRecords 所有视频分析记录
     * @param fileAnalysisRecords  所有文件分析记录
     * @param anchorVideoEntities
     * @param uploadFileEntities
     * @return
     */
    @Override
    public void timingUpdateData(List<Long> listUserIds, List<VideoAnalysisRecordEntity> videoAnalysisRecords, List<UploadFileAnalysisRecordEntity> fileAnalysisRecords, List<AnchorVideoEntity> anchorVideoEntities, List<UploadFileEntity> uploadFileEntities) throws ParseException {
        List<UserAnalysisRollupEntity> rollupSaveEntities = new ArrayList<>();
        List<UserAnalysisRollupEntity> rollupUpdateEntities = new ArrayList<>();

        // 已有的用户汇总
        List<UserAnalysisRollupEntity> rollupList = userAnalysisRollupService.list();
        // 转成Map
        Map<Long,UserAnalysisRollupEntity> rollupMap = new HashMap<>();
        for (UserAnalysisRollupEntity rollup : rollupList) {
            rollupMap.put(rollup.getUserId(),rollup);
        }

        // // 得到每个用户视频记录表和文件记录表的数据
        Map<Long, List<AnchorVideoEntity>> anchorVideoMap = anchorVideoEntities.stream().collect(Collectors.groupingBy(AnchorVideoEntity::getUserId));
        Map<Long, List<UploadFileEntity>> uploadFileMap = uploadFileEntities.stream().collect(Collectors.groupingBy(UploadFileEntity::getUserId));

        // 分析总次数
        Map<Long,Long> videoRolluRecordMap = videoAnalysisRecords.stream().collect(Collectors.groupingBy(VideoAnalysisRecordEntity::getUserId,Collectors.counting()));
        Map<Long,Long> fileRolluRecordMap = fileAnalysisRecords.stream().collect(Collectors.groupingBy(UploadFileAnalysisRecordEntity::getUserId,Collectors.counting()));
        // 平均分析次数
        Map<Long,List<VideoAnalysisRecordEntity>> videoAverageRecordMap = videoAnalysisRecords.stream().collect(Collectors.groupingBy(VideoAnalysisRecordEntity::getUserId));
        Map<Long,List<UploadFileAnalysisRecordEntity>> fileAverageRecordMap = fileAnalysisRecords.stream().collect(Collectors.groupingBy(UploadFileAnalysisRecordEntity::getUserId));

        for (Long userId : listUserIds){
            UserAnalysisRollupEntity userAnalysisRollupEntity = new UserAnalysisRollupEntity();
            // 算出每个用户的分析总数
            int videoValue = videoRolluRecordMap.getOrDefault(userId, 0L).intValue();
            int fileValue = fileRolluRecordMap.getOrDefault(userId, 0L).intValue();
            int sumValue = 0;
            if (videoValue > 0 || fileValue > 0) {
                sumValue = videoValue + fileValue;
            }
            // 得到每个用户视频记录表和文件记录表的数据
            List<AnchorVideoEntity> anchorAverage = anchorVideoMap.getOrDefault(userId, Collections.emptyList());
            List<UploadFileEntity> uploadAverage = uploadFileMap.getOrDefault(userId, Collections.emptyList());
            // 根据时间排序组成一个集合
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 拿到视频分析记录和文件分析记录的时间
            List<String> oldTBDateList = Stream.concat(
                    anchorAverage.stream().map(AnchorVideoEntity::getAnalysisTime),
                    uploadAverage.stream().map(UploadFileEntity::getAnalysisTime)
            ).sorted().toList();
            // 将视频分析记录和文件分析记录的时间转换成 时间 格式
            List<Date> oldFinalDateList = new ArrayList<>();
            for (String s : oldTBDateList) {
                    Date Finaldate = sdf.parse(s);
                    oldFinalDateList.add(Finaldate);
            }

            // 得到每个用户视频分析记录表和文件分析记录表的数据
            List<VideoAnalysisRecordEntity> videoAverage = videoAverageRecordMap.getOrDefault(userId, Collections.emptyList());
            List<UploadFileAnalysisRecordEntity> fileAverage = fileAverageRecordMap.getOrDefault(userId, Collections.emptyList());
           // 根据时间排序组成一个集合
            List<Date> newTBDateList = Stream.concat(
                    videoAverage.stream().map(VideoAnalysisRecordEntity::getCreateDate),
                    fileAverage.stream().map(UploadFileAnalysisRecordEntity::getCreateDate)
            ).toList();

            // 将两张表的时间集合组成新的集合
            List<Date> collect = Stream.concat(oldFinalDateList.stream(), newTBDateList.stream()).sorted().collect(Collectors.toList());

            Date startDate = null;
            Date lastDate = null;
            int dayValue = 0;

            // 计算时间差
            if (collect.size() > 0 && collect != null){
                startDate = collect.get(0);
                lastDate = collect.get(collect.size() - 1);
                long startTime = startDate.getTime();
                long endTime = lastDate.getTime();
                long finalTime = endTime - startTime;
                long diffDays = 1;
                if(finalTime > 0){
                    diffDays = TimeUnit.MILLISECONDS.toDays(finalTime);
                }
                if (finalTime % TimeUnit.DAYS.toMillis(1) > 0){
                    diffDays++;
                }
                dayValue = sumValue / (int) diffDays;
            }
            // 是否已经拥有一条以上用户汇总数据
            if (rollupList != null && rollupList.size() > 0) {
                // 判断是新增数据还是修改
                UserAnalysisRollupEntity rollup = rollupMap.get(userId);
                if (rollup != null && rollup.getUserId().equals(userId)){
                        userAnalysisRollupEntity.setId(rollup.getId());
                        userAnalysisRollupEntity.setAnalysisSum(sumValue);
                        userAnalysisRollupEntity.setDayAverageAnalysis(dayValue);
                        userAnalysisRollupEntity.setLastAnalysis(lastDate);
                        userAnalysisRollupEntity.setCreateDate(rollup.getCreateDate());
                        userAnalysisRollupEntity.setUpdateDate(new Date());
                        rollupUpdateEntities.add(userAnalysisRollupEntity);
                    }else{
                        userAnalysisRollupEntity.setId(SnowflakeManager.nextValue());
                        userAnalysisRollupEntity.setUserId(userId);
                        userAnalysisRollupEntity.setAnalysisSum(sumValue);
                        userAnalysisRollupEntity.setDayAverageAnalysis(dayValue);
                        userAnalysisRollupEntity.setLastAnalysis(lastDate);
                        userAnalysisRollupEntity.setCreateDate(new Date());
                        userAnalysisRollupEntity.setUpdateDate(new Date());
                        rollupSaveEntities.add(userAnalysisRollupEntity);
                    }
            }else{
                userAnalysisRollupEntity.setId(SnowflakeManager.nextValue());
                userAnalysisRollupEntity.setUserId(userId);
                userAnalysisRollupEntity.setAnalysisSum(sumValue);
                userAnalysisRollupEntity.setDayAverageAnalysis(dayValue);
                userAnalysisRollupEntity.setLastAnalysis(lastDate);
                userAnalysisRollupEntity.setCreateDate(new Date());
                userAnalysisRollupEntity.setUpdateDate(new Date());
                rollupSaveEntities.add(userAnalysisRollupEntity);
            }
        }

        if (rollupSaveEntities.size() > 0){
            userAnalysisRollupService.saveBatch(rollupSaveEntities);
        }
        if (rollupUpdateEntities.size() > 0){
            userAnalysisRollupService.updateBatchById(rollupUpdateEntities);
        }
    }

    @Override
    public void timingUpdateDataAll(List<UserVo> userList) {
        if (ObjectUtils.isEmpty(userList)) {
            return;
        }
        // 已有的用户汇总
        log.info("[定时更新用户分析汇总数据] 开始查询需要的全部数据");
        long startAll = System.currentTimeMillis();

        log.debug("[定时更新用户分析汇总数据] 开始获取已有用户汇总数据");
        long start = System.currentTimeMillis();
        Map<Long, UserAnalysisRollupEntity> userRollupMap = userAnalysisRollupService.list()
                .stream()
                .collect(Collectors.toMap(UserAnalysisRollupEntity::getUserId, Function.identity(), (a, b) -> a));
        log.debug("[定时更新用户分析汇总数据] 获取已有用户汇总数据用时{}ms", System.currentTimeMillis() - start);

        // 获取全部分组后的视频分析记录
        log.debug("[定时更新用户分析汇总数据] 开始获取视频分析记录");
        start = System.currentTimeMillis();
        Map<Long, AnalysisRollupVo> videoRollupMap = userAnalysisRollupService.videoAnalysisGroupAll()
                .stream()
                .collect(Collectors.toMap(AnalysisRollupVo::getUserId, Function.identity(), (a, b) -> a));
        log.debug("[定时更新用户分析汇总数据] 获取视频分析记录用时{}ms", System.currentTimeMillis() - start);

        // 获取全部分组后的文件分析记录
        log.debug("[定时更新用户分析汇总数据] 开始获取文件分析记录");
        start = System.currentTimeMillis();
        Map<Long, AnalysisRollupVo> fileRollupMap = userAnalysisRollupService.fileAnalysisGroupAll()
                .stream()
                .collect(Collectors.toMap(AnalysisRollupVo::getUserId, Function.identity(), (a, b) -> a));
        log.debug("[定时更新用户分析汇总数据] 获取文件分析记录用时{}ms", System.currentTimeMillis() - start);

        // 获取对比分析记录
        log.debug("[定时更新用户分析汇总数据] 开始获取对比分析记录");
        start = System.currentTimeMillis();
        Map<Long, AnalysisRollupVo> contrastAnalysisMap = userAnalysisRollupService.contrastAnalysisGroupAll()
                .stream()
                .collect(Collectors.toMap(AnalysisRollupVo::getUserId, Function.identity(), (a, b) -> a));
        log.debug("[定时更新用户分析汇总数据] 获取对比分析记录用时{}ms", System.currentTimeMillis() - start);

        // 获取自用抖音数量
        log.debug("[定时更新用户分析汇总数据] 开始获取自用抖音数量");
        start = System.currentTimeMillis();
        Map<String, AnalysisRollupVo> ownCountMap = userAnalysisRollupService.ownCountGroupAll()
                .stream()
                .collect(Collectors.toMap(item -> item.getUserId() + "_" + item.getTenantId(), Function.identity(), (a, b) -> a));
        log.debug("[定时更新用户分析汇总数据] 获取自用抖音数量用时{}ms", System.currentTimeMillis() - start);

        // 获取邀请链接code和渠道名称
        log.debug("[定时更新用户分析汇总数据] 开始获取邀请链接code和渠道名称");
        start = System.currentTimeMillis();
        Map<String, InviteUrlCodeAndPromotionName> inviteCodeMap = userAnalysisRollupService.inviteCodeAndPromotionNameGroupAll()
                .stream()
                .collect(Collectors.toMap(InviteUrlCodeAndPromotionName::getUrlCode, Function.identity(), (a, b) -> a));
        log.debug("[定时更新用户分析汇总数据] 获取邀请链接code和渠道名称用时{}ms", System.currentTimeMillis() - start);


        log.info("[定时更新用户分析汇总数据] 获取全部数据用时{}ms", System.currentTimeMillis() - startAll);


        List<UserAnalysisRollupEntity> saveList = new ArrayList<>();
        List<UserAnalysisRollupEntity> updateList = new ArrayList<>();
        Date now = new Date();
        // 开始处理数据
        log.info("[定时更新用户分析汇总数据] 开始处理数据 用户数{}", userList.size());
        start = System.currentTimeMillis();
        for (UserVo user : userList) {
            long userId = user.getId();
            UserAnalysisRollupEntity item = new UserAnalysisRollupEntity();
            item.setUserId(userId);

            // 计算分析总数（视频分析 + 文件分析 + 对比分析）
            long videoCount = videoRollupMap.getOrDefault(userId, new AnalysisRollupVo()).getCount() != null ?
                    videoRollupMap.getOrDefault(userId, new AnalysisRollupVo()).getCount() : 0L;
            long fileCount = fileRollupMap.getOrDefault(userId, new AnalysisRollupVo()).getCount() != null ?
                    fileRollupMap.getOrDefault(userId, new AnalysisRollupVo()).getCount() : 0L;
            int analysisSum = (int) (videoCount + fileCount);
            item.setAnalysisSum(analysisSum);

            // 计算日平均分析
            AnalysisRollupVo videoRollup = videoRollupMap.get(userId);
            AnalysisRollupVo fileRollup = fileRollupMap.get(userId);
            Date minTime = Stream.of(videoRollup, fileRollup)
                    .filter(ObjectUtil::isNotEmpty)
                    .map(AnalysisRollupVo::getMinTime)
                    .filter(ObjectUtil::isNotEmpty)
                    .min(Date::compareTo)
                    .orElse(null);
            Date maxTime = Stream.of(videoRollup, fileRollup)
                    .filter(ObjectUtil::isNotEmpty)
                    .map(AnalysisRollupVo::getMaxTime)
                    .filter(ObjectUtil::isNotEmpty)
                    .max(Date::compareTo)
                    .orElse(null);
            int dayAverageAnalysis = (minTime != null && maxTime != null) ?
                    (int) DateUtil.between(minTime, maxTime, DateUnit.DAY) : 0;
            item.setDayAverageAnalysis(dayAverageAnalysis);

            // 设置最后分析时间
            item.setLastAnalysis(maxTime);

            // 设置对比分析次数
            long contrastCount = contrastAnalysisMap.getOrDefault(userId, new AnalysisRollupVo()).getCount() != null ?
                    contrastAnalysisMap.getOrDefault(userId, new AnalysisRollupVo()).getCount() : 0L;
            item.setContrastAnalysis((int) contrastCount);

            // 设置自用抖音号数量
            long ownCount = ownCountMap.getOrDefault(user.getId() + "_" + user.getActiveTenantId(), new AnalysisRollupVo()).getCount() != null ?
                    ownCountMap.getOrDefault(user.getId() + "_" + user.getActiveTenantId(), new AnalysisRollupVo()).getCount() : 0L;
            item.setOwnCount((int) ownCount);

            // 设置邀请链接code和渠道名称
            InviteUrlCodeAndPromotionName inviteUrlCodeAndPromotionName = inviteCodeMap.get(user.getInviteUrlCode());
            if (inviteUrlCodeAndPromotionName != null) {
                item.setPromotionName(inviteUrlCodeAndPromotionName.getPromotionName());
            }

            UserAnalysisRollupEntity entity = userRollupMap.get(userId);
            if (entity != null) {
                item.setId(entity.getId());
                item.setUpdateDate(now);
                updateList.add(item);
            } else {
                item.setId(SnowflakeManager.nextValue());
                item.setCreateDate(now);
                item.setUpdateDate(now);
                saveList.add(item);
            }

        }
        log.info("[定时更新用户分析汇总数据] 处理数据用时{}ms", System.currentTimeMillis() - start);

        // 保存和更新
        log.info("[定时更新用户分析汇总数据] 开始保存和更新, 保存{}条, 更新{}条", saveList.size(), updateList.size());
        start = System.currentTimeMillis();
        if (ObjectUtil.isNotEmpty(saveList)) {
            userAnalysisRollupService.saveBatch(saveList);
        }
        if (ObjectUtil.isNotEmpty(updateList)) {
            userAnalysisRollupService.updateBatchById(updateList);
        }
        log.info("[定时更新用户分析汇总数据] 保存和更新用时{}ms", System.currentTimeMillis() - start);
    }

    /**
     * 根据日平均分析查询
     * @param startDayAnalysis 日平均分析时间(开始查询条件)
     * @param endDayAnalysis 日平均分析时间(结束查询条件)
     * @return
     */
    @Override
    public List<Long> getUserIdsByDayAnalysis(Long startDayAnalysis, Long endDayAnalysis) {
        QueryWrapper<UserAnalysisRollupEntity> wrapper = new QueryWrapper<>();

        if (startDayAnalysis != null){
            wrapper.ge("day_average_analysis", startDayAnalysis);
        }
        if (endDayAnalysis != null){
            wrapper.le("day_average_analysis", endDayAnalysis);
        }
        List<UserAnalysisRollupEntity> listed = userAnalysisRollupService.list(wrapper);
        if (listed != null && listed.size() > 0){
            return listed.stream().map(UserAnalysisRollupEntity::getUserId).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 根据多久未分析去查询用户
     * @param startLongNotAnalysis 多久未分析时间(开始查询条件)
     * @param endLongNotAnalysis 多久未分析时间(结束查询条件)
     * @return
     */
    @Override
    public List<Long> getUserIdsByNotAnalysis(Long startLongNotAnalysis, Long endLongNotAnalysis) {
        List<UserAnalysisRollupEntity> rollupList = userAnalysisRollupService.list();
        Date currentDate = new Date();

        if (startLongNotAnalysis != null && endLongNotAnalysis != null){
            List<Long> userIds = rollupList.stream().map(item ->{
                if (item.getLastAnalysis() != null){
                    long finalTime = currentDate.getTime() - item.getLastAnalysis().getTime();
                    if (finalTime > 0){
                        long days = TimeUnit.MILLISECONDS.toDays(finalTime);
                        if (days >= startLongNotAnalysis && days <= endLongNotAnalysis ){
                            return item.getUserId();
                        }
                    }
                }
                return null;
            }).toList();
            return userIds;
        }

        if (startLongNotAnalysis != null && endLongNotAnalysis == null){
            List<Long> userIds = rollupList.stream().map(item ->{
                if (item.getLastAnalysis() != null){
                    long finalTime = currentDate.getTime() - item.getLastAnalysis().getTime();
                    if (finalTime > 0){
                        long days = TimeUnit.MILLISECONDS.toDays(finalTime);
                        if (days >= startLongNotAnalysis){
                            return item.getUserId();
                        }
                    }
                }
                return null;
            }).toList();
            return userIds;
        }

        return null;
    }

    /**
     * 查询所有用户的分析汇总
     * @return
     */
    @Override
    public List<UserAnalysisRollupEntity> listAll() {
        List<UserAnalysisRollupEntity> list = userAnalysisRollupService.list();
        return list;
    }
}