package com.jiuyu.replay.generic.bo.aiagent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 主播列表查询入参（多条件过滤 + 游标分页）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AnchorListQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 上一页返回的 nextCursor；首页传 null
     */
    private Long cursor;

    /**
     * 每页条数，范围 1~200，默认 50
     */
    private Integer pageSize;

    /**
     * 主播名称，模糊匹配
     */
    private String anchorName;

    /**
     * 平台类型（多选）：0=抖音 1=快手 2=视频号
     */
    private List<Integer> platforms;

    /**
     * 添加时间范围-起（yyyy-MM-dd HH:mm:ss）
     */
    private String addStartDate;

    /**
     * 添加时间范围-止
     */
    private String addEndDate;

    /**
     * 最后开始录制时间范围-起
     */
    private String lastRecordStartDate;

    /**
     * 最后开始录制时间范围-止
     */
    private String lastRecordEndDate;

    /**
     * 行业 ID；支持父行业，传父行业 id 时服务端自动展开为「该行业 + 所有子孙行业」
     */
    private Long tradeId;

    /**
     * 主播账号（抖音号等），精确匹配
     */
    private String anchorNumber;

    /**
     * 归属类型：0=自有账号 1=同行账号；不传=全部
     */
    private Integer accountType;

    /**
     * 数据源类型：1=自己录制（userId+tenantId）2=全租户（仅 tenantId，仅 userType=0 生效，子账号自动回落为 1）
     * 4=自己录制+云空间（本人 OR 已上传云空间）；主播列表无云空间(3)；不传默认 1
     */
    private Integer dataSourceType = 1;


    /**
     * 行业ID列表
     */
    @JsonIgnore
    private List<Long> tradeIds;
}
