package com.jiuyu.replay.generic.bo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页请求对象")
public class PageBo {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页
     */
    @Schema(description = "当前页")
    private Integer page = 1;
    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数")
    private Integer limit = 10;
}
