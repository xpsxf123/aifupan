package com.jiuyu.replay.video.project.vo.hotsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 爆款搜索历史记录视图对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 爆款搜索历史记录显示数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索历史记录视图对象")
public class VideoHotSearchHistoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索历史ID
     */
    @Schema(description = "搜索历史ID", example = "1001")
    private Long historyId;

    /**
     * 关键词
     */
    @Schema(description = "关键词", example = "列表去掉原先的<更新>，左边返回新增文案")
    private String keyword;

    /**
     * 搜索结果数量
     */
    @Schema(description = "搜索结果数量", example = "54")
    private Integer resultCount;

    /**
     * 搜索时间
     */
    @Schema(description = "搜索时间", example = "2025-06-12")
    private LocalDate searchTime;
}
