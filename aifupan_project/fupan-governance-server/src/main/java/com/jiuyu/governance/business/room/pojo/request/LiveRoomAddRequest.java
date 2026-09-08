package com.jiuyu.governance.business.room.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 直播间新增请求
 *
 * @author HeHui
 * @date 2026-03-25
 */
@Getter
@Setter
public class LiveRoomAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 直播平台类型 (0: 抖音, 1: 快手, 2: 视频号)
     */
    @NotNull(message = "请选择直播平台")
    private Integer platform;


    /**
     * 主播平台账号 (如抖音号)
     */
    @NotBlank(message = "请输入主播账号")
    private String anchorNumber;

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
     * 被委派为该直播间管理员/负责人的员工ID列表
     */
    private List<Long> managerUserIds;
}
