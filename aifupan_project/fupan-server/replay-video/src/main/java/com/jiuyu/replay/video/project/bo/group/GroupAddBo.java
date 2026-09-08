package com.jiuyu.replay.video.project.bo.group;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 添加分组业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 添加分组参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "添加分组业务对象")
public class GroupAddBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称", example = "新分组")
    @NotBlank(message = "分组名称不能为空")
    @Length(max = 50, message = "分组名称长度不能超过50个字符")
    private String groupName;

    /**
     * 分组类型: 1-达人订阅, 2-爆款订阅
     */
    @Schema(description = "分组类型: 1-达人订阅, 2-爆款订阅", example = "1", allowableValues = {"1", "2"})
    @NotNull(message = "分组类型不能为空")
    @Min(value = 1, message = "分组类型最小为1")
    @Max(value = 2, message = "分组类型最大为2")
    private Byte groupType;

    /**
     * 分组描述
     */
    @Schema(description = "分组描述", example = "这是一个新分组")
    @Length(max = 200, message = "分组描述长度不能超过200个字符")
    private String groupDescription;
}
