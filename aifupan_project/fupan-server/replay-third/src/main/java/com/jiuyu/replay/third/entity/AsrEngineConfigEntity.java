package com.jiuyu.replay.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ASR 引擎按租户 / 用户 / 语言优先级配置实体。
 *
 * <p>对应表 {@code tb_asr_engine_config}。命中优先级（高 → 低）：
 * userId+language &gt; userId &gt; tenant+language &gt; tenant；
 * 全部未命中时由调用方兜底返回 {@code sense-voice}。</p>
 *
 * <p>哨兵值约定：{@link #userId} 为 {@code 0} 表示租户级默认（不绑定具体用户）；
 * {@link #language} 为空串表示不限语言。两者用于在唯一键
 * {@code uk_tenant_user_lang} 上保留可去重的真实值，
 * 避免 NULL 在 MySQL 唯一键中被多次插入。</p>
 *
 * @author hehh
 * @date 2026-05-25
 */
@Data
@TableName("tb_asr_engine_config")
public class AsrEngineConfigEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，雪花 ID。
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 租户 ID。
     */
    private Long tenantId;

    /**
     * 用户 ID；{@code 0} 表示租户级默认配置（不绑定具体用户）。
     */
    private Long userId;

    /**
     * 语言编码；空串表示不限语言。
     */
    private String language;

    /**
     * ASR 引擎列表，逗号分隔，顺序即返回顺序。
     */
    private String engines;

    /**
     * 备注。
     */
    private String remarks;

    /**
     * 软删除标记：0 正常 1 已删除。
     */
    private Integer isDeleted;

    /**
     * 创建时间。
     */
    private LocalDateTime createDate;

    /**
     * 最后修改时间。
     */
    private LocalDateTime updateDate;
}
