package com.jiuyu.replay.words.vo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DataViewingDetailVo {

    /**
     * 展示label
     */
    @Schema(description = "展示label")
    private String label;
    /**
     * 值
     */
    @Schema(description = "值")
    private Double value;
}
