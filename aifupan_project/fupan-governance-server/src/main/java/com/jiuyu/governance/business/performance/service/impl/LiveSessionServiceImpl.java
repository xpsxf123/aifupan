package com.jiuyu.governance.business.performance.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuyu.governance.business.org.mapper.PositionMapper;
import com.jiuyu.governance.business.org.pojo.entity.Position;
import com.jiuyu.governance.business.performance.mapper.*;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;
import com.jiuyu.governance.business.performance.pojo.entity.*;
import com.jiuyu.governance.business.performance.service.*;
import com.jiuyu.governance.business.performance.utils.MetricsUtil;
import com.jiuyu.governance.business.performance.utils.PerformanceCalculator;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.pojo.bo.RoomScheduleBo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleService;
import com.jiuyu.governance.business.performance.pojo.bo.OceanEngineProcessBo;
import com.jiuyu.governance.business.performance.pojo.bo.VideoProcessContext;
import com.jiuyu.governance.business.performance.pojo.constants.DataSource;
import com.jiuyu.governance.plugins.oss.storage.impl.ProcessDataStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场次服务实现类
 *
 * @author lj
 * &#064;date  2026-03-24
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LiveSessionServiceImpl extends ServiceImpl<LiveSessionMapper, LiveSession> implements LiveSessionService {

    /** 乐观锁最大重试次数 */
    private static final int MAX_RETRY = 3;

    private final SessionOriginalValueMapper sessionOriginalValueMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final ProcessDataStorageService processDataStorageService;
    private final ObjectMapper objectMapper;
    private final VideoProductMapper videoProductMapper;
    private final ProductMapper productMapper;
    private final SessionProductMapper sessionProductMapper;
    private final SessionPerformanceMapper sessionPerformanceMapper;
    private final SchedulePerformanceMapper schedulePerformanceMapper;
    private final LiveRoomScheduleService liveRoomScheduleService;
    private final SchedulePerformanceStaffMapper schedulePerformanceStaffMapper;
    private final SchedulePerformanceDetailMapper schedulePerformanceDetailMapper;
    private final PositionMapper positionMapper;
    private final ManagerPerformanceProcessor managerPerformanceProcessor;

    /**
     * 阶段2：事务内保存场次和商品
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveInTransaction(VideoProcessContext ctx) {
        // 查询该租户+批次号是否已有场次
        LiveSession existingSession = this.lambdaQuery()
                .eq(LiveSession::getTenantId, ctx.getTenantId())
                .eq(LiveSession::getBatchNumber, ctx.getBatchNumber())
                .one();

        Long sessionId;
        if (existingSession == null) {
            // 场次不存在，创建新场次
            sessionId = createNewSession(ctx.getVideos(), ctx.getTenantId(), ctx.getBatchNumber(), ctx.getLiveRoom());
        } else {
            sessionId = existingSession.getId();
            if (existingSession.getSource() == DataSource.SYSTEM.getCode()) {
                // 数据来源为系统推送，更新场次数据
                updateExistingSession(existingSession, ctx.getVideos(), ctx.getLiveRoom());
            } else {
                // 数据来源为手动录入，不更新业绩数据，记录到原始值表
                saveOriginalValueIfNotExists(existingSession, ctx.getVideos());
            }
        }

        ctx.setSessionId(sessionId);

        // 如果有 OSS URL，此时场次已存在，统一写库
        if (ctx.getOssUrl() != null) {
            this.lambdaUpdate()
                    .set(LiveSession::getOssUrl, ctx.getOssUrl())
                    .set(LiveSession::getUpdateDate, LocalDateTime.now())
                    .eq(LiveSession::getId, sessionId)
                    .update();
            log.info("[场次处理] 写入场次OSS URL, sessionId={}, ossUrl={}", sessionId, ctx.getOssUrl());
        }

        // 处理商品数据
        processGoodsData(ctx.getVideos(), sessionId);
    }

    /**
     * 阶段3：事务内计算业绩
     */
    @Transactional(rollbackFor = Exception.class)
    public void calculatePerformanceInTransaction(VideoProcessContext ctx) {
        LiveSession session = this.getById(ctx.getSessionId());
        ctx.setSession(session);

        if (session != null) {
            String secUid = ctx.getLiveRoom() != null ? ctx.getLiveRoom().getSecUid() : null;

            calculateSessionPerformance(session, ctx.getProcessData(), secUid);

            if (ctx.getProcessData() != null && !ctx.getProcessData().isEmpty()) {
                calculateSchedulePerformance(session, ctx.getProcessData(), secUid);
            }
        }
    }

    /**
     * 查询直播间信息
     */
    @Override
    public LiveRoom findLiveRoom(Long tenantId, String secUid) {
        if (secUid == null || secUid.isEmpty()) {
            log.warn("[场次处理] secUid为空, tenantId={}", tenantId);
            return null;
        }
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getTenantId, tenantId)
                .eq(LiveRoom::getSecUid, secUid)
                .last("LIMIT 1");
        return liveRoomMapper.selectOne(wrapper);
    }

    /**
     * 创建新场次
     */
    private Long createNewSession(List<LiveVideo> videos, Long tenantId, String batchNumber, LiveRoom liveRoom) {
        LiveSession session = new LiveSession();
        session.setTenantId(tenantId);
        session.setBatchNumber(batchNumber);
        session.setSource(DataSource.SYSTEM.getCode());
        session.setVersion(0);
        session.setIsDeleted(0);
        session.setCreateDate(LocalDateTime.now());
        session.setUpdateDate(LocalDateTime.now());

        // 填充直播间信息
        if (liveRoom != null) {
            session.setLiveRoomId(liveRoom.getId());
            session.setCompanyId(liveRoom.getCompanyId());
            session.setDeptId(liveRoom.getDeptId());
            session.setTeamId(liveRoom.getTeamId());
        } else {
            session.setLiveRoomId(0L);
            session.setCompanyId(0L);
            session.setDeptId(0L);
            session.setTeamId(0L);
        }

        // 合并视频业绩数据到场次
        mergeVideoDataToSession(session, videos);

        this.save(session);
        return session.getId();
    }

    /**
     * 更新已有场次（系统推送来源）- 使用乐观锁
     */
    private void updateExistingSession(LiveSession existingSession, List<LiveVideo> videos, LiveRoom liveRoom) {
        for (int i = 0; i < MAX_RETRY; i++) {
            // 重新查询最新数据
            LiveSession session = this.getById(existingSession.getId());
            if (session == null) {
                log.warn("[场次处理] 场次不存在, sessionId={}", existingSession.getId());
                return;
            }

            // 填充直播间信息
            if (liveRoom != null) {
                session.setLiveRoomId(liveRoom.getId());
                session.setCompanyId(liveRoom.getCompanyId());
                session.setDeptId(liveRoom.getDeptId());
                session.setTeamId(liveRoom.getTeamId());
            }

            // 合并视频业绩数据到场次
            mergeVideoDataToSession(session, videos);
            session.setUpdateDate(LocalDateTime.now());

            // 乐观锁更新（MyBatis-Plus @Version 自动处理）
            boolean updated = this.updateById(session);
            if (updated) {
                log.info("[场次处理] 更新场次成功, sessionId={}", session.getId());
                return;
            }
            log.warn("[场次处理] 乐观锁冲突，重试 {}/{}, sessionId={}", i + 1, MAX_RETRY, session.getId());
        }
        throw new RuntimeException("更新场次失败，乐观锁冲突超过最大重试次数");
    }

    /**
     * 手动录入来源的场次：将系统推送的数据记录到原始值表（仅首次记录）
     */
    private void saveOriginalValueIfNotExists(LiveSession existingSession, List<LiveVideo> videos) {
        Long sessionId = existingSession.getId();
        Long tenantId = existingSession.getTenantId();

        // 用视频数据计算出系统推送的业绩值，作为原始值记录
        BigDecimal salesRevenue = MetricsUtil.calcMaxAmount(videos.stream().map(LiveVideo::getSalesRevenue).collect(Collectors.toList()));
        BigDecimal refund = MetricsUtil.calcMaxAmount(videos.stream().map(LiveVideo::getRefund).collect(Collectors.toList()));
        BigDecimal investment = MetricsUtil.calcMaxAmount(videos.stream().map(LiveVideo::getInvestment).collect(Collectors.toList()));
        BigDecimal netSales = MetricsUtil.subtract(salesRevenue, refund);
        BigDecimal roi = MetricsUtil.roi(netSales, investment);
        Integer viewCount = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getViewCount).collect(Collectors.toList()));
        Integer refundQuantity = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getRefundQuantity).collect(Collectors.toList()));
        Integer payComboCnt = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getPayComboCnt).collect(Collectors.toList()));
        Integer exposureCount = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getExposureCount).collect(Collectors.toList()));
        Integer followCount = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getFollowCount).collect(Collectors.toList()));
        Integer maxOnline = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getMaxOnline).collect(Collectors.toList()));

        BasePerformanceEntity performance = new BasePerformanceEntity();
        performance.setViewCount(viewCount);
        performance.setSalesRevenue(salesRevenue);
        performance.setRefund(refund);
        performance.setInvestment(investment);
        performance.setNetSales(netSales);
        performance.setRoi(roi);
        performance.setRefundQuantity(refundQuantity);
        performance.setPayComboCnt(payComboCnt);
        performance.setExposureCount(exposureCount);
        performance.setFollowCount(followCount);
        performance.setMaxOnline(maxOnline);
        performance.setRefundRate(MetricsUtil.toPercentValue(
                MetricsUtil.divide(refundQuantity, payComboCnt), 2));
        performance.setThousandSales(MetricsUtil.multiply(
                MetricsUtil.divide(salesRevenue, viewCount != null ? BigDecimal.valueOf(viewCount) : null), BigDecimal.valueOf(1000)));
        performance.setConversionRate(MetricsUtil.toPercentValue(
                MetricsUtil.divide(payComboCnt, viewCount), 2));
        performance.setUvValue(MetricsUtil.divide(salesRevenue, viewCount != null ? BigDecimal.valueOf(viewCount) : null));
        performance.setFollowRate(MetricsUtil.toPercentValue(
                MetricsUtil.divide(followCount, viewCount), 2));

        // 点击-成交率和互动率取最后一条视频的值
        videos.sort(Comparator.comparing(LiveVideo::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        LiveVideo lastVideo = videos.get(videos.size() - 1);
        performance.setClickPaymentRate(lastVideo.getClickPaymentRate());
        performance.setInteractionRate(lastVideo.getInteractionRate());

        managerPerformanceProcessor.saveOriginalValueForPerformance(tenantId, sessionId, SessionOriginalValue.SOURCE_TYPE_SESSION, performance);
    }

    /**
     * 合并视频业绩数据到场次对象
     */
    private void mergeVideoDataToSession(LiveSession session, List<LiveVideo> videos) {
        // 按时间排序
        videos.sort(Comparator.comparing(LiveVideo::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

        // 场次时间范围
        LocalDateTime startTime = videos.stream()
                .map(LiveVideo::getStartTime)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(LocalDateTime.now());

        LocalDateTime endTime = videos.stream()
                .map(LiveVideo::getEndTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.now());

        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setDuration((int) Duration.between(startTime, endTime).getSeconds());

        // 场观：取最大累计值
        Integer viewCount = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getViewCount).collect(Collectors.toList()));
        session.setViewCount(viewCount);

        // 销售额：取最大累计值
        BigDecimal salesRevenue = MetricsUtil.calcMaxAmount(videos.stream().map(LiveVideo::getSalesRevenue).collect(Collectors.toList()));
        session.setSalesRevenue(salesRevenue);

        // 退款：取最大累计值
        BigDecimal refund = MetricsUtil.calcMaxAmount(videos.stream().map(LiveVideo::getRefund).collect(Collectors.toList()));
        session.setRefund(refund);

        // 投放：取最大值
        BigDecimal investment = MetricsUtil.calcMaxAmount(videos.stream().map(LiveVideo::getInvestment).collect(Collectors.toList()));
        session.setInvestment(investment);

        // 退款单量：取最大值
        Integer refundQuantity = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getRefundQuantity).collect(Collectors.toList()));
        session.setRefundQuantity(refundQuantity);

        // 成交单量：取最大值
        Integer payComboCnt = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getPayComboCnt).collect(Collectors.toList()));
        session.setPayComboCnt(payComboCnt);

        // 曝光次数：取最大值
        Integer exposureCount = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getExposureCount).collect(Collectors.toList()));
        session.setExposureCount(exposureCount);

        // 涨粉人数：取最大值
        Integer followCount = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getFollowCount).collect(Collectors.toList()));
        session.setFollowCount(followCount);

        // 最高在线：取最大值
        Integer maxOnline = MetricsUtil.calcMaxViewCount(videos.stream().map(LiveVideo::getMaxOnline).collect(Collectors.toList()));
        session.setMaxOnline(maxOnline);

        // 点击-成交率：取最后一条视频的值
        LiveVideo lastVideo = videos.get(videos.size() - 1);
        session.setClickPaymentRate(lastVideo.getClickPaymentRate());

        // 互动率：取最后一条视频的值
        session.setInteractionRate(lastVideo.getInteractionRate());

        // 净销售额 = 销售额 - 退款
        BigDecimal netSales = MetricsUtil.subtract(salesRevenue, refund);
        session.setNetSales(netSales);

        // ROI = 净销售额 / 投放
        session.setRoi(MetricsUtil.roi(netSales, investment));

        // 退款率 = 退款单量 / 成交单量
        session.setRefundRate(MetricsUtil.toPercentValue(
                MetricsUtil.divide(refundQuantity, payComboCnt), 2));

        // 千次成交 = 销售额 / 观看人数 * 1000
        session.setThousandSales(MetricsUtil.multiply(
                MetricsUtil.divide(salesRevenue, viewCount != null ? BigDecimal.valueOf(viewCount) : null), BigDecimal.valueOf(1000)));

        // 带货转化率 = 成交单量 / 观看人数 * 100%
        session.setConversionRate(MetricsUtil.toPercentValue(
                MetricsUtil.divide(payComboCnt, viewCount), 2));

        // UV价值 = 销售额 / 观看人数
        session.setUvValue(MetricsUtil.divide(salesRevenue, viewCount != null ? BigDecimal.valueOf(viewCount) : null));

        // 涨粉率 = 涨粉人数 / 观看人数 * 100%
        session.setFollowRate(MetricsUtil.toPercentValue(
                MetricsUtil.divide(followCount, viewCount), 2));
    }

    /**
     * 处理商品数据
     * 1、查询出视频对应的商品数据(video_product)
     * 2、处理商品数据，商品id一样的合并成一个，数据取最大值
     * 3、保存到product表（商品id已存在，覆盖更新），再把场次的商品数据保存到session_product表
     */
    private void processGoodsData(List<LiveVideo> videos, Long sessionId) {
        if (sessionId == null) {
            log.warn("[商品处理] sessionId为空，跳过商品数据处理");
            return;
        }

        // 获取租户信息
        LiveVideo firstVideo = videos.get(0);
        Long tenantId = firstVideo.getTenantId();

        // 1、查询出视频对应的商品数据
        List<String> videoIds = videos.stream()
                .map(LiveVideo::getVideoId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (videoIds.isEmpty()) {
            log.info("[商品处理] 无视频ID，跳过商品数据处理, sessionId={}", sessionId);
            return;
        }

        List<VideoProduct> allVideoProducts = videoProductMapper.selectList(
                new LambdaQueryWrapper<VideoProduct>()
                        .eq(VideoProduct::getTenantId, tenantId)
                        .in(VideoProduct::getVideoId, videoIds));
        if (allVideoProducts.isEmpty()) {
            log.info("[商品处理] 无商品数据, sessionId={}, videoIds={}", sessionId, videoIds);
            return;
        }

        // 2、按商品productId分组，合并取最大值
        Map<String, VideoProduct> mergedProducts = new LinkedHashMap<>();
        for (VideoProduct vp : allVideoProducts) {
            String productId = vp.getProductId();
            if (productId == null || productId.isEmpty()) {
                continue;
            }
            mergedProducts.merge(productId, vp, this::mergeVideoProduct);
        }

        if (mergedProducts.isEmpty()) {
            return;
        }

        // 查询场次信息获取组织架构
        LiveSession session = this.getById(sessionId);
        Long companyId = session != null ? session.getCompanyId() : 0L;
        Long deptId = session != null ? session.getDeptId() : 0L;
        Long teamId = session != null ? session.getTeamId() : 0L;

        // 3、保存到product表和session_product表
        for (VideoProduct mergedVp : mergedProducts.values()) {
            // 3.1 保存/更新商品基础信息到product表，返回 product.id
            Long productId = saveOrUpdateProduct(tenantId, mergedVp);

            // 3.2 保存/更新场次商品关联到session_product表（使用 product.id 关联）
            saveOrUpdateSessionProduct(tenantId, sessionId, productId, mergedVp, companyId, deptId, teamId);
        }

    }

    /**
     * 合并两个VideoProduct，数值字段取最大值
     */
    private VideoProduct mergeVideoProduct(VideoProduct existing, VideoProduct incoming) {
        // 基础信息优先用新的（标题、图片、价格）
        if (incoming.getTitle() != null) {
            existing.setTitle(incoming.getTitle());
        }
        if (incoming.getImageUri() != null) {
            existing.setImageUri(incoming.getImageUri());
        }
        existing.setMarketPrice(maxBigDecimal(existing.getMarketPrice(), incoming.getMarketPrice()));
        existing.setExplainCnt(maxInteger(existing.getExplainCnt(), incoming.getExplainCnt()));
        existing.setProductShowUcnt(maxLong(existing.getProductShowUcnt(), incoming.getProductShowUcnt()));
        existing.setProductClickUcnt(maxLong(existing.getProductClickUcnt(), incoming.getProductClickUcnt()));
        existing.setProductShowClickUcntRatio(maxBigDecimal(existing.getProductShowClickUcntRatio(), incoming.getProductShowClickUcntRatio()));
        existing.setProductShowPayUcntRatio(maxBigDecimal(existing.getProductShowPayUcntRatio(), incoming.getProductShowPayUcntRatio()));
        existing.setProductClickPayUcntRatio(maxBigDecimal(existing.getProductClickPayUcntRatio(), incoming.getProductClickPayUcntRatio()));
        existing.setGpm(maxBigDecimal(existing.getGpm(), incoming.getGpm()));
        existing.setPayAmt(maxBigDecimal(existing.getPayAmt(), incoming.getPayAmt()));
        existing.setAvgMaxPayAmtMin(maxBigDecimal(existing.getAvgMaxPayAmtMin(), incoming.getAvgMaxPayAmtMin()));
        existing.setPayComboCnt(maxLong(existing.getPayComboCnt(), incoming.getPayComboCnt()));
        existing.setPayCnt(maxLong(existing.getPayCnt(), incoming.getPayCnt()));
        existing.setCreateCnt(maxLong(existing.getCreateCnt(), incoming.getCreateCnt()));
        existing.setCreatePayUcntRatio(maxBigDecimal(existing.getCreatePayUcntRatio(), incoming.getCreatePayUcntRatio()));
        existing.setPayDepositPreOrderCnt(maxLong(existing.getPayDepositPreOrderCnt(), incoming.getPayDepositPreOrderCnt()));
        existing.setPresaleDepayDeamt(maxBigDecimal(existing.getPresaleDepayDeamt(), incoming.getPresaleDepayDeamt()));
        existing.setPayDepositPreOrderAmt(maxBigDecimal(existing.getPayDepositPreOrderAmt(), incoming.getPayDepositPreOrderAmt()));
        existing.setRefundCnt(maxInteger(existing.getRefundCnt(), incoming.getRefundCnt()));
        existing.setRealRefundAmt(maxBigDecimal(existing.getRealRefundAmt(), incoming.getRealRefundAmt()));
        existing.setRefundRate(maxBigDecimal(existing.getRefundRate(), incoming.getRefundRate()));
        return existing;
    }

    /**
     * 保存或更新商品基础信息到product表
     *
     * @return product.id（主键）
     */
    private Long saveOrUpdateProduct(Long tenantId, VideoProduct vp) {
        Product existingProduct = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getProductId, vp.getProductId())
                .last("limit 1")
        );

        if (existingProduct != null) {
            // 已存在，更新
            existingProduct.setName(vp.getTitle());
            existingProduct.setImageUri(vp.getImageUri());
            existingProduct.setUpdateDate(LocalDateTime.now());
            productMapper.updateById(existingProduct);
            return existingProduct.getId();
        } else {
            // 不存在，新增
            Product product = new Product();
            product.setTenantId(tenantId);
            product.setProductId(vp.getProductId());
            product.setName(vp.getTitle());
            product.setImageUri(vp.getImageUri());
            product.setCreateDate(LocalDateTime.now());
            product.setUpdateDate(LocalDateTime.now());
            product.setIsDeleted(0);
            productMapper.insert(product);
            return product.getId();
        }
    }

    /**
     * 保存或更新场次商品关联到session_product表
     *
     * @param productId product表的主键ID（非第三方商品ID）
     */
    private void saveOrUpdateSessionProduct(Long tenantId, Long sessionId, Long productId,
                                            VideoProduct vp, Long companyId, Long deptId, Long teamId) {
        SessionProduct existingSp = sessionProductMapper.selectOne(new LambdaQueryWrapper<SessionProduct>()
                .eq(SessionProduct::getTenantId, tenantId)
                .eq(SessionProduct::getSessionId, sessionId)
                .eq(SessionProduct::getProductId, productId)
                .last("limit 1"));

        if (existingSp != null) {
            // 已存在，覆盖更新
            fillSessionProductFromVideoProduct(existingSp, vp, companyId, deptId, teamId);
            existingSp.setUpdateDate(LocalDateTime.now());
            sessionProductMapper.updateById(existingSp);
        } else {
            // 不存在，新增
            SessionProduct sp = new SessionProduct();
            sp.setTenantId(tenantId);
            sp.setSessionId(sessionId);
            sp.setProductId(productId);
            fillSessionProductFromVideoProduct(sp, vp, companyId, deptId, teamId);
            sp.setCreateDate(LocalDateTime.now());
            sp.setUpdateDate(LocalDateTime.now());
            sp.setIsDeleted(0);
            sessionProductMapper.insert(sp);
        }
    }

    /**
     * 从VideoProduct填充SessionProduct的业务字段
     */
    private void fillSessionProductFromVideoProduct(SessionProduct sp, VideoProduct vp,
                                                    Long companyId, Long deptId, Long teamId) {
        sp.setQuantity(vp.getPayComboCnt() != null ? vp.getPayComboCnt().intValue() : 0);
        sp.setPrice(vp.getMarketPrice() != null ? vp.getMarketPrice() : BigDecimal.ZERO);
        sp.setSalesAmount(vp.getPayAmt() != null ? vp.getPayAmt() : BigDecimal.ZERO);
        sp.setExposureClickRate(vp.getProductShowClickUcntRatio() != null ? vp.getProductShowClickUcntRatio() : BigDecimal.ZERO);
        sp.setExposureConversionRate(vp.getProductShowPayUcntRatio() != null ? vp.getProductShowPayUcntRatio() : BigDecimal.ZERO);
        sp.setGpm(vp.getGpm() != null ? vp.getGpm() : BigDecimal.ZERO);
        sp.setRefundQuantity(vp.getRefundCnt() != null ? vp.getRefundCnt() : 0);
        sp.setRefundAmount(vp.getRealRefundAmt() != null ? vp.getRealRefundAmt() : BigDecimal.ZERO);
        sp.setRefundRate(vp.getRefundRate() != null ? vp.getRefundRate() : BigDecimal.ZERO);
        sp.setClickPaymentRate(vp.getProductClickPayUcntRatio() != null ? vp.getProductClickPayUcntRatio() : BigDecimal.ZERO);
        sp.setCompanyId(companyId);
        sp.setDeptId(deptId);
        sp.setTeamId(teamId);
    }

    private BigDecimal maxBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }

    private Long maxLong(Long a, Long b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }

    private Integer maxInteger(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }

    /**
     * 加载视频OSS过程数据
     * <p>
     * 一个场次仅一条 live_video 记录，直接使用其 videoOssUrl：
     * 1. 下载zip文件
     * 2. 解压提取JSON，反序列化为 List&lt;OceanEngineProcessBo&gt;
     * 3. 按 gatherDateTime 去重排序
     * 4. 不重新上传，ossUrl 直接使用 live_video 的原始地址
     * </p>
     *
     * @param ctx 视频处理上下文
     */
    @Override
    public void loadVideoOssData(VideoProcessContext ctx) {
        List<LiveVideo> videos = ctx.getVideos();
        Long tenantId = ctx.getTenantId();
        String batchNumber = ctx.getBatchNumber();

        // 一个场次仅一条 live_video
        LiveVideo video = videos.get(0);
        String videoOssUrl = video.getVideoOssUrl();

        if (videoOssUrl == null || videoOssUrl.isEmpty()) {
            log.info("[场次处理] 视频无OSS数据, tenantId={}, batchNumber={}", tenantId, batchNumber);
            ctx.setProcessData(Collections.emptyList());
            return;
        }

        // ossUrl 直接使用 live_video 的原始地址，不再合并上传
        ctx.setOssUrl(videoOssUrl);

        // 下载zip字节数据
        List<OceanEngineProcessBo> allProcessData = processDataStorageService.downloadProcessDataFromOss(videoOssUrl, OceanEngineProcessBo.class);

        if (allProcessData.isEmpty()) {
            log.info("[场次处理] 解析后无过程数据, tenantId={}, batchNumber={}", tenantId, batchNumber);
            ctx.setProcessData(Collections.emptyList());
            return;
        }

        // 按 gatherDateTime 去重（保留每个时间戳的第一条）并排序
        List<OceanEngineProcessBo> deduped = new ArrayList<>(
                allProcessData.stream()
                        .filter(bo -> bo.getGatherDateTime() != null)
                        .collect(Collectors.toMap(
                                OceanEngineProcessBo::getGatherDateTime,
                                bo -> bo,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new))
                        .values());
        deduped.sort(Comparator.comparing(OceanEngineProcessBo::getGatherDateTime));

        ctx.setProcessData(deduped);
        log.info("[场次处理] 加载OSS过程数据完成, tenantId={}, batchNumber={}, 数据条数={}",
                tenantId, batchNumber, deduped.size());
    }

    // ========================= 场次业绩计算 =========================

    /**
     * 计算场次业绩（以天为维度）
     * 不跨天：直接使用场次的业绩数据
     * 跨天：通过实时数据差值计算
     */
    private void calculateSessionPerformance(LiveSession session, List<OceanEngineProcessBo> processData, String secUid) {
        LocalDateTime startTime = session.getStartTime();
        LocalDateTime endTime = session.getEndTime();
        if (startTime == null || endTime == null) {
            log.warn("[业绩计算] 场次时间为空, sessionId={}", session.getId());
            return;
        }

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();
        boolean crossDay = !startDate.equals(endDate);

        if (!crossDay) {
            saveOrUpdateSessionPerformance(session, startDate, startTime, endTime, secUid);
            return;
        }

        if (processData == null || processData.isEmpty()) {
            log.info("[业绩计算] 跨天场次无实时数据，跳过分日业绩计算, sessionId={}", session.getId());
            return;
        }

        // 构建关键时间点列表与查找Map
        List<Long> keyTimePoints = buildKeyTimePoints(startTime, endTime, startDate, endDate);
        Map<LocalDateTime, OceanEngineProcessBo> valueMap = buildKeyValueMap(
                processData, keyTimePoints,
                startTime.atZone(PerformanceCalculator.ZONE).toInstant().toEpochMilli());

        for (int i = 0; i < keyTimePoints.size(); i++) {

            // 跳过最后一个时间点
            if (i == keyTimePoints.size() - 1){
                break;
            }
            int nextIndex = i + 1;

            Long startIndexTime = keyTimePoints.get(i);
            Long endTIndexTime = keyTimePoints.get(nextIndex);
            LocalDateTime currentLocal = Instant.ofEpochMilli(startIndexTime).atZone(PerformanceCalculator.ZONE).toLocalDateTime();
            LocalDateTime nextLocal = Instant.ofEpochMilli(endTIndexTime).atZone(PerformanceCalculator.ZONE).toLocalDateTime();

            BasePerformanceEntity interval = computeInterval(processData, valueMap,
                    startIndexTime, currentLocal, endTIndexTime, nextLocal);
            if (interval == null) {
                log.warn("[业绩计算] 计算场次业绩间隔数据失败, sessionId={}, startTime={}, endTime={}",
                        session.getId(), currentLocal, nextLocal);
                continue;
            }

            LocalDateTime dayStart = currentLocal;
            LocalDateTime dayEnd = nextLocal;
            if (i != 0){
                dayStart = dayStart.plusSeconds(1);
            }

            LiveSession currentSession = BeanUtil.copyProperties(session, LiveSession.class);
            currentSession.setPerformance(interval);
            saveOrUpdateSessionPerformance(currentSession, dayStart.toLocalDate(), dayStart, dayEnd, secUid);
        }
    }



    /**
     * 构建关键时间点列表（epochMilli）
     * [startTime, start_23:59:59, day2_23:59:59, ..., endTime]
     */
    private List<Long> buildKeyTimePoints(LocalDateTime startTime, LocalDateTime endTime,
                                           LocalDate startDate, LocalDate endDate) {
        List<Long> keyTimePoints = new ArrayList<>();
        // 第0个：startTime
        keyTimePoints.add(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        // 中间：每天 23:59:59
        LocalDate current = startDate;
        while (current.isBefore(endDate)) {
            keyTimePoints.add(current.atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            current = current.plusDays(1);
        }
        // 最后一个：endTime
        keyTimePoints.add(endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return keyTimePoints;
    }

    /**
     * 计算每个关键时间点的累加值
     * - key[0]（startTime）：全部为0
     * - key[last]（endTime）：取processData最后一条数据的值
     * - 中间点：10分钟阈值规则 + 线性插值
     */
    private List<OceanEngineProcessBo> buildKeyTimeValues(List<OceanEngineProcessBo> processData, List<Long> keyTimePoints, Long videoStartTime) {
        if (processData == null || processData.isEmpty()){
            return Collections.emptyList();
        }
        List<OceanEngineProcessBo> keyValues = new ArrayList<>();
        for (int i = 0; i < keyTimePoints.size(); i++) {
            Long time = keyTimePoints.get(i);
            if (i == 0 && Objects.equals(videoStartTime, time)) {
                // V(start_time) = 0
                keyValues.add(OceanEngineProcessBo.builder()
                        .gatherDateTime(Instant.ofEpochMilli(time).atZone(PerformanceCalculator.ZONE).toLocalDateTime())
                        .exposureCount(0)
                        .viewCount(0)
                        .onlineCount(0)
                        .followCount(0)
                        .salesRevenue(BigDecimal.ZERO)
                        .refund(BigDecimal.ZERO)
                        .investment(BigDecimal.ZERO)
                        .refundQuantity(0)
                        .payComboCnt(0)
                        .build());
            }
//            else if (time <= endVal.getGatherDateTime().atZone(PerformanceCalculator.ZONE).toInstant().toEpochMilli()) {
//                // V(end_time) = 最终累加值
//                OceanEngineProcessBo lastBo = processData.get(processData.size() - 1);
//                keyValues.add(BeanUtil.copyProperties(lastBo, OceanEngineProcessBo.class));
//            }
            else {
                // 中间关键时间点：10分钟阈值 + 线性插值
                keyValues.add(PerformanceCalculator.getAccumulatedValuesAtTimestamp(processData, keyTimePoints.get(i)));
            }
        }
        return keyValues;
    }

    /**
     * 构建关键时间点对应的值查找Map
     */
    private Map<LocalDateTime, OceanEngineProcessBo> buildKeyValueMap(
            List<OceanEngineProcessBo> processData, List<Long> keyTimePoints, Long videoStartTime) {
        List<OceanEngineProcessBo> keyValues = buildKeyTimeValues(processData, keyTimePoints, videoStartTime);
        return keyValues.stream()
                .filter(v -> v.getGatherDateTime() != null)
                .collect(Collectors.toMap(OceanEngineProcessBo::getGatherDateTime, v -> v, (a, b) -> a));
    }

    /**
     * 根据两个时间点的实时数据值，计算区间增量业绩
     */
    private BasePerformanceEntity calculateIntervalFromValues(
            List<OceanEngineProcessBo> processData,
            long startEpochMilli, long endEpochMilli,
            OceanEngineProcessBo startValue, OceanEngineProcessBo endValue) {
        BasePerformanceEntity startData = new BasePerformanceEntity();
        startData.setPerformanceOceanEngine(startValue);
        BasePerformanceEntity endData = new BasePerformanceEntity();
        endData.setPerformanceOceanEngine(endValue);
        return PerformanceCalculator.calculateIntervalPerformance(
                processData, startEpochMilli, endEpochMilli, startData, endData);
    }

    /**
     * 从 valueMap 取值并计算区间业绩
     * @return 区间业绩，查不到值或计算失败返回 null
     */
    private BasePerformanceEntity computeInterval(
            List<OceanEngineProcessBo> processData,
            Map<LocalDateTime, OceanEngineProcessBo> valueMap,
            long startEpochMilli, LocalDateTime startLocal,
            long endEpochMilli, LocalDateTime endLocal) {
        OceanEngineProcessBo startValue = valueMap.get(startLocal);
        OceanEngineProcessBo endValue = valueMap.get(endLocal);
        if (startValue == null || endValue == null) {
            return null;
        }
        return calculateIntervalFromValues(processData, startEpochMilli, endEpochMilli, startValue, endValue);
    }

    /**
     * 保存或更新场次业绩
     */
    private void saveOrUpdateSessionPerformance(LiveSession session, LocalDate statsDate,
                                                 LocalDateTime dayStart, LocalDateTime dayEnd, String secUid) {
        // 查询是否已存在
        SessionPerformance existing = sessionPerformanceMapper.selectOne(new LambdaQueryWrapper<SessionPerformance>()
                .eq(SessionPerformance::getTenantId, session.getTenantId())
                .eq(SessionPerformance::getSessionId, session.getId())
                .eq(SessionPerformance::getStatsDate, statsDate)
                .last("limit 1")
        );

        if (existing == null) {
            // 不存在，插入
            SessionPerformance sp = new SessionPerformance();
            sp.setTenantId(session.getTenantId());
            sp.setSessionId(session.getId());
            sp.setStatsDate(statsDate);
            sp.setStartTime(dayStart);
            sp.setEndTime(dayEnd);
            sp.setSecUid(secUid);
            sp.setSource(DataSource.SYSTEM.getCode());
            sp.setPerformance(session);
            sp.setLiveRoomId(session.getLiveRoomId());
            sp.setCompanyId(session.getCompanyId());
            sp.setDeptId(session.getDeptId());
            sp.setTeamId(session.getTeamId());
            sp.setCreateDate(LocalDateTime.now());
            sp.setUpdateDate(LocalDateTime.now());
            sp.setIsDeleted(0);
            sessionPerformanceMapper.insert(sp);
        } else if (existing.getSource() == DataSource.SYSTEM.getCode()) {
            // 已存在且为系统录入，覆盖更新
            existing.setStartTime(dayStart);
            existing.setEndTime(dayEnd);
            existing.setPerformance(session);
            existing.setLiveRoomId(session.getLiveRoomId());
            existing.setUpdateDate(LocalDateTime.now());
            sessionPerformanceMapper.updateById(existing);
        } else {
            // 已存在且为手动录入，不更新，记录原始值
            managerPerformanceProcessor.saveOriginalValueForPerformance(
                    session.getTenantId(), existing.getId(),
                    SessionOriginalValue.SOURCE_TYPE_SESSION_PERFORMANCE,
                    session);
        }
    }

    // ========================= 排班业绩计算 =========================

    /**
     * 计算排班业绩
     */
    private void calculateSchedulePerformance(LiveSession session, List<OceanEngineProcessBo> processData, String secUid) {
        if (secUid == null || secUid.isEmpty()) {
            log.warn("[排班业绩] secUid为空, sessionId={}", session.getId());
            return;
        }
        Long tenantId = session.getTenantId();

        LivePlatformType platformType = resolvePlatformType(session);
        if (platformType == null) {
            return;
        }

        List<RoomScheduleBo> schedules = liveRoomScheduleService.getRangeTimeLiveSchedule(
                tenantId, platformType, List.of(secUid),
                session.getStartTime(), session.getEndTime());
        if (schedules == null || schedules.isEmpty()) {
            log.info("[排班业绩] 未找到重叠排班, sessionId={}, secUid={}", session.getId(), secUid);
            return;
        }

        Map<Long, SchedulePerformance> existingSpMap = loadExistingSpMap(tenantId, schedules);
        Map<Long, String> positionNameMap = loadPositionNameMap(schedules);

        List<ScheduleProcessContext> spContexts = buildSpContexts(
                session, schedules, existingSpMap, processData, secUid, tenantId);
        if (spContexts.isEmpty()) {
            return;
        }

        List<SchedulePerformanceStaff> newStaffs = saveDetailsAndCollectStaff(
                tenantId, session, spContexts, positionNameMap);

        if (!newStaffs.isEmpty()) {
            for (SchedulePerformanceStaff staff : newStaffs) {
                schedulePerformanceStaffMapper.insert(staff);
            }
        }

        aggregateAllSps(tenantId, spContexts.stream().map(c -> c.spId).collect(Collectors.toSet()), spContexts);
    }

    /**
     * 解析直播间平台类型
     */
    private LivePlatformType resolvePlatformType(LiveSession session) {
        LiveRoom liveRoom = liveRoomMapper.selectById(session.getLiveRoomId());
        if (liveRoom == null) {
            log.warn("[排班业绩] 直播间不存在, sessionId={}, liveRoomId={}", session.getId(), session.getLiveRoomId());
            return null;
        }
        LivePlatformType platformType = LivePlatformType.getByValue(liveRoom.getPlatform());
        if (platformType == null) {
            log.warn("[排班业绩] 未知平台类型, sessionId={}, platform={}", session.getId(), liveRoom.getPlatform());
        }
        return platformType;
    }

    /**
     * 批量查询岗位名称
     */
    private Map<Long, String> loadPositionNameMap(List<RoomScheduleBo> schedules) {
        Set<Long> positionIds = schedules.stream()
                .filter(s -> s.getWorkUserList() != null)
                .flatMap(s -> s.getWorkUserList().stream())
                .map(RoomScheduleBo.WorkUser::getPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (positionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return positionMapper.selectByIds(positionIds).stream()
                .collect(Collectors.toMap(Position::getId, Position::getName, (a, b) -> a));
    }

    /**
     * 批量查询已有排班业绩主记录
     */
    private Map<Long, SchedulePerformance> loadExistingSpMap(Long tenantId, List<RoomScheduleBo> schedules) {
        Set<Long> scheduleIds = schedules.stream()
                .map(RoomScheduleBo::getScheduleId)
                .collect(Collectors.toSet());
        return schedulePerformanceMapper.selectList(
                new LambdaQueryWrapper<SchedulePerformance>()
                        .eq(SchedulePerformance::getTenantId, tenantId)
                        .in(SchedulePerformance::getScheduleId, scheduleIds))
                .stream()
                .collect(Collectors.toMap(SchedulePerformance::getScheduleId, sp -> sp, (a, b) -> a));
    }

    /**
     * 第一遍：计算每个排班的业绩并创建/获取排班业绩主记录
     */
    private List<ScheduleProcessContext> buildSpContexts(
            LiveSession session, List<RoomScheduleBo> schedules,
            Map<Long, SchedulePerformance> existingSpMap,
            List<OceanEngineProcessBo> processData, String secUid, Long tenantId) {

        List<ScheduleProcessContext> spContexts = new ArrayList<>();
        for (RoomScheduleBo schedule : schedules) {
            LocalDateTime scheduleStart = schedule.getStartWork();
            LocalDateTime scheduleEnd = schedule.getEndWork();
            if (scheduleStart == null || scheduleEnd == null) {
                continue;
            }

            LocalDateTime overlapStart = scheduleStart.isAfter(session.getStartTime()) ? scheduleStart : session.getStartTime();
            LocalDateTime overlapEnd = scheduleEnd.isBefore(session.getEndTime()) ? scheduleEnd : session.getEndTime();
            if (!overlapStart.isBefore(overlapEnd)) {
                continue;
            }

            BasePerformanceEntity perf = new BasePerformanceEntity();

            boolean scheduleContainsSession = !scheduleStart.isAfter(session.getStartTime())
                    && !scheduleEnd.isBefore(session.getEndTime());

            if (scheduleContainsSession) {
                perf.setPerformanceCalculate(session);
                perf.setClickPaymentRate(session.getClickPaymentRate());
                perf.setInteractionRate(session.getInteractionRate());
            } else {
                long startEpoch = schedule.getStartWork().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long endEpoch = schedule.getEndWork().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                Map<LocalDateTime, OceanEngineProcessBo> valueMap = buildKeyValueMap(
                        processData, List.of(startEpoch, endEpoch),
                        session.getStartTime().atZone(PerformanceCalculator.ZONE).toInstant().toEpochMilli());

                BasePerformanceEntity interval = computeInterval(processData, valueMap,
                        startEpoch, schedule.getStartWork(), endEpoch, schedule.getEndWork());
                if (interval == null) {
                    log.warn("[业绩计算] 计算排班业绩间隔数据失败, sessionId={}, startTime={}, endTime={}",
                            session.getId(), schedule.getStartWork(), schedule.getEndWork());
                    continue;
                }
                perf.setPerformanceCalculate(interval);
            }

            Long scheduleId = schedule.getScheduleId();
            SchedulePerformance existingSp = existingSpMap.get(scheduleId);
            Long spId;
            int source;
            if (existingSp == null) {
                SchedulePerformance sp = new SchedulePerformance();
                sp.setTenantId(tenantId);
                sp.setScheduleId(scheduleId);
                sp.setLiveRoomId(session.getLiveRoomId());
                sp.setStartTime(overlapStart);
                sp.setEndTime(overlapEnd);
                sp.setSecUid(secUid);
                sp.setSource(DataSource.SYSTEM.getCode());
                sp.setPerformance(perf);
                sp.setCompanyId(session.getCompanyId());
                sp.setDeptId(session.getDeptId());
                sp.setTeamId(session.getTeamId());
                sp.setCreateDate(LocalDateTime.now());
                sp.setUpdateDate(LocalDateTime.now());
                sp.setIsDeleted(false);
                schedulePerformanceMapper.insert(sp);
                spId = sp.getId();
                source = DataSource.SYSTEM.getCode();
            } else {
                spId = existingSp.getId();
                source = existingSp.getSource();
            }

            spContexts.add(new ScheduleProcessContext(spId, schedule.getScheduleId(), schedule.getWorkUserList(),
                    overlapStart, overlapEnd, perf, source));
        }
        return spContexts;
    }

    /**
     * 第二遍：保存贡献明细并收集新人员
     */
    private List<SchedulePerformanceStaff> saveDetailsAndCollectStaff(
            Long tenantId, LiveSession session,
            List<ScheduleProcessContext> spContexts, Map<Long, String> positionNameMap) {

        Set<Long> allSpIds = spContexts.stream().map(c -> c.spId).collect(Collectors.toSet());
        Map<String, SchedulePerformanceDetail> existingDetailByKey = schedulePerformanceDetailMapper.selectList(
                new LambdaQueryWrapper<SchedulePerformanceDetail>()
                        .eq(SchedulePerformanceDetail::getTenantId, tenantId)
                        .in(SchedulePerformanceDetail::getSchedulePerformanceId, allSpIds))
                .stream()
                .collect(Collectors.toMap(
                        d -> d.getSchedulePerformanceId() + "_" + d.getSessionId(),
                        d -> d, (a, b) -> a));

        Map<String, SchedulePerformanceStaff> existingStaffByKey = schedulePerformanceStaffMapper.selectList(
                new LambdaQueryWrapper<SchedulePerformanceStaff>()
                        .eq(SchedulePerformanceStaff::getTenantId, tenantId)
                        .in(SchedulePerformanceStaff::getSchedulePerformanceId, allSpIds))
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getSchedulePerformanceId() + "_" + s.getEmployeeId(),
                        s -> s, (a, b) -> a));

        List<SchedulePerformanceStaff> newStaffs = new ArrayList<>();
        for (ScheduleProcessContext ctx : spContexts) {
            String detailKey = ctx.spId + "_" + session.getId();
            SchedulePerformanceDetail detail = existingDetailByKey.get(detailKey);
            if (detail == null) {
                detail = new SchedulePerformanceDetail();
                detail.setTenantId(tenantId);
                detail.setSchedulePerformanceId(ctx.spId);
                detail.setSessionId(session.getId());
                detail.setCreateDate(LocalDateTime.now());
            }
            detail.setStartTime(ctx.overlapStart);
            detail.setEndTime(ctx.overlapEnd);
            detail.setPerformance(ctx.perf);
            detail.setUpdateDate(LocalDateTime.now());
            detail.setIsDeleted(false);

            if (detail.getId() != null) {
                schedulePerformanceDetailMapper.updateById(detail);
            } else {
                schedulePerformanceDetailMapper.insert(detail);
            }

            if (ctx.workUsers != null) {
                for (RoomScheduleBo.WorkUser wu : ctx.workUsers) {
                    if (wu.getEmployeeId() == null) {
                        continue;
                    }
                    String staffKey = ctx.spId + "_" + wu.getEmployeeId();
                    SchedulePerformanceStaff existingStaff = existingStaffByKey.get(staffKey);
                    if (existingStaff == null) {
                        SchedulePerformanceStaff staff = new SchedulePerformanceStaff();
                        staff.setTenantId(tenantId);
                        staff.setSchedulePerformanceId(ctx.spId);
                        staff.setEmployeeId(wu.getEmployeeId());
                        staff.setEmployeeName(wu.getEmployeeName());
                        staff.setPositionId(wu.getPositionId() != null ? wu.getPositionId() : 0L);
                        staff.setPositionName(positionNameMap.getOrDefault(wu.getPositionId(), null));
                        staff.setPositionCode(wu.getPositionCode());
                        staff.setIsScheduleStaff(true);
                        staff.setCreateDate(LocalDateTime.now());
                        staff.setUpdateDate(LocalDateTime.now());
                        staff.setIsDeleted(false);
                        newStaffs.add(staff);
                        existingStaffByKey.put(staffKey, staff);
                    } else if (!Boolean.TRUE.equals(existingStaff.getIsScheduleStaff())) {
                        existingStaff.setIsScheduleStaff(true);
                        existingStaff.setUpdateDate(LocalDateTime.now());
                        schedulePerformanceStaffMapper.updateById(existingStaff);
                    }
                }
            }
        }
        return newStaffs;
    }

    /**
     * 汇总回写排班业绩主表
     */
    private void aggregateAllSps(Long tenantId, Set<Long> allSpIds, List<ScheduleProcessContext> spContexts) {
        for (Long spId : allSpIds) {
            ScheduleProcessContext ctx = spContexts.stream()
                    .filter(c -> c.spId.equals(spId)).findFirst().orElse(null);
            if (ctx == null) {
                continue;
            }
            if (ctx.source == DataSource.SYSTEM.getCode()) {
                aggregateAndUpdateSchedulePerformance(tenantId, spId);
            } else {
                SchedulePerformanceDetail[] aggregated = aggregateDetails(tenantId, spId);
                if (aggregated.length > 0) {
                    managerPerformanceProcessor.saveOriginalValueForPerformance(
                            tenantId, spId,
                            SessionOriginalValue.SOURCE_TYPE_SCHEDULE_PERFORMANCE,
                            aggregated[0]);
                }
            }
        }
    }

    /**
     * 排班处理上下文（内部使用）
     */
    private static class ScheduleProcessContext {
        final Long spId;
        final Long scheduleId;
        final List<RoomScheduleBo.WorkUser> workUsers;
        final LocalDateTime overlapStart;
        final LocalDateTime overlapEnd;
        final BasePerformanceEntity perf;
        final int source;

        ScheduleProcessContext(Long spId, Long scheduleId, List<RoomScheduleBo.WorkUser> workUsers,
                              LocalDateTime overlapStart, LocalDateTime overlapEnd,
                              BasePerformanceEntity perf, int source) {
            this.spId = spId;
            this.scheduleId = scheduleId;
            this.workUsers = workUsers;
            this.overlapStart = overlapStart;
            this.overlapEnd = overlapEnd;
            this.perf = perf;
            this.source = source;
        }
    }

    /**
     * 查询指定排班业绩下的所有贡献明细
     */
    private SchedulePerformanceDetail[] aggregateDetails(Long tenantId, Long schedulePerformanceId) {
        List<SchedulePerformanceDetail> details = schedulePerformanceDetailMapper.selectList(
                new LambdaQueryWrapper<SchedulePerformanceDetail>()
                        .eq(SchedulePerformanceDetail::getTenantId, tenantId)
                        .eq(SchedulePerformanceDetail::getSchedulePerformanceId, schedulePerformanceId));
        if (details == null || details.isEmpty()) {
            return new SchedulePerformanceDetail[0];
        }

        // 汇总为一条虚拟记录
        SchedulePerformanceDetail sum = new SchedulePerformanceDetail();
        int totalViewCount = 0;
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalInvestment = BigDecimal.ZERO;
        int totalExposureCount = 0;
        int totalFollowCount = 0;
        int totalRefundQuantity = 0;
        int totalPayComboCnt = 0;
        Integer overallMaxOnline = null;
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;

        for (SchedulePerformanceDetail d : details) {
            if (d.getViewCount() != null) totalViewCount += d.getViewCount();
            if (d.getSalesRevenue() != null) totalSales = totalSales.add(d.getSalesRevenue());
            if (d.getRefund() != null) totalRefund = totalRefund.add(d.getRefund());
            if (d.getInvestment() != null) totalInvestment = totalInvestment.add(d.getInvestment());
            if (d.getExposureCount() != null) totalExposureCount += d.getExposureCount();
            if (d.getFollowCount() != null) totalFollowCount += d.getFollowCount();
            if (d.getRefundQuantity() != null) totalRefundQuantity += d.getRefundQuantity();
            if (d.getPayComboCnt() != null) totalPayComboCnt += d.getPayComboCnt();
            if (d.getMaxOnline() != null) {
                overallMaxOnline = overallMaxOnline == null ? d.getMaxOnline() : Math.max(overallMaxOnline, d.getMaxOnline());
            }
            if (d.getStartTime() != null && (minStart == null || d.getStartTime().isBefore(minStart))) {
                minStart = d.getStartTime();
            }
            if (d.getEndTime() != null && (maxEnd == null || d.getEndTime().isAfter(maxEnd))) {
                maxEnd = d.getEndTime();
            }
        }

        sum.setStartTime(minStart);
        sum.setEndTime(maxEnd);
        sum.setViewCount(totalViewCount);
        sum.setSalesRevenue(totalSales);
        sum.setRefund(totalRefund);
        sum.setInvestment(totalInvestment);
        sum.setExposureCount(totalExposureCount);
        sum.setFollowCount(totalFollowCount);
        sum.setRefundQuantity(totalRefundQuantity);
        sum.setPayComboCnt(totalPayComboCnt);
        sum.setMaxOnline(overallMaxOnline);

        // 统一重算衍生指标（net_sales = sales_revenue - refund、refund_rate、roi、thousand_sales、conversion_rate、uv_value、follow_rate）
        sum.calculateDerivedMetrics();

        // 点击-成交率和互动率取最后一条明细的值（不在 calculateDerivedMetrics 重算范围内）
        SchedulePerformanceDetail lastDetail = details.get(details.size() - 1);
        sum.setClickPaymentRate(lastDetail.getClickPaymentRate());
        sum.setInteractionRate(lastDetail.getInteractionRate());

        return new SchedulePerformanceDetail[]{sum};
    }

    /**
     * 汇总贡献明细并回写排班业绩主表
     */
    private void aggregateAndUpdateSchedulePerformance(Long tenantId, Long schedulePerformanceId) {
        SchedulePerformanceDetail[] aggregated = aggregateDetails(tenantId, schedulePerformanceId);
        if (aggregated.length == 0) {
            return;
        }
        SchedulePerformanceDetail sum = aggregated[0];

        SchedulePerformance sp = schedulePerformanceMapper.selectById(schedulePerformanceId);
        if (sp == null) {
            return;
        }
        sp.setStartTime(sum.getStartTime());
        sp.setEndTime(sum.getEndTime());
        sp.setPerformance(sum);
        sp.setUpdateDate(LocalDateTime.now());
        schedulePerformanceMapper.updateById(sp);
    }

}
