package com.jiuyu.replay.third.tablestore.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/16 下午5:40
 */
@Data
public class RangeBase implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "每页记录数(默认50)，queryType为0时向上查询limit条，为1时向下查询limit条，为2时上下总查询 limit * 2 条; ")
    private Integer limit = 50;

    @Schema(description = "查询类型(默认1)，0向上查询，1向下查询，2上下都查询; 上下查询必须要传recordDate参数")
    private Integer queryType = 1;

}
