package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：liwj
 * &#064;description：更新用户资产的vo
 * @date ：2025/9/16 16:21
 */
@Data
@Schema(description = "更新用户资产的vo")
public class UpdateUserPropertyVo implements Serializable {
    private static final long serialVersionUID = 1L;


    private Long userId;

    private String code;

    private Long quantity;

}
