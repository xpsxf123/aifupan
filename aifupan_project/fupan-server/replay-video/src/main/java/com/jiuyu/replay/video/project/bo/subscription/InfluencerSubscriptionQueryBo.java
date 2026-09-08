package com.jiuyu.replay.video.project.bo.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 达人订阅查询业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 达人订阅列表查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "达人订阅查询业务对象")
public class InfluencerSubscriptionQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 达人昵称（模糊查询）
     */
    @Schema(description = "达人昵称", example = "美食达人王老师")
    @Length(max = 50, message = "达人昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 行业筛选
     */
    @Schema(description = "行业筛选", example = "1")
    private Long industryId;
}
