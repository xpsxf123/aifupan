package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频弹幕列表查询入参（游标分页）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BarrageQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 视频唯一标识
     */
    @NotBlank(message = "videoId不能为空")
    private String videoId;

    /**
     * 上一页返回的 nextCursor；首页传 null。
     *
     * <p>注意：弹幕底层存储为 TableStore（分页非主键游标），此处 cursor 实为页码（从 1 开始），
     * 由服务端封装。Agent 仍按「原样回传 nextCursor 直到 hasMore=false」使用即可，无需感知其为页码。</p>
     */
    private Long cursor;

    /**
     * 每页条数，默认 200、上限 500
     */
    private Integer pageSize;
}
