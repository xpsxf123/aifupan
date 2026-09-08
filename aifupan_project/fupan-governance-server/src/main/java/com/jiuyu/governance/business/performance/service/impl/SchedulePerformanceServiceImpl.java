package com.jiuyu.governance.business.performance.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.mapper.PositionMapper;
import com.jiuyu.governance.business.org.pojo.entity.Position;
import com.jiuyu.governance.business.performance.mapper.*;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceEntity;
import com.jiuyu.governance.business.performance.pojo.bo.DailyPerformanceStatsBO;
import com.jiuyu.governance.business.performance.pojo.bo.MultiPeriodSessionStatsBO;
import com.jiuyu.governance.business.performance.pojo.bo.PeriodPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.SchedulePerformanceBO;
import com.jiuyu.governance.business.performance.pojo.bo.StaffDailyStatsBO;
import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformanceDataRequest;
import com.jiuyu.governance.business.performance.pojo.response.client.ClientPerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.client.ClientSchedulePerformanceResponse;
import com.jiuyu.governance.business.performance.pojo.response.client.IndicatorResponse;
import com.jiuyu.governance.business.performance.utils.DataUtil;
import com.jiuyu.governance.business.performance.pojo.constants.DataSource;
import com.jiuyu.governance.business.performance.pojo.entity.SchedulePerformance;
import com.jiuyu.governance.business.performance.pojo.entity.SchedulePerformanceDetail;
import com.jiuyu.governance.business.performance.pojo.entity.SchedulePerformanceImage;
import com.jiuyu.governance.business.performance.pojo.entity.SchedulePerformanceStaff;
import com.jiuyu.governance.business.performance.pojo.entity.SessionOriginalValue;
import com.jiuyu.governance.business.performance.pojo.request.DailyPerformanceStatsRequest;
import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformancePageRequest;
import com.jiuyu.governance.business.performance.pojo.request.ScheduleListRequest;
import com.jiuyu.governance.business.performance.pojo.request.SchedulePerformanceSaveRequest;
import com.jiuyu.governance.business.performance.pojo.response.*;
import com.jiuyu.governance.business.performance.service.SchedulePerformanceService;
import com.jiuyu.governance.business.room.mapper.WorkScheduleMapper;
import com.jiuyu.governance.business.room.pojo.bo.RoomScheduleBo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.business.room.pojo.entity.WorkSchedule;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomResponse;
import com.jiuyu.governance.business.room.service.LiveRoomScheduleService;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.common.utils.ValueCompareUtils;
import com.jiuyu.governance.openfeign.replay.SystemService;
import com.jiuyu.governance.openfeign.replay.response.SystemKvResponse;
import com.jiuyu.governance.plugins.oss.storage.impl.ImagesStorageService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排班业绩服务实现类
 *
 * @author lj
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
public class SchedulePerformanceServiceImpl extends ServiceImpl<SchedulePerformanceMapper, SchedulePerformance>
        implements SchedulePerformanceService {

    private final SchedulePerformanceMapper schedulePerformanceMapper;
    private final SchedulePerformanceStaffMapper schedulePerformanceStaffMapper;
    private final SchedulePerformanceImageMapper schedulePerformanceImageMapper;
    private final SchedulePerformanceDetailMapper schedulePerformanceDetailMapper;
    private final SessionOriginalValueMapper sessionOriginalValueMapper;
    private final ImagesStorageService imagesStorageService;
    private final WorkScheduleMapper workScheduleMapper;
    private final ManagerPerformanceProcessor managerPerformanceProcessor;
    private final SessionPerformanceMapper sessionPerformanceMapper;
    private final SystemService systemService;
    private final PositionMapper positionMapper;
    @Autowired
    @Lazy
    private LiveRoomScheduleService liveRoomScheduleService;

    @Override
    public PageData<SchedulePerformanceResponse> pageQuerySchedulePerformance(SchedulePerformancePageRequest request) {
        Page<SchedulePerformanceBO> page = new Page<>(request.getPage(), request.getLimit());

        IPage<SchedulePerformanceBO> result = schedulePerformanceMapper.pageQuerySchedulePerformance(
                page,
                request.getTenantId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getAnchorName(),
                request.getLiveRoomId()
        );

        if (result.getRecords().isEmpty()) {
            return PageData.empty();
        }

        List<Long> performanceIds = result.getRecords().stream()
                .map(SchedulePerformanceBO::getId)
                .collect(Collectors.toList());

        // 提取唯一的直播间ID和日期
        Set<Long> liveRoomIds = result.getRecords().stream()
                .map(SchedulePerformanceBO::getLiveRoomId)
                .collect(Collectors.toSet());
        Set<LocalDate> dates = result.getRecords().stream()
                .map(bo -> bo.getStartTime().toLocalDate())
                .collect(Collectors.toSet());

        // 以天为维度批量查询全部人员
        List<StaffDailyStatsBO> staffByRoomAndDate =
                queryStaffByRoomAndDates(request.getTenantId(), new ArrayList<>(liveRoomIds), new ArrayList<>(dates));

        // 批量查询原始值
        Map<Long, SessionOriginalValue> ovMap = sessionOriginalValueMapper.selectList(
                new LambdaQueryWrapper<SessionOriginalValue>()
                        .in(SessionOriginalValue::getSourceId, performanceIds)
                        .eq(SessionOriginalValue::getSourceType, SessionOriginalValue.SOURCE_TYPE_SCHEDULE_PERFORMANCE)
                        .eq(SessionOriginalValue::getSource, 1))
                .stream()
                .collect(Collectors.toMap(SessionOriginalValue::getSourceId, ov -> ov, (a, b) -> a));

        // 组装响应
        List<SchedulePerformanceResponse> responses = result.getRecords().stream()
                .map(bo -> {

                    // 过滤出当前排班的人员：同直播间 + 时间有重叠，按employeeId去重
                    List<SchedulePerformanceResponse.StaffInfo> staffList = new ArrayList<>(staffByRoomAndDate.stream()
                            .filter(s -> bo.getLiveRoomId().equals(s.getLiveRoomId())
                                    && bo.getStartTime().isBefore(s.getEndTime())
                                    && bo.getEndTime().isAfter(s.getStartTime()))
                            .collect(Collectors.toMap(
                                    StaffDailyStatsBO::getEmployeeId,
                                    s -> SchedulePerformanceResponse.StaffInfo.builder()
                                            .positionId(s.getPositionId())
                                            .positionName(s.getPositionName())
                                            .employeeId(s.getEmployeeId())
                                            .employeeName(s.getEmployeeName())
                                            .build(),
                                    (a, b) -> a))
                            .values());

                    PerformanceData pd = buildPerformanceData(bo, null, ovMap.get(bo.getId()));
                    return SchedulePerformanceResponse.builder()
                            .id(bo.getId())
                            .liveRoomId(bo.getLiveRoomId())
                            .secUid(bo.getSecUid())
                            .startTime(bo.getStartTime())
                            .endTime(bo.getEndTime())
                            .source(bo.getSource())
                            .staffList(staffList)
                            .performanceData(pd)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageData<>(responses, result.getTotal(), request.getLimit(), request.getPage(), result.getPages());
    }

    /**
     * 按直播间和日期批量查询人员信息（一天内人员+岗位去重）
     *
     * @param tenantId   租户ID
     * @param liveRoomIds 直播间ID列表
     * @param dates  日期列表
     * @return 按 "liveRoomId_date" 分组的人员信息
     */
    private List<StaffDailyStatsBO> queryStaffByRoomAndDates(
            Long tenantId, List<Long> liveRoomIds, List<LocalDate> dates) {
        if (liveRoomIds.isEmpty() || dates.isEmpty()) {
            return List.of();
        }

        return schedulePerformanceMapper.selectStaffByRoomAndDates(
                tenantId, liveRoomIds, dates);
    }

    @Override
    public PageData<DailyPerformanceStatsResponse> pageQueryDailyStats(DailyPerformanceStatsRequest request) {
        // 构建分页对象
        Page<DailyPerformanceStatsBO> page = new Page<>(request.getPage(), request.getLimit());

        // 查询按天统计的主数据
        IPage<DailyPerformanceStatsBO> dailyStatsPage = schedulePerformanceMapper.selectDailyStats(
                page,
                request.getTenantId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getAnchorName(),
                request.getLiveRoomId(),
                request.getSortField(),
                request.getSortOrder()
        );

        if (dailyStatsPage.getRecords().isEmpty()) {
            return PageData.empty();
        }

        // 提取日期列表
        List<LocalDate> dateList = dailyStatsPage.getRecords().stream()
                .map(DailyPerformanceStatsBO::getStatsDate)
                .collect(Collectors.toList());

        // 查询人员统计信息
        Map<LocalDate, List<StaffDailyStats>> staffMap = queryStaffDailyStats(
                request.getTenantId(),
                request.getLiveRoomId(),
                request.getStartTime(),
                request.getEndTime(),
                dateList
        );

        // 组装响应数据
        List<DailyPerformanceStatsResponse> responseList = dailyStatsPage.getRecords().stream()
                .map(bo -> convertToResponse(bo, staffMap.getOrDefault(bo.getStatsDate(), Collections.emptyList())))
                .collect(Collectors.toList());

        return new PageData<>(responseList, dailyStatsPage.getTotal(), request.getLimit(), request.getPage(), dailyStatsPage.getPages());
    }

    /**
     * 查询人员每日统计信息
     *
     * @param tenantId   租户ID
     * @param liveRoomId 直播间ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param dateList   日期列表
     * @return 按日期分组的人员统计信息
     */
    private Map<LocalDate, List<StaffDailyStats>> queryStaffDailyStats(
            Long tenantId,
            Long liveRoomId,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            List<LocalDate> dateList) {

        if (dateList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<StaffDailyStatsBO> staffList = schedulePerformanceMapper.selectStaffDailyStats(
                tenantId,
                liveRoomId,
                startTime,
                endTime
        );

        return staffList.stream()
                .map(this::convertToStaffDailyStats)
                .collect(Collectors.groupingBy(StaffDailyStats::getStatsDate));
    }

    /**
     * 将 BO 转换为 StaffDailyStats
     */
    private StaffDailyStats convertToStaffDailyStats(StaffDailyStatsBO bo) {
        return StaffDailyStats.builder()
                .statsDate(bo.getStatsDate())
                .positionId(bo.getPositionId())
                .positionName(bo.getPositionName())
                .employeeId(bo.getEmployeeId())
                .employeeName(bo.getEmployeeName())
                .scheduleCount(bo.getScheduleCount())
                .build();
    }

    /**
     * 将 DailyPerformanceStatsBO 转换为 DailyPerformanceStatsResponse
     */
    private DailyPerformanceStatsResponse convertToResponse(DailyPerformanceStatsBO bo, List<StaffDailyStats> staffList) {
        return DailyPerformanceStatsResponse.builder()
                .statsDate(bo.getStatsDate())
                .scheduleCount(bo.getScheduleCount())
                .liveDurationMinutes(bo.getLiveDurationMinutes())
                .staffList(staffList)
                .viewCount(bo.getViewCount())
                .salesRevenue(bo.getSalesRevenue())
                .refund(bo.getRefund())
                .netSales(bo.getNetSales())
                .investment(bo.getInvestment())
                .roi(bo.getRoi())
                .build();
    }

    @Override
    public SchedulePerformanceDetailResponse getDetailById(Long id, Long tenantId) {
        // 1. 查询班次业绩主数据
        SchedulePerformance performance = schedulePerformanceMapper.selectOne(
                new LambdaQueryWrapper<SchedulePerformance>()
                        .eq(SchedulePerformance::getId, id)
                        .eq(SchedulePerformance::getIsDeleted, 0)
        );

        if (performance == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "业绩记录不存在");
        }

        // 2. 查询人员列表
        List<SchedulePerformanceStaff> staffEntityList = schedulePerformanceStaffMapper.selectList(
                new LambdaQueryWrapper<SchedulePerformanceStaff>()
                        .eq(SchedulePerformanceStaff::getSchedulePerformanceId, id)
                        .eq(SchedulePerformanceStaff::getIsDeleted, 0)
        );

        List<SchedulePerformanceResponse.StaffInfo> staffList = staffEntityList.stream()
                .map(staff -> SchedulePerformanceResponse.StaffInfo.builder()
                        .id(staff.getId())
                        .positionId(staff.getPositionId())
                        .positionName(staff.getPositionName())
                        .employeeId(staff.getEmployeeId())
                        .employeeName(staff.getEmployeeName())
                        .isScheduleStaff(staff.getIsScheduleStaff())
                        .build())
                .collect(Collectors.toList());

        // 3. 查询凭证图片
        SchedulePerformanceImage image = schedulePerformanceImageMapper.selectOne(
                new LambdaQueryWrapper<SchedulePerformanceImage>()
                        .eq(SchedulePerformanceImage::getId, id)
                        .eq(SchedulePerformanceImage::getIsDeleted, 0)
        );

        // 4. 查询原始值（仅当 source = MANUAL 时）
        SessionOriginalValue originalValue = null;
        if (performance.getSource() != null && performance.getSource() == DataSource.MANUAL.getCode()) {
            originalValue = sessionOriginalValueMapper.selectOne(
                    new LambdaQueryWrapper<SessionOriginalValue>()
                            .eq(SessionOriginalValue::getSourceId, id)
                            .eq(SessionOriginalValue::getSourceType, SessionOriginalValue.SOURCE_TYPE_SCHEDULE_PERFORMANCE)
                            .eq(SessionOriginalValue::getSource, 1)
                            .orderByDesc(SessionOriginalValue::getCreateDate)
                            .last("LIMIT 1")
            );
        }

        // 5. 构建业绩数据
        PerformanceData performanceData = buildPerformanceData(performance, image, originalValue);

        // 6. 组装响应
        return SchedulePerformanceDetailResponse.builder()
                .id(performance.getId())
                .scheduleId(performance.getScheduleId())
                .liveRoomId(performance.getLiveRoomId())
                .startTime(performance.getStartTime())
                .endTime(performance.getEndTime())
                .secUid(performance.getSecUid())
                .source(performance.getSource())
                .staffList(staffList)
                .performanceData(performanceData)
                .createDate(performance.getCreateDate())
                .updateDate(performance.getUpdateDate())
                .build();
    }

    /**
     * 构建业绩数据
     */
    public PerformanceData buildPerformanceData(
            BasePerformanceEntity performance,
            SchedulePerformanceImage image,
            SessionOriginalValue originalValue) {
        SchedulePerformanceImage imageNew = image != null ? image : new SchedulePerformanceImage();
        SessionOriginalValue  originalValueNew = originalValue != null ? originalValue : new SessionOriginalValue();
        return PerformanceData.builder()
                .viewCount(buildFieldDetail(performance.getViewCount(), imageNew.getViewCountImageUrl(), originalValueNew.getViewCount()))
                .salesRevenue(buildFieldDetail(performance.getSalesRevenue(), imageNew.getSalesRevenueImageUrl(), originalValueNew.getSalesRevenue()))
                .refund(buildFieldDetail(performance.getRefund(), imageNew.getRefundImageUrl(), originalValueNew.getRefund()))
                .investment(buildFieldDetail(performance.getInvestment(), imageNew.getInvestmentImageUrl(), originalValueNew.getInvestment()))
                .refundQuantity(buildFieldDetail(performance.getRefundQuantity(), imageNew.getRefundQuantityImageUrl(), originalValueNew.getRefundQuantity()))
                .payComboCnt(buildFieldDetail(performance.getPayComboCnt(), imageNew.getPayComboCntImageUrl(), originalValueNew.getPayComboCnt()))
                .netSales(buildFieldDetail(performance.getNetSales(), imageNew.getNetSalesImageUrl(), originalValueNew.getNetSales()))
                .roi(buildFieldDetail(performance.getRoi(), imageNew.getRoiImageUrl(), originalValueNew.getRoi()))
                .exposureCount(buildFieldDetail(performance.getExposureCount(), imageNew.getExposureCountImageUrl(), originalValueNew.getExposureCount()))
                .followCount(buildFieldDetail(performance.getFollowCount(), imageNew.getFollowCountImageUrl(), originalValueNew.getFollowCount()))
                .clickPaymentRate(buildFieldDetail(performance.getClickPaymentRate(), imageNew.getClickPaymentRateImageUrl(), originalValueNew.getClickPaymentRate()))
                .interactionRate(buildFieldDetail(performance.getInteractionRate(), imageNew.getInteractionRateImageUrl(), originalValueNew.getInteractionRate()))
                .maxOnline(buildFieldDetail(performance.getMaxOnline(), imageNew.getMaxOnlineImageUrl(), originalValueNew.getMaxOnline()))
                .refundRate(buildFieldDetail(performance.getRefundRate(), null, originalValueNew.getRefundRate()))
                .thousandSales(buildFieldDetail(performance.getThousandSales(), null, originalValueNew.getThousandSales()))
                .conversionRate(buildFieldDetail(performance.getConversionRate(), null, originalValueNew.getConversionRate()))
                .uvValue(buildFieldDetail(performance.getUvValue(), null, originalValueNew.getUvValue()))
                .followRate(buildFieldDetail(performance.getFollowRate(), null, originalValueNew.getFollowRate()))
                .build();
    }

    /**
     * 构建字段详情
     *
     * @param currentValue   当前值
     * @param imageUrl       图片ossKey
     * @param originalValue  原始值（可能为null）
     */
    public <T> FieldDetail<T> buildFieldDetail(T currentValue, String imageUrl, T originalValue) {
        // 判断是否修改：原始值为null表示未修改过，或者当前值与原始值相同也表示未修改
        boolean modified = false;
        if (originalValue != null) {
            modified = !ValueCompareUtils.isValueEqual(currentValue, originalValue);
        }

        return FieldDetail.<T>builder()
                .value(currentValue)
                .imageUrl(getFullUrl(imageUrl))
                .modified(modified)
                .build();
    }

    /**
     * 获取完整URL
     * 注意：ossKey本身已是完整地址，直接调用getPublicUrl返回
     */
    private String getFullUrl(String ossKey) {
        if (!StringUtils.hasText(ossKey)) {
            return null;
        }
        return imagesStorageService.getPublicUrl(ossKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSchedulePerformance(SchedulePerformanceSaveRequest request, Long tenantId, Long userId) {
        // 1. 校验并获取时间信息
        ScheduleTimeInfo timeInfo = validateAndGetTimeInfo(request);

        // 2. 判断新增还是修改
        Long performanceId = request.getId();
        SchedulePerformance originalPerformance = null;

        if (performanceId != null) {
            // 修改：查询原记录
            originalPerformance = schedulePerformanceMapper.selectOne(
                    new LambdaQueryWrapper<SchedulePerformance>()
                            .eq(SchedulePerformance::getId, performanceId)
                            .eq(SchedulePerformance::getIsDeleted, 0)
            );
            if (originalPerformance == null) {
                throw new BusinessException(SystemErrorCode.NOT_FOUND, "业绩记录不存在");
            }

            // 3. 如果原数据来源是系统，记录原始值
            if (originalPerformance.getSource() != null && originalPerformance.getSource() == DataSource.SYSTEM.getCode()) {
                managerPerformanceProcessor.saveOriginalValueForPerformance(tenantId, originalPerformance.getId(),
                        SessionOriginalValue.SOURCE_TYPE_SCHEDULE_PERFORMANCE, originalPerformance);
            }
        }

        // 4. 保存或更新主表
        SchedulePerformance performance = buildSchedulePerformance(request, performanceId, tenantId, userId,
                timeInfo.startTime, timeInfo.endTime, timeInfo.liveRoomId);

        if (performanceId == null) {
            // 新增
            schedulePerformanceMapper.insert(performance);
            performanceId = performance.getId();
        } else {
            // 更新
            performance.setId(performanceId);
            schedulePerformanceMapper.updateById(performance);
        }

        // 编辑时：如果 request 没传 scheduleId，保留原值
        if (performanceId != null && request.getScheduleId() == null && originalPerformance != null) {
            performance.setScheduleId(originalPerformance.getScheduleId());
        }

        // 5. 处理人员表
        saveStaffList(request.getStaffList(), performanceId, performance.getScheduleId(), tenantId, userId);

        // 6. 处理图片表
        savePerformanceImage(request.getPerformanceData(), performanceId, tenantId, userId);

        return performanceId;
    }

    @Override
    public PageData<LiveRoomPerformanceResponse> LiveRoomPerformanceData(PageData<LiveRoomResponse> result, long tenantId) {
        if (result == null) {
            return new PageData<>(List.of(), 0, 0, 0, 0);
        }

        // 转换数据
        PageData<LiveRoomPerformanceResponse> data = result.conversion(
                item -> BeanUtil.copyProperties(item, LiveRoomPerformanceResponse.class));

        if (result.getList() == null || result.getList().isEmpty()) {
            return data;
        }

        List<Long> liveRoomIds = data.getList().stream()
                .map(LiveRoomPerformanceResponse::getId)
                .toList();

        // 计算6个时段的日期范围
        DataUtil.PeriodDateRange range = DataUtil.calculatePeriodDateRange();

        // 批量查询6个时段的业绩数据（每个时段一次SQL，按liveRoomId分组）
        Map<Long, PeriodPerformanceBO> todayMap = queryAndGroupByLiveRoomId(tenantId, liveRoomIds, range.today(), range.today());
        Map<Long, PeriodPerformanceBO> yesterdayMap = queryAndGroupByLiveRoomId(tenantId, liveRoomIds, range.yesterday(), range.yesterday());
        Map<Long, PeriodPerformanceBO> thisWeekMap = queryAndGroupByLiveRoomId(tenantId, liveRoomIds, range.thisWeekStart(), range.today());
        Map<Long, PeriodPerformanceBO> lastWeekMap = queryAndGroupByLiveRoomId(tenantId, liveRoomIds, range.lastWeekStart(), range.lastWeekEnd());
        Map<Long, PeriodPerformanceBO> thisMonthMap = queryAndGroupByLiveRoomId(tenantId, liveRoomIds, range.thisMonthStart(), range.today());
        Map<Long, PeriodPerformanceBO> lastMonthMap = queryAndGroupByLiveRoomId(tenantId, liveRoomIds, range.lastMonthStart(), range.lastMonthEnd());

        // 批量查询所有直播间的场次数量和直播时长（一次SQL，按liveRoomId分组）
        List<MultiPeriodSessionStatsBO> sessionStatsList = schedulePerformanceMapper.queryPeriodSessionStatsByLiveRoomIds(
                tenantId, liveRoomIds,
                range.today(), range.yesterday(),
                range.thisWeekStart(), range.lastWeekStart(),
                range.lastWeekEnd(), range.thisMonthStart(),
                range.lastMonthStart(), range.lastMonthEnd()
        );
        Map<Long, MultiPeriodSessionStatsBO> sessionStatsMap = sessionStatsList.stream()
                .collect(Collectors.toMap(MultiPeriodSessionStatsBO::getLiveRoomId, bo -> bo, (a, b) -> a));

        // 组装每个直播间的业绩统计
        for (LiveRoomPerformanceResponse response : data.getList()) {
            Long liveRoomId = response.getId();

            PeriodPerformanceBO todayBo = todayMap.get(liveRoomId);
            PeriodPerformanceBO yesterdayBo = yesterdayMap.get(liveRoomId);
            PeriodPerformanceBO thisWeekBo = thisWeekMap.get(liveRoomId);
            PeriodPerformanceBO lastWeekBo = lastWeekMap.get(liveRoomId);
            PeriodPerformanceBO thisMonthBo = thisMonthMap.get(liveRoomId);
            PeriodPerformanceBO lastMonthBo = lastMonthMap.get(liveRoomId);

            MultiPeriodSessionStatsBO sessionStatsBO = sessionStatsMap.get(liveRoomId);
            if (sessionStatsBO == null) {
                sessionStatsBO = new MultiPeriodSessionStatsBO();
            }

            // 用 schedule 数据覆盖场次相关字段
            todayBo = mergeSessionStats(todayBo, sessionStatsBO.getTodaySessionCount(), sessionStatsBO.getTodayDuration());
            yesterdayBo = mergeSessionStats(yesterdayBo, sessionStatsBO.getYesterdaySessionCount(), sessionStatsBO.getYesterdayDuration());
            thisWeekBo = mergeSessionStats(thisWeekBo, sessionStatsBO.getThisWeekSessionCount(), sessionStatsBO.getThisWeekDuration());
            lastWeekBo = mergeSessionStats(lastWeekBo, sessionStatsBO.getLastWeekSessionCount(), sessionStatsBO.getLastWeekDuration());
            thisMonthBo = mergeSessionStats(thisMonthBo, sessionStatsBO.getThisMonthSessionCount(), sessionStatsBO.getThisMonthDuration());
            lastMonthBo = mergeSessionStats(lastMonthBo, sessionStatsBO.getLastMonthSessionCount(), sessionStatsBO.getLastMonthDuration());

            response.setPerformanceStatistics(DataUtil.buildPeriodStatsResponse(
                    todayBo, yesterdayBo, thisWeekBo, lastWeekBo, thisMonthBo, lastMonthBo));
        }

        return data;
    }

    /**
     * 按直播间ID批量查询业绩数据并分组
     *
     * @param tenantId    租户ID
     * @param liveRoomIds 直播间ID列表
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 按直播间ID分组的业绩数据
     */
    private Map<Long, PeriodPerformanceBO> queryAndGroupByLiveRoomId(Long tenantId, List<Long> liveRoomIds, LocalDate startDate, LocalDate endDate) {
        List<PeriodPerformanceBO> list = sessionPerformanceMapper.queryPeriodStatsByLiveRoomIds(tenantId, liveRoomIds, startDate, endDate);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream()
                .collect(Collectors.toMap(PeriodPerformanceBO::getLiveRoomId, bo -> bo, (a, b) -> a));
    }

    /**
     * 用 schedule 数据的场次数量和直播时长覆盖 session_performance 的对应字段
     */
    private PeriodPerformanceBO mergeSessionStats(PeriodPerformanceBO target, Integer sessionCount, Integer duration) {
        if (target == null) {
            target = new PeriodPerformanceBO();
        }
        target.setSessionCount(sessionCount);
        target.setDuration(duration);
        return target;
    }

    /**
     * 校验并获取时间信息
     * <p>
     * 校验规则：
     * 1. scheduleId有值时：从班次获取时间，检查班次是否已有业绩
     * 2. scheduleId为空时：使用前端时间，检查时间重叠
     * </p>
     *
     * @param request 保存请求
     * @return 时间信息（开始时间、结束时间、直播间ID）
     */
    private ScheduleTimeInfo validateAndGetTimeInfo(SchedulePerformanceSaveRequest request) {
        if (request.getScheduleId() != null) {
            // 情况A：关联班次
            return validateWithScheduleId(request);
        } else {
            // 情况B：手动录入，不关联班次
            return validateWithoutScheduleId(request);
        }
    }

    /**
     * 情况A：关联班次时的校验
     */
    private ScheduleTimeInfo validateWithScheduleId(SchedulePerformanceSaveRequest request) {
        // 查询班次
        WorkSchedule schedule = workScheduleMapper.selectById(request.getScheduleId());
        if (schedule == null || schedule.getIsDeleted()) {
            throw new BusinessException(BizErrorCode.HAS_DEPENDENCY, "关联的直播班次不存在");
        }

        // 从班次获取时间和直播间ID
        var workRange = schedule.workRange();
        LocalDateTime startTime = workRange.getStart();
        LocalDateTime endTime = workRange.getEnd();
        Long liveRoomId = schedule.getLiveRoomId();

        // 检查该班次是否已有业绩记录（排除当前记录）
        LambdaQueryWrapper<SchedulePerformance> wrapper = new LambdaQueryWrapper<SchedulePerformance>()
                .eq(SchedulePerformance::getScheduleId, request.getScheduleId())
                .eq(SchedulePerformance::getIsDeleted, 0);
        if (request.getId() != null) {
            wrapper.ne(SchedulePerformance::getId, request.getId());
        }
        SchedulePerformance existing = schedulePerformanceMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BusinessException(BizErrorCode.DATA_DUPLICATED, "该班次已有业绩记录，不能重复添加");
        }

        return new ScheduleTimeInfo(startTime, endTime, liveRoomId);
    }

    /**
     * 情况B：不关联班次时的校验
     */
    private ScheduleTimeInfo validateWithoutScheduleId(SchedulePerformanceSaveRequest request) {
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        Long liveRoomId = request.getLiveRoomId();

        // 校验时间逻辑：结束时间必须大于开始时间
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(BizErrorCode.PARAM_INVALID, "结束时间必须大于开始时间");
        }

        // 检查该直播间是否存在时间重叠的业绩（排除当前记录）
        // 重叠判断：(新startTime < 已有endTime) AND (新endTime > 已有startTime)
        LambdaQueryWrapper<SchedulePerformance> wrapper = new LambdaQueryWrapper<SchedulePerformance>()
                .eq(SchedulePerformance::getLiveRoomId, liveRoomId)
                .eq(SchedulePerformance::getIsDeleted, 0)
                .lt(SchedulePerformance::getStartTime, endTime)
                .gt(SchedulePerformance::getEndTime, startTime);
        if (request.getId() != null) {
            wrapper.ne(SchedulePerformance::getId, request.getId());
        }
        SchedulePerformance overlapping = schedulePerformanceMapper.selectOne(wrapper);
        if (overlapping != null) {
            throw new BusinessException(BizErrorCode.TIME_CONFLICT, "该时间段与已有业绩记录存在重叠");
        }

        return new ScheduleTimeInfo(startTime, endTime, liveRoomId);
    }

    /**
     * 时间信息内部类
     */
    private record ScheduleTimeInfo(LocalDateTime startTime, LocalDateTime endTime, Long liveRoomId) {
    }

    /**
     * 构建业绩实体
     */
    private SchedulePerformance buildSchedulePerformance(
            SchedulePerformanceSaveRequest request,
            Long performanceId,
            Long tenantId,
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long liveRoomId) {

        SchedulePerformance performance = new SchedulePerformance();
        if (performanceId != null) {
            performance.setId(performanceId);
        }
        performance.setTenantId(tenantId);
        performance.setScheduleId(request.getScheduleId());
        performance.setLiveRoomId(liveRoomId);
        performance.setStartTime(startTime);
        performance.setEndTime(endTime);
        performance.setSecUid(request.getSecUid());
        performance.setSource(DataSource.MANUAL.getCode());

        // 业绩数据
        if (request.getPerformanceData() != null) {
            SchedulePerformanceDataRequest data = request.getPerformanceData();
            if (data.getViewCount() != null) {
                performance.setViewCount(data.getViewCount().getValue());
            }
            if (data.getSalesRevenue() != null) {
                performance.setSalesRevenue(data.getSalesRevenue().getValue());
            }
            if (data.getRefund() != null) {
                performance.setRefund(data.getRefund().getValue());
            }
            if (data.getInvestment() != null) {
                performance.setInvestment(data.getInvestment().getValue());
            }
            if (data.getRefundQuantity() != null) {
                performance.setRefundQuantity(data.getRefundQuantity().getValue());
            }
            if (data.getPayComboCnt() != null) {
                performance.setPayComboCnt(data.getPayComboCnt().getValue());
            }
            if (data.getExposureCount() != null) {
                performance.setExposureCount(data.getExposureCount().getValue());
            }
            if (data.getFollowCount() != null) {
                performance.setFollowCount(data.getFollowCount().getValue());
            }
            if (data.getClickPaymentRate() != null) {
                performance.setClickPaymentRate(data.getClickPaymentRate().getValue());
            }
            if (data.getInteractionRate() != null) {
                performance.setInteractionRate(data.getInteractionRate().getValue());
            }
            if (data.getMaxOnline() != null) {
                performance.setMaxOnline(data.getMaxOnline().getValue());
            }
        }

        // 调用 setPerformanceCalculate 自动计算衍生指标（netSales, roi, refundRate, thousandSales）
        performance.setPerformanceCalculate(performance);

        LocalDateTime now = LocalDateTime.now();
        performance.setUpdateDate(now);
        performance.setUpdateBy(userId);
        
        if (performanceId == null) {
            // 新增时设置创建时间和创建人
            performance.setCreateDate(now);
            performance.setCreateBy(userId);
        }

        return performance;
    }

    /**
     * 保存人员列表，按 scheduleId 自动判断 isScheduleStaff
     *
     * @param staffList    人员列表
     * @param performanceId 排班业绩ID
     * @param scheduleId   排班ID，可为 null（null 时全员 isScheduleStaff = false）
     * @param tenantId     租户ID
     * @param userId       当前用户ID
     */
    private void saveStaffList(List<SchedulePerformanceResponse.StaffInfo> staffList,
                               Long performanceId, Long scheduleId,
                               Long tenantId, Long userId) {
        // 1. 如果有排班ID，查询排班中的员工ID集合
        Set<Long> scheduleEmployeeIds = Collections.emptySet();
        if (scheduleId != null) {
            List<RoomScheduleBo.WorkUser> scheduleEmployees =
                    liveRoomScheduleService.listScheduleEmployees(scheduleId);
            scheduleEmployeeIds = scheduleEmployees.stream()
                    .map(RoomScheduleBo.WorkUser::getEmployeeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        // 2. 删除原人员记录
        schedulePerformanceStaffMapper.delete(
                new LambdaQueryWrapper<SchedulePerformanceStaff>()
                        .eq(SchedulePerformanceStaff::getSchedulePerformanceId, performanceId));

        List<Long> positionIds = staffList.stream()
                .map(SchedulePerformanceResponse.StaffInfo::getPositionId)
                .filter(item -> ObjectUtil.isNotEmpty(item) && item != 0).toList();
        Map<Long, Position> positionMap = positionMapper.selectByIds(positionIds)
                .stream().collect(Collectors.toMap(Position::getId, Function.identity()));

        // 3. 批量插入新人员
        LocalDateTime now = LocalDateTime.now();
        List<SchedulePerformanceStaff> insertList = new java.util.ArrayList<>();
        for (SchedulePerformanceResponse.StaffInfo staff : staffList) {
            Position position = positionMap.get(staff.getPositionId());
            if (position == null) {
                continue;
            }
            SchedulePerformanceStaff staffEntity = BeanUtil.copyProperties(staff, SchedulePerformanceStaff.class);
            staffEntity.setSchedulePerformanceId(performanceId);
            staffEntity.setPositionCode(position.getPositionCode());
            staffEntity.setTenantId(tenantId);
            staffEntity.setCreateDate(now);
            staffEntity.setUpdateDate(now);
            staffEntity.setCreateBy(userId);
            staffEntity.setUpdateBy(userId);

            // 判断是否排班中的人员
            boolean isSchedule = staff.getEmployeeId() != null
                    && scheduleEmployeeIds.contains(staff.getEmployeeId());
            staffEntity.setIsScheduleStaff(isSchedule);

            insertList.add(staffEntity);
        }

        if (com.jiuyu.framework.util.EmptyUtil.isNotEmpty(insertList)) {
            schedulePerformanceStaffMapper.insert(insertList);
        }
    }

    /**
     * 保存凭证图片
     */
    private void savePerformanceImage(SchedulePerformanceDataRequest performanceData,
                                      Long performanceId, Long tenantId, Long userId) {
        if (performanceData == null) {
            return;
        }

        // 查询是否已存在图片记录
        SchedulePerformanceImage existingImage = schedulePerformanceImageMapper.selectOne(
                new LambdaQueryWrapper<SchedulePerformanceImage>()
                        .eq(SchedulePerformanceImage::getId, performanceId)
                        .eq(SchedulePerformanceImage::getIsDeleted, 0)
        );

        SchedulePerformanceImage image = existingImage != null ? existingImage : new SchedulePerformanceImage();
        image.setId(performanceId);
        image.setTenantId(tenantId);

        // 提取图片 ossKey（前端传入的 imageUrl 可能是完整 URL 或 ossKey）
        if (performanceData.getViewCount() != null && performanceData.getViewCount().getImageUrl() != null) {
            image.setViewCountImageUrl(extractOssKey(performanceData.getViewCount().getImageUrl()));
        }
        if (performanceData.getSalesRevenue() != null && performanceData.getSalesRevenue().getImageUrl() != null) {
            image.setSalesRevenueImageUrl(extractOssKey(performanceData.getSalesRevenue().getImageUrl()));
        }
        if (performanceData.getRefund() != null && performanceData.getRefund().getImageUrl() != null) {
            image.setRefundImageUrl(extractOssKey(performanceData.getRefund().getImageUrl()));
        }
        if (performanceData.getInvestment() != null && performanceData.getInvestment().getImageUrl() != null) {
            image.setInvestmentImageUrl(extractOssKey(performanceData.getInvestment().getImageUrl()));
        }
        if (performanceData.getRefundQuantity() != null && performanceData.getRefundQuantity().getImageUrl() != null) {
            image.setRefundQuantityImageUrl(extractOssKey(performanceData.getRefundQuantity().getImageUrl()));
        }
        if (performanceData.getPayComboCnt() != null && performanceData.getPayComboCnt().getImageUrl() != null) {
            image.setPayComboCntImageUrl(extractOssKey(performanceData.getPayComboCnt().getImageUrl()));
        }
        if (performanceData.getExposureCount() != null && performanceData.getExposureCount().getImageUrl() != null) {
            image.setExposureCountImageUrl(extractOssKey(performanceData.getExposureCount().getImageUrl()));
        }
        if (performanceData.getFollowCount() != null && performanceData.getFollowCount().getImageUrl() != null) {
            image.setFollowCountImageUrl(extractOssKey(performanceData.getFollowCount().getImageUrl()));
        }
        if (performanceData.getClickPaymentRate() != null && performanceData.getClickPaymentRate().getImageUrl() != null) {
            image.setClickPaymentRateImageUrl(extractOssKey(performanceData.getClickPaymentRate().getImageUrl()));
        }
        if (performanceData.getInteractionRate() != null && performanceData.getInteractionRate().getImageUrl() != null) {
            image.setInteractionRateImageUrl(extractOssKey(performanceData.getInteractionRate().getImageUrl()));
        }
        if (performanceData.getMaxOnline() != null && performanceData.getMaxOnline().getImageUrl() != null) {
            image.setMaxOnlineImageUrl(extractOssKey(performanceData.getMaxOnline().getImageUrl()));
        }

        LocalDateTime now = LocalDateTime.now();
        image.setUpdateDate(now);
        image.setUpdateBy(userId);

        if (existingImage != null) {
            schedulePerformanceImageMapper.updateById(image);
        } else {
            image.setCreateDate(now);
            image.setCreateBy(userId);
            schedulePerformanceImageMapper.insert(image);
        }
    }

    /**
     * 从完整 URL 中提取 ossKey
     * 如果 URL 以 https:// 或 http:// 开头，提取路径部分作为 ossKey
     * 如果不以 http 开头，直接返回原值（假设已经是 ossKey）
     */
    private String extractOssKey(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        // 如果是完整 URL，提取路径部分
        if (imageUrl.startsWith("https://") || imageUrl.startsWith("http://")) {
            int pathStart = imageUrl.indexOf("://");
            if (pathStart > 0) {
                int slashIndex = imageUrl.indexOf('/', pathStart + 3);
                if (slashIndex > 0) {
                    return imageUrl.substring(slashIndex + 1);
                }
            }
        }
        // 如果不是完整 URL，直接返回原值
        return imageUrl;
    }

    /**
     * 删除班次业绩（逻辑删除）
     *
     * @param id       班次业绩ID
     * @param tenantId 租户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedulePerformance(Long id, Long tenantId) {
        // 1. 校验记录是否存在
        SchedulePerformance performance = schedulePerformanceMapper.selectOne(
                new LambdaQueryWrapper<SchedulePerformance>()
                        .eq(SchedulePerformance::getId, id)
                        .eq(SchedulePerformance::getTenantId, tenantId)
                        .eq(SchedulePerformance::getIsDeleted, false)
        );
        if (performance == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "班次业绩记录不存在");
        }

        // 2. 删除主表
        performance.setIsDeleted(true);
        schedulePerformanceMapper.updateById(performance);

        // 3. 删除明细表
        schedulePerformanceDetailMapper.update(null,
                new LambdaUpdateWrapper<SchedulePerformanceDetail>()
                        .eq(SchedulePerformanceDetail::getSchedulePerformanceId, id)
                        .eq(SchedulePerformanceDetail::getTenantId, tenantId)
                        .set(SchedulePerformanceDetail::getIsDeleted, true)
        );

        // 4. 删除人员表
        schedulePerformanceStaffMapper.update(null,
                new LambdaUpdateWrapper<SchedulePerformanceStaff>()
                        .eq(SchedulePerformanceStaff::getSchedulePerformanceId, id)
                        .eq(SchedulePerformanceStaff::getTenantId, tenantId)
                        .set(SchedulePerformanceStaff::getIsDeleted, true)
        );

        // 5. 删除图片表
        schedulePerformanceImageMapper.update(null,
                new LambdaUpdateWrapper<SchedulePerformanceImage>()
                        .eq(SchedulePerformanceImage::getId, id)
                        .eq(SchedulePerformanceImage::getTenantId, tenantId)
                        .set(SchedulePerformanceImage::getIsDeleted, true)
        );
    }

    @Override
    public ClientSchedulePerformanceResponse queryBySecUidAndTimeRange(Long tenantId, String secUid,
                                                               LocalDateTime startTime, LocalDateTime endTime) {
        List<SchedulePerformance> performances = this.lambdaQuery()
                .eq(SchedulePerformance::getTenantId, tenantId)
                .eq(SchedulePerformance::getSecUid, secUid)
                .le(SchedulePerformance::getStartTime, endTime)
                .ge(SchedulePerformance::getEndTime, startTime)
                .orderByDesc(SchedulePerformance::getStartTime)
                .list();

        if (performances == null || performances.isEmpty()) {
            return null;
        }

        SchedulePerformance performance = mergeSchedulePerformances(performances);


        ClientSchedulePerformanceResponse result = BeanUtil.copyProperties(performance, ClientSchedulePerformanceResponse.class);

        ArrayList<ClientPerformanceResponse> clientPerformanceResponse = new ArrayList<>();

        ClientPerformanceResponse trafficData = new ClientPerformanceResponse(
                "流量数据",
                Arrays.asList(
                        new IndicatorResponse<>("曝光次数", performance.getExposureCount(), null),
                        new IndicatorResponse<>("观看人数", performance.getViewCount(), null),
                        new IndicatorResponse<>("最高在线", performance.getMaxOnline(), null),
                        new IndicatorResponse<>("涨粉人数", performance.getFollowCount(), null),
                        new IndicatorResponse<>("互动率", performance.getInteractionRate(), "%"),
                        new IndicatorResponse<>("涨粉率", performance.getFollowRate(), "%")
                ));

        ClientPerformanceResponse investmentData = new ClientPerformanceResponse(
                "投入回报",
                Arrays.asList(
                        new IndicatorResponse<>("投放金额", performance.getInvestment(), null),
                        new IndicatorResponse<>("ROI", performance.getRoi(), null)
                ));


        ClientPerformanceResponse salesData = new ClientPerformanceResponse(
                "销售数据",
                Arrays.asList(
                        new IndicatorResponse<>("成交单量", performance.getPayComboCnt(), null),
                        new IndicatorResponse<>("销售金额", performance.getSalesRevenue(), null),
                        new IndicatorResponse<>("带货转化率", performance.getConversionRate(), "%"),
                        new IndicatorResponse<>("uv价值", performance.getUvValue(), null),
                        new IndicatorResponse<>("点击-成交率", performance.getClickPaymentRate(), "%"),
                        new IndicatorResponse<>("千次成交", performance.getThousandSales(), null),
                        new IndicatorResponse<>("净销售额", performance.getNetSales(), null),
                        new IndicatorResponse<>("退款金额", performance.getRefund(), null),
                        new IndicatorResponse<>("退款单量", performance.getRefundQuantity(), null),
                        new IndicatorResponse<>("退款率", performance.getRefundRate(), "%")
                ));

        clientPerformanceResponse.add(trafficData);
        clientPerformanceResponse.add(investmentData);
        clientPerformanceResponse.add(salesData);
        result.setClientPerformanceResponse(clientPerformanceResponse);
        ApiResponse<SystemKvResponse> governanceFrontEndUrl = systemService.getByKey("governance_front_end_url");
        if (governanceFrontEndUrl.ok() && governanceFrontEndUrl.getData() != null && governanceFrontEndUrl.getData().getKvValue() != null){
            String kvValue = governanceFrontEndUrl.getData().getKvValue();
            result.setUpdateUrl(StrUtil.format(
                    "{}/live-room-performance/detail/{}?tagType=1&startTime={}&endTime={}",
                    kvValue, performance.getLiveRoomId(), DateUtil.format(performance.getStartTime(), "yyyy-MM-dd 00:00:00"), DateUtil.format(performance.getEndTime(), "yyyy-MM-dd 23:59:59")
            ));
        }

        return result;
    }

    /**
     * 合并多个排班业绩数据（时间重叠时求和）
     */
    private SchedulePerformance mergeSchedulePerformances(List<SchedulePerformance> list) {
        if (list.size() == 1) {
            return list.get(0);
        }

        SchedulePerformance merged = new SchedulePerformance();
        SchedulePerformance first = list.get(0);

        merged.setId(first.getId());
        merged.setTenantId(first.getTenantId());
        merged.setScheduleId(first.getScheduleId());
        merged.setLiveRoomId(first.getLiveRoomId());
        merged.setSecUid(first.getSecUid());
        merged.setSource(first.getSource());
        merged.setCompanyId(first.getCompanyId());
        merged.setDeptId(first.getDeptId());
        merged.setTeamId(first.getTeamId());
        merged.setCreateDate(first.getCreateDate());
        merged.setCreateBy(first.getCreateBy());
        merged.setIsDeleted(first.getIsDeleted());

        merged.setStartTime(list.stream()
                .map(SchedulePerformance::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null));
        merged.setEndTime(list.stream()
                .map(SchedulePerformance::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        merged.setUpdateDate(list.stream()
                .map(SchedulePerformance::getUpdateDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));

        merged.setViewCount(sumInt(list, SchedulePerformance::getViewCount));
        merged.setSalesRevenue(sumBigDecimal(list, SchedulePerformance::getSalesRevenue));
        merged.setRefund(sumBigDecimal(list, SchedulePerformance::getRefund));
        merged.setInvestment(sumBigDecimal(list, SchedulePerformance::getInvestment));
        merged.setRefundQuantity(sumInt(list, SchedulePerformance::getRefundQuantity));
        merged.setPayComboCnt(sumInt(list, SchedulePerformance::getPayComboCnt));
        merged.setExposureCount(sumInt(list, SchedulePerformance::getExposureCount));
        merged.setFollowCount(sumInt(list, SchedulePerformance::getFollowCount));
        merged.setMaxOnline(maxInt(list, SchedulePerformance::getMaxOnline));

        merged.calculateDerivedMetrics();
        return merged;
    }

    private Integer sumInt(List<SchedulePerformance> list, Function<SchedulePerformance, Integer> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).reduce(0, Integer::sum);
    }

    private BigDecimal sumBigDecimal(List<SchedulePerformance> list, Function<SchedulePerformance, BigDecimal> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer maxInt(List<SchedulePerformance> list, Function<SchedulePerformance, Integer> getter) {
        return list.stream().map(getter).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSchedulePerformanceStaff(List<SchedulePerformanceResponse.StaffInfo> staffInfoList, Long scheduleId, Long tenantId, Long userId) {

        if (ObjectUtil.isNull(staffInfoList) || ObjectUtil.isNull(scheduleId) || ObjectUtil.isNull(tenantId) || ObjectUtil.isNull(userId)) {
            return;
        }

        SchedulePerformance one = this.lambdaQuery()
                .eq(SchedulePerformance::getScheduleId, scheduleId)
                .eq(SchedulePerformance::getTenantId, tenantId)
                .last("limit 1")
                .one();

        if (one == null){
            return;
        }

        saveStaffList(staffInfoList, one.getId(), scheduleId, tenantId, userId);
    }

    @Override
    public List<ScheduleListItemResponse> queryScheduleList(ScheduleListRequest request, Long tenantId) {
        LocalDate date = request.getDate();
        String secUid = request.getSecUid();
        Long spId = request.getSchedulePerformanceId();

        // 2. 查询当天排班
        Collection<String> secUidList = Collections.singletonList(secUid);
        List<RoomScheduleBo> schedules = liveRoomScheduleService.getRangeTimeLiveSchedule(
                tenantId, LivePlatformType.getByValue(request.getPlatformType()), secUidList,
                date.atStartOfDay(), date.atTime(LocalTime.of(23, 59, 59)));

        if (schedules.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 批量查哪些排班已有业绩
        List<Long> scheduleIds = schedules.stream()
                .map(RoomScheduleBo::getScheduleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Long> scheduleIdToPerfId = schedulePerformanceMapper.selectList(
                        new LambdaQueryWrapper<SchedulePerformance>()
                                .select(SchedulePerformance::getId, SchedulePerformance::getScheduleId)
                                .eq(SchedulePerformance::getTenantId, tenantId)
                                .in(SchedulePerformance::getScheduleId, scheduleIds))
                .stream()
                .collect(Collectors.toMap(
                        SchedulePerformance::getScheduleId,
                        SchedulePerformance::getId, (a, b) -> a));

        // 4. 组装响应
        // 1. 确定当前编辑的排班ID
        Long currentScheduleId = null;
        if (spId != null) {
            SchedulePerformance sp = schedulePerformanceMapper.selectById(spId);
            if (sp != null) {
                currentScheduleId = sp.getScheduleId();
            }
        }
        Long currentScheduleId2 = currentScheduleId;
        return schedules.stream().map(s -> {
            boolean hasPerf = scheduleIdToPerfId.containsKey(s.getScheduleId());
            int hasPerformance;
            if (Objects.equals(s.getScheduleId(), currentScheduleId2)) {
                hasPerformance = 0;
            } else {
                hasPerformance = hasPerf ? 1 : 0;
            }

            List<ScheduleListItemResponse.WorkUserInfo> workUsers = Collections.emptyList();
            if (s.getWorkUserList() != null) {
                workUsers = s.getWorkUserList().stream()
                        .filter(wu -> wu.getEmployeeId() != null)
                        .map(wu -> ScheduleListItemResponse.WorkUserInfo.builder()
                                .employeeId(wu.getEmployeeId())
                                .employeeName(wu.getEmployeeName())
                                .positionId(wu.getPositionId())
                                .positionCode(wu.getPositionCode())
                                .build())
                        .toList();
            }

            return ScheduleListItemResponse.builder()
                    .scheduleId(s.getScheduleId())
                    .roomId(s.getRoomId())
                    .secUid(s.getSecUid())
                    .startTime(s.getStartWork())
                    .endTime(s.getEndWork())
                    .workUserList(workUsers)
                    .hasPerformance(hasPerformance)
                    .build();
        }).toList();
    }
}
