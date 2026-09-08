package com.jiuyu.replay.power.bll;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.power.SalesListBo;
import com.jiuyu.replay.generic.bo.power.statistics.SalesCardStatisticsDataBo;
import com.jiuyu.replay.generic.bo.power.statistics.SalesListStatisticsPageBo;
import com.jiuyu.replay.generic.bo.power.statistics.TeamCardStatisticsDataBo;
import com.jiuyu.replay.generic.feign.power.SalesFeign;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesListVo;
import com.jiuyu.replay.generic.vo.power.SalesVo;
import com.jiuyu.replay.generic.vo.power.statistics.*;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ：lujie
 * @description：销售统计接口
 * @date ：2026/1/30 09:44
 */
@Component
@AllArgsConstructor
public class SalesStatisticsBll {

    private final SalesFeign salesFeign;
    private final SystemKvProducer systemKvProducer;
    private final UserProducer userProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DictDataFeign dictDataFeign;


    public List<SalesListVo> selectSales() {
        // 判断是否有选择销售的
        SalesListBo bo = new SalesListBo();
        bo.setLimit(-1);
        bo.setSalesType(UserEnums.salesType.ADMIN.getCode());
        Long currentSalesUserId = changeSalesAdmin();
        if (currentSalesUserId != null) {
            bo.setUserId(currentSalesUserId);
        }
        PageUtils<SalesListVo> page = salesFeign.queryPage(bo);
        if (ObjectUtil.isEmpty(page) || ObjectUtil.isEmpty(page.getList())) {
            throw new BusinessException("当前用户无权限");
        }
        List<SalesListVo> list = page.getList();
        // 把userId等于登录UserId的数据排到最前面
        UserCacheVo user = GlobalObject.getLocalUser();
        list.sort((o1, o2) -> {
            if (ObjectUtil.equals(o1.getUserId(), user.getId())) {
                return -1;
            }
            if (ObjectUtil.equals(o2.getUserId(), user.getId())) {
                return 1;
            }
            return 0;
        });

        return list;
    }

    /**
     * 检查当前登录的用户是否是销售管理员
     *
     * @return 是就返回null，不是就返回当前的用户id
     */
    private Long changeSalesAdmin() {
        UserCacheVo user = GlobalObject.getLocalUser();
        // 判断是否有选择销售的
        String temp = "salesStatistics:selectSales";
        SystemKvInfoVo kv = systemKvProducer.getByKey("statistics_select_sales_apiCode");
        if (ObjectUtil.isNotEmpty(user) && kv != null && ObjectUtil.isNotEmpty(kv.getKvValue())) temp = kv.getKvValue();
        String code = temp;
        boolean flag = user.getMenuList().stream().anyMatch(item -> ObjectUtil.equals(item.getType(), 1) && ObjectUtil.equals(item.getUrl(), code));
        if (flag) {
            return null;
        }
        return user.getId();
    }

    /**
     * 判断是否有权限
     *
     * @return 是否有权限
     */
    private boolean isAdmin(Long userId) {
        Long tempUserId = changeSalesAdmin();
        return tempUserId == null || ObjectUtil.equals(tempUserId, userId);
    }

    /**
     * 判断当前用户是否有权限查看这个销售
     *
     * @param salesId 销售id
     */
    private void changeAuthority(Long salesId) {
        if (salesId == null) {
            throw new BusinessException("销售不能为空");
        }
        SalesInfoVo sales = salesFeign.getById(salesId);
        if (sales == null) {
            throw new BusinessException("没有这个销售");
        }
        if (!isAdmin(sales.getUserId())) {
            throw new BusinessException("当前用户无权限");
        }
    }

    public SalesCardStatisticsDataVo salesCardStatisticsData(SalesCardStatisticsDataBo salesCardStatisticsData) {

        // 判断权限
        changeAuthority(salesCardStatisticsData.getSalesId());
        SalesCardStatisticsDataVo res = new SalesCardStatisticsDataVo();
        String redisKey = StrUtil.format("replay:user:statistics:salesCard:{}", CommonUtils.objectToString(salesCardStatisticsData));

//        // 查询redis
//        SalesCardStatisticsDataVo res1 = CommonUtils.redisDataToObject(redisTemplate, redisKey, SalesCardStatisticsDataVo.class);
//        if (res1 != null) {
//            return res1;
//        }

        // 注册
        SalesCardStatisticsDataVo temp1 = userProducer.statisticsRegister(salesCardStatisticsData);
        res.setRegisterNum(temp1.getRegisterNum());
        res.setTotalRegisterNum(temp1.getTotalRegisterNum());

        // 试用
        SalesCardStatisticsDataVo temp2 = userProducer.statisticsTrial(salesCardStatisticsData);
        res.setTrialUserNum(temp2.getTrialUserNum());
        res.setTrialLogoUserNum(temp2.getTrialLogoUserNum());

        // 续费到期数
        SalesCardStatisticsDataVo temp3 = userProducer.statisticsRenewalExpires(salesCardStatisticsData);
        res.setRenewalExpiresNum(temp3.getRenewalExpiresNum());

        // 成交客户数
        SalesCardStatisticsDataVo temp4 = userProducer.statisticsDealCustomers(salesCardStatisticsData);
        res.setDealCustomersNum(temp4.getDealCustomersNum());

        redisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(res), 10, TimeUnit.MINUTES);

        return res;
    }

    public List<UserAmbitionStatisticsVo> salesEchartsStatisticsData(SalesCardStatisticsDataBo salesCardStatisticsData) {

        // 判断权限
        changeAuthority(salesCardStatisticsData.getSalesId());
        List<UserAmbitionStatisticsVo> res;
        String redisKey = StrUtil.format("replay:user:statistics:salesEcharts:{}", CommonUtils.objectToString(salesCardStatisticsData));

        // 查询redis
//        res = CommonUtils.redisDataToList(redisTemplate, redisKey, UserAmbitionStatisticsVo.class);
//        if (res != null) {
//            return res;
//        }

        // 用户购买意愿度分布图
        res = userProducer.salesEchartsStatisticsData(salesCardStatisticsData);

        redisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(res), 10, TimeUnit.MINUTES);
        return res;
    }

    /**
     * 3日内试用即将到期客户
     *
     * @param bo 参数
     * @return 结果
     */
    public PageUtils<SalesStatisticsList> salesTrialAboutTo3DayExpires(SalesListStatisticsPageBo bo) {

        // 校验排序字段
        changeSort(bo);

        // 判断权限
        changeAuthority(bo.getSalesId());

        PageUtils<SalesStatisticsList> res = userProducer.salesTrialAboutTo3DayExpires(bo);

        // 设置根据状态名称
        if (res != null && ObjectUtil.isNotEmpty(res.getList())) {
            // 获取状态名称
            setAccordingStatusName(res.getList());
        }

        return res;
    }

    /**
     * 客户数据看板-3日内待跟进客户
     *
     * @param bo 参数
     * @return 结果
     */
    public PageUtils<SalesStatisticsList> salesAboutTo3Day(SalesListStatisticsPageBo bo) {
        // 校验排序字段
        changeSort(bo);

        // 判断权限
        changeAuthority(bo.getSalesId());

        PageUtils<SalesStatisticsList> res = userProducer.salesAboutTo3Day(bo);

        // 设置根据状态名称
        if (res != null && ObjectUtil.isNotEmpty(res.getList())) {
            // 获取状态名称
            setAccordingStatusName(res.getList());
        }

        return res;
    }

    /**
     * 15日内试用即将续费客户
     *
     * @param bo 参数
     * @return 结果
     */
    public PageUtils<SalesStatisticsList> salesTrialAboutTo15DayRenewal(SalesListStatisticsPageBo bo) {

        // 校验排序字段
        changeSort(bo);

        // 判断权限
        changeAuthority(bo.getSalesId());

        PageUtils<SalesStatisticsList> res = userProducer.salesTrialAboutTo15DayRenewal(bo);

        // 设置根据状态名称
        if (res != null && ObjectUtil.isNotEmpty(res.getList())) {
            // 获取状态名称
            setAccordingStatusName(res.getList());
        }

        return res;
    }

    /**
     * 根据状态名称
     *
     * @param list 列表
     */
    private void setAccordingStatusName(List<SalesStatisticsList> list) {
        if (ObjectUtil.isEmpty(list)) {
            return;
        }

        // 获取状态名称
        Map<String, String> dictMap = dictDataFeign.dictDataParentLabelByCode("system_according_status").stream()
                .collect(Collectors.toMap(DictDataListVo::getValue, DictDataListVo::getLabel, (oldValue, newValue) -> oldValue));
        if (ObjectUtil.isEmpty(dictMap)) {
            return;
        }
        list.forEach(item -> {
            // 设置根据状态名称
            if (ObjectUtil.isNotEmpty(item.getAccordingStatus())) {
                item.setAccordingStatusName(dictMap.get(item.getAccordingStatus()));
            }
            // 手机号脱敏
            if (ObjectUtil.isNotEmpty(item.getPhone())) {
                item.setPhone(CommonUtils.maskPhone(item.getPhone()));
            }
        });
    }

    /**
     * 校验排序字段
     *
     * @param bo 参数
     */
    private void changeSort(SalesListStatisticsPageBo bo) {
        bo.setSortOrder(changeSort(bo.getSortOrder()));
    }

    private void changeSort(TeamCardStatisticsDataBo bo) {
        bo.setSortOrder(changeSort(bo.getSortOrder()));
    }


    private String changeSort(String sortOrder) {
        if (ObjectUtil.isEmpty(sortOrder)) {
            sortOrder = "asc";
        }

        if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
            throw new BusinessException("排序类型未知");
        }
        return sortOrder;
    }

    public SalesCardStatisticsDataVo teamCardStatisticsData(TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        String redisKey = StrUtil.format("replay:user:statistics:teamCard:{}", CommonUtils.objectToString(teamCardStatisticsDataBo));

        // 查询redis
        SalesCardStatisticsDataVo res1 = CommonUtils.redisDataToObject(redisTemplate, redisKey, SalesCardStatisticsDataVo.class);
        if (res1 != null) {
            return res1;
        }

        SalesCardStatisticsDataVo res = new SalesCardStatisticsDataVo();

        // 注册
        SalesCardStatisticsDataVo temp1 = userProducer.statisticsRegisterTeam(teamCardStatisticsDataBo);
//        res.setRegisterNum(temp1.getRegisterNum());
        res.setTotalRegisterNum(temp1.getTotalRegisterNum());

        // 试用
        SalesCardStatisticsDataVo temp2 = userProducer.statisticsTrialTeam(teamCardStatisticsDataBo);
        res.setTrialUserNum(temp2.getTrialUserNum());
        res.setTrialLogoUserNum(temp2.getTrialLogoUserNum());

        // 续费到期数
        TeamCardStatisticsDataBo bo = new TeamCardStatisticsDataBo();
        bo.setStartDate(DateUtil.formatDateTime(new Date()));
        bo.setEndDate(DateUtil.formatDate(DateUtil.offsetDay(new Date(), 30)) + " 23:59:59");
        bo.setDeptId(teamCardStatisticsDataBo.getDeptId());
        SalesCardStatisticsDataVo temp3 = userProducer.statisticsRenewalExpiresTeam(bo);
        res.setRenewalExpiresNum(temp3.getRenewalExpiresNum());

        // 成交客户数
        SalesCardStatisticsDataVo temp4 = userProducer.statisticsDealCustomersTeam(teamCardStatisticsDataBo);
        res.setDealCustomersNum(temp4.getDealCustomersNum());

        redisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(res), 10, TimeUnit.MINUTES);

        return res;
    }

    /**
     * 团队数据看板-用户购买意愿度分布图
     *
     * @param teamCardStatisticsDataBo 参数
     * @return 结果
     */
    public List<TeamEchartsStatisticsVo> teamEchartsStatisticsData(TeamCardStatisticsDataBo teamCardStatisticsDataBo) {
        List<TeamEchartsStatisticsVo> list = userProducer.teamEchartsStatisticsData(teamCardStatisticsDataBo);
        if (ObjectUtil.isEmpty(list)) {
            return list;
        }

        List<Long> userIds = list.stream().map(TeamEchartsStatisticsVo::getUserId).distinct().toList();
        Map<Long, SalesInfoVo> salesMap = salesFeign.listByUserIds(userIds).stream().collect(Collectors.toMap(SalesVo::getUserId, Function.identity(), (a, b) -> a));
        if (ObjectUtil.isEmpty(salesMap)) {
            return list;
        }

        list.forEach(item -> {

            // 设置销售名称和id
            if (ObjectUtil.isNotEmpty(item.getUserId())) {
                SalesInfoVo vo = salesMap.get(item.getUserId());
                if (ObjectUtil.isNotEmpty(vo)) {
                    item.setSalesId(vo.getId());
                    item.setSalesName(vo.getSalesName());
                }
            }
        });

        list = list.stream().filter(item -> ObjectUtil.isNotEmpty(item.getSalesId())).toList();
        return list;
    }

    /**
     * 团队数据看板-3日内试用即将到期客户统计
     *
     * @param bo 参数
     * @return 结果
     */
    public Map<String, Integer> teamTrialAboutTo3DayExpiresStatistics(TeamCardStatisticsDataBo bo) {
        return userProducer.teamTrialAboutTo3DayExpiresStatistics(bo);
    }

    /**
     * 3日内试用即将到期客户
     *
     * @param bo 参数
     * @return 结果
     */
    public PageUtils<SalesStatisticsList> teamTrialAboutTo3DayExpires(TeamCardStatisticsDataBo bo) {

        // 校验排序字段
        changeSort(bo);

        PageUtils<SalesStatisticsList> res = userProducer.teamTrialAboutTo3DayExpires(bo);

        // 设置根据状态名称
        if (res != null && ObjectUtil.isNotEmpty(res.getList())) {
            // 获取状态名称
            setAccordingStatusName(res.getList());
        }

        return res;
    }

    /**
     * 团队数据看板-销售跟进统计
     *
     * @param bo 参数
     * @return 结果
     */
    public List<EachSalesFollowStatisticsVo> teamSalesFollowList(TeamCardStatisticsDataBo bo) {
        return userProducer.teamSalesFollowList(bo);
    }
}
