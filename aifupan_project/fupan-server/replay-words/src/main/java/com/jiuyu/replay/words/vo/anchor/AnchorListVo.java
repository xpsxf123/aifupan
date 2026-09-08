package com.jiuyu.replay.words.vo.anchor;

import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "主播列表信息")
public class AnchorListVo extends AnchorUrlUserVo {

    /**
     * 昨日录制列表
     */
    @Schema(description = "昨日录制列表")
    private List<AnchorYesterdayRecordVo> yesterdayRecordList;
    /**
     * 昨日录制数量
     */
    @Schema(description = "昨日录制数量")
    private Integer yesterdayRecordNum;
}
