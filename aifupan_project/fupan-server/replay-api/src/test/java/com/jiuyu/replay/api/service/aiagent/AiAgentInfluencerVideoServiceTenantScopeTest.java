package com.jiuyu.replay.api.service.aiagent;

import com.jiuyu.replay.api.service.aiagent.impl.AiAgentInfluencerVideoServiceImpl;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.generic.bo.aiagent.AiAgentBaseBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerAnalyticsOverviewBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerVideoStatsBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoAudioTextBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoDetailBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoGlobalSearchBo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerAnalyticsVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoItemVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoStatsVo;
import com.jiuyu.replay.generic.vo.aiagent.StreamPageVo;
import com.jiuyu.replay.generic.vo.aiagent.VideoAudioTextVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.video.project.dao.VideoHotSearchDao;
import com.jiuyu.replay.video.project.dao.VideoInfluencerInfoDao;
import com.jiuyu.replay.video.project.dao.VideoInfoDao;
import com.jiuyu.replay.video.project.dao.VideoInfoDao.VideoHashRow;
import com.jiuyu.replay.video.project.dao.VideoUserHotSubscriptionDao;
import com.jiuyu.replay.video.project.document.VideoContentExtract;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.repository.VideoExtractContentRepository;
import com.jiuyu.replay.words.producer.TradeProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 租户可见范围（订阅达人）与文案授权（本租户提取成功）口径单测。
 *
 * <p>覆盖三条业务规则：① tb_video_user_video 只在本租户发起过提取时才有记录，故它是文案授权的唯一依据；
 * ② 订阅了达人即可见该达人全部作品，但提取状态按租户口径给；③ 全库爬取库检索统一不返回文案。
 * SQL 层（订阅子查询 / EXISTS 过滤）为 DB 行为，由 mapper 评审 + 联调验证，不在本单测范围。</p>
 */
class AiAgentInfluencerVideoServiceTenantScopeTest {

    private static final Long TENANT_ID = 1L;

    private final VideoInfluencerInfoDao videoInfluencerInfoDao = mock(VideoInfluencerInfoDao.class);
    private final VideoInfoDao videoInfoDao = mock(VideoInfoDao.class);
    private final VideoExtractContentRepository videoExtractContentRepository =
            mock(VideoExtractContentRepository.class);
    private final AiAgentInfluencerVideoServiceImpl service = new AiAgentInfluencerVideoServiceImpl(
            videoInfluencerInfoDao,
            videoInfoDao,
            mock(VideoHotSearchDao.class),
            mock(VideoUserHotSubscriptionDao.class),
            videoExtractContentRepository,
            mock(ResilientRedisTemplate.class),
            mock(TradeProducer.class));

    private static <T extends AiAgentBaseBo> T identity(T bo) {
        bo.setUserId(1001L);
        bo.setTenantId(TENANT_ID);
        bo.setUserType(0);
        return bo;
    }

    private static VideoInfoEntity video(Long id, String hash) {
        VideoInfoEntity e = new VideoInfoEntity();
        e.setId(id);
        e.setVideoHash(hash);
        e.setExtractStatus((byte) 1);   // 全局库已提取：不应直接透出给租户
        return e;
    }

    private static VideoContentExtract extract(String hash) {
        VideoContentExtract c = new VideoContentExtract();
        c.setVideoHash(hash);
        c.setOriginalAudioContent("原文-" + hash);
        c.setAudioContent("AI-" + hash);
        return c;
    }

    /**
     * 让「本租户提取成功」的视频集合固定为 extractedIds。
     */
    private void givenTenantExtracted(Long... extractedIds) {
        List<Long> extracted = List.of(extractedIds);
        when(videoInfoDao.listTenantExtractedVideoIds(eq(TENANT_ID), any()))
                .thenAnswer(inv -> {
                    Collection<?> asked = inv.getArgument(1);
                    return extracted.stream().filter(asked::contains).toList();
                });
    }

    private VideoGlobalSearchBo searchBo(boolean collectedOnly, boolean withAudioText) {
        VideoGlobalSearchBo bo = identity(new VideoGlobalSearchBo());
        bo.setPageSize(10);   // 带 pageSize 直接回源，跳过首页缓存
        bo.setCollectedOnly(collectedOnly);
        bo.setWithAudioText(withAudioText);
        return bo;
    }

    @Test
    @DisplayName("全库检索：不做租户限制 —— 本租户没提取过也照给文案，extractStatus 取全局原值")
    void globalVideoSearch_globalScope_noTenantRestriction() {
        givenTenantExtracted();   // 本租户一条都没提取过
        when(videoInfoDao.selectList(any())).thenReturn(List.of(video(100L, "h100"), video(200L, "h200")));
        when(videoExtractContentRepository.findByVideoHashIn(any()))
                .thenReturn(List.of(extract("h100"), extract("h200")));

        R<StreamPageVo<InfluencerVideoItemVo>> r = service.globalVideoSearch(searchBo(false, true));

        List<InfluencerVideoItemVo> list = r.getData().getList();
        assertEquals("原文-h100", list.get(0).getOriginalAudioContent());
        assertEquals("原文-h200", list.get(1).getOriginalAudioContent());
        assertEquals((byte) 1, list.get(0).getExtractStatus());   // = tb_video_info.extract_status 原值
        // 全库口径不查租户提取授权
        verify(videoInfoDao, never()).listTenantExtractedVideoIds(any(), any());
    }

    @Test
    @DisplayName("全库检索 withAudioText=false：不回源 Mongo")
    void globalVideoSearch_withoutAudioText_skipsMongo() {
        when(videoInfoDao.selectList(any())).thenReturn(List.of(video(100L, "h100")));

        R<StreamPageVo<InfluencerVideoItemVo>> r = service.globalVideoSearch(searchBo(false, false));

        assertNull(r.getData().getList().get(0).getOriginalAudioContent());
        verify(videoExtractContentRepository, never()).findByVideoHashIn(any());
    }

    @Test
    @DisplayName("租户范围检索：按订阅达人 author_id 下推，仅本租户提取成功的视频挂载文案")
    void globalVideoSearch_tenantScope_onlyAuthorizedVideosCarryText() {
        givenTenantExtracted(100L);   // 100 已提取成功，200 未提取
        when(videoInfluencerInfoDao.selectSubscribedInfluencerIds(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn(List.of(7L, 8L));
        when(videoInfoDao.pageSubscribedInfluencerVideos(eq(TENANT_ID), any(), isNull(), isNull(),
                isNull(), isNull(), isNull(), anyInt(), anyBoolean(), anyBoolean(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(video(100L, "h100"), video(200L, "h200")));
        when(videoExtractContentRepository.findByVideoHashIn(any())).thenReturn(List.of(extract("h100")));

        R<StreamPageVo<InfluencerVideoItemVo>> r = service.globalVideoSearch(searchBo(true, true));

        List<InfluencerVideoItemVo> list = r.getData().getList();
        assertEquals("原文-h100", list.get(0).getOriginalAudioContent());
        assertEquals((byte) 1, list.get(0).getExtractStatus());
        assertNull(list.get(1).getOriginalAudioContent());
        assertEquals((byte) 0, list.get(1).getExtractStatus());
        // 未授权视频的 hash 不应进入 Mongo 查询
        verify(videoExtractContentRepository).findByVideoHashIn(List.of("h100"));
        // 订阅达人 id 以字符串形式下推为 author_id 绑定参数（不在 SQL 里 CAST）
        verify(videoInfoDao).pageSubscribedInfluencerVideos(eq(TENANT_ID), eq(List.of("7", "8")), isNull(),
                isNull(), isNull(), isNull(), isNull(), anyInt(), anyBoolean(), anyBoolean(), isNull(), isNull(),
                anyInt());
    }

    @Test
    @DisplayName("租户范围检索：一个达人都没订阅时直接返回空页，不查视频表")
    void globalVideoSearch_tenantScope_noSubscription_returnsEmptyPage() {
        when(videoInfluencerInfoDao.selectSubscribedInfluencerIds(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn(List.of());

        R<StreamPageVo<InfluencerVideoItemVo>> r = service.globalVideoSearch(searchBo(true, false));

        assertTrue(r.getData().getList().isEmpty());
        verify(videoInfoDao, never()).pageSubscribedInfluencerVideos(any(), any(), any(), any(), any(), any(),
                any(), anyInt(), anyBoolean(), anyBoolean(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("租户范围检索：达人过滤与订阅范围取交集，非订阅达人被剔除")
    void globalVideoSearch_tenantScope_intersectsFilterWithSubscription() {
        givenTenantExtracted();
        when(videoInfluencerInfoDao.selectSubscribedInfluencerIds(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn(List.of(7L, 8L));
        when(videoInfoDao.pageSubscribedInfluencerVideos(eq(TENANT_ID), any(), isNull(), isNull(),
                isNull(), isNull(), isNull(), anyInt(), anyBoolean(), anyBoolean(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(video(100L, "h100")));

        VideoGlobalSearchBo bo = searchBo(true, false);
        bo.setInfluencerIdList(List.of(8L, 9L));   // 9 未订阅
        service.globalVideoSearch(bo);

        verify(videoInfoDao).pageSubscribedInfluencerVideos(eq(TENANT_ID), eq(List.of("8")), isNull(),
                isNull(), isNull(), isNull(), isNull(), anyInt(), anyBoolean(), anyBoolean(), isNull(), isNull(),
                anyInt());
    }

    @Test
    @DisplayName("批量取文案：本租户未提取成功的视频文案为 null，行仍返回")
    void batchVideoAudioText_unauthorizedVideo_returnsNullContent() {
        givenTenantExtracted(100L);
        VideoHashRow r100 = new VideoHashRow();
        r100.setId(100L);
        r100.setVideoHash("h100");
        VideoHashRow r200 = new VideoHashRow();
        r200.setId(200L);
        r200.setVideoHash("h200");
        when(videoInfoDao.listVideoHashByIds(any())).thenReturn(List.of(r100, r200));
        when(videoExtractContentRepository.findByVideoHashIn(any())).thenReturn(List.of(extract("h100")));

        VideoAudioTextBatchBo bo = identity(new VideoAudioTextBatchBo());
        bo.setVideoIdList(List.of(100L, 200L));
        R<List<VideoAudioTextVo>> r = service.batchVideoAudioText(bo);

        List<VideoAudioTextVo> list = r.getData();
        assertEquals("原文-h100", list.get(0).getOriginalAudioContent());
        assertEquals("AI-h100", list.get(0).getAudioContent());
        assertNull(list.get(1).getOriginalAudioContent());
        assertNull(list.get(1).getAudioContent());
    }

    @Test
    @DisplayName("批量明细：withAudioText=true 时也只给本租户提取成功的视频文案")
    void videoDetailBatch_unauthorizedVideo_returnsNullContent() {
        givenTenantExtracted(100L);
        when(videoInfoDao.selectBatchIds(any())).thenReturn(List.of(video(100L, "h100"), video(200L, "h200")));
        when(videoExtractContentRepository.findByVideoHashIn(any())).thenReturn(List.of(extract("h100")));

        VideoDetailBatchBo bo = identity(new VideoDetailBatchBo());
        bo.setVideoIdList(List.of(100L, 200L));
        bo.setWithAudioText(true);
        R<List<InfluencerVideoItemVo>> r = service.videoDetailBatch(bo);

        assertEquals("原文-h100", r.getData().get(0).getOriginalAudioContent());
        assertNull(r.getData().get(1).getOriginalAudioContent());
    }

    @Test
    @DisplayName("批量统计 collectedOnly=true：未订阅达人各项统计为 0，且不回源统计查询")
    void batchInfluencerVideoStats_notSubscribed_returnsZeroStats() {
        when(videoInfluencerInfoDao.selectSubscribedInfluencerIds(eq(TENANT_ID), isNull(), any()))
                .thenReturn(List.of());   // 一个都没订阅

        InfluencerVideoStatsBatchBo bo = identity(new InfluencerVideoStatsBatchBo());
        bo.setInfluencerIdList(List.of(7L, 8L));
        bo.setCollectedOnly(true);
        R<List<InfluencerVideoStatsVo>> r = service.batchInfluencerVideoStats(bo);

        List<InfluencerVideoStatsVo> list = r.getData();
        assertEquals(2, list.size());
        assertEquals(7L, list.get(0).getInfluencerId());
        assertEquals(0, list.get(0).getVideoCount());
        assertEquals(0L, list.get(1).getTotalLikeCount());
        verify(videoInfoDao, never()).batchStatsByAuthorIds(any(), any(), any());
    }

    @Test
    @DisplayName("统计概览 scope=1：达人未被本租户订阅时返回全零，不查全库作品")
    void influencerAnalyticsOverview_notSubscribed_returnsZeroAnalytics() {
        when(videoInfluencerInfoDao.selectSubscribedInfluencerIds(eq(TENANT_ID), isNull(), any()))
                .thenReturn(List.of());

        InfluencerAnalyticsOverviewBo bo = identity(new InfluencerAnalyticsOverviewBo());
        bo.setInfluencerId(7L);
        bo.setScope((byte) 1);
        R<InfluencerAnalyticsVo> r = service.influencerAnalyticsOverview(bo);

        InfluencerAnalyticsVo vo = r.getData();
        assertEquals(0, vo.getSampleCount());
        assertEquals(0L, vo.getAvgEngagement());
        assertTrue(vo.getTrendSeries().isEmpty());
        verify(videoInfoDao, never()).listAuthorClipMetrics(any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("统计概览 scope=2：全库口径不校验订阅关系，直接查全库作品")
    void influencerAnalyticsOverview_globalScope_skipsSubscriptionCheck() {
        when(videoInfoDao.listAuthorClipMetrics(any(), any(), any(), any(), anyInt())).thenReturn(List.of());

        InfluencerAnalyticsOverviewBo bo = identity(new InfluencerAnalyticsOverviewBo());
        bo.setInfluencerId(7L);
        bo.setScope((byte) 2);
        service.influencerAnalyticsOverview(bo);

        verify(videoInfluencerInfoDao, never()).selectSubscribedInfluencerIds(any(), any(), any());
        verify(videoInfoDao).listAuthorClipMetrics(eq("7"), isNull(), any(), any(), anyInt());
    }
}
