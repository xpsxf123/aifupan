package com.jiuyu.replay.order.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/4/7 10:42
 */
@Data
public class ClientVersionConfigBo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 版本未知
     */
    @NotNull(message = "版本不能为空")
    private String clientVersion;

    private Long userId;
}
