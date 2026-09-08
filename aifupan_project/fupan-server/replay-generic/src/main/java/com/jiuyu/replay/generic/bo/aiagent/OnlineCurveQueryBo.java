package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 视频在线曲线 + 人群画像查询入参。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OnlineCurveQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 视频唯一标识
     */
    @NotBlank(message = "videoId不能为空")
    private String videoId;

    /**
     * 折线抽样步长，默认 1（越大点越稀疏）
     */
    private Integer step = 1;
}
