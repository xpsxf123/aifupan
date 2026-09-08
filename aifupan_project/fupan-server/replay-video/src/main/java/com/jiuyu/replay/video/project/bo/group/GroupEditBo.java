package com.jiuyu.replay.video.project.bo.group;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * 编辑分组业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 编辑分组参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "编辑分组业务对象")
public class GroupEditBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @Schema(description = "分组ID", example = "1001")
    @NotNull(message = "分组ID不能为空")
    private Long groupId;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称", example = "修改后的分组")
    @NotBlank(message = "分组名称不能为空")
    @Length(max = 50, message = "分组名称长度不能超过50个字符")
    private String groupName;

    /**
     * 分组描述
     */
    @Schema(description = "分组描述", example = "这是修改后的分组描述")
    @Length(max = 200, message = "分组描述长度不能超过200个字符")
    private String groupDescription;
}
