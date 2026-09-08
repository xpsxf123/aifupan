package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：ai-身份和额外要求
 * @date ：2025/2/21 下午3:29
 */
@Data
public class AiIdentityAndAdditionalVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "身份设置列表")
    public List<String> identityList;

    @Schema(description = "额外要求列表")
    public List<String> additionalList;
}
