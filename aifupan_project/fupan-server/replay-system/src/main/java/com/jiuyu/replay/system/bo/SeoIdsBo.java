package com.jiuyu.replay.system.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量删除入参。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@Schema(description = "批量删除入参")
public class SeoIdsBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键集合 */
    @Schema(description = "主键集合")
    private List<Long> ids;
}
