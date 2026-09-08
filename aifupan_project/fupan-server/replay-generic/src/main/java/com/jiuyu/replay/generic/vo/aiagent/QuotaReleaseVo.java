package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 算力额度预扣释放结果。
 *
 * @author fupan-server
 */
@Data
public class QuotaReleaseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否已释放（含 no-op：预扣已不存在也返回 true）。 */
    private Boolean released;
}
