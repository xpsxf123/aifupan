package com.jiuyu.replay.third.api;

import cn.hutool.core.collection.CollUtil;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.vo.third.DanMuItemVo;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 上午11:28
 */
@Slf4j
@Component
@AllArgsConstructor
public class TableStoreApi implements TableStoreFeign {

    private final TableStoreBll tableStoreBll;

    @Override
    public List<Map<String, Object>> getBarrageDataList(String videoId, List<String> audioaAlyses) {
        return tableStoreBll.getBarrageDataList(videoId, audioaAlyses, null);
    }

    @Override
    public Long queryDanMuSearchCount(QueryDanMuBo queryDanMuBo) {
        return tableStoreBll.queryDanMuSearchCount(queryDanMuBo);
    }

    /**
     * 按时间窗查询弹幕数据（互动巡检切片数据源）。
     *
     * <p>委托 {@link TableStoreBll#queryDanMuSearchData(QueryDanMuBo)}（已含 2h Redis 缓存），
     * 取 QueryDanMuVo.list 做内存映射 DanMuVo → DanMuItemVo（字段子集），不引入新缓存 key。</p>
     *
     * @param bo 查询条件（startTime/endTime/videoId/batchNumber/userId/tenantId 必填）
     * @return 弹幕条目列表（空列表非 null）
     */
    @Override
    public List<DanMuItemVo> queryDanMuSearchData(QueryDanMuBo bo) {
        try {
            com.jiuyu.replay.generic.vo.common.R<QueryDanMuVo> r = tableStoreBll.queryDanMuSearchData(bo);
            if (r == null || r.getData() == null || CollUtil.isEmpty(r.getData().getList())) {
                return Collections.emptyList();
            }
            List<DanMuVo> danMuList = r.getData().getList();
            List<DanMuItemVo> result = new ArrayList<>(danMuList.size());
            for (DanMuVo danMu : danMuList) {
                DanMuItemVo item = new DanMuItemVo();
                item.setContent(danMu.getContent());
                item.setRecordDate(danMu.getRecordDate());
                item.setNickName(danMu.getNickName());
                item.setLevel(danMu.getLevel());
                item.setIsNew(danMu.getIsNew());
                item.setFansLevelCurrent(danMu.getFansLevelCurrent());
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            log.warn("[TableStoreApi] queryDanMuSearchData 异常，返回空列表 videoId={}", bo.getVideoId(), e);
            return Collections.emptyList();
        }
    }
}
