package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 视频商品分页查询请求
 *
 * @author AI Assistant
 * @date 2026-07-28
 */
@Getter
@Setter
public class VideoProductPageRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 直播批次号 */
    @NotBlank(message = "直播批次号不能为空")
    private String batchNumber;

    /** 视频开始时间（格式：yyyy-MM-dd HH:mm:ss） */
    private LocalDateTime startTime;

    /** 视频结束时间（格式：yyyy-MM-dd HH:mm:ss） */
    private LocalDateTime endTime;

    /** 排序字段（默认：payAmt） */
    private String sortBy;

    /** 商品标题（模糊搜索） */
    private String title;

    /** 排序方向：asc-升序，desc-降序（默认：desc） */
    private String sortOrder;

    /** 爱复盘视频ID（传入后从视频表获取真实tenantId，不再使用当前用户租户） */
    private String videoId;

    /** 租户ID（由控制器从 token 注入，或由 videoId 查询得到） */
    private Long tenantId;
}
