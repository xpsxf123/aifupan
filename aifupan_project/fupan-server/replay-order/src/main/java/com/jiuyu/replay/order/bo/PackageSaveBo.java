package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 版本保存参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "版本保存参数")
public class PackageSaveBo extends PackageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 增量包列表
     */
    @Schema(description = "增量包列表")
    private List<IncrementBo> incrementList;
}
