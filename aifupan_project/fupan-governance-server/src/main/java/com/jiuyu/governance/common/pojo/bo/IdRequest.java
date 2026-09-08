package com.jiuyu.governance.common.pojo.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * id参数
 *
 * @author HeHui
 * @date 2026-03-18 14:27
 */
@Getter
@Setter
public class IdRequest {

    @NotNull(message = "缺少ID")
    private Long id;
}
