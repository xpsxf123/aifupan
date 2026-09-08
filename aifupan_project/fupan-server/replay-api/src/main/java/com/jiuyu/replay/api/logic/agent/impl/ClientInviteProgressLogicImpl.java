package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.ClientInviteProgressBll;
import com.jiuyu.replay.agent.bo.ClientInviteProgressBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressListBo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteProgressLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.CommodityTypeBll;
import com.jiuyu.replay.order.bll.PackageBll;
import jakarta.annotation.Resource;

import java.util.List;


/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@Service
public class ClientInviteProgressLogicImpl implements ClientInviteProgressLogic {

    @Resource
    private ClientInviteProgressBll clientInviteProgressBll;
    @Resource
    private CommodityTypeBll commodityTypeBll;
    @Resource
    private PackageBll packageBll;


    @Override
    public R<PageUtils<ClientInviteProgressListVo>> queryPage(ClientInviteProgressListBo clientInviteProgressListBo) {

        return clientInviteProgressBll.queryPage(clientInviteProgressListBo);
    }

    @Override
    public R<ClientInviteProgressInfoVo> info(Long id) {

        return clientInviteProgressBll.info(id);
    }

    @Override
    public R<String> save(ClientInviteProgressBo clientInviteProgressBo) {

        return clientInviteProgressBll.save(clientInviteProgressBo);
    }

    @Override
    public R<String> update(ClientInviteProgressBo clientInviteProgressBo) {

        return clientInviteProgressBll.update(clientInviteProgressBo);
    }

    @Override
    public R<String> delete(Long id) {

        return clientInviteProgressBll.delete(id);
    }

    @Override
    public R<String> setInviteProgressAndReward(List<ClientInviteProgressBo> clientInviteProgressBoList) {

        return clientInviteProgressBll.setInviteProgressAndReward(clientInviteProgressBoList);
    }

    @Override
    public R<List<ClientInviteProgressInfoVo>> getInviteProgressAndReward() {

//        return clientInviteProgressBll.getInviteProgressAndReward(null);
        return R.ok();
    }

    @Override
    public R<List<ClientInviteProgressInfoVo>> clientGetInviteProgressAndReward() {

//        R<List<ClientInviteProgressInfoVo>> clientInviteProgressInfoVosR = clientInviteProgressBll.getInviteProgressAndReward(null, 0);
//        List<ClientInviteProgressInfoVo> progressInfoVos = clientInviteProgressInfoVosR.getData();
//        if(progressInfoVos != null && progressInfoVos.size() > 0) {
//
//            // 获取所有商品类型
//            R<List<CommodityTypeInfoVo>> commodityTypeListR =  commodityTypeBll.listAll();
//            List<CommodityTypeInfoVo> commodityTypeList = commodityTypeListR.getData();
//
//            // 获取所有版本套餐
//            R<List<PackageInfoVo>> packageListR = packageBll.listAll(1);
//            List<PackageInfoVo> packageList = packageListR.getData();
//
//            for (ClientInviteProgressInfoVo progressInfoVo : progressInfoVos) {
//                List<ClientInviteProgressRewardInfoVo> rewardList = progressInfoVo.getRewardList();
//                if(rewardList != null && rewardList.size() > 0) {
//                    for (ClientInviteProgressRewardInfoVo rewardInfoVo : rewardList) {
//                        if(rewardInfoVo.getRewardType() == 0) {
//                            // 奖励类型是版本
//                            if(packageList != null && packageList.size() > 0) {
//                                for (PackageInfoVo packageInfoVo : packageList) {
//                                    if(packageInfoVo.getId().equals(rewardInfoVo.getPackageId())) {
//                                        // 设置版本名称
//                                        rewardInfoVo.setPackageName(packageInfoVo.getName());
//                                        // 设置版本价格和时长
//                                        List<CommodityPriceVo> commodityPriceList = packageInfoVo.getCommodityPriceList();
//                                        if(commodityPriceList != null && commodityPriceList.size() > 0) {
//                                            for (CommodityPriceVo commodityPriceVo : commodityPriceList) {
//                                                if(commodityPriceVo.getId().equals(rewardInfoVo.getPackagePriceId())) {
//                                                    rewardInfoVo.setPackagePrice(commodityPriceVo.getRealPrice());
//                                                    rewardInfoVo.setPackageDuration(commodityPriceVo.getValidityNum() + getUnitStr(commodityPriceVo.getValidityUnit()));
//                                                    break;
//                                                }
//                                            }
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//                        }else if(rewardInfoVo.getRewardType() == 1) {
//                            // 奖励类型是增量包
//                            rewardInfoVo.setCommodityTypeValidity(rewardInfoVo.getValidityNum() + getUnitStr(rewardInfoVo.getValidityUnit()));
//                            if(commodityTypeList != null && commodityTypeList.size() > 0) {
//                                for (CommodityTypeInfoVo commodityTypeVo : commodityTypeList) {
//                                    if(commodityTypeVo.getId().equals(rewardInfoVo.getCommodityTypeId())) {
//                                        // 设置增量包名称
//                                        rewardInfoVo.setCommodityTypeName(commodityTypeVo.getName());
//                                        rewardInfoVo.setCommodityTypeUnit(commodityTypeVo.getUnit());
//                                        if(commodityTypeVo.getCode().equals("aiAnalysisTime")) {
//                                            // 奖励是分析时长，存储的值是分钟，需要转成小时
//                                            rewardInfoVo.setCommodityNumber(rewardInfoVo.getCommodityNumber() / 60);
//                                        }else if(commodityTypeVo.getCode().equals("storageNum")) {
//                                            // 奖励是存储空间，存储的是KB，需要转成G
//                                            rewardInfoVo.setCommodityNumber(rewardInfoVo.getCommodityNumber() * 1024 * 1024);
//                                        }
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return clientInviteProgressInfoVosR;
        return R.ok();
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

}

