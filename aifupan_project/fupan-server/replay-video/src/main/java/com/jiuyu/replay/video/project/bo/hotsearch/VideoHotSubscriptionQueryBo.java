package com.jiuyu.replay.video.project.bo.hotsearch;

import com.jiuyu.replay.video.project.bo.VideoPageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 爆款订阅查询业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 爆款订阅查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "爆款订阅查询业务对象")
public class VideoHotSubscriptionQueryBo extends VideoPageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关键词
     */
    @Schema(description = "关键词", example = "关键词1")
    @Length(max = 100, message = "关键词长度不能超过100个字符")
    private String keyword;
}
