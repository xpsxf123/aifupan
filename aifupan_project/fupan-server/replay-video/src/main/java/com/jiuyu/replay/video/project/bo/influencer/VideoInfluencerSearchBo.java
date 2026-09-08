package com.jiuyu.replay.video.project.bo.influencer;

import com.jiuyu.replay.common.validated.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 达人搜索业务对象
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 达人搜索请求的业务对象，封装搜索参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人搜索业务对象")
public class VideoInfluencerSearchBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索关键词（抖音号/昵称）", example = "抖音号或昵称")
    @NotBlank(message = "搜索关键词不能为空")
    @Length(max = 100, message = "搜索关键词长度不能超过100个字符")
    private String searchKeyword;

    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    @NotNull(message = "平台类型不能为空")
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法")
    private Byte platformType;

    @Schema(description = "搜素结果达人列表")
    @Valid
    private List<VideoInfluencerSaveSearchInfoBo> influencerList;
}
