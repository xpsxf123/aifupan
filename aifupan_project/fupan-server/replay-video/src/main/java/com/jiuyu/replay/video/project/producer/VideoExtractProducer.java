package com.jiuyu.replay.video.project.producer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.video.common.enums.ExtractStatusEnum;
import com.jiuyu.replay.video.common.enums.PlatformTypeEnum;
import com.jiuyu.replay.video.common.service.MongoUpsertService;
import com.jiuyu.replay.video.project.bo.VideoExtractBatchDeleteBo;
import com.jiuyu.replay.video.project.bo.VideoExtractBo;
import com.jiuyu.replay.video.project.bo.VideoExtractQueryBo;
import com.jiuyu.replay.video.project.bo.VideoExtractRemoteVideoBo;
import com.jiuyu.replay.video.project.dao.VideoInfoDao;
import com.jiuyu.replay.video.project.document.VideoContentExtract;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.entity.VideoUserHotSubscriptionEntity;
import com.jiuyu.replay.video.project.entity.VideoUserInfluencerSubscriptionEntity;
import com.jiuyu.replay.video.project.entity.VideoUserVideoEntity;
import com.jiuyu.replay.video.project.repository.VideoExtractContentRepository;
import com.jiuyu.replay.video.project.service.*;
import com.jiuyu.replay.video.project.vo.OperationUserVo;
import com.jiuyu.replay.video.project.vo.VideoContentExtractVo;
import com.jiuyu.replay.video.project.vo.VideoExtractQueryVo;
import com.jiuyu.replay.video.project.vo.VideoExtractVo;
import com.jiuyu.replay.video.project.vo.video.ExtractPromptVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视频提取文案业务编排
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 短视频提取文案模块的业务逻辑聚合层
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoExtractProducer {

    private final UserFeign userFeign;
    private final VideoUserVideoService videoUserVideoService;
    private final VideoInfoService videoInfoService;
    private final VideoInfoDao videoInfoDao;
    private final VideoExtractContentRepository videoExtractContentRepository;
    private final MongoUpsertService mongoUpsertService;
    private final RedissonClient redissonClient;
    private final ImgOssUtils imgOssUtils;
    private final VideoExtractTransactionService videoExtractTransactionService;
    private final UserPropertyFeign userPropertyFeign;
    private final VideoUserInfluencerSubscriptionService videoUserInfluencerSubscriptionService;
    private final VideoUserHotSubscriptionService videoUserHotSubscriptionService;
    private final SystemKvService systemKvService;
    private final DictDataFeign dictDataFeign;


    /**
     * 视频提取文案通过url或者本地上传
     *
     * @param videoExtractBo 上传数据
     * @return 视频提取结果
     */
    @Transactional(rollbackFor = Exception.class)
    public VideoExtractVo extractFromUrlAndLocal(VideoExtractBo videoExtractBo) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户id不存在");

        // 根据提取状态处理不同的业务逻辑
        ExtractStatusEnum extractStatusEnum = ExtractStatusEnum.getByCode(videoExtractBo.getExtractStatus());
        switch (extractStatusEnum) {
            case PENDING:
                return handlePendingStatus(videoExtractBo, tenantId, userCacheVo);
            case PROCESSING:
                return handleProcessingStatus(videoExtractBo, tenantId, userCacheVo);
            case COMPLETED:
                return handleCompletedStatus(videoExtractBo, tenantId, userCacheVo);
            case FAILED:
                return handleFailedStatus(videoExtractBo, tenantId, userCacheVo);
            default:
                throw new BusinessException("视频状态错误！");
        }
    }

    /**
     * 查询历史提取记录
     *
     * @param videoExtractQueryBo 查询数据
     * @return
     */
    public PageUtils<VideoExtractQueryVo> queryHistoryRecord(VideoExtractQueryBo videoExtractQueryBo) {
        // 用户id
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        // 租户id
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        if (Objects.isNull(tenantId) || Objects.equals(tenantId, 0L)) {
            return new PageUtils<>(videoExtractQueryBo.getPage(), videoExtractQueryBo.getLimit());
        }

        LambdaQueryWrapper<VideoUserVideoEntity> queryWrapper = new LambdaQueryWrapper<>(VideoUserVideoEntity.class).eq(VideoUserVideoEntity::getTenantId, tenantId).orderByDesc(VideoUserVideoEntity::getCreatedDate);
        if (Objects.nonNull(videoExtractQueryBo.getOperatorUserId())) {
            queryWrapper.eq(VideoUserVideoEntity::getUserId, videoExtractQueryBo.getOperatorUserId());
        }
        if (Objects.nonNull(videoExtractQueryBo.getSourceType())) {
            queryWrapper.eq(VideoUserVideoEntity::getSourceType, videoExtractQueryBo.getSourceType());
        }
        if (Objects.nonNull(videoExtractQueryBo.getStartTime())) {
            queryWrapper.ge(VideoUserVideoEntity::getExtractTime, videoExtractQueryBo.getStartTime());
        }
        if (Objects.nonNull(videoExtractQueryBo.getEndTime())) {
            queryWrapper.le(VideoUserVideoEntity::getExtractTime, videoExtractQueryBo.getEndTime());
        }
        if (StrUtil.isNotBlank(videoExtractQueryBo.getVideoTitle())) {
            queryWrapper.like(VideoUserVideoEntity::getVideoTitle, videoExtractQueryBo.getVideoTitle().trim());
        }

        Page<VideoUserVideoEntity> page = videoUserVideoService.page(new Page<>(videoExtractQueryBo.getPage(), videoExtractQueryBo.getLimit()), queryWrapper);
        List<VideoUserVideoEntity> records = page.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return new PageUtils<>(page, Collections.emptyList());
        }
        // 提取用户Id
        List<Long> userIds = records.stream().map(VideoUserVideoEntity::getUserId).toList();
        List<UserDto> userDtoList = userFeign.listByIds(userIds);
        if (CollectionUtil.isEmpty(userDtoList)) {
            return new PageUtils<>(page, Collections.emptyList());
        }
        // 提取短视频id
        List<Long> videoIds = records.stream().map(VideoUserVideoEntity::getVideoId).toList();
        List<VideoInfoEntity> videoInfoEntityList = videoInfoService.listByIds(videoIds);
        if (CollectionUtil.isEmpty(videoInfoEntityList)) {
            return new PageUtils<>(page, Collections.emptyList());
        }
        // 组装数据
        Map<Long, UserDto> userDtoMap = userDtoList.stream().collect(Collectors.toMap(UserDto::getId, Function.identity(), (existing, replacement) -> existing));
        Map<Long, VideoInfoEntity> videoInfoEntityMap = videoInfoEntityList.stream().collect(Collectors.toMap(VideoInfoEntity::getId, Function.identity(), (existing, replacement) -> existing));
        ArrayList<VideoExtractQueryVo> videoExtractQueryVos = new ArrayList<>();
        records.forEach(record -> {

            VideoInfoEntity videoInfoEntity = videoInfoEntityMap.get(record.getVideoId());
            videoExtractQueryVos.add(VideoExtractQueryVo.builder()
                    .id(record.getId())
                    .videoId(record.getVideoId())
                    .extractId(record.getId())
                    .videoTitle(record.getVideoTitle())
                    .sourceType(record.getSourceType())
                    .videoUrl(videoInfoEntity != null ? videoInfoEntity.getVideoUrl() : null)
                    .coverUrl(videoInfoEntity != null ? getCoverUrl(videoInfoEntity.getCoverUrl(), videoInfoEntity.getPlatformType()) : null)
                    .duration(videoInfoEntity != null ? videoInfoEntity.getDuration() : null)
                    .userId(record.getUserId())
                    .nickName(userDtoMap.get(record.getUserId()) != null ? userDtoMap.get(record.getUserId()).getNickName() : null)
                    .extractTime(record.getExtractTime())
                    .extractStatus(record.getExtractStatus())
                    .extractErrorReason(record.getExtractErrorReason())
                    .build());
        });
        return new PageUtils<>(page, videoExtractQueryVos);
    }

    /**
     * 获取封面图片URL
     * 只有本地图片的URL是oss中的key
     *
     * @param coverUrl     url
     * @param platformType 平台类型不能为空
     * @return
     */
    private String getCoverUrl(String coverUrl, Byte platformType) {
        if (Objects.isNull(platformType)) {
            return null;
        }

        if (ObjectUtil.equals(platformType, PlatformTypeEnum.LOCAL_UPLOAD.getCode()) && !coverUrl.startsWith("http")) {
            coverUrl = imgOssUtils.getUrl(coverUrl);
        }
        return coverUrl;
    }

    /**
     * 批量删除视频提取记录
     *
     * @param batchDeleteBo 批量删除请求参数
     * @return 删除是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDelete(VideoExtractBatchDeleteBo batchDeleteBo) {
        // 获取ID列表
        List<Long> ids = batchDeleteBo.getIds();
        if (CollectionUtil.isEmpty(ids)) {
            return false;
        }

        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户id不存在");

        // 查询要删除的记录，验证权限
        List<VideoUserVideoEntity> videoUserVideoEntities = videoUserVideoService.listByIds(ids);
        if (CollectionUtil.isEmpty(videoUserVideoEntities)) {
            return false;
        }

        // 验证权限：只能删除自己租户下的记录
        List<VideoUserVideoEntity> validEntities = videoUserVideoEntities.stream()
                .filter(entity -> Objects.equals(entity.getTenantId(), tenantId))
                .filter(entity -> {
                    // 子账号只能删除自己的记录
                    if (userCacheVo.getUserType() == 2) {
                        return Objects.equals(entity.getUserId(), userId);
                    }
                    return true; // 主账号可以删除租户下所有记录
                })
                .toList();

        if (CollectionUtil.isEmpty(validEntities)) {
            throw new BusinessException("没有可删除的记录或无权限删除");
        }

        // 获取对应的视频ID列表
        List<Long> videoIds = validEntities.stream()
                .map(VideoUserVideoEntity::getVideoId)
                .toList();

        // 获取有效的记录ID列表
        List<Long> validIds = validEntities.stream()
                .map(VideoUserVideoEntity::getId)
                .toList();

        // 批量删除用户视频关联表记录（逻辑删除）
        return videoUserVideoService.removeByIds(validIds);
    }

    /**
     * 通过用户视频关联表ID查询文案内容
     *
     * @param id 用户视频关联表ID
     * @return 文案内容，如果没有找到返回null
     */
    public VideoContentExtractVo getContentById(Long id) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户id不存在");

        // 获取并验证视频数据（不验证状态，因为查询内容不需要特定状态）
        VideoDataPair videoData = validateAndGetVideoData(
                VideoExtractBo.builder().id(id).build(),
                tenantId,
                userCacheVo,
                null // 不验证状态
        );

        VideoInfoEntity videoInfoEntity = videoData.videoInfoEntity;

        // 检查是否有video_hash
        if (StrUtil.isBlank(videoInfoEntity.getVideoHash())) {
            log.warn("视频记录ID: {} 没有video_hash值", id);
            return null;
        }

        // 通过video_hash查询MongoDB中的文案内容
        Optional<VideoContentExtract> contentOpt = videoExtractContentRepository.findByVideoHash(videoInfoEntity.getVideoHash());

        if (contentOpt.isPresent()) {
            VideoContentExtract videoContentExtract = contentOpt.get();
            return VideoContentExtractVo.builder().videoHash(videoContentExtract.getVideoHash()).audioContent(videoContentExtract.getAudioContent()).build();
        } else {
            return null;
        }
    }

    /**
     * 查询操作人列表
     *
     * @return 如果是子账号直接返回List.of() 如果是主账号返回现有数据用户列表
     */
    public List<OperationUserVo> getOperationUsers() {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户id不存在");

        // 获取该租户id所有的用户信息 如果没有视频直接返回null
        // 使用 QueryWrapper 进行去重查询
        QueryWrapper<VideoUserVideoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT user_id") // 指定去重字段
                .eq("tenant_id", tenantId).in("source_type", 1, 2).eq("is_deleted", 0);
        List<VideoUserVideoEntity> videoEntityList = videoUserVideoService.list(queryWrapper);
        if (CollectionUtil.isEmpty(videoEntityList)) {
            return List.of();
        }
        List<Long> userIds = videoEntityList.stream().distinct().map(VideoUserVideoEntity::getUserId).toList();
        List<UserDto> userDtoList = userFeign.listByIds(userIds);
        if (CollectionUtil.isEmpty(userDtoList)) {
            return List.of();
        }
        List<OperationUserVo> operationUserVos = new ArrayList<>();
        userDtoList.forEach(userDto -> operationUserVos.add(OperationUserVo.builder().userId(userDto.getId()).userType(userDto.getUserType()).nickName(userDto.getNickName()).build()));
        return operationUserVos.stream().sorted(Comparator.comparingInt(OperationUserVo::getUserType).reversed()).toList();
    }

    /**
     * 处理待处理状态的视频提取请求
     */
    private VideoExtractVo handlePendingStatus(VideoExtractBo videoExtractBo, Long tenantId, UserCacheVo userCacheVo) {
        // 验证必要参数
        SystemKvEntity shortVideoDurationMax = systemKvService.getByKey("short_video_duration_max");
        int durationMax = shortVideoDurationMax != null ? Integer.parseInt(shortVideoDurationMax.getKvValue()) : 900;
        Assert.isTrue(StrUtil.isNotBlank(videoExtractBo.getVideoTitle()), "视频文件名称不能为空");
        Assert.isTrue(StrUtil.isNotBlank(videoExtractBo.getVideoUrl()), "视频文件URL不能为空");
        Assert.isTrue(StrUtil.isNotBlank(videoExtractBo.getCoverUrl()), "视频封面URL不能为空");
        Assert.isTrue(videoExtractBo.getDuration() != null && videoExtractBo.getDuration() > 0 && videoExtractBo.getDuration() <= durationMax, "仅支持提取" + durationMax / 60 + "分钟内视频提取文案");

        // 创建视频基础信息
        LocalDateTime now = LocalDateTime.now();
        VideoInfoEntity videoInfoEntity = VideoInfoEntity.builder()
                .id(SnowflakeManager.nextValue())
                .platformType(PlatformTypeEnum.LOCAL_UPLOAD.getCode())
                .authorId(userCacheVo.getId().toString())
                .authorName(userCacheVo.getNickName())
                .extractStatus(ExtractStatusEnum.NON.getCode())
                .title(videoExtractBo.getVideoTitle())
                .videoUrl(videoExtractBo.getVideoUrl())
                .duration(videoExtractBo.getDuration())
                .coverUrl(videoExtractBo.getCoverUrl())
                .createdDate(now).updateDate(now)
                .build();

        // 创建用户视频关联信息
        VideoUserVideoEntity videoUserVideoEntity = VideoUserVideoEntity.builder()
                .id(SnowflakeManager.nextValue())
                .videoId(videoInfoEntity.getId())
                .userId(userCacheVo.getId())
                .tenantId(tenantId)
                .sourceType(videoExtractBo.getSourceType())
                .videoTitle(videoExtractBo.getVideoTitle())
                .sourceId(userCacheVo.getId())
                .extractStatus(ExtractStatusEnum.PENDING.getCode())
                .createdDate(now).updateDate(now)
                .build();

        // 保存数据
        videoInfoService.save(videoInfoEntity);
        videoUserVideoService.save(videoUserVideoEntity);

        VideoExtractVo convert = BeanConvertUtils.convert(videoUserVideoEntity, VideoExtractVo.class);
        convert.setVideoUrl(videoExtractBo.getVideoUrl());
        convert.setAuthorId(videoInfoEntity.getAuthorId());
        convert.setAuthorName(videoInfoEntity.getAuthorName());
        return convert;
    }

    /**
     * 处理处理中状态的视频提取请求
     */
    private VideoExtractVo handleProcessingStatus(VideoExtractBo videoExtractBo, Long tenantId, UserCacheVo userCacheVo) {
        // 验证必要参数
        Assert.notNull(videoExtractBo.getId(), "提取文案ID不能为空");
        Assert.isTrue(StrUtil.isNotBlank(videoExtractBo.getVideoHash()), "视频文件hash不能为空");

        // 获取并验证视频数据
        VideoDataPair videoData = validateAndGetVideoData(videoExtractBo, tenantId, userCacheVo, Collections.singletonList(ExtractStatusEnum.PENDING.getCode()));
        VideoUserVideoEntity videoUserVideoEntity = videoData.videoUserVideoEntity();
        VideoInfoEntity videoInfoEntity = videoData.videoInfoEntity();

        // 更新状态为处理中
        videoUserVideoEntity.setExtractStatus(ExtractStatusEnum.PROCESSING.getCode());
        videoInfoEntity.setVideoHash(videoExtractBo.getVideoHash());

        // 检查是否已存在提取结果（基于hash值）
        Optional<VideoContentExtract> contentOpt = videoExtractContentRepository.findByVideoHash(videoExtractBo.getVideoHash());
        if (contentOpt.isPresent()) {
            // 直接使用已有的提取结果
            LocalDateTime now = LocalDateTime.now();
            VideoExtractVo result = updateCompletedStatus(videoUserVideoEntity, now, videoInfoEntity);
            result.setExtractContent(contentOpt.get().getAudioContent());
            result.setVideoUrl(videoInfoEntity.getVideoUrl());

            // 调用消耗资源接口
            boolean property = userPropertyFeign.useShortVideoProperty(userCacheVo.getId(), OrderEnums.commodityTypeCode.SHORT_VIDEO_NUM.getCode(), -1L);
            if (!property) {
                throw new BusinessException("套餐资源不足！");
            }
            return result;
        }

        // 保存更新后的状态
        videoInfoService.updateById(videoInfoEntity);
        videoUserVideoService.updateById(videoUserVideoEntity);

        VideoExtractVo convert = BeanConvertUtils.convert(videoUserVideoEntity, VideoExtractVo.class);
        convert.setVideoUrl(videoInfoEntity.getVideoUrl());
        convert.setAuthorId(videoInfoEntity.getAuthorId());
        convert.setAuthorName(videoInfoEntity.getAuthorName());
        return convert;
    }

    /**
     * 处理已完成状态的视频提取请求
     */
    private VideoExtractVo handleCompletedStatus(VideoExtractBo videoExtractBo, Long tenantId, UserCacheVo userCacheVo) {

        // 验证必要参数
        Assert.notNull(videoExtractBo.getId(), "提取文案ID不能为空");
        Assert.isTrue(StrUtil.isNotBlank(videoExtractBo.getExtractContent()), "提取文案数据不能为空！");

        // 获取并验证视频数据
        VideoDataPair videoData = validateAndGetVideoData(videoExtractBo, tenantId, userCacheVo, Collections.singletonList(ExtractStatusEnum.PROCESSING.getCode()));
        VideoUserVideoEntity videoUserVideoEntity = videoData.videoUserVideoEntity();
        VideoInfoEntity videoInfoEntity = videoData.videoInfoEntity();

        // 更新状态为已完成
        LocalDateTime now = LocalDateTime.now();
        VideoExtractVo result = updateCompletedStatus(videoUserVideoEntity, now, videoInfoEntity);
        result.setExtractContent(videoExtractBo.getExtractContent());
        result.setVideoUrl(videoInfoEntity.getVideoUrl());
        result.setAuthorId(videoInfoEntity.getAuthorId());
        result.setAuthorName(videoInfoEntity.getAuthorName());

        // 使用MongoDB Upsert操作保存或更新文案数据（原子性操作，避免重复数据）
        // 兼容老版本：如果originalExtractContent存在则保存，否则传null
        VideoContentExtract savedExtract = mongoUpsertService.upsertVideoContent(
                videoInfoEntity.getVideoHash(),
                videoExtractBo.getExtractContent(),
                videoExtractBo.getOriginalExtractContent()
        );

        Assert.notNull(savedExtract, "保存或更新视频提取文案失败！");

        // 调用消耗资源接口
        boolean property = userPropertyFeign.useShortVideoProperty(userCacheVo.getId(), OrderEnums.commodityTypeCode.SHORT_VIDEO_NUM.getCode(), -1L);
        if (!property) {
            throw new BusinessException("套餐资源不足！");
        }
        return result;
    }


    /**
     * 处理失败状态的视频提取请求
     */
    private VideoExtractVo handleFailedStatus(VideoExtractBo videoExtractBo, Long tenantId, UserCacheVo userCacheVo) {
        // 验证必要参数
        Assert.notNull(videoExtractBo.getId(), "提取文案ID不能为空");
        Assert.isTrue(StrUtil.isNotBlank(videoExtractBo.getExtractErrorReason()), "提取失败原因不能为空");

        // 获取并验证视频数据
        VideoDataPair videoData = validateAndGetVideoData(videoExtractBo, tenantId, userCacheVo, Arrays.asList(ExtractStatusEnum.PENDING.getCode(), ExtractStatusEnum.PROCESSING.getCode()));
        VideoUserVideoEntity videoUserVideoEntity = videoData.videoUserVideoEntity();
        VideoInfoEntity videoInfoEntity = videoData.videoInfoEntity();

        // 更新状态为失败
        LocalDateTime now = LocalDateTime.now();
        videoUserVideoEntity.setExtractStatus(ExtractStatusEnum.FAILED.getCode());
        videoUserVideoEntity.setExtractErrorReason(videoExtractBo.getExtractErrorReason());
        videoUserVideoEntity.setExtractTime(now);


        // 保存更新后的状态
        videoUserVideoService.updateById(videoUserVideoEntity);

        VideoExtractVo convert = BeanConvertUtils.convert(videoUserVideoEntity, VideoExtractVo.class);
        convert.setVideoUrl(videoInfoEntity.getVideoUrl());
        convert.setAuthorId(videoInfoEntity.getAuthorId());
        convert.setAuthorName(videoInfoEntity.getAuthorName());
        return convert;

    }

    /**
     * 更新数据状态
     *
     * @param videoUserVideoEntity
     * @param now
     * @param videoInfoEntity
     * @return
     */
    private VideoExtractVo updateCompletedStatus(VideoUserVideoEntity videoUserVideoEntity, LocalDateTime now, VideoInfoEntity videoInfoEntity) {
        videoUserVideoEntity.setExtractStatus(ExtractStatusEnum.COMPLETED.getCode());
        videoUserVideoEntity.setExtractTime(now);
        videoInfoEntity.setExtractStatus(ExtractStatusEnum.VIDEO_COMPLETED.getCode());
        videoInfoEntity.setExtractTime(now);

        // 保存更新后的状态
        videoInfoService.updateById(videoInfoEntity);
        videoUserVideoService.updateById(videoUserVideoEntity);

        // 构建返回结果
        VideoExtractVo result = BeanConvertUtils.convert(videoUserVideoEntity, VideoExtractVo.class);
        return result;
    }

    /**
     * 获取并验证视频数据的公共方法
     *
     * @param videoExtractBo     视频提取请求对象
     * @param tenantId           租户ID
     * @param userCacheVo        用户信息
     * @param expectedStatusList 期望的视频状态
     * @return 验证后的视频数据对象
     */
    private VideoDataPair validateAndGetVideoData(VideoExtractBo videoExtractBo, Long tenantId,
                                                  UserCacheVo userCacheVo, List<Byte> expectedStatusList) {
        // 获取视频用户关联数据
        VideoUserVideoEntity videoUserVideoEntity = videoUserVideoService.getById(videoExtractBo.getId());
        Assert.notNull(videoUserVideoEntity, "视频不存在");

        // 验证租户权限
        Assert.isTrue(Objects.equals(tenantId, videoUserVideoEntity.getTenantId()), "无当前视频操作权限");

        // 验证用户权限（子账号只能操作自己的视频）
        /*if (userCacheVo.getUserType() == 2) {
            Assert.isTrue(Objects.equals(videoUserVideoEntity.getUserId(), userCacheVo.getId()), "无当前视频操作权限");
        }*/

        // 验证视频状态（如果指定了期望状态）
        if (CollectionUtil.isNotEmpty(expectedStatusList)) {
            Assert.isTrue(expectedStatusList.contains(videoUserVideoEntity.getExtractStatus()), "视频状态不合法");
        }

        // 获取视频基础信息
        VideoInfoEntity videoInfoEntity = videoInfoService.getById(videoUserVideoEntity.getVideoId());
        Assert.notNull(videoInfoEntity, "短视频数据不存在");

        return new VideoDataPair(videoUserVideoEntity, videoInfoEntity);
    }

    /**
     * 查询待处理队列数据
     *
     * @return 当前用户所有待处理和处理中状态的视频记录
     */
    public List<VideoExtractVo> getPendingQueue() {
        // 获取当前用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();

        // 查询待处理和处理中状态的记录
        List<VideoUserVideoEntity> pendingEntities = videoUserVideoService.list(
                Wrappers.<VideoUserVideoEntity>lambdaQuery()
                        .eq(VideoUserVideoEntity::getUserId, userId)
                        .in(VideoUserVideoEntity::getExtractStatus,
                                ExtractStatusEnum.PENDING.getCode(),
                                ExtractStatusEnum.PROCESSING.getCode())
                        .orderByAsc(VideoUserVideoEntity::getCreatedDate) // 按创建时间升序
        );
        if (CollectionUtil.isEmpty(pendingEntities)) {
            return List.of();
        }
        List<Long> videoIds = pendingEntities.stream().map(VideoUserVideoEntity::getVideoId).distinct().toList();
        List<VideoInfoEntity> videoInfoEntityList = videoInfoService.listByIds(videoIds);
        if (CollectionUtil.isEmpty(videoInfoEntityList)) {
            return List.of();
        }
        Map<Long, VideoInfoEntity> videoInfoEntityMap = videoInfoEntityList.stream().collect(Collectors.toMap(VideoInfoEntity::getId, Function.identity(), (existing, replacement) -> existing));
        List<VideoExtractVo> videoExtractVos = new ArrayList<>();

        pendingEntities.forEach(item -> {
            VideoExtractVo videoExtractVo = BeanConvertUtils.convert(item, VideoExtractVo.class);
            videoExtractVo.setVideoUrl(videoInfoEntityMap.get(item.getVideoId()).getVideoUrl());
            videoExtractVos.add(videoExtractVo);
        });

        return videoExtractVos;
    }

    /**
     * 批量创建提取文案任务
     * 专门用于第一批上传，批量创建用户视频关联记录
     * 后续状态转换使用extractFromUrlAndLocal接口
     *
     * @param videoExtractRemoteVideoBo 批量创建请求参数
     * @return 批量创建结果
     */
    public List<VideoExtractVo> batchCreateExtract(VideoExtractRemoteVideoBo videoExtractRemoteVideoBo) {

        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userCacheVo.getId()).getData();
        // 验证租户信息
        Assert.isTrue(Objects.nonNull(tenantId) && !Objects.equals(tenantId, 0L), "租户id不存在!");

        // 获取有效提取文案视频列表
        List<VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> videoInfoVos = getVideoExtractRemoteVideoInfoVos(videoExtractRemoteVideoBo);
        if (CollectionUtil.isEmpty(videoInfoVos)) {
            log.warn("未找到有效的需要提取文案的视频列表！");
            return List.of();
        }

        // 构建锁的key集合，基于platformType和platformVideoId
        Set<String> lockKeys = videoInfoVos.stream()
                .map(entity -> LockKeyPrefix.VIDEO_INFO.getLockKey("batchCreateExtract:" + tenantId + ":" + entity.getVideoId().toString()))
                .collect(Collectors.toSet());
        // 创建多个锁
        RLock[] locks = lockKeys.stream()
                .map(redissonClient::getLock)
                .toArray(RLock[]::new);

        // 使用 MultiLock 确保所有锁都获取成功
        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean lockAcquired = false;

        try {
            // 尝试获取所有锁，最多等待10秒，锁定30秒后自动释放
            lockAcquired = multiLock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("获取分布式多重锁失败，lockKeys: {}, 数据量: {}", lockKeys, videoInfoVos.size());
                throw new BusinessException("批量提取文案正在处理中，请稍后重试！");
            }

            // 调用事务服务执行业务逻辑
            return videoExtractTransactionService.executeTransactionalBatchCreate(videoExtractRemoteVideoBo, userCacheVo, tenantId, videoInfoVos);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式多重锁被中断，lockKeys: {}, 数据量: {}", lockKeys, videoInfoVos.size(), e);
            throw new BusinessException("操作被中断，请重试");
        } finally {
            // 只有成功获取锁的情况下才释放
            if (lockAcquired) {
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    log.error("释放分布式多重锁异常，lockKeys: {}", lockKeys, e);
                }
            }
        }
    }


    @NotNull
    private List<VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> getVideoExtractRemoteVideoInfoVos(VideoExtractRemoteVideoBo videoExtractRemoteVideoBo) {
        List<VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> videoInfoVos = videoExtractRemoteVideoBo.getVideoInfoVos();

        if (CollectionUtil.isEmpty(videoInfoVos)) {
            return new ArrayList<>();
        }

        // 入参去重：避免处理重复的(platformType, platformVideoId)组合
        List<VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> distinctVideoInfoVos = new ArrayList<>(videoInfoVos.stream()
                .collect(Collectors.toMap(
                        vo -> vo.getPlatformType() + "_" + vo.getPlatformVideoId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ))
                .values());

        // 批量查询：构建查询条件，避免N+1查询问题
        List<VideoInfoEntity> videoInfoEntities = batchQueryVideoInfoEntities(distinctVideoInfoVos);

        // 构建映射关系：platformType_platformVideoId -> VideoInfoEntity
        Map<String, VideoInfoEntity> videoInfoMap = videoInfoEntities.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getPlatformType() + "_" + entity.getPlatformVideoId(),
                        Function.identity(),
                        (existing, replacement) -> existing // 如果有重复key，保留第一个
                ));

        // 填充数据并过滤
        SystemKvEntity shortVideoDurationMax = systemKvService.getByKey("short_video_duration_max");
        int durationMax = shortVideoDurationMax != null ? Integer.parseInt(shortVideoDurationMax.getKvValue()) : 900;
        return distinctVideoInfoVos.stream()
                .peek(item -> {
                    String key = item.getPlatformType() + "_" + item.getPlatformVideoId();
                    VideoInfoEntity videoInfoEntity = videoInfoMap.get(key);

                    if (Objects.nonNull(videoInfoEntity)) {
                        item.setVideoId(videoInfoEntity.getId());
                        item.setTitle(videoInfoEntity.getTitle());
                        item.setVideoUrl(videoInfoEntity.getVideoUrl());
                        item.setExtractStatus(videoInfoEntity.getExtractStatus());
                        item.setDuration(videoInfoEntity.getDuration());
                        item.setAuthorId(videoInfoEntity.getAuthorId());
                        item.setAuthorName(videoInfoEntity.getAuthorName());
                    } else {
                        item.setVideoId(null);
                        item.setTitle(null);
                        item.setVideoUrl(null);
                        item.setExtractStatus(null);
                        item.setDuration(null);
                    }
                })
                .filter(s -> Objects.nonNull(s.getVideoId()) &&
                        Objects.nonNull(s.getDuration()) &&
                        s.getDuration() <= durationMax)
                .collect(Collectors.toList());
    }

    /**
     * 批量查询视频信息实体
     * 避免N+1查询问题，提升性能
     * 使用OR条件组合：(platformType=1 AND platformVideoId=123) OR (platformType=2 AND platformVideoId=234)
     *
     * @param videoInfoVos 视频信息VO列表
     * @return 视频信息实体列表
     */
    private List<VideoInfoEntity> batchQueryVideoInfoEntities(List<VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo> videoInfoVos) {
        if (CollectionUtil.isEmpty(videoInfoVos)) {
            return new ArrayList<>();
        }

        Map<Byte, Set<String>> platformVideoMapping = videoInfoVos.stream().collect(Collectors.groupingBy(VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo::getPlatformType, Collectors.mapping(VideoExtractRemoteVideoBo.VideoExtractRemoteVideoInfoVo::getPlatformVideoId, Collectors.toSet())));


        // 使用XML查询方式执行批量查询
        List<VideoInfoEntity> existingEntities = videoInfoDao.batchQueryByPlatformAndVideoIds(platformVideoMapping.values().stream().flatMap(Collection::stream).distinct().toList(), platformVideoMapping.keySet());
        if (EmptyUtil.isEmpty(existingEntities)) {
            return new ArrayList<>();
        }
        return existingEntities.stream().filter(item -> {
            Set<String> videoIds = platformVideoMapping.get(item.getPlatformType());
            if (EmptyUtil.isEmpty(videoIds)) {
                return false;
            }
            return videoIds.contains(item.getPlatformVideoId());
        }).toList();
    }


    /**
     * 重新提取文案
     *
     * @param userVideoId 用户视频ID
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reuseExtract(Long userVideoId) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();
        // 查询用户视频ID
        VideoUserVideoEntity videoUserVideoEntity = videoUserVideoService.getById(userVideoId);
        Assert.notNull(videoUserVideoEntity, "提取视频不存在！");
        // 验证租户权限
        Assert.isTrue(Objects.equals(tenantId, videoUserVideoEntity.getTenantId()), "无当前视频操作权限");
        // 验证视频状态
        Assert.isTrue(Objects.equals(videoUserVideoEntity.getExtractStatus(), ExtractStatusEnum.FAILED.getCode()), "视频状态不合法");
        // 修改状态为待处理
        videoUserVideoEntity.setExtractStatus(ExtractStatusEnum.PENDING.getCode());
        videoUserVideoEntity.setUpdateDate(LocalDateTime.now());
        return videoUserVideoService.updateById(videoUserVideoEntity);
    }

    /**
     * 获取视频详情
     *
     * @param videoId
     * @return
     */
    public VideoInfoEntity getVideoInfoByVideoId(Long videoId) {
        return videoInfoService.lambdaQuery()
                .eq(VideoInfoEntity::getId, videoId)
                .last("limit 1")
                .select(VideoInfoEntity::getPlatformType, VideoInfoEntity::getPlatformVideoId,
                        VideoInfoEntity::getVideoHash, VideoInfoEntity::getTitle,
                        VideoInfoEntity::getAuthorName, VideoInfoEntity::getPublishTime,
                        VideoInfoEntity::getCreatedDate)
                .one();
    }

    /**
     * 重新统计用户的资产
     *
     * @return 是否成功
     */
    public Boolean syncUserShortVideoProperty(Long userId, Long tenantId) {

        long currentInfluencerCount = videoUserInfluencerSubscriptionService.count(new LambdaQueryWrapper<VideoUserInfluencerSubscriptionEntity>()
                .eq(VideoUserInfluencerSubscriptionEntity::getUserId, userId)
                .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
        );

        long influencerCount = videoUserInfluencerSubscriptionService.count(new LambdaQueryWrapper<VideoUserInfluencerSubscriptionEntity>()
                .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
        );


        long currentHotCount = videoUserHotSubscriptionService.count(new LambdaQueryWrapper<VideoUserHotSubscriptionEntity>()
                .eq(VideoUserHotSubscriptionEntity::getUserId, userId)
                .eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId)
        );


        long hotCount = videoUserHotSubscriptionService.count(new LambdaQueryWrapper<VideoUserHotSubscriptionEntity>()
                .eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId)
        );
        try {
            // 账号订阅达人数量
            userPropertyFeign.updateByPropertyNumRetBoolean(userId, "userSubscribeInfluencerNum", currentInfluencerCount);
            // 共享订阅达人数量
            userPropertyFeign.updateByPropertyNumRetBoolean(userId, "subscribeInfluencerNum", influencerCount);
            // 账号订阅爆款数量
            userPropertyFeign.updateByPropertyNumRetBoolean(userId, "userSubscribeHotVideoNum", currentHotCount);
            // 共享订阅爆款数量
            userPropertyFeign.updateByPropertyNumRetBoolean(userId, "subscribeHotVideoNum", hotCount);
        } catch (Exception e) {
            log.error("同步订阅达人和爆款资产报错", e);
            return false;
        }
        return true;
    }

    public Boolean unbindDelInfluencerAndHot(Long userId, Long tenantId) {

        // 删除对应的达人
        videoUserInfluencerSubscriptionService.remove(new LambdaQueryWrapper<VideoUserInfluencerSubscriptionEntity>()
                .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
                .eq(VideoUserInfluencerSubscriptionEntity::getUserId, userId)
        );

        // 删除对应的爆款
        videoUserHotSubscriptionService.remove(new LambdaQueryWrapper<VideoUserHotSubscriptionEntity>()
                .eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId)
                .eq(VideoUserHotSubscriptionEntity::getUserId, userId)
        );
        return syncUserShortVideoProperty(userId, tenantId);
    }

    /**
     * 获取提取文案后优化文案的提示词
     *
     * @return 提示词
     */
    public ExtractPromptVo getExtractPrompt() {
        ExtractPromptVo result = new ExtractPromptVo();

        // 获取ai模型
        int aiModel = 0;
        SystemKvEntity kvInfoVo = systemKvService.getByKey("optimize_extract_copy_ai_code");
        if (ObjectUtil.isNotEmpty(kvInfoVo)) {
            DictDataListVo dictDataListVo = dictDataFeign.dictDataByLabel("client_ai_model", kvInfoVo.getKvValue());
            if (ObjectUtil.isNotEmpty(dictDataListVo)) {
                aiModel = NumberUtil.parseInt(dictDataListVo.getValue(), aiModel);
            }
        }
        String cueWord = "根据以下内容所在行业的语言习惯，以及前后的语义，帮我纠正错别字和标点符号，并给我改正后的原文，改正过的地方标粗体，不要给我纠正了什么，不要额外输出其他内容，我只要改正后的原文。";
        SystemKvEntity cueKv = systemKvService.getByKey("optimize_extract_copy_prompt");
        if (ObjectUtil.isNotEmpty(cueKv) && cueKv.getKvValue() != null && ObjectUtil.isNotEmpty(cueKv.getKvValue().trim())) {
            cueWord = cueKv.getKvValue();
        }
        result.setCueWord(cueWord);
        result.setAiIdentity("你是一个文案整理专家");
        result.setAiModel(aiModel);
        return result;
    }

    /**
     * 视频数据对象包装类
     */
    private record VideoDataPair(VideoUserVideoEntity videoUserVideoEntity, VideoInfoEntity videoInfoEntity) {

    }
}
