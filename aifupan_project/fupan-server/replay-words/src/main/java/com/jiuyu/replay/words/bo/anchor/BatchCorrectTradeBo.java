package com.jiuyu.replay.words.bo.anchor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量修正所属行业
 *
 * @author HeHui
 * @date 2026-01-30 18:26
 */
@Getter
@Setter
public class BatchCorrectTradeBo {

    /**
     * 榜单列表返回的  similarCollectId
     */
    @NotEmpty(message = "请选择热榜主播")
    @Size(max = 100, message = "最多选择100个热榜主播")
    private List<Long> similarCollectIds;
    /**
     * 行业id
     */
    @NotNull(message = "行业id不能为空")
    private Long tradeId;
}
