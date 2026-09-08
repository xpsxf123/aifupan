package com.jiuyu.replay.generic.vo.words.video;

import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 子账号昨日有小结数据的视频列表VO
 *
 * @author AI Assistant
 */
@Data
@Schema(description = "子账号昨日有小结数据的视频列表VO")
public class SubUserYesterdayNotesVideoListVo extends AnchorVideoInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主播信息
     */
    @Schema(description = "主播信息")
    private AnchorUrlInfoVo anchorUrlInfoVo;
}
