package com.jiuyu.replay.video.project.bo.hotsearch;

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
 * 爆款搜索业务对象
 *
 * @author RayChou
 * @date 2025-08-29
 * @description 爆款搜索请求的业务对象，封装搜索参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款搜索业务对象")
public class VideoHotSearchBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索关键词", example = "美食制作")
    @NotBlank(message = "搜索关键词不能为空")
    @Length(max = 100, message = "搜索关键词长度不能超过100个字符")
    private String searchKeyword;

    @Schema(description = "平台类型: 1-抖音, 2-快手, 3-视频号", example = "1")
    @NotNull(message = "平台类型不能为空")
    @EnumValue(byteValues = {1, 2, 3}, message = "平台类型不合法")
    private Byte platformType;

    @Schema(description = "抓取数据类型：1：服务器 2：第三方", example = "1")
    @NotNull(message = "抓取数据类型不能为空")
    @EnumValue(byteValues = {1, 2}, message = "抓取数据类型不合法")
    private Byte snatchDataType;

    @Schema(description = "搜索结果视频列表")
    @Valid
    private List<VideoHotSearchInfoBo> videoList;
}
