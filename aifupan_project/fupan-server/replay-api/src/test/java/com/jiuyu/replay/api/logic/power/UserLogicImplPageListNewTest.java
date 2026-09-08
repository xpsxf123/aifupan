package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.agent.bll.InviteUrlCodeBll;
import com.jiuyu.replay.agent.producer.ChannelProducer;
import com.jiuyu.replay.api.logic.power.impl.UserLogicImpl;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.power.bll.UserRemarkBll;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.LatestRemarkVo;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.words.producer.TradeProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link UserLogicImpl#pageListNew} 三字段装配单测
 *
 * <p>覆盖 AC-1（三字段装配）、AC-2（N+1 红线：latestRemark 恰 1 次批量、算力零额外往返）、
 * AC-3（无算力流水 / 无跟进的兜底）。</p>
 *
 * <p>算力两字段由 {@code UserMapper.xml#pageListNew} 的 LEFT JOIN 随主查询带出，
 * Java 侧只做 {@code defaultIfNull(..., 0L)} 兜底；本单测通过 mock {@code userBll.pageListNew}
 * 的返回值模拟 SQL 带出/未带出两种情形。</p>
 *
 * @author test-engineer
 * @date 2026-07-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserLogicImplPageListNewTest {

    @Mock
    private UserBll userBll;

    @Mock
    private UserRemarkBll userRemarkBll;

    @Mock
    private TradeProducer tradeProducer;

    @Mock
    private ChannelProducer channelProducer;

    @Mock
    private OrderFeign orderFeign;

    @Mock
    private InviteUrlCodeBll inviteUrlCodeBll;

    /**
     * 旧方案（已作废）的算力取数通道，用于断言在线读不再走任何 Java 跨模块调用
     */
    @Mock
    private UserPropertyFeign userPropertyFeign;

    @Mock
    private UserPropertyBll userPropertyBll;

    @InjectMocks
    private UserLogicImpl userLogic;

    @BeforeEach
    void setUp() {
        // pageListNew 从 ThreadLocal 取当前登录人；平台管理员(adminUserType=0)不走代理商分支
        UserCacheVo admin = new UserCacheVo();
        admin.setId(1L);
        admin.setAdminUserType(0);
        GlobalObject.setLocalUser(admin);
    }

    @AfterEach
    void tearDown() {
        GlobalObject.removeLocalUser();
    }

    // ---------- 辅助方法 ----------

    /**
     * 构造一行列表数据。算力两字段传 null 表示 rollup 表未命中（LEFT JOIN 取空）。
     */
    private UserListVo row(Long id, Long parentId, Long userPower, Long tenantPower) {
        UserListVo vo = new UserListVo();
        vo.setId(id);
        vo.setParentId(parentId);
        vo.setUserPowerConsume(userPower);
        vo.setTenantPowerConsume(tenantPower);
        return vo;
    }

    /**
     * 把若干行包成分页返回值。
     */
    private PageUtils<UserListVo> page(UserListVo... rows) {
        PageUtils<UserListVo> pageUtils = new PageUtils<>();
        pageUtils.setList(new ArrayList<>(Arrays.asList(rows)));
        pageUtils.setTotalCount(rows.length);
        return pageUtils;
    }

    /**
     * 构造一条最新跟进。
     */
    private LatestRemarkVo remarkVo(String remark, Date time, String createName) {
        LatestRemarkVo vo = new LatestRemarkVo();
        vo.setRemark(remark);
        vo.setRemarkTime(time);
        vo.setCreateName(createName);
        return vo;
    }

    // ========== AC-1：三字段装配 ==========

    /**
     * AC-1：SQL 已带出算力两字段 + 批量取到跟进 → 三字段值与造数一致。
     */
    @Test
    @DisplayName("AC-1 三字段装配：算力取 SQL 带出值，latestRemark 取批量结果")
    void pageListNew_assemblesThreeFieldsFromSqlAndBatchRemark() {
        // Given：两行，均已由 LEFT JOIN 带出算力值
        Date remarkTime = new Date(1_760_000_000_000L);
        PageUtils<UserListVo> raw = page(row(1001L, 0L, 123456L, 987654L),
                row(1002L, 1001L, 5000L, 987654L));
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(raw);

        Map<Long, LatestRemarkVo> remarkMap = new HashMap<>();
        remarkMap.put(1001L, remarkVo("客户已确认续费意向", remarkTime, "张三"));
        when(userRemarkBll.batchLatestByUserIds(anyCollection())).thenReturn(remarkMap);

        // When
        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        // Then
        List<UserListVo> list = result.getData().getList();
        assertEquals(2, list.size());

        UserListVo first = list.get(0);
        assertEquals(123456L, first.getUserPowerConsume());
        assertEquals(987654L, first.getTenantPowerConsume());
        assertNotNull(first.getLatestRemark());
        assertEquals("客户已确认续费意向", first.getLatestRemark().getRemark());
        assertEquals(remarkTime, first.getLatestRemark().getRemarkTime());
        assertEquals("张三", first.getLatestRemark().getCreateName());

        // 子账号行：算力取自身值，租户值与主账号同租户一致；无跟进 → latestRemark 为 null
        UserListVo second = list.get(1);
        assertEquals(5000L, second.getUserPowerConsume());
        assertEquals(987654L, second.getTenantPowerConsume());
        assertNull(second.getLatestRemark());
    }

    /**
     * AC-4：latestRemark 以用户自身 id 为 key，不得错用主账号 id（packageUserId）。
     */
    @Test
    @DisplayName("AC-4 latestRemark 按用户自身 id 取，不用主账号 id")
    void pageListNew_latestRemarkKeyedByOwnUserIdNotParentId() {
        // Given：子账号 2002 的父账号是 2001，两人各有一条跟进
        PageUtils<UserListVo> raw = page(row(2002L, 2001L, 100L, 900L));
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(raw);

        Map<Long, LatestRemarkVo> remarkMap = new HashMap<>();
        remarkMap.put(2001L, remarkVo("父账号的跟进", new Date(), "父跟进人"));
        remarkMap.put(2002L, remarkVo("子账号的跟进", new Date(), "子跟进人"));
        when(userRemarkBll.batchLatestByUserIds(anyCollection())).thenReturn(remarkMap);

        // When
        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        // Then
        UserListVo row = result.getData().getList().get(0);
        assertEquals("子账号的跟进", row.getLatestRemark().getRemark());
        // 用户算力取自身，不并入父账号
        assertEquals(100L, row.getUserPowerConsume());
        assertEquals(900L, row.getTenantPowerConsume());
    }

    // ========== AC-3：兜底 ==========

    /**
     * AC-3：rollup 表未命中（算力为 null）+ 无跟进 → 两算力字段兜底 0L，latestRemark 为 null，不抛异常。
     */
    @Test
    @DisplayName("AC-3 无算力流水 / 无跟进：算力兜底 0L，latestRemark 为 null，不抛异常")
    void pageListNew_noRollupRowAndNoRemark_defaultsToZeroAndNull() {
        // Given：LEFT JOIN 未命中 → 两字段为 null；无任何跟进
        PageUtils<UserListVo> raw = page(row(3001L, 0L, null, null));
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(raw);
        when(userRemarkBll.batchLatestByUserIds(anyCollection())).thenReturn(new HashMap<>());

        // When
        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        // Then
        UserListVo row = result.getData().getList().get(0);
        assertEquals(0L, row.getUserPowerConsume());
        assertEquals(0L, row.getTenantPowerConsume());
        assertNull(row.getLatestRemark());
    }

    /**
     * AC-3：只有租户侧命中、用户侧未命中 → 用户算力兜底 0L，租户算力保留真实值。
     */
    @Test
    @DisplayName("AC-3 仅租户侧命中：用户算力兜底 0L，租户算力保留原值")
    void pageListNew_onlyTenantRollupHit_userPowerDefaultsToZero() {
        PageUtils<UserListVo> raw = page(row(3002L, 0L, null, 66666L));
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(raw);

        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        UserListVo row = result.getData().getList().get(0);
        assertEquals(0L, row.getUserPowerConsume());
        assertEquals(66666L, row.getTenantPowerConsume());
    }

    /**
     * AC-3：批量取跟进抛异常 → 降级为空，列表主数据与算力字段照常返回，不阻断接口。
     */
    @Test
    @DisplayName("AC-3 跟进查询异常：降级为空，不阻断列表与算力字段")
    void pageListNew_remarkQueryThrows_degradesGracefully() {
        PageUtils<UserListVo> raw = page(row(3003L, 0L, 777L, 888L));
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(raw);
        when(userRemarkBll.batchLatestByUserIds(anyCollection()))
                .thenThrow(new RuntimeException("模拟跟进查询失败"));

        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        UserListVo row = result.getData().getList().get(0);
        assertNull(row.getLatestRemark());
        assertEquals(777L, row.getUserPowerConsume());
        assertEquals(888L, row.getTenantPowerConsume());
    }

    /**
     * AC-3：空列表 → 直接返回，不发跟进批量查询，不抛异常。
     */
    @Test
    @DisplayName("AC-3 空列表：不发跟进批量查询")
    void pageListNew_emptyList_skipsRemarkBatchQuery() {
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(new PageUtils<>());

        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        assertNotNull(result.getData());
        verifyNoInteractions(userRemarkBll);
    }

    // ========== AC-2：N+1 红线 ==========

    /**
     * AC-2：装配 N 行时 batchLatestByUserIds 恰被调用 1 次（for-loop 内无查询）。
     */
    @Test
    @DisplayName("AC-2 N+1 红线：batchLatestByUserIds 恰被调用 1 次")
    void pageListNew_nRows_callsBatchLatestByUserIdsExactlyOnce() {
        // Given：30 行
        int n = 30;
        UserListVo[] rows = new UserListVo[n];
        for (int i = 0; i < n; i++) {
            rows[i] = row(4000L + i, 0L, (long) i, 100L);
        }
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(page(rows));
        when(userRemarkBll.batchLatestByUserIds(anyCollection())).thenReturn(new HashMap<>());

        // When
        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        // Then：装配 30 行，跟进批量查询只发 1 次
        assertEquals(n, result.getData().getList().size());
        verify(userRemarkBll, times(1)).batchLatestByUserIds(anyCollection());
        // 主查询也只发 1 次
        verify(userBll, times(1)).pageListNew(any(UserListBo.class));
    }

    /**
     * AC-2：算力两字段零额外 DB 往返 —— 装配路径不得再走任何算力取数通道
     * （旧方案的 UserPropertyFeign / UserPropertyBll 均须零交互）。
     */
    @Test
    @DisplayName("AC-2 算力零额外往返：不调用任何算力取数通道")
    void pageListNew_powerFields_issueNoAdditionalQueries() {
        UserListVo[] rows = new UserListVo[10];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = row(5000L + i, 0L, (long) (i * 100), 9999L);
        }
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(page(rows));
        when(userRemarkBll.batchLatestByUserIds(anyCollection())).thenReturn(new HashMap<>());

        R<PageUtils<UserListVo>> result = userLogic.pageListNew(new UserListBo());

        // 算力值原样来自主查询
        List<UserListVo> list = result.getData().getList();
        assertEquals(0L, list.get(0).getUserPowerConsume());
        assertEquals(900L, list.get(9).getUserPowerConsume());
        assertEquals(9999L, list.get(9).getTenantPowerConsume());

        // 装配过程零算力取数调用
        verifyNoInteractions(userPropertyFeign);
        verifyNoInteractions(userPropertyBll);
    }

    /**
     * AC-2：其余批量装配通道（行业 / 渠道 / 订单 / 渠道明细）各只调用 1 次，for-loop 内无查询。
     */
    @Test
    @DisplayName("AC-2 其余批量通道各只调用 1 次，for-loop 内无查询")
    void pageListNew_otherBatchChannels_calledExactlyOnce() {
        UserListVo[] rows = new UserListVo[15];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = row(6000L + i, 0L, 1L, 2L);
        }
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(page(rows));
        when(userRemarkBll.batchLatestByUserIds(anyCollection())).thenReturn(new HashMap<>());

        userLogic.pageListNew(new UserListBo());

        verify(tradeProducer, times(1)).listByIds(anyCollection());
        verify(channelProducer, times(1)).listByIds(anyCollection());
        verify(orderFeign, times(1)).currentOrderByUserIds(any());
        verify(inviteUrlCodeBll, times(1)).inviteCodeAndPromotionName(any());
    }

    // ========== AC-6：其它端点不受影响 ==========

    /**
     * AC-6：三新字段是 additive 的普通 VO 字段，未经 pageListNew 装配时保持 null，
     * 复用 UserListVo 的其它端点（pageList / subAccountList / listByIds 等）行为不变。
     */
    @Test
    @DisplayName("AC-6 未经 pageListNew 装配的 UserListVo：三新字段保持 null")
    void userListVo_withoutPageListNewAssembly_keepsThreeNewFieldsNull() {
        UserListVo vo = new UserListVo();
        vo.setId(7001L);

        assertNull(vo.getUserPowerConsume());
        assertNull(vo.getTenantPowerConsume());
        assertNull(vo.getLatestRemark());
    }

    /**
     * AC-2 补充：传给 batchLatestByUserIds 的 userIds 就是本页用户 id 全集（不含父账号 id）。
     */
    @Test
    @DisplayName("AC-2 批量入参为本页用户 id 全集，不含父账号 id")
    void pageListNew_passesOnlyPageUserIdsToRemarkBatch() {
        PageUtils<UserListVo> raw = page(row(8001L, 8000L, 1L, 2L), row(8002L, 8000L, 3L, 4L));
        when(userBll.pageListNew(any(UserListBo.class))).thenReturn(raw);
        when(userRemarkBll.batchLatestByUserIds(anyCollection())).thenReturn(new HashMap<>());

        userLogic.pageListNew(new UserListBo());

        verify(userRemarkBll).batchLatestByUserIds(ArgumentMatchers.<Collection<Long>>argThat(actual ->
                actual != null && actual.size() == 2 && actual.containsAll(Arrays.asList(8001L, 8002L))));
    }
}
