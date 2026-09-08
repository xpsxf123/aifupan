package com.jiuyu.replay.ai.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量报告状态查询请求
 */
@Data
public class BatchReportStatusBo {

    /**
     * 资源列表，最多 100 条
     */
    @NotEmpty(message = "sources 不能为空")
    private List<@Valid SourceItemBo> sources;

    /**
     * 单个资源定位项
     */
    @Data
    public static class SourceItemBo {
        /**
         * 资源类型 0录制视频 1上传文件
         */
        @NotNull(message = "sourceType 不能为空")
        private Integer sourceType;
        /**
         * 业务场景 0复盘 1视频分析 2文案预审
         */
        @NotNull(message = "sceneType 不能为空")
        private Integer sceneType;
        /**
         * 资源ID
         */
        @NotBlank(message = "sourceId 不能为空")
        private String sourceId;

        /**
         * 主播唯一标识，可选。传入后该 source 的 monitors[].monitorEnabled 会按
         * (userId, tenantId, secUid) 查 tb_anchor_url_user 的对应类型开关；
         * 未传 / 查无记录 → 该字段返 null。
         */
        private String secUid;
    }
}
