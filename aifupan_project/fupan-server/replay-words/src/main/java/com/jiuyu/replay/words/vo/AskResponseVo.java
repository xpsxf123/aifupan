package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/24 下午4:39
 */
@Data
@Schema(description = "助手问答的出参")
public class AskResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "资产剩余")
    private PropertyDto property;

    @Schema(description = "问题")
    private ConversationVo problem;

    @Schema(description = "回答")
    private ConversationVo answer;

    @Schema(description = "是否检查ai内容")
    private Boolean checkAiContent;

    // 资产剩余
    @Data
    public static class PropertyDto{
        @Schema(description = "当前问答使用")
        private Integer currerntUseNum;

        @Schema(description = "剩余资产")
        private Integer surplusNum;
    }

}
