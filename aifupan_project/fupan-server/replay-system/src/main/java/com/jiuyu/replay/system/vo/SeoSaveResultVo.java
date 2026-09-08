package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存结果。
 *
 * <p>必须回传实际入库的 slug——它可能因冲突被追加了随机数，
 * 而这是运营唯一一次得知最终 URL 的机会（前端据此提示）。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "保存结果")
public class SeoSaveResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Schema(description = "主键")
    private Long id;

    /** 实际入库的 slug */
    @Schema(description = "实际入库的 slug")
    private String slug;

    /** 是否因冲突被追加了随机数后缀 */
    @Schema(description = "是否因冲突被追加了随机数后缀")
    private Boolean slugAppended;
}
