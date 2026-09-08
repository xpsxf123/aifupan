package com.jiuyu.replay.activity.bll;

import com.jiuyu.replay.activity.constant.ActivityProperties;
import com.jiuyu.replay.activity.constant.Constant;
import com.jiuyu.replay.activity.rse.ClientInviteActivityRse;
import com.jiuyu.replay.activity.rse.ClientInviteProgressRewardRse;
import com.jiuyu.replay.activity.rse.ClientInviteProgressRse;
import com.jiuyu.replay.common.constant.packageunit.CommodityTypeConvert;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteActivityListBo;
import com.jiuyu.replay.generic.bo.activity.ClientInviteProgressBo;
import com.jiuyu.replay.generic.feign.order.CommodityTypeFeign;
import com.jiuyu.replay.generic.feign.order.PackageFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.activity.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.CommodityPriceVo;
import com.jiuyu.replay.generic.vo.order.CommodityTypeInfoVo;
import com.jiuyu.replay.generic.vo.order.PackageInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 邀请活动
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-24 16:10:28
 */
@Component
public class ClientInviteActivityBll {

    @Resource
    private ActivityProperties activityProperties;
    @Resource
    private ClientInviteActivityRse clientInviteActivityRse;
    @Resource
    private ClientInviteProgressRse clientInviteProgressRse;
    @Resource
    private CommodityTypeFeign commodityTypeFeign;
    @Resource
    private PackageFeign packageFeign;
    @Resource
    private ClientInviteProgressRewardRse clientInviteProgressRewardRse;

    /**
     * 客户端获取邀请活动信息
     * @return
     */
    public R<ClientInviteActivityInfoVo> infoByClient() {

        ClientInviteActivityInfoVo inviteActivityInfoVo = clientInviteActivityRse.infoByActivate(activityProperties.getClientDefaultInviteActivityId());

        if(inviteActivityInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "未配置邀请活动");
        }

        List<ClientInviteProgressInfoVo> progressInfoVos = clientInviteProgressRse.getInviteProgressAndReward(inviteActivityInfoVo.getId(), 1);
        if(progressInfoVos != null && progressInfoVos.size() > 0) {

            // 获取所有商品类型
            R<List<CommodityTypeInfoVo>> commodityTypeListR =  commodityTypeFeign.listAll();
            List<CommodityTypeInfoVo> commodityTypeList = commodityTypeListR.getData();

            // 获取所有版本套餐
            R<List<PackageInfoVo>> packageListR = packageFeign.listAll(1);
            List<PackageInfoVo> packageList = packageListR.getData();

            for (ClientInviteProgressInfoVo progressInfoVo : progressInfoVos) {
                List<ClientInviteProgressRewardInfoVo> rewardList = progressInfoVo.getRewardList();
                if(rewardList != null && rewardList.size() > 0) {
                    for (ClientInviteProgressRewardInfoVo rewardInfoVo : rewardList) {
                        if(rewardInfoVo.getRewardType() == 0) {
                            // 奖励类型是版本
                            if(packageList != null && packageList.size() > 0) {
                                for (PackageInfoVo packageInfoVo : packageList) {
                                    if(packageInfoVo.getId().equals(rewardInfoVo.getPackageId())) {
                                        // 设置版本名称
                                        rewardInfoVo.setPackageName(packageInfoVo.getName());
                                        // 设置版本价格和时长
                                        List<CommodityPriceVo> commodityPriceList = packageInfoVo.getCommodityPriceList();
                                        if(commodityPriceList != null && commodityPriceList.size() > 0) {
                                            for (CommodityPriceVo commodityPriceVo : commodityPriceList) {
                                                if(commodityPriceVo.getId().equals(rewardInfoVo.getPackagePriceId())) {
                                                    rewardInfoVo.setPackagePrice(commodityPriceVo.getRealPrice());
                                                    rewardInfoVo.setPackageDuration(commodityPriceVo.getValidityNum() + getUnitStr(commodityPriceVo.getValidityUnit()));
                                                    break;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }else if(rewardInfoVo.getRewardType() == 1) {
                            // 奖励类型是增量包
                            rewardInfoVo.setCommodityTypeValidity(rewardInfoVo.getValidityNum() + getUnitStr(rewardInfoVo.getValidityUnit()));
                            if(commodityTypeList != null && commodityTypeList.size() > 0) {
                                for (CommodityTypeInfoVo commodityTypeVo : commodityTypeList) {
                                    if(commodityTypeVo.getId().equals(rewardInfoVo.getCommodityTypeId())) {
                                        // 设置增量包名称
                                        rewardInfoVo.setCommodityTypeName(commodityTypeVo.getName());
                                        rewardInfoVo.setCommodityTypeUnit(commodityTypeVo.getUnit());
                                        rewardInfoVo.setCommodityNumber(CommodityTypeConvert.commodityNumberConvert(commodityTypeVo.getCode(), rewardInfoVo.getCommodityNumber()));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        inviteActivityInfoVo.setProgressList(progressInfoVos);

        return R.ok(inviteActivityInfoVo);


    }

    /**
     * 返回有效期单位
     * @param unit 0：小时 1：天 2：月 3：季度 4：半年，5：年
     * @return
     */
    private String getUnitStr(Integer unit) {

        String[] arr = new String[]{"小时", "天", "月", "季度", "半年", "年"};

        return arr[unit];
    }

    /**
     * 根据活动id获取启用中的活动
     * @param id 活动id
     * @return
     */
    public R<ClientInviteActivityInfoVo> infoActivateById(Long id) {

        ClientInviteActivityInfoVo clientInviteActivityInfoVo = this.clientInviteActivityRse.infoByActivate(id);

        return R.ok(clientInviteActivityInfoVo);
    }

    /**
     * 邀请活动列表
     * @param clientInviteActivityListBo 邀请活动列表查询参数
     * @return
     */
    public R<PageUtils<ClientInviteActivityListVo>> queryPage(ClientInviteActivityListBo clientInviteActivityListBo) {

        return R.ok("获取成功", clientInviteActivityRse.queryPage(clientInviteActivityListBo));
    }

    /**
     * 邀请活动信息
     * @param id 邀请活动id
     * @param activityStatus 活动状态 0：未启用 1：启用中
     * @return
     */
    public R<ClientInviteActivityInfoVo> info(Long id, Integer activityStatus) {

        ClientInviteActivityInfoVo clientInviteActivityInfoVo = clientInviteActivityRse.info(id, activityStatus);
        if(clientInviteActivityInfoVo != null) {
            // 获取进度列表
            List<ClientInviteProgressInfoVo> progressList = this.clientInviteProgressRse.listByActivityIdAndStatus(clientInviteActivityInfoVo.getId(), 1);
            if(progressList != null && progressList.size() > 0) {
                // 获取进度奖励列表
                List<Long> progressIds = progressList.stream().map(ClientInviteProgressVo::getId).toList();
                List<ClientInviteProgressRewardInfoVo> progressRewardList = this.clientInviteProgressRewardRse.listByProgressIdsAndStatus(progressIds);
                if(progressRewardList != null && progressRewardList.size() > 0) {
                    Map<Long, List<ClientInviteProgressRewardInfoVo>> rewardMap = progressRewardList.stream().collect(Collectors.groupingBy(
                            ClientInviteProgressRewardVo::getProgressId,
                            Collectors.toList()
                    ));

                    for (ClientInviteProgressInfoVo progressInfoVo : progressList) {
                        List<ClientInviteProgressRewardInfoVo> progressRewardInfoVos = rewardMap.get(progressInfoVo.getId());
                        progressInfoVo.setRewardList(progressRewardInfoVos);
                    }
                }

                clientInviteActivityInfoVo.setProgressList(progressList);
            }
        }


        return R.ok("获取成功", clientInviteActivityInfoVo);
    }

    /**
     * 新增邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(ClientInviteActivityBo clientInviteActivityBo) {

        // 保存活动信息
        ClientInviteActivityInfoVo clientInviteActivityInfoVo = clientInviteActivityRse.save(clientInviteActivityBo);
        // 保存进度和奖励信息
        List<ClientInviteProgressBo> progressBoList = clientInviteActivityBo.getClientInviteProgressBoList();
        if(progressBoList != null && progressBoList.size() > 0) {
            for (ClientInviteProgressBo clientInviteProgressBo : progressBoList) {
                clientInviteProgressBo.setInviteActivityId(clientInviteActivityInfoVo.getId());
            }
            clientInviteActivityRse.saveBatch(clientInviteActivityBo.getClientInviteProgressBoList());
        }

        return R.ok("添加成功");
    }

    /**
     * 修改邀请活动
     * @param clientInviteActivityBo 邀请活动对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(ClientInviteActivityBo clientInviteActivityBo) {
        // 修改活动信息
        clientInviteActivityRse.update(clientInviteActivityBo);
        // 将旧的所有进度状态改为停用
        clientInviteActivityRse.disableOld(clientInviteActivityBo.getId());
        // 保存进度和奖励信息
        List<ClientInviteProgressBo> progressBoList = clientInviteActivityBo.getClientInviteProgressBoList();
        if(progressBoList != null && progressBoList.size() > 0) {
            for (ClientInviteProgressBo clientInviteProgressBo : progressBoList) {
                clientInviteProgressBo.setInviteActivityId(clientInviteActivityBo.getId());
            }
            clientInviteActivityRse.saveBatch(clientInviteActivityBo.getClientInviteProgressBoList());
        }

        return R.ok("修改成功");
    }

    /**
     * 删除邀请活动
     * @param id 邀请活动id
     * @return
     */
    public R<String> delete(Long id) {

        clientInviteActivityRse.deleteById(id);
        return R.ok("删除成功");
    }
}

