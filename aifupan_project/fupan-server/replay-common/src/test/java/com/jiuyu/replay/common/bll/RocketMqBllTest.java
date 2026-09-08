package com.jiuyu.replay.common.bll;

import com.jiuyu.replay.common.entity.MqMessageRecordEntity;
import com.jiuyu.replay.common.repository.service.MqMessageRecordService;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RocketMqBll 单测（B7）
 *
 * <p>覆盖 syncSendAndDeliverToTopic 的直发成功 / 直发失败保留 status=0 / save 失败三个场景。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RocketMqBllTest {

    @Mock
    private MqMessageRecordService mqMessageRecordService;

    @Mock
    private RocketMQClientTemplate rocketMQClientTemplate;

    @InjectMocks
    private RocketMqBll rocketMqBll;

    @Test
    @DisplayName("syncSendAndDeliverToTopic 直发成功时改 status=1 并写入 messageId")
    void syncSendAndDeliverToTopic_directSendSuccess_statusChangedTo1() {
        // given
        when(mqMessageRecordService.save(any())).thenReturn(true);
        SendReceipt receipt = mock(SendReceipt.class);
        MessageId messageId = mock(MessageId.class);
        when(messageId.toString()).thenReturn("msg-id-001");
        when(receipt.getMessageId()).thenReturn(messageId);
        when(rocketMQClientTemplate.syncSendNormalMessage(anyString(), any(Message.class))).thenReturn(receipt);

        // when
        boolean result = rocketMqBll.syncSendAndDeliverToTopic(100L, "topic1", "tag1", "key1", "body1");

        // then
        assertThat(result).isTrue();
        verify(mqMessageRecordService).save(any());
        verify(mqMessageRecordService).updateById(argThat(record ->
                record.getMessageStatus() == (byte) 1 && "msg-id-001".equals(record.getMessageId())));
    }

    @Test
    @DisplayName("syncSendAndDeliverToTopic 直发失败时不抛异常 status 保留 0 不调 updateById")
    void syncSendAndDeliverToTopic_directSendFailure_keepsStatus0NoException() {
        // given
        when(mqMessageRecordService.save(any())).thenReturn(true);
        when(rocketMQClientTemplate.syncSendNormalMessage(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("网络超时"));

        // when
        boolean result = rocketMqBll.syncSendAndDeliverToTopic(100L, "topic1", "tag1", "key1", "body1");

        // then：直发失败不抛、事务正常提交返回 true
        assertThat(result).isTrue();
        verify(mqMessageRecordService).save(any());
        // 直发失败不调 updateById（status=0 保留等 XXL-Job 兜底）
        verify(mqMessageRecordService, never()).updateById(any(MqMessageRecordEntity.class));
    }

    @Test
    @DisplayName("syncSendAndDeliverToTopic save 失败时立即返回 false 且不调 RocketMQClientTemplate")
    void syncSendAndDeliverToTopic_saveFailure_returnsFalseSkipsMq() {
        // given
        when(mqMessageRecordService.save(any())).thenReturn(false);

        // when
        boolean result = rocketMqBll.syncSendAndDeliverToTopic(100L, "topic1", "tag1", "key1", "body1");

        // then
        assertThat(result).isFalse();
        verify(rocketMQClientTemplate, never()).syncSendNormalMessage(anyString(), any(Message.class));
        verify(mqMessageRecordService, never()).updateById(any(MqMessageRecordEntity.class));
    }
}
