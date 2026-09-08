package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 算力额度余额。
 *
 * @author fupan-server
 */
@Data
public class QuotaBalanceVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总额度。 */
    private Long total;

    /** 已用额度。 */
    private Long used;

    /** 可用额度（= max(0, total − used)）。 */
    private Long available;

    /** 下次重置时间（当前生效套餐订单中 aiToken 资产的下次清零时间；无生效套餐或不重置时为 null）。 */
    private Date nextResetDate;

    /** 套餐过期时间（当前生效的非增量包订单过期时间；无生效套餐时为 null）。 */
    private Date packageExpireDate;
}
