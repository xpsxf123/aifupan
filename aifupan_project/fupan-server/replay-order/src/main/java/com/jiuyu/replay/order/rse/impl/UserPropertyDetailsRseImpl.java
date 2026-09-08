package com.jiuyu.replay.order.rse.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.generic.dto.order.UserResourceConsumptionDto;
import com.jiuyu.replay.order.entity.UserPropertyDetailsEntity;
import com.jiuyu.replay.order.repository.service.UserPropertyDetailsService;
import com.jiuyu.replay.order.rse.UserPropertyDetailsRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户资产消费记录RSE实现
 *
 * @author AI Assistant
 */
@Service
public class UserPropertyDetailsRseImpl implements UserPropertyDetailsRse {

    @Resource
    private UserPropertyDetailsService userPropertyDetailsService;

    @Override
    public List<UserResourceConsumptionDto> getYesterdayResourceConsumption(List<Long> userIds, Long parentUserId) {
        if (ObjectUtil.isEmpty(userIds)) {
            return new ArrayList<>();
        }

        // 计算昨日的开始和结束时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterdayStart = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayEnd = yesterdayStart.withHour(23).withMinute(59).withSecond(59);

        return getUserResourceConsumptionByTimeRange(userIds, parentUserId, yesterdayStart, yesterdayEnd);
    }

    @Override
    public List<UserResourceConsumptionDto> getMonthlyResourceConsumption(List<Long> userIds, Long parentUserId) {
        if (ObjectUtil.isEmpty(userIds)) {
            return new ArrayList<>();
        }

        // 计算本月的开始和结束时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = now.withHour(23).withMinute(59).withSecond(59);

        return getUserResourceConsumptionByTimeRange(userIds, parentUserId, monthStart, monthEnd);
    }

    /**
     * 根据时间范围获取用户资源消耗统计
     *
     * @param userIds   用户ID列表
     * @param parentUserId 父用户id
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    private List<UserResourceConsumptionDto> getUserResourceConsumptionByTimeRange(List<Long> userIds, Long parentUserId, LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        // 查询昨日的资源消耗记录（只查询减少的记录，signs=0表示减）
        QueryWrapper<UserPropertyDetailsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds)
                .eq("parent_user_id", parentUserId)
                // 只统计消耗（减少）的记录
                .eq("signs", 0)
                .between("create_date", startTimeStr, endTimeStr);

        List<UserPropertyDetailsEntity> detailsList = userPropertyDetailsService.list(queryWrapper);

        if (ObjectUtil.isEmpty(detailsList)) {
            return new ArrayList<>();
        }

        // 按用户ID和商品类型分组统计消耗数量
        String remarks = "到资产清零时间";
        Map<String, UserResourceConsumptionDto> consumptionMap = detailsList.stream()
                .filter(item -> ObjectUtil.isEmpty(item.getRemarks()) || !item.getRemarks().startsWith(remarks))
                .collect(Collectors.groupingBy(
                        detail -> detail.getUserId() + "_" + detail.getCommodityTypeCode(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    UserPropertyDetailsEntity first = list.get(0);
                                    UserResourceConsumptionDto dto = new UserResourceConsumptionDto();
                                    dto.setUserId(first.getUserId());
                                    dto.setCommodityTypeId(first.getCommodityTypeId());
                                    dto.setCommodityTypeCode(first.getCommodityTypeCode());
                                    dto.setCommodityTypeName(first.getCommodityTypeName());
                                    dto.setCommodityTypeUnit(first.getCommodityTypeUnit());
                                    // 统计总消耗数量
                                    Long totalConsumption = list.stream()
                                            .mapToLong(UserPropertyDetailsEntity::getQuantity)
                                            .sum();
                                    dto.setConsumptionQuantity(commodityNumberConvert(dto.getCommodityTypeCode(), totalConsumption));
                                    return dto;
                                })));

        return new ArrayList<>(consumptionMap.values());
    }

    /**
     * 增量包数据转换
     *
     * @param code   商品类型code
     * @param number 商品数量
     * @return
     */
    private String commodityNumberConvert(String code, Long number) {
        if (StrUtil.isBlank(code)) {
            return number + "";
        }

        if (code.equals("aiAnalysisTime") || code.equals("textExtraction")||code.equals("textExtractionNum")) {
            // 奖励是分析时长，存储的值是分钟，需要转成小时

            // 指定保留1位小数
            DecimalFormat df = new DecimalFormat("#.#");
            df.setRoundingMode(java.math.RoundingMode.HALF_UP); // 设置舍入模式为四舍五入

            return df.format(number / 60.0);

        } else if (code.equals("storageNum")) {
            // 奖励是存储空间，存储的是KB，需要转成G

            // 指定保留2位小数
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(java.math.RoundingMode.HALF_UP); // 设置舍入模式为四舍五入

            return df.format(number / 1024.0 / 1024.0);
        }

        return number + "";
    }
}
