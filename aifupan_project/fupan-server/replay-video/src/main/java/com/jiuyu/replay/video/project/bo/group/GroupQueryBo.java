package com.jiuyu.replay.video.project.bo.group;

import com.jiuyu.replay.common.validated.EnumValue;
import com.jiuyu.replay.video.project.bo.VideoPageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分组查询业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 分组查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分组查询业务对象")
public class GroupQueryBo extends VideoPageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组类型: 1-达人订阅, 2-爆款订阅
     */
    @Schema(description = "分组类型: 1-达人订阅, 2-爆款订阅", example = "1", allowableValues = {"1", "2"})
    @NotNull(message = "分组类型不能为空")
    @EnumValue(byteValues = {1, 2}, message = "分组类型不合法")
    private Byte groupType;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称", example = "默认分组")
    @Length(max = 50, message = "分组名称长度不能超过50个字符")
    private String groupName;
}
