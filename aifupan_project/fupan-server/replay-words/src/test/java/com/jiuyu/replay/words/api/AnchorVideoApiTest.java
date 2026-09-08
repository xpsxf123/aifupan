package com.jiuyu.replay.words.api;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.entity.SocketCollectMessageEntity;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.repository.service.SocketCollectMessageService;
import com.jiuyu.replay.words.rse.AnchorVideoRse;
import com.jiuyu.replay.words.rse.VideoSliceRse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link AnchorVideoApi#hasBarrage(String)} 单测。
 *
 * <p>2026-06-09 新增 SPI，修复手动触发互动巡检报错"本场无弹幕数据"的 bug。
 * 根因：{@code AnchorVideoInfoVo.existBarrage} 是运行时聚合字段，
 * {@code AnchorVideoProducerImpl.getByVideoId} 走 {@code BeanUtils.copyProperties} 不补该字段，
 * 导致单视频查询场景下 {@code video.getExistBarrage()} 始终 null。
 * 改由本 SPI 显式查 {@code tb_socket_collect_message.total_barrage_num &gt; 0} 实时判定。</p>
 *
 * <p>实现层级：跳过 {@code SocketCollectMessageProducer} 直注 {@code SocketCollectMessageService}，
 * 避免与 {@code TableStoreBll → anchorVideoFeign} 形成循环依赖（hotfix 2026-06-09）。</p>
 *
 * <p>AC-4 覆盖 4 种 socket 数据状态：
 * <ul>
 *   <li>查无记录 → 未采集到 → false</li>
 *   <li>totalBarrageNum=null → 字段空 → false</li>
 *   <li>totalBarrageNum=0 → 已采集但无弹幕 → false</li>
 *   <li>totalBarrageNum&gt;0 → 有弹幕 → true</li>
 * </ul>
 * </p>
 *
 * @author beta
 * @date 2026-06-09
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnchorVideoApiTest {

    @Mock
    private AnchorVideoProducer anchorVideoProducer;
    @Mock
    private AnchorVideoRse anchorVideoRse;
    @Mock
    private VideoSliceRse videoSliceRse;
    @Mock
    private SocketCollectMessageService socketCollectMessageService;

    @InjectMocks
    private AnchorVideoApi anchorVideoApi;

    // -------- AC-4 hasBarrage 边界覆盖 --------

    @Test
    @DisplayName("AC-4.1: SocketCollectMessageService.getOne 查无记录（未采集到 socket 数据）→ false")
    void hasBarrage_socketInfoNull_returnsFalse() {
        when(socketCollectMessageService.getOne(any(Wrapper.class))).thenReturn(null);

        R<Boolean> result = anchorVideoApi.hasBarrage("v1");

        assertThat(result).isNotNull();
        assertThat(result.getData()).isFalse();
    }

    @Test
    @DisplayName("AC-4.2: entity.totalBarrageNum=null（字段未填）→ false（不抛 NPE）")
    void hasBarrage_totalBarrageNumNull_returnsFalse_noNPE() {
        SocketCollectMessageEntity entity = new SocketCollectMessageEntity();
        entity.setTotalBarrageNum(null);
        when(socketCollectMessageService.getOne(any(Wrapper.class))).thenReturn(entity);

        R<Boolean> result = anchorVideoApi.hasBarrage("v1");

        assertThat(result).isNotNull();
        assertThat(result.getData()).isFalse();
    }

    @Test
    @DisplayName("AC-4.3: entity.totalBarrageNum=0（已采集但无弹幕）→ false")
    void hasBarrage_totalBarrageNumZero_returnsFalse() {
        SocketCollectMessageEntity entity = new SocketCollectMessageEntity();
        entity.setTotalBarrageNum(0);
        when(socketCollectMessageService.getOne(any(Wrapper.class))).thenReturn(entity);

        R<Boolean> result = anchorVideoApi.hasBarrage("v1");

        assertThat(result).isNotNull();
        assertThat(result.getData()).isFalse();
    }

    @Test
    @DisplayName("AC-4.4: entity.totalBarrageNum>0（有弹幕）→ true")
    void hasBarrage_totalBarrageNumPositive_returnsTrue() {
        SocketCollectMessageEntity entity = new SocketCollectMessageEntity();
        entity.setTotalBarrageNum(42);
        when(socketCollectMessageService.getOne(any(Wrapper.class))).thenReturn(entity);

        R<Boolean> result = anchorVideoApi.hasBarrage("v1");

        assertThat(result).isNotNull();
        assertThat(result.getData()).isTrue();
    }

    /**
     * AC-4.5（边界补充）：totalBarrageNum=1（恰好阈值）→ true。
     * 防止意外的 {@code &gt;= 0} 或 {@code &gt; 1} 误改实现。
     */
    @Test
    @DisplayName("AC-4.5: entity.totalBarrageNum=1（边界）→ true")
    void hasBarrage_totalBarrageNumOne_returnsTrue() {
        SocketCollectMessageEntity entity = new SocketCollectMessageEntity();
        entity.setTotalBarrageNum(1);
        when(socketCollectMessageService.getOne(any(Wrapper.class))).thenReturn(entity);

        R<Boolean> result = anchorVideoApi.hasBarrage("v1");

        assertThat(result.getData()).isTrue();
    }
}
