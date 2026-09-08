package com.jiuyu.replay.generic.feign.third;

import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.vo.third.DanMuItemVo;

import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 上午11:44
 */
public interface TableStoreFeign {

    List<Map<String, Object>> getBarrageDataList(String videoId, List<String> audioaAlyses);

    /**
     * 查询弹幕数据总条数
     *
     * @param queryDanMuBo 查询条件
     * @return 总条数
     */
    Long queryDanMuSearchCount(QueryDanMuBo queryDanMuBo);

    /**
     * 按时间窗查询弹幕数据（互动巡检切片数据源）。
     *
     * <p>内部委托 TableStoreBll.queryDanMuSearchData（已含 2h Redis 缓存），
     * 取 QueryDanMuVo.list 元素映射为精简 DanMuItemVo 返回。</p>
     *
     * @param bo 查询条件（startTime/endTime/videoId/batchNumber/userId/tenantId 必填）
     * @return 弹幕条目列表（空列表非 null）
     */
    List<DanMuItemVo> queryDanMuSearchData(QueryDanMuBo bo);
}
