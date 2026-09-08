package com.jiuyu.governance.business.room.pojo.request;

import com.baomidou.mybatisplus.annotation.TableField;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 直播间修改请求
 *
 * @author HeHui
 * @date 2026-03-25
 */
@Getter
@Setter
public class LiveRoomUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 要修改的直播间主键ID
     */
    @NotNull(message = "直播间ID不能为空")
    private Long id;

    /**
     * 绑定的行业分类ID
     */
    private Long tradeId;

    /**
     * 所属子公司的唯一ID
     */
    @NotNull(message = "所属公司不能为空")
    private Long companyId;

    /**
     * 所属部门的唯一ID (非必填，若不传则归属于公司)
     */
    private Long deptId;

    /**
     * 所属小组的唯一ID (非必填，若不传则归属于部门/公司)
     */
    private Long teamId;

    /**
     * 首播日期
     */
    private LocalDate debutDate;

    /**
     * 重新委派为该直播间管理员/负责人的员工ID列表 (全量覆盖)
     */
    private List<Long> managerUserIds;
}
