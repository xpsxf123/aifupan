package com.jiuyu.replay.api.service.aiagent;

import com.jiuyu.replay.api.service.aiagent.impl.AiAgentInfluencerVideoServiceImpl;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerListQueryBo;
import com.jiuyu.replay.generic.vo.aiagent.CursorPageVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerItemVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.video.project.dao.VideoHotSearchDao;
import com.jiuyu.replay.video.project.dao.VideoInfluencerInfoDao;
import com.jiuyu.replay.video.project.dao.VideoInfluencerInfoDao.TenantInfluencerRow;
import com.jiuyu.replay.video.project.dao.VideoInfoDao;
import com.jiuyu.replay.video.project.dao.VideoUserHotSubscriptionDao;
import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import com.jiuyu.replay.video.project.repository.VideoExtractContentRepository;
import com.jiuyu.replay.words.producer.TradeProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AiAgentInfluencerVideoServiceImpl#influencerList} 装配逻辑单测：
 * 覆盖 industryId 映射、分页顺序、hasMore/nextCursor 及 industryId 为 null 的 NPE 防护。
 * SQL 层（仅关联订阅表 + GROUP BY 去重 + MAX(industry_id)）为 DB 行为，由 mapper 评审 + 联调验证，不在本单测范围。
 */
class AiAgentInfluencerVideoServiceInfluencerListTest {

    private final VideoInfluencerInfoDao videoInfluencerInfoDao = mock(VideoInfluencerInfoDao.class);
    private final TradeProducer tradeProducer = mock(TradeProducer.class);
    private final AiAgentInfluencerVideoServiceImpl service = new AiAgentInfluencerVideoServiceImpl(
            videoInfluencerInfoDao,
            mock(VideoInfoDao.class),
            mock(VideoHotSearchDao.class),
            mock(VideoUserHotSubscriptionDao.class),
            mock(VideoExtractContentRepository.class),
            mock(ResilientRedisTemplate.class),
            tradeProducer);

    private static InfluencerListQueryBo boWith(int pageSize) {
        InfluencerListQueryBo bo = new InfluencerListQueryBo();
        bo.setUserId(1001L);
        bo.setTenantId(1L);
        bo.setUserType(0);
        bo.setPageSize(pageSize);
        return bo;
    }

    private static VideoInfluencerInfoEntity entity(Long id) {
        return new VideoInfluencerInfoEntity().setId(id);
    }

    @Test
    @DisplayName("多行订阅：industryId 逐条映射且保持分页降序，hasMore/nextCursor 正确")
    void influencerList_multiRows_mapsIndustryIdAndPreservesOrderAndPaging() {
        // pageSize=2，DAO 返回 3 行（limit=pageSize+1）触发 hasMore
        when(videoInfluencerInfoDao.pageTenantInfluencerIds(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(Arrays.asList(
                        new TenantInfluencerRow(300L, 30L, null),
                        new TenantInfluencerRow(200L, 20L, null),
                        new TenantInfluencerRow(100L, 10L, null)));
        // selectBatchIds 只对前 2 个 id（300,200）调用；乱序返回验证装配顺序取自 pageRows
        when(videoInfluencerInfoDao.selectBatchIds(any()))
                .thenReturn(Arrays.asList(entity(200L), entity(300L)));
        when(tradeProducer.getTradeNames(any())).thenReturn(Map.of(30L, "美妆", 20L, "服饰"));

        R<CursorPageVo<InfluencerItemVo>> r = service.influencerList(boWith(2));

        CursorPageVo<InfluencerItemVo> page = r.getData();
        List<InfluencerItemVo> list = page.getList();
        assertEquals(2, list.size());
        assertEquals(300L, list.get(0).getId());
        assertEquals(30L, list.get(0).getIndustryId());
        assertEquals("美妆", list.get(0).getIndustryName());
        assertEquals(200L, list.get(1).getId());
        assertEquals(20L, list.get(1).getIndustryId());
        assertEquals("服饰", list.get(1).getIndustryName());
        assertTrue(page.getHasMore());
        assertEquals(200L, page.getNextCursor());
    }

    @Test
    @DisplayName("industryId 非空但行业库缺失该行业：industryName 回填为 null，不抛异常")
    void influencerList_industryNotFound_nameNull() {
        when(videoInfluencerInfoDao.pageTenantInfluencerIds(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(new TenantInfluencerRow(100L, 99L, null)));
        when(videoInfluencerInfoDao.selectBatchIds(any()))
                .thenReturn(List.of(entity(100L)));
        when(tradeProducer.getTradeNames(any())).thenReturn(Map.of());   // 行业库无 99

        R<CursorPageVo<InfluencerItemVo>> r = service.influencerList(boWith(10));

        InfluencerItemVo vo = r.getData().getList().get(0);
        assertEquals(99L, vo.getIndustryId());
        assertNull(vo.getIndustryName());
    }

    @Test
    @DisplayName("industryId 为 null：装配不抛 NPE，VO.industryId 为 null")
    void influencerList_nullIndustryId_noNpe() {
        when(videoInfluencerInfoDao.pageTenantInfluencerIds(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(new TenantInfluencerRow(100L, null, null)));
        when(videoInfluencerInfoDao.selectBatchIds(any()))
                .thenReturn(List.of(entity(100L)));

        R<CursorPageVo<InfluencerItemVo>> r = service.influencerList(boWith(10));

        List<InfluencerItemVo> list = r.getData().getList();
        assertEquals(1, list.size());
        assertEquals(100L, list.get(0).getId());
        assertNull(list.get(0).getIndustryId());
        assertNull(list.get(0).getIndustryName());
    }

    @Test
    @DisplayName("达人明细缺失：该行跳过，不进入结果")
    void influencerList_missingEntity_skipsRow() {
        when(videoInfluencerInfoDao.pageTenantInfluencerIds(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(Arrays.asList(
                        new TenantInfluencerRow(300L, 30L, null),
                        new TenantInfluencerRow(200L, 20L, null)));
        // 只有 300 的明细存在，200 缺失
        when(videoInfluencerInfoDao.selectBatchIds(any()))
                .thenReturn(List.of(entity(300L)));

        R<CursorPageVo<InfluencerItemVo>> r = service.influencerList(boWith(10));

        List<InfluencerItemVo> list = r.getData().getList();
        assertEquals(1, list.size());
        assertEquals(300L, list.get(0).getId());
        assertEquals(30L, list.get(0).getIndustryId());
    }

    @Test
    @DisplayName("无订阅达人：返回空页")
    void influencerList_noRows_returnsEmptyPage() {
        when(videoInfluencerInfoDao.pageTenantInfluencerIds(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of());

        R<CursorPageVo<InfluencerItemVo>> r = service.influencerList(boWith(10));

        CursorPageVo<InfluencerItemVo> page = r.getData();
        assertTrue(page.getList().isEmpty());
        assertNull(page.getNextCursor());
        assertFalse(page.getHasMore());
    }
}
