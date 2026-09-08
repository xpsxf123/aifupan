package com.jiuyu.replay.words.repository.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.alibaba.OssUtils;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bll.DataScreenshotBll;
import com.jiuyu.replay.words.bo.VideoNotesQueryBo;
import com.jiuyu.replay.words.bo.video.VideoBelongUserBO;
import com.jiuyu.replay.words.entity.VideoDataViewingParagraphEntity;
import com.jiuyu.replay.words.entity.VideoTextNotes;
import com.jiuyu.replay.words.enums.NotesType;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.handler.VideoCombinationHandler;
import com.jiuyu.replay.words.producer.VideoDataViewingConfuseProducer;
import com.jiuyu.replay.words.repository.mongo.VideoTextNotesRepository;
import com.jiuyu.replay.words.repository.service.VideoDataViewingParagraphService;
import com.jiuyu.replay.words.repository.service.VideoTextNotesService;
import com.jiuyu.replay.words.vo.DataScreenshotListVo;
import com.jiuyu.replay.words.vo.NotesContentVo;
import com.jiuyu.replay.words.vo.VideoNotesInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.mongodb.bulk.BulkWriteResult;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 视频复盘全文笔记服务 实现
 * 原文笔记，复盘小结，分段笔记
 *
 * @author HeHui
 * @date 2025-06-07 14:45
 */
@Service
@Slf4j
public class VideoTextNotesServiceImpl implements VideoTextNotesService {

    private final VideoTextNotesRepository videoTextNotesRepository;

    private final VideoCombinationHandler videoCombinationHandler;

    private final OssUtils ossUtils;

    private final IdentifierGenerator identifierGenerator;

    private final RedisTemplate<String, String> redisTemplate;

    private final DataScreenshotBll dataScreenshotBll;

    private final VideoDataViewingConfuseProducer viewingConfuseProducer;

    private final RedissonClient redissonClient;

    private final VideoDataViewingParagraphService videoDataViewingParagraphService;

    private final MongoTemplate mongoTemplate;

    /**
     * 视频笔记oss前缀
     * 变量1: 笔记类型
     * 变量2: 笔记来源类型
     * 变量3: 视频id
     */
    private final String OSS_PREFIX = "video/text_notes/%s/%s-%s";
    /**
     * 笔记文件后缀
     * 变量1: 版本号
     */
    private final String OSS_SUFFIX = "-%s.html";

    /**
     * 缓存key
     * 变量1: 笔记类型
     * 变量2: 视频类型
     * 变量3: 视频id
     */
    private final String CACHE_KEY = "aifupan:video-notes-%s:%s_for_type:%s";

    /**
     * 压缩内容前缀
     */
    private static final String COMPRESSED_PREFIX = "GZIP:";

    /**
     * 最小压缩大小（字节）- 2kb
     */
    private static final int MIN_COMPRESS_SIZE = 2048;

    /**
     * 最大压缩等级
     */
    private static final int MAX_COMPRESSION_LEVEL = 9;


    public VideoTextNotesServiceImpl(VideoTextNotesRepository videoTextNotesRepository, VideoCombinationHandler videoCombinationHandler, OssUtils ossUtils, ObjectProvider<IdentifierGenerator> identifierGeneratorProvider, RedisTemplate<String, String> redisTemplate, DataScreenshotBll dataScreenshotBll, VideoDataViewingConfuseProducer viewingConfuseProducer, RedissonClient redissonClient, VideoDataViewingParagraphService videoDataViewingParagraphService, MongoTemplate mongoTemplate) {
        this.videoTextNotesRepository = videoTextNotesRepository;
        this.videoCombinationHandler = videoCombinationHandler;
        this.ossUtils = ossUtils;
        this.identifierGenerator = identifierGeneratorProvider.stream().findFirst().orElseGet(DefaultIdentifierGenerator::new);
        this.redisTemplate = redisTemplate;
        this.dataScreenshotBll = dataScreenshotBll;
        this.viewingConfuseProducer = viewingConfuseProducer;
        this.redissonClient = redissonClient;
        this.videoDataViewingParagraphService = videoDataViewingParagraphService;
        this.mongoTemplate = mongoTemplate;
    }


    /**
     * 初始化视频笔记数据中的租户ID和用户ID字段。
     * <p>
     * 该方法用于处理历史数据中 tenantId 和 userId 为空的视频笔记记录，
     * 根据其关联的视频来源类型和视频ID查询对应的租户和用户信息，并批量更新到数据库中。
     * </p>
     *
     * <p>执行流程如下：</p>
     * <ol>
     *   <li>通过分批查询找出所有 tenantId 和 userId 为空的视频笔记；</li>
     *   <li>根据视频来源类型(sourceType)和视频ID(sourceId)分组查询对应的租户和用户信息；</li>
     *   <li>将查询到的租户和用户信息批量更新到对应的视频笔记记录中。</li>
     * </ol>
     *
     * <p>注意：此操作为一次性初始化操作，不建议频繁调用。</p>
     */
    @Override
    public void initTenantNotes() {
        log.info("[视频笔记] init tenant and user start");

        // 分批查询未设置租户和用户的视频笔记
        BatchQuery<Long, VideoTextNotes> batchQuery = new BatchQuery<>((limit, idx) -> {
            Criteria criteria = Criteria.where("id").gte(idx)
                .and("tenantId").isNull()
                .and("userId").isNull();

            Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.asc("id")))
                .limit(limit);
            query.fields()
                .include("id")
                .include("sourceType")
                .include("sourceId");
            log.info("[视频笔记] init tenant and user. query idx gt {} limit {}", idx, limit);
            return mongoTemplate.find(query, VideoTextNotes.class);
        }, VideoTextNotes::getId);

        // 构建 sourceType code 到枚举的映射
        Map<Integer, VideoSourceType> sourceTypeMap = Arrays.stream(VideoSourceType.values())
            .collect(Collectors.toMap(VideoSourceType::getCode, Function.identity()));

        // 批量处理每一批视频笔记数据
        batchQuery.consumer((batch, textNotes) -> {
            log.info("[视频笔记] init tenant and user. batch {} rows size: {}", batch, textNotes.size());
            // 根据视频类型分组
            Map<VideoSourceType, List<String>> sourceMap = textNotes.stream()
                .collect(Collectors.groupingBy(
                    notes -> sourceTypeMap.get(notes.getSourceType()),
                    Collectors.mapping(VideoTextNotes::getSourceId, Collectors.toList())
                ));

            // 查询视频所属的租户和用户信息
            Map<VideoSourceType, Map<String, VideoBelongUserBO>> videoBelongUserMap = videoCombinationHandler.findVideoBelongUserMap(sourceMap);
            if (EmptyUtil.isEmpty(videoBelongUserMap)) {
                log.info("[视频笔记] init tenant and user. no video belong user, batch {} sourceMap: {}", batch, JsonTemplate.toJson(sourceMap));
                return;
            }

            // 按视频类型和视频ID对笔记ID进行分组
            Map<VideoSourceType, Map<String, List<Long>>> videoSourceNotesMap = textNotes.stream()
                .collect(Collectors.groupingBy(
                    notes -> sourceTypeMap.get(notes.getSourceType()),
                    Collectors.groupingBy(
                        VideoTextNotes::getSourceId,
                        Collectors.mapping(VideoTextNotes::getId, Collectors.toList())
                    )
                ));

            // 构建批量更新操作
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, VideoTextNotes.class);
            videoBelongUserMap.forEach((type, videoMap) -> {
                Map<String, List<Long>> notesMap = videoSourceNotesMap.get(type);
                videoMap.forEach((videoId, user) -> {
                    List<Long> notesIdList = notesMap.get(videoId);
                    Update update = new Update()
                        .set("tenantId", user.tenantId())
                        .set("userId", user.userId());
                    Query query = Query.query(Criteria.where("id").in(notesIdList));
                    bulkOps.updateMulti(query, update);
                    log.info("[视频笔记] init tenant and user. update notes. batch {} type: {}, videoId: {} set to tenantId: {} userId: {}, notesIdList: {}",
                        batch, type.getDesc(), videoId, user.tenantId(), user.userId(), notesIdList);
                });
            });

            // 执行批量更新并记录结果
            BulkWriteResult result = bulkOps.execute();
            log.info("[视频笔记] init tenant and user. batch {} result insert: {} update: {} delete: {}",
                batch, result.getInsertedCount(), result.getModifiedCount(), result.getDeletedCount());
        });

        // 启动批量处理流程
        long totalCount = batchQuery.run(0L);
        log.info("[视频笔记] init tenant and user. finish total rows {}", totalCount);
    }


    /**
     * 获取缓存key
     *
     * @param sourceId   源id
     * @param sourceType 源类型
     * @param notesType  笔记类型
     *
     * @return {@link String }
     */
    private String getCacheKey(String sourceId, VideoSourceType sourceType, NotesType notesType) {
        return String.format(CACHE_KEY, notesType.getCode(), sourceType.name(), sourceId) + ":last";
    }


    /**
     * 获取历史缓存key
     *
     * @param sourceId   源id
     * @param sourceType 源类型
     * @param notesType  笔记类型
     * @param version    版本
     *
     * @return {@link String }
     */
    private String getHistoryCacheKey(String sourceId, VideoSourceType sourceType, NotesType notesType, int version) {
        return String.format(CACHE_KEY, notesType.getCode(), sourceType.name(), sourceId) + ":" + version;
    }

    /**
     * 保存笔记信息
     *
     * <p>该方法用于将用户输入的笔记内容保存到系统中，若已有相同内容则不进行更新；
     * 若内容有变化，则升级笔记版本并存储新内容至OSS。</p>
     *
     * @param sourceId   笔记来源的ID，表示笔记关联的视频ID
     * @param sourceType 笔记来源的类型，用于区分笔记所属的视频类型
     * @param notesType  笔记的类型，支持以下取值：
     *                   <ul>
     *                     <li>1: 原文笔记</li>
     *                     <li>2: 复盘小结</li>
     *                     <li>3: 分段笔记</li>
     *                   </ul>
     * @param content    笔记的内容，即用户输入的文本信息
     * @param user       当前执行操作的用户缓存对象，包含用户权限和身份信息
     *
     * @return 返回一个封装了新生成笔记ID的响应对象 {@link R<Long>}，
     *     如果保存成功返回对应的笔记ID，失败则抛出异常或返回错误信息
     */
    @CustomRedissonLock(key = "'video_notes_save:' + #args[0] + ':' + #args[1] + ':' + #args[2]")
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public R<Long> saveNotes(String sourceId, VideoSourceType sourceType, NotesType notesType, String content, UserCacheVo user) {
        if (HtmlUtil.cleanHtmlTag(content).length() > 100000) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "笔记内容过长,字数限制在10万内");
        }
        // 校验用户是否具有编辑视频笔记的权限
        R<VideoBelongUserBO> permissionResult = videoCombinationHandler.checkVideoEditPermission(sourceId, sourceType, user);
        if (permissionResult.fail()) {
            // 权限校验失败，直接返回错误响应
            return R.error(permissionResult.getCode(), permissionResult.getMsg());
        }
        // 是否空内容
        boolean emptyContent = StrUtil.isBlank(content);
        // 尝试查找已存在的笔记记录
        Long notesId = videoTextNotesRepository.findVideoTextNotes(sourceId, sourceType.getCode(), notesType.getCode())
            .map(videoTextNotes -> {
                // 计算当前内容的MD5哈希值
                String hash = emptyContent ? "" : DigestUtil.md5Hex(content);

                // 比较新旧内容哈希值，判断内容是否有变更
                if (Objects.equals(hash, videoTextNotes.getContentHash())) {
                    log.info("[全文笔记] save video text notes, content noChanges videoId: {}, videoType: {}, notesType: {}， user:{}", sourceId, sourceType, notesType, user.getId());
                    return videoTextNotes.getId(); // 内容未变化，直接返回原有笔记ID
                }

                // 内容发生变化，升级笔记版本
                videoTextNotes.upgrade(hash, user);
                // 更新数据库中的笔记信息（包括版本号、更新人、更新时间等）
                videoTextNotesRepository.save(videoTextNotes);
                if (!emptyContent) {
                    // 将更新后的内容上传至OSS对象存储
                    String key = ossUtils.putObjectByString(AiOssUtils.bucketNameAi, videoTextNotes.contentFilePath().get(), content);
                    if (StrUtil.isBlank(key)) {
                        // 上传失败时抛出异常，事务回滚
                        throw new RRException("笔记保存失败,请联系客服或管理");
                    }
                }
                redisTemplate.delete(this.getCacheKey(sourceId, sourceType, notesType));
                // 返回升级后的笔记ID
                return videoTextNotes.getId();
            }).orElseGet(() -> {
                VideoBelongUserBO videoBelongUserBO = permissionResult.getData();
                // 如果不存在现有笔记，则创建新的笔记对象
                VideoTextNotes notes = new VideoTextNotes();
                notes.setId(identifierGenerator.nextId(null).longValue()); // 使用ID生成器生成唯一ID
                notes.setTenantId(videoBelongUserBO.tenantId());
                notes.setUserId(videoBelongUserBO.userId());
                notes.setSourceId(sourceId); // 设置视频源ID
                notes.setSourceType(sourceType.getCode()); // 设置视频源类型
                notes.setNotesType(notesType.getCode()); // 设置笔记类型
                notes.setCreateTime(LocalDateTime.now()); // 设置创建时间
                notes.setUpdateTime(notes.getUpdateTime()); // 设置更新时间为创建时间
                notes.setIsDeleted(false); // 初始化删除标识为false（未删除）
                notes.setCreateUserId(user.getId()); // 设置创建用户ID
                notes.setUpdateUserId(user.getId()); // 设置更新用户ID
                notes.setContentHash(emptyContent ? "" : DigestUtil.md5Hex(content)); // 计算并设置内容哈希
                notes.setLastVersion(1); // 初始版本号设为1
                notes.setContentFilePath(String.format(OSS_PREFIX, notesType, sourceType.getCode(), notes.getId()) + OSS_SUFFIX); // 构建内容文件路径模板
                notes.setEditors(List.of(VideoTextNotes.Editor.first(user.getId(), user.getNickName())));
                // 插入新笔记到MongoDB中
                videoTextNotesRepository.insert(notes);
                if (!emptyContent) {
                    // 将笔记内容上传至OSS
                    String key = ossUtils.putObjectByString(AiOssUtils.bucketNameAi, notes.contentFilePath().get(), content);
                    if (StrUtil.isBlank(key)) {
                        // 上传失败时抛出异常，事务回滚
                        throw new RRException("笔记保存失败,请联系客服或管理");
                    }
                }
                log.info("[全文笔记] save video text notes, content upload success videoId: {}, videoType: {}, notesType: {}， ossKey:{}", sourceId, sourceType, notesType, notes.contentFilePath());
                // 返回新创建的笔记ID
                redisTemplate.delete(this.getCacheKey(sourceId, sourceType, notesType));
                return notes.getId();
            });
        return R.ok(notesId); // 返回成功响应及笔记ID
    }


    /**
     * 获取源数据小结（数据截图/数据看板）
     *
     * @param sourceId   源id
     * @param sourceType 源类型
     *
     * @return {@link Optional }<{@link String }>
     */
    @Override
    public Optional<String> getSourceDataSummary(String sourceId, VideoSourceType sourceType) {
        log.info("[全文笔记] init video text notes summary query data screenshot, videoId: {}, videoType: {}", sourceId, sourceType);
        R<List<DataScreenshotListVo>> dataScreenshotResult = dataScreenshotBll.getExistDataScreenshotList(sourceType.getCode(), sourceId);
        String content = "";
        if (CollUtil.isNotEmpty(dataScreenshotResult.getData())) {
            log.info("[全文笔记] init video text notes summary query data screenshot exist, videoId: {}, videoType: {}, dataScreenshotSize: {}", sourceId, sourceType, dataScreenshotResult.getData().size());
            Parser parser = Parser.builder().build();
            HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
            content = dataScreenshotResult.getData().stream().map(DataScreenshotListVo::getAiContent).filter(StrUtil::isNotBlank)
                .map(str -> {
                    try {
                        return htmlRenderer.render(parser.parse(str.replaceAll(">", "&gt;")));
                    } catch (Exception e) {
                        log.error("[全文笔记] init video text notes summary query data screenshot parse error, videoId: {}, videoType: {}, dataScreenshot: {}", sourceId, sourceType, str, e);
                        return str;
                    }
                }).collect(Collectors.joining("<br/>"));
        } else {
            log.info("[全文笔记] init video text notes summary query viewing confuse videoId: {}, videoType: {}", sourceId, sourceType);
            VideoDataViewingConfuseInfoVo viewingConfuse = viewingConfuseProducer.getByVideoId(sourceId);
            if (viewingConfuse != null) {
                if (Objects.equals(viewingConfuse.getDataSourceType(), 1)) {
                    videoDataViewingParagraphService.lambdaQuery().eq(VideoDataViewingParagraphEntity::getDataViewingConfuseId, viewingConfuse.getId())
                        .last("limit 1").oneOpt().filter(paragraph -> !paragraph.empty())
                        .ifPresent(paragraph -> {
                            viewingConfuse.setTotalWatchNum(paragraph.getTotalWatchNum());
                            viewingConfuse.setAverageOnlineNum(paragraph.getAverageOnlineNum());
                            viewingConfuse.setAverageResidenceTime(paragraph.getAverageResidenceTime());
                            viewingConfuse.setIncrementFollowerCount(paragraph.getIncrementFollowerCount());
                            viewingConfuse.setConvertFanRate(paragraph.getConvertFanRate());
                            viewingConfuse.setInteractionPercent(paragraph.getInteractionPercent());
                            viewingConfuse.setVolumeStart(paragraph.getVolumeStart());
                            viewingConfuse.setVolumeEnd(paragraph.getVolumeEnd());
                            viewingConfuse.setPurchaseCountStart(paragraph.getPurchaseCountStart());
                            viewingConfuse.setPurchaseCountEnd(paragraph.getPurchaseCountEnd());
                            viewingConfuse.setCustomerUnitPriceStart(paragraph.getCustomerUnitPriceStart());
                            viewingConfuse.setCustomerUnitPriceEnd(paragraph.getCustomerUnitPriceEnd());
                            viewingConfuse.setUvValueStart(paragraph.getUvValueStart());
                            viewingConfuse.setUvValueEnd(paragraph.getUvValueEnd());
                            viewingConfuse.setGoodsConvertRateStart(paragraph.getGoodsConvertRateStart());
                            viewingConfuse.setGoodsConvertRateEnd(paragraph.getGoodsConvertRateEnd());
                            viewingConfuse.setDataStatus(paragraph.getDataStatus());
                            viewingConfuse.setIsTakeProduct(paragraph.getIsTakeProduct());
                            viewingConfuse.setBatchNumber(paragraph.getBatchNumber());
                            viewingConfuse.setAnchorNumber(paragraph.getAnchorNumber());
                            viewingConfuse.setWatchFlowList(paragraph.getWatchFlowList());
                            viewingConfuse.setPayFlowList(paragraph.getPayFlowList());
                            viewingConfuse.setPayUserPortrait(paragraph.getPayUserPortrait());
                            viewingConfuse.setWatchUserPortrait(paragraph.getWatchUserPortrait());
                            viewingConfuse.setSecUid(paragraph.getSecUid());
                            viewingConfuse.setGpmStart(paragraph.getGpmStart());
                            viewingConfuse.setGpmEnd(paragraph.getGpmEnd());
                        });
                }
                if (viewingConfuse.empty()) {
                    return Optional.of("<div class='video-data-screenshot'></div>");
                }
                content = viewingConfuse.formatStr("<br/>");
                log.info("[全文笔记] init video text notes summary query viewing confuse success videoId: {}, videoType: {}, content: {}", sourceId, sourceType, content);
            }
        }
        if (StrUtil.isBlank(content)) {
            log.info("[全文笔记] init video text notes summary query data screenshot and viewing confuse fail, videoId: {}, videoType: {}", sourceId, sourceType);
            return Optional.empty();
        }
        return Optional.of("<div class='video-data-screenshot'>" + content + "</div>");
    }


    /**
     * 查询视频笔记信息
     *
     * @param queryBo 查询条件
     *
     * @return {@link PageData }<{@link VideoNotesInfoVo }>
     */
    @Override
    public PageData<VideoNotesInfoVo> queryPage(VideoNotesQueryBo queryBo) {
        // 1. 构建查询条件
        List<Criteria> criteriaList = new ArrayList<>();

        // 租户ID列表
        if (EmptyUtil.isNotEmpty(queryBo.getTenantIds())) {
            criteriaList.add(Criteria.where("tenantId").in(queryBo.getTenantIds()));
        }

        // 用户ID列表
        if (EmptyUtil.isNotEmpty(queryBo.getUserIds())) {
            criteriaList.add(Criteria.where("userId").in(queryBo.getUserIds()));
        }

        // 创建时间范围
        if (queryBo.getStartTime() != null) {
            criteriaList.add(Criteria.where("createTime").gte(queryBo.getStartTime()));
        }
        if (queryBo.getEndTime() != null) {
            criteriaList.add(Criteria.where("createTime").lte(queryBo.getEndTime()));
        }

        // sourceType 列表
        if (EmptyUtil.isNotEmpty(queryBo.getSourceTypes())) {
            criteriaList.add(Criteria.where("sourceType").in(queryBo.getSourceTypes()));
        }

        // notesType 精确匹配
        if (queryBo.getNotesType() != null) {
            criteriaList.add(Criteria.where("notesType").is(queryBo.getNotesType()));
        }
        // 组合所有条件
        Criteria criteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 2. 构建分页信息
        int currentPage = queryBo.getPage() == null || queryBo.getPage() < 1 ? 0 : queryBo.getPage() - 1;
        int pageSize = queryBo.getLimit() == null || queryBo.getLimit() < 1 ? 10 : queryBo.getLimit();

        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Order.desc("id"))); // 按id倒序

        // 3. 查询总数（用于分页）
        Query countQuery = new Query(criteria);
        long total = mongoTemplate.count(countQuery, VideoTextNotes.class);

        if (total == 0) {
            return PageData.empty();
        }

        // 4. 查询分页数据
        Query query = new Query(criteria)
            .with(pageable); // 包含排序和分页


        List<VideoTextNotes> list = mongoTemplate.find(query, VideoTextNotes.class);
        PageData<VideoTextNotes> pageData = new PageData<>(pageable.getPageNumber(), pageable.getPageSize());
        pageData.setList(list);
        pageData.setTotalCount(total);

        // 5. 返回分页结果
        return pageData.conversion(notes -> {
            VideoNotesInfoVo info = new VideoNotesInfoVo();
            info.setId(notes.getId());
            info.setTenantId(notes.getTenantId());
            info.setUserId(notes.getUserId());
            info.setSourceId(notes.getSourceId());
            info.setSourceType(notes.getSourceType());
            info.setNotesType(notes.getNotesType());
            info.setLastVersion(notes.getLastVersion());
            info.setCreateTime(notes.getCreateTime());
            info.setUpdateTime(notes.getUpdateTime());
            info.setEditors(null);
            if (EmptyUtil.isNotEmpty(notes.getEditors())) {
                info.setEditors(notes.getEditors().stream().map(editor -> {
                    NotesContentVo.Editor editorVo = new NotesContentVo.Editor();
                    editorVo.setUserId(editor.getUserId());
                    editorVo.setUserName(editor.getUserName());
                    editorVo.setEditTime(editor.getEditTime());
                    editorVo.setVersion(editor.getVersion());
                    return editorVo;
                }).toList());
            }
            return info;
        });
    }

    /**
     * 初始化视频笔记摘要
     *
     * @param sourceId        视频源ID
     * @param sourceType      视频源类型
     * @param videoBelongUser 视频归属用户
     *
     * @return 返回一个视频笔记摘要对象，如果初始化失败则返回null
     */
    private VideoTextNotes initSummary(String sourceId, VideoSourceType sourceType, VideoBelongUserBO videoBelongUser) {
        RLock lock = redissonClient.getLock("video_notes_save:" + sourceId);
        try {
            if (!lock.tryLock()) {
                return null;
            }
            return this.getSourceDataSummary(sourceId, sourceType).map(content -> {
                VideoTextNotes summary = new VideoTextNotes();
                summary.setId(identifierGenerator.nextId(null).longValue());
                summary.setTenantId(videoBelongUser.tenantId());
                summary.setUserId(videoBelongUser.userId());
                summary.setSourceId(sourceId);
                summary.setSourceType(sourceType.getCode());
                summary.setNotesType(NotesType.REVIEW_NOTES.getCode());
                summary.setCreateTime(LocalDateTime.now());
                summary.setUpdateTime(summary.getCreateTime());
                summary.setIsDeleted(false);
                summary.setCreateUserId(0L);
                summary.setUpdateUserId(0L);
                summary.setContentHash(DigestUtil.md5Hex(content)); // 计算并设置内容哈希
                summary.setLastVersion(1); // 初始版本号设为1
                summary.setContentFilePath(String.format(OSS_PREFIX, NotesType.REVIEW_NOTES, sourceType.getCode(), summary.getId()) + OSS_SUFFIX); // 构建内容文件路径模板
                log.info("[全文笔记] init video text notes summary upload for oss, videoId: {}, videoType: {}, notesType: {}", sourceId, sourceType, summary.getNotesType());
                // 将笔记内容上传至OSS
                String key = ossUtils.putObjectByString(AiOssUtils.bucketNameAi, summary.contentFilePath().get(), content);
                if (StrUtil.isBlank(key)) {
                    log.error("[全文笔记] init video text notes summary upload for oss fail, videoId: {}, videoType: {}, notesType: {}", sourceId, sourceType, summary.getNotesType());
                    return null;
                }
                videoTextNotesRepository.save(summary);
                redisTemplate.delete(this.getCacheKey(sourceId, sourceType, NotesType.REVIEW_NOTES));
                log.info("[全文笔记] init video text notes summary success videoId: {}, videoType: {}, notesType: {}", sourceId, sourceType, summary.getNotesType());
                return summary;
            }).orElse(null);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 获取笔记内容
     *
     * <p>该方法用于从系统中获取指定视频、类型和用户的笔记内容。
     * 首先尝试从 Redis 缓存中获取内容，若缓存不存在，则从 MongoDB 中查询并加载至缓存。</p>
     *
     * @param sourceId   笔记来源的ID，表示笔记关联的视频ID
     * @param sourceType 笔记来源的类型，用于区分笔记所属的视频类型（0：视频，1：文件，2：对比分析）
     * @param notesType  笔记的类型，支持以下取值：
     *                   <ul>
     *                     <li>1: 原文笔记</li>
     *                     <li>2: 复盘小结</li>
     *                     <li>3: 分段笔记</li>
     *                   </ul>
     * @param user       当前执行操作的用户对象，用于权限校验及租户隔离
     *
     * @return 返回一个封装了笔记内容的 {@link Optional} 对象，
     *     如果找到对应笔记则返回包含内容的 Optional，否则返回空 Optional
     */
    @Override
    public Optional<NotesContentVo> getNotesContent(String sourceId, VideoSourceType sourceType, NotesType notesType, UserCacheVo user) {
        return videoCombinationHandler.queryPermission(sourceId, sourceType, user, () -> {
            Optional<VideoTextNotes> videoTextNotes = videoTextNotesRepository.findVideoTextNotes(sourceId, sourceType.getCode(), notesType.getCode()).or(() -> {
                if (!Objects.equals(notesType, NotesType.REVIEW_NOTES)) {
                    return Optional.empty();
                }
                R<VideoBelongUserBO> editPermission = videoCombinationHandler.checkVideoEditPermission(sourceId, sourceType, user);
                if (editPermission.fail()) {
                    log.info("[全文笔记] get video text notes content initSummary check permission notPassed, videoId: {}, videoType: {}, notesType: {}, userId: {}", sourceId, sourceType, notesType, user.getId());
                    return Optional.empty();
                }
                // 初始化视频小结
                return Optional.ofNullable(this.initSummary(sourceId, sourceType, editPermission.getData()));
            });
            if (videoTextNotes.isEmpty()) {
                return Optional.empty();
            }
            // 如果是空内容那么就不需要找笔记内容了
            Optional<String> notesContentCommon = StrUtil.isBlank(videoTextNotes.get().getContentHash()) ? Optional.of("") : this.getNotesContentCommon(
                sourceId,
                VideoTextNotes::contentFilePath,
                id -> this.getCacheKey(id, sourceType, notesType),
                () -> videoTextNotes
            );
            VideoTextNotes notes = videoTextNotes.get();
            NotesContentVo contentVo = new NotesContentVo();
            contentVo.setLastVersion(notes.getLastVersion());
            contentVo.setCreateTime(notes.getCreateTime());
            contentVo.setUpdateTime(notes.getUpdateTime());
            notesContentCommon.ifPresent(contentVo::setContent);
            if (CollUtil.isNotEmpty(notes.getEditors())) {
                contentVo.setEditors(notes.getEditors().stream().map(editor -> {
                    NotesContentVo.Editor editorVo = new NotesContentVo.Editor();
                    editorVo.setUserId(editor.getUserId());
                    editorVo.setUserName(editor.getUserName());
                    editorVo.setEditTime(editor.getEditTime());
                    editorVo.setVersion(editor.getVersion());
                    return editorVo;
                }).toList());
            }
            return Optional.of(contentVo);
        }, Optional::empty);
    }


    /**
     * 获取指定视频、类型和版本的历史笔记内容
     *
     * <p>该方法用于从系统中获取特定视频的历史版本笔记内容。首先检查用户是否具有访问权限，
     * 然后尝试从 Redis 缓存中获取内容；若缓存未命中，则从 MongoDB 中查询对应的笔记记录，
     * 并通过版本号获取历史文件路径，最终从 OSS 读取内容并写入缓存。</p>
     *
     * @param sourceId   笔记来源的视频 ID，用于标识特定的视频资源
     * @param sourceType 笔记来源的视频类型，用于区分不同类型的视频源（如普通视频、文件等）
     * @param notesType  笔记的类型，支持以下取值：
     *                   <ul>
     *                     <li>1: 原文笔记</li>
     *                     <li>2: 复盘小结</li>
     *                     <li>3: 分段笔记</li>
     *                   </ul>
     * @param version    请求的历史版本号，用于定位特定版本的笔记内容
     * @param user       当前执行操作的用户对象，用于权限校验及租户隔离
     *
     * @return 返回一个封装了历史笔记内容的 {@link Optional} 对象，
     *     如果找到对应的历史笔记则返回包含内容的 Optional，否则返回空 Optional
     */
    @Override
    public Optional<String> getHistoryContent(String sourceId, VideoSourceType sourceType, NotesType notesType, int version, UserCacheVo user) {
        return videoCombinationHandler.queryPermission(sourceId, sourceType, user, () -> {
            return getNotesContentCommon(
                sourceId,
                notes -> notes.historyFilePath(version),
                id -> this.getHistoryCacheKey(id, sourceType, notesType, version)
                , () -> videoTextNotesRepository.findVideoTextNotes(sourceId, sourceType.getCode(), notesType.getCode())
            );
        }, Optional::empty);
    }


    /**
     * 获取是否存在笔记
     *
     * @param sourceIds  源id集合
     * @param sourceType 源类型
     * @param notesType  笔记类型
     *
     * @return {@link Map }<{@link String }, {@link Boolean }> key:源id value:是否存在笔记
     */
    @Override
    public Map<String, Boolean> getExistsNotes(Collection<String> sourceIds, VideoSourceType sourceType, NotesType notesType) {
        if (CollUtil.isEmpty(sourceIds)) {
            return Map.of();
        }
        if (sourceType == null) {
            return Map.of();
        }
        try {
            List<VideoTextNotes> notesList = videoTextNotesRepository.findSourceIdBySourceTypeAndNotesTypeAndIsDeleted(sourceIds, sourceType.getCode(), notesType.getCode());
            if (CollUtil.isEmpty(notesList)) {
                return Map.of();
            }

            return notesList.stream().map(VideoTextNotes::getSourceId).collect(Collectors.toMap(Function.identity(), item -> true, (a, b) -> a));
        } catch (Throwable e) {
            log.error("[全文笔记] query sourceID exists error, param sourceIds: {}, sourceType:{}, notesType:{}", sourceIds, sourceType, notesType);
            return Map.of();
        }
    }


    /**
     * 源是否存在笔记
     *
     * @param sourceIds  源id集合
     * @param sourceType 源类型
     *
     * @return {@link BiFunction }<{@link String }, {@link NotesType }, {@link Boolean }> param1:源id param2:笔记类型
     */
    @Override
    public BiFunction<String, NotesType, Boolean> sourceExists(Collection<String> sourceIds, VideoSourceType sourceType) {
        if (CollUtil.isEmpty(sourceIds) || sourceType == null) {
            return (sourceId, notesType) -> false;
        }
        try {
            List<VideoTextNotes> sourcesNotes = videoTextNotesRepository.findSourcesNotes(sourceIds, sourceType.getCode());
            Map<Integer, List<String>> noteTypeMap = sourcesNotes.stream().collect(Collectors.groupingBy(VideoTextNotes::getNotesType, Collectors.mapping(VideoTextNotes::getSourceId, Collectors.toList())));
            return (sourceId, notesType) -> {
                List<String> sourceIdsByNotesType = noteTypeMap.getOrDefault(notesType.getCode(), Collections.emptyList());
                return sourceIdsByNotesType.contains(sourceId);
            };
        } catch (Throwable e) {
            log.error("[全文笔记] query sourceID exists error, param sourceIds: {}, sourceType:{}", sourceIds, sourceType);
            return (sourceId, notesType) -> false;
        }

    }

    /**
     * 通用获取笔记内容的方法
     *
     * @param sourceId         视频源ID
     * @param cacheKeySupplier 提供缓存 key 的函数
     * @param pathFunction     提供文件路径的函数（可带版本等参数）
     *
     * @return 返回笔记内容的 Optional 对象
     */
    private Optional<String> getNotesContentCommon(String sourceId,
                                                   Function<VideoTextNotes, Optional<String>> pathFunction,
                                                   Function<String, String> cacheKeySupplier,
                                                   Supplier<Optional<VideoTextNotes>> notesSupplier) {
        String cacheKey = cacheKeySupplier.apply(sourceId);

        String cachedContent = redisTemplate.opsForValue().get(cacheKey);
        if (cachedContent != null) {
            // 检查是否为压缩内容
            if (cachedContent.startsWith(COMPRESSED_PREFIX)) {
                try {
                    // 解压内容
                    byte[] compressedData = Base64.getDecoder().decode(cachedContent.substring(5));
                    String decompressedContent = decompressGzip(compressedData);
                    return Optional.of(decompressedContent);
                } catch (Exception e) {
                    log.error("解压缓存内容失败，key为: {}", cacheKey, e);
                }
            }
            return Optional.of(cachedContent);
        }
        AtomicInteger timeout = new AtomicInteger(1);
        Optional<String> contentOptional = notesSupplier.get()
            .map(notes -> {
                long betweenDay = LocalDateTimeUtil.between(notes.getUpdateTime() == null ? notes.getCreateTime() : notes.getUpdateTime(), LocalDateTime.now(), ChronoUnit.DAYS);
                int timeoutHours;
                if (betweenDay < 7) {
                    // 更新时间在最近 7 天内，缓存 3 天（72 小时）
                    timeoutHours = 24 * 3; // 72 小时
                } else {
                    // 超过 7 天后，每增加一天，缓存时间减少 4 小时，最低为 3 小时
                    long daysBeyond = betweenDay - 7;
                    timeoutHours = Math.max(3, 72 - ((int) daysBeyond * 4));
                }
                timeout.set(timeoutHours);
                Optional<String> pathOpt = pathFunction.apply(notes);
                if (pathOpt.isPresent()) {
                    // 空内容的话忽略查询oss
                    if (StrUtil.isBlank(notes.getContentFilePath())) {
                        return null;
                    }
                    try {
                        return ossUtils.getObjectBackStr(AiOssUtils.bucketNameAi, ossUtils.completePath(pathOpt.get()));
                    } catch (IOException e) {
                        log.error("读取笔记内容失败，路径为: {}", pathOpt.get(), e);
                        return null;
                    }
                }
                return null;
            });

        // 优化缓存存储：对大内容进行压缩
        String contentToCache = contentOptional.orElse("");
        String finalCachedContent;

        // 判断内容大小是否超过最小压缩大小
        int contentLength = contentToCache.getBytes().length;
        if (contentLength > MIN_COMPRESS_SIZE) {
            try {
                // 动态计算压缩等级，内容越大压缩等级越高，最高为最大压缩等级 每100kb 增加一级
                int compressionLevel = Math.min(MAX_COMPRESSION_LEVEL, Math.max(3, contentLength / (1024 * 100)));
                byte[] compressedData = this.compressGzip(contentToCache, compressionLevel);
                finalCachedContent = COMPRESSED_PREFIX + Base64.getEncoder().encodeToString(compressedData);
            } catch (Exception e) {
                log.error("压缩内容失败，使用原始内容", e);
                finalCachedContent = contentToCache;
            }
        } else {
            finalCachedContent = contentToCache;
        }

        redisTemplate.opsForValue().setIfAbsent(cacheKey, finalCachedContent, timeout.get(), TimeUnit.HOURS);
        return contentOptional;
    }

    /**
     * GZIP压缩字符串
     *
     * @param data             要压缩的数据
     * @param compressionLevel 压缩等级 (1-9)
     *
     * @return 压缩后的字节数组
     *
     * @throws IOException IO异常
     */
    private byte[] compressGzip(String data, int compressionLevel) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        try (GZIPOutputStream gzipOS = new GZIPOutputStream(bos) {
            {
                def.setLevel(compressionLevel);
            }
        }) {
            gzipOS.write(data.getBytes());
        }
        return bos.toByteArray();
    }

    /**
     * GZIP解压缩字节数组
     *
     * @param compressedData 压缩的数据
     *
     * @return 解压后的字符串
     *
     * @throws IOException IO异常
     */
    private String decompressGzip(byte[] compressedData) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressedData));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString();
        }
    }
}
