package com.jiuyu.replay.ai.task;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorReportZombieHandler 单测（B4 修补包-3 P2 僵尸恢复）。
 *
 * <p>覆盖：happy 扫到僵尸改 FAILED / 无僵尸跳过 / 锁获取失败跳过。</p>
 *
 * @author beta
 * @date 2026-06-01
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorReportZombieHandlerTest {

    @Mock
    private ScriptMonitorReportService reportService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @InjectMocks
    private ScriptMonitorReportZombieHandler handler;

    @BeforeEach
    void setUp() {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
    }

    @Test
    @DisplayName("happy: 扫到 2 条僵尸 → 改 status=3 + 写 reason")
    void scansAndRecoversZombies() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        ScriptMonitorReportEntity z1 = newZombie(1L);
        ScriptMonitorReportEntity z2 = newZombie(2L);
        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mock(LambdaQueryChainWrapper.class);
        when(reportService.lambdaQuery()).thenReturn(q);
        when(q.eq(any(), any())).thenReturn(q);
        when(q.lt(any(), any())).thenReturn(q);
        when(q.last(anyString())).thenReturn(q);
        when(q.list()).thenReturn(Arrays.asList(z1, z2));
        when(reportService.updateById(any())).thenReturn(true);

        handler.recoverZombieReports();

        verify(reportService, times(2)).updateById(any());
        assertEquals(ScriptMonitorStatusEnum.GENERATE_FAILED.getCode(), z1.getStatus());
        assertEquals(ScriptMonitorStatusEnum.GENERATE_FAILED.getCode(), z2.getStatus());
    }

    @Test
    @DisplayName("无僵尸 → 不调 updateById")
    void noZombies_skipsUpdate() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mock(LambdaQueryChainWrapper.class);
        when(reportService.lambdaQuery()).thenReturn(q);
        when(q.eq(any(), any())).thenReturn(q);
        when(q.lt(any(), any())).thenReturn(q);
        when(q.last(anyString())).thenReturn(q);
        when(q.list()).thenReturn(Collections.emptyList());

        handler.recoverZombieReports();

        verify(reportService, never()).updateById(any());
    }

    @Test
    @DisplayName("锁获取失败 → 直接退出，不查表")
    void lockFailed_returnsImmediately() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        handler.recoverZombieReports();

        verify(reportService, never()).lambdaQuery();
        verify(reportService, never()).updateById(any());
    }

    private ScriptMonitorReportEntity newZombie(Long id) {
        ScriptMonitorReportEntity e = new ScriptMonitorReportEntity();
        e.setId(id);
        e.setStatus(ScriptMonitorStatusEnum.GENERATING.getCode());
        return e;
    }
}
