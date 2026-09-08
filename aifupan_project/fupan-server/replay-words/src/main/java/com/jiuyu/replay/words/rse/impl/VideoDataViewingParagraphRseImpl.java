package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.words.video.SaveSliceCorrelationDataBo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.entity.VideoDataViewingParagraphEntity;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import com.jiuyu.replay.words.repository.service.VideoDataViewingParagraphService;
import com.jiuyu.replay.words.producer.VideoDataViewingConfuseProducer;
import com.jiuyu.replay.words.rse.VideoDataViewingParagraphRse;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class VideoDataViewingParagraphRseImpl implements VideoDataViewingParagraphRse {

    @Resource
    private VideoDataViewingParagraphService videoDataViewingParagraphService;
    @Resource
    private AnchorVideoService anchorVideoService;
    @Resource
    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;

    @Override
    public void saveOrUpdateParagraphData(VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo, List<OceanEngineProcessBo> realTimeDataList) {

        if(realTimeDataList == null || realTimeDataList.isEmpty()) {
            return;
        }

        AnchorVideoEntity videoEntity = this.anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>().eq(AnchorVideoEntity::getVideoId, dataViewingConfuseInfoVo.getVideoId()));
        if(videoEntity == null) {
            return;
        }

        long startTimeStamp = videoEntity.getStartTime().getTime();
        long endTimeStamp = videoEntity.getEndTime().getTime();
        List<OceanEngineProcessBo> resultRealTimeDataList = realTimeDataList
                .stream()
                .filter(item -> item.getGatherTimeStamp() >= startTimeStamp && item.getGatherTimeStamp() <= (endTimeStamp + 1000))
                .sorted(Comparator.comparingLong(OceanEngineProcessBo::getGatherTimeStamp))
                .toList();

        if(resultRealTimeDataList.isEmpty() || resultRealTimeDataList.size() < 2) {
            return;
        }

        // 获取旧记录
        LambdaQueryWrapper<VideoDataViewingParagraphEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingParagraphEntity::getDataViewingConfuseId, dataViewingConfuseInfoVo.getId());
        VideoDataViewingParagraphEntity oldDataViewingParagraphEntity = this.videoDataViewingParagraphService.getOne(wrapper);
        boolean isAdd = oldDataViewingParagraphEntity == null;

        VideoDataViewingParagraphEntity videoDataViewingParagraphEntity = calculateParagraphData(resultRealTimeDataList, dataViewingConfuseInfoVo);

        videoDataViewingParagraphEntity.setDataViewingConfuseId(dataViewingConfuseInfoVo.getId());
        videoDataViewingParagraphEntity.setUpdateDate(new Date());

        // 保存
        if(isAdd) {
            videoDataViewingParagraphEntity.setId(SnowflakeManager.nextValue());
            videoDataViewingParagraphEntity.setCreateDate(new Date());
            this.videoDataViewingParagraphService.save(videoDataViewingParagraphEntity);
        }else {
            videoDataViewingParagraphEntity.setId(oldDataViewingParagraphEntity.getId());
            this.videoDataViewingParagraphService.updateById(videoDataViewingParagraphEntity);
        }


//        // 获取旧记录
//        LambdaQueryWrapper<VideoDataViewingParagraphEntity> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(VideoDataViewingParagraphEntity::getDataViewingConfuseId, dataViewingConfuseInfoVo.getId());
//        VideoDataViewingParagraphEntity videoDataViewingParagraphEntity = this.videoDataViewingParagraphService.getOne(wrapper);
//
//        boolean isAdd = true;
//        if(videoDataViewingParagraphEntity == null) {
//            videoDataViewingParagraphEntity = new VideoDataViewingParagraphEntity();
//            // 赋值
//            BeanUtils.copyProperties(dataViewingConfuseInfoVo, videoDataViewingParagraphEntity);
//            videoDataViewingParagraphEntity.setId(SnowflakeManager.nextValue());
//            videoDataViewingParagraphEntity.setCreateDate(new Date());
//        }else {
//            Long oldId = videoDataViewingParagraphEntity.getId();
//            BeanUtils.copyProperties(dataViewingConfuseInfoVo, videoDataViewingParagraphEntity);
//            videoDataViewingParagraphEntity.setId(oldId);
//            isAdd = false;
//        }
//
//        videoDataViewingParagraphEntity.setDataViewingConfuseId(dataViewingConfuseInfoVo.getId());
//        videoDataViewingParagraphEntity.setUpdateDate(new Date());
//
//        OceanEngineProcessBo oceanEngineProcessBo = resultRealTimeDataList.get(0);
//        int payComboCnt = checkNull(oceanEngineProcessBo.getPayComboCnt());
//        int payAmt = checkNull(oceanEngineProcessBo.getPayAmt());
//        int followAnchorUcnt = checkNull(oceanEngineProcessBo.getFollowAnchorUcnt());
//        int watchNum = checkNull(oceanEngineProcessBo.getWatchNum());
//
//        if(dataViewingConfuseInfoVo.getTotalWatchNum() != null) {
//            int value = dataViewingConfuseInfoVo.getTotalWatchNum() - watchNum;
//            videoDataViewingParagraphEntity.setTotalWatchNum(Math.max(value, 0));
//        }
//        if(dataViewingConfuseInfoVo.getIncrementFollowerCount() != null) {
//            int value = dataViewingConfuseInfoVo.getIncrementFollowerCount() - followAnchorUcnt;
//            videoDataViewingParagraphEntity.setIncrementFollowerCount(Math.max(value, 0));
//        }
//        if(dataViewingConfuseInfoVo.getVolumeStart() != null) {
//            int value = dataViewingConfuseInfoVo.getVolumeStart() - (payAmt / 100);
//            videoDataViewingParagraphEntity.setVolumeStart(Math.max(value, 0));
//            videoDataViewingParagraphEntity.setVolumeEnd(Math.max(value, 0));
//        }
//        if(dataViewingConfuseInfoVo.getPurchaseCountStart() != null) {
//            int value = dataViewingConfuseInfoVo.getPurchaseCountStart() - payComboCnt;
//            videoDataViewingParagraphEntity.setPurchaseCountStart(Math.max(value, 0));
//            videoDataViewingParagraphEntity.setPurchaseCountEnd(Math.max(value, 0));
//        }
//        if(videoDataViewingParagraphEntity.getVolumeStart() != null && videoDataViewingParagraphEntity.getPurchaseCountStart() != null) {
//            if(videoDataViewingParagraphEntity.getPurchaseCountStart() > 0) {
//                // 客单价 = 销售额 / 销量
//                double value = (double)videoDataViewingParagraphEntity.getVolumeStart() / videoDataViewingParagraphEntity.getPurchaseCountStart();
//                videoDataViewingParagraphEntity.setCustomerUnitPriceStart(Math.max(value, 0));
//                videoDataViewingParagraphEntity.setCustomerUnitPriceEnd(Math.max(value, 0));
//            }else {
//                videoDataViewingParagraphEntity.setCustomerUnitPriceStart(0.0);
//                videoDataViewingParagraphEntity.setCustomerUnitPriceEnd(0.0);
//            }
//        }
//        if(videoDataViewingParagraphEntity.getVolumeStart() != null && videoDataViewingParagraphEntity.getTotalWatchNum() != null) {
//            if(videoDataViewingParagraphEntity.getTotalWatchNum() > 0) {
//                // uv价值 = 销售额 / 观看人数
//                double value = (double)videoDataViewingParagraphEntity.getVolumeStart() / videoDataViewingParagraphEntity.getTotalWatchNum();
//                videoDataViewingParagraphEntity.setUvValueStart(Math.max(value, 0));
//                videoDataViewingParagraphEntity.setUvValueEnd(Math.max(value, 0));
//            }else {
//                videoDataViewingParagraphEntity.setUvValueStart(0.0);
//                videoDataViewingParagraphEntity.setUvValueEnd(0.0);
//            }
//
//        }
//        if(videoDataViewingParagraphEntity.getPurchaseCountStart() != null && videoDataViewingParagraphEntity.getTotalWatchNum() != null) {
//            if(videoDataViewingParagraphEntity.getTotalWatchNum() > 0) {
//                // 带货转化率 = 销量 / 观看人数
//                double value = (double)videoDataViewingParagraphEntity.getPurchaseCountStart() / videoDataViewingParagraphEntity.getTotalWatchNum();
//                videoDataViewingParagraphEntity.setGoodsConvertRateStart(Math.max(value, 0));
//                videoDataViewingParagraphEntity.setGoodsConvertRateEnd(Math.max(value, 0));
//            }else {
//                videoDataViewingParagraphEntity.setGoodsConvertRateStart(0.0);
//                videoDataViewingParagraphEntity.setGoodsConvertRateEnd(0.0);
//            }
//        }
//        if(videoDataViewingParagraphEntity.getTotalWatchNum() != null && videoDataViewingParagraphEntity.getIncrementFollowerCount() != null) {
//            if(videoDataViewingParagraphEntity.getTotalWatchNum() > 0) {
//                // 转粉率 = 新增粉丝量 / 观看人数
//                double value = (double)videoDataViewingParagraphEntity.getIncrementFollowerCount() / videoDataViewingParagraphEntity.getTotalWatchNum();
//                videoDataViewingParagraphEntity.setConvertFanRate(Math.max(value, 0));
//            }else {
//                videoDataViewingParagraphEntity.setConvertFanRate(0.0);
//            }
//        }
//
//        // 保存
//        if(isAdd) {
//            videoDataViewingParagraphEntity.setId(SnowflakeManager.nextValue());
//            videoDataViewingParagraphEntity.setCreateDate(new Date());
//            this.videoDataViewingParagraphService.save(videoDataViewingParagraphEntity);
//        }else {
//            this.videoDataViewingParagraphService.updateById(videoDataViewingParagraphEntity);
//        }

    }

    @Override
    public VideoDataViewingConfuseInfoVo infoByVideoId(String videoId) {

        LambdaQueryWrapper<VideoDataViewingParagraphEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoDataViewingParagraphEntity::getVideoId, videoId);
        VideoDataViewingParagraphEntity videoDataViewingParagraphEntity = this.videoDataViewingParagraphService.getOne(wrapper);

        if(videoDataViewingParagraphEntity != null) {
            VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = BeanConvertUtils.convert(videoDataViewingParagraphEntity, VideoDataViewingConfuseInfoVo.class);
            videoDataViewingConfuseInfoVo.setDataSourceType(1);

            return videoDataViewingConfuseInfoVo;
        }

        return null;
    }

    /**
     * 将 paragraph 各指标值限制在 confuse 表值以下
     */
    private void capByConfuse(VideoDataViewingParagraphEntity paragraph, VideoDataViewingConfuseInfoVo confuse) {
        paragraph.setTotalWatchNum(min(paragraph.getTotalWatchNum(), confuse.getTotalWatchNum()));
    }

    private Integer min(Integer a, Integer b) {
        if (a == null || b == null) return a;
        return a <= b ? a : b;
    }

    private Double min(Double a, Double b) {
        if (a == null || b == null) return a;
        return a <= b ? a : b;
    }

    @Override
    public void saveSliceVideoParagraphData(SaveSliceCorrelationDataBo saveSliceCorrelationDataBo, List<OceanEngineProcessBo> realTimeDataList, VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo) {
        if(realTimeDataList == null || realTimeDataList.isEmpty()) {
            return;
        }
        List<OceanEngineProcessBo> resultRealTimeDataList = realTimeDataList
                .stream()
                .filter(item -> item.getGatherTimeStamp() >= saveSliceCorrelationDataBo.getSliceStartNaturalTime() && item.getGatherTimeStamp() <= saveSliceCorrelationDataBo.getSliceEndNaturalTime())
                .sorted(Comparator.comparingLong(OceanEngineProcessBo::getGatherTimeStamp))
                .toList();

        if(resultRealTimeDataList.isEmpty() || resultRealTimeDataList.size() < 2) {
            return;
        }

        VideoDataViewingParagraphEntity dataViewingParagraphEntity = calculateParagraphData(resultRealTimeDataList, dataViewingConfuseInfoVo);
        dataViewingParagraphEntity.setId(SnowflakeManager.nextValue());
        dataViewingParagraphEntity.setCreateDate(new Date());
        dataViewingParagraphEntity.setUpdateDate(new Date());
        dataViewingParagraphEntity.setDataViewingConfuseId(dataViewingConfuseInfoVo.getId());

        this.videoDataViewingParagraphService.save(dataViewingParagraphEntity);

    }

    /**
     * 计算本段数据
     * @param resultRealTimeDataList 过程数据
     * @param dataViewingConfuseInfoVo 截止数据
     * @return
     */
    private VideoDataViewingParagraphEntity calculateParagraphData(List<OceanEngineProcessBo> resultRealTimeDataList, VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo) {
        // 获取第一条数据和最后一条数据
        OceanEngineProcessBo oneRealTimeData = resultRealTimeDataList.get(0);
        OceanEngineProcessBo lastRealTimeData = resultRealTimeDataList.get(resultRealTimeDataList.size() - 1);
        // 设置切片数据看板本段数据
        VideoDataViewingParagraphEntity dataViewingParagraphEntity = BeanConvertUtils.convert(dataViewingConfuseInfoVo, VideoDataViewingParagraphEntity.class);
//        dataViewingParagraphEntity.setId(SnowflakeManager.nextValue());
//        dataViewingParagraphEntity.setCreateDate(new Date());
//        dataViewingParagraphEntity.setUpdateDate(new Date());
//        dataViewingParagraphEntity.setDataViewingConfuseId(dataViewingConfuseInfoVo.getId());

        int totalWatchNum = checkNull(lastRealTimeData.getWatchNum()) - checkNull(oneRealTimeData.getWatchNum());
        dataViewingParagraphEntity.setTotalWatchNum(totalWatchNum);
        int followAnchorUcnt = checkNull(lastRealTimeData.getFollowAnchorUcnt()) - checkNull(oneRealTimeData.getFollowAnchorUcnt());
        dataViewingParagraphEntity.setIncrementFollowerCount(followAnchorUcnt);

//        dataViewingParagraphEntity.setAverageOnlineNum(-1);
//        dataViewingParagraphEntity.setAverageResidenceTime(-1);
//        dataViewingParagraphEntity.setInteractionPercent(-1.0);
        int volume = (checkNull(lastRealTimeData.getPayAmt()) - checkNull(oneRealTimeData.getPayAmt())) / 100;
        dataViewingParagraphEntity.setVolumeStart(volume);
        dataViewingParagraphEntity.setVolumeEnd(volume);
        int payComboCnt = checkNull(lastRealTimeData.getPayComboCnt()) - checkNull(oneRealTimeData.getPayComboCnt());
        dataViewingParagraphEntity.setPurchaseCountStart(payComboCnt);
        dataViewingParagraphEntity.setPurchaseCountEnd(payComboCnt);

        if(payComboCnt > 0) {
            // 客单价 = 销售额 / 销量
            double customerUnitPrice = (double)volume / payComboCnt;
            dataViewingParagraphEntity.setCustomerUnitPriceStart(customerUnitPrice);
            dataViewingParagraphEntity.setCustomerUnitPriceEnd(customerUnitPrice);
        }else {
            dataViewingParagraphEntity.setCustomerUnitPriceStart(-1.0);
            dataViewingParagraphEntity.setCustomerUnitPriceEnd(-1.0);
        }

        if(totalWatchNum > 0) {
            // 转粉率 = 新增粉丝量 / 观看人次
            dataViewingParagraphEntity.setConvertFanRate((double)followAnchorUcnt / totalWatchNum);
            // uv价值 = 销售额 / 观看人数
            double uvValue = (double)volume / totalWatchNum;
            dataViewingParagraphEntity.setUvValueStart(uvValue);
            dataViewingParagraphEntity.setUvValueEnd(uvValue);
            // 带货转化率 = 销量 / 观看人数
            double GoodsConvertRate = (double)payComboCnt / totalWatchNum;
            dataViewingParagraphEntity.setGoodsConvertRateStart(GoodsConvertRate);
            dataViewingParagraphEntity.setGoodsConvertRateEnd(GoodsConvertRate);
            // 千次观看成交金额(GPM) = 成交金额 / 观看人次 * 1000
            double gpm = (double)volume / totalWatchNum * 1000;
            dataViewingParagraphEntity.setGpmStart(gpm);
            dataViewingParagraphEntity.setGpmEnd(gpm);
        }else {
            dataViewingParagraphEntity.setConvertFanRate(0.0);
            dataViewingParagraphEntity.setUvValueStart(0.0);
            dataViewingParagraphEntity.setUvValueEnd(0.0);
            dataViewingParagraphEntity.setGoodsConvertRateStart(0.0);
            dataViewingParagraphEntity.setGoodsConvertRateEnd(0.0);
            dataViewingParagraphEntity.setGpmStart(null);
            dataViewingParagraphEntity.setGpmEnd(null);
        }

        // 计算退款金额（分→元）
        BigDecimal refundDeltaFen = checkNullDecimal(lastRealTimeData.getRefundAmt())
                .subtract(checkNullDecimal(oneRealTimeData.getRefundAmt()));
        double refundAmountYuan = refundDeltaFen.doubleValue() / 100.0;
        dataViewingParagraphEntity.setRefundAmount(refundAmountYuan);

        // 计算投放消耗（分→元）
        BigDecimal costDeltaFen = checkNullDecimal(lastRealTimeData.getQianchuanCost())
                .subtract(checkNullDecimal(oneRealTimeData.getQianchuanCost()));
        double launchRoiAmountYuan = Math.max(costDeltaFen.doubleValue() / 100.0, 0);
        dataViewingParagraphEntity.setLaunchRoiAmount(launchRoiAmountYuan);

        // 截止数据和本段数据取最小值
        capByConfuse(dataViewingParagraphEntity, dataViewingConfuseInfoVo);

        // 从 capped 值重新计算ROI，确保与组件值一致
        recalculateRoi(dataViewingParagraphEntity);

        return dataViewingParagraphEntity;
    }

    /**
     * 检查值是否为null，为null返回0
     * @param value 值
     * @return
     */
    private int checkNull(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal checkNullDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 从 capped 的销售额/退款/消耗重新计算整体支付ROI和净成交ROI
     */
    private void recalculateRoi(VideoDataViewingParagraphEntity entity) {
        double volume = entity.getVolumeStart() != null ? entity.getVolumeStart() : 0;
        double refund = entity.getRefundAmount() != null ? entity.getRefundAmount() : 0;
        double cost = entity.getLaunchRoiAmount() != null ? entity.getLaunchRoiAmount() : 0;

        if (cost > 0) {
            entity.setOverallCostRoi(Math.round((volume / cost) * 100.0) / 100.0);
            entity.setRoi(entity.getOverallCostRoi());
            double netAmt = volume - refund;
            if (netAmt > 0) {
                entity.setNetTransactionRoi(Math.round((netAmt / cost) * 100.0) / 100.0);
            } else {
                entity.setNetTransactionRoi(0.0);
            }
        } else {
            entity.setOverallCostRoi(0.0);
            entity.setNetTransactionRoi(0.0);
            entity.setRoi(0.0);
        }
    }

    @Override
    public List<VideoDataViewingConfuseInfoVo> listByVideoIds(List<String> videoIds) {
        if(videoIds == null || videoIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<VideoDataViewingParagraphEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VideoDataViewingParagraphEntity::getVideoId, videoIds);
        List<VideoDataViewingParagraphEntity> list = this.videoDataViewingParagraphService.list(wrapper);

        if(list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        return list.stream()
                .map(item -> {
                    VideoDataViewingConfuseInfoVo vo = BeanConvertUtils.convert(item, VideoDataViewingConfuseInfoVo.class);
                    vo.setDataSourceType(1);
                    return vo;
                })
                .toList();
    }
}
