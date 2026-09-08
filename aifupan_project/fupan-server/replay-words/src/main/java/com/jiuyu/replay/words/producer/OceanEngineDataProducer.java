package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.vo.OnlineChartVo;
import com.jiuyu.replay.words.vo.chart.CurveData;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * 巨量引擎数据表Producer
 * 
 * @author liaoxin
 * @date 2025-06-13
 */
public interface OceanEngineDataProducer {


    void processOceanEngineTimeSeriesData(List<OceanEngineProcessBo> datas, OnlineChartVo onlineChartVo, String videoId, Date startTime, Date endTime);


    /**
     * 把数据转换为对应的时间的数据
     *
     * @param dataList
     * @param onlineDataList
     * @param startTime
     * @param endTime
     * @return
     */
    List<OceanEngineProcessBo> convertToIncrementalData(List<OceanEngineProcessBo> dataList, List<CurveData> onlineDataList, Date startTime, Date endTime);

    /**
     * 把增量数据转换为图表数据
     *
     * @param incrementalData
     * @param valueExtractor
     * @param startTime
     * @return
     */
    List<CurveData> convertToChartData(List<OceanEngineProcessBo> incrementalData, Function<OceanEngineProcessBo, Integer> valueExtractor, Date startTime);
}