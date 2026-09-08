package com.jiuyu.replay.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果中的单篇条目。
 *
 * <p>notes 的文案由后端给全，前端只负责展示、不做拼接——
 * 失败原因有十来类，拼接规则散到前端必然与后端判断逻辑漂移。
 *
 * @author claude
 * @date 2026-08-13
 */
@Data
@Schema(description = "导入结果单篇条目")
public class SeoImportItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原始文件名 */
    @Schema(description = "原始文件名")
    private String file;

    /** 解析出的文章标题，解析失败时为空 */
    @Schema(description = "文章标题")
    private String title;

    /** 是否导入成功 */
    @Schema(description = "是否导入成功")
    private Boolean success;

    /** 说明：失败原因，或成功但需要运营知情的项（slug 被追加、标签被截断、图片抓取失败等） */
    @Schema(description = "说明")
    private List<String> notes = new ArrayList<>();
}
