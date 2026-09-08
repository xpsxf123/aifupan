package com.jiuyu.replay.third.bo;

import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/11 19:01
 */
@Schema(description = "弹幕导出条件")
@Data
public class QueryDanMuExport extends QueryDanMuBo {

    @Schema(description = "导出文件名称")
    private String fileName;


}
