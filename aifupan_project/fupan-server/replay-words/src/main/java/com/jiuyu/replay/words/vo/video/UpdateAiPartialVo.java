package com.jiuyu.replay.words.vo.video;

import com.jiuyu.replay.words.bo.video.UpdateAiPartialBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/4 14:49
 */
@Data
public class UpdateAiPartialVo extends UpdateAiPartialBo {

    @Schema(description = "是否更新")
    private Boolean isUpdate;

}
