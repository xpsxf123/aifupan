package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bo.AddAnchorKeywordsBo;
import com.jiuyu.replay.words.bo.RemoveAnchorKeywordsBo;
import com.jiuyu.replay.words.constant.SystemKeyConstant;
import com.jiuyu.replay.words.entity.AnchorCruxWordsEntity;
import com.jiuyu.replay.words.entity.AnchorCruxWordsRelaEntity;
import com.jiuyu.replay.words.rse.AnchorCruxWordsRse;
import com.jiuyu.replay.words.rse.AnchorRse;
import com.jiuyu.replay.words.rse.AnchorUrlUserRse;
import com.jiuyu.replay.words.rse.AnchorVideoRse;
import com.jiuyu.replay.words.vo.AnchorCruxWordsVo;
import com.jiuyu.replay.words.vo.AnchorVideoSimpleVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主播关键词业务逻辑层
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@Component
public class AnchorCruxWordsBll {

    /**
     * 默认视频时长要求，单位：秒
     */
    private static final Integer DEFAULT_VIDEO_DURATION = 1800;

    @Resource
    private AnchorCruxWordsRse anchorCruxWordsRse;
    @Resource
    private SystemKvService systemKvService;
    @Resource
    private AnchorUrlUserRse anchorUrlUserRse;
    @Resource
    private AnchorVideoRse anchorVideoRse;
    @Resource
    private UserFeign userFeign;
    @Resource
    private AnchorRse anchorRse;

    /**
     * 根据主播secUid查询关键词列表
     *
     * @param secUid 主播secUid
     * @return 关键词列表
     */
    public R<List<AnchorCruxWordsVo>> listBySecUid(String secUid) {
        if (!StringUtils.hasText(secUid)) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播secUid不能为空");
        }

        List<AnchorCruxWordsVo> list = anchorCruxWordsRse.listBySecUid(secUid);
        return R.ok("获取成功", list);
    }

    /**
     * 获取昨日新添加并且没有关键词的主播
     *
     * @return 主播视频列表
     */
    public List<AnchorVideoSimpleVo> listYesterdayAnchorWithoutKeyword() {
        // 1. 获取视频时长要求
        Integer videoDuration = systemKvService.getValueByKey(SystemKeyConstant.generateAnchorKeywordVideoDuration, DEFAULT_VIDEO_DURATION);

        // 2. 获取昨日新添加的主播
        Set<String> yesterdaySecUids = anchorUrlUserRse.listYesterdayAnchorSecUids();
        if (yesterdaySecUids.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 过滤掉已有关键词的主播
        Set<String> existKeywordSecUids = anchorCruxWordsRse.listExistKeywordSecUids(yesterdaySecUids);
        List<String> noKeywordSecUids = yesterdaySecUids.stream()
                .filter(secUid -> !existKeywordSecUids.contains(secUid))
                .collect(Collectors.toList());

        if (noKeywordSecUids.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 获取符合条件的视频记录（时长 >= 要求，analysis_status = 2）
        Map<String, String> latestVideoMap = anchorVideoRse.listLatestVideoBySecUidsAndDuration(noKeywordSecUids, videoDuration);
        if (latestVideoMap.isEmpty()) {
            return new ArrayList<>();
        }

        // 5. 组装返回结果
        List<AnchorVideoSimpleVo> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : latestVideoMap.entrySet()) {
            AnchorVideoSimpleVo vo = new AnchorVideoSimpleVo();
            vo.setSecUid(entry.getKey());
            vo.setVideoId(entry.getValue());
            result.add(vo);
        }

        return result;
    }

    /**
     * 获取最近添加并且没有关键词并且有符合时长视频的主播
     *
     * @param limit 最大返回数量
     * @return 主播视频列表
     */
    public List<AnchorVideoSimpleVo> listRecentAnchorWithoutKeyword(int limit) {
        // 1. 获取视频时长要求
        Integer videoDuration = systemKvService.getValueByKey(SystemKeyConstant.generateAnchorKeywordVideoDuration, DEFAULT_VIDEO_DURATION);

        List<AnchorVideoSimpleVo> result = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 600;

        while (result.size() < limit) {
            // 2. 分页获取最近添加的主播
            List<String> recentSecUids = anchorRse.listRecentAnchorSecUids(pageNum, pageSize);
            if (recentSecUids.isEmpty()) {
                break;
            }

            // 3. 过滤掉已有关键词的主播
            Set<String> existKeywordSecUids = anchorCruxWordsRse.listExistKeywordSecUids(recentSecUids);
            List<String> noKeywordSecUids = recentSecUids.stream()
                    .filter(secUid -> !existKeywordSecUids.contains(secUid))
                    .collect(Collectors.toList());

            if (!noKeywordSecUids.isEmpty()) {
                // 4. 获取符合条件的视频记录（时长 >= 要求，analysis_status = 2）
                Map<String, String> latestVideoMap = anchorVideoRse.listLatestVideoBySecUidsAndDuration(noKeywordSecUids, videoDuration);
                for (Map.Entry<String, String> entry : latestVideoMap.entrySet()) {
                    AnchorVideoSimpleVo vo = new AnchorVideoSimpleVo();
                    vo.setSecUid(entry.getKey());
                    vo.setVideoId(entry.getValue());
                    result.add(vo);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }

            // 已经是最后一页，退出循环
            if (recentSecUids.size() < pageSize) {
                break;
            }
            pageNum++;
        }

        return result;
    }

    /**
     * 校验后返回videoId
     *
     * @param secUid 主播
     * @return video
     */
    public AnchorVideoSimpleVo changeYesToVideoId(String secUid) {
        // 1. 获取视频时长要求
        Integer videoDuration = systemKvService.getValueByKey(SystemKeyConstant.generateAnchorKeywordVideoDuration, DEFAULT_VIDEO_DURATION);

        // 2. 获取符合条件的视频记录（时长 >= 要求，analysis_status = 2）
        Map<String, String> latestVideoMap = anchorVideoRse.listLatestVideoBySecUidsAndDuration(List.of(secUid), videoDuration);
        if (latestVideoMap.isEmpty()) {
            return null;
        }

        AnchorVideoSimpleVo res = new AnchorVideoSimpleVo();
        res.setSecUid(secUid);
        res.setVideoId(latestVideoMap.get(secUid));
        return res;
    }

    /**
     * 保存主播的关键词
     *
     * @param secUid   主播secUid
     * @param keywords 关键词列表
     * @param userId   用户id（可为空）
     * @param tenantId 租户id（可为空）
     * @return 保存结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Void> saveAnchorKeywords(String secUid, List<String> keywords, Long userId, Long tenantId) {
        if (!StringUtils.hasText(secUid)) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播secUid不能为空");
        }
        if (keywords == null || keywords.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "关键词列表不能为空");
        }

        // 去掉多余的关键词
        int keySize = systemKvService.getValueByKey("generate_anchor_keyword_size", 10);
        if (keywords.size() > keySize) {
            keywords = keywords.subList(0, keySize);
        }

        // 1. 过滤空白关键词
        List<String> validKeywords = keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (validKeywords.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "有效关键词不能为空");
        }

        // 2. 根据关键词列表批量获取旧的关键词
        Map<String, AnchorCruxWordsEntity> existKeywordMap = anchorCruxWordsRse.listByKeywords(validKeywords);

        // 3. 找出不存在的关键词，保存到主播关键词表
        List<String> newKeywords = validKeywords.stream()
                .filter(k -> !existKeywordMap.containsKey(k))
                .collect(Collectors.toList());

        Date now = new Date();
        if (!newKeywords.isEmpty()) {
            List<AnchorCruxWordsEntity> newKeywordEntities = new ArrayList<>();
            for (int i = 0; i < newKeywords.size(); i++) {
                AnchorCruxWordsEntity entity = new AnchorCruxWordsEntity();
                entity.setId(SnowflakeManager.nextValue());
                entity.setKeyword(newKeywords.get(i));
                entity.setCreateDate(now);
                entity.setUpdateDate(now);
                newKeywordEntities.add(entity);
                // 放入map便于后续关联
                existKeywordMap.put(newKeywords.get(i), entity);
            }
            anchorCruxWordsRse.saveBatchKeywords(newKeywordEntities);
        }

        // 4. 查询已存在的主播关键词关联
        List<Long> allKeywordIds = validKeywords.stream()
                .map(existKeywordMap::get)
                .filter(Objects::nonNull)
                .map(AnchorCruxWordsEntity::getId)
                .collect(Collectors.toList());
        Set<Long> existRelaKeywordIds = anchorCruxWordsRse.listExistRelaKeywordIds(secUid, allKeywordIds);

        // 5. 保存主播和关键词的关联（过滤已存在的关联）
        List<AnchorCruxWordsRelaEntity> relaList = new ArrayList<>();
        for (String keyword : validKeywords) {
            AnchorCruxWordsEntity keywordEntity = existKeywordMap.get(keyword);
            if (keywordEntity != null && !existRelaKeywordIds.contains(keywordEntity.getId())) {
                AnchorCruxWordsRelaEntity rela = new AnchorCruxWordsRelaEntity();
                rela.setId(SnowflakeManager.nextValue());
                rela.setSecUid(secUid);
                rela.setKeywordId(keywordEntity.getId());
                rela.setUserId(userId);
                rela.setTenantId(tenantId);
                rela.setCreateDate(now);
                rela.setUpdateDate(now);
                relaList.add(rela);
            }
        }
        if (!relaList.isEmpty()) {
            anchorCruxWordsRse.saveBatchRela(relaList);
        }

        return R.ok("保存成功");
    }

    /**
     * 保存主播的关键词-先清除旧的关键词
     *
     * @param secUid   主播secUid
     * @param keywords 关键词列表
     * @param userId   用户id（可为空）
     * @param tenantId 租户id（可为空）
     * @return 保存结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Void> clearAndSaveAnchorKeywords(String secUid, List<String> keywords, Long userId, Long tenantId) {
        if (!StringUtils.hasText(secUid)) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播secUid不能为空");
        }
        if (keywords == null || keywords.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "关键词列表不能为空");
        }

        this.anchorCruxWordsRse.deleteAnchorKeywords(secUid);

        saveAnchorKeywords(secUid, keywords, userId, tenantId);

        return R.ok("保存成功");
    }

    /**
     * 添加主播关键词
     *
     * @param bo 添加主播关键词请求参数
     * @return 添加结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> addKeywords(AddAnchorKeywordsBo bo) {
        if (!StringUtils.hasText(bo.getSecUid())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播secUid不能为空");
        }
        if (bo.getKeywords() == null || bo.getKeywords().isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "关键词列表不能为空");
        }

        // 过滤空白关键词
        List<String> validKeywords = bo.getKeywords().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (validKeywords.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "有效关键词不能为空");
        }

        // 获取当前登录用户信息
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        Long userId = user.getId();
        Long tenantId = user.getActiveTenantId();

        // 调用已有的保存方法
        R<Void> result = saveAnchorKeywords(bo.getSecUid(), validKeywords, userId, tenantId);
        if (result.getCode() != 0) {
            return R.error(result.getCode(), result.getMsg());
        }

        return R.ok("添加成功");
    }

    /**
     * 移除主播关键词
     *
     * @param bo 移除主播关键词请求参数
     * @return 移除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> removeKeywords(RemoveAnchorKeywordsBo bo) {
        if (!StringUtils.hasText(bo.getSecUid())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播secUid不能为空");
        }
        if (bo.getKeywordIds() == null || bo.getKeywordIds().isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "关键词ID列表不能为空");
        }

        // 过滤空值
        List<Long> validKeywordIds = bo.getKeywordIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (validKeywordIds.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "有效关键词ID不能为空");
        }

        int count = anchorCruxWordsRse.removeByKeywordIds(bo.getSecUid(), validKeywordIds);
        return R.ok("移除成功，共移除" + count + "个关键词");
    }

}
