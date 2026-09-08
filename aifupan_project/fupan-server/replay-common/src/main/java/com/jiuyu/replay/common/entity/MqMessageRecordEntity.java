package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * MQ消息记录表
 * </p>
 *
 * @author RayChou
 * @since 2025-06-03
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_mq_message_record")
public class MqMessageRecordEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId("id")
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 消息主题
     */
    @TableField("message_topic")
    private String messageTopic;

    /**
     * 消息标签
     */
    @TableField("message_tag")
    private String messageTag;

    /**
     * 消息id
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 消息key
     */
    @TableField("message_key")
    private String messageKey;

    /**
     * 消息内容
     */
    @TableField("message_body")
    private String messageBody;

    /**
     * 消息状态：0：待发送 1：已发送 2：已消费 3：发送失败 4：消费失败
     */
    @TableField("message_status")
    private Byte messageStatus;

    /**
     * 发送重试次数 发送重试10次后状态转为发送失败
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 下次重试时间
     */
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    /**
     * 延时发送时间（为null表示立即发送，不为null表示延时到指定时间发送）
     */
    @TableField("delay_send_time")
    private LocalDateTime delaySendTime;

    /**
     * 创建时间
     */
    @TableField("create_date")
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    @TableField("update_date")
    private LocalDateTime updateDate;

    /**
     * 是否已删除
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
