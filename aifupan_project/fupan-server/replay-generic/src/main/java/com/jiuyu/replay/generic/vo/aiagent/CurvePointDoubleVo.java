package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author HeHui
 * @date 2026-07-30 19:28
 */
@Getter
@Setter
public class CurvePointDoubleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 3671742301059929764L;
    private Long dateTime;

    private Long dateTimeNew;

    private BigDecimal valueNum;
}
