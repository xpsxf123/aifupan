package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户最新一条跟进记录
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Data
public class LatestRemarkVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 跟进内容
     */
    @Schema(description = "跟进内容")
    private String remark;

    /**
     * 记录时间(tb_user_remark.create_date)
     */
    @Schema(description = "记录时间")
    private Date remarkTime;

    /**
     * 跟进人姓名(create_id → nickName)，解析不到为 null
     */
    @Schema(description = "跟进人姓名")
    private String createName;
}
