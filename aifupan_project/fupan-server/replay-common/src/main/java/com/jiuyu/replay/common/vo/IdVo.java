package com.jiuyu.replay.common.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当个ID参数
 *
 * @author HeHui
 * @date 2025-06-26 18:26
 */
@Getter
@Setter
public class IdVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 19069357948881143L;

    @NotNull(message = "id不能为空")
    private Long id;
}
