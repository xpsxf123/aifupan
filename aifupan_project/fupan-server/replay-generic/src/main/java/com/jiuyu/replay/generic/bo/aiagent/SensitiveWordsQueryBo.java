package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行业敏感词库查询入参（身份三件套 + 行业 ID + 平台）。
 *
 * <p>面向 AI Agent 输出话术前的合规自检：按行业层级取「4 级（自身）+ 3 级 + 2 级 + 1 级 + 全行业」的系统敏感词，
 * 供模型规避。取词口径为<b>父链向上回溯</b>——给定 {@code tradeId}（通常为最细的 4 级行业），
 * 服务端递归其所有父级行业并叠加全行业(id=1)，一次性返回链上全部启用敏感词。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SensitiveWordsQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 起点行业 ID（通常为最细一级，如 4 级行业）。服务端据此回溯父链 + 全行业取词。
     */
    @NotNull(message = "tradeId不能为空")
    private Long tradeId;

    /**
     * 平台编号（AI Agent 取数约定）：0=抖音 1=快手 2=视频号；为空表示不限平台（返回全平台 + 各平台专属词的并集）。
     *
     * <p>注意：内部敏感词库的 {@code platform_type} 约定为 0=全平台 1=抖音 2=快手 3=视频号，
     * 与本字段相差 1，服务端做 {@code platform_type IN (0, platform + 1)} 映射，调用方无需关心。</p>
     */
    private Integer platform;

    /**
     * 词语类型：0=敏感词（默认）1=关键词。为空按 0 处理。仅取系统词（resource_type=0）。
     */
    private Integer wordsType;
}
