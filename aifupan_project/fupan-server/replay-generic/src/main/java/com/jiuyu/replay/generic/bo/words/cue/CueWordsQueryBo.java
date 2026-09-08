package com.jiuyu.replay.generic.bo.words.cue;

import lombok.Data;

import java.io.Serializable;

/**
 * 按行业 + 助手类型 + 租户取单条提示词的查询入参 Bo。
 *
 * <p>服务于 {@link com.jiuyu.replay.generic.feign.words.CueWordsFeign#getCueWordByTradeAndType}
 * —— "行业父链回溯 + 兜底 1L + 同层级租户定制赢通用" 语义。
 * {@code tradeId / tenantId / cueType} 由调用方按业务场景设置，其他维度提供合理默认
 * （scope=0 全文 / applyTo=0 单个分析 / accountType=0 自由账号），调用方按需覆盖。</p>
 *
 * <p>命中规则：</p>
 * <ul>
 *   <li>同行业层级：{@code tenant_id = tenantId} 赢 {@code tenant_id = 0}（SQL ORDER BY tenant_id DESC）</li>
 *   <li>跨行业层级：子层级赢父层级（API 层按 tradeIds 顺序选首个有数据的行业）</li>
 *   <li>{@code tenantId = null} 或 {@code 0}：退化为只查通用</li>
 * </ul>
 *
 * @author beta
 * @date 2026-06-05
 */
@Data
public class CueWordsQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行业 id（直播间维度 tb_anchor_url_user.trade_id）；null 时仅走兜底 trade_id=1
     */
    private Long tradeId;

    /**
     * 租户 id；null 或 0 时只查通用提示词（tenant_id=0）
     */
    private Long tenantId;

    /**
     * 助手类型（参见 com.jiuyu.replay.common.constant.AiEnums.askType，
     * 16=话术质检生成 / 17=话术质检合并 等）
     */
    private Integer cueType;

    /**
     * 范围 0:全文，1:段落；默认 0 全文
     */
    private Integer scope = 0;

    /**
     * 提示词用于 0:单个分析，1:对比分析；默认 0 单个分析
     */
    private Integer applyTo = 0;

    /**
     * 账号归属类型 0:自由账号，1:同行账号；默认 0 自由账号
     */
    private Integer accountType = 0;

    /**
     * 对比使用场景 1:对比上一次场、2:不同直播间对比、3:同直播间对比；null 不参与过滤
     */
    private Integer syncScene;


    private Integer limit = 15;
}
