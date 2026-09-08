package com.jiuyu.replay.video.project.bo.group;

import com.jiuyu.replay.common.validated.EnumValue;
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
 * 订阅分组业务对象
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 订阅分组的新增、修改请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订阅分组业务对象")
public class SubscriptionGroupBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID（修改时必填）
     */
    @Schema(description = "分组ID（修改时必填）", example = "1")
    private Long id;

    /**
     * 分组类型: 1-达人订阅, 2-爆款订阅
     */
    @Schema(description = "分组类型: 1-达人订阅, 2-爆款订阅", example = "1")
    @NotNull(message = "分组类型不能为空", groups = {Add.class, Update.class})
    @EnumValue(byteValues = {1, 2}, message = "分组类型不合法", groups = {Add.class, Update.class})
    private Byte groupType;

    /**
     * 分组名称
     */
    @Schema(description = "分组名称", example = "美食达人")
    @NotBlank(message = "分组名称不能为空", groups = {Add.class, Update.class})
    @Length(max = 50, message = "分组名称长度不能超过50个字符", groups = {Add.class, Update.class})
    private String groupName;

    /**
     * 分组描述
     */
    @Schema(description = "分组描述", example = "专注美食内容的达人分组")
    @Length(max = 200, message = "分组描述长度不能超过200个字符", groups = {Add.class, Update.class})
    private String description;

    /**
     * 排序
     */
    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    /**
     * 新增分组验证组
     */
    public interface Add {
    }

    /**
     * 修改分组验证组
     */
    public interface Update {
    }
}
