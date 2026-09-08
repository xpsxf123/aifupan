package com.jiuyu.replay.power.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class RoleListBo extends PageBo {

    /**
     * 模糊搜索查询条件
     */
    @Schema(description = "模糊搜索查询条件")
    private String keyword;
}
